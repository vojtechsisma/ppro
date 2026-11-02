package cz.uhk.ppro.horyavoda.infrastructure.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nastavení naplnění databáze syntetickými daty.
 *
 * @param enabled       zda se má seed při startu spustit
 * @param adminPassword heslo výchozího správce (SEED_ADMIN_PASSWORD); prázdné = účet se nevytvoří
 * @param staffPassword heslo výchozí obsluhy pultu (SEED_STAFF_PASSWORD); prázdné = účet se nevytvoří
 * @param demoData      ukázkový provoz (SEED_DEMO_DATA): další kusy, zákazníci, výpůjčky a rezervace
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(boolean enabled, String adminPassword, String staffPassword, boolean demoData) {
}
