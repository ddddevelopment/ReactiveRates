package com.reactiverates.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
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
                .servers(createServers())
                .components(createComponents())
                .addSecurityItem(createSecurityRequirement());
    }

    private Info createApiInfo() {
        return new Info()
                .title("ReactiveRates API")
                .description(
                    "💱 ReactiveRates API — реактивный сервис для мгновенной конвертации валют и получения актуальных и исторических курсов.\n" +
                    "\n" +
                    "Возможности:\n" +
                    "• Мгновенная конвертация валют\n" +
                    "• Актуальные и исторические курсы\n" +
                    "• Проверка поддержки валютных пар\n" +
                    "• Высокая производительность (WebFlux)\n" +
                    "• Кэширование для ускорения ответов\n" +
                    "\n" +
                    "🔐 **Аутентификация:** Для доступа к API используйте Bearer токен в заголовке Authorization.\n" +
                    "Пример: `Authorization: Bearer your-jwt-token-here`\n" +
                    "\nДокументация: /swagger-ui.html"
                )
                .version("1.0.0")
                .contact(createContact())
                .license(createLicense());
    }

    private Contact createContact() {
        return new Contact()
                .name("Dmitrii Gorbachev")
                .url("https://github.com/ddddevelopment");
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

    private Components createComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", createBearerSecurityScheme());
    }

    private SecurityScheme createBearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Введите ваш JWT токен в формате: Bearer <token>");
    }

    private SecurityRequirement createSecurityRequirement() {
        return new SecurityRequirement()
                .addList("bearerAuth");
    }
} 