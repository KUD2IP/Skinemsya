# Deployment: CI/CD

## Purpose

Документ описывает production-ready CI/CD: разделённые pipeline для backend и frontend, GHCR, Git Flow и деплой на единственный production VPS.

## Architecture

CI и CD разделены. Каждый workflow выполняет одну задачу.

```text
feature/* ──PR──► develop ──► release/X.Y.Z ──PR──► master ──► production deploy
hotfix/*  ──PR──► master + develop
```

| Workflow | Repo | Trigger | Действие |
| --- | --- | --- | --- |
| `backend-ci.yml` | Skinemsya | PR → develop/master/release/hotfix | Maven verify, quality, tests |
| `backend-release.yml` | Skinemsya | push `release/*` | Verify + Docker push RC + draft release |
| `backend-production.yml` | Skinemsya | push `master` | Verify + Docker push + deploy + GitHub Release |
| `backend-hotfix.yml` | Skinemsya | PR/push `hotfix/*` | Strict CI + hotfix image; emergency deploy via dispatch |
| `codeql.yml` | Skinemsya | PR/push + weekly | SAST |
| `dependency-review.yml` | Skinemsya | PR | Dependency diff |
| `frontend-ci.yml` | Skinemsya_ui | PR | typecheck + build |
| `frontend-release.yml` | Skinemsya_ui | push `release/*` | RC artifact |
| `frontend-production.yml` | Skinemsya_ui | push `master` | rsync static + smoke check |

Reusable workflows:

- `reusable-maven-ci.yml`
- `reusable-docker-build.yml`
- `reusable-deploy-backend.yml`

Composite actions:

- `.github/actions/setup-ssh`
- `.github/actions/prepare-deploy-env`
- `.github/actions/maven-verify`

## Docker images (GHCR)

Registry: `ghcr.io/<github-owner>/skinemsya-backend`

| Tag | Когда | Mutable |
| --- | --- | --- |
| `1.2.3` | release + production | No |
| `1.2.3-rc` | push `release/*` | No |
| `1.2` | production minor pointer | Yes |
| `sha-abc1234` | каждый build | No |
| `production` | последний prod deploy | Yes |

`latest` не используется.

Deploy на VPS:

```bash
docker compose -f docker-compose.prod.yml pull backend
docker compose -f docker-compose.prod.yml up -d --no-deps backend
```

## Quality gates (backend CI)

1. Compile
2. Spotless
3. Checkstyle
4. SpotBugs
5. Unit tests
6. Integration tests (Testcontainers)
7. Package

## Production deploy

1. Сохранить текущий `APP_VERSION` в `deploy/.previous-version`
2. Rsync `deploy/` на VPS
3. `docker compose pull backend` + `up -d --no-deps backend`
4. Health check: `https://skinemsya-vse.ru/actuator/health` (30×10s)
5. При fail — rollback на `.previous-version`
6. `docker image prune -f`

## GitHub Environment

| Environment | Назначение | Approval |
| --- | --- | --- |
| `production` | Единственный deploy target | 1 reviewer (рекомендуется) |

Staging environment **не используется** (один VPS = production).

## Secrets migration

Переименуй secrets в GitHub → Environment **production**:

| Было (staging) | Стало (production) |
| --- | --- |
| `STAGING_ENV` / `STAGING_ENV_B64` | `PRODUCTION_ENV` (multiline) |
| `STAGING_HOST` | `PRODUCTION_HOST` |
| `STAGING_USER` | `PRODUCTION_USER` |
| `STAGING_SSH_PORT` | `PRODUCTION_SSH_PORT` (optional) |
| `SSH_PRIVATE_KEY` | `SSH_PRIVATE_KEY` (environment) |
| `SSH_KNOWN_HOSTS` | `SSH_KNOWN_HOSTS` (optional) |

### VPS: GHCR pull (один раз)

На сервере под пользователем `deploy`:

```bash
echo '<PAT read:packages>' | docker login ghcr.io -u <github-user> --password-stdin
```

Создай PAT с scope `read:packages` или используй fine-grained token.

В `deploy/.env` должны быть:

```env
GHCR_OWNER=your-github-owner-lowercase
APP_VERSION=1.0.0
```

`APP_VERSION` и `GHCR_OWNER` дописываются автоматически в CI.

### GHCR package visibility

После первого push сделай пакет `skinemsya-backend` **public** (Settings → Package settings)  
или оставь private и используй `docker login` на VPS.

## Branch protection (рекомендуется)

Required checks для PR:

- Backend: `Verify` (backend-ci), CodeQL, Dependency Review
- Frontend: `Typecheck and build` (frontend-ci)

Deploy branches:

- `master` — только через merge `release/*` или `hotfix/*`

## Rollback (manual)

На VPS:

```bash
cd /opt/skinemsya/deploy
prev=$(cat .previous-version)
sed -i "s/^APP_VERSION=.*/APP_VERSION=${prev}/" .env
APP_VERSION="${prev}" docker compose -f docker-compose.prod.yml pull backend
APP_VERSION="${prev}" docker compose -f docker-compose.prod.yml up -d --no-deps backend
```

## Related Documents

- `docs/deployment/staging-server.md` — настройка VPS (production)
- `docs/deployment/reverse-proxy-and-storage.md`
- `docs/testing/testing-strategy.md`
