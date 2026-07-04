# AI: Context Guide

## Purpose

Документ является стартовой картой контекста для AI-агентов. Его нужно читать первым, если агент не знает проект и должен быстро понять, какие документы дают ответы на разные вопросы.

## Context

Проект `skinemsya` находится на стадии документационного проектирования и подготовки MVP. Часть кода может быть скелетом Maven-проекта и не отражать финальную архитектуру. Поэтому AI-агент должен получать контекст из документации, а не пытаться восстановить архитектуру только по текущим Java-файлам.

## Responsibilities

- Дать краткую mental model проекта.
- Объяснить, какие документы читать для разных задач.
- Зафиксировать ключевые бизнес- и архитектурные инварианты.
- Предупредить AI-агента о типичных неверных выводах.
- Связать бизнес, архитектуру, модули, БД, безопасность и тестирование.

## Non Responsibilities

- Документ не заменяет подробные документы разделов.
- Документ не вводит новые бизнес-правила.
- Документ не является backlog или roadmap.
- Документ не разрешает писать код без чтения релевантных документов.

## Design Decisions

## Project Mental Model

`skinemsya` — backend Telegram Mini App для разделения счета внутри группы. Пользовательский путь:

1. Пользователь входит через Telegram Mini App.
2. Backend проверяет Telegram init data.
3. Пользователь попадает в группу:
   - `CHAT_LINKED` — группа создана из Telegram-чата;
   - `STANDALONE` — группа создана вручную в Mini App.
4. В группе создается мероприятие.
5. Выбирается плательщик.
6. Позиции добавляются вручную или из распознанного чека.
7. Участники выбирают свои позиции.
8. Backend рассчитывает долги.
9. Должник смотрит реквизиты (`перевести`), переводит деньги вне системы и нажимает `перевел`.
10. Плательщик проверяет поступление и нажимает `получил`.
11. Долг закрывается.

Backend — не банк, не OCR-сервис и не микросервисная платформа. Backend — транзакционная система бизнес-правил.

## What Is MVP

MVP включает:

- Telegram authentication.
- JWT session.
- `CHAT_LINKED` и `STANDALONE` группы.
- Минимальная роль `owner`.
- Мероприятия.
- Ручные позиции.
- Загрузка чека и интеграция с Python ML-сервисом.
- Валидация JSON чека backend-ом.
- Расчет долгов.
- Ручное закрытие платежа через `перевести` → `перевел` → `получил`.
- Базовые уведомления.
- PostgreSQL + Liquibase.
- Unit и integration tests.

MVP не включает:

- Kafka.
- Микросервисы.
- СБП как обязательную интеграцию.
- Event sourcing.
- CQRS.
- Сложные роли.
- OCR внутри Java backend.
- Частичные платежи.

## Canonical Module Map

| Module | Owns | Must Not Own |
| --- | --- | --- |
| `app` | Spring Boot bootstrap, configuration, OpenAPI, security wiring | Business logic |
| `common` | Neutral shared primitives, errors, Money | Domain-specific rules |
| `auth` | Telegram auth, JWT, refresh tokens | User profile |
| `users` | User, Telegram identity, profile, payment details | Group membership |
| `groups` | Group, group type, group members, owner | Event lifecycle |
| `events` | Event, participants, payer, event status | Positions and debt calculation |
| `receipts` | Receipt, positions, selections, ML JSON normalization | Debt calculation |
| `debts` | Debt calculation, debt status, summaries | Payment confirmation details |
| `payments` | Payment operation, debtor/payer confirmations | Debt amount calculation |
| `notifications` | Notification records and delivery | Domain state ownership |
| `files` | Uploaded file metadata and storage references | Receipt parsing |
| `integrations` | Telegram/ML/file storage/SBP adapters | Business decisions |

## Document Reading Paths

### If implementing groups

Read:

