package com.reactiverates.api.rest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reactiverates.domain.model.ConversionRequest;
import com.reactiverates.domain.model.ConversionResult;
import com.reactiverates.domain.model.ExchangeRate;
import com.reactiverates.domain.service.CurrencyConversionService;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@Tag(
    name = "💱 Currency Conversion", 
    description = "Операции для конвертации валют и получения курсов обмена"
)
public class CurrencyConversionController {
    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionController.class);
    
    private final CurrencyConversionService service;

    public CurrencyConversionController(CurrencyConversionService service) {
        this.service = service;
    }

    @PostMapping("/convert")
    @Operation(
        summary = "💰 Конвертировать валюту",
        description = "Конвертирует указанную сумму из одной валюты в другую по актуальному курсу"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "✅ Конвертация выполнена успешно",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConversionResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "❌ Некорректные данные запроса"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "🔍 Валютная пара не найдена"
        )
    })
    public Mono<ResponseEntity<ConversionResult>> convertCurrency(
        @Valid @RequestBody ConversionRequest request
    ) {
        log.info("Converting {} {} to {}", request.amount(), request.fromCurrency(), request.toCurrency());

        return service.convertCurrency(request)
            .map(result -> {
                log.info("Conversion successful: {} {} = {} {}", 
                    request.amount(), request.fromCurrency(), result.convertedAmount(), request.toCurrency());
                return ResponseEntity.ok(result);
            })
            .doOnError(error -> log.error("Conversion failed for {}: {}", request, error.getMessage()));
    }

    @GetMapping("/rates")
    @Operation(
        summary = "📊 Получить курс обмена",
        description = "Возвращает текущий курс обмена между двумя валютами"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "✅ Курс получен успешно",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExchangeRate.class)
            )
        )
    })
    public Mono<ResponseEntity<ExchangeRate>> getExchangeRate(
        @Parameter(
            description = "Исходная валюта (3-х буквенный код ISO)",
            example = "USD",
            required = true
        )
        @RequestParam("from") String fromCurrency, 
        
        @Parameter(
            description = "Целевая валюта (3-х буквенный код ISO)", 
            example = "EUR",
            required = true
        )
        @RequestParam("to") String toCurrency
    ) {
        log.info("Getting exchange rate: {} -> {}", fromCurrency, toCurrency);

        return service.getExchangeRate(fromCurrency, toCurrency)
            .map(rate -> {
                log.info("Exchange rate retrieved: {} -> {} = {}", fromCurrency, toCurrency, rate.rate());
                return ResponseEntity.ok(rate);
            })
            .doOnError(error -> log.error("Failed to get rate {} -> {}: {}", 
                fromCurrency, toCurrency, error.getMessage()));
    }
    
    @GetMapping("/rates/support")
    @Operation(
        summary = "🔍 Проверить поддержку валютной пары",
        description = "Проверяет, поддерживается ли конвертация между указанными валютами"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "✅ Проверка выполнена",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SupportResponse.class)
            )
        )
    })
    public Mono<ResponseEntity<SupportResponse>> checkCurrencySupport(
        @Parameter(
            description = "Исходная валюта для проверки",
            example = "USD",
            required = true
        )
        @RequestParam String fromCurrency,
        
        @Parameter(
            description = "Целевая валюта для проверки",
            example = "EUR", 
            required = true
        )
        @RequestParam String toCurrency
    ) {
        log.debug("Checking currency pair support: {} -> {}", fromCurrency, toCurrency);
        
        return service.isCurrencyPairSupported(fromCurrency, toCurrency)
            .map(supported -> ResponseEntity.ok(new SupportResponse(supported)))
            .doOnNext(response -> log.debug("Currency pair {} -> {} supported: {}", 
                fromCurrency, toCurrency, response.getBody().supported()));
    }
    
    @Schema(description = "Ответ о поддержке валютной пары")
    public record SupportResponse(
        @Schema(description = "Поддерживается ли данная валютная пара", example = "true")
        boolean supported
    ) {}
}
