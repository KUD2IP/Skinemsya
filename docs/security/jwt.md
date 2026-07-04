# Security: JWT

## Purpose

Документ описывает JWT-токены: claims, сроки жизни, подпись и хранение на клиенте.

## Context

После Telegram-аутентификации backend выпускает JWT для stateless-авторизации запросов. ADR-0006 фиксирует этот выбор.

## Responsibilities

- Определить структуру access token и refresh token.
- Зафиксировать claims, сроки жизни, алгоритм подписи.
- Описать хранение на клиенте.
- Определить процесс refresh.

## Non Responsibilities

- Документ не описывает Telegram init data validation.
- Документ не реализует JwtService.

## Design Decisions

Access token:
- Алгоритм: HS256.
- Срок жизни: 15 минут.
- Claims: `sub` (backend user id), `iat`, `exp`.
- Минимальные claims — без ролей и permissions на MVP.

Refresh token:
- Opaque token (UUID), хранится в БД (`refresh_tokens`).
- Срок жизни: 7 дней.
- При refresh — выдается новая пара access + refresh.
- Старый refresh token revoke.

Подпись:
- Secret из env var `JWT_SECRET`.
- Минимум 256 бит энтропии.

Хранение на клиенте:
- Access token — в памяти Mini App (не localStorage для безопасности).
- Refresh token — secure storage Telegram Mini App или httpOnly cookie (решение frontend).

## Constraints

- JWT secret не в репозитории.
- Access token не содержит Telegram user id.
- Refresh token одноразовый при rotation.

## Future Evolution

- RS256 с key pair.
- Claims для ролей.
- Short-lived access + long-lived refresh с rotation.

## Related Documents

- `docs/adr/ADR-0006-jwt-authentication.md`
- `docs/security/authentication.md`
- `docs/security/secrets-management.md`
- `docs/modules/auth.md`
