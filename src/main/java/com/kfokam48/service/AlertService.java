package com.kfokam48.service;

import com.kfokam48.dto.StockAlertDTO;
import com.kfokam48.entity.StockAlert;
import com.kfokam48.entity.StockAlert.AlertSeverity;
import com.kfokam48.entity.StockAlert.AlertType;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.StockAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Couche Service : consultation et résolution manuelle des alertes de stock.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final StockAlertRepository stockAlertRepository;

    @Transactional(readOnly = true)
    public List<StockAlertDTO> getAllAlerts() {
        return stockAlertRepository.findByResolvedFalseOrderByCreatedAtDesc()
                .stream().map(StockAlertDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StockAlertDTO> getCriticalAlerts() {
        return stockAlertRepository.findBySeverityAndResolvedFalse(AlertSeverity.CRITICAL)
                .stream().map(StockAlertDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StockAlertDTO> getLowStockAlerts() {
        return stockAlertRepository.findByAlertTypeAndResolvedFalse(AlertType.LOW_STOCK)
                .stream().map(StockAlertDTO::from).toList();
    }

    @Transactional
    public StockAlertDTO resolveAlert(Long alertId) {
        StockAlert alert = stockAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        log.info("Alert {} resolved", alertId);
        return StockAlertDTO.from(stockAlertRepository.save(alert));
    }
}
