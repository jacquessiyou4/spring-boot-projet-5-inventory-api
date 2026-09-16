package com.kfokam48.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA d'un mouvement de stock (table « stock_movements »).
 *
 * Trace chaque variation (entrée, sortie, ajustement) avec la quantité avant et
 * après, ce qui rend l'évolution du stock auditable.
 */
@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Exclu de toString() et equals()/hashCode() : la relation est bidirectionnelle.
     * Lombok @Data génère sinon des méthodes qui s'appellent mutuellement entre
     * les deux entités (article -> commentaires -> article -> ...), ce qui lève
     * un StackOverflowError dès qu'on journalise ou compare une entité chargée.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 100)
    private String reference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum MovementType {
        IN, OUT, ADJUSTMENT, TRANSFER
    }
}
