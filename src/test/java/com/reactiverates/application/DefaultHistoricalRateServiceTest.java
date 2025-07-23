package com.reactiverates.application;

import com.reactiverates.domain.model.Currency;
import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateProvider;
import com.reactiverates.domain.service.HistoricalRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для DefaultHistoricalRateService")
class DefaultHistoricalRateServiceTest {

    @Mock
    private HistoricalRateRepository repository;

    @Mock
    private HistoricalRateProvider provider;

    @InjectMocks
    private DefaultHistoricalRateService service;

    @Captor
    private ArgumentCaptor<Flux<HistoricalExchangeRate>> fluxCaptor;

    @Captor
    private ArgumentCaptor<Set<LocalDate>> datesCaptor;

    private String fromCurrencyCode;
    private String toCurrencyCode;
    private Currency fromCurrency;
    private Currency toCurrency;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        fromCurrencyCode = "EUR";
        toCurrencyCode = "USD";
        fromCurrency = new Currency(fromCurrencyCode, "Dollar", "$");
        toCurrency = new Currency(toCurrencyCode, "Euro", "€");

        // Используем даты, которые не попадают на выходные дни для простоты
        startDate = LocalDate.of(2023, 10, 2); // Понедельник
        endDate = LocalDate.of(2023, 10, 4);   // Среда

