package com.kfokam48.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Régression : Product référence ses mouvements et ses alertes, qui référencent
 * en retour leur produit. Avec @Data, Lombok générait des méthodes récursives
 * entre les deux entités (StackOverflowError au premier log ou à la première
 * comparaison). Les deux extrémités sont désormais exclues.
 */
class EntityRelationsTest {

    private Product productWithHistory() {
        Product product = Product.builder()
                .id(1L).sku("SKU-001").name("Smartphone").category("Electronics")
                .price(new BigDecimal("599.99")).currentStock(10).minStockLevel(5).build();
        Movement movement = Movement.builder()
                .id(1L).product(product).movementType(Movement.MovementType.IN)
                .quantity(10).quantityBefore(0).quantityAfter(10).build();
        StockAlert alert = StockAlert.builder()
                .id(1L).product(product).alertType(StockAlert.AlertType.LOW_STOCK)
                .severity(StockAlert.AlertSeverity.WARNING).resolved(false).build();
        product.setMovements(List.of(movement));
        product.setAlerts(List.of(alert));
        return product;
    }

    @Test
    void toString_onBidirectionalRelations_shouldNotOverflow() {
        Product product = productWithHistory();

        assertThatCode(product::toString).doesNotThrowAnyException();
        assertThatCode(() -> product.getMovements().get(0).toString()).doesNotThrowAnyException();
        assertThatCode(() -> product.getAlerts().get(0).toString()).doesNotThrowAnyException();
        assertThat(product.toString()).contains("SKU-001");
    }

    @Test
    void hashCode_onBidirectionalRelations_shouldNotOverflow() {
        Product product = productWithHistory();

        assertThatCode(product::hashCode).doesNotThrowAnyException();
        assertThatCode(() -> product.getMovements().get(0).hashCode()).doesNotThrowAnyException();
        assertThatCode(() -> product.getAlerts().get(0).hashCode()).doesNotThrowAnyException();
    }

    @Test
    void entitiesCanBeStoredInHashSet() {
        Product product = productWithHistory();

        Set<Object> set = new HashSet<>();
        assertThatCode(() -> {
            set.add(product);
            set.add(product.getMovements().get(0));
            set.add(product.getAlerts().get(0));
        }).doesNotThrowAnyException();

        assertThat(set).hasSize(3);
    }
}
