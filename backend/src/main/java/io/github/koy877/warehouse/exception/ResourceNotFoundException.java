package io.github.koy877.warehouse.exception;

/**
 * Wird geworfen, wenn eine angeforderte Entitaet nicht existiert.
 * Vom GlobalExceptionHandler auf 404 gemapped.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
