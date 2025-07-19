package com.reactiverates.infrastructure.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.reactiverates.domain.model.Currency;
import com.reactiverates.infrastructure.client.dto.ExchangeRateApiResponse;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateClientTest {
    @Mock
    private WebClient webClient;

    @Mock
    private RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;
    
    private ExchangeRateClient exchangeRateClient;

    @BeforeEach
    void setUp() {
        exchangeRateClient = new ExchangeRateClient(webClient);
    }

    @Test
    void shouldGetCurrentExchangeRate_Success() {
        Currency from = Currency.USD;
        Currency to = Currency.EUR;
        ExchangeRateApiResponse.QueryInfo queryInfo = new ExchangeRateApiResponse.QueryInfo(
            from.code(), 
            to.code(), 
            BigDecimal.ONE
        );
        ExchangeRateApiResponse.RateInfo rateInfo = new ExchangeRateApiResponse.RateInfo(BigDecimal.valueOf(0.85123));
        ExchangeRateApiResponse mockResponse = new ExchangeRateApiResponse(true, queryInfo, rateInfo, LocalDate.now(), BigDecimal.valueOf(0.85123));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRateApiResponse.class)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(exchangeRateClient.getCurrentRate(from.code(), to.code()))
            .expectNextMatches(exchangeRate -> {
                return exchangeRate.fromCurrency().code().equals(from.code()) &&
                       exchangeRate.toCurrency().code().equals(to.code()) &&
                       exchangeRate.rate().compareTo(BigDecimal.valueOf(0.85123)) == 0 &&
                       exchangeRate.timestamp() != null;
            })
            .verifyComplete();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(Function.class));
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(ExchangeRateApiResponse.class);
    }
}
