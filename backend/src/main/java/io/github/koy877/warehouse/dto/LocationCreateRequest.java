package io.github.koy877.warehouse.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record LocationCreateRequest(
    @NotBlank String code,
    @NotNull @PositiveOrZero Integer capacity
) { 
}
