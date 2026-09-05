package io.github.koy877.warehouse.dto;

import io.github.koy877.warehouse.entities.StockMovement;


public class Stock_MovementsMapper {

    public static Stock_movementsResponse toResponse(StockMovement stockMovement) {
        return new Stock_movementsResponse(
            stockMovement.getId(),
            stockMovement.getProduct().getId(),
            stockMovement.getSourceLocation() != null ? stockMovement.getSourceLocation().getId() : null,
            stockMovement.getTargetLocation() != null ? stockMovement.getTargetLocation().getId() : null,
            stockMovement.getType(),
            stockMovement.getUser().getId(),
            stockMovement.getQuantity(),          
            stockMovement.getCreatedAt()
        );
    }
} 