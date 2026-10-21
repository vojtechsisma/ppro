package cz.uhk.ppro.horyavoda.domain.model;

/** Stav jedné položky výpůjčky – každý kus se vrací samostatně. */
public enum RentalItemStatus {
    RENTED,
    RETURNED,
    DAMAGED
}
