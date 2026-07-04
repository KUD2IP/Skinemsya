# AI: Module Rules

## Purpose

Правила модулей: владение объектами, контракты, зависимости, события, БД.

## Context

Каждый модуль владеет своей областью. AI-агенты должны знать правила при добавлении кода в модуль.

## Responsibilities

- Определить правила владения доменными объектами.
- Зафиксировать правила публичных контрактов.
- Описать правила зависимостей и БД-объектов.

## Non Responsibilities

- Документ не описывает конкретные классы (см. `modules/*.md`).

## Design Decisions

Владение:
- Модуль владеет своими domain objects, entities, tables.
- Другой модуль обращается через public contract (interface в `application` layer).
- Запрещен import entity/repository из другого модуля.

Владение означает, что только модуль-владелец может:

- создавать и изменять свои aggregate/domain objects;
- решать, какие состояния допустимы;
- выполнять persistence операций через repository;
- публиковать события о своих изменениях;
- определять public contract для других модулей.

Пример: `debts` владеет статусом долга. `payments` может инициировать закрытие долга только через public contract `DebtService.close(debtId)`. `payments` не должен менять `debts.status` напрямую через repository или SQL.

Public contracts:
- Interface в `{module}.application` package.
- DTO для cross-module communication — в public contract, не entity.
- Contract methods — use case oriented: `createGroup()`, `calculateDebts()`.
- Contracts stable — breaking change требует обновления docs.

Public contract должен быть узким и бизнес-ориентированным. Он не должен превращаться в generic CRUD API для чужих модулей.

Допустимо:

```java
GroupAccessService.requireMember(groupId, userId);
DebtService.calculateForEvent(eventId);
PaymentService.confirmByPayer(paymentId, payerId);
```

Недопустимо:

```java
GroupRepository findGroupRepository();
DebtEntity save(DebtEntity entity);
Map<String, Object> execute(String operation, Object payload);
```

Контракт должен скрывать внутреннюю persistence-модель. Если другой модуль просит поля entity, значит контракт спроектирован неправильно.

Зависимости:
- Только направленные, по `architecture/module-dependencies.md`.
- Новая зависимость — обновить module-dependencies.md + ADR если significant.
- `common` — types only, no domain logic.
- `integrations` — adapters only, no business decisions.

Разрешенная зависимость не означает, что можно использовать любые классы модуля. Даже если `payments` зависит от `debts`, он использует только application-level API `debts`, а не `debts.infrastructure`.

Запрещенные признаки нарушения:

- import из пакета `.infrastructure` другого модуля;
- import JPA entity другого модуля;
- SQL query к таблице другого модуля;
- DTO чужого REST API используется как внутренний доменный объект;
- `common` начинает содержать `DebtStatus`, `GroupRole`, `ReceiptStatus`.

Events:
- Event class в domain layer модуля-publisher.
- Event name: past tense — `GroupCreated`, `DebtsCalculated`.
- Listener в application layer модуля-consumer.
- MVP: Spring `@EventListener` (in-process), не Kafka.

События на MVP используются только для side effects, которые не должны менять результат основной транзакции. Пример: уведомление после расчета долгов. Если consumer падает, основная бизнес-операция не должна становиться непонятной. Для критичных изменений предпочтителен явный application service call.

Правило выбора:

| Ситуация | Использовать |
| --- | --- |
| Нужно вернуть результат пользователю в этом же request | Прямой вызов public contract |
| Нужно выполнить уведомление после изменения | Internal event |
| Нужно изменить состояние другого core-модуля в той же транзакции | Прямой вызов public contract |
| Нужно интегрироваться с внешней системой | Adapter в `integrations` |
| Нужно надежно доставить событие между сервисами | Post-MVP outbox + Kafka |

Database:
- Таблица принадлежит одному модулю (см. `database/table-catalog.md`).
- Repository в `infrastructure` layer владельца.
- Миграции: changeset в модуле-владельце или в `app`.
- Cross-module FK допустим на уровне БД, но не cross-module repository access.

FK между таблицами разных модулей — это ограничение целостности, а не разрешение на прямой доступ. Например, `events.payer_id` может ссылаться на `users.id`, но `events` не должен менять профиль пользователя. Для этого есть `users` public contract.

## Module Boundary Examples

### Создание мероприятия

1. `events` получает `groupId`, `payerId`, `creatorId`.
2. `events` вызывает `groups.requireMember(groupId, creatorId)`.
3. `events` вызывает `groups.requireMember(groupId, payerId)`.
4. `events` создает `Event` и `EventParticipant`.
5. `events` публикует `EventCreated`.

Что запрещено:

- `events` читает таблицу `group_members` через свой repository;
- `events` сам решает, является ли пользователь участником группы, без `groups`;
- `events` создает пользователя, если payer не найден.

### Расчет долгов

1. `debts` получает `eventId`.
2. `debts` получает участников через `events` public contract.
3. `debts` получает выбранные позиции через `receipts` public contract.
4. `debts` считает суммы и сохраняет `Debt`.
5. `debts` публикует `DebtsCalculated`.

Что запрещено:

- `debts` читает `positions` напрямую;
- `receipts` сам создает долги;
- `payments` пересчитывает суммы долга.

### Закрытие платежа

1. `payments` проверяет, что requester — должник или плательщик.
2. `payments` фиксирует `DEBTOR_CONFIRMED` после `перевел`.
3. `payments` фиксирует `PAYER_CONFIRMED` после `получил`.
4. `payments` вызывает `debts.close(debtId)`.

Что запрещено:

- `debts` хранит `debtor_confirmed_at`;
- `payments` меняет `debts` таблицу напрямую;
- долг закрывается после `перевел` без `получил`.

## Change Checklist

Перед изменением модуля агент проверяет:

- Является ли этот модуль владельцем изменяемого бизнес-объекта?
- Не требуется ли public contract другого модуля?
- Не появится ли новая зависимость?
- Нужно ли обновить `architecture/module-dependencies.md`?
- Нужно ли обновить `database/table-catalog.md`?
- Нужно ли добавить или изменить тестовые fixtures?

## Constraints

- Один модуль — одна ответственность.
- Не создавать `shared` module с бизнес-логикой.
- Не дублировать entities между модулями.

## Future Evolution

- API modules для extracted services.
- Automated dependency graph checks.
- Event schema registry.

## Related Documents

- `docs/architecture/module-dependencies.md`
- `docs/adr/ADR-0010-module-boundaries.md`
- `docs/modules/*.md`
- `docs/ai/forbidden-patterns.md`
