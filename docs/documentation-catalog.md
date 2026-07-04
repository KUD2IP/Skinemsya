# Documentation Catalog

## Purpose

Каталог фиксирует полный план документационной базы backend-проекта `skinemsya`.
Документ используется как контрольная точка этапа 1: он описывает будущие документы, но не заменяет их содержимое.

## Scope

Документация проектируется для MVP Telegram Mini App по разделению счетов.
Архитектурная модель MVP: модульный монолит на Java 21, Spring Boot 3.x, PostgreSQL и Liquibase.

Документы должны объяснять:

- что делает компонент;
- зачем он нужен;
- почему выбрано текущее решение;
- какие ограничения есть у MVP;
- как решение может развиваться без преждевременного усложнения.

## Required Document Sections

Каждый обычный документ должен содержать разделы:

- `Purpose`
- `Context`
- `Responsibilities`
- `Non Responsibilities`
- `Design Decisions`
- `Constraints`
- `Future Evolution`
- `Related Documents`

Каждый документ модуля должен содержать разделы:

- `Purpose`
- `Responsibilities`
- `Domain Objects`
- `Dependencies`
- `Events`
- `Database Objects`
- `Public Contracts`
- `Future Extensions`

Каждый ADR-документ должен дополнительно содержать раздел:

- `Status` — `Accepted`, `Proposed` или `Superseded`

## Target Module Structure

Документация описывает целевую архитектуру из 12 модулей: `app`, `common`, `auth`, `users`, `groups`, `events`, `receipts`, `debts`, `payments`, `notifications`, `files`, `integrations`.

Текущий Maven-скелет в `skinemsya_parent/pom.xml` приводится к этой структуре поэтапно. Каноническое имя модуля интеграций — `integrations` (Maven artifactId и директория).

## Documentation Status

