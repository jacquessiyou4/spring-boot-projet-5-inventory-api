package com.kfokam48.dto;

import com.kfokam48.entity.StockAlert;
import com.kfokam48.entity.StockAlert.AlertSeverity;
import com.kfokam48.entity.StockAlert.AlertType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Voir MovementDTO : même raison d'être. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertDTO {
    private Long id;
    private Long productId;
    private String productSku;
    private AlertType alertType;
    private AlertSeverity severity;
    private String message;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;

    public static StockAlertDTO from(StockAlert alert) {
        return new StockAlertDTO(
                alert.getId(),
                alert.getProduct().getId(),
                alert.getProduct().getSku(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getResolved(),
                alert.getResolvedAt(),
                alert.getCreatedAt()
        );
    }
}
