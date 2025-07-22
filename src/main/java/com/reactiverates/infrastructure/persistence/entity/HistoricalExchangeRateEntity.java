package com.reactiverates.infrastructure.persistence.entity;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Table("historical_exchange_rates")
public class HistoricalExchangeRateEntity {
    
    @Id
    @Column("ID")
    private Long id;
    
    @Column("from_currency")
    private String fromCurrency;
    
    @Column("to_currency") 
    private String toCurrency;
    
    @Column("rate")
    private BigDecimal rate;
    
    @Column("date")
    private LocalDate date;
    
    @Column("provider_name")
    private String providerName;
    
    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate  
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    public HistoricalExchangeRateEntity() {
    }
    
    public HistoricalExchangeRateEntity(String fromCurrency, String toCurrency, 
                                       BigDecimal rate, LocalDate date, String providerName) {
        this.fromCurrency = Objects.requireNonNull(fromCurrency, "fromCurrency cannot be null");
        this.toCurrency = Objects.requireNonNull(toCurrency, "toCurrency cannot be null");
        this.rate = Objects.requireNonNull(rate, "rate cannot be null");
        this.date = Objects.requireNonNull(date, "date cannot be null");
        this.providerName = Objects.requireNonNull(providerName, "providerName cannot be null");
        
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
    }
    
    public static HistoricalExchangeRateEntity of(String fromCurrency, String toCurrency,
                                                  BigDecimal rate, LocalDate date, String providerName) {
        return new HistoricalExchangeRateEntity(fromCurrency, toCurrency, rate, date, providerName);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        HistoricalExchangeRateEntity that = (HistoricalExchangeRateEntity) obj;
        return Objects.equals(id, that.id) &&
               Objects.equals(fromCurrency, that.fromCurrency) &&
               Objects.equals(toCurrency, that.toCurrency) &&
               Objects.equals(rate, that.rate) &&
               Objects.equals(date, that.date) &&
               Objects.equals(providerName, that.providerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromCurrency, toCurrency, rate, date, providerName);
    }

    @Override
    public String toString() {
        return "HistoricalExchangeRateEntity{" +
               "id=" + id +
               ", fromCurrency='" + fromCurrency + '\'' +
               ", toCurrency='" + toCurrency + '\'' +
               ", rate=" + rate +
               ", date=" + date +
               ", providerName='" + providerName + '\'' +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
} 