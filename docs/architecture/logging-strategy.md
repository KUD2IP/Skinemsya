# Logging Strategy

## Purpose

Документ описывает стратегию логирования backend для MVP: что логировать, какие данные запрещены, как связывать события одного запроса.

## Context

Backend обрабатывает пользовательские, финансовые и потенциально чувствительные данные. Логи нужны для диагностики ошибок, поддержки пользователей и DevOps-наблюдения, но не должны становиться хранилищем персональных или платежных данных.

## Responsibilities

- Определить базовые правила логирования.
- Зафиксировать использование correlation id.
- Описать уровни логирования.
- Указать запрет на чувствительные данные.
- Связать logging с error handling и deployment environments.

## Non Responsibilities

- Документ не выбирает конкретную observability-платформу.
- Документ не описывает настройку log aggregation.
- Документ не требует distributed tracing на MVP.
- Документ не заменяет audit strategy.

## Design Decisions

Каждый входящий запрос должен иметь correlation id. Если клиент не передал корректный id, backend создает новый. Correlation id должен попадать в логи request processing, integration calls и error handling.

Уровни логирования:

- `INFO`: запуск приложения, успешные ключевые бизнес-события без чувствительных данных, изменение статусов платежей и чеков.
- `WARN`: повторяемые, но не критичные проблемы: недоступность внешней системы, неполный ML-ответ, спорный платежный статус.
- `ERROR`: сбои, из-за которых сценарий не выполнен: ошибка БД, критичная integration failure, unexpected runtime error.
- `DEBUG`: технические детали для локальной разработки, выключены или ограничены в production.

Логировать можно:

- backend user id;
- group id, event id, receipt id, debt id, payment id;
- статус операции;
- integration name;
- duration и result category;
- correlation id.

Логировать нельзя:

- JWT;
- Telegram init data полностью;
- Telegram bot token;
- персональные банковские реквизиты сверх безопасного идентификатора операции;
- полный текст или изображение чека;
- raw responses внешних систем, если они содержат персональные или платежные данные.

## Constraints

- Production logs должны быть безопасны для передачи DevOps-инженеру.
- Логи не должны содержать секреты из environment variables.
- Логи не должны использоваться как источник бизнес-правды.
- Ошибки интеграций должны быть диагностируемыми без раскрытия чувствительных данных.
- Audit и logging не являются одним и тем же: audit фиксирует значимые изменения состояния, logging помогает диагностировать runtime.

## Future Evolution

- Централизованный сбор логов.
- Метрики по сценариям: обработка чека, расчет долгов, платежи.
- Distributed tracing после появления выделенных сервисов.
- Business audit log для спорных платежей и действий участников.
- Alerting по росту ошибок ML-сервиса, СБП или БД.

## Related Documents

- `docs/architecture/error-handling.md`
- `docs/architecture/request-flow.md`
- `docs/security/secrets-management.md`
- `docs/security/api-security.md`
- `docs/database/audit-strategy.md`
- `docs/deployment/environments.md`

