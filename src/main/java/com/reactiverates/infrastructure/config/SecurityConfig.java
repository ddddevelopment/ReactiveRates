package com.reactiverates.infrastructure.config;

import com.reactiverates.infrastructure.security.JwtAuthenticationEntryPoint;
import com.reactiverates.infrastructure.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthFilter jwtAuthFilter;
    
    @Value("${security.cors.allowed-origins}")
    private String[] allowedOrigins;
    
    @Value("${security.cors.allowed-methods}")
    private String allowedMethods;
    
    @Value("${security.cors.allowed-headers}")
    private String allowedHeaders;
    
    @Value("${security.cors.allow-credentials}")
    private boolean allowCredentials;
    
    private static final String[] PUBLIC_URLS = {
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus",
        "/swagger-ui/**",
        "/api-docs/**",
        "/swagger-resources/**",
        "/webjars/**"
    };
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .exceptionHandling(exception -> 
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeExchange(auth -> {
                auth.pathMatchers(PUBLIC_URLS).permitAll()
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyExchange().authenticated();
            })
            .addFilterBefore(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Когда allowCredentials = true, нельзя использовать "*" в allowedOrigins
        if (allowCredentials) {
            // Используем конкретные origins из конфигурации
            configuration.setAllowedOriginPatterns(List.of(allowedOrigins));
        } else {
            configuration.setAllowedOrigins(List.of("*"));
        }
        
        configuration.setAllowedMethods(List.of(allowedMethods.split(",")));
        configuration.setAllowedHeaders(List.of(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}