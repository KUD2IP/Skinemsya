# ADR-0011: Распознавание чека через OpenRouter

## Status

Accepted. Заменяет прод-путь из [ADR-0009](ADR-0009-python-ml-service.md).

## Purpose

Зафиксировать, что фото чека читает vision-модель через OpenRouter, а не локальный Python OCR.

## Decision

- Прод-вызов идёт в OpenRouter, модель по умолчанию `qwen/qwen3.8-flash`, reasoning выключен.
- Контракт ответа остаётся `MlReceiptResponse`. Пустой ключ `OPENROUTER_API_KEY` оставляет тестовую заглушку, не Python-сервис.
- Подписка на 30 дней. Карта и СБП — ЮKassa, сумма счёта `49.00` RUB при цене 4900 копеек. Telegram Stars — вторая кнопка, на ней нет рублёвой цены.
- Ручной ввод позиций бесплатный. Дневной потолок защищает ключ и не называется лимитом подписки.
- Контейнер `ml-service` в деплой не входит. Репозиторий `Skinemsya_ml` архивируется, история git сохраняется.

## Related Documents

- `docs/integrations/receipt-json-contract.md`
- `docs/deployment/docker.md`
