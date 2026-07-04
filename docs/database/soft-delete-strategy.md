# Database: Soft Delete Strategy

## Purpose

Документ описывает подход к удалению данных: где soft delete, где hard delete, влияние на долги и платежи.

## Context

Удаление групп и мероприятий должно быть безопасным: нельзя удалить данные с активными расчетами. Soft delete сохраняет целостность ссылок.

## Responsibilities

- Определить таблицы с soft delete.
- Зафиксировать правила удаления.
- Описать влияние на связанные данные.

## Non Responsibilities

- Документ не реализует delete logic.
- Документ не описывает GDPR right to erasure.

## Design Decisions

Soft delete (колонка `deleted_at`):
- `groups` — владелец может удалить, если нет активных мероприятий с незакрытыми долгами.
- `events` — владелец/создатель может удалить, если нет рассчитанных долгов или все долги закрыты.

Hard delete запрещен для:
- `debts` — только изменение статуса.
- `payments` — immutable после `payer_confirmed`.
- `users` — не удаляются на MVP (деактивация post-MVP).

Запросы:
- Все SELECT по groups/events фильтруют `WHERE deleted_at IS NULL`.
- Partial index на `deleted_at IS NULL`.

Каскад:
- Удаление группы не каскадирует на events/debts — явная проверка в application service.
- Удаление event — soft delete, debts остаются для истории.

## Constraints

- Нельзя soft delete группу с активными незакрытыми долгами.
- Нельзя soft delete мероприятие с долгами в статусе `ожидает подтверждения`.
- `deleted_at` не очищается (no undelete на MVP).

## Future Evolution

- Undelete для владельца.
- Архив вместо delete.
- Data retention policy.
- Anonymization вместо delete для users.

## Related Documents

- `docs/database/entity-relations.md`
- `docs/business/business-rules.md`
- `docs/modules/groups.md`
- `docs/modules/events.md`
