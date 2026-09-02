package io.github.koy877.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductCreateRequest(
    @NotBlank String sku,
    @NotBlank String name,
    String description,
    @NotBlank String unit,
    @NotNull @PositiveOrZero Integer minStock

) {
    
}
