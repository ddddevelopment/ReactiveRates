package com.reactiverates.infrastructure.cache;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateCache;
import com.reactiverates.infrastructure.config.RedisConfig.CacheProperties;

import reactor.core.publisher.Mono;

public class RedisRateCache implements RateCache {
    private static final Logger log = LoggerFactory.getLogger(RedisRateCache.class);

    private final ReactiveRedisTemplate<String, ExchangeRate> redisTemplate;
    private final CacheProperties cacheProperties;
    
    // Простая статистика
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    public RedisRateCache(ReactiveRedisTemplate<String, ExchangeRate> redisTemplate, 
                         CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
        
        log.info("Initialized RedisRateCache with TTL: {}, key prefix: {}", 
                cacheProperties.ttl(), cacheProperties.keyPrefix());
    }

    @Override
    public Mono<ExchangeRate> getRate(String fromCurrency, String toCurrency) {
        String key = createKey(fromCurrency, toCurrency);
        
        return redisTemplate.opsForValue().get(key)
            .cast(ExchangeRate.class)
            .filter(this::isRateValid)
            .doOnNext(rate -> {
                hitCount.incrementAndGet();
                log.info("✅ REDIS CACHE HIT: key={}, rate={}", key, rate.rate());
            })
            .switchIfEmpty(Mono.fromRunnable(() -> {
                missCount.incrementAndGet();
                log.debug("Cache MISS for key: {}", key);
            }).then(Mono.empty()))
            .onErrorResume(throwable -> {
                log.warn("Redis get operation failed for key: {}, error: {}", key, throwable.getMessage());
                missCount.incrementAndGet();
                return Mono.empty();
            });
    }

    @Override
    public Mono<Void> putRate(ExchangeRate rate) {
        String key = createKey(rate.fromCurrency().code(), rate.toCurrency().code());
        
        return redisTemplate.opsForValue()
            .set(key, rate, cacheProperties.ttl())
            .doOnSuccess(success -> {
                if (success) {
                    log.info("✅ REDIS CACHE PUT: key={}, rate={}, TTL={}", key, rate.rate(), cacheProperties.ttl());
                } else {
                    log.warn("❌ REDIS CACHE PUT FAILED: key={}", key);
                }
            })
            .onErrorResume(throwable -> {
                log.error("Failed to put rate to Redis for key: {}, error: {}", key, throwable.getMessage());
                return Mono.just(false);
            })
            .then();
    }

    @Override
    public Mono<Void> clearAll() {
        String pattern = cacheProperties.keyPrefix() + "*";
        
        return redisTemplate.keys(pattern)
            .flatMap(redisTemplate::delete)
            .doOnNext(deletedCount -> evictionCount.addAndGet(deletedCount))
            .then()
            .doOnSuccess(v -> log.info("Cache cleared, pattern: {}", pattern))
            .onErrorResume(throwable -> {
                log.error("Failed to clear cache, error: {}", throwable.getMessage());
                return Mono.empty();
            });
    }

    @Override
    public Mono<Void> evict(String fromCurrency, String toCurrency) {
        String key = createKey(fromCurrency, toCurrency);
        
        return redisTemplate.delete(key)
            .doOnNext(deletedCount -> {
                if (deletedCount > 0) {
                    evictionCount.incrementAndGet();
                    log.debug("Evicted key: {}", key);
                }
            })
            .onErrorResume(throwable -> {
                log.warn("Failed to evict key: {}, error: {}", key, throwable.getMessage());
                return Mono.just(0L);
            })
            .then();
    }

    @Override
    public Mono<CacheStats> getStats() {
        return redisTemplate.keys(cacheProperties.keyPrefix() + "*")
            .count()
            .map(estimatedSize -> {
                long totalRequests = hitCount.get() + missCount.get();
                double hitRate = totalRequests > 0 ? (double) hitCount.get() / totalRequests : 0.0;
                
                return new CacheStats(
                    hitCount.get(),
                    missCount.get(),
                    evictionCount.get(),
                    estimatedSize,
                    hitRate
                );
            })
            .onErrorReturn(new CacheStats(hitCount.get(), missCount.get(), evictionCount.get(), 0L, 0.0));
    }

    private String createKey(String fromCurrency, String toCurrency) {
        return cacheProperties.keyPrefix() + fromCurrency + "->" + toCurrency;
    }

    private boolean isRateValid(ExchangeRate rate) {
        if (rate == null || rate.timestamp() == null) {
            return false;
        }
        return rate.timestamp().isAfter(LocalDateTime.now().minus(cacheProperties.ttl()));
    }
}
