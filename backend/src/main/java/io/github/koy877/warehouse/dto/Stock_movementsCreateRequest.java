package io.github.koy877.warehouse.dto;

import io.github.koy877.warehouse.entities.enums.MovementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Stock_movementsCreateRequest(
        @NotBlank String productId,
        String sourceLocation,
        String targetLocation,
        @NotNull MovementType type,
        @NotNull @Positive Integer quantity) {
}
