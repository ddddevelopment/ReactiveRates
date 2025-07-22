package com.reactiverates.domain.service;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface HistoricalRateService {
    
    Flux<HistoricalExchangeRate> getHistoricalRates(
        String fromCurrency, 
        String toCurrency, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    Mono<Boolean> isDataCompleteForPeriod(
        String fromCurrency, 
        String toCurrency, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    Mono<Long> getHistoricalDataCount(String fromCurrency, String toCurrency);
    
    Mono<LocalDate[]> getDataDateRange(String fromCurrency, String toCurrency);
} 