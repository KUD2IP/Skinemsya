# ML: Receipt Recognition

## Purpose

Документ описывает ML/OCR-контур: что делает Python-сервис, какие данные возвращает backend.

## Context

Распознавание чеков — единственная ML-задача MVP. Python-сервис выполняет OCR и возвращает структурированный JSON.

## Responsibilities

- Описать pipeline распознавания.
- Зафиксировать входные и выходные данные.
- Определить обработку confidence и ошибок.

## Non Responsibilities

- Документ не описывает ML-модели и training.
- Документ не заменяет `receipt-json-contract.md`.

## Design Decisions

Pipeline:
1. Backend передает URL/base64 изображения чека.
2. Python-сервис: preprocess image → OCR → parse receipt structure.
3. Возвращает JSON по контракту `receipt-json-contract.md`.
4. Backend валидирует и создает позиции.

Вход: JPEG/PNG/WebP, max 10 MB.
Выход: JSON с merchant, date, items[], total, confidence.

Обработка confidence:
- `confidence >= 0.7` — автоматическое создание позиций.
- `0.5 <= confidence < 0.7` — позиции создаются с предупреждением.
- `confidence < 0.5` — FAILED, ручное добавление.

Ошибки:
- Unreadable image → `errors: ["unreadable"]`.
- No items found → `errors: ["no items"]`.
- Timeout → backend retry 1x, then FAILED.

## Constraints

- ML-сервис не имеет доступа к БД backend.
- ML-сервис stateless на MVP.
- Качество распознавания — ответственность ML-команды.

## Future Evolution

- GPU acceleration.
- Fine-tuned models for Russian receipts.
- Multi-language support.
- Async processing queue.

## Related Documents

- `docs/integrations/ml-service.md`
- `docs/integrations/receipt-json-contract.md`
- `docs/ml/ml-boundaries.md`
- `docs/architecture/receipt-processing-flow.md`
