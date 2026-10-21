package cz.uhk.ppro.horyavoda.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfigurace autentizace načtená z prostředí (SECRET_KEY, ACCESS_TOKEN_EXPIRE_MINUTES).
 *
 * @param jwtSecret                 sdílený klíč pro podpis JWT (HS256), minimálně 32 bajtů
 * @param accessTokenExpireMinutes  platnost vydaného access tokenu v minutách
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String jwtSecret, long accessTokenExpireMinutes) {

    private static final int MIN_SECRET_BYTES = 32;

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Chybí proměnná prostředí SECRET_KEY (klíč pro podpis JWT).");
        }
        if (jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("SECRET_KEY musí mít alespoň " + MIN_SECRET_BYTES + " bajtů (požadavek HS256).");
        }
        if (accessTokenExpireMinutes <= 0) {
            throw new IllegalStateException("ACCESS_TOKEN_EXPIRE_MINUTES musí být kladné číslo.");
        }
    }
}
