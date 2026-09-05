package io.github.koy877.warehouse.dto;

import java.time.Instant;

public record LocationResponse(
    String id,
    String code,
    Integer capacity,
    Instant createdAt,
    Instant updatedAt
) {    
}
