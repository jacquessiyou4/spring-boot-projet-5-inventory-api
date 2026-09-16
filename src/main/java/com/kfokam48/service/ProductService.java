package com.kfokam48.service;

import com.kfokam48.dto.CreateProductDTO;
import com.kfokam48.dto.ProductDTO;
import com.kfokam48.dto.StockMovementDTO;
import com.kfokam48.entity.Product;
import com.kfokam48.entity.Product.ProductStatus;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Couche Service : logique métier des produits.
 *
 * Contrôle l'unicité du SKU, applique les modifications (y compris la quantité,
 * tracée comme ajustement), déclenche la réévaluation des alertes et gère le
 * cache Redis des lectures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final StockService stockService;

    /**
     * Le cache Redis était configuré (CacheConfig + docker-compose) mais aucune
     * méthode n'était annotée : il ne servait à rien. Les lectures produit sont
     * désormais mises en cache, et toute écriture (produit ou mouvement de
     * stock) invalide l'ensemble du cache pour ne jamais servir un stock périmé.
     */
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductDTO createProduct(CreateProductDTO dto) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + dto.getSku());
        }

        Product product = Product.builder()
                .sku(dto.getSku())
                .name(dto.getName())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .price(dto.getPrice())
                .currentStock(dto.getInitialStock())
                .minStockLevel(dto.getMinStockLevel())
                .maxStockLevel(dto.getMaxStockLevel())
                .unit(dto.getUnit())
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(product);
        // Alerte immédiate si le produit est créé déjà sous le seuil / en rupture.
        stockService.evaluateStock(saved);
        log.info("Product created with id: {} and SKU: {}", saved.getId(), saved.getSku());
        return toDTO(saved);
    }

    /**
     * Le résultat est mis en cache : il doit rester une ArrayList mutable.
     * Le sérialiseur Redis (typage Jackson NON_FINAL) n'écrit pas d'identifiant
     * de type pour une liste immuable, qui deviendrait donc illisible au
     * rechargement. Ne pas remplacer Collectors.toList() par Stream.toList().
     */
    @Cacheable("products")
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "products", key = "'id_' + #id")
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toDTO(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductDTO updateProduct(Long id, CreateProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getSku().equals(dto.getSku()) && productRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + dto.getSku());
        }

        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setMinStockLevel(dto.getMinStockLevel());
        product.setMaxStockLevel(dto.getMaxStockLevel());
        product.setUnit(dto.getUnit());

        // Le cahier des charges demande de pouvoir modifier la quantité : on
        // l'applique comme un mouvement ADJUSTMENT pour garder l'historique
        // de stock cohérent et auditable.
        if (dto.getInitialStock() != null && !dto.getInitialStock().equals(product.getCurrentStock())) {
            stockService.adjustStock(product, dto.getInitialStock(), "Ajustement via mise à jour du produit");
        }

        product = productRepository.save(product);

        // Re-évalue le statut des alertes après modification du seuil.
        stockService.evaluateStock(product);
        return toDTO(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
        log.info("Product deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getOutOfStockProducts() {
        return productRepository.findOutOfStockProducts().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Passerelle vers StockService qui invalide le cache produit. */
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductDTO applyStockIn(Long productId, StockMovementDTO dto) {
        return toDTO(stockService.stockIn(productId, dto));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductDTO applyStockOut(Long productId, StockMovementDTO dto) {
        return toDTO(stockService.stockOut(productId, dto));
    }

    private ProductDTO toDTO(Product product) {
        return new ProductDTO(
                product.getId(), product.getSku(), product.getName(),
                product.getDescription(), product.getCategory(), product.getPrice(),
                product.getCurrentStock(), product.getMinStockLevel(),
                product.getMaxStockLevel(), product.getUnit(), product.getStatus(),
                product.getCreatedAt(), product.getUpdatedAt()
        );
    }
}
