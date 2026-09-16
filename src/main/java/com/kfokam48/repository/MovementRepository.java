package com.kfokam48.repository;

import com.kfokam48.entity.Movement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Accès à l'historique des mouvements de stock.
 */
@Repository
public interface MovementRepository extends JpaRepository<Movement, Long> {
    List<Movement> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<Movement> findTop100ByOrderByCreatedAtDesc();
}
