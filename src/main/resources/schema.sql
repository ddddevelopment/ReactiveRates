-- Создание таблицы для хранения исторических курсов валют
CREATE TABLE IF NOT EXISTS historical_exchange_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19, 8) NOT NULL,
    date DATE NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Уникальный индекс для предотвращения дублирования
    UNIQUE KEY uk_currency_pair_date (from_currency, to_currency, date),
    
    -- Индексы для быстрого поиска
    INDEX idx_currency_pair (from_currency, to_currency),
    INDEX idx_date_range (date),
    INDEX idx_provider (provider_name)
);

-- Комментарии к таблице и колонкам
COMMENT ON TABLE historical_exchange_rates IS 'Таблица исторических курсов валют';
COMMENT ON COLUMN historical_exchange_rates.from_currency IS 'Исходная валюта (3-символьный код)';
COMMENT ON COLUMN historical_exchange_rates.to_currency IS 'Целевая валюта (3-символьный код)';
COMMENT ON COLUMN historical_exchange_rates.rate IS 'Курс обмена (до 8 знаков после запятой)';
COMMENT ON COLUMN historical_exchange_rates.date IS 'Дата курса';
COMMENT ON COLUMN historical_exchange_rates.provider_name IS 'Источник данных (ExchangeRateAPI, UniRateAPI, etc.)'; 