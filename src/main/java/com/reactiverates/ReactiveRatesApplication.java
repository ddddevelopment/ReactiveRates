package com.reactiverates;

import com.reactiverates.infrastructure.config.ExchangeRateApiConfig;
import com.reactiverates.infrastructure.config.MockProviderConfig;
import com.reactiverates.infrastructure.config.UniRateApiConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Rates Service Application
 * 
 * Основной микросервис для работы с валютными курсами
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties({
    UniRateApiConfig.class, 
    ExchangeRateApiConfig.class,
    MockProviderConfig.class
})
public class ReactiveRatesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveRatesApplication.class, args);
	}

}
