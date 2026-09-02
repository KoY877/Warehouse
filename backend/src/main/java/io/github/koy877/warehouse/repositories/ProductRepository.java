package io.github.koy877.warehouse.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.koy877.warehouse.entities.Product;

public interface ProductRepository extends JpaRepository<Product, String> {
    boolean existsBySku(String sku);

}