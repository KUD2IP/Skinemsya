# Onboarding: Getting Started

## Purpose

Документ дает быстрый старт новому разработчику: что прочитать, как поднять проект, как пройти основной сценарий.

## Context

Новый разработчик должен понять систему и начать работу за один день без изучения исходного кода.

## Responsibilities

- Определить порядок чтения документации.
- Описать шаги запуска проекта.
- Дать чеклист первого дня.

## Non Responsibilities

- Документ не заменяет полную документацию.
- Документ не описывает frontend.

## Design Decisions

Порядок чтения (день 1):

1. `business/glossary.md` — термины.
2. `business/mvp-scope.md` — что входит в MVP.
3. `business/user-flows.md` — основной сценарий.
4. `architecture/system-overview.md` — система целиком.
5. `architecture/backend-architecture.md` — модули.
6. `onboarding/project-structure.md` — структура репозитория.

Порядок чтения (день 2-3):

7. `architecture/module-dependencies.md`
8. `modules/*.md` — модули по мере работы.
9. `database/erd.md` + `table-catalog.md`
10. `testing/testing-strategy.md`

Запуск:
1. Prerequisites: Java 21, Maven, Docker.
2. `docker compose up -d`
3. `.env.example` → `.env`
4. `mvn clean install`
5. `mvn spring-boot:run -pl app`
6. Swagger UI: `http://localhost:8080/swagger-ui.html`

Чеклист первого дня:
- [ ] Прочитал glossary и mvp-scope.
- [ ] Поднял backend локально.
- [ ] Понял основной user flow (чат → группа → мероприятие → долг → оплата).
- [ ] Понял разницу CHAT_LINKED и STANDALONE групп.
- [ ] Знаю, где искать документацию модуля.

## Constraints

- Документация — источник истины, не код.
- Код пока заглушка — ориентироваться на docs.

## Future Evolution

- Video walkthrough.
- Interactive tutorial.
- Devcontainer one-click setup.

## Related Documents

- `docs/deployment/local-development.md`
- `docs/business/user-flows.md`
- `docs/onboarding/project-structure.md`
- `docs/onboarding/glossary.md`
