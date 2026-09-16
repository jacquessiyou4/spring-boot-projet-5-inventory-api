package com.kfokam48.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Données reçues pour créer ou mettre à jour un produit.
 *
 * À la création, « initialStock » fixe le stock de départ ; à la mise à jour,
 * il exprime la nouvelle quantité et l'écart est tracé comme mouvement
 * ADJUSTMENT.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductDTO {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    // Borne haute : la colonne est un NUMERIC(10,2).
    @DecimalMax(value = "99999999.99", message = "Price must not exceed 99 999 999.99")
    private BigDecimal price;

    /**
     * Stock initial à la création ; à la mise à jour, nouvelle quantité en stock
     * (un mouvement ADJUSTMENT est alors enregistré). Laisser null pour ne pas
     * toucher au stock existant.
     */
    @Min(value = 0, message = "Initial stock cannot be negative")
    private Integer initialStock = 0;

    @Min(value = 1, message = "Minimum stock level must be at least 1")
    private Integer minStockLevel = 5;

    private Integer maxStockLevel = 1000;

    private String unit = "UNIT";
}
