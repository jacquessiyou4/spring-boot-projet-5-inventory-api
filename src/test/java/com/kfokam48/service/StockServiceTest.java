package com.kfokam48.service;

import com.kfokam48.dto.StockMovementDTO;
import com.kfokam48.dto.MovementDTO;
import com.kfokam48.entity.Movement;
import com.kfokam48.entity.StockAlert;
import com.kfokam48.entity.Product;
import com.kfokam48.entity.Product.ProductStatus;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.MovementRepository;
import com.kfokam48.repository.ProductRepository;
import com.kfokam48.repository.StockAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private StockAlertRepository stockAlertRepository;

    @InjectMocks
    private StockService stockService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .sku("SKU001")
                .name("Test Product")
                .category("Electronics")
                .price(BigDecimal.valueOf(99.99))
                .currentStock(50)
                .minStockLevel(10)
                .maxStockLevel(200)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void stockIn_shouldIncreaseQuantity() {
        StockMovementDTO dto = new StockMovementDTO(20, "Restock", "PO-001");
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));

        stockService.stockIn(1L, dto);

        assertThat(sampleProduct.getCurrentStock()).isEqualTo(70);
        verify(productRepository).save(sampleProduct);
        verify(movementRepository).save(any(Movement.class));
    }

    @Test
    void stockIn_whenProductNotFound_shouldThrow() {
        StockMovementDTO dto = new StockMovementDTO(20, "Restock", null);
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.stockIn(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void stockOut_shouldDecreaseQuantity() {
        StockMovementDTO dto = new StockMovementDTO(10, "Sale", "ORD-001");
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));

        stockService.stockOut(1L, dto);

        assertThat(sampleProduct.getCurrentStock()).isEqualTo(40);
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void stockOut_whenInsufficientStock_shouldThrow() {
        StockMovementDTO dto = new StockMovementDTO(100, "Sale", null);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> stockService.stockOut(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void stockOut_whenLowStock_shouldCreateAlert() {
        sampleProduct.setCurrentStock(15);
        StockMovementDTO dto = new StockMovementDTO(10, "Sale", null);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));

        stockService.stockOut(1L, dto);

        assertThat(sampleProduct.getCurrentStock()).isEqualTo(5);
        verify(stockAlertRepository).save(any()); // low stock alert
    }

    @Test
    void stockOut_whenZeroStock_shouldCreateCriticalAlert() {
        sampleProduct.setCurrentStock(10);
        StockMovementDTO dto = new StockMovementDTO(10, "Sale", null);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));

        stockService.stockOut(1L, dto);

        assertThat(sampleProduct.getCurrentStock()).isEqualTo(0);
        verify(stockAlertRepository).save(any()); // out of stock alert
    }

    @Test
    void getProductMovements_shouldReturnList() {
        Movement mv = Movement.builder().id(1L).product(sampleProduct).quantity(10).build();
        when(movementRepository.findByProductIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(mv));

        when(productRepository.existsById(1L)).thenReturn(true);

        List<MovementDTO> result = stockService.getProductMovements(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductSku()).isEqualTo(sampleProduct.getSku());
    }

    /**
     * Régression : au passage de LOW_STOCK à OUT_OF_STOCK, l'alerte LOW_STOCK
     * restait ouverte indéfiniment et s'affichait en parallèle de l'alerte
     * de rupture dans /alerts.
     */
    @Test
    void stockOut_toZero_shouldResolvePreviousLowStockAlert() {
        sampleProduct.setCurrentStock(2);
        sampleProduct.setMinStockLevel(5);
        StockAlert lowStock = StockAlert.builder()
                .id(10L).product(sampleProduct)
                .alertType(StockAlert.AlertType.LOW_STOCK)
                .severity(StockAlert.AlertSeverity.WARNING)
                .resolved(false).build();

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockAlertRepository.findByProductIdAndResolvedFalse(1L))
                .thenReturn(List.of(lowStock));

        StockMovementDTO dto = new StockMovementDTO(2, "Vente", "REF-1");
        stockService.stockOut(1L, dto);

        assertThat(sampleProduct.getCurrentStock()).isZero();
        assertThat(lowStock.getResolved()).isTrue();
        assertThat(lowStock.getResolvedAt()).isNotNull();
    }

    @Test
    void stockIn_aboveThreshold_shouldResolveOpenAlerts() {
        sampleProduct.setCurrentStock(0);
        sampleProduct.setMinStockLevel(5);
        StockAlert outOfStock = StockAlert.builder()
                .id(11L).product(sampleProduct)
                .alertType(StockAlert.AlertType.OUT_OF_STOCK)
                .severity(StockAlert.AlertSeverity.CRITICAL)
                .resolved(false).build();

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockAlertRepository.findByProductIdAndResolvedFalse(1L))
                .thenReturn(List.of(outOfStock));

        stockService.stockIn(1L, new StockMovementDTO(50, "Réassort", "REF-2"));

        assertThat(sampleProduct.getCurrentStock()).isEqualTo(50);
        assertThat(outOfStock.getResolved()).isTrue();
    }
}
