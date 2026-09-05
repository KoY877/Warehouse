package io.github.koy877.warehouse.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import io.github.koy877.warehouse.dto.Stock_MovementsMapper;
import io.github.koy877.warehouse.dto.Stock_movementsCreateRequest;
import io.github.koy877.warehouse.dto.Stock_movementsResponse;
import io.github.koy877.warehouse.entities.Location;
import io.github.koy877.warehouse.entities.Product;
import io.github.koy877.warehouse.entities.StockMovement;
import io.github.koy877.warehouse.entities.User;
import io.github.koy877.warehouse.entities.enums.MovementType;
import io.github.koy877.warehouse.exception.ConflictException;
import io.github.koy877.warehouse.exception.ResourceNotFoundException;
import io.github.koy877.warehouse.repositories.LocationRepository;
import io.github.koy877.warehouse.repositories.ProductRepository;
import io.github.koy877.warehouse.repositories.Stock_movementsRepository;
import io.github.koy877.warehouse.repositories.StocksRepository;
import io.github.koy877.warehouse.entities.Stocks;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class Stock_movementsService {

        private final Stock_movementsRepository stock_movementsRepository;
        private final ProductRepository productRepository;
        private final LocationRepository locationRepository;
        private final StocksRepository stocksRepository;

        /**
         * Creates a new stock movement (INBOUND, OUTBOUND or TRANSFER) and applies
         * its effect on the current inventory.
         *
         * EXPLANATION:
         * 1. Validates that the request shape (source/target location) matches the
         * movement type.
         * 2. Resolves Product, source Location and target Location from the
         * database.
         * 3. Resolves the currently authenticated user from the SecurityContext.
         * 4. For OUTBOUND and TRANSFER, checks that enough stock is available at
         * the source location before touching anything.
         * 5. Applies the inventory change depending on the movement type.
         * 6. Persists the movement itself as an audit/history record.
         */
        @Transactional
        public Stock_movementsResponse createStock_movements(@Valid @NotNull Stock_movementsCreateRequest request) {
                // Reject requests where source/target locations don't match the movement
                // type (e.g. INBOUND with a source location).
                validateMovementRequest(request);

                Product product = productRepository.findById(request.productId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Product not found: " + request.productId()));

                // Source location is only present for OUTBOUND and TRANSFER.
                Location sourceLocation = null;
                if (request.sourceLocation() != null) {
                        sourceLocation = locationRepository.findById(request.sourceLocation())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Source location not found: " + request.sourceLocation()));
                }

                // Target location is only present for INBOUND and TRANSFER.
                Location targetLocation = null;
                if (request.targetLocation() != null) {
                        targetLocation = locationRepository.findById(request.targetLocation())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Target location not found: " + request.targetLocation()));
                }

                // The movement must always be attributed to the currently authenticated
                // user; this is read from Spring Security's context, never from the
                // request body, to avoid mass assignment of the "user" field.
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
                        throw new IllegalStateException("No authenticated user found in SecurityContext");
                }

                // Step 1: For OUTBOUND and TRANSFER, check that enough source stock is
                // available. Both movement types subtract from the source location, so
                // both must be guarded against negative stock.
                if (request.type() == MovementType.OUTBOUND || request.type() == MovementType.TRANSFER) {
                        checkOutboundStock(product, sourceLocation, request.quantity());
                }

                // Step 2: Apply the inventory booking depending on the movement type.
                switch (request.type()) {
                        case INBOUND:
                                // INBOUND: quantity is ADDED at the target location.
                                updateStock(product, targetLocation, request.quantity());
                                break;
                        case OUTBOUND:
                                // OUTBOUND: quantity is SUBTRACTED from the source location.
                                updateStock(product, sourceLocation, -request.quantity());
                                break;
                        case TRANSFER:
                                // TRANSFER: first leaves the source location, then arrives at the
                                // target location.
                                updateStock(product, sourceLocation, -request.quantity());
                                updateStock(product, targetLocation, request.quantity());
                                break;
                }

                // Step 3: Persist the movement itself (for audit/history), independently
                // of the current stock level.
                StockMovement stockMovement = new StockMovement();
                stockMovement.setProduct(product);
                stockMovement.setSourceLocation(sourceLocation);
                stockMovement.setTargetLocation(targetLocation);
                stockMovement.setUser(user);
                stockMovement.setType(request.type());
                stockMovement.setQuantity(request.quantity());

                StockMovement saved = stock_movementsRepository.save(stockMovement);

                return Stock_MovementsMapper.toResponse(saved);
        }

        /**
         * Returns every stock movement, mapped to its response DTO.
         *
         * EXPLANATION: Entities are never returned directly from the service; each
         * StockMovement is converted through Stock_MovementsMapper.
         */
        @Transactional
        public List<Stock_movementsResponse> getAllStock_movements() {
                // Load all Stock_movements and map each one to its response DTO.
                return stock_movementsRepository.findAll()
                                .stream()
                                .map(Stock_MovementsMapper::toResponse)
                                .toList();
        }

        /**
         * Returns a single stock movement by id.
         *
         * EXPLANATION: Throws ResourceNotFoundException (404) if no movement exists
         * for the given id.
         */
        @Transactional
        public Stock_movementsResponse getStock_movementById(@NotNull String id) {
                // Find the Stock movement or throw if missing.
                StockMovement stockMovement = stock_movementsRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("StockMovement not found: " + id));

                return Stock_MovementsMapper.toResponse(stockMovement);
        }

        /**
         * Updates an existing stock movement record in place.
         *
         * EXPLANATION:
         * 1. Validates the new request shape, same as creation.
         * 2. Loads the existing movement or throws 404.
         * 3. Re-resolves Product/Location/User exactly like createStock_movements().
         * 4. Overwrites the movement fields and saves.
         *
         * WARNING: unlike createStock_movements(), this method does NOT touch the
         * Stocks table. Editing a movement's quantity/type/locations here does not
         * reverse the old booking or apply the new one, so Stocks can drift out of
         * sync with the movement history. See Priority 2 in the project notes.
         */
        @Transactional
        public Stock_movementsResponse updateStock_movement(@NotNull Stock_movementsCreateRequest request,
                        @NotNull String id) {
                                
                // Reject requests where source/target locations don't match the movement
                // type (e.g. INBOUND with a source location).
                validateMovementRequest(request);

                Product product = productRepository.findById(request.productId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Product not found: " + request.productId()));

                // Source location is only present for OUTBOUND and TRANSFER.
                Location sourceLocation = null;
                if (request.sourceLocation() != null) {
                        sourceLocation = locationRepository.findById(request.sourceLocation())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Source location not found: " + request.sourceLocation()));
                }

                // Target location is only present for INBOUND and TRANSFER.
                Location targetLocation = null;
                if (request.targetLocation() != null) {
                        targetLocation = locationRepository.findById(request.targetLocation())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Target location not found: " + request.targetLocation()));
                }

                // The movement must always be attributed to the currently authenticated
                // user; this is read from Spring Security's context, never from the
                // request body, to avoid mass assignment of the "user" field.
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
                        throw new IllegalStateException("No authenticated user found in SecurityContext");
                }

                // Step 1: For OUTBOUND and TRANSFER, check that enough source stock is
                // available. Both movement types subtract from the source location, so
                // both must be guarded against negative stock.
                if (request.type() == MovementType.OUTBOUND || request.type() == MovementType.TRANSFER) {
                        checkOutboundStock(product, sourceLocation, request.quantity());
                }

                // Step 2: Apply the inventory booking depending on the movement type.
                switch (request.type()) {
                        case INBOUND:
                                // INBOUND: quantity is ADDED at the target location.
                                updateStock(product, targetLocation, request.quantity());
                                break;
                        case OUTBOUND:
                                // OUTBOUND: quantity is SUBTRACTED from the source location.
                                updateStock(product, sourceLocation, -request.quantity());
                                break;
                        case TRANSFER:
                                // TRANSFER: first leaves the source location, then arrives at the
                                // target location.
                                updateStock(product, sourceLocation, -request.quantity());
                                updateStock(product, targetLocation, request.quantity());
                                break;
                }

                // Step 3: Persist the movement itself (for audit/history), independently
                // of the current stock level.
                StockMovement stockMovement = new StockMovement();
                stockMovement.setProduct(product);
                stockMovement.setSourceLocation(sourceLocation);
                stockMovement.setTargetLocation(targetLocation);
                stockMovement.setUser(user);
                stockMovement.setType(request.type());
                stockMovement.setQuantity(request.quantity());

                StockMovement saved = stock_movementsRepository.save(stockMovement);

                return Stock_MovementsMapper.toResponse(saved);
        }

        /**
         * Validates that source/target locations match the movement type.
         *
         * EXPLANATION:
         * - INBOUND requires a target location only (no source).
         * - OUTBOUND requires a source location only (no target).
         * - TRANSFER requires both source and target locations.
         * Throws ConflictException (409) when the shape doesn't match.
         */
        private void validateMovementRequest(Stock_movementsCreateRequest request) {
                MovementType type = request.type();

                if (type == MovementType.INBOUND) {
                        // INBOUND must not carry a source location, and must carry a target.
                        if (request.sourceLocation() != null || request.targetLocation() == null) {
                                throw new ConflictException("INBOUND requires only a target location");
                        }
                }

                if (type == MovementType.OUTBOUND) {
                        // OUTBOUND must not carry a target location, and must carry a source.
                        if (request.sourceLocation() == null || request.targetLocation() != null) {
                                throw new ConflictException("OUTBOUND requires only a source location");
                        }
                }

                if (type == MovementType.TRANSFER) {
                        // TRANSFER must carry both a source and a target location.
                        if (request.sourceLocation() == null || request.targetLocation() == null) {
                                throw new ConflictException("TRANSFER requires source and target locations");
                        }
                }
        }

        /**
         * Deletes a stock movement record by id.
         *
         * WARNING: this only removes the audit/history row. It does not reverse the
         * inventory change the movement originally applied, so Stocks can drift out
         * of sync with the movement history. See Priority 2 in the project notes.
         */
        @Transactional
        public void deleteStockMovement(@NotNull String id) {
                // Make sure the Product ID exists before attempting to delete.
                if (!stock_movementsRepository.existsById(id)) {
                        throw new ResourceNotFoundException("Stock movement ID not found: " + id);
                }

                // Remove the Product from the database.
                stock_movementsRepository.deleteById(id);
        }

        /**
         * Checks whether enough stock is available for an OUTBOUND (or the
         * outbound leg of a TRANSFER). Throws 409 Conflict if the source stock is
         * lower than the requested quantity.
         *
         * EXPLANATION:
         * 1. Loads the Stock record for the Product + Location combination.
         * 2. If no record exists, treats the available quantity as 0.
         * 3. Compares available quantity against the requested quantity.
         * 4. Throws 409 Conflict when not enough stock is available.
         */
        private void checkOutboundStock(Product product, Location sourceLocation, Integer requestedQuantity) {
                // Look up the current stock row; absent means nothing has ever been
                // booked in at this Product + Location.
                Stocks currentStock = stocksRepository
                                .findByProductIdAndLocationId(product.getId(), sourceLocation.getId())
                                .orElse(null);

                int availableQuantity = currentStock != null ? currentStock.getQuantity() : 0;

                if (availableQuantity < requestedQuantity) {
                        throw new ConflictException(
                                        "Insufficient stock. Available: " + availableQuantity +
                                                        ", Requested: " + requestedQuantity);
                }
        }

        /**
         * Updates the stock quantity for a Product at a Location.
         *
         * EXPLANATION:
         * 1. Looks up the Stock record via findByProductIdAndLocationId()
         * (unique constraint on (product_id, location_id)).
         * 2. If none exists, creates a new Stock with quantity = 0 as the base.
         * 3. Adds quantityChange to the current quantity
         * (e.g. +10 for an inbound, -5 for an outbound).
         * 4. Persists the updated Stock back to the database.
         *
         * TRANSACTIONAL: this method is always called from within
         * createStock_movements(), which is annotated with @Transactional. All
         * changes are committed or rolled back together as one unit.
         */
        private void updateStock(Product product, Location location, Integer quantityChange) {
                // Find the existing Stock row, or build a fresh one starting at 0 if this
                // Product has never been booked at this Location before.
                Stocks stock = stocksRepository
                                .findByProductIdAndLocationId(product.getId(), location.getId())
                                .orElseGet(() -> {
                                        Stocks newStock = new Stocks();
                                        newStock.setProduct(product);
                                        newStock.setLocation(location);
                                        newStock.setQuantity(0);
                                        return newStock;
                                });

                // Apply the delta (positive for inbound, negative for outbound) and save.
                stock.setQuantity(stock.getQuantity() + quantityChange);
                stocksRepository.save(stock);

                log.info("Stock updated: Product={}, Location={}, NewQuantity={}",
                                product.getId(), location.getId(), stock.getQuantity());
        }
}
