# Module: common

## Purpose

Модуль `common` содержит общие типы, ошибки и утилиты, которые не принадлежат конкретному домену, но нужны нескольким модулям.

## Responsibilities

- Определять базовые доменно-нейтральные типы: идентификаторы, денежные значения, timestamps.
- Предоставлять иерархию доменных и технических ошибок.
- Содержать общие утилиты валидации и форматирования.
- Определять shared contracts для пагинации, результатов операций и correlation id.

## Domain Objects

- `Money` — денежная сумма в минимальных единицах (копейки).
- `DomainError` — базовый тип бизнес-ошибки с кодом и сообщением.
- `TechnicalError` — ошибка инфраструктуры или интеграции.
- `PageRequest` / `PageResult` — пагинация для списков.
- `CorrelationId` — идентификатор трассировки запроса.

## Dependencies

- Нет зависимостей от других модулей. `common` — нижний слой.

## Events

На MVP внутренние события не публикуются из `common`. Модуль предоставляет только типы для будущих событий.

## Database Objects

`common` не владеет таблицами БД.

## Public Contracts

- `Money.ofKopecks(long)` — создание денежной суммы.
- `DomainException` — базовое исключение с error code.
- `ErrorCode` — enum каталога ошибок (ссылка на `architecture/error-handling.md`).
- Общие DTO-типы для пагинации и идентификаторов.

## Future Extensions

- Общие audit-поля (`createdAt`, `updatedAt`) как mixin или base entity.
- Event envelope types для Kafka.
- ArchUnit-правила для проверки зависимостей модулей.
