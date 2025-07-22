package com.reactiverates.application;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateProvider;
import com.reactiverates.domain.service.HistoricalRateRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultHistoricalRatesService Tests")
class DefaultHistoricalRatesServiceTest {

    @Mock
    private HistoricalRateRepository repository;
    
    @Mock 
    private HistoricalRateProvider provider;
    
    private DefaultHistoricalRateService service;
    
    private final LocalDate startDate = LocalDate.of(2024, 1, 15); 
    private final LocalDate endDate = LocalDate.of(2024, 1, 17);
    private final String fromCurrency = "USD";
    private final String toCurrency = "EUR";
    
    @BeforeEach
    void setUp() {
        when(provider.getProviderName()).thenReturn("TestProvider");
        service = new DefaultHistoricalRateService(repository, provider);
    }

    @Nested
    @DisplayName("getHistoricalRates method")
    class GetHistoricalRatesMethod {
        
        @Test
        @DisplayName("должен объединять данные из БД и API для недостающих дат")
        void shouldCombineDataFromDbAndApiForMissingDates() {
            // ARRANGE
            HistoricalExchangeRate dbRate = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, 
                new BigDecimal("1.0850"), 
                LocalDate.of(2024, 1, 15), 
                "Database"
            );
            
            HistoricalExchangeRate apiRate1 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency,
                new BigDecimal("1.0860"), 
                LocalDate.of(2024, 1, 16),
                "ExchangeRateAPI"
            );
            
