# AI: Architecture Rules

## Purpose

Архитектурные правила для AI-агентов и разработчиков.

## Context

Модульный монолит требует соблюдения архитектурных правил для сохранения границ и возможности future scaling.

## Responsibilities

- Зафиксировать правила слойности.
- Определить транзакционные границы.
- Описать правила интеграционных адаптеров.

## Non Responsibilities

- Документ не описывает конкретные классы.
- Документ не заменяет ADR.

## Design Decisions

Модульный монолит:
- Один deployable artifact.
- Один PostgreSQL database.
- 12 модулей с явными границами.
- Зависимости — только по `architecture/module-dependencies.md`.

Слойность (внутри модуля):
- `api` → `application` → `domain` ← `infrastructure`.
- `domain` не зависит от `infrastructure`.
- `api` не вызывает `infrastructure` напрямую.
- Controllers — thin, только mapping и validation.

Транзакции:
- `@Transactional` на application service methods.
- Одна транзакция per use case.
- Cross-module calls в рамках одной транзакции (monolith advantage).
- Read-only transactions для queries.

Интеграции:
- Все внешние вызовы через `integrations` module adapters.
- Доменные модули зависят от adapter interface, не от HTTP client.
- Timeout и error mapping в adapter.
- Retry — только в adapter, не в domain.

Будущая событийность:
- Internal domain events — in-process на MVP.
- Event classes в `domain` layer модуля-publisher.
- Listeners в `application` layer модуля-consumer.
- Kafka — post-MVP, через outbox pattern.

## Constraints

- Не добавлять слои без необходимости (no hexagonal over-engineering).
- Не создавать shared service layer между модулями.
- Не использовать event bus library на MVP.

## Future Evolution

- ArchUnit layer checks.
- Module boundary enforcement in build.
- Event catalog documentation.

## Related Documents

- `docs/architecture/backend-architecture.md`
- `docs/architecture/module-dependencies.md`
- `docs/ai/forbidden-patterns.md`
- `docs/ai/module-rules.md`
