# Module: notifications

## Purpose

Модуль `notifications` пишет в групповой чат Telegram о ключевых событиях сбора (`Event`): запуск, напоминания, «скинул», закрытие.

## Responsibilities

- Отправка сообщений в групповой чат через Telegram bot API.
- Реакция на доменные события других модулей.
- Одно сообщение на событие: без личных сообщений бота и без дублей.
- Сообщения с inline-кнопкой **Скинуть** и deep link в Mini App.

## Domain Objects

- `Notification` — запись уведомления: `id`, `userId`, `type`, `payload`, `status`, `sentAt`.
- `NotificationType` — enum: `EVENT_CREATED`, `DISTRIBUTION_STARTED`, `DEBTS_CALCULATED`, `PAYMENT_PENDING`, `PAYMENT_DISPUTED`, `DEBT_CLOSED`, `EVENT_COMPLETED`, `REMINDER`.
- `NotificationStatus` — enum: `PENDING`, `SENT`, `FAILED`.

Личные сообщения бота (`PRIVATE_DM`) не используются. Standalone-группа без `telegramChatId` — сообщение не отправляется.

## Dependencies

- `users` — имя и `@username` для упоминаний.
- `groups`, `events`, `debts` — контекст сбора, участники, неоплаченные долги.
- `integrations` — Telegram bot API adapter.
- `common` — ошибки и доменные события.

## Events

Потребляет (internal):
- `EventSentToDistribution`, `DebtorConfirmed`, `EventCompleted`.

`DebtsCalculated` и `PaymentDisputed` в чат не пишутся. Отложенный 2-часовой джоб плательщику не публикует события и не создаёт новые `payer_reminder_jobs`.

Публикует:
- `NotificationSent`, `NotificationFailed`.

## Database Objects

- `notifications` — id, user_id (FK), type, payload (JSON), status, sent_at, created_at.
- Index: (user_id, created_at).

## Public Contracts

- `NotificationService.sendToGroupChat(telegramChatId, type, payload)` → void (текст без кнопки)
- `NotificationService.sendToGroupChat(telegramChatId, type, payload, eventId)` → void (с кнопкой **Скинуть**)
- `NotificationService.remindIncompleteSelections(eventId, requesterId)` → void
- REST: `POST /api/v1/events/{eventId}/remind`
- Внутренний listener на доменные события.

## Message Templates (MVP)

Шаблоны согласованы с `docs/product/ux-checklist.md`. Плейсхолдеры: `{name}`, `{title}`, `{total}`, `{amount}`, `{mentions}`.

Упоминания: `@username`, если есть; иначе имя. Несколько человек — через запятую, перед последним «и»: `@ivan, @anna и Петр`.

### Групповой чат

| Триггер | `NotificationType` | Текст | Inline-кнопка | Deep link |
| --- | --- | --- | --- | --- |
| Сбор запущен | `DISTRIBUTION_STARTED` | `{name} запустил сбор «{title}» на {total} ₽. Выберите свои позиции` | **Скинуть** | `startapp=event_{eventId}` |
| Участник скинул | `PAYMENT_PENDING` | `{name} скинул` | — | — |
| Сбор закрыт | `EVENT_COMPLETED` | `Сбор «{title}» закрыт. Все скинули!` | **Скинуть** | `startapp=event_{eventId}` |
| Напоминание выбрать (`DISTRIBUTION`) | `REMINDER` | `Ждём выбор позиций от {mentions}` | **Скинуть** | `startapp=event_{eventId}` |
| Напоминание перевести (`CALCULATED`) | `REMINDER` | `Ждём перевод от {mentions}` | **Скинуть** | `startapp=event_{eventId}` |

Напоминание на `DISTRIBUTION` — все участники без `selectionCompletedAt`, кроме плательщика. На `CALCULATED` — должники со статусом `UNPAID`. Одно сообщение на нажатие «Напомнить».

### Deep links

| Формат | Назначение |
| --- | --- |
| `startapp=chat_{chatId}` | Вход в группу из чата |
| `startapp=event_{eventId}` | Вход на сбор |
| `startapp=group_{groupId}` | Приглашение в группу |

Backend парсит `start_param` в `TelegramInitDataValidator` / `TelegramStartParam` и направляет Mini App на соответствующий экран.

Приглашение шарится через `https://t.me/share/url` с текстом из `InviteLinkResponse.shareText`.

## Future Extensions

- Очередь уведомлений с retry.
- Персонализированные шаблоны.
- Push через Telegram inline buttons.

## Related Documents

- `docs/product/ux-checklist.md`
- `docs/integrations/telegram.md`
- `docs/architecture/payment-flow.md`
