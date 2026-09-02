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

        @Transactional
        public Stock_movementsResponse createStock_movements(@Valid @NotNull Stock_movementsCreateRequest request) {
                validateMovementRequest(request);

                Product product = productRepository.findById(request.productId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Product not found: " + request.productId()));

                Location sourceLocation = null;
                if (request.sourceLocation() != null) {
                        sourceLocation = locationRepository.findById(request.sourceLocation())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Source location not found: " + request.sourceLocation()));
                }

                Location targetLocation = null;
                if (request.targetLocation() != null) {
                        targetLocation = locationRepository.findById(request.targetLocation())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Target location not found: " + request.targetLocation()));
                }

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
                        throw new IllegalStateException("No authenticated user found in SecurityContext");
                }

                // Schritt 1: Für OUTBOUND und TRANSFER prüfen, ob genug Quellbestand vorhanden
                // ist
                if (request.type() == MovementType.OUTBOUND || request.type() == MovementType.TRANSFER) {
                        checkOutboundStock(product, sourceLocation, request.quantity());
                }

                // Schritt 2: Bestandsbuchung durchführen je nach Bewegungstyp
                switch (request.type()) {
                        case INBOUND:
                                // INBOUND: Quantity wird zu Zielort ADDIERT
                                updateStock(product, targetLocation, request.quantity());
                                break;
                        case OUTBOUND:
                                // OUTBOUND: Quantity wird von Quellort SUBTRAHIERT
                                updateStock(product, sourceLocation, -request.quantity());
                                break;
                        case TRANSFER:
                                // TRANSFER: Zuerst vom Quellort weg, dann zum Zielort hin
                                updateStock(product, sourceLocation, -request.quantity());
                                updateStock(product, targetLocation, request.quantity());
                                break;
                }

                // Schritt 3: Bewegung speichern (für Audit/History)
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

        @Transactional
        public List<Stock_movementsResponse> getAllStock_movements() {
                // Load all Stock_movements and map each one to its response DTO
                return stock_movementsRepository.findAll()
                                .stream()
                                .map(Stock_MovementsMapper::toResponse)
                                .toList();
        }

        @Transactional
        public Stock_movementsResponse getStock_movementById(@NotNull String id) {
                // Find the Stock movement or throw if missing
                StockMovement stockMovement = stock_movementsRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("StockMovement not found: " + id));

                return Stock_MovementsMapper.toResponse(stockMovement);
        }

        @Transactional
        public Stock_movementsResponse updateStock_movement(@NotNull Stock_movementsCreateRequest request,
                        @NotNull String id) {
                validateMovementRequest(request);

                StockMovement stockMovement = stock_movementsRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Stock movement not found"));

                Product product = productRepository.findById(request.productId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Product not found: " + request.productId()));

                Location sourceLocation = null;
                if (request.sourceLocation() != null) {
                        sourceLocation = locationRepository.findById(request.sourceLocation())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Source location not found: " + request.sourceLocation()));
                }

                Location targetLocation = null;
                if (request.targetLocation() != null) {
                        targetLocation = locationRepository.findById(request.targetLocation())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Target location not found: " + request.targetLocation()));
                }

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
                        throw new IllegalStateException("No authenticated user found in SecurityContext");
                }

                stockMovement.setProduct(product);
                stockMovement.setSourceLocation(sourceLocation);
                stockMovement.setTargetLocation(targetLocation);
                stockMovement.setUser(user);
                stockMovement.setType(request.type());
                stockMovement.setQuantity(request.quantity());

                StockMovement saved = stock_movementsRepository.save(stockMovement);

                return Stock_MovementsMapper.toResponse(saved);
        }

        private void validateMovementRequest(Stock_movementsCreateRequest request) {
                MovementType type = request.type();

                if (type == MovementType.INBOUND) {
                        if (request.sourceLocation() != null || request.targetLocation() == null) {
                                throw new ConflictException("INBOUND requires only a target location");
                        }
                }

                if (type == MovementType.OUTBOUND) {
                        if (request.sourceLocation() == null || request.targetLocation() != null) {
                                throw new ConflictException("OUTBOUND requires only a source location");
                        }
                }

                if (type == MovementType.TRANSFER) {
                        if (request.sourceLocation() == null || request.targetLocation() == null) {
                                throw new ConflictException("TRANSFER requires source and target locations");
                        }
                }
        }

        @Transactional
        public void deleteStockMovement(@NotNull String id) {
                // Make sure the Product ID exists before attempting to delete
                if (!stock_movementsRepository.existsById(id)) {
                        throw new ResourceNotFoundException("Stock movement ID not found: " + id);
                }

                // Remove the Product from the database
                stock_movementsRepository.deleteById(id);
        }

        /**
         * Prüft, ob ausreichend Bestand für OUTBOUND vorhanden ist.
         * Wirft 409 Conflict, wenn Quellbestand < gewünschte Menge.
         * 
         * ERKLÄRUNG:
         * 1. Lädt den Stock-Record für die Kombination Product + Location
         * 2. Falls kein Record existiert, nimmt die Methode Bestand = 0 an
         * 3. Vergleicht verfügbaren Bestand mit angefordeter Menge
         * 4. Wirft 409 Conflict, wenn zu wenig vorhanden ist
         */
        private void checkOutboundStock(Product product, Location sourceLocation, Integer requestedQuantity) {
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
         * Aktualisiert den Bestand für ein Produkt an einem Lagerort.
         * 
         * ERKLÄRUNG der Logik:
         * 1. Suche Bestandsrecord via findByProductIdAndLocationId()
         * (unique constraint auf (product_id, location_id))
         * 2. Falls nicht vorhanden, erstelle einen neuen Stock mit quantity = 0
         * 3. Addiere quantityChange zur aktuellen Quantity
         * (z.B. +10 für Zugang, -5 für Abgang)
         * 4. Speichere den aktualisierten Stock zurück in DB
         * 
         * TRANSACTIONAL: Diese Methode wird immer innerhalb von createStock_movements()
         * aufgerufen, die mit @Transactional annotiert ist. Alle Änderungen werden
         * atomare zusammen committed oder rolled back.
         */

        private void updateStock(Product product, Location location, Integer quantityChange) {
                Stocks stock = stocksRepository
                                .findByProductIdAndLocationId(product.getId(), location.getId())
                                .orElseGet(() -> {
                                        Stocks newStock = new Stocks();
                                        newStock.setProduct(product);
                                        newStock.setLocation(location);
                                        newStock.setQuantity(0);
                                        return newStock;
                                });

                stock.setQuantity(stock.getQuantity() + quantityChange);
                stocksRepository.save(stock);

                log.info("Stock updated: Product={}, Location={}, NewQuantity={}",
                                product.getId(), location.getId(), stock.getQuantity());
        }
}
