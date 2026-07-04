# Security: Secrets Management

## Purpose

Документ описывает управление секретами: Telegram bot token, JWT secret, параметры БД и интеграций.

## Context

Секреты не должны попадать в репозиторий. Каждое окружение имеет свой набор секретов.

## Responsibilities

- Перечислить все секреты проекта.
- Определить способ хранения по окружениям.
- Запретить hardcode секретов.
- Описать ротацию секретов.

## Non Responsibilities

- Документ не настраивает Vault/K8s secrets.
- Документ не описывает CI/CD pipeline детально.

## Design Decisions

Секреты MVP:

| Секрет | Env var | Окружения |
| --- | --- | --- |
| Telegram bot token | `TELEGRAM_BOT_TOKEN` | local, test, prod |
| JWT secret | `JWT_SECRET` | local, test, prod |
| DB password | `DB_PASSWORD` | local, test, prod |
| DB URL | `DB_URL` | local, test, prod |
| ML service URL | `ML_SERVICE_URL` | local, test, prod |
| File storage path | `FILE_STORAGE_PATH` | local, test, prod |
| SBP credentials | `SBP_*` | prod only (post-MVP) |

Local: `.env` файл (в `.gitignore`), пример в `.env.example`.
Test/Prod: env vars в CI/CD или secrets manager.

Ротация:
- JWT secret — при компрометации; все refresh tokens revoke.
- Bot token — через @BotFather.
- DB password — по политике инфраструктуры.

## Constraints

- Секреты не логируются.
- Секреты не в application.yml в репозитории.
- `.env.example` содержит только имена переменных, не значения.

## Future Evolution

- HashiCorp Vault или cloud secrets manager.
- Automatic secret rotation.
- Separate secrets per integration.

## Related Documents

- `docs/deployment/environments.md`
- `docs/deployment/configuration-management.md`
- `docs/security/jwt.md`
- `docs/integrations/telegram.md`
