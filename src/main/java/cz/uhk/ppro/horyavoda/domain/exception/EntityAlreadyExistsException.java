package cz.uhk.ppro.horyavoda.domain.exception;

/** Entita se zadaným unikátním identifikátorem již existuje. */
public class EntityAlreadyExistsException extends DomainException {

    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
