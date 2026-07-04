# ADR-0004: Liquibase

## Status

Accepted

## Purpose

Зафиксировать Liquibase как инструмент управления схемой PostgreSQL.

## Context

Backend `skinemsya` хранит транзакционные данные в PostgreSQL. Изменения схемы должны быть повторяемыми, проверяемыми в CI и одинаково применимыми локально, в тестовом окружении и production. Ручное изменение БД не подходит, потому что быстро приводит к расхождению окружений.

## Responsibilities

- Зафиксировать способ изменения схемы БД.
- Обеспечить воспроизводимость миграций.
- Поддержать Testcontainers и CI.
- Задать основу для naming conventions.

## Non Responsibilities

- ADR не создает SQL-миграции.
- ADR не определяет каталог таблиц.
- ADR не описывает все правила именования.
- ADR не заменяет database-документацию.

## Design Decisions

Liquibase используется для всех изменений схемы. Каждое изменение БД должно быть описано changeset и применяться автоматически при запуске или в CI/CD-процессе, в зависимости от будущего deployment-подхода.

Liquibase выбран потому, что он хорошо интегрируется со Spring Boot, поддерживает контроль порядка изменений и позволяет проверить миграции на PostgreSQL в интеграционных тестах.

## Constraints

- Нельзя менять production schema вручную.
- Нельзя изменять уже примененный changeset без явной процедуры исправления.
- Все таблицы, индексы и constraints должны создаваться через миграции.
- Имена объектов БД должны соответствовать `database/naming-conventions.md`.
- Миграции должны быть совместимы с PostgreSQL.

## Future Evolution

После MVP можно добавить строгие проверки миграций в CI, отдельный dry-run этап, rollback-процедуры и миграционный pipeline перед деплоем. Эти механизмы должны вводиться постепенно, когда появится production-эксплуатация.

## Related Documents

- `docs/database/naming-conventions.md`
- `docs/database/table-catalog.md`
- `docs/deployment/ci-cd.md`
- `docs/testing/testcontainers.md`
- `docs/adr/ADR-0003-postgresql.md`

