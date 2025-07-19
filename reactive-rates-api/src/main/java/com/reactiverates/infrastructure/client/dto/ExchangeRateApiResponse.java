package com.reactiverates.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExchangeRateApiResponse(
    @JsonProperty("success") boolean success,
    @JsonProperty("query") QueryInfo query,
    @JsonProperty("info") RateInfo info,
    @JsonProperty("date") LocalDate date,
    @JsonProperty("result") BigDecimal result
) {
    public record QueryInfo(
        @JsonProperty("from") String from,
        @JsonProperty("to") String to,
        @JsonProperty("amount") BigDecimal amount
    ) { }

    public record RateInfo(
        @JsonProperty("rate") BigDecimal rate
    ) { }

    public boolean isValid() {
        return success && 
        result != null && 
        result.compareTo(BigDecimal.ZERO) > 0 && 
        query != null && 
        info != null;
    }

    public BigDecimal getExchangeRate() {
        return info != null ? info.rate() : result;
    }
}
