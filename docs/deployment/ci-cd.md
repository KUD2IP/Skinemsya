# Deployment: CI/CD

## Purpose

Документ описывает CI/CD pipeline: сборка, тесты, миграции, Docker image и деплой MVP.

## Context

MVP требует минимальный, но надежный CI/CD pipeline без enterprise-сложности.

## Responsibilities

- Описать этапы CI/CD.
- Зафиксировать quality gates.
- Определить артефакты pipeline.

## Non Responsibilities

- Документ не настраивает GitHub Actions / GitLab CI.
- Документ не описывает production infrastructure.

## Design Decisions

Pipeline stages:

1. **Build**: `mvn clean compile -B`
2. **Unit tests**: `mvn test -B`
3. **Integration tests**: `mvn verify -B` (Testcontainers, requires Docker)
4. **Package**: `mvn package -B -DskipTests` → JAR artifact
5. **Docker image**: build and push `skinemsya-backend:$TAG`
6. **Deploy staging**: deploy to staging environment
7. **Deploy prod**: manual approval → deploy to prod

Quality gates:
- Build success.
- All tests pass.
- No critical dependency vulnerabilities (optional on MVP).
- Liquibase migrate succeeds on Testcontainers.

Артефакты:
- JAR: `app/target/app-*.jar`
- Docker image: `skinemsya-backend:$GIT_SHA`
- Test reports: Surefire + Failsafe XML.

Deploy MVP:
- Single instance backend + managed PostgreSQL.
- Rolling restart (no blue-green на MVP).
- Liquibase migrate on startup.

## Constraints

- Pipeline < 15 минут на MVP.
- Docker required for integration tests stage.
- Prod deploy — manual trigger.

## Future Evolution

- Automated staging deploy on merge to main.
- Blue-green / canary deployment.
- Database migration as separate step.
- Slack/Telegram notifications on deploy.

## Related Documents

- `docs/testing/testing-strategy.md`
- `docs/adr/ADR-0004-liquibase.md`
- `docs/deployment/docker.md`
- `docs/deployment/environments.md`
