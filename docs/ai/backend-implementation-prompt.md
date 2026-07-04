# AI: Backend Implementation Prompt

## Purpose

Промпт для поэтапной реализации backend `skinemsya` AI-агентом. Документ фиксирует обязательный workflow, порядок этапов, запреты и правила остановки.

## Context

Проект реализуется как модульный монолит строго по документации в `docs/`. Документация — источник истины; текущий Maven-скелет может не отражать целевое состояние.

## Responsibilities

- Задать порядок чтения документов перед каждым этапом.
- Описать 6 этапов MVP и критерии завершения.
- Зафиксировать запреты и архитектурные ограничения.
- Определить структуру модулей и формат отчёта.

## Non Responsibilities

- Документ не заменяет детальные module/business/architecture docs.
- Документ не является backlog задач внутри этапа.

---

## Главный принцип

Документация является источником истины.

Перед каждым этапом агент обязан заново прочитать релевантные документы из `docs/`, даже если уже читал их ранее.

### Базовые документы (каждый этап)

1. [`docs/ai/context-guide.md`](context-guide.md)
2. [`docs/documentation-catalog.md`](../documentation-catalog.md)
3. [`docs/business/mvp-scope.md`](../business/mvp-scope.md)
4. [`docs/architecture/system-overview.md`](../architecture/system-overview.md)
5. [`docs/architecture/backend-architecture.md`](../architecture/backend-architecture.md)
6. [`docs/architecture/module-dependencies.md`](../architecture/module-dependencies.md)
7. [`docs/roadmap/mvp-roadmap.md`](../roadmap/mvp-roadmap.md)

Затем читать документы конкретного этапа.

---

## Обязательный workflow каждого этапа

1. Прочитать базовые документы.
2. Прочитать документы модулей, которые затрагиваются.
3. Прочитать связанные документы:
   - `docs/business/*`
   - `docs/architecture/*`
   - `docs/modules/*`
   - `docs/database/*`
   - `docs/security/*`
   - `docs/testing/*`
   - `docs/ai/*`
4. Сформировать краткий план этапа.
5. Реализовать только то, что входит в текущий этап.
6. Добавить или обновить тесты.
7. Запустить релевантные проверки.
8. Создать подробный markdown-отчёт в `docs/implementation-reports/` по шаблону [`template.md`](../implementation-reports/template.md).
9. **Остановиться** и ждать подтверждения пользователя перед следующим этапом.

---

## Запреты (MVP)

На MVP запрещено:

- микросервисы;
- Kafka, RabbitMQ, message brokers;
- Event Sourcing;
- CQRS;
- полноценная СБП-интеграция;
- OCR внутри Java backend;
- сложная ролевая модель;
- enterprise-паттерны без необходимости;
- generic `BaseService`;
- прямой доступ к таблицам чужого модуля;
- бизнес-логика в `app`, `common`, `integrations`;
- хранение секретов в коде;
- использование `float`/`double` для денег;
- использование Telegram username как стабильного идентификатора.

---

## Целевая архитектура

Проект реализуется как модульный монолит.

Целевые модули (12):

| Модуль | Назначение |
| --- | --- |
| `app` | Spring Boot bootstrap, wiring, security, OpenAPI |
| `common` | Нейтральные типы, ошибки, Money |
| `auth` | Telegram auth, JWT, refresh tokens |
| `users` | User, profile, payment details |
| `groups` | Groups, members, owner |
| `events` | Events, participants, payer |
| `receipts` | Receipts, positions, selections |
| `debts` | Debt calculation, statuses |
| `payments` | Payment operations, confirmations |
| `notifications` | Notification records, delivery |
| `files` | File metadata, storage references |
| `integrations` | External adapters (Telegram, ML, storage, SBP stub) |

Каноническое имя модуля интеграций — `integrations`. Документы модуля: [`docs/modules/integrations/integrations.md`](../modules/integrations/integrations.md).

Если текущая Maven-структура отличается от документации, считать документацию целевым состоянием.

---

## Структура модуля

В каждом доменном модуле использовать слои:

```text
{module}/
  api/              REST controllers, request/response DTOs
  application/      use cases, transactions, public contracts
  domain/           domain model, rules, domain errors
  infrastructure/   JPA entities, repositories, adapters, mappers
```

### Правила слоёв

