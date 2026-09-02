package io.github.koy877.warehouse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import io.github.koy877.warehouse.dto.LocationCreateRequest;
import io.github.koy877.warehouse.dto.LocationMapper;
import io.github.koy877.warehouse.dto.LocationResponse;
import io.github.koy877.warehouse.entities.Location;
import io.github.koy877.warehouse.exception.ResourceNotFoundException;
import io.github.koy877.warehouse.repositories.LocationRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Validated
public class LocationService {
    
    private final LocationRepository locationRepository;

    @Transactional
    public LocationResponse createLocation(@NotNull LocationCreateRequest request){

        // Create a Location
        Location location = new Location();
        location.setCode(request.code());
        location.setCapacity(request.capacity());

        Location saved = locationRepository.save(location);

        return LocationMapper.toResponse(saved);
    }

    @Transactional
    public List<LocationResponse> getAllLocations(){
        // Load all Locations and map each one to its response DTO
        return locationRepository.findAll()
                    .stream()
                    .map(LocationMapper::toResponse)
                    .toList();
    }

    @Transactional
    public LocationResponse getLocationById(@NotNull String id) {
        // Find the Product or throw if missing
        Location location = locationRepository.findById(id)
                            .orElseThrow(()-> new ResourceNotFoundException("Location not found: "+ id));

        return LocationMapper.toResponse(location);
    }

    @Transactional
    public LocationResponse updateLocation(@NotNull LocationCreateRequest request,
        @NotNull String id
    ){
        Location location = locationRepository.findById(id)
                                .orElseThrow(()-> new ResourceNotFoundException(id));

        // Update a Location
        location.setCode(request.code());
        location.setCapacity(request.capacity());

        Location saved = locationRepository.save(location);

        return LocationMapper.toResponse(saved);
    }

    @Transactional
    public void deleteLocation(@NotNull String id) {
        // Make sure the Location ID exists before attempting to delete
        if (!locationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Loacation ID not found: "+ id);
        }

        // Remove the location from the database
        locationRepository.deleteById(id);
    }

}
