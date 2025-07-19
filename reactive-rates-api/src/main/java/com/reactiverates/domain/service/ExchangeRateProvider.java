package com.reactiverates.domain.service;

import com.reactiverates.domain.model.ExchangeRate;

import reactor.core.publisher.Mono;

public interface ExchangeRateProvider {
    /**
     * Получает текущий курс обмена валют
     * 
     * @param fromCurrency Исходная валюта (USD, EUR, etc.)
     * @param toCurrency   Целевая валюта
     * @return Mono с курсом обмена
     */
    Mono<ExchangeRate> getCurrentRate(String fromCurrency, String toCurrency);

    /**
     * Проверяет доступность провайдера
     * 
     * @return true если провайдер доступен
     */
    Mono<Boolean> isAvailable();

    /**
     * Возвращает имя провайдера для логирования/мониторинга
     */
    String getProviderName();

    /**
     * Возвращает приоритет провайдера (чем меньше число, тем выше приоритет)
     */
    default int getPriority() {
        return 100;
    }
}
