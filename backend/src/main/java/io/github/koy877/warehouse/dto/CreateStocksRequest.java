package io.github.koy877.warehouse.dto;

import jakarta.validation.constraints.NotNull;

public record CreateStocksRequest (
    @NotNull String product,
    @NotNull String location,
    @NotNull Integer quantity
) {
    
}
