package com.reactiverates.infrastructure.client;

import com.reactiverates.domain.exception.ExternalApiException;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateProvider;
import com.reactiverates.infrastructure.client.dto.ExchangeRateApiResponse;
import com.reactiverates.infrastructure.config.ExchangeRateApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class ExchangeRateApiClient implements RateProvider {

    private static final String PROVIDER_NAME = "ExchangeRate-API.com";
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateApiClient.class);

    private final WebClient webClient;
    private final ExchangeRateApiConfig config;

    public ExchangeRateApiClient(@Qualifier("exchangeRateWebClient") WebClient webClient, ExchangeRateApiConfig config) {
        this.webClient = webClient;
        this.config = config;
    }

    @Override
    public Mono<ExchangeRate> getCurrentRate(String fromCurrency, String toCurrency) {
        log.debug("[{}] Fetching rate: {} -> {}", PROVIDER_NAME, fromCurrency, toCurrency);

        return webClient.get()
            .uri("/v6/{apiKey}/latest/{from}", config.apiKey(), fromCurrency)
            .retrieve()
            .bodyToMono(ExchangeRateApiResponse.class)
            .flatMap(response -> {
                if (!response.isSuccess()) {
                    String errorMsg = String.format("[%s] API error: %s", PROVIDER_NAME, response.errorType());
                    log.error(errorMsg);
                    return Mono.error(new ExternalApiException(errorMsg));
                }

                BigDecimal rate = response.conversionRates().get(toCurrency);
                if (rate == null) {
                    String errorMsg = String.format("[%s] Currency '%s' not found in response for base '%s'", PROVIDER_NAME, toCurrency, fromCurrency);
                    log.error(errorMsg);
                    return Mono.error(new ExternalApiException(errorMsg));
                }

                return Mono.just(ExchangeRate.of(fromCurrency, toCurrency, rate, PROVIDER_NAME));
            });
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return getCurrentRate("USD", "EUR")
            .map(rate -> true)
            .onErrorReturn(false)
            .doOnNext(available -> log.debug("[{}] Availability check: {}", PROVIDER_NAME, available));
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public int getPriority() {
        return config.priority();
    }
} 