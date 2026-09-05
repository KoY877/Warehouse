package io.github.koy877.warehouse.exception;

import java.time.Instant;

/**
 * Einheitliches Fehlerformat fuer alle 4xx-Antworten der API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
