package com.reactiverates.application;

import com.reactiverates.domain.model.ConversionRequest;
import com.reactiverates.domain.model.ConversionResult;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.RateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для DefaultCurrencyConversionService")
class DefaultCurrencyConversionServiceTest {

    @Mock
    private RateProvider rateProvider;

    @InjectMocks
    private DefaultCurrencyConversionService conversionService;

    private String fromCurrency;
    private String toCurrency;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        fromCurrency = "EUR";
        toCurrency = "USD";
        amount = new BigDecimal("100.00");
    }

    @Test
    @DisplayName("✅ Успешная конвертация валюты с использованием курса от провайдера")
    void convertCurrency_whenRateProviderSucceeds_thenReturnsCorrectConversion() {
        // Arrange
        ConversionRequest request = ConversionRequest.of(fromCurrency, toCurrency, amount);
        BigDecimal rateValue = new BigDecimal("1.0755");
        ExchangeRate exchangeRate = ExchangeRate.of(fromCurrency, toCurrency, rateValue, "TestProvider");

        when(rateProvider.getCurrentRate(fromCurrency, toCurrency)).thenReturn(Mono.just(exchangeRate));

        // Act
        Mono<ConversionResult> resultMono = conversionService.convertCurrency(request);

        // Assert
        StepVerifier.create(resultMono)
            .assertNext(result -> {
                BigDecimal expectedAmount = amount.multiply(rateValue).setScale(4, RoundingMode.HALF_UP);
                assertThat(result.request()).isEqualTo(request);
                assertThat(result.exchangeRate()).isEqualTo(exchangeRate);
                assertThat(result.convertedAmount()).isEqualByComparingTo(expectedAmount);
            })
            .verifyComplete();

        verify(rateProvider).getCurrentRate(fromCurrency, toCurrency);
    }

    @Test
    @DisplayName("🟰 Конвертация одинаковых валют должна вернуть ту же сумму с курсом 1.0")
    void convertCurrency_whenFromAndToCurrenciesAreSame_thenReturnsAmountWithRateOfOne() {
        // Arrange
        String sameCurrency = "USD";
        ConversionRequest request = ConversionRequest.of(sameCurrency, sameCurrency, amount);

        // Act
        Mono<ConversionResult> resultMono = conversionService.convertCurrency(request);

        // Assert
        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertThat(result.request()).isEqualTo(request);
                assertThat(result.convertedAmount()).isEqualByComparingTo(amount);
                assertThat(result.exchangeRate().rate()).isEqualByComparingTo(BigDecimal.ONE);
                assertThat(result.exchangeRate().providerName()).isEqualTo("Internal");
            })
            .verifyComplete();
        
        verify(rateProvider, never()).getCurrentRate(any(), any());
    }

    @Test
    @DisplayName("❌ Должен вернуть ошибку, если провайдер курсов возвращает ошибку")
    void convertCurrency_whenRateProviderFails_thenReturnsError() {
        // Arrange
        ConversionRequest request = ConversionRequest.of(fromCurrency, toCurrency, amount);
        RuntimeException providerException = new RuntimeException("Provider unavailable");

        when(rateProvider.getCurrentRate(fromCurrency, toCurrency)).thenReturn(Mono.error(providerException));

        // Act
        Mono<ConversionResult> resultMono = conversionService.convertCurrency(request);

        // Assert
        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                                            "Provider unavailable".equals(throwable.getMessage()))
            .verify();
    }

    @Test
    @DisplayName("👍 isCurrencyPairSupported должен вернуть true, если курс получен")
    void isCurrencyPairSupported_whenProviderReturnsRate_thenReturnsTrue() {
        // Arrange
        ExchangeRate exchangeRate = ExchangeRate.of(fromCurrency, toCurrency, BigDecimal.valueOf(1.1), "TestProvider");
        when(rateProvider.getCurrentRate(fromCurrency, toCurrency)).thenReturn(Mono.just(exchangeRate));

        // Act
        Mono<Boolean> resultMono = conversionService.isCurrencyPairSupported(fromCurrency, toCurrency);

        // Assert
        StepVerifier.create(resultMono)
            .expectNext(true)
            .verifyComplete();
    }
    
    @Test
    @DisplayName("👎 isCurrencyPairSupported должен вернуть false, если провайдер вернул ошибку")
    void isCurrencyPairSupported_whenProviderReturnsError_thenReturnsFalse() {
        // Arrange
        when(rateProvider.getCurrentRate(fromCurrency, toCurrency)).thenReturn(Mono.error(new RuntimeException("Unsupported pair")));

        // Act
        Mono<Boolean> resultMono = conversionService.isCurrencyPairSupported(fromCurrency, toCurrency);

        // Assert
        StepVerifier.create(resultMono)
            .expectNext(false)
            .verifyComplete();
    }
}