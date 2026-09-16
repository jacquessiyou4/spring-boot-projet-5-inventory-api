package com.kfokam48.controller;

import com.kfokam48.dto.StockAlertDTO;
import com.kfokam48.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST des alertes de stock.
 *
 * Consultation des alertes actives (toutes, critiques, stock bas) et résolution
 * manuelle d'une alerte.
 */
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Stock Alerts", description = "Alertes de stock")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Récupérer toutes les alertes actives")
    public ResponseEntity<List<StockAlertDTO>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/critical")
    @Operation(summary = "Récupérer les alertes critiques")
    public ResponseEntity<List<StockAlertDTO>> getCriticalAlerts() {
        return ResponseEntity.ok(alertService.getCriticalAlerts());
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Récupérer les alertes de stock bas")
    public ResponseEntity<List<StockAlertDTO>> getLowStockAlerts() {
        return ResponseEntity.ok(alertService.getLowStockAlerts());
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Résoudre une alerte")
    public ResponseEntity<StockAlertDTO> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }
}
