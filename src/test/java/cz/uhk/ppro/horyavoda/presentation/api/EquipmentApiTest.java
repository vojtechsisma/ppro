package cz.uhk.ppro.horyavoda.presentation.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquipmentApiTest extends ApiIntegrationTest {

    @Test
    void createdCategoryAppearsInPublicList() throws Exception {
        String authorization = adminAuthorizationHeader();
        long categoryId = createCategory(authorization, "Běžky", "BEZKY", 300.0, 550.0);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) categoryId)));
    }

    @Test
    void createEquipmentSucceeds() throws Exception {
        String authorization = adminAuthorizationHeader();
        long categoryId = createCategory(authorization, "Lyže", "LYZE", 400.0, 700.0);

        mockMvc.perform(createEquipmentRequest(authorization, categoryId, "SK-001", "Salomon Race"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inventory_number").value("SK-001"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.category.code").value("LYZE"));
    }

    @Test
    void duplicateInventoryNumberIsRejected() throws Exception {
        String authorization = adminAuthorizationHeader();
        long categoryId = createCategory(authorization, "Snowboardy", "SNB", 350.0, 600.0);

        mockMvc.perform(createEquipmentRequest(authorization, categoryId, "SNB-001", "Burton Rip"))
                .andExpect(status().isCreated());
        mockMvc.perform(createEquipmentRequest(authorization, categoryId, "SNB-001", "Jiný model"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("již v systému existuje")));
    }

    @Test
    void itemSentToServiceIsNotOfferedAsAvailable() throws Exception {
        String authorization = adminAuthorizationHeader();
        long categoryId = createCategory(authorization, "Hole", "HOLE", 100.0, 180.0);
        MvcResult created = mockMvc.perform(createEquipmentRequest(authorization, categoryId, "HOL-01", "Leki Worldcup"))
                .andExpect(status().isCreated())
                .andReturn();
        long itemId = readId(created);

        mockMvc.perform(post("/api/equipment/{id}/service", itemId).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_SERVICE"));

        // Ranní pultový filtr volných kusů nesmí kus v servisu nabídnout
        mockMvc.perform(get("/api/equipment").param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem((int) itemId))));
    }

    @Test
    void deletedEquipmentIsNoLongerFound() throws Exception {
        String authorization = adminAuthorizationHeader();
        long categoryId = createCategory(authorization, "Helmy", "HELM", 100.0, 180.0);
        MvcResult created = mockMvc.perform(createEquipmentRequest(authorization, categoryId, "HLM-01", "Uvex P1"))
                .andExpect(status().isCreated())
                .andReturn();
        long itemId = readId(created);

        mockMvc.perform(delete("/api/equipment/{id}", itemId).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/equipment/{id}", itemId))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingEquipmentWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category_id\": 1, \"inventory_number\": \"X-1\", \"model_name\": \"Test\", \"size\": \"M\", \"year_acquired\": 2024}"))
                .andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.RequestBuilder createEquipmentRequest(
            String authorization, long categoryId, String inventoryNumber, String modelName) {
        String body = """
                {"category_id": %d, "inventory_number": "%s", "model_name": "%s", "size": "170 cm", "year_acquired": 2023}
                """.formatted(categoryId, inventoryNumber, modelName);
        return post("/api/equipment")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
