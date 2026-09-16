package com.kfokam48.dto;

import com.kfokam48.entity.Movement;
import com.kfokam48.entity.Movement.MovementType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Évite d'exposer l'entité Movement directement : la relation LAZY vers Product
 * était sérialisée à travers open-in-view et exposait tout le graphe produit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovementDTO {
    private Long id;
    private Long productId;
    private String productSku;
    private MovementType movementType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String reason;
    private String reference;
    private LocalDateTime createdAt;

    public static MovementDTO from(Movement movement) {
        return new MovementDTO(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getSku(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getQuantityBefore(),
                movement.getQuantityAfter(),
                movement.getReason(),
                movement.getReference(),
                movement.getCreatedAt()
        );
    }
}
