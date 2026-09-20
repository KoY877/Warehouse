package io.github.koy877.warehouse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.koy877.warehouse.dto.Stock_movementsCreateRequest;
import io.github.koy877.warehouse.dto.Stock_movementsResponse;
import io.github.koy877.warehouse.entities.Location;
import io.github.koy877.warehouse.entities.Product;
import io.github.koy877.warehouse.entities.StockMovement;
import io.github.koy877.warehouse.entities.User;
import io.github.koy877.warehouse.entities.enums.MovementType;
import io.github.koy877.warehouse.entities.enums.Role;
import io.github.koy877.warehouse.exception.ConflictException;
import io.github.koy877.warehouse.exception.ResourceNotFoundException;
import io.github.koy877.warehouse.repositories.LocationRepository;
import io.github.koy877.warehouse.repositories.ProductRepository;
import io.github.koy877.warehouse.repositories.Stock_movementsRepository;
import io.github.koy877.warehouse.repositories.StocksRepository;
import io.github.koy877.warehouse.entities.Stocks;

@ExtendWith(MockitoExtension.class)
class StockMovementsServiceTest {

        @Mock
        private Stock_movementsRepository stockMovementsRepository;

        @Mock
        private ProductRepository productRepository;

        @Mock
        private LocationRepository locationRepository;

        @Mock
        private StocksRepository stocksRepository;

        @InjectMocks
        private Stock_movementsService stockMovementsService;

        private User user;

        @BeforeEach
        void setUp() {
                user = new User("firebase-uid-1", "test@warehouse.io", "Test User", Role.WAREHOUSE_OPERATOR);
                user.setId("user-1");
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void createStockMovements_inboundRejectsSourceLocation() {
                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-1",
                                "source-1",
                                null,
                                MovementType.INBOUND,
                                5);

                assertThatThrownBy(() -> stockMovementsService.createStock_movements(request))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("target location");

                verify(locationRepository, never()).findById(any());
        }

        @Test
        void createStockMovements_inboundAddsQuantityToTargetLocation() {
                Product product = new Product();
                product.setId("product-1");

                Location targetLocation = new Location();
                targetLocation.setId("target-1");

                Stocks existingStock = new Stocks();
                existingStock.setProduct(product);
                existingStock.setLocation(targetLocation);
                existingStock.setQuantity(2);

                when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
                when(locationRepository.findById("target-1")).thenReturn(Optional.of(targetLocation));
                when(stocksRepository.findByProductIdAndLocationId("product-1", "target-1"))
                                .thenReturn(Optional.of(existingStock));
                when(stocksRepository.save(any(Stocks.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(stockMovementsRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
                        StockMovement movement = invocation.getArgument(0);
                        movement.setId("movement-1");
                        return movement;
                });

                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-1",
                                null,
                                "target-1",
                                MovementType.INBOUND,
                                3);

                Stock_movementsResponse response = stockMovementsService.createStock_movements(request);

                assertThat(response).isNotNull();
                assertThat(response.type()).isEqualTo(MovementType.INBOUND);
                assertThat(existingStock.getQuantity()).isEqualTo(5);
                verify(stocksRepository).save(existingStock);
                verify(stockMovementsRepository).save(any(StockMovement.class));
        }

        @Test
        void createStockMovements_outboundAcceptsSourceOnly() {
                Product product = new Product();
                product.setId("product-1");

                Location sourceLocation = new Location();
                sourceLocation.setId("source-1");

                // Stock available: 10 units at the source location
                Stocks existingStock = new Stocks();
                existingStock.setProduct(product);
                existingStock.setLocation(sourceLocation);
                existingStock.setQuantity(10);

                when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
                when(locationRepository.findById("source-1")).thenReturn(Optional.of(sourceLocation));

                // StocksRepository mock: stock exists
                when(stocksRepository.findByProductIdAndLocationId("product-1", "source-1"))
                                .thenReturn(Optional.of(existingStock));

                // StocksRepository mock: stock is saved after the update
                when(stocksRepository.save(any(Stocks.class))).thenAnswer(invocation -> {
                        Stocks stock = invocation.getArgument(0);
                        stock.setQuantity(5); // 10 - 5 = 5
                        return stock;
                });

                when(stockMovementsRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
                        StockMovement movement = invocation.getArgument(0);
                        movement.setId("movement-1");
                        return movement;
                });

                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-1",
                                "source-1",
                                null,
                                MovementType.OUTBOUND,
                                5);

                Stock_movementsResponse response = stockMovementsService.createStock_movements(request);

                assertThat(response).isNotNull();
                assertThat(response.type()).isEqualTo(MovementType.OUTBOUND);

