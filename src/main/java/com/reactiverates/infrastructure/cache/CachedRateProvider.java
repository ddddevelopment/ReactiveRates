package com.reactiverates.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateCache;
import com.reactiverates.domain.service.RateProvider;

import reactor.core.publisher.Mono;

public class CachedRateProvider implements RateProvider {
    private static final Logger log = LoggerFactory.getLogger(CachedRateProvider.class);

    private final RateProvider delegate;
    private final RateCache cache;

    public CachedRateProvider(RateProvider delegate, RateCache cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Mono<ExchangeRate> getCurrentRate(String fromCurrency, String toCurrency) {
        String cacheKey = createCacheKey(fromCurrency, toCurrency);

        return cache.getRate(fromCurrency, toCurrency)
            .doOnNext(cachedRate -> log.debug("Cache HIT for {}: {}", cacheKey, cachedRate.rate()))
            .switchIfEmpty(
                fetchAndCache(fromCurrency, toCurrency, cacheKey)
            );
    }
    
    private String createCacheKey(String from, String to) {
        return from + "->" + to;
    }

    private Mono<ExchangeRate> fetchAndCache(String fromCurrency, String toCurrency, String cacheKey) {
        log.debug("Cache MISS for {}, fetching from provider: {}", cacheKey, delegate.getProviderName());
        
        return delegate.getCurrentRate(fromCurrency, toCurrency)
            .flatMap(rate -> cache.putRate(rate)
                .thenReturn(rate)
                .doOnSuccess(r -> log.debug("Cached rate for {}: {}", cacheKey, r.rate())));
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public String getProviderName() {
        return delegate.getProviderName() + "(Cached)";
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }    
}
