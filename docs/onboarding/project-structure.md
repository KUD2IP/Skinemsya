# Onboarding: Project Structure

## Purpose

Документ объясняет структуру репозитория: Maven-модули, пакеты, документация.

## Context

Проект — Maven multi-module modular monolith. Структура должна быть понятна без чтения pom.xml.

## Responsibilities

- Описать структуру репозитория.
- Объяснить назначение каждого Maven-модуля.
- Показать расположение документации.

## Non Responsibilities

- Документ не описывает package structure внутри модулей (будет при реализации).

## Design Decisions

```
skinemsya_java/
├── pom.xml                          # Root parent POM
├── docker-compose.yml               # Local PostgreSQL
├── docs/                            # Документация (источник истины)
│   ├── architecture/
│   ├── adr/
│   ├── business/
│   ├── database/
│   ├── modules/
│   ├── integrations/
│   ├── security/
│   ├── testing/
│   ├── deployment/
│   ├── roadmap/
│   ├── ml/
│   ├── onboarding/
│   ├── ai/
│   └── documentation-catalog.md
└── skinemsya_parent/                # Maven parent
    ├── pom.xml
    ├── app/                         # Spring Boot entrypoint
    ├── common/                      # Shared types, errors
    ├── auth/                        # Telegram auth, JWT
    ├── users/                       # Users, profiles
    ├── groups/                      # Groups, members
    ├── events/                      # Events (мероприятия) — to be added
    ├── receipts/                    # Receipts, positions, selections
    ├── debts/                       # Debt calculation — to be added
    ├── payments/                    # Payment operations
    ├── notifications/               # Telegram notifications
    ├── files/                       # File storage — to be added
    └── integrations/                 # External adapters (Telegram, ML, storage, SBP)
```

Целевая структура — 12 модулей. Maven-модули `events`, `debts`, `files`, `integrations` включены в `skinemsya_parent/pom.xml`.

Структура пакетов (при реализации):
```
skinemsya.vse.ru.{module}/
├── api/           # REST controllers, DTOs
├── application/   # Application services, public contracts
├── domain/        # Domain models, business rules
└── infrastructure/ # Repositories, adapters, entities
```

## Constraints

- Один репозиторий, один deployable artifact.
- Документация в `docs/`, не в module README.
- Maven module name = documentation module name.

## Future Evolution

- Separate repos для ML-сервиса.
- API module extraction.

## Related Documents

- `docs/architecture/backend-architecture.md`
- `docs/modules/*.md`
- `docs/documentation-catalog.md`
- `docs/onboarding/getting-started.md`
