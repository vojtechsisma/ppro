package cz.uhk.ppro.horyavoda.presentation.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiIntegrationTest {

    @Test
    void registerUserSucceedsAndNeverExposesPasswordHash() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "novyrecepcni", "email": "recepce@horyavoda.cz", "password": "tajneheslo123",
                                 "full_name": "Karel Novák", "role": "STAFF"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("novyrecepcni"))
                .andExpect(jsonPath("$.email").value("recepce@horyavoda.cz"))
                .andExpect(jsonPath("$.hashed_password").doesNotExist());
    }

    @Test
    void registerDuplicateUsernameFails() throws Exception {
        createAdminUser();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "testadmin", "email": "jiny@example.com", "password": "heslo123",
                                 "full_name": "Duplikát", "role": "STAFF"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("již existuje")));
    }

    @Test
    void loginSucceedsWithValidCredentials() throws Exception {
        createAdminUser();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", ADMIN_USERNAME)
                        .param("password", ADMIN_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.user.username").value(ADMIN_USERNAME));
    }

    @Test
    void loginWithInvalidPasswordFails() throws Exception {
        createAdminUser();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", ADMIN_USERNAME)
                        .param("password", "spatneheslo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(containsString("Neplatné")));
    }

    @Test
    void currentUserProfileIsReturnedForValidToken() throws Exception {
        String authorization = adminAuthorizationHeader();
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME));
    }

    @Test
    void currentUserProfileWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
