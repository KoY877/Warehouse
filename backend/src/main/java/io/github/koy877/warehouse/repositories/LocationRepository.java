package io.github.koy877.warehouse.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.koy877.warehouse.entities.Location;

public interface LocationRepository extends JpaRepository<Location, String>{

} 
