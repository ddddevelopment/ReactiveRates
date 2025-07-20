package com.reactiverates.infrastructure.client;

import com.reactiverates.domain.model.Currency;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateProvider;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Random;

/**
 * Mock-провайдер курсов валют для разработки и тестирования
 */
@Component
@ConditionalOnProperty(name = "mock-provider.enabled", havingValue = "true")
public class MockExchangeRateProvider implements RateProvider {
    
    private static final Logger log = LoggerFactory.getLogger(MockExchangeRateProvider.class);
    private static final String PROVIDER_NAME = "Mock Provider";
    private static final int PROVIDER_PRIORITY = 100; // Низкий приоритет
    
    private final Random random = new Random();
    
    // Базовые курсы для имитации
    private static final Map<String, BigDecimal> BASE_RATES = Map.of(
        "USD-EUR", new BigDecimal("0.9234"),
        "EUR-USD", new BigDecimal("1.0829"),
        "USD-RUB", new BigDecimal("92.5"),
        "RUB-USD", new BigDecimal("0.0108"),
        "EUR-RUB", new BigDecimal("101.2"),
        "RUB-EUR", new BigDecimal("0.0099"),
        "USD-GBP", new BigDecimal("0.7854"),
        "GBP-USD", new BigDecimal("1.2732")
    );

    @Override
    public Mono<ExchangeRate> getCurrentRate(String fromCurrency, String toCurrency) {
        log.debug("[{}] Fetching mock rate: {} -> {}", PROVIDER_NAME, fromCurrency, toCurrency);
        
        return Mono.delay(Duration.ofMillis(100 + random.nextInt(200))) // Имитируем задержку сети
            .then(Mono.fromCallable(() -> {
                String pair = fromCurrency + "-" + toCurrency;
                BigDecimal baseRate = BASE_RATES.get(pair);
                
                if (baseRate == null) {
                    baseRate = new BigDecimal("1.0")
                        .add(new BigDecimal(random.nextDouble() * 2 - 1)); // ±1
                }
                
                double variation = 1.0 + (random.nextDouble() * 0.04 - 0.02);
                BigDecimal finalRate = baseRate.multiply(new BigDecimal(variation)).setScale(6, RoundingMode.HALF_UP);
                
                Currency from = Currency.of(fromCurrency);
                Currency to = Currency.of(toCurrency);
                
                log.debug("[{}] Mock rate generated: {} -> {} = {}", 
                    PROVIDER_NAME, fromCurrency, toCurrency, finalRate);
                
                return ExchangeRate.of(from, to, finalRate, PROVIDER_NAME);
            }));
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return Mono.just(true)
            .doOnNext(available -> log.debug("[{}] Availability: {}", PROVIDER_NAME, available));
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public int getPriority() {
        return PROVIDER_PRIORITY;
    }
} 