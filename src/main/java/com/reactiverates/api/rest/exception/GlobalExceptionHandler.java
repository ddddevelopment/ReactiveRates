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

/**
 * Реактивный глобальный обработчик исключений для WebFlux
 */
@RestControllerAdvice
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
    public Mono<ResponseEntity<ProblemDetail>> handleGenericError(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        return Mono.fromCallable(() -> {
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            problemDetail.setTitle("Internal Server Error");
            problemDetail.setDetail("An unexpected error occurred. Please contact support if the problem persists.");
            problemDetail.setType(URI.create("https://api.reactive-rates.com/errors/internal"));
            
            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("errorCode", "INTERNAL_001");
            problemDetail.setProperty("traceId", java.util.UUID.randomUUID().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
        });
    }
}