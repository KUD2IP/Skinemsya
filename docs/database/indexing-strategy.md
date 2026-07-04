# Database: Indexing Strategy

## Purpose

Документ описывает стратегию индексов PostgreSQL для MVP: какие индексы нужны и почему.

## Context

MVP не требует сложной оптимизации, но базовые индексы необходимы для частых запросов: поиск по Telegram user id, группам, мероприятиям, долгам.

## Responsibilities

- Определить индексы для каждой таблицы.
- Обосновать выбор индексов.
- Зафиксировать partial indexes где нужно.

## Non Responsibilities

- Документ не проводит performance testing.
- Документ не создает changesets.

## Design Decisions

### Обязательные индексы MVP

| Таблица | Индекс | Обоснование |
| --- | --- | --- |
| users | UNIQUE(telegram_user_id) | Поиск при auth |
| groups | UNIQUE(telegram_chat_id) WHERE telegram_chat_id IS NOT NULL | Поиск chat-linked группы |
| groups | INDEX(owner_id) | Группы владельца |
| group_members | UNIQUE(group_id, user_id) | Проверка членства |
| group_members | INDEX(user_id) | Группы пользователя |
| events | INDEX(group_id) | Мероприятия группы |
| events | INDEX(payer_id) | Мероприятия плательщика |
| event_participants | UNIQUE(event_id, user_id) | Проверка участия |
| positions | INDEX(event_id) | Позиции мероприятия |
| position_selections | UNIQUE(position_id, user_id) | Выбор позиций |
| debts | INDEX(event_id) | Долги мероприятия |
| debts | INDEX(debtor_id) | Долги должника |
| debts | INDEX(creditor_id) | Долги кредитора |
| debts | INDEX(status) | Фильтр по статусу |
| payments | UNIQUE(debt_id) | Один платеж на долг |
| refresh_tokens | INDEX(token_hash) | Поиск при refresh |
| refresh_tokens | INDEX(user_id) | Токены пользователя |
| notifications | INDEX(user_id, created_at DESC) | Лента уведомлений |
| files | INDEX(owner_id) | Файлы пользователя |

Partial indexes:
- `groups.telegram_chat_id` — partial unique WHERE NOT NULL.
- `groups.deleted_at` — partial WHERE deleted_at IS NULL для активных групп.

## Constraints

- Не создавать индексы «на всякий случай» — только для известных запросов.
- Индексы добавляются через Liquibase changesets.
- Именование по `naming-conventions.md`.

## Future Evolution

- Composite indexes при появлении медленных запросов.
- EXPLAIN ANALYZE в CI для критичных запросов.
- Partitioning notifications по дате.

## Related Documents

- `docs/database/table-catalog.md`
- `docs/database/naming-conventions.md`
- `docs/deployment/environments.md`
