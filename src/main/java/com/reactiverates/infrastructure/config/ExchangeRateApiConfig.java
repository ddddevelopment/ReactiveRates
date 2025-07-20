package com.reactiverates.infrastructure.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "exchangerate-api")
public record ExchangeRateApiConfig(
    @NotEmpty String baseUrl,
    @NotEmpty String apiKey,
    int priority,
    Duration timeout,
    Duration connectTimeout
) {
    public Duration timeout() {
        return timeout != null ? timeout : Duration.ofSeconds(10);
    }

    public Duration connectTimeout() {
        return connectTimeout != null ? connectTimeout : Duration.ofSeconds(5);
    }
} 