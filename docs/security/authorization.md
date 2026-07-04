# Security: Authorization

## Purpose

Документ описывает авторизацию: кто имеет доступ к группам, мероприятиям, чекам, долгам и платежам.

## Context

После аутентификации backend должен проверять, что пользователь является участником соответствующего контекста перед выполнением операции.

## Responsibilities

- Определить правила доступа к ресурсам.
- Зафиксировать роль `owner` в MVP.
- Описать проверки на уровне application service.
- Дать основу для authorization tests.

## Non Responsibilities

- Документ не описывает Spring Security configuration детально.
- Документ не реализует RBAC framework.

## Design Decisions

Правила доступа MVP:

| Ресурс | Кто имеет доступ |
| --- | --- |
| Группа | Только участники группы |
| Создание standalone-группы | Любой аутентифицированный пользователь |
| Редактирование группы | Владелец (`owner`) |
| Удаление группы | Владелец |
| Добавление участника (standalone) | Владелец |
| Мероприятие | Участники группы |
| Позиции, чек | Участники мероприятия |
| Выбор позиций | Участники мероприятия (только свои) |
| Долги | Участники мероприятия (должник или плательщик) |
| Платеж | Должник (`перевел`) или плательщик (`получил`) |
| Профиль | Только владелец профиля |
| Файл чека | Участники мероприятия, к которому привязан чек |

Проверка выполняется в application service каждого модуля, не в controller.

## Constraints

- Нет доступа к чужим группам/мероприятиям.
- Authorization error → HTTP 403.
- Роль `owner` — единственная MVP-роль с расширенными правами.

## Future Evolution

- Роль `admin` в группе.
- Custom permissions.
- Policy-based authorization (Spring Security `@PreAuthorize`).

## Related Documents

- `docs/business/business-rules.md`
- `docs/modules/groups.md`
- `docs/modules/events.md`
- `docs/security/authentication.md`
