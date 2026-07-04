# Database: Naming Conventions

## Purpose

Документ фиксирует правила именования таблиц, колонок, constraints, индексов и Liquibase changesets.

## Context

Единые соглашения об именах упрощают миграции, code review и работу AI-агентов.

## Responsibilities

- Определить правила имен таблиц и колонок.
- Зафиксировать формат constraints и индексов.
- Описать структуру Liquibase changesets.

## Non Responsibilities

- Документ не создает changesets.
- Документ не определяет Java naming (см. `ai/coding-standards.md`).

## Design Decisions

Таблицы:
- snake_case, множественное число: `users`, `group_members`, `event_participants`.
- Префикс модуля не используется — таблица именуется по сущности.

Колонки:
- snake_case: `telegram_user_id`, `amount_kopecks`, `created_at`.
- PK: `id` (BIGSERIAL).
- FK: `{entity}_id` — `group_id`, `user_id`, `event_id`.
- Boolean: `is_` prefix не используется — `is_shared` допустимо для ясности.
- Timestamps: `created_at`, `updated_at`, `deleted_at`, `{action}_at`.
- Деньги: `{field}_kopecks` (BIGINT).

Constraints:
- PK: `pk_{table}` — `pk_users`.
- FK: `fk_{table}_{referenced_table}` — `fk_events_groups`.
- Unique: `uk_{table}_{columns}` — `uk_users_telegram_user_id`.
- Check: `ck_{table}_{rule}` — `ck_debts_amount_positive`.

Индексы:
- `idx_{table}_{columns}` — `idx_debts_debtor_id`.
- Partial: `idx_{table}_{columns}_partial` — `idx_groups_telegram_chat_id_partial`.

Liquibase:
- Файл: `db/changelog/changes/{YYYYMMDD}-{sequence}-{description}.xml`.
- ChangeSet id: `{YYYYMMDD}-{sequence}`.
- Author: имя разработчика или `system`.
- Один logical change per changeset.
- Rollback обязателен для DDL changes.

## Constraints

- Не использовать зарезервированные слова PostgreSQL.
- Не использовать аббревиатуры без расшифровки в документации.
- Все changesets в `db/changelog/db.changelog-master.xml`.

## Future Evolution

- Schema per module при выделении сервисов.
- Automated naming lint в CI.
- Flyway migration path (если смена инструмента).

## Related Documents

- `docs/adr/ADR-0004-liquibase.md`
- `docs/database/table-catalog.md`
- `docs/database/indexing-strategy.md`
- `docs/ai/coding-standards.md`
