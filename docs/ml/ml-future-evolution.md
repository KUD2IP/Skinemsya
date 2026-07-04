# ML: Future Evolution

## Purpose

Документ описывает развитие ML-контура после MVP: качество, асинхронность, событийная интеграция.

## Context

MVP использует синхронный вызов ML-сервиса. По мере роста нагрузки и требований к качеству ML-контур будет развиваться.

## Responsibilities

- Описать улучшения ML после MVP.
- Зафиксировать условия перехода к асинхронной обработке.
- Определить событийную интеграцию ML.

## Non Responsibilities

- Документ не планирует ML roadmap по датам.
- Документ не описывает конкретные модели.

## Design Decisions

Улучшения качества:
- Fine-tuning на русских чеках (супермаркеты, рестораны, кафе).
- Confidence calibration.
- Fallback: multiple OCR engines.
- User feedback loop (correct/incorrect → training data).

Асинхронная обработка (при росте нагрузки):
1. Backend сохраняет receipt → status PROCESSING.
2. Событие `ReceiptUploaded` → ML queue.
3. ML-сервис обрабатывает async → callback/webhook.
4. Backend получает результат → status PROCESSED/FAILED.
5. Уведомление пользователю о готовности.

Событийная интеграция:
- `receipt.uploaded` → Kafka topic → ML consumer.
- `receipt.processed` → Kafka topic → backend consumer.
- Outbox pattern для reliable delivery.

## Constraints

- Async processing — post-MVP.
- Kafka — post-MVP.
- MVP sync call достаточен для < 100 receipts/day.

## Future Evolution

- Dedicated ML team and infrastructure.
- On-device OCR (client-side preprocessing).
- Receipt categorization (food, transport, etc.).

## Related Documents

- `docs/roadmap/scaling-roadmap.md`
- `docs/integrations/ml-service.md`
- `docs/ml/receipt-recognition.md`
- `docs/ml/ml-boundaries.md`
