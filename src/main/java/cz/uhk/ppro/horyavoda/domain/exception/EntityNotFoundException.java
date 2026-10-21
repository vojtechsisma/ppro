package cz.uhk.ppro.horyavoda.domain.exception;

/** Entita nebyla nalezena v datovém úložišti. */
public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
