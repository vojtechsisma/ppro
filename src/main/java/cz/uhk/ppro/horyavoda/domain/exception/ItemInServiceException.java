package cz.uhk.ppro.horyavoda.domain.exception;

/** Kus se nachází v servisu nebo je zapůjčen a nelze s ním požadovanou operaci provést. */
public class ItemInServiceException extends DomainException {

    public ItemInServiceException(String message) {
        super(message);
    }
}