| Path | Status | Notes |
| --- | --- | --- |
| `docs/documentation-catalog.md` | complete | Каталог и статус документации |
| `docs/architecture/system-overview.md` | complete | |
| `docs/architecture/backend-architecture.md` | complete | |
| `docs/architecture/module-dependencies.md` | complete | |
| `docs/architecture/request-flow.md` | complete | |
| `docs/architecture/telegram-auth-flow.md` | complete | |
| `docs/architecture/payment-flow.md` | complete | |
| `docs/architecture/receipt-processing-flow.md` | complete | |
| `docs/architecture/error-handling.md` | complete | |
| `docs/architecture/logging-strategy.md` | complete | |
| `docs/adr/ADR-0001-modular-monolith.md` | complete | |
| `docs/adr/ADR-0002-java21.md` | complete | |
| `docs/adr/ADR-0003-postgresql.md` | complete | |
| `docs/adr/ADR-0004-liquibase.md` | complete | |
| `docs/adr/ADR-0005-mapstruct.md` | complete | |
| `docs/adr/ADR-0006-jwt-authentication.md` | complete | |
| `docs/adr/ADR-0007-manual-payment-first.md` | complete | |
| `docs/adr/ADR-0008-receipt-processing.md` | complete | |
| `docs/adr/ADR-0009-python-ml-service.md` | complete | |
| `docs/adr/ADR-0010-module-boundaries.md` | complete | |
| `docs/business/domain-model.md` | complete | |
| `docs/business/business-rules.md` | complete | |
| `docs/business/user-flows.md` | complete | |
| `docs/business/glossary.md` | complete | |
| `docs/business/use-cases.md` | complete | |
| `docs/business/mvp-scope.md` | complete | |
| `docs/business/post-mvp-features.md` | complete | |
| `docs/product/ux-checklist.md` | complete | UX-чеклист, терминология UI, happy path, чеклисты по ролям |
| `docs/database/erd.md` | complete | |
| `docs/database/entity-relations.md` | complete | |
| `docs/database/table-catalog.md` | complete | |
| `docs/database/indexing-strategy.md` | complete | |
| `docs/database/audit-strategy.md` | complete | |
| `docs/database/soft-delete-strategy.md` | complete | |
| `docs/database/naming-conventions.md` | complete | |
| `docs/modules/common.md` | complete | |
| `docs/modules/app.md` | complete | |
| `docs/modules/auth.md` | complete | |
| `docs/modules/users.md` | complete | |
| `docs/modules/groups.md` | complete | |
| `docs/modules/events.md` | complete | |
| `docs/modules/receipts.md` | complete | |
| `docs/modules/debts.md` | complete | |
| `docs/modules/payments.md` | complete | |
| `docs/modules/notifications.md` | complete | |
| `docs/modules/files.md` | complete | |
| `docs/modules/integrations/integrations.md` | complete | |
| `docs/integrations/telegram.md` | complete | |
| `docs/integrations/sbp.md` | complete | |
| `docs/integrations/ml-service.md` | complete | |
| `docs/integrations/receipt-json-contract.md` | complete | |
| `docs/integrations/file-storage.md` | complete | |
| `docs/security/authentication.md` | complete | |
| `docs/security/authorization.md` | complete | |
| `docs/security/jwt.md` | complete | |
| `docs/security/secrets-management.md` | complete | |
| `docs/security/api-security.md` | complete | |
| `docs/testing/testing-strategy.md` | complete | |
| `docs/testing/unit-tests.md` | complete | |
| `docs/testing/integration-tests.md` | complete | |
| `docs/testing/testcontainers.md` | complete | |
| `docs/testing/test-data.md` | complete | |
| `docs/deployment/local-development.md` | complete | |
| `docs/deployment/docker.md` | complete | |
| `docs/deployment/environments.md` | complete | |
| `docs/deployment/configuration-management.md` | complete | |
| `docs/deployment/ci-cd.md` | complete | |
| `docs/roadmap/mvp-roadmap.md` | complete | |
| `docs/roadmap/post-mvp-roadmap.md` | complete | |
| `docs/roadmap/scaling-roadmap.md` | complete | |
| `docs/ml/receipt-recognition.md` | complete | |
| `docs/ml/ml-boundaries.md` | complete | |
| `docs/ml/ml-future-evolution.md` | complete | |
| `docs/onboarding/getting-started.md` | complete | |
| `docs/onboarding/project-structure.md` | complete | |
| `docs/onboarding/glossary.md` | complete | |
| `docs/onboarding/development-workflow.md` | complete | |
| `docs/ai/project-rules.md` | complete | |
| `docs/ai/development-workflow.md` | complete | |
| `docs/ai/coding-standards.md` | complete | |
| `docs/ai/forbidden-patterns.md` | complete | |
| `docs/ai/architecture-rules.md` | complete | |
| `docs/ai/module-rules.md` | complete | |
| `docs/ai/context-guide.md` | complete | Стартовая карта контекста для AI-агентов |
| `docs/ai/backend-implementation-prompt.md` | complete | Поэтапная реализация MVP, workflow и stop rule |
| `docs/implementation-reports/template.md` | complete | Шаблон отчёта завершения этапа |
| `docs/implementation-reports/phase-2-auth-users.md` | complete | Отчёт Этапа 2 — Auth & Users |

**Прогресс:** 87 complete / 87 total (100%).

## Documentation Tree

```text
docs/
  architecture/
  adr/
  business/
  product/
  database/
  modules/
  integrations/
  security/
  testing/
  deployment/
  roadmap/
  ml/
  onboarding/
  ai/
```

