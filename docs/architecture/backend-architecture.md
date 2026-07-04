# Backend Architecture

## Purpose

Документ описывает backend как модульный монолит: состав модулей, уровни ответственности, правила взаимодействия и архитектурные ограничения MVP.

## Context

Backend должен поддержать полный пользовательский сценарий без микросервисной сложности. При этом проект не должен превратиться в неструктурированный монолит, где любой модуль напрямую меняет данные другого модуля. Поэтому выбран модульный монолит: единое приложение и единый деплой, но с явными границами доменных модулей.

## Responsibilities

- Определить архитектурную форму backend-приложения.
- Разделить проект на модули по доменным областям.
- Зафиксировать роль entrypoint-модуля `app`.
- Зафиксировать назначение `common` и `integrations`.
- Определить правила использования транзакций и публичных контрактов модулей.
- Сохранить возможность будущего выделения сервисов без переписывания доменной модели с нуля.

## Non Responsibilities

- Документ не описывает конкретные классы, DTO, REST endpoints или SQL-миграции.
- Документ не определяет финальную физическую структуру пакетов.
- Документ не заменяет ADR о выборе модульного монолита.
- Документ не разрешает микросервисы на этапе MVP.

## Design Decisions

Backend состоит из следующих модулей:

- `app`: запуск Spring Boot приложения, composition root, подключение конфигурации, security, OpenAPI.
- `common`: общие ошибки, базовые типы, утилиты и контракты, не принадлежащие конкретному домену.
- `auth`: Telegram authentication и выпуск JWT.
- `users`: пользователи и Telegram identity.
- `groups`: группы и участники.
- `events`: мероприятия внутри групп.
- `receipts`: чеки, позиции, выбор позиций, нормализация JSON.
- `debts`: расчет долгов и статусы задолженности.
- `payments`: платежные операции и закрытие долгов.
- `notifications`: уведомления пользователям.
- `files`: загрузка и хранение файлов чеков.
- `integrations`: адаптеры внешних систем.

Каждый доменный модуль должен владеть своей моделью, бизнес-правилами и persistence-объектами. Другие модули взаимодействуют с ним через публичные application-level контракты, а не через внутренние repository или entity.

`app` собирает приложение, но не содержит бизнес-логику. Это снижает риск превращения entrypoint-модуля в универсальный сервисный слой.

`common` не должен становиться свалкой доменной логики. Если тип относится к платежам, он остается в `payments`; если к чекам, он остается в `receipts`.

## Layering Model

Каждый доменный модуль должен иметь одинаковую внутреннюю форму. Это облегчает навигацию разработчикам и AI-агентам:

```text
{module}/
  api/              REST controllers, request/response DTOs
  application/      use cases, transactions, public contracts
  domain/           domain model, rules, domain errors
  infrastructure/   JPA entities, repositories, external adapter implementations
```

Правила слоев:

- `api` не содержит бизнес-логику. Controller валидирует request, вызывает application service и возвращает response.
- `application` координирует use case, транзакцию и вызовы других модулей через public contracts.
- `domain` содержит правила и состояния, которые можно тестировать без Spring и БД.
- `infrastructure` содержит технические детали: JPA, HTTP clients, storage implementations.
- `domain` не зависит от Spring, JPA, HTTP, Telegram или ML.

Если агент не знает, куда положить код, он должен определить, является ли это:

- user-facing endpoint → `api`;
- use case orchestration → `application`;
- business invariant or calculation → `domain`;
- persistence or external system → `infrastructure`.

## Module Ownership Matrix

| Бизнес-область | Module Owner | Что владеет | Что не владеет |
| --- | --- | --- | --- |
| Login, JWT, refresh | `auth` | токены, refresh sessions, Telegram auth flow | profile data |
| User identity/profile | `users` | users, telegram identity, payment details | group membership |
| Groups/members | `groups` | group type, owner, membership | event lifecycle |
| Event lifecycle | `events` | event, payer, participants, event status | position parsing |
| Receipt/positions | `receipts` | receipt, positions, selections | debt calculation |
| Debt calculation | `debts` | debt amount, debt status, summaries | payment confirmation |
| Payment operation | `payments` | payment status, debtor/payer confirmations | amount calculation |
| Files | `files` | stored file metadata and access | receipt JSON parsing |
| Notifications | `notifications` | notification records and delivery attempts | business state changes |
| External APIs | `integrations` | Telegram/ML/storage/SBP clients | business decisions |
| Shared primitives | `common` | neutral errors, Money, pagination | domain-specific enums |
| Runtime composition | `app` | Spring Boot bootstrap, wiring | business logic |

## Transaction Boundaries

MVP использует преимущества монолита: один request может выполнить несколько модульных операций в одной транзакции, если это нужно для согласованности. Но владение логикой сохраняется.

Примеры:

- Создание мероприятия: `events` транзакционно проверяет `groups` membership через public contract и сохраняет event.
- Расчет долгов: `debts` получает данные из `events` и `receipts`, затем сохраняет debts в одной транзакции.
- Подтверждение получения: `payments` переводит payment в `PAYER_CONFIRMED`, затем вызывает `debts.close(debtId)`.

Запреты:

- Не открывать транзакцию в controller.
- Не выполнять сетевые вызовы к ML/Telegram внутри долгой DB-транзакции, если результат не нужен для атомарного изменения.
- Не использовать distributed transaction patterns в MVP.

## How To Add A New Use Case

1. Найти модуль-владелец use case.
2. Проверить `business/business-rules.md`.
3. Проверить, какие public contracts нужны от других модулей.
4. Реализовать application service в модуле-владельце.
5. Доменную проверку вынести в domain rule/value object, если она повторяется или является инвариантом.
6. Persistence оставить в infrastructure.
7. Добавить unit-тесты для domain logic и integration-тест для API/security boundary.
8. Обновить module doc, если public contract или database objects изменились.

## Constraints

- Один backend artifact на MVP.
- Одна основная PostgreSQL database на MVP.
- Межмодульные зависимости должны быть направленными и явно описанными.
- Доменный модуль не должен напрямую обращаться к таблицам другого модуля.
- Интеграции с внешними системами должны проходить через адаптеры.
- Event Driven Architecture не внедряется до появления реального требования.

## Future Evolution

- Публичные контракты модулей могут стать API выделенных сервисов.
- Внутренние доменные события могут стать Kafka events после стабилизации бизнес-событий.
- Модули с высокой нагрузкой или отдельным жизненным циклом могут быть выделены первыми: `receipts`, `payments`, `notifications`.
- Транзакционные границы могут быть пересмотрены при переходе от единой БД к service-owned databases.

## Related Documents

- `docs/architecture/system-overview.md`
- `docs/architecture/module-dependencies.md`
- `docs/adr/ADR-0001-modular-monolith.md`
- `docs/adr/ADR-0010-module-boundaries.md`
- `docs/modules/app.md`
- `docs/modules/common.md`
- `docs/ai/architecture-rules.md`

