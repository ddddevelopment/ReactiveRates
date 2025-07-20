package com.reactiverates.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock-provider")
public record MockProviderConfig(
    boolean enabled
) {} 