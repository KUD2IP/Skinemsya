# Database: Audit Strategy

## Purpose

Документ описывает минимальный MVP-аудит изменений данных без enterprise event sourcing.

## Context

MVP не требует полного audit trail, но базовые timestamps и информация о создателе нужны для отладки и поддержки.

## Responsibilities

- Определить минимальный набор audit-полей.
- Зафиксировать, где audit обязателен.
- Исключить enterprise event sourcing на MVP.

## Non Responsibilities

- Документ не реализует audit log table.
- Документ не описывает GDPR compliance.

## Design Decisions

MVP audit — timestamps на всех таблицах:
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()` — обновляется application layer или trigger.

Таблицы с audit:
- Все таблицы из `table-catalog.md` получают `created_at`.
- Mutable таблицы (users, groups, events, debts, payments) — `updated_at`.

Без MVP:
- `created_by` / `updated_by` — post-MVP (кроме `owner_id` в groups).
- Отдельная `audit_log` таблица.
- Event sourcing.
- Temporal tables.

Платежи:
- `debtor_confirmed_at`, `payer_confirmed_at` в payments — audit действий пользователя.

## Constraints

- Audit не должен замедлять MVP-разработку.
- Timestamps в UTC.
- `updated_at` обновляется при каждом изменении записи.

## Future Evolution

- `audit_log` table с JSON diff.
- `created_by` / `updated_by` user id.
- Immutable event log для спорных платежей.

## Related Documents

- `docs/database/table-catalog.md`
- `docs/security/api-security.md`
- `docs/business/mvp-scope.md`
