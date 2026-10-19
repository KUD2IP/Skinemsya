# Module: events

## Purpose

Модуль `events` управляет мероприятиями внутри групп: создание, плательщик, участники, статус жизненного цикла.

## Responsibilities

- Создание и редактирование мероприятия внутри группы.
- Назначение и смена плательщика до финального расчета.
- Управление участниками мероприятия, включая исключение участника владельцем группы.
- Контроль статуса мероприятия: черновик, распределение, расчет, завершено.
- Удаление мероприятия в любом статусе (создатель сбора, плательщик или владелец группы).
- Возврат запущенного сбора в черновик плательщиком или создателем, пока нет подтверждённого перевода.

## Domain Objects

- `Event` — мероприятие с `id`, `groupId`, `name`, `description`, `payerId`, `status`.
- `EventParticipant` — участник мероприятия (ссылка на user).
- `EventStatus` — enum: `DRAFT`, `DISTRIBUTION`, `CALCULATED`, `COMPLETED`.

## Dependencies

- `groups` — проверка членства, доступ к группе.
- `users` — плательщик и участники.
- `common` — ошибки, Money.

## Events

- `EventCreated` — мероприятие создано.
- `EventUpdated` — изменены название, описание или плательщик.
- `EventSentToDistribution` — позиции отправлены на распределение.
- `EventDeleted` — мероприятие удалено.

## Database Objects

- `events` — id, group_id (FK), name, description, payer_id (FK users), status, created_at, updated_at, deleted_at.
- `event_participants` — id, event_id (FK), user_id (FK). Unique (event_id, user_id).

## Public Contracts

- `EventService.create(groupId, name, payerId, creatorId)` → `Event`
- `EventService.update(eventId, updateData)` → `Event`
- `EventService.changePayer(eventId, newPayerId)` → `Event`
- `EventService.sendToDistribution(eventId)` → `Event`
- `EventService.findByGroup(groupId)` → `List<Event>`
- `EventService.delete(eventId, requesterId)` → void
- `EventService.removeParticipant(eventId, requesterId, targetUserId)` → `Event`
- `EventService.revertToDraft(eventId, requesterId)` → `Event`
- REST: `POST /api/v1/groups/{groupId}/events`, `GET /api/v1/events/{id}`, `GET /api/v1/events/{id}/invite-link`, `POST /api/v1/events/{id}/revert-to-draft`, `DELETE /api/v1/events/{id}`, `DELETE /api/v1/events/{id}/participants/{userId}`

## Authorization Rules

| Operation | Required Access |
| --- | --- |
| Delete event | Group owner, event creator, or payer. Allowed in any status. Soft-deletes the event. |
| Remove participant | Group owner. Cannot remove payer or creator. Blocked if event is completed or debts are locked. |
| Leave event | Participant who is not payer or creator. Blocked when the event is completed, or when this participant's own debt is `PENDING_CONFIRMATION`. Another participant's marked or paid transfer does not block. Leaving drops this participant's selections and unpaid debt. |
| Revert to draft | Payer or creator. From `DISTRIBUTION` or `CALCULATED` only, and only when no debt is `PENDING_CONFIRMATION` or `PAID`. Clears selections and unpaid debts; positions, receipts, shared marks and participants stay. Already `DRAFT` is unchanged. `COMPLETED` is rejected. |
| Invite link | Any group member. `startapp=event_{id}` joins the group (including standalone) and tries to join the event. |

## Future Extensions

- Несколько плательщиков.
- Шаблоны мероприятий.
- Архив мероприятий.
