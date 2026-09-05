package io.github.koy877.warehouse.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.koy877.warehouse.dto.LocationCreateRequest;
import io.github.koy877.warehouse.dto.LocationResponse;
import io.github.koy877.warehouse.service.LocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN'), 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<LocationResponse> createLocatiion (@RequestBody @Valid LocationCreateRequest request) {
        LocationResponse response = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
     
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<List<LocationResponse>> getAllProducts () {
        
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<LocationResponse> getLocationById (@PathVariable String id) {
        
        return ResponseEntity.ok(locationService.getLocationById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN'), 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<LocationResponse> updateLocatiion (@RequestBody @Valid LocationCreateRequest request, @NotBlank String id) {
        LocationResponse response = locationService.updateLocation(request, id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN'), 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<Void> deleteLocatiion (@PathVariable @NotNull String id) {
       // Delete the ticket then return an empty 204 response
        locationService.deleteLocation(id);;
        return ResponseEntity.noContent().build();
    }
}
