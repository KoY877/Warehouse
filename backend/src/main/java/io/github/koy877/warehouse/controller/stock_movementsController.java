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

import io.github.koy877.warehouse.dto.Stock_movementsCreateRequest;
import io.github.koy877.warehouse.dto.Stock_movementsResponse;
import io.github.koy877.warehouse.service.Stock_movementsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock_movements")
public class stock_movementsController {

    private final Stock_movementsService stock_movementsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    public ResponseEntity<Stock_movementsResponse> createStockMovement(
            @RequestBody @Valid Stock_movementsCreateRequest request) {
        Stock_movementsResponse response = stock_movementsService.createStock_movements(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    public ResponseEntity<List<Stock_movementsResponse>> getAllProducts() {

        return ResponseEntity.ok(stock_movementsService.getAllStock_movements());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    public ResponseEntity<Stock_movementsResponse> getProductById(@PathVariable String id) {

        return ResponseEntity.ok(stock_movementsService.getStock_movementById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    public ResponseEntity<Stock_movementsResponse> updateProduct(@RequestBody @Valid Stock_movementsCreateRequest request,
            @PathVariable @NotNull String id) {
        Stock_movementsResponse response = stock_movementsService.updateStock_movement(request, id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable @NotNull String id) {
        // Delete the ticket then return an empty 204 response
        stock_movementsService.deleteStockMovement(id);
        
        return ResponseEntity.noContent().build();
    }

}