- Пакеты: `skinemsya.vse.ru.{module}.{layer}`.
- `api` — валидация request, вызов application service, response. Без бизнес-логики.
- `application` — оркестрация use case, транзакции, вызовы других модулей через public contracts.
- `domain` — бизнес-правила и инварианты. **Без** Spring, JPA, HTTP, Telegram, ML.
- `infrastructure` — JPA entities, repositories, HTTP clients, storage, MapStruct mappers.
- Публичные контракты между модулями — в `application` (interfaces), не через repository/entity.
- Бизнес-логика только в owner-модуле.

Стандарты кода: [`docs/ai/coding-standards.md`](coding-standards.md), [`docs/ai/module-rules.md`](module-rules.md), [`docs/ai/forbidden-patterns.md`](forbidden-patterns.md).

---

## Порядок реализации MVP

Следовать [`docs/roadmap/mvp-roadmap.md`](../roadmap/mvp-roadmap.md).

### Этап 1 — Foundation

**Реализовать:**

- целевую Maven multi-module структуру;
- модули `app`, `common`;
- базовую Spring Boot конфигурацию;
- PostgreSQL datasource;
- Liquibase foundation;
- базовый error handling;
- базовый logging/correlation id;
- начальный test setup.

**Перед началом читать:**

- [`docs/modules/app/app.md`](../modules/app/app.md)
- [`docs/modules/common/common.md`](../modules/common/common.md)
- [`docs/deployment/local-development.md`](../deployment/local-development.md)
- [`docs/deployment/configuration-management.md`](../deployment/configuration-management.md)
- [`docs/database/naming-conventions.md`](../database/naming-conventions.md)
- [`docs/architecture/error-handling.md`](../architecture/error-handling.md)
- [`docs/architecture/logging-strategy.md`](../architecture/logging-strategy.md)
- [`docs/testing/testing-strategy.md`](../testing/testing-strategy.md)

**Критерий завершения:** `mvn clean install` success; приложение стартует с Liquibase migrate на PostgreSQL.

---

### Этап 2 — Auth & Users

**Реализовать:**

- Telegram init data authentication;
- JWT access/refresh;
- refresh token persistence;
- users;
- user profile;
- payment details in profile;
- auth integration tests.

**Перед началом читать:**

- [`docs/modules/auth/auth.md`](../modules/auth/auth.md)
- [`docs/modules/users/users.md`](../modules/users/users.md)
- [`docs/security/authentication.md`](../security/authentication.md)
- [`docs/security/jwt.md`](../security/jwt.md)
- [`docs/security/secrets-management.md`](../security/secrets-management.md)
- [`docs/integrations/telegram.md`](../integrations/telegram.md)
- [`docs/architecture/telegram-auth-flow.md`](../architecture/telegram-auth-flow.md)
- [`docs/adr/ADR-0006-jwt-authentication.md`](../adr/ADR-0006-jwt-authentication.md)

**Критерий завершения:** login через Telegram → JWT → GET /users/me.

---

### Этап 3 — Groups & Events

**Реализовать:**

- `CHAT_LINKED` группы;
- `STANDALONE` группы;
- group members;
- owner role;
- создание мероприятий;
- выбор плательщика;
- event participants;
- authorization checks.

**Перед началом читать:**

- [`docs/modules/groups/groups.md`](../modules/groups/groups.md)
- [`docs/modules/events/events.md`](../modules/events/events.md)
- [`docs/security/authorization.md`](../security/authorization.md)
- [`docs/business/business-rules.md`](../business/business-rules.md)
- [`docs/business/domain-model.md`](../business/domain-model.md)
- [`docs/business/user-flows.md`](../business/user-flows.md)
- [`docs/database/table-catalog.md`](../database/table-catalog.md)

**Критерий завершения:** создать группу (оба типа) → создать мероприятие.

---

### Этап 4 — Files, Receipts & Positions

**Реализовать:**

- загрузку файлов чеков;
- file metadata;
- ручные позиции;
- receipt upload;
- ML-service adapter;
- receipt JSON validation;
- создание позиций из JSON;
- выбор позиций;
- отправку на распределение.

**Перед началом читать:**

