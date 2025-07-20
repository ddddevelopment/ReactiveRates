package com.reactiverates.infrastructure.client;

import com.reactiverates.domain.exception.ExternalApiException;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Primary
public class ChainedRateProvider implements RateProvider {

    private static final Logger log = LoggerFactory.getLogger(ChainedRateProvider.class);

    private final List<RateProvider> providers;

    public ChainedRateProvider(List<RateProvider> providers) {
        this.providers = providers.stream()
            .filter(p -> !(p instanceof ChainedRateProvider))
            .sorted(Comparator.comparingInt(RateProvider::getPriority))
            .collect(Collectors.toList());
    }

    @PostConstruct
    public void init() {
        if (providers.isEmpty()) {
            log.warn("No RateProviders found for ChainedRateProvider.");
        } else {
            String providerChain = providers.stream()
                .map(p -> String.format("%s(priority=%d)", p.getProviderName(), p.getPriority()))
                .collect(Collectors.joining(" -> "));
            log.info("Initialized ChainedRateProvider with chain: {}", providerChain);
        }
    }

    @Override
    public Mono<ExchangeRate> getCurrentRate(String fromCurrency, String toCurrency) {
        return Flux.fromIterable(providers)
            .concatMap(provider -> provider.isAvailable()
                .filter(Boolean::booleanValue)
                .doOnNext(available -> log.debug("Provider {} is available, attempting to fetch rate.", provider.getProviderName()))
                .flatMap(available -> provider.getCurrentRate(fromCurrency, toCurrency)
                    .doOnSuccess(rate -> log.info("Successfully got rate from {}", provider.getProviderName()))
                    .doOnError(err -> log.warn("Provider {} failed to get rate for {}->{}. Reason: {}",
                        provider.getProviderName(), fromCurrency, toCurrency, err.getMessage()))
                )
                // Если isAvailable() вернул false или getCurrentRate() вернул ошибку,
                // onErrorResume переключит на пустой Mono, чтобы concatMap перешел к следующему провайдеру.
                .onErrorResume(err -> {
                    log.warn("Switching to next provider due to error in {}: {}", provider.getProviderName(), err.getMessage());
                    return Mono.empty();
                })
            )
            .next() // Берем первый успешный результат
            .switchIfEmpty(Mono.error(new ExternalApiException("All rate providers are unavailable or failed to provide a rate.")));
    }

    @Override
    public Mono<Boolean> isAvailable() {
        return Flux.fromIterable(providers)
            .flatMap(RateProvider::isAvailable)
            .any(Boolean::booleanValue);
    }

    @Override
    public String getProviderName() {
        return "Chained Rate Provider";
    }
} 