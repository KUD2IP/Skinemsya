# Onboarding: Development Workflow

## Purpose

Документ описывает рабочий процесс разработки: как добавлять изменения, тестировать, документировать.

## Context

Единый workflow для разработчиков и AI-агентов обеспечивает согласованность и качество.

## Responsibilities

- Описать шаги разработки новой функции.
- Зафиксировать правила code review.
- Определить, когда обновлять документацию и ADR.

## Non Responsibilities

- Документ не описывает git workflow (branching strategy).
- Документ не заменяет `ai/development-workflow.md`.

## Design Decisions

Workflow добавления функции:

1. **Прочитать docs** — найти релевантные модули, business rules, ADR.
2. **Проверить scope** — функция в MVP? Если нет — stop, обсудить.
3. **Спроектировать** — обновить docs если меняется контракт или граница модуля.
4. **Реализовать** — в рамках одного модуля, через public contracts.
5. **Тестировать** — unit для бизнес-логики, integration для API.
6. **Code review** — проверить module boundaries, no forbidden patterns.
7. **Обновить docs** — если изменились контракты, таблицы, flows.

Когда создавать ADR:
- Новая технология или инструмент.
- Изменение архитектурного решения.
- Добавление зависимости между модулями.
- Отклонение от MVP scope.

Когда обновлять docs:
- Новый модуль или public contract.
- Изменение бизнес-правила.
- Новая таблица БД.
- Изменение интеграционного контракта.

Правила:
- Один PR — одна функция или bugfix.
- Тесты обязательны для бизнес-логики.
- Не нарушать module boundaries (ADR-0010).
- Не добавлять enterprise patterns на MVP.

## Constraints

- Документация обновляется в том же PR что и код.
- ADR создается до реализации архитектурного изменения.
- AI-агенты следуют `ai/development-workflow.md`.

## Future Evolution

- Automated doc lint (broken links).
- PR template с checklist.
- Architecture review board.

## Related Documents

- `docs/ai/development-workflow.md`
- `docs/testing/testing-strategy.md`
- `docs/adr/ADR-0010-module-boundaries.md`
- `docs/onboarding/getting-started.md`
