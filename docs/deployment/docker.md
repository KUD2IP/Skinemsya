# Deployment: Docker

## Purpose

Документ описывает Docker-подход для контейнеризации backend и PostgreSQL. Python ML-сервис в деплой не входит: распознавание чека идёт через OpenRouter, см. `docs/adr/ADR-0011-openrouter-receipt-vision.md`.

## Context

Docker обеспечивает воспроизводимое окружение для local development и CI/CD.

## Responsibilities

- Описать Docker Compose конфигурацию.
- Зафиксировать Dockerfile для backend.
- Определить сервисы и их связи.

## Non Responsibilities

- Документ не описывает Kubernetes.
- Документ не описывает GitHub Actions (см. `docs/deployment/ci-cd.md`).

## Design Decisions

Docker Compose services:

| Service | Image | Port | Описание |
| --- | --- | --- | --- |
| postgres | postgres:16-alpine | 5432 | Основная БД |
| backend | skinemsya-backend:latest | 8080 | Spring Boot app |

`docker-compose.yml`:
- `postgres` с volume для data persistence.
- `backend` depends_on postgres, env vars из `.env`.

Backend Dockerfile:
- Multi-stage: Maven build → JRE 21 slim runtime.
- Non-root user.
- Health check: `GET /actuator/health`.

Volumes:
- `postgres_data` — БД.
- `file_storage` — uploaded files.

## Constraints

- Backend image < 300 MB.
- PostgreSQL data persisted in volume.
- Secrets через env vars, не в image.

## Future Evolution

- Docker Compose profiles для staging.
- Multi-arch images (arm64/amd64).

## Related Documents

- `docs/deployment/local-development.md`
- `docs/deployment/environments.md`
- `docs/adr/ADR-0011-openrouter-receipt-vision.md`
