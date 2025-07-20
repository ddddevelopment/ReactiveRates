package com.reactiverates.infrastructure.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.reactiverates.infrastructure.client.dto.UniRateApiResponse;
import com.reactiverates.infrastructure.config.UniRateApiConfig;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class UniRateApiClientTest {
    @Mock
    private WebClient webClient;

    @Mock
    private RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;
    
    @Mock
    private UniRateApiConfig config;
    
    private UniRateApiClient exchangeRateClient;

    @BeforeEach
    void setUp() {
        when(config.hasApiKey()).thenReturn(true);
        exchangeRateClient = new UniRateApiClient(webClient, config);
    }

    @Test
    void shouldGetCurrentExchangeRate_Success() {
        // --- Arrange ---
        String fromCurrency = "USD";
        String toCurrency = "EUR";
        BigDecimal rate = new BigDecimal("0.85");
        UniRateApiResponse mockResponse = new UniRateApiResponse(
            fromCurrency,
            toCurrency,
            rate,
            new BigDecimal("0.85"),
            BigDecimal.ONE
        );

        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UniRateApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // --- Act & Assert ---
        StepVerifier.create(exchangeRateClient.getCurrentRate(fromCurrency, toCurrency))
            .expectNextMatches(exchangeRate -> 
                exchangeRate.fromCurrency().code().equals(fromCurrency) &&
                exchangeRate.toCurrency().code().equals(toCurrency) &&
                exchangeRate.rate().compareTo(rate) == 0 &&
                exchangeRate.timestamp() != null
            )
            .verifyComplete();

        // --- Verify ---
        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(Function.class));
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(UniRateApiResponse.class);
    }
}
