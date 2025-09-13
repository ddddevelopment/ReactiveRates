package com.reactiverates.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.reactiverates.domain.service.RateCache;
import com.reactiverates.infrastructure.cache.CaffeineRateCache;

@Configuration
@EnableConfigurationProperties(CacheConfig.CacheProperties.class)
public class CacheConfig {
    
    /**
     * Fallback на Caffeine если Redis отключен
     */
    @Bean
    @ConditionalOnProperty(name = "reactive-rates.cache.use-redis", havingValue = "false", matchIfMissing = true)
    public RateCache caffeineRateCache(CacheProperties properties) {
        return new CaffeineRateCache(properties.ttl(), properties.maxSize());
    }

    @ConfigurationProperties(prefix = "reactive-rates.cache")
    public record CacheProperties(Duration ttl, long maxSize, boolean enabled, boolean useRedis) {
        public CacheProperties() {
            this(Duration.ofMinutes(3), 1000L, true, false);
        }
    }
}
