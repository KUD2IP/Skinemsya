# Module: events

## Purpose

Модуль `events` управляет мероприятиями внутри групп: создание, плательщик, участники, статус жизненного цикла.

## Responsibilities

- Создание и редактирование мероприятия внутри группы.
- Назначение и смена плательщика до финального расчета.
- Управление участниками мероприятия.
- Контроль статуса мероприятия: черновик, распределение, расчет, завершено.
- Удаление мероприятия (владелец мероприятия или владелец группы).

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
- REST: `POST /api/v1/groups/{groupId}/events`, `GET /api/v1/events/{id}`

## Future Extensions

- Несколько плательщиков.
- Шаблоны мероприятий.
- Повторное открытие для исправления.
- Архив мероприятий.
