package com.reactiverates.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.reactiverates.domain.model.ConversionRequest;
import com.reactiverates.domain.model.ConversionResult;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.CurrencyConversionService;
import com.reactiverates.domain.service.RateProvider;

import reactor.core.publisher.Mono;


@Service
public class DefaultCurrencyConversionService implements CurrencyConversionService {
    private static final Logger log = LoggerFactory.getLogger(DefaultCurrencyConversionService.class);
    private final RateProvider rateProvider;

    public DefaultCurrencyConversionService(RateProvider rateProvider) {
        this.rateProvider = rateProvider;
        log.info("Initialized with exchange rate provider: {}", rateProvider.getProviderName());
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
        log.debug("Getting exchange rate via {}: {} -> {}", rateProvider.getProviderName(), fromCurrency, toCurrency);
        return rateProvider.getCurrentRate(fromCurrency, toCurrency);
    }

    @Override
    public Mono<Boolean> isCurrencyPairSupported(String fromCurrency, String toCurrency) {
        return getExchangeRate(fromCurrency, toCurrency)
            .map(rate -> true)
            .onErrorReturn(false);
    }

    private ConversionResult createSameCurrencyResult(ConversionRequest request) {
        ExchangeRate sameCurrencyRate = ExchangeRate.of(request.fromCurrency(), request.toCurrency(), BigDecimal.ONE, "Internal");
        return ConversionResult.of(request, sameCurrencyRate, request.amount());
    }

    private ConversionResult calculateConversion(ConversionRequest request, ExchangeRate rate) {
        BigDecimal convertedAmount = request.amount().multiply(rate.rate()).setScale(4, RoundingMode.HALF_UP);

        return ConversionResult.of(request, rate, convertedAmount);
    }
}
