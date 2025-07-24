package com.reactiverates.infrastructure.client;

import com.reactiverates.domain.model.Currency;
import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateProvider;
import com.reactiverates.infrastructure.config.BaseHistoricalRateProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@BaseHistoricalRateProvider
public class MockHistoricalRateProvider implements HistoricalRateProvider {
    private static final Logger log = LoggerFactory.getLogger(MockHistoricalRateProvider.class);
    private static final String PROVIDER_NAME = "Mock Historical Provider";
    private static final int MAX_HISTORY_DAYS = 365;
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
    public Flux<HistoricalExchangeRate> getHistoricalRates(String fromCurrency, String toCurrency, LocalDate startDate, LocalDate endDate) {
        log.debug("[{}] Generating mock historical rates: {} -> {}, {} - {}", PROVIDER_NAME, fromCurrency, toCurrency, startDate, endDate);
        List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1))
            .filter(date -> date.getDayOfWeek().getValue() <= 5)
            .toList();
        return getHistoricalRatesForDates(fromCurrency, toCurrency, new HashSet<>(dates));
    }

    @Override
    public Flux<HistoricalExchangeRate> getHistoricalRatesForDates(String fromCurrency, String toCurrency, Set<LocalDate> dates) {
        log.debug("[{}] Generating mock historical rates for dates: {} -> {}, {} dates", PROVIDER_NAME, fromCurrency, toCurrency, dates.size());
        return Flux.fromIterable(dates)
            .delayElements(Duration.ofMillis(30))
            .map(date -> generateMockRate(fromCurrency, toCurrency, date));
    }

    @Override
    public Mono<HistoricalExchangeRate> getHistoricalRate(String fromCurrency, String toCurrency, LocalDate date) {
        log.debug("[{}] Generating mock historical rate: {} -> {} on {}", PROVIDER_NAME, fromCurrency, toCurrency, date);
        return Mono.delay(Duration.ofMillis(50))
            .map(ignored -> generateMockRate(fromCurrency, toCurrency, date));
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return Mono.just(true).delayElement(Duration.ofMillis(10));
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public int getMaxHistoryDays() {
        return MAX_HISTORY_DAYS;
    }

    @Override
    public Mono<Boolean> supportsHistoricalData(String fromCurrency, String toCurrency) {
        return Mono.just(true);
    }

    private HistoricalExchangeRate generateMockRate(String fromCurrency, String toCurrency, LocalDate date) {
        String pair = fromCurrency + "-" + toCurrency;
        BigDecimal baseRate = BASE_RATES.getOrDefault(pair, BigDecimal.valueOf(1.0));
        double dayFactor = 1.0 + (date.getDayOfYear() % 10) * 0.001; // небольшая сезонность
        double randomFactor = 1.0 + (ThreadLocalRandom.current().nextDouble() * 0.04 - 0.02); // ±2%
        BigDecimal rate = baseRate
            .multiply(BigDecimal.valueOf(dayFactor))
            .multiply(BigDecimal.valueOf(randomFactor))
            .setScale(6, RoundingMode.HALF_UP);
        Currency from = Currency.of(fromCurrency);
        Currency to = Currency.of(toCurrency);
        log.debug("[{}] Mock historical rate: {} -> {} = {} on {}", PROVIDER_NAME, fromCurrency, toCurrency, rate, date);
        return HistoricalExchangeRate.of(from, to, rate, date, PROVIDER_NAME);
    }
} 