# Security: API Security

## Purpose

Документ описывает безопасность REST API: валидация, защита данных, ограничения MVP.

## Context

Backend предоставляет OpenAPI REST API для Telegram Mini App. API должен быть защищен от типовых угроз без избыточной сложности на MVP.

## Responsibilities

- Определить правила валидации входных данных.
- Запретить утечку персональных и платежных данных.
- Описать защиту файловых endpoints.
- Зафиксировать CORS и HTTPS требования.

## Non Responsibilities

- Документ не реализует rate limiting на MVP (future option).
- Документ не описывает WAF.

## Design Decisions

Валидация:
- Bean Validation (`@Valid`, `@NotNull`, `@Size`) на всех request DTO.
- Денежные суммы — только положительные, в копейках.
- Telegram user id — только при добавлении участника владельцем.

Защита данных:
- Реквизиты плательщика видны только должнику при оплате конкретного долга.
- Профиль — только владельцу.
- Ошибки не раскрывают внутренние детали (stack trace только в логах).
- Correlation id в ответах для трассировки.

Файлы:
- Доступ только с JWT.
- Проверка членства в мероприятии перед выдачей файла.
- MIME-type whitelist.

Transport:
- HTTPS обязателен в test/prod.
- CORS — только origin Telegram Mini App.

## Constraints

- Rate limiting — post-MVP.
- Нет публичных endpoints кроме auth и health.
- Платежные данные не логируются.

## Future Evolution

- Rate limiting per user/IP.
- API versioning (`/api/v2`).
- Request signing для webhooks.
- OWASP dependency check в CI.

## Related Documents

- `docs/architecture/error-handling.md`
- `docs/architecture/logging-strategy.md`
- `docs/security/authentication.md`
- `docs/security/authorization.md`
- `docs/testing/integration-tests.md`
