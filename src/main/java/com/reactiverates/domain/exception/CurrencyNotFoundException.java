package com.reactiverates.domain.exception;

public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String currencyCode) {
        super("Currency not found: " + currencyCode);
    }

    public CurrencyNotFoundException(String fromCurrency, String toCurrency) {
        super("Exchange rate not found for: " + fromCurrency + " -> " + toCurrency);
    }
}
