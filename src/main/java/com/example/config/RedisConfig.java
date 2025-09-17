package com.example.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .entryTtl(Duration.ofMinutes(30)) // Default TTL: 30 minutes
                .disableCachingNullValues()
                .prefixCacheNameWith("app:cache:");

        // Per-cache configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                "users", defaultConfig
                        .entryTtl(Duration.ofMinutes(5))
                        .prefixCacheNameWith("app:users:"),
                "books", defaultConfig
                        .entryTtl(Duration.ofMinutes(10))
                        .prefixCacheNameWith("app:books:")
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}