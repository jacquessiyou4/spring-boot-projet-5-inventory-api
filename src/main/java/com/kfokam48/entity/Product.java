package com.kfokam48.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA d'un produit (table « products »).
 *
 * Porte le nom, le prix et la quantité en stock exigés par le cahier des
 * charges, un SKU unique, les seuils déclenchant les alertes, ainsi que
 * l'historique de ses mouvements et de ses alertes.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "current_stock", nullable = false)
    @Builder.Default
    private Integer currentStock = 0;

    @Column(name = "min_stock_level", nullable = false)
    @Builder.Default
    private Integer minStockLevel = 5;

    @Column(name = "max_stock_level")
    @Builder.Default
    private Integer maxStockLevel = 1000;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String unit = "UNIT";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    /**
     * Exclu de toString() et equals()/hashCode() : la relation est bidirectionnelle.
     * Lombok @Data génère sinon des méthodes qui s'appellent mutuellement entre
     * les deux entités (article -> commentaires -> article -> ...), ce qui lève
     * un StackOverflowError dès qu'on journalise ou compare une entité chargée.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Movement> movements = new ArrayList<>();

    /**
     * Exclu de toString() et equals()/hashCode() : la relation est bidirectionnelle.
     * Lombok @Data génère sinon des méthodes qui s'appellent mutuellement entre
     * les deux entités (article -> commentaires -> article -> ...), ce qui lève
     * un StackOverflowError dès qu'on journalise ou compare une entité chargée.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<StockAlert> alerts = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ProductStatus {
        ACTIVE, INACTIVE, DISCONTINUED
    }
}
