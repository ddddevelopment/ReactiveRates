package com.reactiverates.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${external-apis.exchange-rate.base-url:https://api.exchangerate.host}")
    private String exchangeRateApiUrl;

    @Value("${external-apis.exchange-rate.timeout:10s}")
    private Duration timeout;

    @Bean("exchangeRateWebClient")
    public WebClient exchangRateWebClient() {
        return WebClient.builder()
            .baseUrl(exchangeRateApiUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }
}
