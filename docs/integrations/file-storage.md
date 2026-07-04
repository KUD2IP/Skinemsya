# Integration: File Storage

## Purpose

Документ описывает хранение файлов (изображений чеков): где хранятся, как backend управляет метаданными и доступом.

## Context

Пользователь загружает изображение чека. Backend сохраняет файл и метаданные, предоставляет доступ ML-сервису и участникам мероприятия.

## Responsibilities

- Описать стратегию хранения на MVP и в production.
- Зафиксировать ограничения размера и типов файлов.
- Определить модель доступа к файлам.
- Описать связь файла с модулем `files` и `receipts`.

## Non Responsibilities

- Документ не реализует storage adapter.
- Документ не описывает CDN.

## Design Decisions

MVP (local storage):
- Файлы сохраняются в локальную директорию (`FILE_STORAGE_PATH` env var).
- Метаданные — в таблице `files` (модуль `files`).
- Доступ через authenticated API endpoint.
- ML-сервис получает URL или stream через internal call.

Ограничения:
- Максимальный размер: 10 MB.
- Допустимые MIME: `image/jpeg`, `image/png`, `image/webp`.
- Один файл — один чек (на MVP).

Production (future):
- S3-совместимое хранилище (MinIO, AWS S3).
- Presigned URLs для ML-сервиса.
- Lifecycle policy для удаления старых файлов.

## Constraints

- Файлы не публичны без аутентификации.
- Только участники мероприятия имеют доступ к файлу чека.
- Путь storage не раскрывается клиенту напрямую.

## Future Evolution

- S3/MinIO migration.
- Image compression before storage.
- Virus scanning.
- Separate bucket per environment.

## Related Documents

- `docs/modules/files.md`
- `docs/modules/integrations/integrations.md`
- `docs/deployment/configuration-management.md`
- `docs/security/api-security.md`
