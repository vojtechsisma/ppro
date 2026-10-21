package cz.uhk.ppro.horyavoda.domain.exception;

/** Kus je v daném časovém okně již zarezervován nebo vypůjčen. */
public class DoubleBookingException extends DomainException {

    public DoubleBookingException(String message) {
        super(message);
    }
}
