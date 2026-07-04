# Telegram Auth Flow

## Purpose

Документ описывает аутентификацию пользователя через Telegram Mini App и выпуск backend access token и refresh token.

## Context

Пользователь входит в приложение из Telegram Mini App. Telegram передает init data, которую backend должен проверить. После успешной проверки backend создает или обновляет пользователя и выдает пару токенов: короткоживущий access token для API-запросов и refresh token для продления сессии без повторного прохождения Telegram auth flow.

## Responsibilities

- Описать доверенную точку входа authentication flow.
- Зафиксировать проверку Telegram init data.
- Определить создание или обновление пользователя.
- Описать выпуск access token и refresh token после успешной Telegram-проверки.
- Зафиксировать базовую модель refresh token для MVP.

## Non Responsibilities

- Документ не описывает код проверки подписи Telegram.
- Документ не определяет JWT claims и формат refresh token в деталях.
- Документ не описывает frontend-хранение токена.
- Документ не решает authorization внутри групп и мероприятий.

## Design Decisions

Поток MVP:

1. Mini App получает Telegram init data.
2. Frontend отправляет init data в backend authentication endpoint.
3. Backend проверяет подпись и срок актуальности init data.
4. Backend извлекает Telegram user id и базовый профиль.
5. Модуль `auth` обращается к `users`, чтобы найти, создать или обновить пользователя.
6. Backend выпускает access token с backend user id и минимальными claims.
7. Backend выпускает refresh token, связанный с пользователем и текущей сессией.
8. Последующие API-запросы используют access token.
9. Когда access token истекает, frontend отправляет refresh token в backend.
10. Backend проверяет refresh token, перевыпускает access token и, если принято в security-документе, ротирует refresh token.

Telegram user id является стабильным внешним идентификатором, но внутренние связи должны строиться через backend user id. Это защищает доменную модель от прямой зависимости от внешнего identity provider.

Refresh token нужен сразу, потому что Telegram Mini App не должен требовать повторного входа при каждом истечении короткого access token. MVP-реализация должна быть простой: один активный refresh token на пользовательскую сессию или ограниченный набор активных сессий, хранение только безопасного представления refresh token, возможность отзыва при logout или подозрении на компрометацию.

## Constraints

- Нельзя доверять init data без проверки подписи.
- Нельзя использовать username как уникальный идентификатор пользователя.
- Нельзя хранить Telegram bot token в коде или документации с секретными значениями.
- Access token должен быть подписан секретом из окружения.
- Refresh token нельзя хранить в открытом виде.
- Claims access token должны быть минимальными, чтобы не тащить в токен изменяемое состояние.
- Authorization не должна полагаться только на факт валидного JWT.
- Истекший или отозванный refresh token не должен продлевать сессию.

## Future Evolution

- Ротация refresh token при каждом продлении сессии, если это потребуется по требованиям безопасности.
- Отзыв токенов для критичных инцидентов безопасности.
- Ограничение количества активных сессий пользователя.
- Расширение профиля пользователя настройками Mini App.
- Использование Telegram Bot API для уведомлений после появления notifications-сценариев.

## Related Documents

- `docs/architecture/request-flow.md`
- `docs/security/authentication.md`
- `docs/security/jwt.md`
- `docs/security/authorization.md`
- `docs/integrations/telegram.md`
- `docs/modules/auth.md`
- `docs/modules/users.md`
- `docs/adr/ADR-0006-jwt-authentication.md`
