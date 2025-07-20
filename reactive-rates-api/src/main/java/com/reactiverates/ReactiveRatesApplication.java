package com.reactiverates;

import com.reactiverates.infrastructure.config.ExchangeRateApiConfig;
import com.reactiverates.infrastructure.config.UniRateApiConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({UniRateApiConfig.class, ExchangeRateApiConfig.class})
public class ReactiveRatesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveRatesApplication.class, args);
	}

}
