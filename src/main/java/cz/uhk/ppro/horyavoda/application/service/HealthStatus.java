package cz.uhk.ppro.horyavoda.application.service;

/**
 * Provozní stav aplikace.
 *
 * @param databaseConnected zda je relační databáze dostupná
 * @param databaseError     popis chyby spojení (null, pokud je databáze dostupná)
 */
public record HealthStatus(boolean databaseConnected, String databaseError) {
}
