package io.github.koy877.warehouse.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.koy877.warehouse.entities.Stocks;

public interface StocksRepository extends JpaRepository<Stocks, String>{
    Optional<Stocks> findByProductIdAndLocationId(String productId, String locationId);
}
