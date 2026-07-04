# Roadmap: MVP

## Purpose

Документ описывает порядок реализации MVP по ценности: от auth до базовых уведомлений.

## Context

MVP должен быть выведен на рынок максимально быстро. Порядок реализации определяется зависимостями модулей и продуктовой ценностью.

## Responsibilities

- Определить фазы реализации.
- Зафиксировать порядок модулей.
- Описать критерии завершения каждой фазы.

## Non Responsibilities

- Документ не содержит даты и оценки.
- Документ не является backlog.

## Design Decisions

Фазы реализации:

**Фаза 1 — Foundation:**
- Maven multi-module structure (12 модулей).
- `app`, `common` bootstrap.
- PostgreSQL + Liquibase setup.
- CI/CD pipeline (build + unit tests).
- Критерий: `mvn clean install` success.

**Фаза 2 — Auth & Users:**
- `auth`: Telegram init data validation, JWT.
- `users`: profile, payment details.
- Integration tests for auth flow.
- Критерий: login через Telegram → JWT → GET /users/me.

**Фаза 3 — Groups & Events:**
- `groups`: CHAT_LINKED + STANDALONE, members, owner.
- `events`: create, payer, participants, status.
- Критерий: создать группу (оба типа) → создать мероприятие.

**Фаза 4 — Receipts & Positions:**
- `files`: upload receipt image.
- `receipts`: manual positions, ML integration, selection.
- `integrations`: ML service adapter.
- Критерий: добавить позиции (ручные + из чека) → выбор → распределение.

**Фаза 5 — Debts & Payments:**
- `debts`: calculation, statuses, summary.
- `payments`: перевести → перевел → получил.
- Критерий: полный payment flow, debt closed.

**Фаза 6 — Polish:**
- `notifications`: basic Telegram notifications.
- Error handling, logging.
- Main screen statistics.
- Integration tests for happy path.
- Критерий: MVP scope criteria met (см. `business/mvp-scope.md`).

## Constraints

- Каждая фаза — deployable increment.
- Нет параллельной разработки зависимых модулей без stub.
- SBP и Kafka не входят в MVP roadmap.

## Future Evolution

- См. `post-mvp-roadmap.md`.

## Related Documents

- `docs/business/mvp-scope.md`
- `docs/architecture/system-overview.md`
- `docs/architecture/backend-architecture.md`
- `docs/roadmap/post-mvp-roadmap.md`
