# Deployment: Docker

## Purpose

Документ описывает Docker-подход для контейнеризации backend, PostgreSQL и ML-сервиса.

## Context

Docker обеспечивает воспроизводимое окружение для local development и CI/CD.

## Responsibilities

- Описать Docker Compose конфигурацию.
- Зафиксировать Dockerfile для backend.
- Определить сервисы и их связи.

## Non Responsibilities

- Документ не описывает Kubernetes.
- Документ не создает Dockerfile (только описывает целевое состояние).

## Design Decisions

Docker Compose services:

| Service | Image | Port | Описание |
| --- | --- | --- | --- |
| postgres | postgres:16-alpine | 5432 | Основная БД |
| backend | skinemsya-backend:latest | 8080 | Spring Boot app |
| ml-service | skinemsya-ml:latest | 8000 | Python ML/OCR (опционально local) |

`docker-compose.yml` (целевое состояние):
- `postgres` с volume для data persistence.
- `backend` depends_on postgres, env vars из `.env`.
- `ml-service` — profile `ml` (опциональный запуск).

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
- ML-service containerization.
- Multi-arch images (arm64/amd64).

## Related Documents

- `docs/deployment/local-development.md`
- `docs/deployment/environments.md`
- `docs/integrations/ml-service.md`
