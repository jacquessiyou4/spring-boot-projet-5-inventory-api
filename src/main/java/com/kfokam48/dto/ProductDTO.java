package com.kfokam48.dto;

import com.kfokam48.entity.Product.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Représentation d'un produit renvoyée au client (sans les collections JPA).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer currentStock;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private String unit;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
