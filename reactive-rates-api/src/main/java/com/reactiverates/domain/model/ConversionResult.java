// src/main/java/com/reactiverates/domain/model/ConversionResult.java
package com.reactiverates.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Результат конвертации валюты
 */
@Schema(description = "Результат конвертации валюты с полной информацией о курсе и расчетах")
public record ConversionResult(
    @JsonProperty("request") 
    @Schema(description = "Исходный запрос на конвертацию")
    ConversionRequest request,
    
    @JsonProperty("exchangeRate") 
    @Schema(description = "Информация о курсе обмена на момент конвертации")
    ExchangeRate exchangeRate,
    
    @JsonProperty("convertedAmount") 
    @Schema(
        description = "Сумма после конвертации",
        example = "92.34",
        type = "number"
    )
    BigDecimal convertedAmount,
    
    @JsonProperty("timestamp") 
    @Schema(
        description = "Время выполнения конвертации",
        example = "2024-01-15T10:30:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime timestamp
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