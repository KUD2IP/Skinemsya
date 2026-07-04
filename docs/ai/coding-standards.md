# AI: Coding Standards

## Purpose

Стандарты кода для Java 21 / Spring Boot 3.x проекта skinemsya.

## Context

Единые стандарты обеспечивают читаемость и согласованность кода между разработчиками и AI-агентами.

## Responsibilities

- Зафиксировать naming conventions.
- Определить структуру пакетов.
- Описать правила для MapStruct, тестов, Liquibase.

## Non Responsibilities

- Документ не настраивает IDE formatter.
- Документ не описывает frontend.

## Design Decisions

Java:
- Java 21, Spring Boot 3.x.
- Records для immutable DTO и value objects.
- `Optional` для nullable return values, не для fields.
- `var` — только когда тип очевиден.
- No Lombok на MVP (явные конструкторы, records).

Naming:
- Classes: PascalCase — `DebtService`, `GroupEntity`.
- Methods: camelCase — `calculateDebts()`, `findByChatId()`.
- Constants: UPPER_SNAKE — `MAX_FILE_SIZE`.
- Packages: `skinemsya.vse.ru.{module}.{layer}`.

Layers per module:
- `api` — controllers, request/response DTOs.
- `application` — services, public contracts (interfaces).
- `domain` — domain models, business rules, domain exceptions.
- `infrastructure` — JPA entities, repositories, adapters, mappers.

MapStruct:
- Mappers в `infrastructure/mapper/`.
- `@Mapper(componentModel = "spring")`.
- No business logic in mappers.
- Entity ↔ Domain ↔ DTO — отдельные mapper methods.

Tests:
- JUnit 5 + Mockito.
- Naming: `should{Expected}When{Condition}`.
- Arrange-Act-Assert pattern.
- Test class: `{ClassUnderTest}Test`.

Liquibase:
- Naming по `database/naming-conventions.md`.
- One changeset per logical change.
- Rollback for DDL.

## Constraints

- No reflection-based mapping (use MapStruct).
- No generic `BaseService` or `AbstractRepository` with business logic.
- No static utility classes with state.
- DTOs не содержат бизнес-логику.

## Future Evolution

- Checkstyle/SpotBugs в CI.
- Google Java Format.
- ArchUnit rules.

## Related Documents

- `docs/adr/ADR-0002-java21.md`
- `docs/adr/ADR-0005-mapstruct.md`
- `docs/database/naming-conventions.md`
- `docs/ai/forbidden-patterns.md`
