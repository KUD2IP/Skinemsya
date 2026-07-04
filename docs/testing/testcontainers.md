# Testing: Testcontainers

## Purpose

Документ описывает использование Testcontainers для PostgreSQL в тестах.

## Context

Integration-тесты требуют реальной PostgreSQL для проверки миграций, транзакций и SQL-запросов. Testcontainers поднимает PostgreSQL в Docker-контейнере.

## Responsibilities

- Описать конфигурацию Testcontainers.
- Зафиксировать применение Liquibase-миграций.
- Определить изоляцию тестовых данных.

## Non Responsibilities

- Документ не описывает Docker setup для разработки.
- Документ не настраивает CI runner.

## Design Decisions

Конфигурация:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
```

Lifecycle:
- Container стартует один раз per test suite (`static @Container`).
- Liquibase `migrate` выполняется в `@BeforeAll`.
- Каждый тест — `@Transactional` + rollback или truncate tables.

Изоляция:
- Уникальные test data per test method (random telegram user ids).
- Нет shared state между test classes.
- `@DirtiesContext` только при необходимости.

CI:
- Docker required на CI runner.
- Testcontainers Ryuk disabled если нет Docker socket (fallback: embedded H2 не используется — только Testcontainers).

## Constraints

- PostgreSQL 16 alpine image.
- Container startup < 30 секунд.
- Не использовать production DB для тестов.

## Future Evolution

- Testcontainers для ML-сервиса mock.
- Reusable containers (`withReuse(true)`) для local dev.
- Parallel containers для speed.

## Related Documents

- `docs/adr/ADR-0003-postgresql.md`
- `docs/adr/ADR-0004-liquibase.md`
- `docs/testing/integration-tests.md`
- `docs/deployment/ci-cd.md`
