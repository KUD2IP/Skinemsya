# Request Flow

## Purpose

Документ описывает общий путь backend-запроса от Telegram Mini App до ответа клиенту.

## Context

MVP использует HTTP API, описанный через OpenAPI. Запросы приходят от Telegram Mini App, проходят authentication и authorization, затем обрабатываются application-level логикой нужного модуля. Backend должен сохранять простую и предсказуемую модель обработки без распределенной трассировки и сложной асинхронности.

## Responsibilities

- Описать последовательность обработки HTTP-запроса.
- Зафиксировать место security-проверок.
- Определить, где выполняются валидация, бизнес-логика и транзакции.
- Связать request flow с error handling и logging.

## Non Responsibilities

- Документ не определяет конкретные endpoints.
- Документ не описывает DTO.
- Документ не описывает код Spring Security configuration.
- Документ не заменяет документы по authentication и authorization.

## Design Decisions

Типовой request flow:

1. Telegram Mini App отправляет запрос в backend.
2. Backend принимает запрос через HTTP controller.
3. Security layer проверяет JWT, кроме публичного auth endpoint.
4. Request validation проверяет обязательные поля, форматы и базовые ограничения.
5. Authorization проверяет доступ пользователя к группе, мероприятию, чеку, долгу или платежу.
6. Application service запускает сценарий конкретного модуля.
7. Domain logic применяет бизнес-правила.
8. Persistence layer сохраняет изменения в PostgreSQL в рамках транзакции.
9. Integration adapter вызывается только если сценарий требует внешнего API.
10. Backend возвращает нормализованный response или error response.

Транзакция должна покрывать изменение внутреннего состояния. Внешний вызов не должен удерживать транзакцию дольше необходимого, если это можно избежать без усложнения MVP.

## Constraints

- Все изменяющие операции должны быть авторизованы по контексту пользователя.
- Ошибки должны возвращаться в едином формате.
- Валидация входных данных обязательна до применения бизнес-логики.
- Внешние вызовы должны иметь таймауты.
- Нельзя добавлять asynchronous workflow только ради архитектурной красоты.
- Нельзя раскрывать внутренние ошибки, SQL, секреты или платежные детали в response.

## Future Evolution

- Для долгих операций, например обработки чеков, может появиться асинхронный workflow.
- Correlation id может быть расширен до distributed tracing при появлении нескольких сервисов.
- Domain events могут использоваться для уведомлений и аудита.
- OpenAPI contract tests могут стать обязательным quality gate в CI.

## Related Documents

- `docs/architecture/error-handling.md`
- `docs/architecture/logging-strategy.md`
- `docs/security/authentication.md`
- `docs/security/authorization.md`
- `docs/security/api-security.md`
- `docs/testing/integration-tests.md`

