package com.dibya.knowledgehub.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
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
@EnableCaching
public class CacheConfig {

    private RedisCacheConfiguration defaultConfig(ObjectMapper objectMapper) {
        var serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("knowledgehub::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig(objectMapper))
                .withInitialCacheConfigurations(Map.of(
                        "users", defaultConfig(objectMapper).entryTtl(Duration.ofMinutes(5)),
                        "documents", defaultConfig(objectMapper).entryTtl(Duration.ofMinutes(2))
                ))
                .build();
    }
}
