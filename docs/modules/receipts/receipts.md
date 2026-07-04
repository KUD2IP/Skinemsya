# Module: receipts

## Purpose

Модуль `receipts` управляет чеками, позициями мероприятия, выбором позиций и нормализацией JSON от ML-сервиса.

## Responsibilities

- Ручное добавление и редактирование позиций.
- Загрузка чека и связь с файлом.
- Вызов ML-сервиса и прием структурированного JSON.
- Валидация и нормализация JSON чека.
- Создание позиций из распознанного чека.
- Управление выбором позиций участниками.
- Общие позиции с делением на всех или выбранную группу.

## Domain Objects

- `Receipt` — чек с `id`, `eventId`, `fileId`, `status`, `mlResponse`.
- `Position` — позиция расхода: `name`, `quantity`, `totalPrice`, `isShared`, `source` (`MANUAL` | `RECEIPT`).
- `PositionSelection` — выбор позиции участником.
- `ReceiptStatus` — enum: `UPLOADED`, `PROCESSING`, `PROCESSED`, `FAILED`.

## Dependencies

- `events` — контекст мероприятия, статус распределения.
- `users` — участники выбора.
- `files` — хранение изображения чека.
- `integrations` — вызов ML-сервиса.
- `common` — Money, ошибки.

## Events

- `ReceiptUploaded` — файл чека загружен.
- `ReceiptProcessed` — JSON обработан, позиции созданы.
- `ReceiptFailed` — ML-сервис вернул ошибку.
- `PositionAdded` / `PositionUpdated` — изменение позиций.
- `PositionSelected` — участник выбрал позицию.
- `DistributionCompleted` — все участники завершили выбор.

## Database Objects

- `receipts` — id, event_id (FK), file_id (FK), status, ml_raw_json, created_at.
- `positions` — id, event_id (FK), receipt_id (FK nullable), name, quantity, total_price_kopecks, is_shared, source, created_at.
- `position_selections` — id, position_id (FK), user_id (FK). Unique (position_id, user_id).
- `shared_position_targets` — position_id (FK), user_id (FK) — для общих позиций на выбранную группу.

## Public Contracts

- `ReceiptService.uploadReceipt(eventId, fileId)` → `Receipt`
- `ReceiptService.processReceipt(receiptId)` → `Receipt`
- `PositionService.addManual(eventId, positionData)` → `Position`
- `PositionService.update(positionId, positionData)` → `Position`
- `PositionService.select(positionId, userId)` → `PositionSelection`
- `PositionService.sendToDistribution(eventId)` → void
- REST: `POST /api/v1/events/{eventId}/positions`, `POST /api/v1/events/{eventId}/receipts`

## Position Model

Every position must have:

- `eventId`
- `name`
- `quantity`
- `totalPrice`
- `source`
- optional `receiptId`
- optional sharing metadata

`quantity` is not the number of users who share the position. It is the quantity from receipt/manual input. Sharing is represented by `position_selections` or `shared_position_targets`.

Position source:

| Source | Meaning | Editable |
| --- | --- | --- |
| `MANUAL` | User entered the position manually | Yes, until distribution |
| `RECEIPT` | Created from ML JSON | Yes, until distribution |

Editing a receipt-created position does not require reprocessing the receipt. The edited value becomes the business value used for debt calculation.

## Selection Rules

| Position Type | Who Pays |
| --- | --- |
| Selected by one user | That user |
| Selected by multiple users | Split equally among selected users |
| Shared for all | Split among all event participants |
| Shared for selected targets | Split among listed users |

Constraints:

- A user can select a position only once.
- User must be event participant.
- Position must belong to the same event.
- Selection is allowed only while event status permits distribution.

## Receipt Processing Rules

1. Receipt upload creates `Receipt` with status `UPLOADED`.
2. Processing sets status to `PROCESSING`.
3. Valid ML JSON creates receipt positions and status `PROCESSED`.
4. Invalid JSON sets status `FAILED`; manual positions remain available.
5. Low confidence result creates positions with a warning marker if the model supports it.

The module must store raw ML JSON only if it does not contain sensitive data beyond receipt content. Raw JSON is useful for debugging and regression tests.

## Edge Cases

| Case | Expected Behavior |
| --- | --- |
| User uploads receipt before adding manual positions | Allowed |
| User adds manual positions and then uploads receipt | Allowed; positions are merged in event list |
| ML returns duplicate items | Keep as separate positions unless normalization rule merges them |
| User edits receipt position | Edited position is used for calculation |
| User deletes receipt position | Position excluded from calculation |
| ML fails | Receipt failed, manual position flow remains available |
| Distribution already started | Position edits are blocked |

## AI Implementation Notes

Do not make receipt mandatory. Manual positions are first-class and must work without ML service.

Do not let `debts` read raw receipt JSON. `debts` consumes normalized positions and selections only.

Do not use ML confidence as the only condition for business correctness. Backend validation and user review remain required.

## Future Extensions

- Несколько чеков на мероприятие.
- Ручная коррекция распознанных позиций с diff.
- Асинхронная обработка чека через очередь.
- Confidence-based auto-accept.
