# Implementation Report: Phase 3 — Groups & Events

## Metadata

| Поле | Значение |
| --- | --- |
| Этап | 3 — Groups & Events |
| Дата | 2026-06-28 |
| Статус | completed |
| Агент / автор | Cursor AI Agent |

---

## Прочитанные документы

### Базовые (обязательные)

- [x] `docs/ai/context-guide.md`
- [x] `docs/documentation-catalog.md`
- [x] `docs/business/mvp-scope.md`
- [x] `docs/architecture/system-overview.md`
- [x] `docs/architecture/backend-architecture.md`
- [x] `docs/architecture/module-dependencies.md`
- [x] `docs/roadmap/mvp-roadmap.md`

### Документы этапа

- [x] `docs/integrations/telegram.md`
- [x] `docs/modules/groups/groups.md`
- [x] `docs/modules/events/events.md`
- [x] `docs/business/business-rules.md`
- [x] `docs/business/domain-model.md`
- [x] `docs/security/authorization.md`
- [x] `docs/database/table-catalog.md`
- [x] `docs/testing/integration-tests.md`

---

## Scope

### In scope (реализовано)

**Модуль `integrations` (расширение):**
- `TelegramChatContext`, `TelegramInitData` — identity + optional chat
- `TelegramInitDataValidator.validateWithChat()` — парсинг подписанного поля `chat` после проверки hash
- Unit-тесты: initData с chat и без chat

**Модуль `groups`:**
- Liquibase: `groups`, `group_members` (`20250628-001`)
- Domain: `Group`, `GroupMember`, `GroupType`, `GroupRole`
- `GroupService` / `GroupAccessService`, `GroupDeletionGuard` (интерфейс)
- REST API: standalone, chat-linked, list/get/update/delete, add member
- CHAT_LINKED: chat id из подписанного initData, idempotent join
- Unit-тест `GroupServiceTest`

**Модуль `events`:**
- Liquibase: `events`, `event_participants` (`20250628-002`), колонка `created_by` для auth delete
- Domain: `Event`, `EventParticipant`, `EventStatus`
- `EventService` — create/update/list/delete, participants = все members группы
- `EventServiceImpl` реализует `GroupDeletionGuard` (блок delete группы при active events)
- REST API под `/api/v1/groups/{groupId}/events` и `/api/v1/events/{id}`
- Unit-тест `EventServiceTest`

**Модуль `app` (wiring):**
- Зависимости `groups`, `events`
- Liquibase includes `20250628-001`, `20250628-002`
- `TelegramInitDataTestHelper.buildInitDataWithChat()`
- `LiquibaseMigrationTest` — проверка новых changesets
- `GroupsFlowIntegrationTest`, `GroupsEventsFlowIntegrationTest` (Testcontainers)

### Out of scope (сознательно не делалось)

- Positions, receipts, ML, `sendToDistribution` (Этап 4)
- Debts/payments blocking для delete группы (только events guard)
- Leave group, invite links, admin roles
- Kafka, domain events publishing
- WireMock Telegram Bot API

---

## Изменённые файлы

### Модуль `integrations`

| Файл | Действие | Описание |
| --- | --- | --- |
| `integrations/.../TelegramChatContext.java` | added | Chat id, title, type из initData |
| `integrations/.../TelegramInitData.java` | added | Identity + Optional chat |
| `integrations/.../TelegramInitDataValidator.java` | modified | `validateWithChat()` |
| `integrations/.../TelegramInitDataValidatorImpl.java` | modified | Парсинг chat после hash check |
| `integrations/.../TelegramInitDataValidatorTest.java` | modified | Тесты chat extraction |

### Модуль `groups`

| Файл | Действие | Описание |
| --- | --- | --- |
| `groups/pom.xml` | modified | common, users, integrations, JPA, web, mapstruct |
| `groups/.../20250628-001-groups.xml` | added | Таблицы groups, group_members |
| `groups/.../Group*.java` | added | Domain, service, JPA, API, DTOs |
| `groups/.../GroupServiceTest.java` | added | Unit-тесты |

### Модуль `events`

| Файл | Действие | Описание |
| --- | --- | --- |
| `events/pom.xml` | modified | common, users, groups, JPA, web, mapstruct |
| `events/.../20250628-002-events.xml` | added | Таблицы events, event_participants |
| `events/.../Event*.java` | added | Domain, service, JPA, API, DTOs |
| `events/.../EventServiceTest.java` | added | Unit-тесты |

