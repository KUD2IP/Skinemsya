# Skinemsya Backend

Backend для Telegram Mini App — совместное ведение расходов в группах (чеки, долги, оплаты).

**Стек:** Java 21, Spring Boot 3, PostgreSQL, Liquibase, Docker.

## Быстрый старт (локально)

```bash
docker compose up -d
cp .env.example .env   # заполни секреты
mvn clean install
mvn spring-boot:run -pl skinemsya_parent/app
```

Swagger UI: http://localhost:8080/swagger-ui.html

Подробнее: [docs/onboarding/getting-started.md](docs/onboarding/getting-started.md)

## Деплой (production, GitHub Actions)

1. Подготовь сервер (см. `docs/deployment/staging-server.md`)
2. Настрой environment **production** в GitHub (`PRODUCTION_ENV`, `SSH_PRIVATE_KEY`, `PRODUCTION_HOST`, `PRODUCTION_USER`)
3. Git Flow: `release/*` → PR → `master` → автоматический deploy

Подробнее: [docs/deployment/ci-cd.md](docs/deployment/ci-cd.md)

Workflows: `backend-ci.yml`, `backend-release.yml`, `backend-production.yml`

## Структура репозитория

| Путь | Описание |
| --- | --- |
| `skinemsya_parent/` | Maven-модули backend |
| `deploy/` | Production compose, шаблон `.env` |
| `docs/` | Архитектура, модули, деплой |
| `Dockerfile` | Multi-stage сборка backend-образа |

## Документация

- [Архитектура](docs/architecture/system-overview.md)
- [CI/CD](docs/deployment/ci-cd.md)
- [Docker](docs/deployment/docker.md)
- [Staging на VPS](docs/deployment/staging-server.md)
