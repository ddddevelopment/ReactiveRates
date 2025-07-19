// src/main/java/com/reactiverates/domain/model/ExchangeRate.java
package com.reactiverates.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Курс обмена валют
 * 
 * @param fromCurrency Исходная валюта
 * @param toCurrency   Целевая валюта  
 * @param rate         Курс обмена
 * @param timestamp    Время получения курса
 */
public record ExchangeRate(
    @JsonProperty("from") Currency fromCurrency,
    @JsonProperty("to") Currency toCurrency,
    @JsonProperty("rate") BigDecimal rate,
    @JsonProperty("timestamp") LocalDateTime timestamp
) {
    
    @JsonCreator
    public ExchangeRate {
        Objects.requireNonNull(fromCurrency, "From currency cannot be null");
        Objects.requireNonNull(toCurrency, "To currency cannot be null");
        Objects.requireNonNull(rate, "Exchange rate cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
    }
    
    public static ExchangeRate of(Currency from, Currency to, BigDecimal rate) {
        return new ExchangeRate(from, to, rate, LocalDateTime.now());
    }
    
    public static ExchangeRate of(String fromCode, String toCode, BigDecimal rate) {
        return new ExchangeRate(
            Currency.of(fromCode), 
            Currency.of(toCode), 
            rate, 
            LocalDateTime.now()
        );
    }
}