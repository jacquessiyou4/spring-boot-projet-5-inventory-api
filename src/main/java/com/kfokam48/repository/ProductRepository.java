package com.kfokam48.repository;

import com.kfokam48.entity.Product;
import com.kfokam48.entity.Product.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux données des produits.
 *
 * Fournit la recherche par SKU, les requêtes de stock bas et de rupture, et une
 * lecture avec verrou pessimiste utilisée pour les mises à jour de stock.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory(String category);

    /**
     * Lecture avec verrou pessimiste d'écriture pour garantir des mises à jour
     * de stock atomiques et éviter la survente / les pertes de mises à jour.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.currentStock <= p.minStockLevel AND p.status = 'ACTIVE'")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.currentStock = 0 AND p.status = 'ACTIVE'")
    List<Product> findOutOfStockProducts();
}
