# AI: Project Rules

## Purpose

Правила проекта для AI-агентов: MVP-first подход, запреты, приоритеты.

## Context

AI-агенты участвуют в разработке. Им нужны явные правила, чтобы не усложнять MVP и следовать архитектуре.

## Responsibilities

- Зафиксировать MVP-first принцип.
- Определить запреты.
- Указать источник истины.

## Non Responsibilities

- Документ не описывает coding style (см. `coding-standards.md`).

## Design Decisions

Главные правила:

1. **Документация — источник истины.** Читать docs перед написанием кода.
2. **MVP-first.** Не добавлять функции вне `business/mvp-scope.md`.
3. **Модульный монолит.** Не микросервисы, не Kafka.
4. **Простота.** Минимальный diff, без over-engineering.
5. **Module boundaries.** Не нарушать зависимости из `architecture/module-dependencies.md`.
6. **Тесты.** Unit-тесты для бизнес-логики обязательны.

Запреты на MVP:
- Микросервисы.
- Kafka, RabbitMQ, любые message brokers.
- Event sourcing, CQRS.
- Полноценная СБП-интеграция.
- Сложная ролевая модель.
- Enterprise patterns (Saga, Circuit Breaker library, Service Mesh).
- OCR в Java backend.

Приоритеты:
1. Скорость разработки.
2. Простота поддержки.
3. Быстрый деплой.
4. Возможность масштабирования (через module boundaries, не через premature distribution).

## AI Context Model

AI-агент должен воспринимать репозиторий как проект, где документация опережает реализацию. Текущий Java-код может быть скелетом, generated archetype или устаревшей заготовкой. Если код противоречит документации, агент не должен автоматически считать код более авторитетным. Нужно определить тип расхождения:

| Расхождение | Как действовать |
| --- | --- |
| Код — пустая заготовка, docs описывают целевую архитектуру | Следовать документации |
| Код уже реализует бизнес-сценарий, docs молчат | Предложить обновить docs перед расширением |
| Код и docs описывают разные бизнес-правила | Остановиться и запросить подтверждение |
| Maven-структура отличается от docs | Считать docs целевой структурой, если каталог явно это фиксирует |
| Пользователь дал новое правило в чате | Новое правило выше старых docs; обновить docs перед кодом |

Контекст проекта для агента:

- `skinemsya` — backend Telegram Mini App для разделения счетов.
- Основной продуктовый результат — пользователь понимает, кто кому должен, и закрывает долг ручным переводом.
- MVP должен быть реализован как модульный монолит.
- Python ML-сервис распознает чек, Java backend валидирует JSON и строит доменную модель.
- Ручное создание группы входит в MVP наравне с созданием из Telegram-чата.
- СБП, Kafka, микросервисы и сложные роли не являются MVP.

## Source Of Truth Order

При конфликте источников агент использует следующий порядок:

1. Последнее явное указание пользователя в текущем чате.
2. ADR, если вопрос архитектурный или технологический.
3. `docs/business/*`, если вопрос о бизнес-правиле.
4. `docs/architecture/*`, если вопрос о потоках и границах системы.
5. `docs/modules/{module}.md`, если вопрос о владении логикой.
6. `docs/database/*`, если вопрос о таблицах и связях.
7. Текущий код, если документация не описывает деталь или реализация уже существует.

Если агент не может однозначно применить этот порядок, он должен задать уточняющий вопрос вместо того, чтобы выбирать случайное решение.

## Required Reading By Task Type

Перед изменениями агент обязан прочитать минимальный набор документов:

| Тип задачи | Обязательные документы |
| --- | --- |
| Любая новая функциональность | `business/mvp-scope.md`, `architecture/backend-architecture.md`, релевантный `modules/*.md` |
| Авторизация или доступ | `security/authentication.md`, `security/authorization.md`, `modules/groups.md` |
| Расчет долгов | `business/business-rules.md`, `modules/debts.md`, `modules/receipts.md`, `architecture/payment-flow.md` |
| Платежи | `architecture/payment-flow.md`, `modules/payments.md`, `modules/debts.md`, `adr/ADR-0007-manual-payment-first.md` |
| Чеки и ML | `architecture/receipt-processing-flow.md`, `modules/receipts.md`, `integrations/ml-service.md`, `integrations/receipt-json-contract.md` |
| База данных | `database/table-catalog.md`, `database/naming-conventions.md`, `database/entity-relations.md` |
| Новая интеграция | `modules/integrations/integrations.md`, релевантный `integrations/*.md`, `security/secrets-management.md` |
| Тесты | `testing/testing-strategy.md`, `testing/test-data.md`, релевантный module doc |

## Decision Discipline

AI-агент не должен добавлять абстракцию только потому, что она может понадобиться в будущем. Абстракция допустима только если выполняется хотя бы одно условие:

- она уже описана в документации;
- она устраняет реальное дублирование в текущей задаче;
- она является обязательной границей между модулями;
- без нее невозможно написать тестируемую бизнес-логику.

Если решение можно реализовать простым application service без дополнительных паттернов, MVP предпочитает этот вариант.

## Documentation Update Rules

Документация обновляется до или вместе с кодом, если меняется:

- бизнес-правило;
- public contract модуля;
- таблица, связь или индекс;
- внешний контракт интеграции;
- security rule;
- статус или переход доменной сущности;
- модульная зависимость.

Не нужно обновлять документацию при чистой внутренней перестановке кода, которая не меняет поведения, контрактов и архитектурных правил.

## Constraints

- AI-агент не создает ADR без явного запроса или архитектурного изменения.
- AI-агент не меняет module dependencies без обновления docs.
- AI-агент не коммитит без запроса пользователя.

## Future Evolution

- Automated rule enforcement в CI.
- AI-specific lint rules.

## Related Documents

- `docs/business/mvp-scope.md`
- `docs/adr/ADR-0001-modular-monolith.md`
- `docs/ai/forbidden-patterns.md`
- `docs/ai/architecture-rules.md`
