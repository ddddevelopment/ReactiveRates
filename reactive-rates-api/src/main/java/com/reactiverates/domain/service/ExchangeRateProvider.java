package com.reactiverates.domain.service;

import com.reactiverates.domain.model.ExchangeRate;

import reactor.core.publisher.Mono;

public interface ExchangeRateProvider {
    Mono<ExchangeRate> getCurrentRate(String fromCurrency, String toCurrency);
    Mono<Boolean> isAvailable();
    String getProviderName();
    default int getPriority() {
        return 100;
    }
}
