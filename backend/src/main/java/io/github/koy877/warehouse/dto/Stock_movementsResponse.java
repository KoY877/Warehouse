package io.github.koy877.warehouse.dto;

import java.time.Instant;

import io.github.koy877.warehouse.entities.enums.MovementType;

public record Stock_movementsResponse (
    String id,
    String productId,
    String sourceLocationId,
    String targetLocationId,
    MovementType type,
    String userId,
    Integer quantity,
    Instant created_at
) {
    
}
