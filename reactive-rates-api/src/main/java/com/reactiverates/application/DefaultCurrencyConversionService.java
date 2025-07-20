package com.reactiverates.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reactiverates.domain.exception.CurrencyNotFoundException;
import com.reactiverates.domain.model.ConversionRequest;
import com.reactiverates.domain.model.ConversionResult;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.CurrencyConversionService;
import com.reactiverates.domain.service.ExchangeRateProvider;

import reactor.core.publisher.Mono;

public class DefaultCurrencyConversionService implements CurrencyConversionService {
    private static final Logger log = LoggerFactory.getLogger(DefaultCurrencyConversionService.class);
    private final List<ExchangeRateProvider> providers;

    public DefaultCurrencyConversionService(List<ExchangeRateProvider> providers) {
        this.providers = providers.stream()
            .sorted((p1, p2) -> Integer.compare(p1.getPriority(), p2.getPriority()))
            .toList();

        log.info("Initialized with {} exchange rate providers: {}", 
            providers.size(), 
            providers.stream().map(ExchangeRateProvider::getProviderName).toList());
    }

    @Override
    public Mono<ConversionResult> convertCurrency(ConversionRequest request) {
        log.debug("Converting {} {} to {}", request.amount(), request.fromCurrency(), request.toCurrency());

        if (request.fromCurrency().equals(request.toCurrency())) {
            return Mono.just(createSameCurrencyResult(request));
        }

        return getExchangeRate(request.fromCurrency(), request.toCurrency())
            .map(rate -> calculateConversion(request, rate))
            .doOnSuccess(result -> log.debug("Conversion completed: {} {} = {} {}", 
                request.amount(), request.fromCurrency(), result.convertedAmount(), request.toCurrency()))
            .doOnError(error -> log.error("Conversion failed for {}: {}", request, error.getMessage()));
    }

    @Override
    public Mono<ExchangeRate> getExchangeRate(String fromCurrency, String toCurrency) {
        log.debug("Getting exchange rate: {} -> {}", fromCurrency, toCurrency);

        return providers.stream()
            .reduce(Mono.<ExchangeRate>empty(), 
                (fallback, provider) -> fallback.switchIfEmpty(Mono.defer(() -> {
                    log.debug("Trying provider: {}", provider.getProviderName());
                    return provider.getCurrentRate(fromCurrency, toCurrency)
                        .doOnError(error -> log.warn("Provider {} failed: {}", provider.getProviderName(), error.getMessage()));
                })),
                (m1, m2) -> m1.switchIfEmpty(m2))
                .switchIfEmpty(Mono.error(new CurrencyNotFoundException("No provider could fetch rate for " + fromCurrency + "/" + toCurrency)));
    }

    @Override
    public Mono<Boolean> isCurrencyPairSupported(String fromCurrency, String toCurrency) {
        return getExchangeRate(fromCurrency, toCurrency)
            .map(rate -> true)
            .onErrorReturn(false);
    }

    private ConversionResult createSameCurrencyResult(ConversionRequest request) {
        ExchangeRate sameCurrencyRate = ExchangeRate.of(request.fromCurrency(), request.toCurrency(), BigDecimal.ONE);
        return ConversionResult.of(request, sameCurrencyRate, request.amount());
    }

    private ConversionResult calculateConversion(ConversionRequest request, ExchangeRate rate) {
        BigDecimal convertedAmount = request.amount().multiply(rate.rate()).setScale(4, RoundingMode.HALF_UP);

        return ConversionResult.of(request, rate, convertedAmount);
    }
}
