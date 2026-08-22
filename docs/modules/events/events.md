# Module: events

## Purpose

Модуль `events` управляет мероприятиями внутри групп: создание, плательщик, участники, статус жизненного цикла.

## Responsibilities

- Создание и редактирование мероприятия внутри группы.
- Назначение и смена плательщика до финального расчета.
- Управление участниками мероприятия, включая исключение участника владельцем группы.
- Контроль статуса мероприятия: черновик, распределение, расчет, завершено.
- Удаление мероприятия в любом статусе (создатель сбора, плательщик или владелец группы).

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
- REST: `POST /api/v1/groups/{groupId}/events`, `GET /api/v1/events/{id}`, `GET /api/v1/events/{id}/invite-link`, `DELETE /api/v1/events/{id}`, `DELETE /api/v1/events/{id}/participants/{userId}`

## Authorization Rules

| Operation | Required Access |
| --- | --- |
| Delete event | Group owner, event creator, or payer. Allowed in any status. Soft-deletes the event. |
| Remove participant | Group owner. Cannot remove payer or creator. Blocked if event is completed or debts are locked. |
| Leave event | Participant who is not payer or creator. Same lock as remove. |
| Invite link | Any group member. `startapp=event_{id}` joins the group (including standalone) and tries to join the event. |

## Future Extensions

- Несколько плательщиков.
- Шаблоны мероприятий.
- Повторное открытие для исправления.
- Архив мероприятий.
