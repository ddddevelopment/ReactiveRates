package com.reactiverates.domain.service;

import com.reactiverates.domain.model.ExchangeRate;

import reactor.core.publisher.Mono;

public interface RateCache {
    Mono<ExchangeRate> getRate(String fromCurrency, String toCurrency);
    Mono<Void> putRate(ExchangeRate rate);
    Mono<Void> clearAll();
    Mono<Void> evict(String fromCurrency, String toCurrency);
    Mono<CacheStats> getStats();

    record CacheStats(
        long hitCount,
        long missCount,
        long evictionCount,
        long estimatedSize,
        double hitRate
    ) { }
}