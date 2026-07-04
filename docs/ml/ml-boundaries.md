# ML: Boundaries

## Purpose

Документ фиксирует границы ответственности ML-сервиса и Java backend.

## Context

Четкое разделение предотвращает дублирование логики и упрощает разработку обеих сторон.

## Responsibilities

- Определить, что делает ML, а что — backend.
- Зафиксировать запреты для обеих сторон.

## Non Responsibilities

- Документ не описывает API контракт (см. `ml-service.md`).

## Design Decisions

ML-сервис (Python) отвечает за:
- OCR распознавание текста с изображения.
- Извлечение структуры чека (merchant, date, items).
- Оценку confidence.
- Возврат структурированного JSON.

Java backend отвечает за:
- Валидацию JSON (обязательные поля, суммы, количества).
- Нормализацию (копейки, decimal quantity).
- Бизнес-правила (дубликаты, лимиты позиций).
- Создание доменных моделей (Position, Receipt).
- Решение о принятии/отклонении результата ML.

Backend НЕ делает:
- OCR.
- Обучение моделей.
- Image preprocessing (beyond file validation).

ML НЕ делает:
- Доступ к БД backend.
- Бизнес-валидацию (суммы долгов, участники).
- Создание доменных объектов backend.
- Принятие решений о статусе мероприятия.

## Constraints

- ML-решения не принимаются без backend-валидации.
- Backend не зависит от конкретной ML-модели — только от JSON-контракта.
- Смена ML-модели не требует изменений backend (при сохранении контракта).

## Future Evolution

- ML pre-validation rules (format detection).
- Backend feedback loop для улучшения модели.
- A/B testing ML models.

## Related Documents

- `docs/adr/ADR-0008-receipt-processing.md`
- `docs/adr/ADR-0009-python-ml-service.md`
- `docs/integrations/ml-service.md`
- `docs/ml/receipt-recognition.md`
