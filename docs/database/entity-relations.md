# Database: Entity Relations

## Purpose

Документ описывает связи между сущностями БД: кардинальности, владение данными и правила целостности.

## Context

Модульный монолит использует одну PostgreSQL БД, но каждая таблица принадлежит одному модулю. Связи между таблицами разных модулей — через FK на уровне БД, доступ — через публичные контракты.

## Responsibilities

- Определить кардинальности связей.
- Зафиксировать правила владения данными.
- Описать ограничения целостности.
- Определить поведение при удалении.

## Non Responsibilities

- Документ не создает миграции.
- Документ не описывает индексы (см. `indexing-strategy.md`).

## Design Decisions

Ключевые связи:

| Связь | Кардинальность | Владелец |
| --- | --- | --- |
| User → UserProfile | 1:0..1 | users |
| User → GroupMember | 1:N | groups |
| Group → GroupMember | 1:N | groups |
| Group → Event | 1:N | events |
| Event → Position | 1:N | receipts |
| Event → Receipt | 1:N | receipts |
| Event → Debt | 1:N | debts |
| Debt → Payment | 1:0..1 | payments |
| Receipt → File | N:1 | files |
| Position → PositionSelection | 1:N | receipts |
| User → RefreshToken | 1:N | auth |

Правила удаления:
- Soft delete для groups, events (см. `soft-delete-strategy.md`).
- Hard delete запрещен для debts/payments с активными расчетами.
- Cascade delete не используется между модулями — явная логика в application service.

## Constraints

- FK constraints на уровне БД.
- Модуль не читает таблицы другого модуля напрямую.
- `telegram_chat_id` unique только для non-null значений.

## Future Evolution

- Cross-module FK validation через events вместо прямых FK.
- Temporal tables для audit.

## Related Documents

- `docs/database/erd.md`
- `docs/database/soft-delete-strategy.md`
- `docs/business/domain-model.md`
- `docs/adr/ADR-0010-module-boundaries.md`
