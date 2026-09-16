package com.kfokam48.config;

import com.kfokam48.dto.ProductDTO;
import com.kfokam48.entity.Product.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Régression : le sérialiseur du cache Redis utilisait un ObjectMapper sans
 * JavaTimeModule. Toute valeur contenant un LocalDateTime (ici createdAt)
 * échouait en SerializationException et l'appel renvoyait 500.
 *
 * Les tests d'intégration tournent avec spring.cache.type=simple (cache mémoire,
 * aucune sérialisation) : ils ne pouvaient pas détecter ce défaut. On teste donc
 * le sérialiseur directement, sans serveur Redis.
 */
class CacheSerializationTest {

    private final RedisSerializer<Object> serializer =
            new GenericJackson2JsonRedisSerializer(CacheConfig.redisObjectMapper());

    private ProductDTO sampleProduct() {
        return new ProductDTO(1L, "SKU-001", "Smartphone", "Description", "Electronics",
                new BigDecimal("599.99"), 10, 5, 1000, "UNIT", ProductStatus.ACTIVE,
                LocalDateTime.of(2026, 9, 16, 10, 30), LocalDateTime.of(2026, 9, 16, 11, 0));
    }

    @Test
    void shouldRoundTripProductWithJavaTimeFields() {
        ProductDTO original = sampleProduct();

        Object restored = serializer.deserialize(serializer.serialize(original));

        assertThat(restored).isInstanceOf(ProductDTO.class);
        assertThat((ProductDTO) restored).isEqualTo(original);
        assertThat(((ProductDTO) restored).getCreatedAt()).isEqualTo(original.getCreatedAt());
    }

    /**
     * getAllProducts() met en cache une List : le type doit survivre au round-trip.
     *
     * On utilise ici une ArrayList, exactement ce que produit
     * Collectors.toList() dans ProductService. Le typage par défaut de Jackson
     * est NON_FINAL : une liste immuable (List.of(), Stream.toList()) est d'une
     * classe finale, ne reçoit donc pas d'identifiant de type et ne peut pas
     * être relue. D'où l'obligation de mettre en cache une ArrayList.
     */
    @Test
    void shouldRoundTripProductList() {
        List<ProductDTO> original = new ArrayList<>(List.of(sampleProduct()));

        Object restored = serializer.deserialize(serializer.serialize(original));

        assertThat(restored).isInstanceOf(List.class);
        assertThat((List<?>) restored).hasSize(1).first().isEqualTo(sampleProduct());
    }

    @Test
    void shouldWriteDatesAsIsoStringsNotTimestamps() {
        String json = new String(serializer.serialize(sampleProduct()));

        assertThat(json).contains("2026-09-16T10:30");
    }
}
