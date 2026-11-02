package cz.uhk.ppro.horyavoda.presentation.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stránky frontendu se načtou i bez tokenu; přesměrování na přihlášení řeší až skript stránky. */
class StaticPagesTest extends ApiIntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/index.html", "/login.html", "/customers.html", "/categories.html", "/rentals.html", "/reservations.html",
            "/css/style.css", "/js/common.js", "/js/app.js", "/js/customers.js", "/js/categories.js",
            "/js/rentals.js", "/js/reservations.js"
    })
    void frontendResourceIsServedWithoutToken(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }
}
