package com.reactiverates.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Исторический курс обмена валют за конкретную дату
 * 
 * @param fromCurrency Исходная валюта
 * @param toCurrency   Целевая валюта  
 * @param rate         Курс обмена
 * @param date         Дата курса
 * @param providerName Название провайдера данных
 */
@Schema(description = "Исторический курс обмена между двумя валютами за конкретную дату")
public record HistoricalExchangeRate(
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
    
    @JsonProperty("date")
    @Schema(
        description = "Дата курса",
        example = "2024-01-15",
        type = "string",
        format = "date"
    )
    LocalDate date,

    @JsonProperty("provider")
    @Schema(description = "Источник исторических данных", example = "ExchangeRateAPI")
    String providerName
) {
    
    @JsonCreator
    public HistoricalExchangeRate {
        Objects.requireNonNull(fromCurrency, "From currency cannot be null");
        Objects.requireNonNull(toCurrency, "To currency cannot be null");
        Objects.requireNonNull(rate, "Exchange rate cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");
        Objects.requireNonNull(providerName, "Provider name cannot be null");
        
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        
        if (fromCurrency.equals(toCurrency)) {
            throw new IllegalArgumentException("From and to currencies must be different");
        }
    }
    
    public static HistoricalExchangeRate of(Currency from, Currency to, BigDecimal rate, LocalDate date, String providerName) {
        return new HistoricalExchangeRate(from, to, rate, date, providerName);
    }
    
    public static HistoricalExchangeRate of(String fromCode, String toCode, BigDecimal rate, LocalDate date, String providerName) {
        return new HistoricalExchangeRate(
            Currency.of(fromCode), 
            Currency.of(toCode), 
            rate, 
            date,
            providerName
        );
    }
    
    public ExchangeRate toExchangeRate() {
        return new ExchangeRate(
            fromCurrency,
            toCurrency,
            rate,
            date.atStartOfDay(),
            providerName
        );
    }
    
    public String getCacheKey() {
        return String.format("%s->%s:%s", 
            fromCurrency.code(), 
            toCurrency.code(), 
            date.toString());
    }
} 