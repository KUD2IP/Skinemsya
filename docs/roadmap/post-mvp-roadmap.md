# Roadmap: Post-MVP

## Purpose

Документ описывает развитие продукта после MVP: функции, которые не блокируют первую версию.

## Context

После запуска MVP и подтверждения продуктовой ценности проект может расширяться. Эти функции сознательно исключены из MVP.

## Responsibilities

- Перечислить post-MVP функции по приоритету.
- Описать зависимости между функциями.
- Зафиксировать, что не должно попасть в MVP случайно.

## Non Responsibilities

- Документ не содержит даты.
- Документ не заменяет `business/post-mvp-features.md`.

## Design Decisions

Приоритет post-MVP:

**P1 — High value, moderate effort:**
1. СБП-интеграция (автоматические платежи).
2. Push-уведомления через Telegram bot (rich notifications).
3. Приглашения в standalone-группы по ссылке.

**P2 — Medium value:**
4. Частичные платежи.
5. Расширенная история групп и мероприятий.
6. Роли: admin в группе.
7. Спорные платежи с комментариями.
8. Улучшение ML-качества (confidence thresholds, manual correction UI).

**P3 — Lower priority:**
9. Продвинутая аналитика расходов.
10. Сложные способы деления (проценты, веса).
11. Несколько плательщиков.
12. Оплата разных мест разными людьми.

## Constraints

- Post-MVP функции не реализуются до завершения MVP.
- Каждая post-MVP функция требует обновления документации.
- SBP — первая крупная post-MVP интеграция.

## Future Evolution

- См. `scaling-roadmap.md` для инфраструктурного развития.

## Related Documents

- `docs/business/post-mvp-features.md`
- `docs/architecture/backend-architecture.md`
- `docs/integrations/sbp.md`
- `docs/roadmap/scaling-roadmap.md`
