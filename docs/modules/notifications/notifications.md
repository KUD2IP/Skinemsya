# Module: notifications

## Purpose

Модуль `notifications` отправляет уведомления пользователям о ключевых событиях: запуск сбора, выбор позиций, долги, оплаты, изменения сбора (`Event`).

## Responsibilities

- Отправка уведомлений через Telegram bot API.
- Реакция на доменные события других модулей.
- Учет настроек уведомлений из профиля пользователя.
- Базовые шаблоны сообщений для MVP.
- Сообщения в групповой чат с inline-кнопками и deep links в Mini App.

## Domain Objects

- `Notification` — запись уведомления: `id`, `userId`, `type`, `payload`, `status`, `sentAt`.
- `NotificationType` — enum: `EVENT_CREATED`, `DISTRIBUTION_STARTED`, `DEBTS_CALCULATED`, `PAYMENT_PENDING`, `PAYMENT_DISPUTED`, `DEBT_CLOSED`, `EVENT_COMPLETED`, `REMINDER`.
- `NotificationStatus` — enum: `PENDING`, `SENT`, `FAILED`.
- `NotificationChannel` — enum: `GROUP_CHAT`, `PRIVATE_DM`.

## Dependencies

- `users` — получатель, настройки уведомлений.
- `groups`, `events`, `debts`, `payments` — контекст уведомления.
- `integrations` — Telegram bot API adapter.
- `common` — ошибки.

## Events

Потребляет (internal):
- `EventSentToDistribution`, `DebtsCalculated`, `DebtorConfirmed`, `PaymentDisputed`, `DebtClosed`, `EventCompleted`, `MemberJoined`.

Публикует:
- `NotificationSent`, `NotificationFailed`.

## Database Objects

- `notifications` — id, user_id (FK), type, payload (JSON), status, sent_at, created_at.
- Index: (user_id, created_at).

## Public Contracts

- `NotificationService.send(userId, type, payload)` → `Notification`
- `NotificationService.sendToEventParticipants(eventId, type, payload)` → void
- `NotificationService.sendToGroupChat(telegramChatId, type, payload)` → void
- Внутренний listener на доменные события (не REST на MVP).

## Message Templates (MVP)

Шаблоны согласованы с `docs/product/ux-checklist.md`. Плейсхолдеры: `{name}`, `{title}`, `{total}`, `{amount}`, `{payer}`, `{username}`.

### Групповой чат (`GROUP_CHAT`)

| Триггер | `NotificationType` | Текст | Inline-кнопка | Deep link |
| --- | --- | --- | --- | --- |
| Сбор запущен | `DISTRIBUTION_STARTED` | `{name} запустил сбор «{title}» на {total} ₽. Выберите свои блюда` | **Скинуть** | `startapp=event_{eventId}` |
| Участник отправил | `PAYMENT_PENDING` | `{name} скинула {amount} ₽` | — | — |
| Все выбрали блюда | `DEBTS_CALCULATED` | `Все выбрали блюда. {payer}, проверь переводы` | **Открыть** | `startapp=event_{eventId}` |
| Сбор закрыт | `EVENT_COMPLETED` | `Сбор «{title}» закрыт. Все скинули!` | — | — |
| Напоминание выбрать | `REMINDER` | `Ждём выбор блюд от @{username}` | **Скинуть** | `startapp=event_{eventId}` |

### Личные сообщения (`PRIVATE_DM`)

| Триггер | `NotificationType` | Текст | Inline-кнопка | Deep link |
| --- | --- | --- | --- | --- |
| Новый перевод на проверку | `PAYMENT_PENDING` | `Проверь перевод от {name} ({amount} ₽)` | **Открыть** | `startapp=event_{eventId}` |
| Перевод оспорен | `PAYMENT_DISPUTED` | `{payer} не видит твой перевод {amount} ₽ — проверь и отправь снова` | **Скинуть** | `startapp=event_{eventId}` |
| Напоминание не скинуть | `REMINDER` | `Не забудь скинуть {amount} ₽ за сбор «{title}»` | **Скинуть** | `startapp=event_{eventId}` |

### Deep links

| Формат | Назначение | Статус |
| --- | --- | --- |
| `startapp=chat_{chatId}` | Вход в группу из чата | Реализовано |
| `startapp=event_{eventId}` | Прямой вход на экран выбора блюд / статусов сбора | Требуется (Phase 6) |

Backend парсит `start_param` в `TelegramInitDataValidator` / `TelegramStartParam` и направляет Mini App на соответствующий экран.

## Требование активации бота в личном чате

Telegram доставляет **личные сообщения** (`PRIVATE_DM`) только пользователям, которые открыли чат с ботом и нажали **Start** (`/start`).

| Канал | Тип | Нужен /start в личке? |
| --- | --- | --- |
| Групповой чат | `DISTRIBUTION_STARTED`, `DEBTS_CALCULATED`, `PAYMENT_PENDING` (сообщение в группе), `EVENT_COMPLETED`, `REMINDER` | Нет |
| Личные сообщения | `PAYMENT_PENDING` («Проверь перевод от …») | **Да** |
| Личные сообщения | `PAYMENT_DISPUTED` («… не видит твой перевод …») | **Да** |

При старте приложения бот выставляет описание через `setMyDescription` / `setMyShortDescription`. В групповом welcome-сообщении и в ответе на `/start` в личке пользователю напоминают про активацию.

## Future Extensions

- Push через Telegram inline buttons.
- Email/SMS как альтернативные каналы.
- Очередь уведомлений с retry.
- Персонализированные шаблоны.
- Отложенное напоминание плательщику через 2 ч после `DebtorConfirmed`.

## Related Documents

- `docs/product/ux-checklist.md`
- `docs/integrations/telegram.md`
- `docs/architecture/payment-flow.md`
