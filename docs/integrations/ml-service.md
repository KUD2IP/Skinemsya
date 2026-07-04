# Integration: ML Service

## Purpose

Документ описывает интеграцию backend с отдельным Python ML/OCR-сервисом для распознавания чеков.

## Context

Backend не выполняет OCR. Изображение чека передается в Python-сервис, который возвращает структурированный JSON. Backend валидирует и нормализует результат.

## Responsibilities

- Описать контракт HTTP-вызова ML-сервиса.
- Зафиксировать таймауты, retry и обработку ошибок.
- Определить ответственность за OCR (Python) vs валидацию (Java).
- Описать синхронную модель вызова на MVP.

## Non Responsibilities

- Документ не описывает ML-модели и обучение.
- Документ не определяет инфраструктуру Python-сервиса.
- Документ не заменяет `receipt-json-contract.md`.

## Design Decisions

Вызов на MVP (синхронный):
1. `receipts` модуль получает `fileId` загруженного чека.
2. `integrations.MlServiceClient` отправляет `POST /api/v1/recognize` с URL или base64 изображения.
3. Таймаут: 30 секунд (настраиваемо).
4. При успехе — JSON по контракту `receipt-json-contract.md`.
5. При ошибке — `ReceiptStatus.FAILED`, пользователь может добавить позиции вручную.

Retry: 1 повтор при timeout или 5xx. Без retry при 4xx.

## Constraints

- ML-сервис — отдельный deployable unit.
- Backend не хранит ML-модели.
- Ошибки ML не должны блокировать ручное добавление позиций.
- ML-сервис URL — env var `ML_SERVICE_URL`.

## Future Evolution

- Асинхронная обработка через очередь.
- Callback/webhook от ML-сервиса.
- Batch processing.
- Версионирование API ML-сервиса.

## Related Documents

- `docs/adr/ADR-0009-python-ml-service.md`
- `docs/integrations/receipt-json-contract.md`
- `docs/architecture/receipt-processing-flow.md`
- `docs/modules/receipts.md`
- `docs/ml/receipt-recognition.md`