        // lenient, чтобы не было UnnecessaryStubbingException
        lenient().when(provider.getProviderName()).thenReturn("TestProvider");
    }

    @Test
    @DisplayName("Должен вернуть ошибку, если дата начала позже даты окончания")
    void getHistoricalRates_whenStartDateIsAfterEndDate_thenThrowsIllegalArgumentException() {
        // Arrange
        LocalDate laterStartDate = endDate.plusDays(1);

        // Act
        Flux<HistoricalExchangeRate> result = service.getHistoricalRates(fromCurrencyCode, toCurrencyCode, laterStartDate, endDate);

        // Assert
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    @DisplayName("Должен вернуть ошибку, если запрашиваются будущие даты")
    void getHistoricalRates_whenStartDateIsInTheFuture_thenThrowsIllegalArgumentException() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(1);

        // Act
        Flux<HistoricalExchangeRate> result = service.getHistoricalRates(fromCurrencyCode, toCurrencyCode, futureDate, futureDate);

        // Assert
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    @DisplayName("Должен вернуть курсы только из репозитория, если все данные есть в кэше")
    void getHistoricalRates_whenAllRatesAreInRepository_thenReturnsRatesFromRepositoryOnly() {
        // Arrange
        List<HistoricalExchangeRate> ratesFromDb = List.of(
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.10), startDate, provider.getProviderName()),
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.11), startDate.plusDays(1), provider.getProviderName()),
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.12), endDate, provider.getProviderName())
        );
        when(repository.findByPeriod(fromCurrencyCode, toCurrencyCode, startDate, endDate))
            .thenReturn(Flux.fromIterable(ratesFromDb));

        // Act
        Flux<HistoricalExchangeRate> result = service.getHistoricalRates(fromCurrencyCode, toCurrencyCode, startDate, endDate);

        // Assert
        StepVerifier.create(result)
            .expectNextCount(3)
            .verifyComplete();

        verify(provider, never()).getHistoricalRatesForDates(any(), any(), any());
        verify(repository, never()).saveAll(any(Flux.class));
    }

    @Test
    @DisplayName("Должен запросить и сохранить все курсы, если в кэше ничего нет")
    void getHistoricalRates_whenNoRatesAreInRepository_thenFetchesAndSavesAllRates() {
        // Arrange
        List<HistoricalExchangeRate> ratesFromApi = List.of(
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.10), startDate, provider.getProviderName()),
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.11), startDate.plusDays(1), provider.getProviderName()),
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.12), endDate, provider.getProviderName())
        );
        Set<LocalDate> expectedMissingDates = Set.of(startDate, startDate.plusDays(1), endDate);

        when(repository.findByPeriod(any(), any(), any(), any())).thenReturn(Flux.empty());
        when(provider.getHistoricalRatesForDates(eq(fromCurrencyCode), eq(toCurrencyCode), eq(expectedMissingDates)))
            .thenReturn(Flux.fromIterable(ratesFromApi));// Мок saveAll должен возвращать тот же поток, который получает, чтобы симулировать успешное сохранение
        when(repository.saveAll(any(Flux.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        Flux<HistoricalExchangeRate> result = service.getHistoricalRates(fromCurrencyCode, toCurrencyCode, startDate, endDate);

        // Assert
        StepVerifier.create(result)
            .expectNextSequence(ratesFromApi)
            .verifyComplete();

        verify(provider, times(1)).getHistoricalRatesForDates(eq(fromCurrencyCode), eq(toCurrencyCode), datesCaptor.capture());
        assertThat(datesCaptor.getValue()).containsExactlyInAnyOrderElementsOf(expectedMissingDates);

        verify(repository, times(1)).saveAll(fluxCaptor.capture());
        StepVerifier.create(fluxCaptor.getValue())
            .expectNextSequence(ratesFromApi)
            .verifyComplete();
    }

    @Test
    @DisplayName("Должен запросить только недостающие курсы и объединить их с кэшированными")
    void getHistoricalRates_whenSomeRatesAreInRepository_thenFetchesAndSavesMissingRates() {
        // Arrange
        HistoricalExchangeRate rateFromDb = new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.10), startDate, provider.getProviderName());
        List<HistoricalExchangeRate> ratesFromApi = List.of(
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.11), startDate.plusDays(1), provider.getProviderName()),
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.12), endDate, provider.getProviderName())
        );
        Set<LocalDate> expectedMissingDates = Set.of(startDate.plusDays(1), endDate);

        when(repository.findByPeriod(fromCurrencyCode, toCurrencyCode, startDate, endDate)).thenReturn(Flux.just(rateFromDb));
        when(provider.getHistoricalRatesForDates(eq(fromCurrencyCode), eq(toCurrencyCode), eq(expectedMissingDates)))
            .thenReturn(Flux.fromIterable(ratesFromApi));
        when(repository.saveAll(any(Flux.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        Flux<HistoricalExchangeRate> result = service.getHistoricalRates(fromCurrencyCode, toCurrencyCode, startDate, endDate);

        // Assert
        List<HistoricalExchangeRate> expectedSortedResult = List.of(rateFromDb, ratesFromApi.get(0), ratesFromApi.get(1));
        StepVerifier.create(result)
            .expectNextSequence(expectedSortedResult)
            .verifyComplete();

        verify(provider, times(1)).getHistoricalRatesForDates(eq(fromCurrencyCode), eq(toCurrencyCode), datesCaptor.capture());
        assertThat(datesCaptor.getValue()).containsExactlyInAnyOrderElementsOf(expectedMissingDates);

        verify(repository, times(1)).saveAll(any(Flux.class));
    }

    @Test
    @DisplayName("Должен вернуть ошибку, если провайдер не отвечает (без кэшированных данных)")
    void getHistoricalRates_whenProviderFails_thenReturnsError() {
        // Arrange
        HistoricalExchangeRate rateFromDb = new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.10), startDate, provider.getProviderName());
        RuntimeException apiException = new RuntimeException("API is down");

        when(repository.findByPeriod(fromCurrencyCode, toCurrencyCode, startDate, endDate)).thenReturn(Flux.just(rateFromDb));
        when(provider.getHistoricalRatesForDates(any(), any(), any())).thenReturn(Flux.error(apiException));
        when(repository.saveAll(any(Flux.class))).thenReturn(Flux.error(apiException));

        // Act
        Flux<HistoricalExchangeRate> result = service.getHistoricalRates(fromCurrencyCode, toCurrencyCode, startDate, endDate);

        // Assert
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable instanceof RuntimeException && "API is down".equals(throwable.getMessage()))
            .verify();
    }
    
    @Test
    @DisplayName("Должен завершиться без данных, если сохранение в репозиторий не удалось")
    void getHistoricalRates_whenRepositorySaveFails_shouldCompleteWithoutData() {
        // Arrange
        List<HistoricalExchangeRate> ratesFromApi = List.of(
            new HistoricalExchangeRate(fromCurrency, toCurrency, BigDecimal.valueOf(1.10), startDate, provider.getProviderName())
        );

        when(repository.findByPeriod(any(), any(), any(), any())).thenReturn(Flux.empty());
        when(provider.getHistoricalRatesForDates(any(), any(), any())).thenReturn(Flux.fromIterable(ratesFromApi));
        when(repository.saveAll(any(Flux.class))).thenReturn(Flux.empty());

        // Act
        Flux<HistoricalExchangeRate> result = service.getHistoricalRates(fromCurrencyCode, toCurrencyCode, startDate, startDate);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();
    }
}