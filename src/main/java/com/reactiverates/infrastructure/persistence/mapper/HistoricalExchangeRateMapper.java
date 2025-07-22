package com.reactiverates.infrastructure.persistence.mapper;

import com.reactiverates.domain.model.Currency;
import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.infrastructure.persistence.entity.HistoricalExchangeRateEntity;
import org.springframework.stereotype.Component;

@Component
public class HistoricalExchangeRateMapper {
    
    public HistoricalExchangeRateEntity toEntity(HistoricalExchangeRate domainModel) {
        if (domainModel == null) {
            return null;
        }
        
        return HistoricalExchangeRateEntity.of(
            domainModel.fromCurrency().code(),
            domainModel.toCurrency().code(),
            domainModel.rate(),
            domainModel.date(),
            domainModel.providerName()
        );
    }
    
    public HistoricalExchangeRate toDomain(HistoricalExchangeRateEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return HistoricalExchangeRate.of(
            Currency.of(entity.getFromCurrency()),
            Currency.of(entity.getToCurrency()),
            entity.getRate(),
            entity.getDate(),
            entity.getProviderName()
        );
    }
} 