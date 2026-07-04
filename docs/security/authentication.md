# Security: Authentication

## Purpose

Документ описывает аутентификацию пользователей через Telegram Mini App и связь с JWT.

## Context

Единственный способ входа в MVP — Telegram Mini App. Backend проверяет init data и выпускает JWT для последующих запросов.

## Responsibilities

- Описать поток аутентификации.
- Зафиксировать проверку Telegram init data.
- Определить создание backend-сессии через JWT.
- Описать обновление токенов.

## Non Responsibilities

- Документ не описывает авторизацию доступа к ресурсам (см. `authorization.md`).
- Документ не реализует endpoints.

## Design Decisions

Поток:
1. Клиент отправляет `initData` в `POST /api/v1/auth/telegram`.
2. Backend валидирует HMAC-подпись init data.
3. Backend upsert пользователя в `users`.
4. Backend выпускает access token (15 min) и refresh token (7 days).
5. Клиент передает `Authorization: Bearer <access_token>` в запросах.
6. При истечении access token — `POST /api/v1/auth/refresh`.

Публичные endpoints (без JWT): `/api/v1/auth/telegram`, `/api/v1/auth/refresh`, `/actuator/health`, OpenAPI docs.

## Constraints

- Init data проверяется на каждом auth-запросе.
- Refresh token хранится в БД с возможностью revoke.
- Нет паролей, нет email-аутентификации на MVP.

## Future Evolution

- Multi-device sessions.
- Token rotation.
- OAuth для third-party integrations.

## Related Documents

- `docs/architecture/telegram-auth-flow.md`
- `docs/integrations/telegram.md`
- `docs/security/jwt.md`
- `docs/modules/auth.md`