- [`docs/modules/files/files.md`](../modules/files/files.md)
- [`docs/modules/receipts/receipts.md`](../modules/receipts/receipts.md)
- [`docs/modules/integrations/integrations.md`](../modules/integrations/integrations.md)
- [`docs/integrations/file-storage.md`](../integrations/file-storage.md)
- [`docs/integrations/ml-service.md`](../integrations/ml-service.md)
- [`docs/integrations/receipt-json-contract.md`](../integrations/receipt-json-contract.md)
- [`docs/architecture/receipt-processing-flow.md`](../architecture/receipt-processing-flow.md)
- [`docs/ml/ml-boundaries.md`](../ml/ml-boundaries.md)
- [`docs/testing/test-data.md`](../testing/test-data.md)

**Критерий завершения:** добавить позиции (ручные + из чека) → выбор → распределение.

---

### Этап 5 — Debts & Payments

**Реализовать:**

- расчет долгов;
- deterministic rounding;
- debt statuses;
- main debt summary;
- payment operation;
- `перевести`;
- `перевел`;
- `получил`;
- закрытие долга после подтверждения плательщика.

**Перед началом читать:**

- [`docs/modules/debts/debts.md`](../modules/debts/debts.md)
- [`docs/modules/payments/payments.md`](../modules/payments/payments.md)
- [`docs/architecture/payment-flow.md`](../architecture/payment-flow.md)
- [`docs/business/business-rules.md`](../business/business-rules.md)
- [`docs/adr/ADR-0007-manual-payment-first.md`](../adr/ADR-0007-manual-payment-first.md)
- [`docs/testing/unit-tests.md`](../testing/unit-tests.md)
- [`docs/testing/integration-tests.md`](../testing/integration-tests.md)

**Критерий завершения:** полный payment flow, debt closed.

---

### Этап 6 — Notifications & Polish

**Реализовать:**

- базовые уведомления;
- notification records;
- Telegram bot adapter usage;
- финальные integration tests happy path;
- улучшение ошибок и логов;
- актуализацию OpenAPI.

**Перед началом читать:**

- [`docs/modules/notifications/notifications.md`](../modules/notifications/notifications.md)
- [`docs/integrations/telegram.md`](../integrations/telegram.md)
- [`docs/architecture/logging-strategy.md`](../architecture/logging-strategy.md)
- [`docs/architecture/error-handling.md`](../architecture/error-handling.md)
- [`docs/security/api-security.md`](../security/api-security.md)
- [`docs/testing/testing-strategy.md`](../testing/testing-strategy.md)

**Критерий завершения:** MVP scope criteria met (см. [`docs/business/mvp-scope.md`](../business/mvp-scope.md)).

---

## Формат отчёта этапа

После каждого этапа создать файл `docs/implementation-reports/phase-{N}-{name}.md` по шаблону [`template.md`](../implementation-reports/template.md).

Обязательные разделы:

1. **Metadata** — этап, дата, статус.
2. **Прочитанные документы** — чеклист с отметками.
3. **Scope** — что входило / что сознательно исключено.
4. **Изменённые файлы** — список с кратким описанием.
5. **Архитектурные решения** — ключевые выборы и обоснование по docs.
6. **Тесты** — добавленные тест-классы и результаты команд.
7. **Проверки** — выполненные команды (`mvn clean install`, curl, и т.д.).
8. **Known gaps** — tech debt, отложенное на следующие этапы.
9. **Критерий завершения** — выполнен / не выполнен.
10. **Stop** — явное сообщение об остановке и ожидании подтверждения.

---

## Stop rule

После создания отчёта этапа агент **обязан остановиться**. Не начинать следующий этап без явного подтверждения пользователя.

---

## Constraints

- Не расширять scope этапа без запроса пользователя.
- Не пропускать чтение документации.
- Не коммитить без явного запроса.
- Обновлять docs при изменении контрактов или бизнес-правил.

## Future Evolution

- Добавить чеклисты по каждому этапу.
- Автоматическая валидация отчётов в CI.

## Related Documents

- [`docs/ai/context-guide.md`](context-guide.md)
- [`docs/ai/development-workflow.md`](development-workflow.md)
- [`docs/ai/project-rules.md`](project-rules.md)
- [`docs/ai/coding-standards.md`](coding-standards.md)
- [`docs/implementation-reports/template.md`](../implementation-reports/template.md)
- [`docs/roadmap/mvp-roadmap.md`](../roadmap/mvp-roadmap.md)
