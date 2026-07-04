# Database: ERD

## Purpose

Документ описывает Entity-Relationship Diagram (ERD) базы данных MVP на уровне таблиц и связей.

## Context

Все транзакционные данные хранятся в PostgreSQL. ERD отражает владение таблицами модулями и связи между сущностями домена.

## Responsibilities

- Представить диаграмму таблиц и связей MVP.
- Показать владение таблицами модулями.
- Дать основу для Liquibase-миграций.

## Non Responsibilities

- Документ не создает SQL-миграции.
- Документ не описывает все колонки (см. `table-catalog.md`).

## Design Decisions

```mermaid
erDiagram
    users ||--o| user_profiles : has
    users ||--o{ group_members : participates
    users ||--o{ refresh_tokens : owns
    groups ||--o{ group_members : contains
    groups ||--o{ events : has
    users ||--o{ groups : owns
    events ||--o{ event_participants : includes
    events ||--o{ positions : has
    events ||--o{ receipts : has
    events ||--o{ debts : generates
    receipts ||--o| files : uses
    positions ||--o{ position_selections : selected_by
    positions ||--o{ shared_position_targets : targets
    debts ||--o| payments : settled_by
    users ||--o{ files : uploads
    users ||--o{ notifications : receives

    users {
        bigint id PK
        bigint telegram_user_id UK
        varchar display_name
        timestamp created_at
    }
    groups {
        bigint id PK
        varchar name
        varchar type
        bigint telegram_chat_id UK
        bigint owner_id FK
        timestamp deleted_at
    }
    events {
        bigint id PK
        bigint group_id FK
        varchar name
        bigint payer_id FK
        varchar status
    }
    debts {
        bigint id PK
        bigint event_id FK
        bigint debtor_id FK
        bigint creditor_id FK
        bigint amount_kopecks
        varchar status
    }
    payments {
        bigint id PK
        bigint debt_id FK UK
        varchar status
    }
```

Модули-владельцы таблиц:
- `users`: users, user_profiles
- `auth`: refresh_tokens
- `groups`: groups, group_members
- `events`: events, event_participants
- `receipts`: positions, position_selections, shared_position_targets, receipts
- `files`: files
- `debts`: debts
- `payments`: payments
- `notifications`: notifications

## Constraints

- Одна БД на MVP.
- `telegram_chat_id` nullable (для STANDALONE-групп).
- `debt_id` unique в payments — один платеж на долг.

## Future Evolution

- Partitioning для notifications.
- Read replicas.
- Service-owned databases при выделении сервисов.

## Related Documents

- `docs/database/table-catalog.md`
- `docs/database/entity-relations.md`
- `docs/business/domain-model.md`
- `docs/modules/*.md`
