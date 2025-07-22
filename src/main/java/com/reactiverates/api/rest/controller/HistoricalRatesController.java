package com.reactiverates.api.rest.controller;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRatesService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/historical")
@CrossOrigin(origins = "*")
@Tag(
    name = "📈 Исторические курсы валют",
    description = "Операции для получения и анализа исторических курсов валют"
)
public class HistoricalRatesController {
    private static final Logger log = LoggerFactory.getLogger(HistoricalRatesController.class);
    private final HistoricalRatesService service;

    public HistoricalRatesController(HistoricalRatesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
        summary = "📅 Получить исторические курсы",
        description = "Возвращает исторические курсы обмена между двумя валютами за указанный период (включительно)"
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
        @Parameter(description = "Исходная валюта (3-х буквенный код ISO)", example = "USD", required = true)
        @RequestParam String from,
        @Parameter(description = "Целевая валюта (3-х буквенный код ISO)", example = "EUR", required = true)
        @RequestParam String to,
        @Parameter(description = "Начальная дата периода (в формате YYYY-MM-DD)", example = "2024-01-01", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "Конечная дата периода (в формате YYYY-MM-DD)", example = "2024-01-31", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("[HIST] Получение исторических курсов: {} -> {}, {} - {}", from, to, startDate, endDate);
        return service.getHistoricalRates(from, to, startDate, endDate)
            .doOnError(e -> log.error("[HIST] Ошибка получения исторических курсов: {}", e.getMessage()));
    }

    @GetMapping("/count")
    @Operation(
        summary = "🔢 Количество исторических записей",
        description = "Возвращает количество исторических записей для валютной пары"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "✅ Количество получено",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Long.class)
            )
        )
    })
    public Mono<ResponseEntity<Long>> getHistoricalDataCount(
        @Parameter(description = "Исходная валюта", example = "USD", required = true)
        @RequestParam String from,
        @Parameter(description = "Целевая валюта", example = "EUR", required = true)
        @RequestParam String to
    ) {
        log.info("[HIST] Получение количества исторических записей: {} -> {}", from, to);
        return service.getHistoricalDataCount(from, to)
            .map(ResponseEntity::ok)
            .doOnError(e -> log.error("[HIST] Ошибка получения количества: {}", e.getMessage()));
    }

    @GetMapping("/complete")
    @Operation(
        summary = "✅ Проверить полноту данных за период",
        description = "Проверяет, есть ли в базе все исторические курсы за указанный период"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "✅ Проверка выполнена",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DataCompleteResponse.class)
            )
        )
    })
    public Mono<ResponseEntity<DataCompleteResponse>> isDataCompleteForPeriod(
        @Parameter(description = "Исходная валюта", example = "USD", required = true)
        @RequestParam String from,
        @Parameter(description = "Целевая валюта", example = "EUR", required = true)
        @RequestParam String to,
        @Parameter(description = "Начальная дата периода", example = "2024-01-01", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "Конечная дата периода", example = "2024-01-31", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("[HIST] Проверка полноты данных: {} -> {}, {} - {}", from, to, startDate, endDate);
        return service.isDataCompleteForPeriod(from, to, startDate, endDate)
            .map(DataCompleteResponse::new)
            .map(ResponseEntity::ok)
            .doOnError(e -> log.error("[HIST] Ошибка проверки полноты: {}", e.getMessage()));
    }

    @GetMapping("/range")
    @Operation(
        summary = "📆 Диапазон дат с историческими данными",
        description = "Возвращает диапазон дат, за которые есть исторические курсы для валютной пары"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "✅ Диапазон получен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DateRangeResponse.class)
            )
        )
    })
    public Mono<ResponseEntity<DateRangeResponse>> getDataDateRange(
        @Parameter(description = "Исходная валюта", example = "USD", required = true)
        @RequestParam String from,
        @Parameter(description = "Целевая валюта", example = "EUR", required = true)
        @RequestParam String to
    ) {
        log.info("[HIST] Получение диапазона дат: {} -> {}", from, to);
        return service.getDataDateRange(from, to)
            .map(dates -> new DateRangeResponse(dates.length == 2 ? dates[0] : null, dates.length == 2 ? dates[1] : null))
            .map(ResponseEntity::ok)
            .doOnError(e -> log.error("[HIST] Ошибка получения диапазона дат: {}", e.getMessage()));
    }

    @Schema(description = "Ответ о полноте исторических данных за период")
    public record DataCompleteResponse(
        @Schema(description = "Полны ли данные за период", example = "true")
        boolean complete
    ) {}

    @Schema(description = "Ответ с диапазоном дат исторических данных")
    public record DateRangeResponse(
        @Schema(description = "Самая ранняя дата", example = "2024-01-01")
        LocalDate start,
        @Schema(description = "Самая поздняя дата", example = "2024-12-31")
        LocalDate end
    ) {}
} 