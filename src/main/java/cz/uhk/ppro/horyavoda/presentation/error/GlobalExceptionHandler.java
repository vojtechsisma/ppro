package cz.uhk.ppro.horyavoda.presentation.error;

import cz.uhk.ppro.horyavoda.domain.exception.AuthenticationFailedException;
import cz.uhk.ppro.horyavoda.domain.exception.DomainException;
import cz.uhk.ppro.horyavoda.domain.exception.DoubleBookingException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityAlreadyExistsException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityNotFoundException;
import cz.uhk.ppro.horyavoda.presentation.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/** Překlad doménových a validačních výjimek na HTTP odpovědi ve formátu {@code {"detail": "..."}}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({EntityAlreadyExistsException.class, DoubleBookingException.class})
    public ResponseEntity<ErrorResponse> handleConflict(DomainException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(new ErrorResponse(exception.getMessage()));
    }

    /** Ostatní porušení obchodních pravidel (např. pokus smazat zapůjčený kus). */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "Neplatný požadavek – " + detail);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "Neplatný formát požadavku.");
    }

    /** Porušení integrity v databázi, typicky smazání kusu s historií výpůjček. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "Operaci nelze provést, záznam je navázán na jiná data (např. historii výpůjček).");
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(new ErrorResponse(detail));
    }
}
