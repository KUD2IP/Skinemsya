# Module: payments

## Purpose

Модуль `payments` управляет платежными операциями: просмотр реквизитов, подтверждение перевода должником и получения плательщиком.

## Responsibilities

- Создание платежной операции для долга.
- Показ реквизитов плательщика из профиля (`перевести`).
- Фиксация подтверждения должника (`перевел`).
- Фиксация подтверждения плательщика (`получил`).
- Управление статусами платежа и маппинг на статус долга.
- Защита от дублирования платежей по одному долгу.

## Domain Objects

- `Payment` — платеж: `id`, `debtId`, `status`, `debtorConfirmedAt`, `payerConfirmedAt`.
- `PaymentStatus` — enum: `CREATED`, `DEBTOR_CONFIRMED`, `PAYER_CONFIRMED`, `CANCELLED`, `DISPUTED`.

## Dependencies

- `debts` — долг, закрытие долга.
- `users` — реквизиты плательщика из профиля.
- `common` — Money, ошибки.

## Events

- `PaymentCreated` — платежная операция создана.
- `DebtorConfirmed` — должник нажал `перевел`.
- `PayerConfirmed` — плательщик нажал `получил`; долг закрывается.

## Database Objects

- `payments` — id, debt_id (FK unique), status, debtor_confirmed_at, payer_confirmed_at, created_at, updated_at.

## Public Contracts

- `PaymentService.createForDebt(debtId)` → `Payment`
- `PaymentService.getPaymentDetails(debtId)` → `PaymentDetails` (сумма, реквизиты)
- `PaymentService.confirmByDebtor(paymentId, debtorId)` → `Payment`
- `PaymentService.confirmByPayer(paymentId, payerId)` → `Payment`
- REST: `POST /api/v1/debts/{debtId}/payment`, `POST /api/v1/payments/{id}/confirm-debtor`, `POST /api/v1/payments/{id}/confirm-payer`

## Operation Semantics

### Create Payment

Creates or returns an active payment for a debt.

Rules:

- Debt must exist and be `UNPAID`.
- Requester must be debtor or creditor.
- Only one active payment per debt.
- Payment starts in `CREATED`.

### View Payment Details (`перевести`)

Returns:

- debt amount;
- payer display name;
- payer payment details from `users.UserProfile`;
- current payment status.

Rules:

- Does not change payment status.
- Only debtor can view payer requisites for payment action.
- If payer profile lacks payment details, return business error `PAYMENT_DETAILS_MISSING`.

### Confirm By Debtor (`перевел`)

Rules:

- Requester must be debt debtor.
- Payment must be `CREATED`.
- Set `debtorConfirmedAt`.
- Set status `DEBTOR_CONFIRMED`.
- Notify payer.
- Ask `debts` to move debt to `PENDING_CONFIRMATION`.

### Confirm By Payer (`получил`)

Rules:

- Requester must be debt creditor.
- Payment must be `DEBTOR_CONFIRMED`.
- Set `payerConfirmedAt`.
- Set status `PAYER_CONFIRMED`.
- Call `DebtService.close(debtId)`.
- Notify debtor.

## Status Transition Table

| From | Action | Actor | To |
| --- | --- | --- | --- |
| none | create | debtor or creditor | `CREATED` |
| `CREATED` | `перевел` | debtor | `DEBTOR_CONFIRMED` |
| `DEBTOR_CONFIRMED` | `получил` | payer/creditor | `PAYER_CONFIRMED` |
| `CREATED` | cancel | debtor or system | `CANCELLED` |
| `DEBTOR_CONFIRMED` | dispute | payer/creditor | `DISPUTED` |

## Error Cases

| Condition | Error |
| --- | --- |
| Debt already paid | `DEBT_ALREADY_PAID` |
| Requester is not debtor for `перевел` | `PAYMENT_DEBTOR_REQUIRED` |
| Requester is not creditor for `получил` | `PAYMENT_PAYER_REQUIRED` |
| Payer details missing | `PAYMENT_DETAILS_MISSING` |
| Confirm payer before debtor | `PAYMENT_INVALID_STATE` |
| Duplicate active payment creation | Return existing payment |

## AI Implementation Notes

Do not store bank card numbers or sensitive banking data beyond MVP payment details. The exact allowed fields are defined by security and product decisions; until then use a generic `payment_details` text field.

Do not close debt inside `confirmByDebtor`. The only closing action is payer confirmation.

Do not introduce SBP classes into MVP `payments` domain unless the scope changes. Keep SBP in `integrations/sbp.md` as future evolution.

## Future Extensions

- СБП-интеграция.
- Частичные платежи.
- Спорные платежи с комментариями.
- Webhook от банка.
