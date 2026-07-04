# ADR-0005: MapStruct

## Status

Accepted

## Purpose

Зафиксировать MapStruct как основной инструмент compile-time mapping между слоями backend.

## Context

Backend будет преобразовывать данные между API-моделями, application-моделями, domain-моделями и persistence-моделями. Для MVP важно писать меньше шаблонного кода, но не терять типобезопасность и прозрачность.

Reflection-based mapping может скрывать ошибки до runtime и усложнять диагностику. Ручной mapping прост, но быстро создает много однообразного кода.

## Responsibilities

- Зафиксировать подход к mapping.
- Снизить количество ручного boilerplate.
- Сохранить compile-time проверку.
- Ограничить использование mapping только техническим преобразованием данных.

## Non Responsibilities

- ADR не определяет DTO.
- ADR не создает mapper interfaces.
- ADR не разрешает переносить бизнес-логику в mapper.
- ADR не заменяет coding standards.

## Design Decisions

MapStruct используется для compile-time mapping. Он генерирует код на этапе сборки и позволяет находить часть ошибок до runtime.

Mapper не должен принимать бизнес-решения. Нормализация чека, расчет долга, проверка статуса оплаты и authorization остаются в доменных или application-сервисах. Mapper только переносит данные между представлениями.

Для простых случаев ручной mapping допустим, если он понятнее и короче. MapStruct не должен использоваться механически там, где он усложняет чтение.

## Constraints

- В mapper нельзя помещать бизнес-правила.
- Mapper не должен обращаться к repository или внешним сервисам.
- Ошибки mapping должны быть проверяемы тестами для критичных сценариев.
- Маппинг между модулями не должен обходить публичные контракты.

## Future Evolution

После стабилизации моделей можно добавить общие правила MapStruct configuration, проверки unmapped fields и стандарты именования mapper-компонентов. Эти правила должны быть описаны в `ai/coding-standards.md`.

## Related Documents

- `docs/ai/coding-standards.md`
- `docs/modules/common.md`
- `docs/testing/unit-tests.md`
- `docs/architecture/backend-architecture.md`

