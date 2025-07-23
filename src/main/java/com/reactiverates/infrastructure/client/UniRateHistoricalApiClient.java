package com.reactiverates.infrastructure.client;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateProvider;
import com.reactiverates.domain.exception.ExternalApiException;
import com.reactiverates.infrastructure.client.dto.UniRateHistoricalTimeseriesResponse;
import com.reactiverates.infrastructure.config.BaseHistoricalRateProvider;
import com.reactiverates.infrastructure.config.UniRateApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@BaseHistoricalRateProvider
@ConditionalOnProperty(name = "mock-provider.enabled", havingValue = "false", matchIfMissing = true)
public class UniRateHistoricalApiClient implements HistoricalRateProvider {
    private static final Logger log = LoggerFactory.getLogger(UniRateHistoricalApiClient.class);
    private static final String PROVIDER_NAME = "UniRateAPI-Historical";
    private static final int MAX_HISTORY_DAYS = 5 * 366;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final WebClient webClient;
    private final UniRateApiConfig config;

    public UniRateHistoricalApiClient(@Qualifier("uniRateWebClient") WebClient webClient, UniRateApiConfig config) {
        this.webClient = webClient;
        this.config = config;
    }

    @Override
    public Flux<HistoricalExchangeRate> getHistoricalRates(String fromCurrency, String toCurrency, LocalDate startDate, LocalDate endDate) {
        log.debug("[{}] Fetching time series: {} -> {}, {} - {}", PROVIDER_NAME, fromCurrency, toCurrency, startDate, endDate);
        if (startDate.isAfter(endDate)) {
            return Flux.error(new IllegalArgumentException("Start date cannot be after end date"));
        }
        if (Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays() > MAX_HISTORY_DAYS) {
            return Flux.error(new IllegalArgumentException("Диапазон дат не должен превышать 5 лет"));
        }
        return webClient.get()
                .uri(builder -> {
                    var uriBuilder = builder.path("/api/historical/timeseries")
                            .queryParam("from", fromCurrency)
                            .queryParam("to", toCurrency)
                            .queryParam("start_date", startDate.format(DATE_FORMAT))
                            .queryParam("end_date", endDate.format(DATE_FORMAT));
                    if (config.apiKey() != null && !config.apiKey().isBlank()) {
                        uriBuilder.queryParam("api_key", config.apiKey());
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(UniRateHistoricalTimeseriesResponse.class)
                .flatMapMany(response -> {
                    if (!response.isValid()) {
                        return Flux.error(new ExternalApiException("Некорректный ответ от UniRateAPI (timeseries)"));
                    }
                    Map<String, Map<String, BigDecimal>> data = response.data();
                    return Flux.fromIterable(data.entrySet())
                            .map(entry -> {
                                LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMAT);
                                BigDecimal rate = entry.getValue().get(toCurrency);
                                if (rate == null) {
                                    throw new ExternalApiException("Нет курса для " + toCurrency + " на дату " + entry.getKey());
                                }
                                return HistoricalExchangeRate.of(fromCurrency, toCurrency, rate, date, PROVIDER_NAME);
                            });
                })
                .timeout(config.timeout())
                .retryWhen(Retry.backoff(2, config.connectTimeout()))
                .doOnError(e -> log.error("[{}] Error fetching time series: {}", PROVIDER_NAME, e.getMessage()));
    }

    @Override
    public Flux<HistoricalExchangeRate> getHistoricalRatesForDates(String fromCurrency, String toCurrency, Set<LocalDate> dates) {
        return Flux.fromIterable(dates)
                .flatMap(date -> getHistoricalRate(fromCurrency, toCurrency, date))
                .onErrorContinue((e, o) -> log.warn("[{}] Error fetching rate for date {}: {}", PROVIDER_NAME, o, e.getMessage()));
    }

    @Override
    public Mono<HistoricalExchangeRate> getHistoricalRate(String fromCurrency, String toCurrency, LocalDate date) {
        log.debug("[{}] Fetching historical rate: {} -> {} on {}", PROVIDER_NAME, fromCurrency, toCurrency, date);
        return webClient.get()
                .uri(builder -> {
                    var uriBuilder = builder.path("/api/historical/rates")
                            .queryParam("from", fromCurrency)
                            .queryParam("to", toCurrency)
                            .queryParam("date", date.format(DATE_FORMAT));
                    if (config.apiKey() != null && !config.apiKey().isBlank()) {
                        uriBuilder.queryParam("api_key", config.apiKey());
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(UniRateHistoricalTimeseriesResponse.class)
                .flatMap(response -> {
                    if (!response.isValid() || response.data().isEmpty()) {
                        return Mono.error(new ExternalApiException("Некорректный ответ от UniRateAPI (rates)"));
                    }
                    Map<String, Map<String, BigDecimal>> data = response.data();
                    Map.Entry<String, Map<String, BigDecimal>> entry = data.entrySet().iterator().next();
                    BigDecimal rate = entry.getValue().get(toCurrency);
                    if (rate == null) {
                        return Mono.error(new ExternalApiException("Нет курса для " + toCurrency + " на дату " + entry.getKey()));
                    }
                    LocalDate respDate = LocalDate.parse(entry.getKey(), DATE_FORMAT);
                    return Mono.just(HistoricalExchangeRate.of(fromCurrency, toCurrency, rate, respDate, PROVIDER_NAME));
                })
                .timeout(config.timeout())
                .retryWhen(Retry.backoff(2, config.connectTimeout()))
                .doOnError(e -> log.error("[{}] Error fetching rate for date {}: {}", PROVIDER_NAME, date, e.getMessage()));
    }

    @Override
    public Mono<Boolean> isAvailable() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return getHistoricalRate("USD", "EUR", yesterday)
                .map(rate -> true)
                .onErrorReturn(false)
                .doOnNext(available -> log.debug("[{}] Availability check: {}", PROVIDER_NAME, available));
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
} 