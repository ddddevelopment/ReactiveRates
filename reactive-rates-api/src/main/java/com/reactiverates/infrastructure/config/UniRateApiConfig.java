package com.reactiverates.infrastructure.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 🔧 Конфигурация для внешнего API обмена валют (unirateapi.com)
 */
@Validated
@ConfigurationProperties(prefix = "unirate-api")
public record UniRateApiConfig(
    @NotEmpty String baseUrl,
    String apiKey,
    Duration timeout,
    Duration connectTimeout,
    int priority
) {} 