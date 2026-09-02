package io.github.koy877.warehouse.dto;

import io.github.koy877.warehouse.entities.Product;

public class ProductMapper {
    

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getDescription(),
            product.getUnit(),
            product.getMinStock(),
            product.getActive(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
