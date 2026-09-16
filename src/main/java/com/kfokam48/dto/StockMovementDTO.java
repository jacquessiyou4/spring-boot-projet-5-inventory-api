package com.kfokam48.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Données reçues pour une entrée ou une sortie de stock : quantité, motif et
 * référence justificative.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDTO {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String reason;

    private String reference;
}
