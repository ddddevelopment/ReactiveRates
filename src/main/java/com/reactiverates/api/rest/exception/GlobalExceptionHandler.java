package com.reactiverates.api.rest.exception;

import com.reactiverates.domain.exception.CurrencyNotFoundException;
import com.reactiverates.domain.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.server.reactive.ServerHttpRequest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Реактивный глобальный обработчик исключений для WebFlux
 */
@RestControllerAdvice(assignableTypes = {
    com.reactiverates.api.rest.controller.CurrencyConversionController.class,
    com.reactiverates.api.rest.controller.HistoricalRatesController.class
})
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Обработка ошибок валидации в реактивном стиле
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleValidationErrors(WebExchangeBindException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        return Mono.fromCallable(() -> {
            // Собираем ошибки валидации
            Map<String, String> fieldErrors = new HashMap<>();
            ex.getBindingResult().getFieldErrors().forEach(error -> 
                fieldErrors.put(error.getField(), error.getDefaultMessage())
            );
            
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problemDetail.setTitle("Validation Failed");
            problemDetail.setDetail("Input validation failed");
            problemDetail.setType(URI.create("https://api.reactive-rates.com/errors/validation"));
            
            // Добавляем детали ошибок
            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("errorCode", "VALIDATION_001");
            problemDetail.setProperty("fieldErrors", fieldErrors);
            
            return ResponseEntity.badRequest().body(problemDetail);
        });
    }
    
    /**
     * Обработка ошибок "валюта не найдена" в реактивном стиле
     */
    @ExceptionHandler(CurrencyNotFoundException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleCurrencyNotFound(CurrencyNotFoundException ex) {
        log.warn("Currency not found: {}", ex.getMessage());
        
        return Mono.fromCallable(() -> {
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            problemDetail.setTitle("Currency Not Found");
            problemDetail.setDetail(ex.getMessage());
            problemDetail.setType(URI.create("https://api.reactive-rates.com/errors/currency-not-found"));
            
            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("errorCode", "CURRENCY_001");
            problemDetail.setProperty("supportUrl", "https://reactive-rates.com/support");
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
        });
    }
    
    /**
     * Обработка ошибок внешнего API в реактивном стиле
     */
    @ExceptionHandler(ExternalApiException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleExternalApiError(ExternalApiException ex) {
        log.error("External API error: {}", ex.getMessage());
        
        return Mono.fromCallable(() -> {
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
            problemDetail.setTitle("External Service Unavailable");
            problemDetail.setDetail("Currency exchange service temporarily unavailable. Please try again later.");
            problemDetail.setType(URI.create("https://api.reactive-rates.com/errors/external-api"));
            
            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("errorCode", "EXT_API_001");
            problemDetail.setProperty("retryAfter", "60");
            problemDetail.setProperty("provider", ex.getMessage().contains("ExchangeRate.host") ? "ExchangeRate.host" : "Unknown");
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
        });
    }
    
    /**
     * Обработка всех остальных ошибок в реактивном стиле
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleGenericError(Exception ex, ServerHttpRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return Mono.fromCallable(() -> {
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            problemDetail.setTitle("Internal Server Error");
            problemDetail.setDetail("An unexpected error occurred. Please contact support if the problem persists.");
            problemDetail.setType(URI.create("https://api.reactive-rates.com/errors/internal"));

            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("errorCode", "INTERNAL_001");
            String traceId = UUID.randomUUID().toString();
            problemDetail.setProperty("traceId", traceId);
            problemDetail.setProperty("exception", ex.getClass().getName());
            problemDetail.setProperty("message", ex.getMessage());
            problemDetail.setProperty("path", request.getPath().toString());
            problemDetail.setProperty("suggestion", "Проверьте параметры запроса и повторите попытку. Если ошибка повторяется — обратитесь в поддержку.");

            // Показывать stacktrace только в DEV-режиме или если SHOW_STACKTRACE=true
            String showStack = System.getenv().getOrDefault("SHOW_STACKTRACE", "false");
            if ("true".equalsIgnoreCase(showStack) || isDevProfileActive()) {
                List<String> stack = Arrays.stream(ex.getStackTrace())
                    .map(StackTraceElement::toString)
                    .toList();
                problemDetail.setProperty("stackTrace", stack);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
        });
    }

    // Вспомогательный метод для проверки профиля
    private boolean isDevProfileActive() {
        String profiles = System.getProperty("spring.profiles.active");
        if (profiles == null) return false;
        return Arrays.stream(profiles.split(","))
                .anyMatch(p -> p.trim().equalsIgnoreCase("dev"));
    }
}