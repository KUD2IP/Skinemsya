# Module: integrations

## Purpose

Модуль `integrations` содержит адаптеры внешних систем: Telegram, ML-сервис, файловое хранилище и будущий СБП.

## Responsibilities

- Адаптер проверки Telegram init data.
- Адаптер Telegram bot API для уведомлений.
- HTTP-клиент Python ML-сервиса.
- Адаптер файлового хранилища (local/S3).
- Заглушка СБП-адаптера для post-MVP.
- Обработка таймаутов, retry и маппинг ошибок интеграций.

## Domain Objects

- `TelegramInitDataValidator` — проверка HMAC init data.
- `MlServiceClient` — клиент ML/OCR сервиса.
- `FileStorageAdapter` — абстракция хранения файлов.
- `TelegramBotClient` — отправка сообщений через bot API.
- `SbpAdapter` — заглушка для будущей СБП-интеграции.

## Dependencies

- `common` — ошибки, технические типы.

## Events

`integrations` не публикует доменные события. Только технические результаты вызовов.

## Database Objects

`integrations` не владеет таблицами БД.

## Public Contracts

- `TelegramAdapter.validateInitData(initDataString)` → `TelegramInitData`
- `TelegramAdapter.sendMessage(chatId, text)` → void
- `MlServiceClient.recognizeReceipt(imageUrl)` → `ReceiptJson`
- `FileStorageAdapter.store(inputStream, metadata)` → `StorageRef`
- `FileStorageAdapter.retrieve(storageRef)` → `InputStream`

## Future Extensions

- Circuit breaker для внешних вызовов.
- Полноценный SBP adapter.
- Webhook endpoints для Telegram и банка.
- Contract tests с WireMock.
