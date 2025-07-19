// src/main/java/com/reactiverates/domain/model/ConversionResult.java
package com.reactiverates.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Результат конвертации валюты
 */
public record ConversionResult(
    @JsonProperty("request") ConversionRequest request,
    @JsonProperty("exchangeRate") ExchangeRate exchangeRate,
    @JsonProperty("convertedAmount") BigDecimal convertedAmount,
    @JsonProperty("timestamp") LocalDateTime timestamp
) {
    
    public static ConversionResult of(
        ConversionRequest request, 
        ExchangeRate rate, 
        BigDecimal convertedAmount
    ) {
        return new ConversionResult(
            request, 
            rate, 
            convertedAmount, 
            LocalDateTime.now()
        );
    }
}