1. `business/business-rules.md`
2. `business/domain-model.md`
3. `modules/groups.md`
4. `security/authorization.md`
5. `database/table-catalog.md`
6. `testing/integration-tests.md`

Pay attention to:

- `CHAT_LINKED` vs `STANDALONE`;
- owner role;
- `telegram_chat_id` nullable;
- membership required for event access.

### If implementing receipt processing

Read:

1. `architecture/receipt-processing-flow.md`
2. `modules/receipts.md`
3. `integrations/ml-service.md`
4. `integrations/receipt-json-contract.md`
5. `ml/ml-boundaries.md`
6. `testing/test-data.md`

Pay attention to:

- receipt is optional;
- manual positions are first-class;
- backend validates ML JSON;
- ML confidence is not business truth.

### If implementing debt calculation

Read:

1. `business/business-rules.md`
2. `modules/debts.md`
3. `modules/receipts.md`
4. `modules/events.md`
5. `testing/unit-tests.md`

Pay attention to:

- no floating point for money;
- deterministic rounding;
- payer does not owe himself;
- calculation after final selections only.

### If implementing payment flow

Read:

1. `architecture/payment-flow.md`
2. `modules/payments.md`
3. `modules/debts.md`
4. `adr/ADR-0007-manual-payment-first.md`
5. `security/api-security.md`

Pay attention to:

- `перевести` is read-only;
- `перевел` does not close debt;
- `получил` closes payment and debt;
- requisites are in user profile.

### If changing database

Read:

1. `database/erd.md`
2. `database/table-catalog.md`
3. `database/entity-relations.md`
4. `database/naming-conventions.md`
5. relevant `modules/*.md`

Pay attention to:

- module table ownership;
- no direct access to another module's repository;
- enum values stored as VARCHAR;
- money as kopecks.

## Global Invariants

- User access is always based on backend membership, not Telegram chat membership alone.
- `CHAT_LINKED` group has Telegram chat id; `STANDALONE` group does not.
- Event belongs to group.
- Payer belongs to group.
- Position belongs to event.
- Receipt belongs to event but event can exist without receipt.
- Debt belongs to event and debtor.
- Payment belongs to debt.
- Debt closes only after payer confirmation.
- Backend never trusts frontend to enforce business rules.
- Backend never trusts ML result without validation.

## Common Wrong Assumptions

| Wrong Assumption | Correct Model |
| --- | --- |
| Current Maven modules are final | Documentation defines 12 target modules |
| Groups are only Telegram-chat based | MVP also supports manual standalone groups |
| Receipt is required | Manual positions are enough |
| ML JSON can be stored as truth | Backend validates and normalizes into positions |
| Debt can close when debtor clicks `перевел` | Debt closes only after payer clicks `получил` |
| Payment details belong to event | Payment details belong to user profile |
| `common` can hold all shared enums | Domain enums stay in module owners |
| Kafka is needed for notifications | MVP uses simple in-process or direct calls |

## How To Use This Guide

1. Read this file first.
2. Use the reading paths to choose detailed docs.
3. Before changing code, identify module owner.
4. Before changing behavior, update business or architecture docs.
5. Before changing tables, update database docs.
6. Before adding a dependency, update module dependency docs.

## Constraints

- This guide is only an index and orientation layer.
- If this guide conflicts with detailed docs, detailed docs win unless user explicitly updates the requirement.
- AI agents must not skip detailed docs after reading this guide.

## Future Evolution

- Add examples of completed feature implementation.
- Add decision trees for common changes.
- Add generated link checks for all referenced docs.

## Related Documents

- `docs/ai/backend-implementation-prompt.md` — поэтапная реализация MVP и stop rule
- `docs/implementation-reports/template.md` — шаблон отчёта этапа
- `docs/ai/project-rules.md`
- `docs/ai/development-workflow.md`
- `docs/ai/module-rules.md`
- `docs/architecture/system-overview.md`
- `docs/business/mvp-scope.md`
- `docs/documentation-catalog.md`
