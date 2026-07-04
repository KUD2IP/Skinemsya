# Deployment: Local Development

## Purpose

Документ описывает локальный запуск backend для разработки.

## Context

Разработчик должен поднять backend, PostgreSQL и зависимости без сложной инфраструктуры.

## Responsibilities

- Описать prerequisites.
- Зафиксировать шаги запуска.
- Определить env vars для local.

## Non Responsibilities

- Документ не описывает production deploy.
- Документ не настраивает IDE.

## Design Decisions

Prerequisites:
- Java 21 (JDK).
- Maven 3.9+.
- Docker (для PostgreSQL и Testcontainers).
- Git.

Запуск:
1. `docker compose up -d` — PostgreSQL.
2. Скопировать `.env.example` → `.env` в корень репозитория, заполнить значения.
3. `mvn clean install` — сборка всех модулей.
4. `mvn spring-boot:run -pl skinemsya_parent/app` — запуск backend (профиль `dev` по умолчанию, `.env` подхватывается автоматически).
5. OpenAPI: `http://localhost:8080/swagger-ui.html`.

Конфигурация через `.env`:
- Spring Boot импортирует `.env` из корня репозитория (`spring.config.import`).
- Основные переменные: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `JWT_SECRET`, `TELEGRAM_BOT_TOKEN`.
- Профиль: `SPRING_PROFILES_ACTIVE=dev` (значение по умолчанию, если не задано).

Пример `.env`:
- `DB_URL=jdbc:postgresql://localhost:5432/skinemsya`
- `DB_PASSWORD=skinemsya`
- `JWT_SECRET=dev-secret-min-32-characters-long`
- `TELEGRAM_BOT_TOKEN=<from @BotFather>`
- `ML_SERVICE_URL=http://localhost:8000` (опционально)
- `FILE_STORAGE_PATH=./storage`

Liquibase migrate автоматически при старте Spring Boot.

## Constraints

- Dev profile: `spring.profiles.active=dev` (по умолчанию).
- HTTPS не требуется local.
- ML-сервис опционален — можно мокать.

## Future Evolution

- Devcontainer configuration.
- Hot reload с Spring DevTools.
- Local Telegram test bot setup guide.

## Related Documents

- `docs/adr/ADR-0002-java21.md`
- `docs/deployment/docker.md`
- `docs/deployment/configuration-management.md`
- `docs/onboarding/getting-started.md`
