package com.kfokam48.controller;

import com.kfokam48.dto.MovementDTO;
import com.kfokam48.dto.ProductDTO;
import com.kfokam48.dto.StockMovementDTO;
import com.kfokam48.service.ProductService;
import com.kfokam48.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST des mouvements de stock d'un produit.
 *
 * Entrée (/stock/in), sortie (/stock/out) et historique des mouvements.
 * Les deux premières renvoient le produit à jour.
 */
@RestController
@RequestMapping("/products/{productId}/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Gestion des mouvements de stock")
public class StockController {

    private final StockService stockService;
    private final ProductService productService;

    @PostMapping("/in")
    @Operation(summary = "Entrée de stock")
    public ResponseEntity<ProductDTO> stockIn(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementDTO dto) {
        // Renvoie le produit à jour : l'appelant avait besoin d'un second appel
        // pour connaître le stock résultant.
        return ResponseEntity.ok(productService.applyStockIn(productId, dto));
    }

    @PostMapping("/out")
    @Operation(summary = "Sortie de stock")
    public ResponseEntity<ProductDTO> stockOut(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementDTO dto) {
        return ResponseEntity.ok(productService.applyStockOut(productId, dto));
    }

    @GetMapping("/movements")
    @Operation(summary = "Historique des mouvements d'un produit")
    public ResponseEntity<List<MovementDTO>> getMovements(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getProductMovements(productId));
    }
}
