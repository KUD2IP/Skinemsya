# Implementation Report: Phase 5 — Debts & Payments

## Metadata

| Поле | Значение |
| --- | --- |
| Этап | 5 — Debts & Payments |
| Дата | 2026-07-04 |
| Статус | completed |
| Агент / автор | Cursor Agent |

## Scope

### In scope (реализовано)

- Модуль `debts`: расчёт долгов, kopecks rounding, summary API.
- Модуль `payments`: debtor/payer confirm, bulk confirm, dispute, screenshot FK.
- Selections API с `selected_quantity`, auto-calc при завершении выбора всеми.
- REST: debts, payment-details, confirm-debtor, confirm-all, confirm-payer/dispute by debt.
- Frontend: экраны выбора блюд, оплаты со скрином, дашборд плательщика.
- Integration test: `PaymentFlowIntegrationTest`.

### Out of scope

- SBP, частичные платежи.

## Критерий завершения этапа

**Из roadmap:** полный payment flow, debt closed.

**Статус:** выполнен

## Следующий этап

Phase 6 — Polish & Notifications.
