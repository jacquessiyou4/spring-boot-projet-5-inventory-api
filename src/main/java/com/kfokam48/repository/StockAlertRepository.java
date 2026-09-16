package com.kfokam48.repository;

import com.kfokam48.entity.StockAlert;
import com.kfokam48.entity.StockAlert.AlertSeverity;
import com.kfokam48.entity.StockAlert.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Accès aux alertes de stock, filtrées par produit, type, gravité ou état.
 */
@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findByResolvedFalseOrderByCreatedAtDesc();
    List<StockAlert> findBySeverityAndResolvedFalse(AlertSeverity severity);
    List<StockAlert> findByAlertTypeAndResolvedFalse(AlertType alertType);
    List<StockAlert> findByProductIdAndResolvedFalse(Long productId);
}
