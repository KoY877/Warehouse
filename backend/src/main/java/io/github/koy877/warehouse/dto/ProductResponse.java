package io.github.koy877.warehouse.dto;

import java.time.Instant;

public record ProductResponse(
    String id,
    String sku,
    String name,
    String description,
    String unit,
    Integer minStock,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
