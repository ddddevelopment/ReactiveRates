package com.reactiverates.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * DTO для ответа от внешнего API (unirateapi.com) с эндпоинта /api/rates
 * <p>
 * Согласно официальной OpenAPI спецификации.
 * <p>
 * Пример ответа:
 * <pre>
 * {
 *   "amount": 1,
 *   "base": "USD",
 *   "rate": 0.85,
 *   "result": 85.0,
 *   "to": "EUR"
 * }
 * </pre>
 */
public record UniRateApiResponse(
    @JsonProperty("base") String base,
    @JsonProperty("to") String to,
    @JsonProperty("rate") BigDecimal rate,
    @JsonProperty("result") BigDecimal result,
    @JsonProperty("amount") BigDecimal amount
) {

    /**
     * Проверяет, является ли ответ валидным.
     * @return true, если ответ валиден.
     */
    public boolean isValid() {
        return base != null && to != null && rate != null && rate.compareTo(BigDecimal.ZERO) >= 0;
    }
}
