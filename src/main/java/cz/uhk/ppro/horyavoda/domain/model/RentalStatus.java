package cz.uhk.ppro.horyavoda.domain.model;

/** Stav výpůjčky jako celku. */
public enum RentalStatus {
    ACTIVE,
    /** Část kusů je vrácena, část zůstává u zákazníka. */
    RETURNED_PARTIAL,
    COMPLETED,
    OVERDUE,
    CANCELLED
}
