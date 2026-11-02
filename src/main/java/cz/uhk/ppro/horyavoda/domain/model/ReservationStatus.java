package cz.uhk.ppro.horyavoda.domain.model;

/** Stav rezervace typu. */
public enum ReservationStatus {
    /** Drží kapacitu do času vyzvednutí (plus tolerance). */
    ACTIVE,
    /** Zákazník si vybavení vyzvedl, vznikla výpůjčka. */
    FULFILLED,
    /** Zákazník nepřišel včas, kapacita se uvolnila. */
    EXPIRED,
    CANCELLED
}
