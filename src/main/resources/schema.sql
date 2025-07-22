-- Создание таблицы для хранения исторических курсов валют (H2 compatible, id в кавычках)
CREATE TABLE IF NOT EXISTS historical_exchange_rates (
    "ID" BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19, 8) NOT NULL,
    date DATE NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_currency_pair_date ON historical_exchange_rates (from_currency, to_currency, date);
CREATE INDEX IF NOT EXISTS idx_currency_pair ON historical_exchange_rates (from_currency, to_currency);
CREATE INDEX IF NOT EXISTS idx_date_range ON historical_exchange_rates (date);
CREATE INDEX IF NOT EXISTS idx_provider ON historical_exchange_rates (provider_name);