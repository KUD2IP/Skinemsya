# Testing: Strategy

## Purpose

Документ описывает общую стратегию тестирования MVP: баланс unit, integration и contract tests.

## Context

MVP должен быть быстро разработан, но критичная бизнес-логика (расчет долгов, платежи, auth) требует надежного тестового покрытия без избыточной инфраструктуры.

## Responsibilities

- Определить уровни тестирования.
- Зафиксировать приоритеты покрытия.
- Описать quality gates для CI.
- Дать основу для выбора типа теста.

## Non Responsibilities

- Документ не описывает E2E UI-тесты.
- Документ не задает coverage threshold на MVP.

## Design Decisions

Уровни тестирования:

| Уровень | Что тестирует | Инструменты |
| --- | --- | --- |
| Unit | Бизнес-правила, расчет долгов, нормализация чеков, мапперы | JUnit 5, Mockito |
| Integration | API endpoints, security, транзакции, БД | Spring Boot Test, Testcontainers |
| Contract | JSON-контракт ML-сервиса | JSON schema validation, WireMock |

Приоритеты покрытия (обязательные):
1. Расчет долгов (`debts`) — unit.
2. Нормализация JSON чека (`receipts`) — unit.
3. Telegram auth + JWT — integration.
4. Payment flow (перевел → получил) — integration.
5. Group creation (chat-linked + standalone) — integration.

Quality gates CI:
- Все unit-тесты проходят.
- Все integration-тесты проходят.
- Maven build success.
- Liquibase migrate на Testcontainers PostgreSQL.

## Constraints

- Нет E2E через реальный Telegram на MVP.
- Нет performance/load testing на MVP.
- ML-сервис мокается в integration-тестах (WireMock).

## Future Evolution

- Coverage threshold (80% для debts/payments).
- Contract tests с Pact.
- E2E через Telegram test environment.
- Mutation testing.

## Related Documents

- `docs/testing/unit-tests.md`
- `docs/testing/integration-tests.md`
- `docs/testing/testcontainers.md`
- `docs/deployment/ci-cd.md`
- `docs/business/mvp-scope.md`
