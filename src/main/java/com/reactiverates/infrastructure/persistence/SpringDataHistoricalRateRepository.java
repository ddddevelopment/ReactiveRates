package com.reactiverates.infrastructure.persistence;

import com.reactiverates.infrastructure.persistence.entity.HistoricalExchangeRateEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface SpringDataHistoricalRateRepository extends R2dbcRepository<HistoricalExchangeRateEntity, Long> {
    
    Flux<HistoricalExchangeRateEntity> findByFromCurrencyAndToCurrencyAndDateBetweenOrderByDateAsc(
        String fromCurrency, String toCurrency, LocalDate startDate, LocalDate endDate
    );
    
    Mono<HistoricalExchangeRateEntity> findByFromCurrencyAndToCurrencyAndDate(
        String fromCurrency, String toCurrency, LocalDate date
    );
    
    Mono<Long> countByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
    
    Mono<Boolean> existsByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
    
    @Query("""
        SELECT DISTINCT date 
        FROM historical_exchange_rates 
        WHERE from_currency = :fromCurrency 
          AND to_currency = :toCurrency 
          AND date BETWEEN :startDate AND :endDate
        ORDER BY date
        """)
    Flux<LocalDate> findExistingDatesBetween(
        String fromCurrency, String toCurrency, LocalDate startDate, LocalDate endDate
    );
    
    @Query("""
        SELECT MIN(date) 
        FROM historical_exchange_rates 
        WHERE from_currency = :fromCurrency AND to_currency = :toCurrency
        """)
    Mono<LocalDate> findEarliestDateByPair(String fromCurrency, String toCurrency);
    
    @Query("""
        SELECT MAX(date) 
        FROM historical_exchange_rates 
        WHERE from_currency = :fromCurrency AND to_currency = :toCurrency
        """)
    Mono<LocalDate> findLatestDateByPair(String fromCurrency, String toCurrency);
    
    @Query("DELETE FROM historical_exchange_rates WHERE date < :beforeDate")
    Mono<Integer> deleteByDateBefore(LocalDate beforeDate);
} 