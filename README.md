# ReactiveRates API

Это реактивное API для мгновенной конвертации валют и получения как актуальных, так и исторических курсов. Сервис построен на современном реактивном стеке, поддерживает масштабируемость, отказоустойчивость и удобен для интеграции.

## 🌟 Ключевые возможности

-   **Мгновенная конвертация валют:** Переводите суммы между валютами по актуальному курсу.
-   **Получение текущих курсов:** Запрашивайте курсы для любых валютных пар.
-   **Исторические курсы:** Получайте курсы за любой период, анализируйте динамику.
-   **Проверка поддержки валютных пар:** Быстро узнавайте, поддерживается ли нужная пара.
-   **Высокая производительность:** Реактивный стек (Spring WebFlux) позволяет обрабатывать тысячи запросов одновременно.
-   **Кэширование:** Для ускорения ответов и снижения нагрузки на внешние API.
-   **Гибкая архитектура:** Легко добавлять новых провайдеров и расширять функционал.
-   **Запуск через Docker Compose:** Быстрый старт с помощью контейнеризации.

## 🛠️ Технологический стек

-   **Java 21**
-   **Spring Boot 3.5** (WebFlux, Actuator, Validation, Data R2DBC)
-   **Project Reactor**
-   **PostgreSQL 16** + **R2DBC**
-   **Liquibase** (миграции схемы)
-   **Caffeine Cache**
-   **Swagger/OpenAPI 3** (springdoc)
-   **Docker**
-   **Maven**

## 🏛️ Архитектура

Проект реализован по принципам **Чистой Архитектуры** (Clean Architecture):

1. **api** — контроллеры, модели запросов и ответов
2.  **domain** — бизнес-логика, модели (`Currency`, `ExchangeRate`), интерфейсы сервисов (`CurrencyConversionService`, `RateProvider`). Не зависит от инфраструктуры.
3.  **application** — оркестрация, реализации доменных сервисов (`DefaultCurrencyConversionService`, `DefaultHistoricalRateService`).
4.  **infrastructure** — клиенты внешних API, кэш, конфигурации, работа с БД.

Такое разделение облегчает тестирование, поддержку и расширение.

## 🚀 Быстрый старт

### 1. Запуск через Docker Compose (рекомендуется)

```bash
# Клонируйте репозиторий
 git clone <URL вашего репозитория>
 cd ReactiveRates

# Запустите сервисы
 docker-compose up --build
```

- API будет доступно на `http://localhost:8080`
- PgAdmin — на `http://localhost:5050` (логин: admin@admin.com, пароль: admin)
- PostgreSQL — порт 5432 (логин/пароль: postgres)

### 2. Локальный запуск (без Docker)

```bash
./mvnw clean package
java -jar target/reactive-rates-api-*.jar
```

## 🕹️ API Endpoints

Документация OpenAPI доступна по адресу [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html) после запуска приложения.

### Конвертация валют

-   **POST `/api/v1/convert`**

    Конвертирует сумму из одной валюты в другую.

    **Тело запроса:**
    ```json
    {
      "fromCurrency": "USD",
      "toCurrency": "EUR",
      "amount": 100
    }
    ```

    **Пример ответа:**
    ```json
    {
      "convertedAmount": 92.5,
      "rate": 0.925,
      "provider": "UniRateAPI",
      "timestamp": "2024-05-23T10:30:00"
    }
    ```

### Получение курса обмена

-   **GET `/api/v1/rates?from={fromCurrency}&to={toCurrency}`**

    Возвращает текущий курс обмена между двумя валютами.

    **Пример ответа:**
    ```json
    {
      "fromCurrency": "USD",
      "toCurrency": "RUB",
      "rate": 91.85,
      "timestamp": "2024-05-23T10:30:00Z",
      "provider": "ExchangeRateAPI"
    }
    ```

### Проверка поддержки валютной пары

-   **GET `/api/v1/rates/support?from={fromCurrency}&to={toCurrency}`**

    Проверяет, поддерживается ли конвертация между указанными валютами.

    **Пример ответа:**
    ```json
    {
      "supported": true
    }
    ```

### Исторические курсы валют

-   **GET `/api/v1/historical?from=USD&to=EUR&startDate=2024-01-01&endDate=2024-01-31`**

    Получить исторические курсы за период.

    **Пример ответа:**
    ```json
    [
      {
        "fromCurrency": "USD",
        "toCurrency": "EUR",
        "rate": 0.92,
        "timestamp": "2024-01-01T00:00:00Z"
      },
      ...
    ]
    ```

-   **GET `/api/v1/historical/complete?from=USD&to=EUR&startDate=2024-01-01&endDate=2024-01-31`**

    Проверить полноту исторических данных за период.

    **Пример ответа:**
    ```json
    {
      "complete": true
    }
    ```

-   **GET `/api/v1/historical/range?from=USD&to=EUR`**

    Получить диапазон дат, за которые есть исторические данные.

    **Пример ответа:**
    ```json
    {
      "startDate": "2020-01-01",
      "endDate": "2024-05-23"
    }
    ```