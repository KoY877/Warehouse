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
        void createStockMovements_outboundAcceptsSourceOnly() {
                Product product = new Product();
                product.setId("product-1");

                Location sourceLocation = new Location();
                sourceLocation.setId("source-1");

                // Bestand vorhanden: 10 Stück an Quellort
                Stocks existingStock = new Stocks();
                existingStock.setProduct(product);
                existingStock.setLocation(sourceLocation);
                existingStock.setQuantity(10);

                when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
                when(locationRepository.findById("source-1")).thenReturn(Optional.of(sourceLocation));

                // StocksRepository Mock: Bestand existiert
                when(stocksRepository.findByProductIdAndLocationId("product-1", "source-1"))
                                .thenReturn(Optional.of(existingStock));

                // StocksRepository Mock: Nach Update wird Bestand gespeichert
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

                // Verifiziere: Stocks wurde geprüft und aktualisiert
                // findByProductIdAndLocationId wird 2x aufgerufen: 1x in checkOutboundStock, 1x
                // in updateStock
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
                existingStock.setQuantity(3); // Bestand: 3

                when(productRepository.findById("product-2")).thenReturn(Optional.of(product));
                when(locationRepository.findById("source-2")).thenReturn(Optional.of(sourceLocation));

                when(stocksRepository.findByProductIdAndLocationId("product-2", "source-2"))
                                .thenReturn(Optional.of(existingStock));

                Stock_movementsCreateRequest request = new Stock_movementsCreateRequest(
                                "product-2",
                                "source-2",
                                null,
                                MovementType.OUTBOUND,
                                5); // Demande 5 → insuffisant (bestand = 3)

                // Test pour exception ConflictException
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

}
