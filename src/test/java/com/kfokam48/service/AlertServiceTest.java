package com.kfokam48.service;

import com.kfokam48.dto.StockAlertDTO;
import com.kfokam48.entity.Product;
import com.kfokam48.entity.StockAlert;
import com.kfokam48.entity.StockAlert.AlertSeverity;
import com.kfokam48.entity.StockAlert.AlertType;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.StockAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private StockAlertRepository stockAlertRepository;

    @InjectMocks
    private AlertService alertService;

    private StockAlert sampleAlert;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder().id(1L).sku("SKU-001").name("Test Product").build();
        sampleAlert = StockAlert.builder()
                .id(1L)
                .alertType(AlertType.LOW_STOCK)
                .severity(AlertSeverity.WARNING)
                .message("Low stock warning")
                .resolved(false)
                .product(sampleProduct)
                .build();
    }

    @Test
    void getAllAlerts_shouldReturnList() {
        when(stockAlertRepository.findByResolvedFalseOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(sampleAlert));

        List<StockAlertDTO> result = alertService.getAllAlerts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAlertType()).isEqualTo(AlertType.LOW_STOCK);
    }

    @Test
    void getCriticalAlerts_shouldReturnList() {
        StockAlert critical = StockAlert.builder()
                .id(2L).alertType(AlertType.OUT_OF_STOCK).severity(AlertSeverity.CRITICAL)
                .resolved(false).product(sampleProduct).build();
        when(stockAlertRepository.findBySeverityAndResolvedFalse(AlertSeverity.CRITICAL))
                .thenReturn(Arrays.asList(critical));

        List<StockAlertDTO> result = alertService.getCriticalAlerts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void getLowStockAlerts_shouldReturnList() {
        when(stockAlertRepository.findByAlertTypeAndResolvedFalse(AlertType.LOW_STOCK))
                .thenReturn(Arrays.asList(sampleAlert));

        List<StockAlertDTO> result = alertService.getLowStockAlerts();

        assertThat(result).hasSize(1);
    }

    @Test
    void resolveAlert_shouldMarkResolved() {
        when(stockAlertRepository.findById(1L)).thenReturn(Optional.of(sampleAlert));
        when(stockAlertRepository.save(any(StockAlert.class))).thenReturn(sampleAlert);

        StockAlertDTO result = alertService.resolveAlert(1L);

        assertThat(result.getResolved()).isTrue();
        assertThat(result.getResolvedAt()).isNotNull();
        verify(stockAlertRepository).save(any(StockAlert.class));
    }

    @Test
    void resolveAlert_whenNotExists_shouldThrow() {
        when(stockAlertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.resolveAlert(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
