# Integration: Telegram

## Purpose

Документ описывает интеграцию backend с Telegram: Mini App init data, bot API и ограничения доверия.

## Context

`skinemsya` работает как Telegram Mini App. Аутентификация основана на проверке init data, переданных клиентом. Bot используется для приветственного сообщения в чате и будущих уведомлений.

## Responsibilities

- Валидация Telegram init data (HMAC-SHA256 с bot token).
- Извлечение Telegram user id, username, auth_date.
- Отправка сообщений через bot API (уведомления).
- Обработка ошибок и таймаутов Telegram API.

## Non Responsibilities

- Backend не управляет Telegram-клиентом.
- Backend не хранит bot token в коде.
- Backend не использует Telegram username как стабильный идентификатор.

## Design Decisions

Init data validation:
1. Клиент передает `initData` строку в `POST /api/v1/auth/telegram`.
2. `integrations.TelegramAdapter` проверяет HMAC-подпись с использованием bot token.
3. Проверяется `auth_date` — не старше 24 часов (настраиваемо).
4. Извлекается `user.id` (Telegram user id) для upsert в `users`.

Bot API (MVP):
- Отправка приветственного сообщения при добавлении bot в чат — post-MVP automation; на MVP bot настраивается вручную.
- Уведомления участникам через `sendMessage` в личный чат или групповой чат.

## Constraints

- Bot token хранится в env/secrets, не в репозитории.
- Init data проверяется на каждом auth-запросе.
- Telegram API имеет rate limits; retry с backoff.
- `chat_id` из init data используется только для `CHAT_LINKED`-групп.

## Future Evolution

- Webhook для bot events (новый участник чата).
- Inline keyboard в уведомлениях.
- Deep links для приглашений в standalone-группы.

## Related Documents

- `docs/architecture/telegram-auth-flow.md`
- `docs/modules/auth.md`
- `docs/modules/notifications.md`
- `docs/security/secrets-management.md`
