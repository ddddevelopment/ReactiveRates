package com.reactiverates.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 🔧 Конфигурация для внешнего API обмена валют (unirateapi.com)
 */
@ConfigurationProperties(prefix = "unirate-api")
public class UniRateApiConfig {
    
    private String baseUrl = "https://api.unirateapi.com";
    
    private String apiKey = "";
    
    private Duration timeout = Duration.ofSeconds(10);
    private Duration connectTimeout = Duration.ofSeconds(5);
    
    // Геттеры и сеттеры
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public Duration getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
    
    public Duration getConnectTimeout() {
        return connectTimeout;
    }
    
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    /**
     * 🔍 Проверяет, настроен ли API ключ
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("${");
    }
    
    /**
     * 🔒 Безопасный toString - скрываем API ключ
     */
    @Override
    public String toString() {
        return "UniRateApiConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", timeout='" + timeout + '\'' +
                ", connectTimeout='" + connectTimeout + '\'' +
                ", hasApiKey=" + hasApiKey() +
                '}';
    }
} 