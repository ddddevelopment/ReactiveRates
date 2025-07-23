package com.reactiverates.domain.service;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface HistoricalRateService {
    
    Flux<HistoricalExchangeRate> getHistoricalRates(
        String fromCurrency, 
        String toCurrency, 
        LocalDate startDate, 
        LocalDate endDate
    );
} 