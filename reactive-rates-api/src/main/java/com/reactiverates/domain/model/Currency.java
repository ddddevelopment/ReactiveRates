package com.reactiverates.domain.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Модель валюты для ReactiveRates API
 * 
 * @param code   Код валюты (USD, EUR, RUB)
 * @param name   Полное название (US Dollar, Euro, Russian Ruble)  
 * @param symbol Символ валюты ($, €, ₽)
 */
public record Currency(
    @JsonProperty("code") String code,
    @JsonProperty("name") String name,
    @JsonProperty("symbol") String symbol
) {
    @JsonCreator
    public Currency {
        Objects.requireNonNull(code, "Currency code cannot be null");
        Objects.requireNonNull(name, "Currency name cannot be null");
        Objects.requireNonNull(symbol, "Currency symbol cannot be null");

        if (code.length() != 3) {
            throw new IllegalArgumentException("Currency code must be 3 characters");
        }
    }

    public static Currency of(String code) {
        return new Currency(code, "", "");
    }

    public String getCode() {
        return code;
    }

    public static final Currency USD = new Currency("USD", "US Dollar", "$");
    public static final Currency EUR = new Currency("EUR", "Euro", "€");
    public static final Currency RUB = new Currency("RUB", "Russian Ruble", "₽");
    public static final Currency GBP = new Currency("GBP", "British Pound", "£");
}
