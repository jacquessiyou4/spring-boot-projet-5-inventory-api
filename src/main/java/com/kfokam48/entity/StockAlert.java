package com.kfokam48.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA d'une alerte de stock (table « stock_alerts »).
 *
 * Matérialise l'alerte sur stock bas exigée par le cahier des charges, avec son
 * type, sa gravité et son état de résolution.
 */
@Entity
@Table(name = "stock_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlert {

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
    @Column(nullable = false, length = 20)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.WARNING;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AlertType {
        LOW_STOCK, OUT_OF_STOCK, OVERSTOCK
    }

    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
}
