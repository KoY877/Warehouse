package io.github.koy877.warehouse.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.koy877.warehouse.dto.ProductCreateRequest;
import io.github.koy877.warehouse.dto.ProductResponse;
import io.github.koy877.warehouse.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {

        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN'), 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<ProductResponse> updateProduct(@RequestBody @Valid ProductCreateRequest request,
            @PathVariable @NotNull String id) {
        ProductResponse response = productService.updateProduct(request, id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN'), 'WAREHOUSE_OPERATOR')")
    public ResponseEntity<Void> deleteProduct(@PathVariable @NotNull String id) {
        // Delete the ticket then return an empty 204 response
        productService.deleteProduct(id);
        ;
        return ResponseEntity.noContent().build();
    }
}
