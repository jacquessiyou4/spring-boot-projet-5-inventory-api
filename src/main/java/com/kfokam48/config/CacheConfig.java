package com.kfokam48.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * Configuration du cache Redis des lectures de produits.
 *
 * Les listes et fiches produit sont mises en cache ; toute écriture (produit ou
 * mouvement de stock) invalide le cache afin de ne jamais servir un stock
 * périmé. La sérialisation JSON est configurée explicitement : voir
 * {@link #redisCacheSerializer()} pour la raison.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     RedisSerializer<Object> redisCacheSerializer) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisCacheSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Sérialiseur du cache Redis.
     *
     * L'ObjectMapper par défaut ne sait pas écrire les types java.time : toute
     * valeur mise en cache contenant un LocalDateTime (createdAt/updatedAt d'un
     * DTO) échouait en SerializationException, renvoyée au client en 500.
     * Il faut donc enregistrer JavaTimeModule, et activer le typage par défaut
     * pour que la valeur relue retrouve sa classe d'origine.
     */
    @Bean
    public RedisSerializer<Object> redisCacheSerializer() {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper());
    }

    static ObjectMapper redisObjectMapper() {
        // Le typage par défaut inscrit le nom de classe dans le JSON : on le
        // restreint aux types applicatifs et JDK attendus.
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.kfokam48.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.math.")
                .build();

        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY)
                .build();
    }
}
