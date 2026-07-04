# Integration: Receipt JSON Contract

## Purpose

Документ фиксирует структуру JSON, который Python ML-сервис возвращает backend после распознавания чека.

## Context

Backend получает структурированный JSON и валидирует его перед созданием позиций мероприятия. Контракт должен быть стабильным для тестов и ML-сервиса.

## Responsibilities

- Определить поля JSON-ответа ML-сервиса.
- Зафиксировать обязательные и опциональные поля.
- Описать правила валидации на стороне backend.
- Дать основу для тестовых данных.

## Non Responsibilities

- Документ не описывает формат запроса к ML-сервису (см. `ml-service.md`).
- Документ не определяет entity/DTO классы Java.

## Design Decisions

Структура ответа ML-сервиса:

```json
{
  "merchant": "string | null",
  "date": "YYYY-MM-DD | null",
  "total": 1234.56,
  "currency": "RUB",
  "items": [
    {
      "name": "string",
      "quantity": 1.0,
      "unit_price": 100.00,
      "total_price": 100.00,
      "confidence": 0.95
    }
  ],
  "confidence": 0.90,
  "errors": []
}
```

Правила валидации backend:
- `items` — непустой массив для успешного распознавания.
- Каждый item: `name` обязателен, `quantity` > 0, `total_price` >= 0.
- `total` — если указан, должен быть близок к сумме items (допуск 1%).
- `confidence` < 0.5 — предупреждение, позиции создаются с флагом low_confidence.
- `errors` — массив строк; непустой `errors` при пустом `items` = FAILED.

Нормализация:
- Цены конвертируются в копейки (`Money`).
- `quantity` — decimal, минимум 0.01.
- Дубликаты позиций объединяются по `name` (опционально на MVP).

## Field Specification

| Field | Type | Required | Meaning | Backend Rule |
| --- | --- | --- | --- | --- |
| `merchant` | string/null | no | Название продавца | Trim, max 255 chars |
| `date` | string/null | no | Дата чека | ISO `YYYY-MM-DD`; invalid → warning |
| `total` | number/null | no | Итоговая сумма чека | Convert to kopecks if present |
| `currency` | string/null | no | Валюта | MVP accepts `RUB` or null |
| `items` | array | yes for success | Позиции чека | Empty array means failed/no positions |
| `items[].name` | string | yes | Название позиции | Trim, non-empty, max 255 chars |
| `items[].quantity` | number | yes | Количество | Must be > 0 |
| `items[].unit_price` | number/null | no | Цена за единицу | Optional; not used as source of truth |
| `items[].total_price` | number | yes | Итог по позиции | Must be >= 0 |
| `items[].confidence` | number/null | no | Уверенность по позиции | 0..1, null = low confidence |
| `confidence` | number/null | no | Общая уверенность | 0..1, null = low confidence |
| `errors` | array[string] | no | Ошибки ML | Non-empty errors may still include partial items |

## Valid Examples

Minimal successful response:

```json
{
  "merchant": null,
  "date": null,
  "total": 500.0,
  "currency": "RUB",
  "items": [
    {
      "name": "Салат",
      "quantity": 1,
      "unit_price": null,
      "total_price": 500.0,
      "confidence": 0.82
    }
  ],
  "confidence": 0.82,
  "errors": []
}
```

Partial response with warning:

```json
{
  "merchant": "Cafe",
  "date": "2026-06-15",
  "total": 1200.0,
  "currency": "RUB",
  "items": [
    {
      "name": "Unreadable item",
      "quantity": 1,
      "unit_price": 400.0,
      "total_price": 400.0,
      "confidence": 0.42
    }
  ],
  "confidence": 0.45,
  "errors": ["low_confidence"]
}
```

Failed response:

```json
{
  "merchant": null,
  "date": null,
  "total": null,
  "currency": "RUB",
  "items": [],
  "confidence": 0.0,
  "errors": ["no_items_found"]
}
```

## Backend Decision Matrix

| Condition | Receipt Status | Position Creation |
| --- | --- | --- |
| Valid items, confidence >= 0.7 | `PROCESSED` | Create positions |
| Valid items, confidence 0.5..0.69 | `PROCESSED` with warning | Create positions with low confidence marker |
| Valid items, confidence < 0.5 | `FAILED` or review-required | Do not auto-accept |
| Empty items, errors non-empty | `FAILED` | No positions |
| Invalid JSON shape | `FAILED` | No positions |
| Total mismatch <= 1% | `PROCESSED` with warning | Create positions |
| Total mismatch > 1% | Review-required or failed | Depends on product decision, default failed |

## Contract Evolution Rules

Backward-compatible changes:

- adding optional fields;
- adding new `errors` values;
- adding metadata ignored by backend.

Breaking changes:

- renaming fields;
- changing numeric types to strings;
- removing `items`;
- changing meaning of `total_price`;
- changing currency behavior.

Breaking changes require updating this document, test fixtures and an ADR if the backend behavior changes.

## Constraints

- Контракт версионируется; breaking changes требуют ADR.
- Backend не принимает сырой OCR-текст — только структурированный JSON.
- ML-сервис не возвращает доменные id backend.

## Future Evolution

- Версия контракта в заголовке (`X-Contract-Version`).
- Поддержка нескольких валют.
- Категории позиций.
- Альтернативные варианты распознавания (multiple candidates).

## Related Documents

- `docs/integrations/ml-service.md`
- `docs/modules/receipts.md`
- `docs/testing/test-data.md`
- `docs/ml/receipt-recognition.md`
