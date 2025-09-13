package com.reactiverates.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateCache;
import com.reactiverates.domain.service.RateProvider;
import com.reactiverates.infrastructure.cache.CachedRateProvider;
import com.reactiverates.infrastructure.cache.RedisRateCache;
import com.reactiverates.infrastructure.client.ChainedRateProvider;

@Configuration
@EnableConfigurationProperties({
    RedisConfig.CacheProperties.class
})
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(name = "reactive-rates.cache.use-redis", havingValue = "true")
    public ReactiveRedisTemplate<String, ExchangeRate> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        // Настройка ObjectMapper для правильной сериализации LocalDateTime
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        
        Jackson2JsonRedisSerializer<ExchangeRate> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ExchangeRate.class);
        
        RedisSerializationContext<String, ExchangeRate> serializationContext = 
            RedisSerializationContext.<String, ExchangeRate>newSerializationContext(StringRedisSerializer.UTF_8)
                .value(jsonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    @Bean
    @ConditionalOnProperty(name = "reactive-rates.cache.use-redis", havingValue = "true")
    public RateCache redisRateCache(
            ReactiveRedisTemplate<String, ExchangeRate> redisTemplate,
            CacheProperties cacheProperties) {
        return new RedisRateCache(redisTemplate, cacheProperties);
    }

    @Bean
    @Primary
    public RateProvider cachedChainedRateProvider(ChainedRateProvider chainedProvider, RateCache rateCache) {
        return new CachedRateProvider(chainedProvider, rateCache);
    }

    @ConfigurationProperties(prefix = "reactive-rates.cache")
    public record CacheProperties(
        Duration ttl, 
        boolean enabled,
        String keyPrefix,
        boolean useRedis
    ) {
        public CacheProperties() {
            this(Duration.ofMinutes(5), true, "rates:", false);
        }
    }
}