## Architecture

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/architecture/system-overview.md` | Описать систему целиком. | Границы backend, роль Telegram Mini App, Python ML-сервиса, PostgreSQL, СБП и внешнего файлового хранения. Фиксирует MVP-контур без микросервисов и Kafka. | `business/mvp-scope.md`, `business/domain-model.md`, `integrations/telegram.md`, `integrations/ml-service.md`, `integrations/sbp.md` |
| `docs/architecture/backend-architecture.md` | Описать backend как модульный монолит. | Слои приложения, роль `app`, модулей домена, `common`, `integrations`, правила взаимодействия внутри монолита. | `architecture/system-overview.md`, `architecture/module-dependencies.md`, `adr/ADR-0001-modular-monolith.md`, `adr/ADR-0010-module-boundaries.md` |
| `docs/architecture/module-dependencies.md` | Зафиксировать допустимые зависимости модулей. | Матрица зависимостей между `auth`, `users`, `groups`, `events`, `receipts`, `debts`, `payments`, `notifications`, `files`, `integrations`, `common`. | `architecture/backend-architecture.md`, `modules/common.md`, `ai/module-rules.md` |
| `docs/architecture/request-flow.md` | Описать общий путь HTTP-запроса. | Вход через REST/OpenAPI, security filter chain, application service, доменная логика, транзакции, ошибки и ответ клиенту. | `security/authentication.md`, `security/authorization.md`, `architecture/error-handling.md` |
| `docs/architecture/telegram-auth-flow.md` | Описать аутентификацию через Telegram. | Проверка Telegram init data, создание или обновление пользователя, выдача JWT, ограничения доверия к данным Telegram. | `integrations/telegram.md`, `security/authentication.md`, `security/jwt.md`, `adr/ADR-0006-jwt-authentication.md` |
| `docs/architecture/payment-flow.md` | Описать жизненный цикл оплаты долга. | Создание долга, указание реквизитов плательщика, подтверждение перевода должником и получения плательщиком, будущий переход к СБП. | `business/business-rules.md`, `modules/debts.md`, `modules/payments.md`, `integrations/sbp.md`, `adr/ADR-0007-manual-payment-first.md` |
| `docs/architecture/receipt-processing-flow.md` | Описать обработку чека после OCR. | Получение структурированного JSON от Python-сервиса, валидация, нормализация, создание доменных моделей чека и позиций. | `modules/receipts.md`, `integrations/ml-service.md`, `integrations/receipt-json-contract.md`, `adr/ADR-0008-receipt-processing.md`, `adr/ADR-0009-python-ml-service.md` |
| `docs/architecture/error-handling.md` | Унифицировать ошибки backend. | Категории ошибок, HTTP-коды, доменные ошибки, ошибки интеграций, формат ответа, трассируемость. | `architecture/request-flow.md`, `security/api-security.md`, `testing/integration-tests.md` |
| `docs/architecture/logging-strategy.md` | Описать подход к логированию. | Уровни логирования, correlation id, запрет чувствительных данных, события платежей и чеков, требования для DevOps. | `security/secrets-management.md`, `deployment/environments.md`, `architecture/error-handling.md` |

## ADR

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/adr/ADR-0001-modular-monolith.md` | Зафиксировать выбор модульного монолита. | Обоснование отказа от микросервисов и Kafka на MVP ради скорости разработки, простоты деплоя и сопровождения. | `architecture/backend-architecture.md`, `business/mvp-scope.md` |
| `docs/adr/ADR-0002-java21.md` | Зафиксировать Java 21. | Причины выбора LTS-версии, совместимость со Spring Boot 3.x, влияние на разработку и тестирование. | `deployment/local-development.md`, `testing/testing-strategy.md` |
| `docs/adr/ADR-0003-postgresql.md` | Зафиксировать PostgreSQL. | Обоснование выбора основной БД для транзакционных данных групп, чеков, долгов и платежей. | `database/erd.md`, `database/indexing-strategy.md` |
| `docs/adr/ADR-0004-liquibase.md` | Зафиксировать Liquibase. | Управление схемой БД через миграции, контроль порядка изменений, требования к локальной и CI-среде. | `database/naming-conventions.md`, `deployment/ci-cd.md` |
| `docs/adr/ADR-0005-mapstruct.md` | Зафиксировать MapStruct. | Использование compile-time mapping вместо reflection-based mapping для простоты, типобезопасности и скорости. | `ai/coding-standards.md`, `modules/common.md` |
| `docs/adr/ADR-0006-jwt-authentication.md` | Зафиксировать JWT-аутентификацию. | Сессия Mini App через access token и refresh token после проверки Telegram init data, сроки жизни токенов, MVP-ограничения. | `security/jwt.md`, `architecture/telegram-auth-flow.md` |
| `docs/adr/ADR-0007-manual-payment-first.md` | Зафиксировать ручной платежный сценарий на MVP. | Почему MVP начинает с реквизитов плательщика и двухстороннего подтверждения оплаты, а СБП остается будущим развитием. | `integrations/sbp.md`, `architecture/payment-flow.md`, `business/mvp-scope.md` |
| `docs/adr/ADR-0008-receipt-processing.md` | Зафиксировать границы обработки чеков. | Backend не делает OCR, но валидирует и превращает структурированный JSON в доменные модели. | `architecture/receipt-processing-flow.md`, `modules/receipts.md` |
| `docs/adr/ADR-0009-python-ml-service.md` | Зафиксировать отдельный Python ML-сервис. | Разделение Java backend и ML/OCR-контура, причины для MVP и будущая интеграционная модель. | `integrations/ml-service.md`, `ml/receipt-recognition.md` |
| `docs/adr/ADR-0010-module-boundaries.md` | Зафиксировать правила границ модулей. | Запрет неявных зависимостей, правила публичных контрактов, допустимое использование `common` и `integrations`. | `architecture/module-dependencies.md`, `ai/module-rules.md` |

