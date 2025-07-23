package com.reactiverates.api.rest.controller;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/historical")
@CrossOrigin(origins = "*")
@Tag(
    name = "📊 Исторические курсы валют",
    description = "Операции для получения и анализа исторических курсов валют."
)
public class HistoricalRatesController {
    private static final Logger log = LoggerFactory.getLogger(HistoricalRatesController.class);
    private final HistoricalRateService service;

    public HistoricalRatesController(HistoricalRateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
        summary = "📅 Исторические курсы за период",
        description = "Возвращает исторические курсы обмена между двумя валютами за указанный период (включительно)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "✅ Курсы получены успешно",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = HistoricalExchangeRate.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "❌ Некорректные параметры запроса"
        )
    })
    public Flux<HistoricalExchangeRate> getHistoricalRates(
        @Parameter(description = "Исходная валюта (3 буквы, ISO)", example = "USD", required = true)
        @RequestParam String from,
        @Parameter(description = "Целевая валюта (3 буквы, ISO)", example = "EUR", required = true)
        @RequestParam String to,
        @Parameter(description = "Начальная дата периода (ГГГГ-ММ-ДД)", example = "2024-01-01", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "Конечная дата периода (ГГГГ-ММ-ДД)", example = "2024-01-31", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("[HIST] Fetching historical rates: {} -> {}, {} - {}", from, to, startDate, endDate);
        return service.getHistoricalRates(from, to, startDate, endDate)
            .doOnError(e -> log.error("[HIST] Error fetching historical rates: {}", e.getMessage()))
            .doOnComplete(() -> log.info("[HIST] Completed fetching historical rates: {} -> {}, {} - {}", from, to, startDate, endDate))
            .doOnSubscribe(sub -> log.info("[HIST] Subscribed to historical rates stream: {} -> {}, {} - {}", from, to, startDate, endDate))
            .collectList()
            .flatMapMany(list -> {
                if (list.isEmpty()) {
                    log.warn("[HIST] No historical rates found for {} -> {}, {} - {}", from, to, startDate, endDate);
                } else {
                    log.info("[HIST] Returned {} historical rates for {} -> {}, {} - {}", list.size(), from, to, startDate, endDate);
                }
                return Flux.fromIterable(list);
            });
    }
} 