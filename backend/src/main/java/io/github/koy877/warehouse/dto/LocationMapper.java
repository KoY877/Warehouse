package io.github.koy877.warehouse.dto;

import io.github.koy877.warehouse.entities.Location;

public class LocationMapper {
    
    public static LocationResponse toResponse(Location location) {
        return new LocationResponse(
           location.getId(),
           location.getCode(),
           location.getCapacity(),
           location.getCreatedAt(),
           location.getUpdatedAt()
        );
    }
}
