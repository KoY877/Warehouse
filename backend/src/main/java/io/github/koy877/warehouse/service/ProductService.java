package io.github.koy877.warehouse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import io.github.koy877.warehouse.dto.LocationCreateRequest;
import io.github.koy877.warehouse.dto.LocationMapper;
import io.github.koy877.warehouse.dto.LocationResponse;
import io.github.koy877.warehouse.dto.ProductCreateRequest;
import io.github.koy877.warehouse.dto.ProductMapper;
import io.github.koy877.warehouse.dto.ProductResponse;
import io.github.koy877.warehouse.entities.Product;
import io.github.koy877.warehouse.exception.ConflictException;
import io.github.koy877.warehouse.exception.ResourceNotFoundException;
import io.github.koy877.warehouse.repositories.ProductRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(@NotNull ProductCreateRequest request){

        // Check if the SKU exists        
        boolean skuExists = productRepository.existsBySku(request.sku());
           
        if (skuExists) {
            log.warn("Product SKU already exists: {}", request.sku());
            throw new ConflictException("SKU already exists");
        }

        // Create a Product
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setMinStock(request.minStock());
        product.setUnit(request.unit());
        product.setActive(true);
        
        Product saved = productRepository.save(product);
        
        log.info("Product created successfully with SKU {}", saved.getSku());
        return ProductMapper.toResponse(saved);
    }

    @Transactional
    public List<ProductResponse> getAllProducts(){
        // Load all Products and map each one to its response DTO
        return productRepository.findAll()
                    .stream()
                    .map(ProductMapper::toResponse)
                    .toList();
    }

    @Transactional
    public ProductResponse getProductById(@NotNull String id) {
        // Find the Product or throw if missing
        Product product = productRepository.findById(id)
                            .orElseThrow(()-> new ResourceNotFoundException("Product not found: "+ id));

        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(@NotNull ProductCreateRequest request,
        @NotNull String id
    ){
        Product product = productRepository.findById(id)
                                .orElseThrow(()-> new ResourceNotFoundException("Product not found"));

        // Update a Location
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setMinStock(request.minStock());
        product.setUnit(request.unit());
        product.setActive(true);
        
        Product saved = productRepository.save(product);
        

        return ProductMapper.toResponse(saved);
    }

    @Transactional
    public void deleteProduct(@NotNull String id) {
        // Make sure the Product ID exists before attempting to delete
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product ID not found: "+ id);
        }

        // Remove the Product from the database
        productRepository.deleteById(id);
    }
}
