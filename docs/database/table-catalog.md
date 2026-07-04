# Database: Table Catalog

## Purpose

Документ описывает каталог всех таблиц MVP: назначение, ключевые поля, индексы и владение модулем.

## Context

Каждая таблица принадлежит одному модулю. Каталог — источник истины для проектирования Liquibase-миграций.

## Responsibilities

- Перечислить все таблицы MVP.
- Описать ключевые поля каждой таблицы.
- Указать модуль-владелец.
- Зафиксировать индексы.

## Non Responsibilities

- Документ не создает changesets.
- Документ не описывает полную DDL.

## Design Decisions

## Table Ownership Summary

| Module | Tables | Notes |
| --- | --- | --- |
| `auth` | `refresh_tokens` | Token lifecycle only |
| `users` | `users`, `user_profiles` | Telegram identity and payment details |
| `groups` | `groups`, `group_members` | Group type, owner, membership |
| `events` | `events`, `event_participants` | Event lifecycle and participants |
| `receipts` | `receipts`, `positions`, `position_selections`, `shared_position_targets` | Receipt result and expense positions |
| `files` | `files` | Uploaded file metadata |
| `debts` | `debts` | Calculated obligations |
| `payments` | `payments` | Payment confirmations |
| `notifications` | `notifications` | Delivery attempts |

Ownership means only this module has repositories for the table. Other modules can reference ids and use public contracts.

### users (модуль: users)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | Backend user id |
| telegram_user_id | BIGINT UNIQUE NOT NULL | Telegram user id |
| display_name | VARCHAR(255) | Имя из Telegram |
| created_at | TIMESTAMPTZ NOT NULL | |
| updated_at | TIMESTAMPTZ NOT NULL | |

### user_profiles (модуль: users)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| user_id | BIGINT FK UNIQUE | |
| payment_details | TEXT | Реквизиты для переводов |
| phone | VARCHAR(20) | |
| notification_settings | JSONB | Настройки уведомлений |

### refresh_tokens (модуль: auth)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| user_id | BIGINT FK NOT NULL | |
| token_hash | VARCHAR(64) UNIQUE | SHA-256 hash |
| expires_at | TIMESTAMPTZ NOT NULL | |
| revoked | BOOLEAN DEFAULT false | |

### groups (модуль: groups)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| name | VARCHAR(255) NOT NULL | |
| type | VARCHAR(20) NOT NULL | CHAT_LINKED / STANDALONE |
| telegram_chat_id | BIGINT UNIQUE | Nullable для STANDALONE |
| owner_id | BIGINT FK NOT NULL | |
| deleted_at | TIMESTAMPTZ | Soft delete |

### group_members (модуль: groups)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| group_id | BIGINT FK NOT NULL | |
| user_id | BIGINT FK NOT NULL | |
| role | VARCHAR(20) NOT NULL | owner / member |
| joined_at | TIMESTAMPTZ NOT NULL | |
| UNIQUE(group_id, user_id) | | |

### events (модуль: events)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| group_id | BIGINT FK NOT NULL | |
| name | VARCHAR(255) NOT NULL | |
| description | TEXT | |
| payer_id | BIGINT FK NOT NULL | |
| status | VARCHAR(30) NOT NULL | DRAFT/DISTRIBUTION/CALCULATED/COMPLETED |
| deleted_at | TIMESTAMPTZ | |

### event_participants (модуль: events)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| event_id | BIGINT FK NOT NULL | |
| user_id | BIGINT FK NOT NULL | |
| UNIQUE(event_id, user_id) | | |

### positions (модуль: receipts)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| event_id | BIGINT FK NOT NULL | |
| receipt_id | BIGINT FK | Nullable для ручных |
| name | VARCHAR(255) NOT NULL | |
| quantity | DECIMAL(10,2) NOT NULL | |
| total_price_kopecks | BIGINT NOT NULL | |
| is_shared | BOOLEAN DEFAULT false | |
| source | VARCHAR(20) NOT NULL | MANUAL / RECEIPT |

### position_selections (модуль: receipts)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| position_id | BIGINT FK NOT NULL | |
| user_id | BIGINT FK NOT NULL | |
| UNIQUE(position_id, user_id) | | |

### shared_position_targets (модуль: receipts)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| position_id | BIGINT FK NOT NULL | Общая позиция |
| user_id | BIGINT FK NOT NULL | Участник, на которого делится позиция |
| UNIQUE(position_id, user_id) | | Один target на пользователя |

### receipts (модуль: receipts)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| event_id | BIGINT FK NOT NULL | |
| file_id | BIGINT FK NOT NULL | |
| status | VARCHAR(20) NOT NULL | |
| ml_raw_json | JSONB | |