### Модуль `app`

| Файл | Действие | Описание |
| --- | --- | --- |
| `app/pom.xml` | modified | +groups, +events |
| `app/.../db.changelog-master.xml` | modified | includes groups + events |
| `app/.../LiquibaseMigrationTest.java` | modified | changesets 20250628-001/002 |
| `app/.../TelegramInitDataTestHelper.java` | modified | buildInitDataWithChat |
| `app/.../IntegrationTestSupport.java` | added | authenticate, fetchUserId |
| `app/.../GroupsFlowIntegrationTest.java` | added | Groups flow |
| `app/.../GroupsEventsFlowIntegrationTest.java` | added | Full criterion path |

---

## Архитектурные решения

### CHAT_LINKED через подписанный initData

**Контекст:** группа привязана к Telegram chat; chat id нельзя доверять из `initDataUnsafe`.

**Решение:** `POST /api/v1/groups/chat-linked` принимает `{ initData }`, backend валидирует подпись и извлекает `chat`.

**Обоснование (docs):** `docs/integrations/telegram.md`, `docs/modules/groups/groups.md`

### GroupDeletionGuard в events

**Контекст:** delete группы блокируется при наличии active events; circular dep groups ↔ events.

**Решение:** интерфейс `GroupDeletionGuard` в `groups`, реализация в `EventServiceImpl`; `GroupServiceImpl` инжектит `Optional<GroupDeletionGuard>`.

**Обоснование (docs):** `docs/architecture/module-dependencies.md`

### created_by на events

**Контекст:** delete event — creator или group owner; в plan liquibase нет явной колонки.

**Решение:** добавлена `created_by BIGINT NOT NULL` для корректной authorization без эвристик.

---

## Тесты

### Добавленные тест-классы

| Класс | Модуль | Тип | Что проверяет |
| --- | --- | --- | --- |
| `GroupServiceTest` | groups | unit | standalone, chat idempotent, add member, non-owner |
| `EventServiceTest` | events | unit | create DRAFT, payer not member, update payer |
| `GroupsFlowIntegrationTest` | app | integration | standalone, chat-linked, idempotent, 422 без chat, add member, 403 |
| `GroupsEventsFlowIntegrationTest` | app | integration | event create standalone/chat-linked, 403/422 |

### Результаты

```text
[INFO] BUILD SUCCESS
[INFO] Total time:  23.110 s
[INFO] groups ............................................. SUCCESS
[INFO] events ............................................. SUCCESS
[INFO] app ................................................ SUCCESS
```

---

## Проверки

| Команда | Результат |
| --- | --- |
| `mvn clean install` | PASS |
| `docker compose up -d` | PASS (postgres уже running) |
| `mvn spring-boot:run -pl skinemsya_parent/app` | N/A (smoke через integration tests) |
| Smoke curl (standalone → chat-linked → event) | N/A (покрыто integration tests) |

Пример smoke после login:

```bash
curl -X POST http://localhost:8080/api/v1/groups/standalone \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Друзья"}'

curl -X POST http://localhost:8080/api/v1/groups/chat-linked \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"initData":"..."}'

curl -X POST http://localhost:8080/api/v1/groups/1/events \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Ужин","payerId":1}'
```

---

## Known gaps / Tech debt

- `created_by` добавлен в events schema сверх минимального списка колонок в plan (нужен для delete auth)
- CHAT_LINKED endpoint всегда возвращает 200 (не 201 при первом создании) — допустимо по plan «201 or 200 join»
- Нет endpoint list members группы (не требовался в Phase 3)

---

## Критерий завершения этапа

**Из roadmap:** создать группу (оба типа) → создать мероприятие

**Статус:** выполнен

**Комментарий:** `GroupsEventsFlowIntegrationTest` покрывает STANDALONE + CHAT_LINKED → event в статусе DRAFT с payerId.

---

## Следующий этап

**Этап 4 — Positions & Receipts** — не начинать без подтверждения пользователя.

Ожидаемый scope: positions, receipt upload, ML integration, `sendToDistribution`.

---

## Stop

Этап завершён. Ожидаю подтверждения перед переходом к Этапу 4.
