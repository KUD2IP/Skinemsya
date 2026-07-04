# AI: Forbidden Patterns

## Purpose

Запрещенные паттерны и практики на этапе MVP.

## Context

AI-агенты и разработчики должны знать, что явно запрещено, чтобы не усложнять MVP.

## Responsibilities

- Перечислить запрещенные архитектурные паттерны.
- Описать запрещенные code patterns.
- Объяснить почему каждый паттерн запрещен.

## Non Responsibilities

- Документ не описывает что разрешено (см. architecture-rules, module-rules).

## Design Decisions

Архитектурные запреты (MVP):

| Паттерн | Почему запрещен |
| --- | --- |
| Микросервисы | Сложность деплоя и отладки на MVP |
| Kafka / RabbitMQ | Нет потребителей, premature optimization |
| Event Sourcing | Enterprise complexity, не нужен для MVP |
| CQRS | Два model pipeline без необходимости |
| API Gateway | Один backend, не нужен |
| Service Mesh | Нет микросервисов |
| OCR в Java | ML на Python (ADR-0009) |
| Shared database anti-pattern | Модули не читают чужие таблицы напрямую |

Code patterns запреты:

| Паттерн | Почему запрещен |
| --- | --- |
| God Service | Нарушает module boundaries |
| Universal BaseEntity с 20 полями | Over-engineering |
| Reflection-based DTO mapping | MapStruct (ADR-0005) |
| `@Transactional` на controller | Транзакции в application service |
| Cross-module entity imports | Только public contracts |
| Hardcoded secrets | Env vars (security/secrets-management) |
| Floating point для денег | BIGINT kopecks или BigDecimal |
| Telegram username как ID | Нестабильный идентификатор |

Module запреты:

| Паттерн | Почему запрещен |
| --- | --- |
| `common` с доменной логикой | common — только shared types |
| `integrations` с бизнес-решениями | Только адаптеры |
| `app` с бизнес-логикой | Только composition root |
| Циклические зависимости | Ломает модульность |
| Прямой SQL к чужим таблицам | Нарушает ownership |

## Constraints

- Запреты действуют на MVP. Post-MVP — пересмотр через ADR.
- Исключение только с явным ADR.

## Future Evolution

- ArchUnit automated checks.
- Custom lint rules.

## Related Documents

- `docs/adr/ADR-0001-modular-monolith.md`
- `docs/adr/ADR-0010-module-boundaries.md`
- `docs/ai/architecture-rules.md`
- `docs/ai/module-rules.md`
