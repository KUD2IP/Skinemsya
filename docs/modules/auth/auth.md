# Module: auth

## Purpose

Модуль `auth` отвечает за аутентификацию через Telegram Mini App init data и выпуск JWT-токенов.

## Responsibilities

- Валидация Telegram init data (HMAC-подпись).
- Создание или обновление пользователя через `users` при первом входе.
- Выпуск access token и refresh token.
- Обновление access token по refresh token.
- Интеграция с Spring Security.

## Domain Objects

- `TelegramInitData` — распарсенные данные init data (user id, username, auth date).
- `AuthSession` — пара access + refresh token с метаданными.
- `TokenPair` — access token и refresh token для ответа клиенту.

## Dependencies

- `users` — upsert пользователя после проверки Telegram identity.
- `integrations` — адаптер проверки Telegram init data.
- `common` — ошибки аутентификации.

## Events

- `UserAuthenticated` (internal, MVP) — пользователь успешно аутентифицирован. Потребитель: `users`.

## Database Objects

- `refresh_tokens` — хранение refresh token, user id, expiry, revoked flag.

## Public Contracts

- `AuthService.authenticate(initData)` → `TokenPair`
- `AuthService.refresh(refreshToken)` → `TokenPair`
- `AuthService.revoke(refreshToken)` → void
- REST: `POST /api/v1/auth/telegram`, `POST /api/v1/auth/refresh`

## Future Extensions

- Token rotation при refresh.
- Device/session management.
- Rate limiting на auth endpoints.
- Webhook-аутентификация для bot API.
