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

    @Bean
    public WebClient webClient(UniRateApiConfig config) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout().toMillis())
            .responseTimeout(config.getTimeout())
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler((int) config.getTimeout().toSeconds(), TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler((int) config.getTimeout().toSeconds(), TimeUnit.SECONDS)));

        log.info("🚀 Configuring WebClient for UniRateAPI with base URL: {}", config.getBaseUrl());

        return WebClient.builder()
            .baseUrl(config.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
