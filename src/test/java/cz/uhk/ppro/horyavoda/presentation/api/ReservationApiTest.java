package cz.uhk.ppro.horyavoda.presentation.api;

import cz.uhk.ppro.horyavoda.application.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rezervace typu (kategorie + velikost): drží kapacitu, výpůjčky ji nesmí sebrat,
 * konkrétní kus se přiřadí při výdeji a nevyzvednutá rezervace v 10:30 propadne.
 * Zákazník volá v úterý 19. 1. 2027 v poledne, přijede v sobotu 23. 1. ráno.
 */
class ReservationApiTest extends ApiIntegrationTest {

    static final ZoneId ZONE = ZoneId.of("Europe/Prague");
    static final LocalDateTime TUESDAY_NOON = LocalDateTime.of(2027, 1, 19, 12, 0);
    static final String SATURDAY_MORNING = "2027-01-23T08:00:00";
    static final String SUNDAY_EVENING = "2027-01-24T18:00:00";

    static final MutableClock CLOCK = new MutableClock(TUESDAY_NOON, ZONE);

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return CLOCK;
        }
    }

    @Autowired
    private ReservationService reservationService;

    private String authorization;
    private long customerId;
    private long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        CLOCK.setTo(TUESDAY_NOON);
        authorization = adminAuthorizationHeader();
        categoryId = createCategory(authorization, "Lyže", "LYZE", 250.0, 450.0);
        customerId = createCustomer("OP100200");
    }

    @Test
    void reservationCannotPromiseMoreThanIsFree() throws Exception {
        createItem("L-501", "170 cm");
        createItem("L-502", "170 cm");
        createItem("L-503", "160 cm");

        mockMvc.perform(createReservation("170 cm", 2)).andExpect(status().isCreated());
        mockMvc.perform(createReservation("170 cm", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("volných jen 0")));
    }

    @Test
    void sizeIsMatchedRegardlessOfCaseAndSpaces() throws Exception {
        createItem("L-504", "170 cm");
        mockMvc.perform(createReservation(" 170 CM ", 1)).andExpect(status().isCreated());
    }

    @Test
    void rentalCannotTakeItemsHeldByReservation() throws Exception {
        long first = createItem("L-505", "170 cm");
        long second = createItem("L-506", "170 cm");
        mockMvc.perform(createReservation("170 cm", 1)).andExpect(status().isCreated());

        // Jeden kus zbývá – ten půjčit jde, druhý drží rezervace
        mockMvc.perform(createRental(SATURDAY_MORNING, SUNDAY_EVENING, first)).andExpect(status().isCreated());
        mockMvc.perform(createRental(SATURDAY_MORNING, SUNDAY_EVENING, second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("rezervované")));
    }

    @Test
    void pickupAssignsConcreteItemsAndCreatesRental() throws Exception {
        long ski = createItem("L-507", "170 cm");
        long otherSize = createItem("L-508", "160 cm");
        long reservationId = readId(mockMvc.perform(createReservation("170 cm", 1)).andExpect(status().isCreated()).andReturn());

        CLOCK.setTo(LocalDateTime.of(2027, 1, 23, 8, 30));
        mockMvc.perform(pickUp(reservationId, otherSize))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("neodpovídá")));

        mockMvc.perform(pickUp(reservationId, ski))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.rental_id").value(notNullValue()));
        mockMvc.perform(get("/api/equipment/{id}", ski))
                .andExpect(jsonPath("$.status").value("RENTED"));
    }

    @Test
    void reservationHoldsUntilHalfPastTenThenExpires() throws Exception {
        long ski = createItem("L-509", "170 cm");
        long reservationId = readId(mockMvc.perform(createReservation("170 cm", 1)).andExpect(status().isCreated()).andReturn());

        CLOCK.setTo(LocalDateTime.of(2027, 1, 23, 10, 29));
        mockMvc.perform(getReservation(reservationId)).andExpect(jsonPath("$.status").value("ACTIVE"));

        CLOCK.setTo(LocalDateTime.of(2027, 1, 23, 10, 31));
        mockMvc.perform(getReservation(reservationId)).andExpect(jsonPath("$.status").value("EXPIRED"));
        mockMvc.perform(pickUp(reservationId, ski))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("propadla")));

        // Propadlá rezervace kapacitu nedrží – kus jde půjčit někomu u pultu
        mockMvc.perform(createRental("2027-01-23T10:31:00", SUNDAY_EVENING, ski)).andExpect(status().isCreated());
    }

    @Test
    void expiryJobMarksUncollectedReservations() throws Exception {
        createItem("L-510", "170 cm");
        long reservationId = readId(mockMvc.perform(createReservation("170 cm", 1)).andExpect(status().isCreated()).andReturn());

        CLOCK.setTo(LocalDateTime.of(2027, 1, 23, 11, 0));
        assertEquals(1, reservationService.expireOverdueReservations());
        assertEquals("EXPIRED", reservationService.getReservation(reservationId).getStatus().name());
    }

    @Test
    void cancelledReservationReleasesCapacity() throws Exception {
        createItem("L-511", "170 cm");
        long reservationId = readId(mockMvc.perform(createReservation("170 cm", 1)).andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(createReservation("170 cm", 1)).andExpect(status().isCreated());
    }

    @Test
    void reservationKeepsPriceAgreedAtBooking() throws Exception {
        createItem("L-512", "170 cm");
        mockMvc.perform(createReservation("170 cm", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unit_daily_price").value(250.0))
                .andExpect(jsonPath("$.holds_until").value("2027-01-23T10:30:00"));
    }

    // --- pomocné metody ---

    private long createCustomer(String document) throws Exception {
        String body = """
                {"first_name": "Petra", "last_name": "Dvořáková", "phone": "+420600000001", "id_document_number": "%s"}
                """.formatted(document);
        return readId(mockMvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createItem(String inventoryNumber, String size) throws Exception {
        String body = """
                {"category_id": %d, "inventory_number": "%s", "model_name": "Elan Wingman", "size": "%s", "year_acquired": 2024}
                """.formatted(categoryId, inventoryNumber, size);
        MvcResult result = mockMvc.perform(post("/api/equipment")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private RequestBuilder createReservation(String size, int quantity) {
        String body = """
                {"customer_id": %d, "category_id": %d, "size": "%s", "quantity": %d,
                 "planned_start": "%s", "planned_end": "%s"}
                """.formatted(customerId, categoryId, size, quantity, SATURDAY_MORNING, SUNDAY_EVENING);
        return post("/api/reservations")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private RequestBuilder getReservation(long reservationId) {
        return get("/api/reservations/{id}", reservationId).header(HttpHeaders.AUTHORIZATION, authorization);
    }

    private RequestBuilder pickUp(long reservationId, long... equipmentItemIds) {
        String ids = Arrays.stream(equipmentItemIds).mapToObj(String::valueOf).collect(Collectors.joining(", "));
        return post("/api/reservations/{id}/pickup", reservationId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipment_item_ids\": [" + ids + "]}");
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
}
