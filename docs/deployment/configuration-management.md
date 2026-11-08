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
| OpenRouter API key | `OPENROUTER_API_KEY` | — | Ключ распознавания чека. Пустой ключ оставляет заглушку |
| OpenRouter model | `OPENROUTER_RECEIPT_MODEL` | qwen/qwen3.8-flash | Vision-модель |
| Subscription enforced | `SUBSCRIPTION_ENFORCED` | true | В test выключено, в dev и prod включено |
| Subscription price | `SUBSCRIPTION_PRICE_KOPECKS` | 4900 | 49 ₽ за 30 дней |
| YooKassa shop | `YOOKASSA_SHOP_ID` | — | Магазин для карты и СБП |
| YooKassa secret | `YOOKASSA_SECRET_KEY` | — | Секрет магазина |
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
