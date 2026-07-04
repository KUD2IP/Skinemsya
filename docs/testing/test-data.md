# Testing: Test Data

## Purpose

Документ описывает наборы тестовых данных для MVP: пользователи, группы, чеки, долги и негативные кейсы.

## Context

Стабильные тестовые данные ускоряют написание тестов и обеспечивают воспроизводимость.

## Responsibilities

- Определить fixture-данные для тестов.
- Описать негативные кейсы.
- Зафиксировать тестовые JSON чеков.

## Non Responsibilities

- Документ не создает SQL seed scripts для production.
- Документ не описывает production data.

## Design Decisions

Базовые fixtures:

| Fixture | Описание |
| --- | --- |
| `user_alice` | telegram_user_id: 100001, display_name: "Alice" |
| `user_bob` | telegram_user_id: 100002, display_name: "Bob" |
| `user_charlie` | telegram_user_id: 100003, display_name: "Charlie" |
| `standalone_group` | STANDALONE, owner: alice, members: alice, bob |
| `chat_linked_group` | CHAT_LINKED, chat_id: -100123, owner: alice |
| `event_dinner` | group: standalone_group, payer: alice, 3 participants |
| `positions_dinner` | 3 позиции: суп 300₽, салат 200₽, напитки 500₽ |

Тестовые JSON чеков (см. `integrations/receipt-json-contract.md`):

**valid_receipt.json** — 3 items, total совпадает, confidence 0.9.
**empty_items.json** — items: [], errors: ["no items found"].
**low_confidence.json** — confidence 0.3, items с low confidence.
**mismatched_total.json** — total не совпадает с суммой items.

Негативные кейсы:
- Auth с невалидной подписью init data.
- Доступ к чужой группе.
- Расчет долгов без выбора позиций.
- Confirm payer без debtor confirm.
- Удаление группы с активными долгами.
- ML timeout → receipt FAILED.

## Constraints

- Test data не пересекается с production ids.
- Fixtures создаются в тестах, не в SQL seeds.
- JSON fixtures в `src/test/resources/receipts/`.

## Future Evolution

- Factory pattern для test builders.
- Shared fixture library.
- Faker для random data.

## Related Documents

- `docs/integrations/receipt-json-contract.md`
- `docs/business/business-rules.md`
- `docs/testing/unit-tests.md`
- `docs/testing/integration-tests.md`
