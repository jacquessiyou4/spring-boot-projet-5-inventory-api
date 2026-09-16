package com.kfokam48.service;

import com.kfokam48.dto.MovementDTO;
import com.kfokam48.dto.StockMovementDTO;
import com.kfokam48.entity.Movement;
import com.kfokam48.entity.Movement.MovementType;
import com.kfokam48.entity.Product;
import com.kfokam48.entity.StockAlert;
import com.kfokam48.entity.StockAlert.AlertSeverity;
import com.kfokam48.entity.StockAlert.AlertType;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.MovementRepository;
import com.kfokam48.repository.ProductRepository;
import com.kfokam48.repository.StockAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Couche Service : logique métier des mouvements de stock.
 *
 * Applique les entrées et sorties sous verrou pessimiste pour éviter la
 * survente, journalise chaque mouvement, et crée ou résout les alertes selon
 * le niveau de stock atteint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final ProductRepository productRepository;
    private final MovementRepository movementRepository;
    private final StockAlertRepository stockAlertRepository;

    @Transactional
    public Product stockIn(Long productId, StockMovementDTO dto) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int before = product.getCurrentStock();
        product.setCurrentStock(before + dto.getQuantity());
        productRepository.save(product);

        saveMovement(product, MovementType.IN, dto.getQuantity(), before,
                product.getCurrentStock(), dto.getReason(), dto.getReference());

        evaluateStock(product);
        log.info("Stock IN: {} units for product {}", dto.getQuantity(), productId);
        return product;
    }

    @Transactional
    public Product stockOut(Long productId, StockMovementDTO dto) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getCurrentStock() < dto.getQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: " + product.getCurrentStock() + ", Requested: " + dto.getQuantity());
        }

        int before = product.getCurrentStock();
        product.setCurrentStock(before - dto.getQuantity());
        productRepository.save(product);

        saveMovement(product, MovementType.OUT, dto.getQuantity(), before,
                product.getCurrentStock(), dto.getReason(), dto.getReference());

        evaluateStock(product);
        log.info("Stock OUT: {} units for product {}", dto.getQuantity(), productId);
        return product;
    }

    /** Fixe le stock à une valeur cible en traçant l'écart comme mouvement ADJUSTMENT. */
    @Transactional
    public void adjustStock(Product product, int newQuantity, String reason) {
        int before = product.getCurrentStock();
        product.setCurrentStock(newQuantity);
        saveMovement(product, MovementType.ADJUSTMENT, Math.abs(newQuantity - before),
                before, newQuantity, reason, null);
        log.info("Stock ADJUSTMENT for product {}: {} -> {}", product.getSku(), before, newQuantity);
    }

    @Transactional(readOnly = true)
    public List<MovementDTO> getProductMovements(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(MovementDTO::from).toList();
    }

    private void saveMovement(Product product, MovementType type, int quantity,
                              int before, int after, String reason, String reference) {
        Movement movement = Movement.builder()
                .product(product)
                .movementType(type)
                .quantity(quantity)
                .quantityBefore(before)
                .quantityAfter(after)
                .reason(reason)
                .reference(reference)
                .build();
        movementRepository.save(movement);
    }

    /**
     * Évalue l'état du stock et crée/résout les alertes de manière cohérente.
     * Appelée après une entrée, une sortie de stock et à la création d'un produit.
     * Évite les doublons (une seule alerte active par type), et résout les alertes
     * dès que le stock repasse au-dessus du seuil.
     */
    public void evaluateStock(Product product) {
        List<StockAlert> unresolved = stockAlertRepository.findByProductIdAndResolvedFalse(product.getId());

        if (product.getCurrentStock() <= product.getMinStockLevel()) {
            boolean isOut = product.getCurrentStock() == 0;
            AlertType type = isOut ? AlertType.OUT_OF_STOCK : AlertType.LOW_STOCK;
            AlertSeverity severity = isOut ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;

            boolean alreadyActive = unresolved.stream().anyMatch(a -> a.getAlertType() == type);
            if (!alreadyActive) {
                createAlert(product, type, severity,
                        isOut
                                ? "Product " + product.getSku() + " is out of stock!"
                                : "Product " + product.getSku() + " has low stock: " + product.getCurrentStock());
            }
            // Une alerte d'un AUTRE type n'est plus pertinente : en passant de
            // LOW_STOCK à OUT_OF_STOCK, l'alerte LOW_STOCK restait ouverte
            // indéfiniment et polluait /alerts.
            resolve(unresolved.stream().filter(a -> a.getAlertType() != type).toList(), product);
        } else {
            // Stock redevenu sain : on résout les alertes encore ouvertes.
            resolve(unresolved, product);
        }
    }

    private void resolve(List<StockAlert> alerts, Product product) {
        alerts.forEach(alert -> {
            alert.setResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            stockAlertRepository.save(alert);
            log.info("Alert {} auto-resolved for product {}", alert.getId(), product.getSku());
        });
    }

    private void createAlert(Product product, AlertType type, AlertSeverity severity, String message) {
        StockAlert alert = StockAlert.builder()
                .product(product)
                .alertType(type)
                .severity(severity)
                .message(message)
                .resolved(false)
                .build();
        stockAlertRepository.save(alert);
        log.warn("Alert created for product {}: {} - {}", product.getSku(), type, severity);
    }
}
