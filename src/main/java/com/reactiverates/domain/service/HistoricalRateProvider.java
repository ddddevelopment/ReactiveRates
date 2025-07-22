package com.reactiverates.domain.service;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Set;

public interface HistoricalRateProvider {
    
    Flux<HistoricalExchangeRate> getHistoricalRates(
        String fromCurrency, 
        String toCurrency, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    Flux<HistoricalExchangeRate> getHistoricalRatesForDates(
        String fromCurrency, 
        String toCurrency, 
        Set<LocalDate> dates
    );
    
    Mono<HistoricalExchangeRate> getHistoricalRate(
        String fromCurrency, 
        String toCurrency, 
        LocalDate date
    );
    
    Mono<Boolean> isAvailable();
    
    String getProviderName();
    
    default int getMaxHistoryDays() {
        return 365; 
    }
    
    Mono<Boolean> supportsHistoricalData(String fromCurrency, String toCurrency);
} 