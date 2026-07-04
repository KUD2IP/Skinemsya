# ADR-0002: Java 21

## Status

Accepted

## Purpose

Зафиксировать Java 21 как основную версию Java для backend `skinemsya`.

## Context

Backend строится на Spring Boot 3.x. Проекту нужна современная LTS-версия Java, которая поддерживается экосистемой Spring, Maven, Testcontainers, MapStruct и инструментами CI/CD. Для MVP важно не экспериментировать с нестабильными версиями runtime.

## Responsibilities

- Зафиксировать runtime baseline.
- Обеспечить совместимость со Spring Boot 3.x.
- Дать разработчикам и DevOps единое требование к окружению.
- Упростить локальный запуск и CI.

## Non Responsibilities

- ADR не описывает настройку JDK в IDE.
- ADR не фиксирует конкретный vendor JDK.
- ADR не определяет coding style.
- ADR не требует использования всех новых возможностей Java 21.

## Design Decisions

Java 21 выбрана как LTS-версия. Она совместима с современным Spring Boot 3.x и дает стабильную платформу для backend-разработки.

Проект должен использовать Java 21 как compile и runtime target. Это предотвращает ситуацию, когда часть команды собирает проект на другой версии и получает несовместимое поведение.

Использование новых возможностей Java допускается только если оно улучшает читаемость и не усложняет поддержку. MVP не должен становиться площадкой для демонстрации языка.

## Constraints

- Все разработчики и CI должны использовать Java 21.
- Нельзя добавлять зависимости, требующие более новой версии Java без отдельного ADR.
- Нельзя использовать preview features.
- Код должен оставаться понятным для команды, знакомой с обычной Java backend-разработкой.

## Future Evolution

Переход на более новую LTS-версию возможен после стабилизации MVP и проверки совместимости Spring Boot, Testcontainers, MapStruct, Liquibase и CI/CD. Такой переход должен оформляться отдельным ADR.

## Related Documents

- `docs/deployment/local-development.md`
- `docs/deployment/ci-cd.md`
- `docs/testing/testing-strategy.md`
- `docs/ai/coding-standards.md`

