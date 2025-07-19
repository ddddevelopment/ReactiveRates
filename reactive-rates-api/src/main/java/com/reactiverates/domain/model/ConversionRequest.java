// src/main/java/com/reactiverates/domain/model/ConversionRequest.java
package com.reactiverates.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ConversionRequest(
    @JsonProperty("from")
    @NotBlank(message = "Source currency code cannot be blank")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be 3 uppercase letters")
    String fromCurrency,
    
    @JsonProperty("to") 
    @NotBlank(message = "Target currency code cannot be blank")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be 3 uppercase letters")
    String toCurrency,
    
    @JsonProperty("amount")
    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount
) {
    
    @JsonCreator
    public ConversionRequest { }
    
    public static ConversionRequest of(String from, String to, BigDecimal amount) {
        return new ConversionRequest(from, to, amount);
    }
}