# Module: users

## Purpose

Модуль `users` управляет пользователями, Telegram identity и профилем с реквизитами для переводов.

## Responsibilities

- Хранение пользователей и Telegram identity.
- Upsert пользователя при аутентификации.
- Управление профилем: реквизиты, телефон, настройки уведомлений.
- Предоставление данных пользователя другим модулям.

## Domain Objects

- `User` — внутренний пользователь backend с `id`, `telegramUserId`, `displayName`.
- `UserProfile` — реквизиты для переводов, телефон, notification preferences.
- `TelegramIdentity` — внешняя идентичность из Telegram.

## Dependencies

- `common` — Money, ошибки, идентификаторы.

## Events

- `UserCreated` — новый пользователь зарегистрирован.
- `UserProfileUpdated` — обновлены реквизиты или настройки.

## Database Objects

- `users` — id, telegram_user_id (unique), display_name, created_at, updated_at.
- `user_profiles` — user_id (FK), payment_details, phone, notification_settings (JSON).

## Public Contracts

- `UserService.findById(userId)` → `User`
- `UserService.findByTelegramUserId(telegramUserId)` → `Optional<User>`
- `UserService.upsertFromTelegram(telegramIdentity)` → `User`
- `UserService.updateProfile(userId, profileData)` → `UserProfile`
- `UserService.getPaymentDetails(userId)` → `PaymentDetails`
- REST: `GET /api/v1/users/me`, `PUT /api/v1/users/me/profile`

## Future Extensions

- Аватар пользователя из Telegram.
- Блокировка пользователя.
- История изменений профиля.
