package com.reactiverates.infrastructure.persistence;

import com.reactiverates.domain.model.Currency;
import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.infrastructure.persistence.entity.HistoricalExchangeRateEntity;
import com.reactiverates.infrastructure.persistence.mapper.HistoricalExchangeRateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultHistoricalRateRepository Unit Tests")
class DefaultHistoricalRateRepositoryTest {

    @Mock
    private SpringDataHistoricalRateRepository dataRepository;
    
    @Mock
    private HistoricalExchangeRateMapper mapper;
    
    @InjectMocks
    private DefaultHistoricalRateRepository repository;
    
    @Captor
    private ArgumentCaptor<List<HistoricalExchangeRateEntity>> entitiesCaptor;
    
    private HistoricalExchangeRate domainRate;
    private HistoricalExchangeRateEntity entity;
    private final LocalDate testDate = LocalDate.of(2024, 1, 15);
    private final String fromCurrency = "USD";
    private final String toCurrency = "EUR";
    private final BigDecimal rate = new BigDecimal("1.0850");
    private final String providerName = "TestProvider";
    
    @BeforeEach
    void setUp() {
        domainRate = HistoricalExchangeRate.of(
            Currency.of(fromCurrency),
            Currency.of(toCurrency),
            rate,
            testDate,
            providerName
        );
        
        entity = HistoricalExchangeRateEntity.of(
            fromCurrency,
            toCurrency, 
            rate,
            testDate,
            providerName
        );
    }

    @Nested
    @DisplayName("save method")
    class SaveMethod {
        
        @Test
        @DisplayName("должен делегировать к dataRepository.save()")
        void shouldDelegateToUpsertRateWithCorrectParameters() {
            // ARRANGE
            when(dataRepository.save(entity)).thenReturn(Mono.just(entity));
            
            // ACT
            Mono<HistoricalExchangeRate> result = repository.save(domainRate);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(domainRate)
                .verifyComplete();
                
            verify(dataRepository, times(1)).save(entity);
        }
        
