package cz.uhk.ppro.horyavoda.presentation.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pravidla, na kterých klient trvá: žádný překryv termínů jednoho kusu, kus v servisu se nepůjčuje,
 * penále za každý započatý den prodlení a nemazatelná historie výpůjček.
 * Hodiny jsou pevně nastavené na sobotu 23. 1. 2027 8:00.
 */
class RentalApiTest extends ApiIntegrationTest {

    static final ZoneId ZONE = ZoneId.of("Europe/Prague");
    static final LocalDateTime NOW = LocalDateTime.of(2027, 1, 23, 8, 0);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        }
    }

    private String authorization;
    private long customerId;
    private long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        authorization = adminAuthorizationHeader();
        categoryId = createCategory(authorization, "Lyže", "LYZE", 250.0, 450.0);
        customerId = createCustomer("Jan", "Novák", "OP000123");
    }

    @Test
    void overlappingRentalOfTheSameItemIsRejected() throws Exception {
        long ski = createItem("L-412");
        mockMvc.perform(createRental(at("2027-01-29T15:00"), at("2027-01-31T18:00"), ski))
                .andExpect(status().isCreated());

        mockMvc.perform(createRental(at("2027-01-30T09:00"), at("2027-02-01T18:00"), ski))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("L-412")));
    }

    @Test
    void backToBackRentalsDoNotOverlap() throws Exception {
        long ski = createItem("L-413");
        mockMvc.perform(createRental(at("2027-01-29T15:00"), at("2027-01-31T18:00"), ski))
                .andExpect(status().isCreated());

        mockMvc.perform(createRental(at("2027-01-31T18:00"), at("2027-02-02T18:00"), ski))
                .andExpect(status().isCreated());
    }

    @Test
    void overdueItemThatIsStillOutBlocksNewRentals() throws Exception {
        long ski = createItem("L-414");
        // Měl se vrátit včera večer a pořád je venku
        mockMvc.perform(createRental(at("2027-01-20T09:00"), at("2027-01-22T18:00"), ski))
                .andExpect(status().isCreated());

        mockMvc.perform(createRental(at("2027-01-30T09:00"), at("2027-01-31T18:00"), ski))
                .andExpect(status().isConflict());
    }

    @Test
    void itemInServiceCannotBeRented() throws Exception {
        long ski = createItem("L-415");
        mockMvc.perform(post("/api/equipment/{id}/service", ski).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk());

        mockMvc.perform(createRental(at("2027-01-30T09:00"), at("2027-01-31T18:00"), ski))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("v servisu")));
    }

    @Test
    void rentedItemCannotBeSentToService() throws Exception {
        long ski = createItem("L-416");
        mockMvc.perform(createRental(at("2027-01-23T07:30"), at("2027-01-24T18:00"), ski))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/equipment/{id}/service", ski).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isConflict());
    }

    @Test
    void partialReturnChargesPenaltyOnlyForTheLateItem() throws Exception {
        long ski = createItem("L-417");
        long boots = createItem("L-418");
        MvcResult created = mockMvc.perform(createRental(at("2027-01-20T09:00"), at("2027-01-21T18:00"), ski, boots))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        long rentalId = readId(created);
        String body = created.getResponse().getContentAsString();
        long skiItemId = rentalItemId(body, "L-417");
        long bootsItemId = rentalItemId(body, "L-418");

        // Lyže včas, výpůjčka zůstává otevřená
        mockMvc.perform(returnItem(rentalId, skiItemId, "2027-01-21T17:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED_PARTIAL"))
                .andExpect(jsonPath("$.total_late_fee").value(0));

        // Boty o 25 hodin později = 2 započaté dny × 250 Kč
        mockMvc.perform(returnItem(rentalId, bootsItemId, "2027-01-22T19:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[?(@.inventory_number == 'L-418')].late_fee").value(500.0))
                .andExpect(jsonPath("$.items[?(@.inventory_number == 'L-417')].late_fee").value(0.0))
                .andExpect(jsonPath("$.total_late_fee").value(500.0));

        mockMvc.perform(get("/api/equipment/{id}", boots))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void itemCannotBeReturnedTwice() throws Exception {
        long ski = createItem("L-419");
        MvcResult created = mockMvc.perform(createRental(at("2027-01-22T09:00"), at("2027-01-24T18:00"), ski))
                .andExpect(status().isCreated())
                .andReturn();
        long rentalId = readId(created);
        long rentalItemId = rentalItemId(created.getResponse().getContentAsString(), "L-419");

        mockMvc.perform(returnItem(rentalId, rentalItemId, "2027-01-23T07:00")).andExpect(status().isOk());
        mockMvc.perform(returnItem(rentalId, rentalItemId, "2027-01-23T07:30")).andExpect(status().isBadRequest());
    }

    @Test
    void itemWithRentalHistoryIsNotDeleted() throws Exception {
        long ski = createItem("L-420");
        mockMvc.perform(createRental(at("2027-02-05T09:00"), at("2027-02-06T18:00"), ski))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/equipment/{id}", ski).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("historii")));

        mockMvc.perform(get("/api/equipment/{id}", ski)).andExpect(status().isOk());
        mockMvc.perform(get("/api/rentals").param("equipment_item_id", String.valueOf(ski))
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rentalsRequireLogin() throws Exception {
        mockMvc.perform(get("/api/rentals")).andExpect(status().isUnauthorized());
    }

    // --- pomocné metody ---

    private static long rentalItemId(String rentalJson, String inventoryNumber) {
        List<Number> ids = JsonPath.read(rentalJson, "$.items[?(@.inventory_number == '" + inventoryNumber + "')].id");
        return ids.get(0).longValue();
    }

    private static String at(String dateTime) {
        return dateTime + ":00";
    }

    private long createCustomer(String firstName, String lastName, String document) throws Exception {
        String body = """
                {"first_name": "%s", "last_name": "%s", "phone": "+420600000000", "id_document_number": "%s"}
                """.formatted(firstName, lastName, document);
        return readId(mockMvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createItem(String inventoryNumber) throws Exception {
        String body = """
                {"category_id": %d, "inventory_number": "%s", "model_name": "Atomic Redster", "size": "170 cm", "year_acquired": 2024}
                """.formatted(categoryId, inventoryNumber);
        return readId(mockMvc.perform(post("/api/equipment")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private RequestBuilder createRental(String start, String end, long... equipmentItemIds) {
        String ids = Arrays.stream(equipmentItemIds).mapToObj(String::valueOf).collect(Collectors.joining(", "));
        String body = """
                {"customer_id": %d, "planned_start": "%s", "planned_end": "%s", "equipment_item_ids": [%s]}
                """.formatted(customerId, start, end, ids);
        return post("/api/rentals")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private RequestBuilder returnItem(long rentalId, long rentalItemId, String returnedAt) {
        return post("/api/rentals/{rentalId}/items/{itemId}/return", rentalId, rentalItemId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"returned_at\": \"" + at(returnedAt) + "\"}");
    }
}
