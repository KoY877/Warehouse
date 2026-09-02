package io.github.koy877.warehouse.dto;

import io.github.koy877.warehouse.entities.Location;
import jakarta.validation.constraints.NotNull;

public record StockCreateRequest (
    @NotNull String productId,
    @NotNull Location locationId,
    @NotNull Integer quantity
){
}
