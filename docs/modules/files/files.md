# Module: files

## Purpose

Модуль `files` управляет загрузкой и хранением файлов (изображений чеков) и метаданными доступа.

## Responsibilities

- Прием загрузки изображения чека от клиента.
- Сохранение файла во внешнем или локальном хранилище.
- Хранение метаданных: имя, размер, mime-type, владелец.
- Контроль доступа: только участники мероприятия.
- Предоставление URL или stream для ML-сервиса.

## Domain Objects

- `StoredFile` — файл: `id`, `ownerId`, `originalName`, `mimeType`, `sizeBytes`, `storagePath`, `createdAt`.

## Dependencies

- `users` — владелец файла.
- `integrations` — адаптер файлового хранилища.
- `common` — ошибки.

## Events

- `FileUploaded` — файл загружен и сохранен.

## Database Objects

- `files` — id, owner_id (FK users), original_name, mime_type, size_bytes, storage_path, created_at.

## Public Contracts

- `FileService.upload(ownerId, fileData, mimeType)` → `StoredFile`
- `FileService.getById(fileId)` → `StoredFile`
- `FileService.getContent(fileId)` → `InputStream`
- `FileService.delete(fileId)` → void
- REST: `POST /api/v1/files`, `GET /api/v1/files/{id}`

## Future Extensions

- S3/MinIO как production storage.
- Presigned URLs.
- Автоудаление старых файлов.
- Сжатие изображений.
