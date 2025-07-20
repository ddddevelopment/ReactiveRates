// src/main/java/com/reactiverates/domain/model/ExchangeRate.java
package com.reactiverates.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Информация о курсе обмена между двумя валютами")
public record ExchangeRate(
    @JsonProperty("from") 
    @Schema(description = "Исходная валюта")
    Currency fromCurrency,
    
    @JsonProperty("to") 
    @Schema(description = "Целевая валюта")
    Currency toCurrency,
    
    @JsonProperty("rate") 
    @Schema(
        description = "Курс обмена (сколько единиц целевой валюты за 1 единицу исходной)",
        example = "1.0829",
        type = "number",
        minimum = "0"
    )
    BigDecimal rate,
    
    @JsonProperty("timestamp") 
    @Schema(
        description = "Время получения курса",
        example = "2024-01-15T10:30:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime timestamp
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