package com.reactiverates.infrastructure.client;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.reactiverates.domain.exception.ExternalApiException;
import com.reactiverates.domain.model.Currency;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.ExchangeRateProvider;
import com.reactiverates.infrastructure.client.dto.ExchangeRateApiResponse;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component("exchangeRateHostProvider")
@ConditionalOnProperty(name = "exchange-rate.mock.enabled", havingValue = "false")
public class ExchangeRateClient implements ExchangeRateProvider {
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateClient.class);
    private static final String PROVIDER_NAME = "ExchangeRate.host";
    private static final int PROVIDER_PRIORITY = 1;

    private final WebClient webClient;

    public ExchangeRateClient(@Qualifier("exchangeRateWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<ExchangeRate> getCurrentRate(String fromCurrency, String toCurrency) {
        log.debug("[{}] Fetching rate: {} -> {}", PROVIDER_NAME, fromCurrency, toCurrency);

        return webClient
            .get()
            .uri(builder -> builder.path("/latest")
                .queryParam("from", fromCurrency)
                .queryParam("to", toCurrency)
                .queryParam("amount", 1)
                .build())
            .retrieve()
            .bodyToMono(ExchangeRateApiResponse.class)
            .flatMap(response -> {
                if (!response.isValid()) {
                    return Mono.error(new ExternalApiException(
                        PROVIDER_NAME + " returned invalid response: " + 
                        (response.success() ? "missing rate data" : "API request failed")));
                }
                return Mono.just(mapToExchangeRate(response, fromCurrency, toCurrency));
            })
            .timeout(Duration.ofSeconds(10))
            .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                .doBeforeRetry(signal -> log.warn("[{}] Retrying request: {}", PROVIDER_NAME, signal.failure().getMessage()))
            )
            .onErrorMap(WebClientResponseException.class, ex -> new ExternalApiException(PROVIDER_NAME + "API error: " + ex.getMessage(), ex))
            .doOnSuccess(rate -> log.debug("[{}] Successfully fetched rate: {}", PROVIDER_NAME, rate))
            .doOnError(error -> log.error("[{}] Failed to fetch rate: {}", PROVIDER_NAME, error.getMessage()));
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
        return PROVIDER_PRIORITY;
    }

    private ExchangeRate mapToExchangeRate(ExchangeRateApiResponse response, String fromCode, String toCode) {
        Currency fromCurrency = Currency.of(fromCode);
        Currency toCurrency = Currency.of(toCode);
        return ExchangeRate.of(fromCurrency, toCurrency, response.getExchangeRate());
    }
}
