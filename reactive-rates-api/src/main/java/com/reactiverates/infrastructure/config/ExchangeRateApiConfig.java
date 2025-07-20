package com.reactiverates.infrastructure.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "exchangerate-api")
public record ExchangeRateApiConfig(
    @NotEmpty String baseUrl,
    @NotEmpty String apiKey
) {} 