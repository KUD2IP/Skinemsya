# Deployment: CI/CD

## Purpose

Документ описывает CI/CD pipeline: сборка, тесты, Docker image и деплой на staging.

## Context

MVP использует GitHub Actions без enterprise-сложности.

## Responsibilities

- Описать этапы CI/CD.
- Зафиксировать quality gates.
- Определить артефакты pipeline.

## Non Responsibilities

- Документ не описывает production infrastructure за пределами staging VPS.
- Документ не настраивает фронтенд-репозиторий.

## Design Decisions

Workflow: [`.github/workflows/backend.yml`](../../.github/workflows/backend.yml)

Pipeline stages:

1. **Unit tests** (`test-unit`): `mvn test` без integration-тестов — на каждый push в `main`/`master`.
2. **Integration tests** (`test-integration`): `mvn verify` — опционально, вручную через workflow_dispatch.
3. **Docker image** (`build-image`): `docker build` → artifact `skinemsya-backend.tar.gz`.
4. **Deploy staging** (`deploy-staging`): SSH + rsync + `docker compose up` — вручную через workflow_dispatch.

Quality gates:
- Build success.
- Unit tests pass.
- Liquibase migrate succeeds locally / в integration job.

Артефакты:
- Docker image tarball (retention 3 days).
- JAR внутри образа: `skinemsya_parent/app/target/app-*.jar`.

Deploy staging:
- Single instance backend + PostgreSQL + MinIO на VPS.
- Rolling restart через `docker compose up -d`.
- Liquibase migrate on startup.
- Секреты из GitHub Secrets → `deploy/.env` на сервере.

GitHub Secrets (backend):

| Secret | Назначение |
| --- | --- |
| `STAGING_ENV` или `STAGING_ENV_B64` | Содержимое production `.env` |
| `SSH_PRIVATE_KEY` | SSH-ключ для пользователя `deploy` |
| `STAGING_HOST` | IP/домен VPS |
| `STAGING_USER` | SSH-пользователь (`deploy`) |
| `STAGING_SSH_PORT` | SSH-порт (опционально) |
| `SSH_KNOWN_HOSTS` | Fingerprint хоста (опционально) |

## Constraints

- Pipeline < 15 минут на MVP.
- Docker required для сборки образа (GitHub-hosted runners).
- Staging deploy — manual trigger (workflow_dispatch).

## Future Evolution

- Push image to GHCR вместо tarball artifact.
- Automated staging deploy on merge to main.
- Blue-green / canary deployment.
- Database migration as separate step.

## Related Documents

- `docs/deployment/staging-server.md`
- `docs/testing/testing-strategy.md`
- `docs/adr/ADR-0004-liquibase.md`
- `docs/deployment/docker.md`
- `docs/deployment/environments.md`
