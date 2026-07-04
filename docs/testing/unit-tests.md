# Testing: Unit Tests

## Purpose

Документ описывает unit-тесты: что тестировать без БД и внешних зависимостей.

## Context

Unit-тесты — самый быстрый уровень тестирования. Критичная бизнес-логика должна быть покрыта unit-тестами для детерминированной проверки правил.

## Responsibilities

- Определить scope unit-тестов.
- Перечислить обязательные тест-кейсы.
- Описать подход к мокам.

## Non Responsibilities

- Документ не описывает integration-тесты.
- Документ не задает framework configuration.

## Design Decisions

Обязательные unit-тесты:

**debts — расчет долгов:**
- Равное деление позиции между N участниками.
- Общая позиция на всех участников.
- Общая позиция на выбранную группу.
- Детерминированное округление (копейки).
- Плательщик не имеет долга перед собой.
- Нулевая сумма при отсутствии выбора.

**receipts — нормализация JSON:**
- Валидный JSON → позиции.
- Пустой items → ошибка.
- Несовпадение total и суммы items → предупреждение.
- Low confidence → флаг.
- Конвертация цен в копейки.

**payments — статусы:**
- Маппинг debt status ↔ payment status.
- Нельзя confirm payer без debtor confirm.

**MapStruct mappers:**
- Entity → DTO mapping smoke tests.

Подход:
- Pure functions тестируются без Spring context.
- Service layer — Mockito для зависимостей.
- Без `@SpringBootTest` в unit-тестах.

## Constraints

- Unit-тесты выполняются < 30 секунд суммарно.
- Нет реальной БД.
- Нет сетевых вызовов.

## Future Evolution

- Parameterized tests для таблиц округления.
- Property-based testing для debt calculation.

## Related Documents

- `docs/modules/debts.md`
- `docs/modules/receipts.md`
- `docs/adr/ADR-0005-mapstruct.md`
- `docs/testing/testing-strategy.md`
