# Roadmap: Scaling

## Purpose

Документ описывает возможное масштабирование: переход к Event Driven Architecture, Kafka и выделенным сервисам.

## Context

MVP — модульный монолит. Масштабирование рассматривается только при доказанной необходимости: нагрузка, организационные причины или независимый жизненный цикл модулей.

## Responsibilities

- Описать условия перехода к распределенной архитектуре.
- Зафиксировать порядок выделения сервисов.
- Определить роль Kafka.

## Non Responsibilities

- Документ не внедряет Kafka на MVP.
- Документ не проектирует микросервисы.

## Design Decisions

Условия перехода:
1. MVP стабилен в production > 3 месяцев.
2. Конкретный модуль создает bottleneck (CPU, memory, deploy frequency).
3. Команда выросла и нужна независимая разработка модулей.
4. Доменные события стабилизированы и документированы.

Порядок выделения сервисов (при необходимости):
1. `notifications` — независимый side effect, легко выделить.
2. `receipts` + ML — отдельный жизненный цикл, высокая нагрузка на OCR.
3. `payments` — compliance и надежность.
4. Остальные модули — только при явной необходимости.

Kafka (когда):
- Замена internal events на Kafka topics.
- Async receipt processing.
- Payment events для audit и notifications.
- Outbox pattern для reliable event publishing.

Event Driven Architecture:
- Начать с internal domain events (in-process).
- Документировать event catalog.
- При выделении сервиса — events становятся Kafka messages.
- Schema registry для event schemas.

## Constraints

- Kafka не на MVP.
- Микросервисы не на MVP.
- Каждое выделение сервиса — отдельный ADR.
- Модульные границы MVP проектируются с учетом future extraction.

## Future Evolution

- Service mesh (Istio) — только при > 5 сервисов.
- CQRS для read-heavy modules.
- Multi-region deployment.

## Related Documents

- `docs/adr/ADR-0001-modular-monolith.md`
- `docs/architecture/module-dependencies.md`
- `docs/architecture/backend-architecture.md`
- `docs/ml/ml-future-evolution.md`
