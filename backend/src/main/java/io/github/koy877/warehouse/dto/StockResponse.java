package io.github.koy877.warehouse.dto;

import java.time.Instant;

import io.github.koy877.warehouse.entities.Location;

public record StockResponse (
    String id,
    String productId,
    String locationId,
    Integer quantity,
    Instant createdAt,
    Instant updateAt
) { 
}
