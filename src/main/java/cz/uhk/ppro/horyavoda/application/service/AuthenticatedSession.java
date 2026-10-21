package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.domain.model.User;

/** Výsledek úspěšného přihlášení: ověřený uživatel a jeho access token. */
public record AuthenticatedSession(User user, String accessToken) {
}
