package cz.uhk.ppro.horyavoda.domain.exception;

/** Základní bázová třída pro všechny doménové výjimky. */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
