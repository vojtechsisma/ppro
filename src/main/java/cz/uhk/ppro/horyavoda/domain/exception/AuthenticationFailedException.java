package cz.uhk.ppro.horyavoda.domain.exception;

/** Chybné přihlašovací údaje nebo neaktivní účet. */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
