// src/main/java/com/reactiverates/domain/model/ConversionRequest.java
package com.reactiverates.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@Schema(
    description = "Запрос на конвертацию валюты",
    example = "{\"from\": \"USD\", \"to\": \"EUR\", \"amount\": 100.00}"
)
public record ConversionRequest(
    @JsonProperty("from")
    @NotBlank(message = "Source currency code cannot be blank")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be 3 uppercase letters")
    @Schema(
        description = "Исходная валюта (3-х буквенный код ISO 4217)",
        example = "USD",
        pattern = "[A-Z]{3}",
        minLength = 3,
        maxLength = 3
    )
    String fromCurrency,
    
    @JsonProperty("to") 
    @NotBlank(message = "Target currency code cannot be blank")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be 3 uppercase letters")
    @Schema(
        description = "Целевая валюта (3-х буквенный код ISO 4217)",
        example = "EUR",
        pattern = "[A-Z]{3}",
        minLength = 3,
        maxLength = 3
    )
    String toCurrency,
    
    @JsonProperty("amount")
    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(
        description = "Сумма для конвертации",
        example = "100.00",
        minimum = "0.01",
        type = "number"
    )
    BigDecimal amount
) {
    
    @JsonCreator
    public ConversionRequest { }
    
    public static ConversionRequest of(String from, String to, BigDecimal amount) {
        return new ConversionRequest(from, to, amount);
    }
}