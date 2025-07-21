package com.reactiverates.infrastructure.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.reactiverates.domain.service.RateCache;
import com.reactiverates.domain.service.RateProvider;
import com.reactiverates.infrastructure.cache.CachedRateProvider;
import com.reactiverates.infrastructure.cache.CaffeineRateCache;
import com.reactiverates.infrastructure.client.ChainedRateProvider;

@Configuration
@EnableConfigurationProperties(CacheConfig.CacheProperties.class)
public class CacheConfig {
    @Bean
    public RateCache rateCache(CacheProperties properties) {
        return new CaffeineRateCache(properties.ttl(), properties.maxSize());
    }

    @Bean
    @Primary
    public RateProvider cachedChainedRateProvider(ChainedRateProvider chainedProvider, RateCache rateCache) {
        return new CachedRateProvider(chainedProvider, rateCache);
    }

    @ConfigurationProperties(prefix = "reactive-rates.cache")
    public record CacheProperties(Duration ttl, long maxSize, boolean enabled) {
        public CacheProperties() {
            this(Duration.ofMinutes(3), 1000L, true);
        }
    }
}