        @Test
        @DisplayName("должен возвращать исходный domain объект после успешного сохранения")
        void shouldReturnOriginalDomainObjectAfterSuccessfulSave() {
            // ARRANGE
            when(dataRepository.save(entity))
                .thenReturn(Mono.just(entity));
            
            // ACT
            Mono<HistoricalExchangeRate> result = repository.save(domainRate);
            
            // ASSERT
            StepVerifier.create(result)
                .assertNext(savedRate -> {
                    assertThat(savedRate).isSameAs(domainRate);
                    assertThat(savedRate.fromCurrency().code()).isEqualTo(fromCurrency);
                    assertThat(savedRate.toCurrency().code()).isEqualTo(toCurrency);
                    assertThat(savedRate.rate()).isEqualTo(rate);
                    assertThat(savedRate.date()).isEqualTo(testDate);
                    assertThat(savedRate.providerName()).isEqualTo(providerName);
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("должен передавать ошибку от dataRepository")
        void shouldPropagateErrorFromDataRepository() {
            // ARRANGE
            RuntimeException expectedError = new RuntimeException("Database constraint violation");
            when(dataRepository.save(entity))
                .thenReturn(Mono.error(expectedError));
            
            // ACT
            Mono<HistoricalExchangeRate> result = repository.save(domainRate);
            
            // ASSERT
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("saveAll method")
    class SaveAllMethod {
        
        private List<HistoricalExchangeRate> domainRates;
        private List<HistoricalExchangeRateEntity> entities;
        
        @BeforeEach
        void setUp() {
            domainRates = List.of(
                HistoricalExchangeRate.of(Currency.of("USD"), Currency.of("EUR"), 
                    new BigDecimal("1.0850"), LocalDate.of(2024, 1, 15), "Provider1"),
                HistoricalExchangeRate.of(Currency.of("USD"), Currency.of("EUR"), 
                    new BigDecimal("1.0860"), LocalDate.of(2024, 1, 16), "Provider1"),
                HistoricalExchangeRate.of(Currency.of("GBP"), Currency.of("USD"), 
                    new BigDecimal("1.2650"), LocalDate.of(2024, 1, 15), "Provider2")
            );
            
            entities = List.of(
                HistoricalExchangeRateEntity.of("USD", "EUR", new BigDecimal("1.0850"), LocalDate.of(2024, 1, 15), "Provider1"),
                HistoricalExchangeRateEntity.of("USD", "EUR", new BigDecimal("1.0860"), LocalDate.of(2024, 1, 16), "Provider1"),
                HistoricalExchangeRateEntity.of("GBP", "USD", new BigDecimal("1.2650"), LocalDate.of(2024, 1, 15), "Provider2")
            );
        }
        
        @Test
        @DisplayName("должен возвращать пустой Flux когда input пустой")
        void shouldReturnEmptyFluxWhenInputIsEmpty() {
            // ACT
            Flux<HistoricalExchangeRate> result = repository.saveAll(Flux.empty());
            
            // ASSERT
            StepVerifier.create(result)
                .verifyComplete();
                
            verifyNoInteractions(dataRepository, mapper);
        }
        
        @Test
        @DisplayName("должен использовать batch insert через dataRepository.saveAll()")
        void shouldUseBatchInsertThroughDataRepositorySaveAll() {
            // ARRANGE
            when(mapper.toEntity(domainRates.get(0))).thenReturn(entities.get(0));
            when(mapper.toEntity(domainRates.get(1))).thenReturn(entities.get(1));
            when(mapper.toEntity(domainRates.get(2))).thenReturn(entities.get(2));
            
            when(dataRepository.saveAll(any(List.class)))
                .thenReturn(Flux.fromIterable(entities));
                
            when(mapper.toDomain(entities.get(0))).thenReturn(domainRates.get(0));
            when(mapper.toDomain(entities.get(1))).thenReturn(domainRates.get(1));
            when(mapper.toDomain(entities.get(2))).thenReturn(domainRates.get(2));
            
            // ACT
            Flux<HistoricalExchangeRate> result = repository.saveAll(Flux.fromIterable(domainRates));
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(domainRates.get(0))
                .expectNext(domainRates.get(1))
                .expectNext(domainRates.get(2))
                .verifyComplete();
            
            verify(dataRepository).saveAll(any(List.class));
            verify(mapper, times(3)).toEntity(any());
            verify(mapper, times(3)).toDomain(any());
        }
        
        @Test
        @DisplayName("должен использовать fallback на individual saves при ошибке batch insert")
        void shouldFallbackToIndividualSavesOnBatchInsertError() {
            // ARRANGE
            when(mapper.toEntity(any())).thenReturn(entities.get(0), entities.get(1), entities.get(2));

            RuntimeException batchError = new RuntimeException("Duplicate key constraint violation");
            when(dataRepository.saveAll(any(List.class))).thenReturn(Flux.error(batchError));
            when(dataRepository.save(entities.get(0))).thenReturn(Mono.just(entities.get(0)));
            when(dataRepository.save(entities.get(1))).thenReturn(Mono.just(entities.get(1)));
            when(dataRepository.save(entities.get(2))).thenReturn(Mono.just(entities.get(2)));

            // ACT
            Flux<HistoricalExchangeRate> result = repository.saveAll(Flux.fromIterable(domainRates));

            // ASSERT
            StepVerifier.create(result)
                .expectNext(domainRates.get(0))
                .expectNext(domainRates.get(1))
                .expectNext(domainRates.get(2))
                .verifyComplete();

            verify(dataRepository).saveAll(any(List.class));
            verify(dataRepository, times(3)).save(any(HistoricalExchangeRateEntity.class));
            verify(mapper, times(3)).toEntity(any());
            verify(mapper, never()).toDomain(any());
        }
        
        @Test
        @DisplayName("должен завершаться ошибкой если individual save fails в fallback режиме")
        void shouldFailWhenIndividualSaveFailsInFallbackMode() {
            // ARRANGE
            when(mapper.toEntity(any())).thenReturn(entities.get(0), entities.get(1));

            when(dataRepository.saveAll(any(List.class))).thenReturn(Flux.error(new RuntimeException("Batch failed")));
            when(dataRepository.save(entities.get(0))).thenReturn(Mono.just(entities.get(0)));
            when(dataRepository.save(entities.get(1))).thenReturn(Mono.error(new RuntimeException("Individual save failed")));

            List<HistoricalExchangeRate> partialRates = List.of(domainRates.get(0), domainRates.get(1));

            // ACT
            Flux<HistoricalExchangeRate> result = repository.saveAll(Flux.fromIterable(partialRates));

            // ASSERT
            StepVerifier.create(result)
                .expectNext(domainRates.get(0))
                .expectError(RuntimeException.class)
                .verify();
        }
        
        @Test
        @DisplayName("должен правильно собирать Flux в List перед batch операцией")
        void shouldCollectFluxToListBeforeBatchOperation() {
            // ARRANGE
            when(mapper.toEntity(any())).thenReturn(entities.get(0));
            when(dataRepository.saveAll(any(List.class))).thenReturn(Flux.fromIterable(entities.subList(0, 1)));
            when(mapper.toDomain(any())).thenReturn(domainRates.get(0));
            
            // ACT
            Flux<HistoricalExchangeRate> result = repository.saveAll(
                Flux.fromIterable(domainRates.subList(0, 1))
            );
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(domainRates.get(0))
                .verifyComplete();
            
            verify(dataRepository).saveAll(any(List.class));
        }
    }

    @Nested
    @DisplayName("Find methods with mapping")
    class FindMethodsWithMapping {
        
        @Test
        @DisplayName("findByPeriod() должен делегировать к dataRepository и мапить результат")
        void shouldDelegateAndMapResultForFindByPeriod() {
            // ARRANGE
            LocalDate startDate = LocalDate.of(2024, 1, 10);
            LocalDate endDate = LocalDate.of(2024, 1, 20);
            
            when(dataRepository.findByFromCurrencyAndToCurrencyAndDateBetweenOrderByDateAsc(
                fromCurrency, toCurrency, startDate, endDate
            )).thenReturn(Flux.just(entity));
            
            when(mapper.toDomain(entity)).thenReturn(domainRate);
            
            // ACT
            Flux<HistoricalExchangeRate> result = repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(domainRate)
                .verifyComplete();
            
            verify(dataRepository).findByFromCurrencyAndToCurrencyAndDateBetweenOrderByDateAsc(
                fromCurrency, toCurrency, startDate, endDate);
            verify(mapper).toDomain(entity);
        }
        
        @Test
        @DisplayName("findByDate() должен делегировать к dataRepository и мапить результат")
        void shouldDelegateAndMapResultForFindByDate() {
            // ARRANGE
            when(dataRepository.findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, testDate))
                .thenReturn(Mono.just(entity));
            when(mapper.toDomain(entity)).thenReturn(domainRate);
            
            // ACT
            Mono<HistoricalExchangeRate> result = repository.findByDate(fromCurrency, toCurrency, testDate);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(domainRate)
                .verifyComplete();
            
            verify(dataRepository).findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, testDate);
            verify(mapper).toDomain(entity);
        }
        
        @Test
        @DisplayName("findByDate() должен возвращать пустой Mono если не найдено")
        void shouldReturnEmptyMonoWhenNotFound() {
            // ARRANGE
            when(dataRepository.findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, testDate))
                .thenReturn(Mono.empty());
            
            // ACT
            Mono<HistoricalExchangeRate> result = repository.findByDate(fromCurrency, toCurrency, testDate);
            
            // ASSERT
            StepVerifier.create(result)
                .verifyComplete();
            
            verify(mapper, never()).toDomain(any());
        }
    }

    @Nested
    @DisplayName("Collection operations")
    class CollectionOperations {
        
        @Test
        @DisplayName("findExistingDates() должен собирать Flux дат в Set")
        void shouldCollectFluxDatesIntoSet() {
            // ARRANGE
            LocalDate startDate = LocalDate.of(2024, 1, 10);
            LocalDate endDate = LocalDate.of(2024, 1, 20);
            LocalDate date1 = LocalDate.of(2024, 1, 15);
            LocalDate date2 = LocalDate.of(2024, 1, 18);
            LocalDate date3 = LocalDate.of(2024, 1, 15); // duplicate
            
            when(dataRepository.findExistingDatesBetween(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Flux.just(date1, date2, date3));
            
            // ACT
            Mono<Set<LocalDate>> result = repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate);
            
            // ASSERT
            StepVerifier.create(result)
                .assertNext(dates -> {
                    assertThat(dates).hasSize(2); // Set удаляет дубликаты
                    assertThat(dates).containsExactlyInAnyOrder(date1, date2);
                })
                .verifyComplete();
            
            verify(dataRepository).findExistingDatesBetween(fromCurrency, toCurrency, startDate, endDate);
        }
        
        @Test
        @DisplayName("findExistingDates() должен возвращать пустой Set если дат нет")
        void shouldReturnEmptySetWhenNoDatesFound() {
            // ARRANGE
            LocalDate startDate = LocalDate.of(2024, 1, 10);
            LocalDate endDate = LocalDate.of(2024, 1, 20);
            
            when(dataRepository.findExistingDatesBetween(fromCurrency, toCurrency, startDate, endDate))
                .thenReturn(Flux.empty());
            
            // ACT
            Mono<Set<LocalDate>> result = repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate);
            
            // ASSERT
            StepVerifier.create(result)
                .assertNext(dates -> assertThat(dates).isEmpty())
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Simple delegation methods")
    class SimpleDelegationMethods {
        
        @Test
        @DisplayName("countByPair() должен делегировать к dataRepository")
        void shouldDelegateToDataRepositoryForCountByPair() {
            // ARRANGE
            long expectedCount = 42L;
            when(dataRepository.countByFromCurrencyAndToCurrency(fromCurrency, toCurrency))
                .thenReturn(Mono.just(expectedCount));
            
            // ACT
            Mono<Long> result = repository.countByPair(fromCurrency, toCurrency);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(expectedCount)
                .verifyComplete();
            
            verify(dataRepository).countByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
        }
        
        @Test
        @DisplayName("findEarliestDate() должен делегировать к dataRepository")
        void shouldDelegateToDataRepositoryForFindEarliestDate() {
            // ARRANGE
            LocalDate earliestDate = LocalDate.of(2023, 1, 1);
            when(dataRepository.findEarliestDateByPair(fromCurrency, toCurrency))
                .thenReturn(Mono.just(earliestDate));
            
            // ACT
            Mono<LocalDate> result = repository.findEarliestDate(fromCurrency, toCurrency);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(earliestDate)
                .verifyComplete();
            
            verify(dataRepository).findEarliestDateByPair(fromCurrency, toCurrency);
        }
        
        @Test
        @DisplayName("findLatestDate() должен делегировать к dataRepository")
        void shouldDelegateToDataRepositoryForFindLatestDate() {
            // ARRANGE
            LocalDate latestDate = LocalDate.of(2024, 12, 31);
            when(dataRepository.findLatestDateByPair(fromCurrency, toCurrency))
                .thenReturn(Mono.just(latestDate));
            
            // ACT
            Mono<LocalDate> result = repository.findLatestDate(fromCurrency, toCurrency);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(latestDate)
                .verifyComplete();
            
            verify(dataRepository).findLatestDateByPair(fromCurrency, toCurrency);
        }
        
        @Test
        @DisplayName("existsByPair() должен делегировать к dataRepository")
        void shouldDelegateToDataRepositoryForExistsByPair() {
            // ARRANGE
            when(dataRepository.existsByFromCurrencyAndToCurrency(fromCurrency, toCurrency))
                .thenReturn(Mono.just(true));
            
            // ACT
            Mono<Boolean> result = repository.existsByPair(fromCurrency, toCurrency);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
            
            verify(dataRepository).existsByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
        }
    }

    @Nested
    @DisplayName("Type conversion operations")
    class TypeConversionOperations {
        
        @Test
        @DisplayName("deleteOlderThan() должен конвертировать Integer в Long")
        void shouldConvertIntegerToLong() {
            // ARRANGE
            LocalDate beforeDate = LocalDate.of(2023, 12, 31);
            Integer deletedRows = 15;
            
            when(dataRepository.deleteByDateBefore(beforeDate))
                .thenReturn(Mono.just(deletedRows));
            
            // ACT
            Mono<Long> result = repository.deleteOlderThan(beforeDate);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(15L)
                .verifyComplete();
            
            verify(dataRepository).deleteByDateBefore(beforeDate);
        }
        
        @Test
        @DisplayName("deleteOlderThan() должен возвращать 0L если ничего не удалено")
        void shouldReturnZeroLongWhenNothingDeleted() {
            // ARRANGE
            LocalDate beforeDate = LocalDate.of(2023, 12, 31);
            
            when(dataRepository.deleteByDateBefore(beforeDate))
                .thenReturn(Mono.just(0));
            
            // ACT
            Mono<Long> result = repository.deleteOlderThan(beforeDate);
            
            // ASSERT
            StepVerifier.create(result)
                .expectNext(0L)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("deleteOlderThan() должен передавать ошибку от dataRepository")
        void shouldPropagateErrorFromDataRepositoryForDeleteOlderThan() {
            // ARRANGE
            LocalDate beforeDate = LocalDate.of(2023, 12, 31);
            RuntimeException expectedError = new RuntimeException("Foreign key constraint violation");
            
            when(dataRepository.deleteByDateBefore(beforeDate))
                .thenReturn(Mono.error(expectedError));
            
            // ACT
            Mono<Long> result = repository.deleteOlderThan(beforeDate);
            
            // ASSERT
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("Edge cases and error handling")
    class EdgeCasesAndErrorHandling {
        
        @Test
        @DisplayName("должен корректно обрабатывать null входные параметры в save()")
        void shouldHandleNullInputParameters() {
            // ARRANGE
            HistoricalExchangeRate nullRate = null;
            
            // ACT & ASSERT
            try {
                repository.save(nullRate);
            } catch (NullPointerException e) {
                assertThat(e).isInstanceOf(NullPointerException.class);
            }
        }
        
        @Test
        @DisplayName("должен обрабатывать ошибки маппинга в findByPeriod()")
        void shouldHandleMappingErrors() {
            // ARRANGE
            LocalDate startDate = LocalDate.of(2024, 1, 10);
            LocalDate endDate = LocalDate.of(2024, 1, 20);
            
            when(dataRepository.findByFromCurrencyAndToCurrencyAndDateBetweenOrderByDateAsc(
                fromCurrency, toCurrency, startDate, endDate
            )).thenReturn(Flux.just(entity));
            
            RuntimeException mappingError = new RuntimeException("Mapping failed");
            when(mapper.toDomain(entity)).thenThrow(mappingError);
            
            // ACT
            Flux<HistoricalExchangeRate> result = repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate);
            
            // ASSERT
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        }
        
        @Test
        @DisplayName("должен обрабатывать большие коллекции в saveAll()")
        void shouldHandleLargeCollections() {
            // ARRANGE
            List<HistoricalExchangeRate> largeList = java.util.stream.IntStream.range(0, 1000)
                .mapToObj(i -> HistoricalExchangeRate.of(
                    Currency.of("USD"), 
                    Currency.of("EUR"),
                    new BigDecimal("1.08").add(new BigDecimal(i).multiply(new BigDecimal("0.0001"))),
                    LocalDate.of(2024, 1, 1).plusDays(i % 365),
                    "Provider" + (i % 10)
                ))
                .toList();
                
            when(mapper.toEntity(any())).thenAnswer(invocation -> {
                HistoricalExchangeRate rate = invocation.getArgument(0);
                return HistoricalExchangeRateEntity.of(
                    rate.fromCurrency().code(),
                    rate.toCurrency().code(),
                    rate.rate(),
                    rate.date(),
                    rate.providerName()
                );
            });
            
            when(dataRepository.saveAll(any(List.class))).thenAnswer(invocation -> 
                Flux.fromIterable(invocation.getArgument(0))
            );
            
            when(mapper.toDomain(any())).thenAnswer(invocation -> {
                HistoricalExchangeRateEntity entity = invocation.getArgument(0);
                return HistoricalExchangeRate.of(
                    Currency.of(entity.getFromCurrency()),
                    Currency.of(entity.getToCurrency()),
                    entity.getRate(),
                    entity.getDate(),
                    entity.getProviderName()
                );
            });
            
            // ACT
            Flux<HistoricalExchangeRate> result = repository.saveAll(Flux.fromIterable(largeList));
            
            // ASSERT
            StepVerifier.create(result)
                .expectNextCount(1000)
                .verifyComplete();
            
            verify(dataRepository).saveAll(any(List.class));
        }
    }
} 