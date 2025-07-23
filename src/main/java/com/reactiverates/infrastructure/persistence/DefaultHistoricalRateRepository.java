package com.reactiverates.infrastructure.persistence;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateRepository;
import com.reactiverates.infrastructure.persistence.entity.HistoricalExchangeRateEntity;
import com.reactiverates.infrastructure.persistence.mapper.HistoricalExchangeRateMapper;

import org.reactivestreams.Publisher;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public class DefaultHistoricalRateRepository implements HistoricalRateRepository {
    
    private final SpringDataHistoricalRateRepository dataRepository;
    private final HistoricalExchangeRateMapper mapper;
    
    public DefaultHistoricalRateRepository(
            SpringDataHistoricalRateRepository dataRepository,
            HistoricalExchangeRateMapper mapper) {
        this.dataRepository = dataRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Mono<HistoricalExchangeRate> save(HistoricalExchangeRate historicalRate) {
        HistoricalExchangeRateEntity entity = mapper.toEntity(historicalRate);
        return dataRepository.save(entity)
            .map(mapper::toDomain);
    }
    
    @Override
    public Flux<HistoricalExchangeRate> saveAll(Flux<HistoricalExchangeRate> historicalRates) {
        return dataRepository.saveAll(historicalRates.map(mapper::toEntity)).map(mapper::toDomain);
    }
    
    @Override
    public Flux<HistoricalExchangeRate> findByPeriod(String fromCurrency, String toCurrency, 
                                                     LocalDate startDate, LocalDate endDate) {
        return dataRepository.findByFromCurrencyAndToCurrencyAndDateBetweenOrderByDateAsc(
                fromCurrency, toCurrency, startDate, endDate)
            .map(mapper::toDomain);
    }
    
    @Override
    public Mono<HistoricalExchangeRate> findByDate(String fromCurrency, String toCurrency, LocalDate date) {
        return dataRepository.findByFromCurrencyAndToCurrencyAndDate(fromCurrency, toCurrency, date)
            .map(mapper::toDomain);
    }
    
    @Override
    public Mono<Set<LocalDate>> findExistingDates(String fromCurrency, String toCurrency, 
                                                  LocalDate startDate, LocalDate endDate) {
        return dataRepository.findExistingDatesBetween(fromCurrency, toCurrency, startDate, endDate)
            .collect(java.util.stream.Collectors.toSet());
    }
    
    @Override
    public Mono<Long> countByPair(String fromCurrency, String toCurrency) {
        return dataRepository.countByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
    }
    
    @Override
    public Mono<LocalDate> findEarliestDate(String fromCurrency, String toCurrency) {
        return dataRepository.findEarliestDateByPair(fromCurrency, toCurrency);
    }
    
    @Override
    public Mono<LocalDate> findLatestDate(String fromCurrency, String toCurrency) {
        return dataRepository.findLatestDateByPair(fromCurrency, toCurrency);
    }
    
    @Override
    public Mono<Boolean> existsByPair(String fromCurrency, String toCurrency) {
        return dataRepository.existsByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
    }
    
    @Override
    public Mono<Long> deleteOlderThan(LocalDate beforeDate) {
        return dataRepository.deleteByDateBefore(beforeDate)
            .map(Integer::longValue);
    }
} 