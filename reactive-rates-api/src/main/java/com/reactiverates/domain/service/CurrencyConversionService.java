package com.reactiverates.domain.service;

import com.reactiverates.domain.model.ConversionRequest;
import com.reactiverates.domain.model.ConversionResult;
import com.reactiverates.domain.model.ExchangeRate;

import reactor.core.publisher.Mono;


public interface CurrencyConversionService {
    /**
     * Конвертирует сумму из одной валюты в другую
     * 
     * @param request запрос на конвертацию (from, to, amount)
     * @return результат конвертации с курсом и конвертированной суммой
     */
    Mono<ConversionResult> convertCurrency(ConversionRequest request);

    /**
     * Получает текущий курс между двумя валютами
     * 
     * @param fromCurrency исходная валюта
     * @param toCurrency целевая валюта  
     * @return курс обмена
     */
    Mono<ExchangeRate> getExchangeRate(String fromCurrency, String toCurrency);

    /**
     * Проверяет поддержку валютной пары
     * 
     * @param fromCurrency исходная валюта
     * @param toCurrency целевая валюта
     * @return true если пара поддерживается
     */
    Mono<Boolean> isCurrencyPairSupported(String fromCurrency, String toCurrency);
}