## Business

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/business/domain-model.md` | Описать бизнес-домен. | Основные сущности: пользователь, группа, участник, мероприятие, чек, позиция, долг, платеж. | `business/glossary.md`, `database/entity-relations.md` |
| `docs/business/business-rules.md` | Зафиксировать бизнес-правила. | Правила выбора позиций, расчета долей, округлений, долгов, статусов оплаты и закрытия мероприятия. | `business/domain-model.md`, `architecture/payment-flow.md`, `modules/debts.md` |
| `docs/business/user-flows.md` | Описать пользовательские сценарии. | Основной сценарий через Telegram-чат и альтернативный поток ручного создания группы; путь от группы до закрытия долга. | `business/use-cases.md`, `architecture/request-flow.md` |
| `docs/business/glossary.md` | Унифицировать терминологию. | Термины проекта на русском и технические соответствия для разработки, тестирования и AI-агентов. | Нет |
| `docs/business/use-cases.md` | Описать use cases MVP. | Сценарии создания группы (из чата и вручную), мероприятия, загрузки чека, выбора позиций, оплаты и закрытия долга. | `business/mvp-scope.md`, `business/user-flows.md` |
| `docs/business/mvp-scope.md` | Зафиксировать объем MVP. | Что входит в первую версию, включая ручное и автоматическое создание групп; что исключено; критерии готовности. | `roadmap/mvp-roadmap.md`, `adr/ADR-0001-modular-monolith.md` |
| `docs/business/post-mvp-features.md` | Зафиксировать идеи после MVP. | Улучшения после запуска: роли, история, сложные способы деления, продвинутая аналитика, события, Kafka. | `roadmap/post-mvp-roadmap.md`, `architecture/system-overview.md` |

## Product

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/product/ux-checklist.md` | Зафиксировать UX-спецификацию MVP. | Принципы простоты, happy path, UI-терминология, карта экранов, модель подтверждения оплат, чеклисты по ролям (BA, архитектор, дизайнер, разработка). | `business/user-flows.md`, `business/glossary.md`, `architecture/payment-flow.md`, `modules/notifications/notifications.md` |

