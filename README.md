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

## Деплой на staging (GitHub Actions)

1. Подготовь VPS по инструкции: [docs/deployment/staging-server.md](docs/deployment/staging-server.md)
2. Добавь secrets в GitHub → Settings → Secrets and variables → Actions
3. Push в `main` → дождись сборки
4. **Actions → Backend CI/CD → Run workflow** → включи **Deploy to staging**

CI/CD workflow: [.github/workflows/backend.yml](.github/workflows/backend.yml)

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
