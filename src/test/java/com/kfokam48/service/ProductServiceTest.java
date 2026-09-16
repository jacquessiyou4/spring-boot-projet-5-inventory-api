package com.kfokam48.service;

import com.kfokam48.dto.CreateProductDTO;
import com.kfokam48.dto.ProductDTO;
import com.kfokam48.entity.Product;
import com.kfokam48.entity.Product.ProductStatus;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockService stockService;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .sku("SKU001")
                .name("Test Product")
                .category("Electronics")
                .price(BigDecimal.valueOf(99.99))
                .currentStock(50)
                .minStockLevel(10)
                .maxStockLevel(200)
                .unit("UNIT")
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void createProduct_shouldReturnDTO() {
        CreateProductDTO dto = new CreateProductDTO();
        dto.setSku("SKU001");
        dto.setName("Test Product");
        dto.setCategory("Electronics");
        dto.setPrice(BigDecimal.valueOf(99.99));

        when(productRepository.existsBySku("SKU001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductDTO result = productService.createProduct(dto);

        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("SKU001");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_withDuplicateSku_shouldThrow() {
        CreateProductDTO dto = new CreateProductDTO();
        dto.setSku("SKU001");
        dto.setName("Test");
        dto.setCategory("Cat");
        dto.setPrice(BigDecimal.TEN);

        when(productRepository.existsBySku("SKU001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU001");
    }

    @Test
    void getAllProducts_shouldReturnList() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(sampleProduct));

        List<ProductDTO> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("SKU001");
    }

    @Test
    void getProductById_whenExists_shouldReturnDTO() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductDTO result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getProductById_whenNotExists_shouldThrow() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProduct_shouldReturnUpdatedDTO() {
        CreateProductDTO dto = new CreateProductDTO();
        dto.setName("Updated");
        dto.setCategory("Updated Cat");
        dto.setPrice(BigDecimal.TEN);
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductDTO result = productService.updateProduct(1L, dto);

        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deleteProduct_whenExists_shouldDelete() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        productService.deleteProduct(1L);

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    void deleteProduct_whenNotExists_shouldThrow() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getLowStockProducts_shouldReturnList() {
        when(productRepository.findLowStockProducts()).thenReturn(Arrays.asList(sampleProduct));

        List<ProductDTO> result = productService.getLowStockProducts();

        assertThat(result).hasSize(1);
    }

    @Test
    void getOutOfStockProducts_shouldReturnList() {
        when(productRepository.findOutOfStockProducts()).thenReturn(Arrays.asList(sampleProduct));

        List<ProductDTO> result = productService.getOutOfStockProducts();

        assertThat(result).hasSize(1);
    }
}
