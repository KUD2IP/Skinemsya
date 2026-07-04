# Deployment: Configuration Management

## Purpose

Документ описывает управление конфигурацией: Spring profiles, env vars и настройки компонентов.

## Context

Backend конфигурируется через Spring profiles и environment variables. Секреты не хранятся в application.yml.

## Responsibilities

- Описать структуру конфигурации.
- Перечислить все конфигурируемые параметры.
- Зафиксировать Spring profiles.

## Non Responsibilities

- Документ не создает application.yml.
- Документ не настраивает secrets manager.

## Design Decisions

Spring profiles:
- `dev` — разработка.
- `staging` — test/stage.
- `prod` — production.

Файлы:
- `application.yml` — defaults (no secrets), `spring.config.import` для `.env`.
- `application-dev.yml` — dev overrides (logging, actuator).
- `application-staging.yml` — staging overrides.
- `application-prod.yml` — prod overrides.

Загрузка `.env`:
- Файл `.env` в корне репозитория (копия из `.env.example`).
- Spring Boot импортирует его через `spring.config.import: optional:file:.env[.properties]`.
- Значения из `.env` подставляются в `${DB_URL}`, `${DB_PASSWORD}` и другие placeholders в `application.yml`.
- Профиль по умолчанию: `dev` (переопределяется через `SPRING_PROFILES_ACTIVE`).

Конфигурируемые параметры:

| Параметр | Env var | Default | Описание |
| --- | --- | --- | --- |
| DB URL | `DB_URL` | — | JDBC connection string |
| DB password | `DB_PASSWORD` | — | PostgreSQL password |
| JWT secret | `JWT_SECRET` | — | HS256 signing key |
| JWT access TTL | `JWT_ACCESS_TTL` | 15m | Access token lifetime |
| JWT refresh TTL | `JWT_REFRESH_TTL` | 7d | Refresh token lifetime |
| Telegram bot token | `TELEGRAM_BOT_TOKEN` | — | Bot API token |
| ML service URL | `ML_SERVICE_URL` | — | Python ML endpoint |
| ML service timeout | `ML_SERVICE_TIMEOUT` | 30s | HTTP timeout |
| File storage path | `FILE_STORAGE_PATH` | ./storage | Local file storage |
| Server port | `SERVER_PORT` | 8080 | HTTP port |

## Constraints

- Секреты только через env vars.
- Defaults безопасны для local.
- Prod config validated at startup (fail fast if missing secrets).

## Future Evolution

- Spring Cloud Config.
- Feature flags service.
- Dynamic config reload.

## Related Documents

- `docs/security/secrets-management.md`
- `docs/modules/app.md`
- `docs/deployment/environments.md`
- `docs/deployment/local-development.md`