## Database

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/database/erd.md` | Описать ERD на уровне MVP. | Диаграмма таблиц и связей для пользователей, групп, событий, чеков, долгов и платежей. | `business/domain-model.md`, `database/entity-relations.md` |
| `docs/database/entity-relations.md` | Описать связи сущностей. | Кардинальности, владение данными, правила удаления, ограничения целостности. | `business/domain-model.md`, `database/soft-delete-strategy.md` |
| `docs/database/table-catalog.md` | Описать каталог таблиц. | Назначение каждой таблицы, ключевые поля, индексы, связи, владение модулем. | `database/erd.md`, `modules/*.md` |
| `docs/database/indexing-strategy.md` | Описать стратегию индексов. | Индексы для Telegram user id, групп, мероприятий, долгов, платежей и поиска активных записей. | `database/table-catalog.md`, `deployment/environments.md` |
| `docs/database/audit-strategy.md` | Описать аудит изменений. | Минимальный MVP-аудит: timestamps, created/updated by при необходимости, без enterprise event sourcing. | `database/table-catalog.md`, `security/api-security.md` |
| `docs/database/soft-delete-strategy.md` | Описать подход к удалению. | Где нужен soft delete, где допустимо физическое удаление, влияние на долги и платежи. | `business/business-rules.md`, `database/entity-relations.md` |
| `docs/database/naming-conventions.md` | Зафиксировать имена БД. | Правила имен таблиц, колонок, constraints, индексов, Liquibase changesets. | `adr/ADR-0004-liquibase.md`, `ai/coding-standards.md` |

## Modules

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/modules/common.md` | Описать общий модуль. | Общие типы, ошибки, базовые утилиты, shared contracts, которые не принадлежат конкретному домену. | `architecture/module-dependencies.md`, `ai/module-rules.md` |
| `docs/modules/app.md` | Описать entrypoint-модуль приложения. | Spring Boot bootstrap, wiring модулей, конфигурация приложения, OpenAPI и security composition. | `architecture/backend-architecture.md`, `deployment/configuration-management.md` |
| `docs/modules/auth.md` | Описать модуль аутентификации. | Telegram login, выдача JWT, обновление пользовательской сессии, security integration. | `architecture/telegram-auth-flow.md`, `security/authentication.md`, `security/jwt.md` |
| `docs/modules/users.md` | Описать модуль пользователей. | Профиль пользователя, Telegram identity, базовые пользовательские настройки и связи с группами. | `business/domain-model.md`, `integrations/telegram.md` |
| `docs/modules/groups.md` | Описать модуль групп. | Два типа групп: `CHAT_LINKED` (авто из Telegram-чата) и `STANDALONE` (ручное создание). Участники, роль `owner`, присоединение из чата и ручное добавление участников. | `business/user-flows.md`, `modules/users.md` |
| `docs/modules/events.md` | Описать модуль мероприятий. | Мероприятие внутри группы, участники мероприятия, плательщик, ручные позиции, позиции из чека и статус завершения. | `business/business-rules.md`, `modules/groups.md`, `modules/receipts.md` |
| `docs/modules/receipts.md` | Описать модуль чеков. | Прием структурированного JSON, нормализация и создание позиций из чека как дополнительный способ заполнения мероприятия. | `architecture/receipt-processing-flow.md`, `integrations/receipt-json-contract.md`, `ml/receipt-recognition.md` |
| `docs/modules/debts.md` | Описать модуль долгов. | Расчет долгов, статусы, связь с выбранными позициями, закрытие после оплаты. | `business/business-rules.md`, `architecture/payment-flow.md`, `modules/payments.md` |
| `docs/modules/payments.md` | Описать модуль платежей. | Платежные операции, реквизиты плательщика, двухстороннее подтверждение оплаты и связь с долгом. | `integrations/sbp.md`, `modules/debts.md`, `security/api-security.md` |
| `docs/modules/notifications.md` | Описать модуль уведомлений. | Уведомления пользователям о выборе позиций, долгах, оплатах и изменениях мероприятия. | `integrations/telegram.md`, `modules/events.md`, `modules/debts.md` |
| `docs/modules/files.md` | Описать модуль файлов. | Загрузка изображений чеков, хранение ссылок, связь с ML-сервисом и ограничение доступа. | `integrations/file-storage.md`, `modules/receipts.md`, `security/api-security.md` |
| `docs/modules/integrations/integrations.md` | Описать модуль интеграций. | Общие адаптеры внешних систем: Telegram, СБП, ML-сервис, файловое хранилище. | `integrations/*.md`, `architecture/module-dependencies.md` |

## Integrations

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/integrations/telegram.md` | Описать интеграцию с Telegram. | Telegram Mini App init data, идентификация пользователя, ограничения доверия, будущие уведомления через bot API. | `architecture/telegram-auth-flow.md`, `modules/auth.md`, `modules/notifications.md` |
| `docs/integrations/sbp.md` | Описать будущую интеграцию с СБП. | Условия перехода от ручного перевода к СБП, ожидаемые входные и выходные данные, статусы, подтверждение платежа, ограничения автоматизации. | `architecture/payment-flow.md`, `modules/payments.md`, `adr/ADR-0007-manual-payment-first.md` |
| `docs/integrations/ml-service.md` | Описать Python ML-сервис. | Контракт вызова, ответственность за OCR, формат ответа, ошибки, ретраи и таймауты. | `architecture/receipt-processing-flow.md`, `ml/receipt-recognition.md`, `adr/ADR-0009-python-ml-service.md` |
| `docs/integrations/receipt-json-contract.md` | Зафиксировать JSON чека. | Структура распознанного чека: продавец, дата, позиции, количество, цена, итог, confidence и ошибки. | `modules/receipts.md`, `integrations/ml-service.md`, `testing/test-data.md` |
| `docs/integrations/file-storage.md` | Описать хранение файлов. | Где хранятся изображения чеков, как backend хранит метаданные, ограничения размера и доступа. | `modules/files.md`, `deployment/configuration-management.md`, `security/api-security.md` |

## Security

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/security/authentication.md` | Описать аутентификацию. | Проверка Telegram init data, создание backend-сессии, связь с JWT. | `architecture/telegram-auth-flow.md`, `integrations/telegram.md` |
| `docs/security/authorization.md` | Описать авторизацию. | Доступ к группам, мероприятиям, чекам, долгам и платежам только участникам соответствующего контекста. | `business/business-rules.md`, `modules/groups.md`, `modules/events.md` |
| `docs/security/jwt.md` | Описать JWT. | Claims, срок действия, подпись, хранение на клиенте, access token и refresh token для MVP. | `adr/ADR-0006-jwt-authentication.md`, `security/authentication.md` |
| `docs/security/secrets-management.md` | Описать управление секретами. | Telegram bot token, JWT secret, параметры БД, секреты СБП и ML-сервиса по окружениям. | `deployment/configuration-management.md`, `deployment/environments.md` |
| `docs/security/api-security.md` | Описать безопасность API. | Валидация входных данных, rate limits как future option, защита файлов, запрет утечки персональных и платежных данных. | `architecture/error-handling.md`, `testing/integration-tests.md` |

## Testing

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/testing/testing-strategy.md` | Описать общую стратегию тестирования. | Баланс unit, integration и contract tests для быстрого MVP без избыточной тестовой инфраструктуры. | `business/mvp-scope.md`, `deployment/ci-cd.md` |
| `docs/testing/unit-tests.md` | Описать unit-тесты. | Тестирование бизнес-правил, расчетов долгов, нормализации чеков и мапперов без БД. | `modules/debts.md`, `modules/receipts.md`, `adr/ADR-0005-mapstruct.md` |
| `docs/testing/integration-tests.md` | Описать интеграционные тесты. | Проверка API, security, транзакций, БД, интеграционных адаптеров с заглушками внешних систем. | `testing/testcontainers.md`, `architecture/request-flow.md` |
| `docs/testing/testcontainers.md` | Описать Testcontainers. | PostgreSQL в тестах, применение миграций, изоляция тестовых данных, ограничения скорости CI. | `adr/ADR-0003-postgresql.md`, `adr/ADR-0004-liquibase.md` |
| `docs/testing/test-data.md` | Описать тестовые данные. | Наборы чеков, пользователей, групп, долгов, платежей и негативных кейсов для MVP. | `integrations/receipt-json-contract.md`, `business/business-rules.md` |

## Deployment

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/deployment/local-development.md` | Описать локальный запуск. | Java 21, Maven, PostgreSQL, env vars, запуск backend и зависимостей без сложной инфраструктуры. | `adr/ADR-0002-java21.md`, `deployment/docker.md` |
| `docs/deployment/docker.md` | Описать Docker-подход. | Контейнеризация backend, PostgreSQL для локальной разработки, будущая упаковка ML-сервиса. | `deployment/local-development.md`, `integrations/ml-service.md` |
| `docs/deployment/environments.md` | Описать окружения. | Local, test/stage, prod; отличия конфигурации, БД, секретов и интеграций. | `deployment/configuration-management.md`, `security/secrets-management.md` |
| `docs/deployment/configuration-management.md` | Описать управление конфигурацией. | Spring profiles, env vars, настройки БД, JWT, Telegram, СБП, ML-сервиса и файлового хранения. | `security/secrets-management.md`, `modules/app.md` |
| `docs/deployment/ci-cd.md` | Описать CI/CD. | Сборка Maven, тесты, миграции, Docker image, деплой MVP, минимальные quality gates. | `testing/testing-strategy.md`, `adr/ADR-0004-liquibase.md` |

## Roadmap

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/roadmap/mvp-roadmap.md` | Описать путь к MVP. | Порядок реализации по ценности: auth, groups, events, receipts, debts, payments, базовые уведомления. | `business/mvp-scope.md`, `architecture/system-overview.md` |
| `docs/roadmap/post-mvp-roadmap.md` | Описать развитие после MVP. | Функции после запуска, которые не должны блокировать первую версию. | `business/post-mvp-features.md`, `architecture/backend-architecture.md` |
| `docs/roadmap/scaling-roadmap.md` | Описать возможное масштабирование. | Условия перехода к Event Driven Architecture, Kafka и выделенным сервисам без внедрения на MVP. | `adr/ADR-0001-modular-monolith.md`, `architecture/module-dependencies.md` |

## ML

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/ml/receipt-recognition.md` | Описать ML/OCR-контур. | Что делает Python-сервис, какие данные возвращает backend, какие ошибки и confidence учитываются. | `integrations/ml-service.md`, `integrations/receipt-json-contract.md` |
| `docs/ml/ml-boundaries.md` | Зафиксировать границы ML и backend. | Backend не выполняет OCR, не обучает модели и не принимает ML-решения без валидации доменных правил. | `adr/ADR-0008-receipt-processing.md`, `adr/ADR-0009-python-ml-service.md` |
| `docs/ml/ml-future-evolution.md` | Описать развитие ML после MVP. | Улучшение качества распознавания, асинхронная обработка, очереди и возможная событийная интеграция. | `roadmap/scaling-roadmap.md`, `integrations/ml-service.md` |

## Onboarding

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/onboarding/getting-started.md` | Дать быстрый старт новому разработчику. | Что прочитать первым, как поднять проект, как пройти основной сценарий MVP. | `deployment/local-development.md`, `business/user-flows.md` |
| `docs/onboarding/project-structure.md` | Объяснить структуру проекта. | Модули Maven/Spring, структура пакетов, назначение `docs`, `app`, доменных модулей и интеграций. | `architecture/backend-architecture.md`, `modules/*.md` |
| `docs/onboarding/glossary.md` | Дать краткий словарь для входа. | Сокращенная версия терминов для быстрого старта; основной источник терминов остается в business glossary. | `business/glossary.md` |
| `docs/onboarding/development-workflow.md` | Описать рабочий процесс разработки. | Как добавлять изменения, тестировать, документировать решения и не нарушать границы модулей. | `ai/development-workflow.md`, `testing/testing-strategy.md`, `adr/ADR-0010-module-boundaries.md` |

## AI

| Path | Purpose | Brief Description | Depends On |
| --- | --- | --- | --- |
| `docs/ai/project-rules.md` | Зафиксировать правила проекта для AI-агентов. | MVP-first подход, запрет enterprise-усложнений, запрет микросервисов и Kafka на этапе MVP. | `business/mvp-scope.md`, `adr/ADR-0001-modular-monolith.md` |
| `docs/ai/development-workflow.md` | Описать workflow для AI-агентов. | Как анализировать задачу, какие документы читать, когда обновлять ADR, как проверять изменения. | `onboarding/development-workflow.md`, `testing/testing-strategy.md` |
| `docs/ai/coding-standards.md` | Зафиксировать стандарты кода. | Java 21, Spring Boot, MapStruct, тесты, именование, минимальные абстракции и документация решений. | `adr/ADR-0002-java21.md`, `adr/ADR-0005-mapstruct.md`, `database/naming-conventions.md` |
| `docs/ai/forbidden-patterns.md` | Описать запрещенные паттерны. | Микросервисы на MVP, Kafka на MVP, анемичные универсальные сервисы, shared everything, неявные зависимости модулей. | `adr/ADR-0001-modular-monolith.md`, `adr/ADR-0010-module-boundaries.md` |
| `docs/ai/architecture-rules.md` | Зафиксировать архитектурные правила. | Модульный монолит, слойность, транзакционные границы, интеграционные адаптеры, будущая событийность без преждевременного внедрения. | `architecture/backend-architecture.md`, `architecture/module-dependencies.md` |
| `docs/ai/module-rules.md` | Зафиксировать правила модулей. | Правила владения доменными объектами, публичных контрактов, зависимостей, событий и БД-объектов. | `architecture/module-dependencies.md`, `modules/*.md` |
| `docs/ai/context-guide.md` | Дать стартовую карту контекста для AI-агентов. | Mental model проекта, canonical module map, paths чтения документации по типам задач, глобальные инварианты и типовые неверные предположения. | `ai/project-rules.md`, `ai/development-workflow.md`, `architecture/system-overview.md`, `business/mvp-scope.md` |

## Review Checkpoints

| Этап | Содержание | Статус |
| --- | --- | --- |
| 1 | Каталог документации + Documentation Status | complete |
| 2a | Согласование 27 существующих документов (группы, платежи, роли) | complete |
| 2b | `modules/` — 12 документов | complete |
| 2c | `integrations/` — 5 документов | complete |
| 2d | `security/` — 5 документов | complete |
| 2e | `database/` — 7 документов | complete |
| 2f | `testing/`, `deployment/`, `roadmap/`, `ml/`, `onboarding/`, `ai/` — 23 документа | complete |

Документы создаются по одному разделу с ревью после каждого этапа.
