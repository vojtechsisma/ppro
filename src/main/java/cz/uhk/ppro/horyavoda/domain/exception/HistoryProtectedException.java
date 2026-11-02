package cz.uhk.ppro.horyavoda.domain.exception;

/** Záznam má historii výpůjček, která se nesmí smazat (reklamace, rozhodování o obměně). */
public class HistoryProtectedException extends DomainException {

    public HistoryProtectedException(String message) {
        super(message);
    }
}
