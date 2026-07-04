# ADR-0006: JWT Authentication

## Status

Accepted

## Purpose

Зафиксировать JWT-аутентификацию с access token и refresh token после проверки Telegram Mini App init data.

## Context

Пользователь открывает `skinemsya` внутри Telegram Mini App. Backend должен проверить Telegram init data и затем выдать собственную backend-сессию. Каждый API-запрос не должен заново проходить Telegram-проверку, а короткий access token не должен заставлять пользователя часто перелогиниваться.

## Responsibilities

- Зафиксировать использование access token и refresh token.
- Объяснить связь Telegram authentication и backend session.
- Определить базовые ограничения хранения и отзыва refresh token.
- Поддержать security-документацию.

## Non Responsibilities

- ADR не определяет точный состав JWT claims.
- ADR не выбирает конкретные сроки жизни токенов.
- ADR не описывает frontend-хранение токенов.
- ADR не создает security configuration.

## Design Decisions

После успешной проверки Telegram init data backend выдает:

- access token для авторизации API-запросов;
- refresh token для продления сессии.

Access token должен быть короткоживущим и содержать минимальные claims: идентификатор backend-пользователя и технические данные, необходимые security layer.

Refresh token должен храниться на стороне backend только в безопасном представлении. Для MVP допустима простая модель: один активный refresh token на пользовательскую сессию или ограниченный набор активных сессий. Logout и подозрение на компрометацию должны приводить к отзыву refresh token.

## Constraints

- Нельзя доверять Telegram init data без проверки подписи.
- Нельзя использовать Telegram username как идентификатор безопасности.
- Нельзя хранить refresh token в открытом виде.
- Нельзя помещать роли и права доступа к группам в access token как источник истины.
- Authorization должна проверять актуальное членство пользователя в группе или мероприятии.

## Future Evolution

- Ротация refresh token при каждом обновлении.
- Device/session management.
- Server-side token revocation list.
- Более строгие политики истечения токенов для production.
- Интеграционные тесты сценариев безопасности.

## Related Documents

- `docs/architecture/telegram-auth-flow.md`
- `docs/security/authentication.md`
- `docs/security/jwt.md`
- `docs/security/authorization.md`
- `docs/integrations/telegram.md`
- `docs/modules/auth.md`
