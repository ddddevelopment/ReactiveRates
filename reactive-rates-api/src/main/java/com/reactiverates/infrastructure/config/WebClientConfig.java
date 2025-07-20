package com.reactiverates.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;

@Configuration
public class WebClientConfig {
    @Value("${external-apis.exchange-rate.base-url:https://api.exchangerate.host}")
    private String exchangeRateApiUrl;

    @Value("${external-apis.exchange-rate.timeout:10s}")
    private Duration timeout;

    @Value("${external-apis.exchange-rate.connect-timeout:5s}")
    private Duration connectTimeout;

    @Bean("exchangeRateWebClient")
    public WebClient exchangeRateWebClient() {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(timeout)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                (int) connectTimeout.toMillis());
        
        return WebClient.builder()
            .baseUrl(exchangeRateApiUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }
}
