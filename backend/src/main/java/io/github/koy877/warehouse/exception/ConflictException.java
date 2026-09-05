package io.github.koy877.warehouse.exception;

/**
 * Wird geworfen, wenn eine Aktion einen fachlichen Konflikt verursachen
 * wuerde (z. B. letzter verbleibender ADMIN wird degradiert). Vom
 * GlobalExceptionHandler auf 409 gemapped.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
