package com.reactiverates.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

// DTO для ответа от unirateapi.com /api/historical/timeseries
public record UniRateHistoricalTimeseriesResponse(
    @JsonProperty("start_date") String startDate,
    @JsonProperty("end_date") String endDate,
    @JsonProperty("base") String base,
    @JsonProperty("data") Map<String, Map<String, BigDecimal>> data
) {
    public boolean isValid() {
        return base != null && data != null && !data.isEmpty();
    }
} 