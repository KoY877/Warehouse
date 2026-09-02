package io.github.koy877.warehouse.dto;

import io.github.koy877.warehouse.entities.Stocks;

public class StocksMapper {
    public static StockResponse toResponse(Stocks stocks) {
        return new StockResponse(
            stocks.getId(),
            stocks.getProduct().getId(),
            stocks.getLocation().getId(),
            stocks.getQuantity(),          
            stocks.getCreatedAt(),
            stocks.getUpdatedAt()
        );
    }
}
