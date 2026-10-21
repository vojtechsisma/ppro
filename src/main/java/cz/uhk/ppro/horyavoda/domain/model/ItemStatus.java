package cz.uhk.ppro.horyavoda.domain.model;

/** Provozní stav fyzického kusu vybavení. */
public enum ItemStatus {
    /** Volný k výpůjčce. */
    AVAILABLE,
    /** Aktuálně zapůjčen. */
    RENTED,
    /** V servisní údržbě – nesmí se půjčit ani rezervovat. */
    IN_SERVICE,
    /** Vyřazeno z fondu. */
    RETIRED
}