                // Verify: Stocks was checked and updated
                // findByProductIdAndLocationId is called twice: once in checkOutboundStock,
                // once in updateStock
                verify(stocksRepository, times(2)).findByProductIdAndLocationId("product-1", "source-1");
                verify(stocksRepository).save(any(Stocks.class));
                verify(stockMovementsRepository).save(any(StockMovement.class));
        }

        @Test
        void createStockMovements_transferRequiresSourceAndTarget() {
                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-1",
                                null,
                                "target-1",
                                MovementType.TRANSFER,
                                5);

                assertThatThrownBy(() -> stockMovementsService.createStock_movements(request))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("source and target");
        }

        @Test
        void createStockMovements_outboundThrowsConflictIfInsufficientStock() {
                Product product = new Product();
                product.setId("product-2");

                Location sourceLocation = new Location();
                sourceLocation.setId("source-2");

                Stocks existingStock = new Stocks();
                existingStock.setProduct(product);
                existingStock.setLocation(sourceLocation);
                existingStock.setQuantity(3); // Stock: 3

                when(productRepository.findById("product-2")).thenReturn(Optional.of(product));
                when(locationRepository.findById("source-2")).thenReturn(Optional.of(sourceLocation));

                when(stocksRepository.findByProductIdAndLocationId("product-2", "source-2"))
                                .thenReturn(Optional.of(existingStock));

                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-2",
                                "source-2",
                                null,
                                MovementType.OUTBOUND,
                                5); // Requests 5 → insufficient (stock = 3)

                // Verifies that a ConflictException is thrown
                assertThatThrownBy(() -> stockMovementsService.createStock_movements(request))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("Insufficient stock");
        }

        @Test
        void createStockMovements_transferThrowsConflictIfInsufficientSourceStock() {
                Product product = new Product();
                product.setId("product-3");

                Location sourceLocation = new Location();
                sourceLocation.setId("source-3");

                Location targetLocation = new Location();
                targetLocation.setId("target-3");

                Stocks existingStock = new Stocks();
                existingStock.setProduct(product);
                existingStock.setLocation(sourceLocation);
                existingStock.setQuantity(3);

                when(productRepository.findById("product-3")).thenReturn(Optional.of(product));
                when(locationRepository.findById("source-3")).thenReturn(Optional.of(sourceLocation));
                when(locationRepository.findById("target-3")).thenReturn(Optional.of(targetLocation));
                when(stocksRepository.findByProductIdAndLocationId("product-3", "source-3"))
                                .thenReturn(Optional.of(existingStock));

                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-3",
                                "source-3",
                                "target-3",
                                MovementType.TRANSFER,
                                5);

                assertThatThrownBy(() -> stockMovementsService.createStock_movements(request))
                        .isInstanceOf(ConflictException.class)
                        .hasMessageContaining("Insufficient stock");

                verify(stocksRepository, never()).save(any(Stocks.class));
                verify(stockMovementsRepository, never()).save(any(StockMovement.class));
        }

        @Test
        void updateStockMovements_inboundAddsQuantityToTargetLocation() {
                Product product = new Product();
                product.setId("product-1");

                Location targetLocation = new Location();
                targetLocation.setId("target-1");

                // The OLD movement being replaced: INBOUND of 5 units to target-1,
                // already applied. This is what updateStock_movement() must load via
                // stockMovementsRepository.findById(id) and reverse before booking the
                // new quantity.
                StockMovement existingMovement = new StockMovement();
                existingMovement.setId("movement-1");
                existingMovement.setProduct(product);
                existingMovement.setTargetLocation(targetLocation);
                existingMovement.setType(MovementType.INBOUND);
                existingMovement.setQuantity(5);

                // Current Stocks reflects that old movement: 5 units already booked in
                // at product-1/target-1.
                Stocks existingStock = new Stocks();
                existingStock.setProduct(product);
                existingStock.setLocation(targetLocation);
                existingStock.setQuantity(5);

                when(stockMovementsRepository.findById("movement-1"))
                                .thenReturn(Optional.of(existingMovement));
                when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
                when(locationRepository.findById("target-1")).thenReturn(Optional.of(targetLocation));
                when(stocksRepository.findByProductIdAndLocationId("product-1", "target-1"))
                                .thenReturn(Optional.of(existingStock));
                when(stocksRepository.save(any(Stocks.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(stockMovementsRepository.save(any(StockMovement.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // New request: same product/target location, quantity raised to 8.
                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-1", null, "target-1", MovementType.INBOUND, 8);

                Stock_movementsResponse response = stockMovementsService.updateStock_movement(request, "movement-1");

                assertThat(response).isNotNull();
                assertThat(response.quantity()).isEqualTo(8);
                // Reversal of the old booking: 5 - 5 = 0. New booking applied: 0 + 8 = 8.
                assertThat(existingStock.getQuantity()).isEqualTo(8);

                // findByProductIdAndLocationId/save are each hit twice on the same
                // Stocks row: once by reverseStockEffect(), once by the new booking.
                verify(stocksRepository, times(2)).findByProductIdAndLocationId("product-1", "target-1");
                verify(stocksRepository, times(2)).save(existingStock);

                // The SAME movement row is updated, not a new one inserted -- this is
                // the regression check for the original bug.
                verify(stockMovementsRepository).save(existingMovement);
        }

        @Test
        void updateStockMovements_throwsResourceNotFoundWhenMovementIdUnknown() {
                when(stockMovementsRepository.findById("unknown-id")).thenReturn(Optional.empty());

                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-1", null, "target-1", MovementType.INBOUND, 5);

                assertThatThrownBy(() -> stockMovementsService.updateStock_movement(request, "unknown-id"))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("unknown-id");

                // Nothing downstream of the missing movement should ever be touched:
                // no reversal, no re-resolution, no new booking.
                verify(productRepository, never()).findById(any());
                verify(locationRepository, never()).findById(any());
                verify(stocksRepository, never()).findByProductIdAndLocationId(any(), any());
                verify(stocksRepository, never()).save(any(Stocks.class));
                verify(stockMovementsRepository, never()).save(any(StockMovement.class));
        }

        @Test
        void updateStockMovements_typeChangeFromInboundToOutboundReversesAndRebooks() {
                Product product = new Product();
                product.setId("product-1");

                Location sourceLocation = new Location();
                sourceLocation.setId("source-1");

                Location targetLocation = new Location();
                targetLocation.setId("target-1");

                // The OLD movement: INBOUND of 5 units to target-1, already applied.
                StockMovement existingMovement = new StockMovement();
                existingMovement.setId("movement-2");
                existingMovement.setProduct(product);
                existingMovement.setTargetLocation(targetLocation);
                existingMovement.setType(MovementType.INBOUND);
                existingMovement.setQuantity(5);

                // target-1 currently holds exactly what that old INBOUND booked in.
                Stocks existingTargetStock = new Stocks();
                existingTargetStock.setProduct(product);
                existingTargetStock.setLocation(targetLocation);
                existingTargetStock.setQuantity(5);

                // source-1 has its own, independent stock, needed for the NEW
                // OUTBOUND leg and its insufficient-stock check.
                Stocks existingSourceStock = new Stocks();
                existingSourceStock.setProduct(product);
                existingSourceStock.setLocation(sourceLocation);
                existingSourceStock.setQuantity(10);

                when(stockMovementsRepository.findById("movement-2"))
                                .thenReturn(Optional.of(existingMovement));
                when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
                when(locationRepository.findById("source-1")).thenReturn(Optional.of(sourceLocation));
                when(stocksRepository.findByProductIdAndLocationId("product-1", "target-1"))
                                .thenReturn(Optional.of(existingTargetStock));
                when(stocksRepository.findByProductIdAndLocationId("product-1", "source-1"))
                                .thenReturn(Optional.of(existingSourceStock));
                when(stocksRepository.save(any(Stocks.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(stockMovementsRepository.save(any(StockMovement.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // New request: same product, now OUTBOUND of 2 units FROM source-1
                // (no target location for an OUTBOUND).
                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-1", "source-1", null, MovementType.OUTBOUND, 2);

                Stock_movementsResponse response = stockMovementsService.updateStock_movement(request, "movement-2");

                assertThat(response).isNotNull();
                assertThat(response.type()).isEqualTo(MovementType.OUTBOUND);
                assertThat(response.quantity()).isEqualTo(2);

                // Reversal of the old INBOUND leg: 5 - 5 = 0. The new OUTBOUND leg
                // never touches target-1 again.
                assertThat(existingTargetStock.getQuantity()).isEqualTo(0);
                // New OUTBOUND leg applied at source-1: 10 - 2 = 8.
                assertThat(existingSourceStock.getQuantity()).isEqualTo(8);

                // Reversal only ever needs target-1 once.
                verify(stocksRepository, times(1)).findByProductIdAndLocationId("product-1", "target-1");
                // source-1 is read twice: once by checkOutboundStock(), once by the
                // new booking's updateStock().
                verify(stocksRepository, times(2)).findByProductIdAndLocationId("product-1", "source-1");

                // Same movement row updated in place, now carrying the new type/quantity.
                verify(stockMovementsRepository).save(existingMovement);
        }

}