### files (модуль: files)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| owner_id | BIGINT FK NOT NULL | |
| original_name | VARCHAR(255) | |
| mime_type | VARCHAR(50) NOT NULL | |
| size_bytes | BIGINT NOT NULL | |
| storage_path | VARCHAR(500) NOT NULL | |
| created_at | TIMESTAMPTZ NOT NULL | |

### debts (модуль: debts)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| event_id | BIGINT FK NOT NULL | |
| debtor_id | BIGINT FK NOT NULL | |
| creditor_id | BIGINT FK NOT NULL | |
| amount_kopecks | BIGINT NOT NULL | |
| status | VARCHAR(30) NOT NULL | |

### payments (модуль: payments)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| debt_id | BIGINT FK UNIQUE NOT NULL | |
| status | VARCHAR(30) NOT NULL | |
| debtor_confirmed_at | TIMESTAMPTZ | |
| payer_confirmed_at | TIMESTAMPTZ | |

### notifications (модуль: notifications)

| Колонка | Тип | Описание |
| --- | --- | --- |
| id | BIGSERIAL PK | |
| user_id | BIGINT FK NOT NULL | |
| type | VARCHAR(50) NOT NULL | |
| payload | JSONB | |
| status | VARCHAR(20) NOT NULL | |
| sent_at | TIMESTAMPTZ | |

## Cross-Table Constraints

Important constraints for Liquibase design:

| Constraint | Reason |
| --- | --- |
| `users.telegram_user_id` unique | One backend user per Telegram user |
| `user_profiles.user_id` unique | One profile per user |
| `groups.telegram_chat_id` unique where not null | One chat-linked group per Telegram chat |
| `group_members(group_id, user_id)` unique | User cannot be duplicated in group |
| `event_participants(event_id, user_id)` unique | User cannot be duplicated in event |
| `position_selections(position_id, user_id)` unique | User cannot select same position twice |
| `payments.debt_id` unique | One active/simple MVP payment per debt |
| `debts.amount_kopecks > 0` check | No zero/negative debts |
| `positions.total_price_kopecks >= 0` check | Positions cannot have negative cost |
| `positions.quantity > 0` check | Quantity must be positive |

## State Columns

State columns use `VARCHAR`, not PostgreSQL enum, to keep Liquibase changes simple:

| Table | Column | Allowed MVP Values |
| --- | --- | --- |
| `groups` | `type` | `CHAT_LINKED`, `STANDALONE` |
| `group_members` | `role` | `owner`, `member` |
| `events` | `status` | `DRAFT`, `DISTRIBUTION`, `CALCULATED`, `COMPLETED` |
| `receipts` | `status` | `UPLOADED`, `PROCESSING`, `PROCESSED`, `FAILED` |
| `positions` | `source` | `MANUAL`, `RECEIPT` |
| `debts` | `status` | `UNPAID`, `PENDING_CONFIRMATION`, `PAID` |
| `payments` | `status` | `CREATED`, `DEBTOR_CONFIRMED`, `PAYER_CONFIRMED`, `CANCELLED`, `DISPUTED` |
| `notifications` | `status` | `PENDING`, `SENT`, `FAILED` |

Application code must validate values. Database check constraints can be added for MVP if they do not slow development; otherwise application validation is acceptable until enums stabilize.

## Deletion And Retention Notes

- `groups.deleted_at` and `events.deleted_at` implement soft delete.
- `users` are not deleted on MVP.
- `debts` and `payments` are not deleted after creation; status changes preserve history.
- `files` may be deleted physically in future retention policy, but metadata should remain while receipt references exist.
- `notifications` can be retained for a limited window post-MVP; MVP can keep them indefinitely if volume is low.

## AI Implementation Notes

When adding a table:

1. Identify module owner.
2. Add table to this catalog.
3. Add indexes to `indexing-strategy.md`.
4. Add relationships to `erd.md` and `entity-relations.md`.
5. Add Liquibase naming according to `naming-conventions.md`.
6. Add tests using Testcontainers if persistence behavior matters.

Do not add tables for future Kafka/outbox/SBP unless the feature is moved into MVP scope.

## Constraints

- Все таблицы имеют `created_at` (где не указано иное).
- Денежные суммы — `BIGINT` в копейках.
- Enum-значения — `VARCHAR`, не PostgreSQL ENUM (для гибкости миграций).

## Future Evolution

- Таблицы для СБП-платежей.
- Audit log table.
- Event outbox table.

## Related Documents

- `docs/database/erd.md`
- `docs/database/indexing-strategy.md`
- `docs/database/naming-conventions.md`
- `docs/modules/*.md`
