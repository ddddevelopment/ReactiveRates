package com.reactiverates.infrastructure.cache;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateCache;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class CaffeineRateCache implements RateCache {
    private static final Logger log = LoggerFactory.getLogger(CaffeineRateCache.class);

    private final Cache<String, ExchangeRate> cache;
    private final Duration cacheTtl;

    public CaffeineRateCache(Duration cacheTtl, long maxSize) {
        this.cacheTtl = cacheTtl;
        this.cache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTtl)
            .recordStats()
            .build();

        log.info("Initialized CaffeineRateCache with TTL: {}, max size: {}", cacheTtl, maxSize);
    }

    @Override
    public Mono<ExchangeRate> getRate(String fromCurrency, String toCurrency) {
        return Mono.fromCallable(() -> {
            String key = createKey(fromCurrency, toCurrency);
            ExchangeRate cachedRate = cache.getIfPresent(key);

            if (cachedRate != null && isRateValid(cachedRate)) {
                return cachedRate;
            }

            if (cachedRate != null) {
                cache.invalidate(key);
                log.debug("Evicted expired rate for {}", key);
            }

            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .filter(rate -> rate != null);
    }

    @Override
    public Mono<Void> putRate(ExchangeRate rate) {
        return Mono.fromRunnable(() -> {
            String key = createKey(rate.fromCurrency().code(), rate.toCurrency().code());
            cache.put(key, rate);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    @Override
    public Mono<Void> clearAll() {
        return Mono.fromRunnable(() -> {
            cache.invalidateAll();
            log.info("Cache cleared");
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    @Override
    public Mono<Void> evict(String fromCurrency, String toCurrency) {
        return Mono.fromRunnable(() -> {
            String key = createKey(fromCurrency, toCurrency);
            cache.invalidate(key);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    @Override
    public Mono<CacheStats> getStats() {
        return Mono.fromCallable(() -> {
            var stats = cache.stats();
            return new CacheStats(
                stats.hitCount(),
                stats.missCount(),
                stats.evictionCount(),
                cache.estimatedSize(),
                stats.hitRate()
            );
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    private String createKey(String fromCurrency, String toCurrency) {
        return fromCurrency + "->" + toCurrency;
    }

    private boolean isRateValid(ExchangeRate rate) {
        return rate.timestamp().isAfter(LocalDateTime.now().minus(cacheTtl));
    }
}
