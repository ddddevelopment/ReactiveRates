package com.reactiverates.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI reactiveRatesOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers());
    }

    private Info createApiInfo() {
        return new Info()
                .title("ReactiveRates API")
                .description("""
                    🌍 **Реактивный API для обмена валют**
                    
                    Этот API предоставляет мгновенную информацию о курсах валют и конвертации.
                    Построен на Spring WebFlux для высокой производительности.
                    
                    ## 🚀 Возможности:
                    - ⚡ Реальные курсы валют
                    - 💱 Конвертация любых сумм
                    - 🔍 Проверка поддержки валютных пар
                    - 📊 Кэширование для быстрого ответа
                    
                    ## 📝 Примеры использования:
                    1. **Конвертация:** Узнать сколько евро получится за $100
                    2. **Курс:** Получить текущий курс EUR/USD
                    3. **Проверка:** Убедиться что пара валют поддерживается
                    """)
                .version("1.0.0")
                .contact(createContact())
                .license(createLicense());
    }

    private Contact createContact() {
        return new Contact()
                .name("ReactiveRates Team")
                .email("support@reactiverates.com")
                .url("https://github.com/your-repo/reactive-rates");
    }

    private License createLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"),
                new Server()
                        .url("https://api.reactiverates.com")
                        .description("Production Server")
        );
    }
} 