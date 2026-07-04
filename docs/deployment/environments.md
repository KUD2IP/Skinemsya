# Deployment: Environments

## Purpose

Документ описывает окружения проекта: local, test/stage, prod и их отличия.

## Context

Каждое окружение имеет свою конфигурацию, БД, секреты и интеграции.

## Responsibilities

- Определить окружения и их назначение.
- Зафиксировать отличия конфигурации.
- Описать доступ к секретам по окружениям.

## Non Responsibilities

- Документ не настраивает инфраструктуру.
- Документ не описывает CI/CD pipeline детально.

## Design Decisions

| Окружение | Назначение | БД | Интеграции |
| --- | --- | --- | --- |
| dev | Разработка на машине разработчика | Docker PostgreSQL | Mock ML, test bot |
| test/stage | Pre-production testing | Managed PostgreSQL | Staging ML, test bot |
| prod | Production | Managed PostgreSQL (HA) | Production ML, prod bot |

Отличия:
- **dev**: `spring.profiles.active=dev`, debug logging, `.env` из корня репозитория, no HTTPS.
- **test/stage**: `spring.profiles.active=staging`, info logging, HTTPS, Testcontainers в CI.
- **prod**: `spring.profiles.active=prod`, warn logging, HTTPS, monitoring.

Секреты:
- dev: `.env` файл в корне репозитория.
- test/stage: CI/CD secrets или cloud secrets manager.
- prod: cloud secrets manager, rotation policy.

## Constraints

- Нет shared БД между окружениями.
- Prod secrets не доступны в dev.
- Test/stage использует отдельный Telegram bot.

## Future Evolution

- Preview environments per PR.
- Blue-green deployment.
- Multi-region prod.

## Related Documents

- `docs/deployment/configuration-management.md`
- `docs/security/secrets-management.md`
- `docs/deployment/ci-cd.md`
