package com.reactiverates.infrastructure.client;

import com.reactiverates.domain.exception.ExternalApiException;
import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateProvider;
import com.reactiverates.infrastructure.config.BaseHistoricalRateProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Component
public class ChainedHistoricalRateProvider implements HistoricalRateProvider {
    private static final Logger log = LoggerFactory.getLogger(ChainedHistoricalRateProvider.class);
    private final List<HistoricalRateProvider> providers;

    public ChainedHistoricalRateProvider(@BaseHistoricalRateProvider List<HistoricalRateProvider> providers) {
        this.providers = providers.stream()
            .sorted(Comparator.comparingInt(HistoricalRateProvider::getMaxHistoryDays).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public Flux<HistoricalExchangeRate> getHistoricalRates(String fromCurrency, String toCurrency, LocalDate startDate, LocalDate endDate) {
        return Flux.fromIterable(providers)
            .concatMap(provider -> provider.isAvailable()
                .filter(Boolean::booleanValue)
                .doOnNext(available -> log.debug("Provider {} is available, attempting to fetch historical rates.", provider.getProviderName()))
                .flatMapMany(available -> provider.getHistoricalRates(fromCurrency, toCurrency, startDate, endDate)
                    .doOnNext(rate -> log.info("Successfully got historical rate from {}", provider.getProviderName()))
                    .doOnError(err -> log.warn("Provider {} failed to get historical rates for {}->{}. Reason: {}",
                        provider.getProviderName(), fromCurrency, toCurrency, err.getMessage()))
                )
                .onErrorResume(err -> {
                    log.warn("Switching to next provider due to error in {}: {}", provider.getProviderName(), err.getMessage());
                    return Flux.empty();
                })
            )
            .switchIfEmpty(Flux.error(new ExternalApiException("All historical rate providers are unavailable or failed to provide rates.")));
    }

    @Override
    public Flux<HistoricalExchangeRate> getHistoricalRatesForDates(String fromCurrency, String toCurrency, Set<LocalDate> dates) {
        return Flux.fromIterable(providers)
            .concatMap(provider -> provider.isAvailable()
                .filter(Boolean::booleanValue)
                .doOnNext(available -> log.debug("Provider {} is available, attempting to fetch historical rates for dates.", provider.getProviderName()))
                .flatMapMany(available -> provider.getHistoricalRatesForDates(fromCurrency, toCurrency, dates)
                    .doOnNext(rate -> log.info("Successfully got historical rate from {}", provider.getProviderName()))
                    .doOnError(err -> log.warn("Provider {} failed to get historical rates for dates for {}->{}. Reason: {}",
                        provider.getProviderName(), fromCurrency, toCurrency, err.getMessage()))
                )
                .onErrorResume(err -> {
                    log.warn("Switching to next provider due to error in {}: {}", provider.getProviderName(), err.getMessage());
                    return Flux.empty();
                })
            )
            .switchIfEmpty(Flux.error(new ExternalApiException("All historical rate providers are unavailable or failed to provide rates for dates.")));
    }

    @Override
    public Mono<HistoricalExchangeRate> getHistoricalRate(String fromCurrency, String toCurrency, LocalDate date) {
        return Flux.fromIterable(providers)
            .concatMap(provider -> provider.isAvailable()
                .filter(Boolean::booleanValue)
                .doOnNext(available -> log.debug("Provider {} is available, attempting to fetch historical rate.", provider.getProviderName()))
                .flatMap(available -> provider.getHistoricalRate(fromCurrency, toCurrency, date)
                    .doOnSuccess(rate -> log.info("Successfully got historical rate from {}", provider.getProviderName()))
                    .doOnError(err -> log.warn("Provider {} failed to get historical rate for {}->{}. Reason: {}",
                        provider.getProviderName(), fromCurrency, toCurrency, err.getMessage()))
                )
                .onErrorResume(err -> {
                    log.warn("Switching to next provider due to error in {}: {}", provider.getProviderName(), err.getMessage());
                    return Mono.empty();
                })
            )
            .next()
            .switchIfEmpty(Mono.error(new ExternalApiException("All historical rate providers are unavailable or failed to provide a rate.")));
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return Flux.fromIterable(providers)
            .flatMap(HistoricalRateProvider::isAvailable)
            .any(Boolean::booleanValue);
    }

    @Override
    public String getProviderName() {
        return "Chained Historical Rate Provider";
    }

    @Override
    public int getMaxHistoryDays() {
        return providers.stream().mapToInt(HistoricalRateProvider::getMaxHistoryDays).max().orElse(365);
    }

    @Override
    public Mono<Boolean> supportsHistoricalData(String fromCurrency, String toCurrency) {
        return Flux.fromIterable(providers)
            .concatMap(provider -> provider.supportsHistoricalData(fromCurrency, toCurrency)
                .filter(Boolean::booleanValue)
            )
            .hasElements();
    }
} 