package com.reactiverates.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean("uniRateWebClient")
    public WebClient uniRateWebClient(UniRateApiConfig config) {
        log.info("🚀 Configuring WebClient for UniRateAPI with base URL: {}", config.baseUrl());

        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeout().toMillis())
            .responseTimeout(config.timeout())
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler((int) config.timeout().toSeconds(), TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler((int) config.timeout().toSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
            .baseUrl(config.baseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Bean("exchangeRateWebClient")
    public WebClient exchangeRateWebClient(ExchangeRateApiConfig config) {
        log.info("🚀 Configuring WebClient for ExchangeRate-API.com with base URL: {}", config.baseUrl());
        
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeout().toMillis())
            .responseTimeout(config.timeout())
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler((int) config.timeout().toSeconds(), TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler((int) config.timeout().toSeconds(), TimeUnit.SECONDS)));
        
        return WebClient.builder()
            .baseUrl(config.baseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
