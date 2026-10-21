package cz.uhk.ppro.horyavoda.presentation.api;

import com.jayway.jsonpath.JsonPath;
import cz.uhk.ppro.horyavoda.domain.model.User;
import cz.uhk.ppro.horyavoda.domain.model.UserRole;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Společný základ integračních testů API: celý Spring kontext nad in-memory H2
 * se schématem z Flyway migrací. Každý test běží v transakci, která se na konci vrátí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ApiIntegrationTest {

    protected static final String ADMIN_USERNAME = "testadmin";
    protected static final String ADMIN_PASSWORD = "adminpass123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Vytvoří testovacího administrátora (obdoba fixture admin_user). */
    protected User createAdminUser() {
        return userRepository.save(new User(
                ADMIN_USERNAME, "testadmin@example.com", passwordEncoder.encode(ADMIN_PASSWORD),
                "Test Administrátor", UserRole.ADMIN));
    }

    /** Přihlásí administrátora a vrátí hodnotu hlavičky Authorization (obdoba fixture auth_headers). */
    protected String adminAuthorizationHeader() throws Exception {
        createAdminUser();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", ADMIN_USERNAME)
                        .param("password", ADMIN_PASSWORD))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
        return "Bearer " + token;
    }

    /** Vytvoří kategorii přes API a vrátí její ID. */
    protected long createCategory(String authorization, String name, String code, double baseDailyRate, double weekendRate)
            throws Exception {
        String body = """
                {"name": "%s", "code": "%s", "base_daily_rate": %s, "weekend_rate": %s}
                """.formatted(name, code, baseDailyRate, weekendRate);
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    protected static long readId(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
