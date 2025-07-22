package com.reactiverates.domain.service;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Set;

public interface HistoricalRateRepository {
    
    Mono<HistoricalExchangeRate> save(HistoricalExchangeRate historicalRate);
    
    Flux<HistoricalExchangeRate> saveAll(Flux<HistoricalExchangeRate> historicalRates);
    
    Flux<HistoricalExchangeRate> findByPeriod(
        String fromCurrency, 
        String toCurrency, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    Mono<HistoricalExchangeRate> findByDate(
        String fromCurrency, 
        String toCurrency, 
        LocalDate date
    );
    
    Mono<Set<LocalDate>> findExistingDates(
        String fromCurrency, 
        String toCurrency, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    Mono<Long> countByPair(String fromCurrency, String toCurrency);
    
    Mono<LocalDate> findEarliestDate(String fromCurrency, String toCurrency);
    
    Mono<LocalDate> findLatestDate(String fromCurrency, String toCurrency);
    
    Mono<Boolean> existsByPair(String fromCurrency, String toCurrency);
    
    Mono<Long> deleteOlderThan(LocalDate beforeDate);
} 