            HistoricalExchangeRate apiRate2 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency,
                new BigDecimal("1.0870"), 
                LocalDate.of(2024, 1, 17),
                "ExchangeRateAPI"
            );
            
            when(repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Flux.just(dbRate));
                
            when(repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Mono.just(Set.of(LocalDate.of(2024, 1, 15))));
                
            when(provider.getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any()))
                .thenReturn(Flux.just(apiRate1, apiRate2));
                
            when(repository.save(any(HistoricalExchangeRate.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, startDate, endDate
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(dbRate)    
                .expectNext(apiRate1)  
                .expectNext(apiRate2)  
                .verifyComplete();     
            
            verify(repository, times(1)).findByPeriod(fromCurrency, toCurrency, startDate, endDate);
            verify(repository, times(1)).findExistingDates(fromCurrency, toCurrency, startDate, endDate);
            verify(provider, times(1)).getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any());
            verify(repository, times(2)).save(any(HistoricalExchangeRate.class));
        }

        @Test
        @DisplayName("должен возвращать ошибку когда startDate после endDate")
        void shouldReturnErrorWhenStartDateAfterEndDate() {
            // ARRANGE
            LocalDate invalidStartDate = LocalDate.of(2024, 1, 20);
            LocalDate invalidEndDate = LocalDate.of(2024, 1, 15);
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, invalidStartDate, invalidEndDate
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
                
            verify(repository, never()).findByPeriod(any(), any(), any(), any());
            verify(repository, never()).findExistingDates(any(), any(), any(), any());
            verify(provider, never()).getHistoricalRatesForDates(any(), any(), any());
            verify(repository, never()).save(any());
        }
        
        @Test
        @DisplayName("должен возвращать ошибку для будущих дат")
        void shouldReturnErrorForFutureDates() {
            // ARRANGE
            LocalDate futureStartDate = LocalDate.now().plusDays(1);
            LocalDate futureEndDate = LocalDate.now().plusDays(5);
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, futureStartDate, futureEndDate
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
                
            verify(repository, never()).findByPeriod(any(), any(), any(), any());
            verify(repository, never()).findExistingDates(any(), any(), any(), any());
            verify(provider, never()).getHistoricalRatesForDates(any(), any(), any());
            verify(repository, never()).save(any());
        }
        
        @Test
        @DisplayName("должен возвращать только данные из БД когда все даты уже есть")
        void shouldReturnOnlyDatabaseDataWhenAllDatesExist() {
            // ARRANGE
            HistoricalExchangeRate dbRate1 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0850"), 
                LocalDate.of(2024, 1, 15), "Database"
            );
            HistoricalExchangeRate dbRate2 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0860"), 
                LocalDate.of(2024, 1, 16), "Database"
            );
            
            when(repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Flux.just(dbRate1, dbRate2));
                
            when(repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Mono.just(Set.of(
                    LocalDate.of(2024, 1, 15), 
                    LocalDate.of(2024, 1, 16),
                    LocalDate.of(2024, 1, 17)
                )));
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, startDate, endDate
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(dbRate1)
                .expectNext(dbRate2)
                .verifyComplete();
                
            verify(repository, times(1)).findByPeriod(fromCurrency, toCurrency, startDate, endDate);
            verify(repository, times(1)).findExistingDates(fromCurrency, toCurrency, startDate, endDate);
            verify(provider, never()).getHistoricalRatesForDates(any(), any(), any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("должен исключать выходные при поиске недостающих дат")
        void shouldExcludeWeekendsWhenSearchingMissingDates() {
            // ARRANGE
            LocalDate friday = LocalDate.of(2024, 1, 19);
            LocalDate monday = LocalDate.of(2024, 1, 22);
            
            HistoricalExchangeRate dbRate = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0850"), friday, "Database"
            );
            
            HistoricalExchangeRate apiRate = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0860"), monday, "ExchangeRateAPI"
            );
            
            when(repository.findByPeriod(fromCurrency, toCurrency, friday, monday))
                .thenReturn(Flux.just(dbRate));
                
            when(repository.findExistingDates(fromCurrency, toCurrency, friday, monday))
                .thenReturn(Mono.just(Set.of(friday)));
                
            when(provider.getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any()))
                .thenReturn(Flux.just(apiRate));
                
            when(repository.save(any(HistoricalExchangeRate.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, friday, monday
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(dbRate)
                .expectNext(apiRate) 
                .verifyComplete();
                
            verify(provider).getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), 
                argThat(dates -> dates.size() == 1 && dates.contains(monday)));
        }

        @Test
        @DisplayName("должен передавать ошибку API когда нет данных в БД")
        void shouldPropagateApiErrorWhenNoDataInDatabase() {
            // ARRANGE
            when(repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Flux.empty());
                
            when(repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Mono.just(Set.of()));
                
            when(provider.getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any()))
                .thenReturn(Flux.error(new RuntimeException("API недоступен")));
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, startDate, endDate
            );
            
            // ASSERT
            StepVerifier.create(result)
                .verifyComplete();
                
            verify(repository, times(1)).findByPeriod(fromCurrency, toCurrency, startDate, endDate);
            verify(provider, times(1)).getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any());
        }

        @Test
        @DisplayName("должен возвращать данные из БД при ошибке API")
        void shouldReturnDatabaseDataWhenApiError() {
            // ARRANGE
            HistoricalExchangeRate dbRate = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0850"), 
                LocalDate.of(2024, 1, 15), "Database"
            );
            
            when(repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Flux.just(dbRate));
                
            when(repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Mono.just(Set.of(LocalDate.of(2024, 1, 15))));
                
            when(provider.getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any()))
                .thenReturn(Flux.error(new RuntimeException("API недоступен")));
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, startDate, endDate
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(dbRate)
                .verifyComplete();
                
            verify(repository, times(1)).findByPeriod(fromCurrency, toCurrency, startDate, endDate);
            verify(provider, times(1)).getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any());
            verify(repository, never()).save(any());
        }

        @Test 
        @DisplayName("должен правильно сортировать результаты по дате")
        void shouldSortResultsByDate() {
            // ARRANGE
            HistoricalExchangeRate dbRate2 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0860"), 
                LocalDate.of(2024, 1, 16), "Database"
            );
            
            HistoricalExchangeRate dbRate3 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0870"), 
                LocalDate.of(2024, 1, 17), "Database"
            );
            
            HistoricalExchangeRate apiRate1 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0850"), 
                LocalDate.of(2024, 1, 15), "ExchangeRateAPI"
            );
            
            when(repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Flux.just(dbRate3, dbRate2));
                
            when(repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Mono.just(Set.of(
                    LocalDate.of(2024, 1, 16), 
                    LocalDate.of(2024, 1, 17)
                )));
                
            when(provider.getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any()))
                .thenReturn(Flux.just(apiRate1));
                
            when(repository.save(any(HistoricalExchangeRate.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, startDate, endDate
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(apiRate1) 
                .expectNext(dbRate2)   
                .expectNext(dbRate3)   
                .verifyComplete();
        }

        @Test
        @DisplayName("должен обрабатывать период из одного дня")
        void shouldHandleSingleDayPeriod() {
            // ARRANGE
            LocalDate singleDay = LocalDate.of(2024, 1, 15);
            
            HistoricalExchangeRate apiRate = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0850"), singleDay, "ExchangeRateAPI"
            );
            
            when(repository.findByPeriod(fromCurrency, toCurrency, singleDay, singleDay))
                .thenReturn(Flux.empty());
                
            when(repository.findExistingDates(fromCurrency, toCurrency, singleDay, singleDay))
                .thenReturn(Mono.just(Set.of()));
                
            when(provider.getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any()))
                .thenReturn(Flux.just(apiRate));
                
            when(repository.save(any(HistoricalExchangeRate.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, singleDay, singleDay
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(apiRate)
                .verifyComplete();
                
            verify(provider).getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), 
                argThat(dates -> dates.size() == 1 && dates.contains(singleDay)));
        }

        @Test
        @DisplayName("должен продолжать работу при ошибке сохранения отдельных записей")
        void shouldContinueWorkWhenSomeRecordsFailToSave() {
            // ARRANGE
            HistoricalExchangeRate apiRate1 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0860"), 
                LocalDate.of(2024, 1, 16), "ExchangeRateAPI"
            );
            
            HistoricalExchangeRate apiRate2 = HistoricalExchangeRate.of(
                fromCurrency, toCurrency, new BigDecimal("1.0870"), 
                LocalDate.of(2024, 1, 17), "ExchangeRateAPI"
            );
            
            when(repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Flux.empty());
                
            when(repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Mono.just(Set.of()));
                
            when(provider.getHistoricalRatesForDates(eq(fromCurrency), eq(toCurrency), any()))
                .thenReturn(Flux.just(apiRate1, apiRate2));
            
            when(repository.save(apiRate1))
                .thenReturn(Mono.just(apiRate1));
            when(repository.save(apiRate2))
                .thenReturn(Mono.error(new RuntimeException("Database constraint violation")));
            
            // ACT
            Flux<HistoricalExchangeRate> result = service.getHistoricalRates(
                fromCurrency, toCurrency, startDate, endDate
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(apiRate1)
                .expectNext(apiRate2)
                .verifyComplete();
                
            verify(repository, times(1)).save(apiRate1);
            verify(repository, times(1)).save(apiRate2);
        }
    }

    @Nested
    @DisplayName("getHistoricalDataCount method")
    class GetHistoricalDataCountMethod {
        
        @Test
        @DisplayName("должен возвращать количество исторических записей для валютной пары")
        void shouldReturnCountForValidCurrencyPair() {
            // ARRANGE
            long count = 43L;
            when(repository.countByPair(fromCurrency, toCurrency)).thenReturn(Mono.just(count));

            // ACT
            Mono<Long> result = service.getHistoricalDataCount(fromCurrency, toCurrency);

            // ASSERT
            StepVerifier.create(result)
                .expectNext(count)
                .verifyComplete();
            
            verify(repository, times(1)).countByPair(fromCurrency, toCurrency);
        }

        @Test
        @DisplayName("должен возвращать 0 когда записей нет")
        void shouldReturnZeroWhenNoDataExists() {
            // ARRANGE
            when(repository.countByPair(fromCurrency, toCurrency)).thenReturn(Mono.just(0L));

            // ACT
            Mono<Long> result = service.getHistoricalDataCount(fromCurrency, toCurrency);

            // ASSERT
            StepVerifier.create(result).expectNext(0L).verifyComplete();
            verify(repository, times(1)).countByPair(fromCurrency, toCurrency);
        }
    }

    @Nested
    @DisplayName("isDataCompleteForPeriod method")
    class IsDataCompleteForPeriodMethod {
        
        @Test
        @DisplayName("должен возвращать true когда все даты есть в бд")
        void shouldReturnTrueWhenAllDatesExist() {
            // ARRANGE
            LocalDate jan15 = LocalDate.of(2024, 1, 15);
            LocalDate jan16 = LocalDate.of(2024, 1, 16);
            LocalDate jan17 = LocalDate.of(2024, 1, 17);
            Set<LocalDate> dates = Set.of(jan15, jan16, jan17);
            when(repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate)).thenReturn(Mono.just(dates));

            // ACT
            Mono<Boolean> result = service.isDataCompleteForPeriod(fromCurrency, toCurrency, startDate, endDate);

            // ASSERT
            StepVerifier.create(result).expectNext(true).verifyComplete();
            verify(repository, times(1)).findExistingDates(fromCurrency, toCurrency, startDate, endDate);
        }

        @Test
        @DisplayName("должен возвращать false когда не хватает данных")
        void shouldReturnFalseWhenSomeDatesAreMissing() {
            // ARRANGE
            LocalDate jan16 = LocalDate.of(2024, 1, 16);
            LocalDate jan17 = LocalDate.of(2024, 1, 17);
            Set<LocalDate> dates = Set.of(jan16, jan17);
            when(repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate)).thenReturn(Mono.just(dates));

            // ACT
            Mono<Boolean> result = service.isDataCompleteForPeriod(fromCurrency, toCurrency, startDate, endDate);

            // ASSERT
            StepVerifier.create(result).expectNext(false).verifyComplete();
            verify(repository, times(1)).findExistingDates(fromCurrency, toCurrency, startDate, endDate);
        }
    }

    @Nested
    @DisplayName("getDataDateRange method")
    class GetDataDateRangeMethod {
        
        @Test
        @DisplayName("должен возвращать диапазон дат когда данные существуют")
        void shouldReturnDateRangeWhenDataExists() {
            // ARRANGE
            LocalDate earliestDate = LocalDate.of(2024, 1, 1);
            LocalDate latestDate = LocalDate.of(2024, 12, 31);
            
            when(repository.findEarliestDate(fromCurrency, toCurrency))
                .thenReturn(Mono.just(earliestDate));
            when(repository.findLatestDate(fromCurrency, toCurrency))
                .thenReturn(Mono.just(latestDate));
            
            // ACT
            Mono<LocalDate[]> result = service.getDataDateRange(fromCurrency, toCurrency);
            
            // ASSERT
            StepVerifier.create(result)
                .assertNext(dates -> {
                    assert dates.length == 2;
                    assert dates[0].equals(earliestDate);
                    assert dates[1].equals(latestDate);
                })
                .verifyComplete();
                
            verify(repository, times(1)).findEarliestDate(fromCurrency, toCurrency);
            verify(repository, times(1)).findLatestDate(fromCurrency, toCurrency);
        }

        @Test
        @DisplayName("должен возвращать пустой массив когда данных нет")
        void shouldReturnEmptyArrayWhenNoDataExists() {
            // ARRANGE
            when(repository.findEarliestDate(fromCurrency, toCurrency))
                .thenReturn(Mono.empty());
            when(repository.findLatestDate(fromCurrency, toCurrency))
                .thenReturn(Mono.empty());
            
            // ACT
            Mono<LocalDate[]> result = service.getDataDateRange(fromCurrency, toCurrency);
            
            // ASSERT
            StepVerifier.create(result)
                .assertNext(dates -> {
                    assert dates.length == 0;
                })
                .verifyComplete();
                
            verify(repository, times(1)).findEarliestDate(fromCurrency, toCurrency);
            verify(repository, times(1)).findLatestDate(fromCurrency, toCurrency);
        }
    }
} 