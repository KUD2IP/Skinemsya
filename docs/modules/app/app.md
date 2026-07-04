# Module: app

## Purpose

Модуль `app` является entrypoint Spring Boot приложения: composition root, конфигурация, security wiring и OpenAPI.

## Responsibilities

- Запуск Spring Boot приложения.
- Подключение всех доменных модулей через dependency injection.
- Конфигурация Spring Security filter chain.
- Регистрация REST controllers из доменных модулей.
- Настройка OpenAPI/Swagger UI.
- Применение глобальных exception handlers.
- Конфигурация Liquibase, datasource, logging.

## Domain Objects

`app` не владеет доменными объектами.

## Dependencies

- Все доменные модули: `auth`, `users`, `groups`, `events`, `receipts`, `debts`, `payments`, `notifications`, `files`, `integrations`, `common`.

## Events

`app` не публикует и не потребляет доменные события. Только wiring.

## Database Objects

`app` не владеет таблицами. Liquibase changesets размещаются в `app` или в модуле-владельце таблицы.

## Public Contracts

- `Application` — Spring Boot main class.
- `SecurityConfig` — JWT filter chain, public endpoints.
- `OpenApiConfig` — OpenAPI metadata.
- `GlobalExceptionHandler` — маппинг ошибок в HTTP-ответы.

## Future Extensions

- Health checks и readiness probes.
- Actuator endpoints для DevOps.
- Feature flags configuration.
- Отдельный API-gateway при выделении сервисов.
