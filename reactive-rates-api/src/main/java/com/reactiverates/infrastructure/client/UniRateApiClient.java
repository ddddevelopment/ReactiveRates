package com.reactiverates.infrastructure.client;

import com.reactiverates.domain.exception.ExternalApiException;
import com.reactiverates.domain.model.Currency;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateProvider;
import com.reactiverates.infrastructure.client.dto.UniRateApiResponse;
import com.reactiverates.infrastructure.config.UniRateApiConfig;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@ConditionalOnProperty(name = "unirate-api.mock.enabled", havingValue = "false", matchIfMissing = true)
public class UniRateApiClient implements RateProvider {
    private static final String PROVIDER_NAME = "UniRateAPI";
    private static final Logger log = LoggerFactory.getLogger(UniRateApiClient.class);

    private final WebClient webClient;
    private final UniRateApiConfig config;

    public UniRateApiClient(@Qualifier("uniRateWebClient") WebClient webClient, UniRateApiConfig config) {
        this.webClient = webClient;
        this.config = config;
    }

    @Override
    public int getPriority() {
        return config.priority();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return getCurrentRate("USD", "EUR")
            .map(rate -> true)
            .onErrorReturn(false)
            .doOnNext(available -> log.debug("[{}] Availability check: {}", PROVIDER_NAME, available));
    }

    @Override
    public Mono<ExchangeRate> getCurrentRate(String fromCurrency, String toCurrency) {
        log.debug("[{}] Fetching rate via /api/rates: {} -> {}", PROVIDER_NAME, fromCurrency, toCurrency);

        return webClient
            .get()
            .uri(builder -> {
                var uriBuilder = builder.path("/api/rates")
                    .queryParam("from", fromCurrency)
                    .queryParam("to", toCurrency)
                    .queryParam("amount", 1); 

                if (config.apiKey() != null && !config.apiKey().isBlank()) {
                    uriBuilder.queryParam("api_key", config.apiKey());
                    log.debug("[{}] Using API key for request", PROVIDER_NAME);
                } else {
                    log.warn("[{}] ⚠️ API key not configured! Request may fail", PROVIDER_NAME);
                }
                
                return uriBuilder.build();
            })
            .retrieve()
            .bodyToMono(UniRateApiResponse.class)
            .flatMap(response -> {
                log.debug("[{}] API Response received: {}", PROVIDER_NAME, response);
                
                if (!response.isValid()) {
                    String errorMsg = String.format(
                        "API request failed or returned invalid data for %s -> %s.", 
                        fromCurrency, toCurrency);
                    return Mono.error(new ExternalApiException(errorMsg));
                }
                
                return Mono.just(mapToExchangeRate(response));
            })
            .timeout(config.timeout())
            .retryWhen(Retry.backoff(2, config.connectTimeout())
                .doBeforeRetry(signal -> log.warn("[{}] Retrying request: {}", 
                    PROVIDER_NAME, signal.failure().getMessage()))
            )
            .onErrorMap(WebClientResponseException.class, ex -> {
                String detailedError = String.format("%s API error (status: %d): %s", 
                    PROVIDER_NAME, ex.getStatusCode().value(), ex.getResponseBodyAsString());
                return new ExternalApiException(detailedError, ex);
            })
            .doOnSuccess(rate -> log.debug("[{}] Successfully fetched rate: {} -> {} = {}", 
                PROVIDER_NAME, fromCurrency, toCurrency, rate.rate()))
            .doOnError(error -> log.error("[{}] Failed to fetch rate {}->{}: {}", 
                PROVIDER_NAME, fromCurrency, toCurrency, error.getMessage()));
    }

    private ExchangeRate mapToExchangeRate(UniRateApiResponse response) {
        return new ExchangeRate(
            Currency.of(response.base()),
            Currency.of(response.to()),
            response.rate(),
            LocalDateTime.now(),
            PROVIDER_NAME
        );
    }
}
