# AI: Development Workflow

## Purpose

Workflow для AI-агентов: как анализировать задачу, какие документы читать, как проверять изменения.

## Context

AI-агент должен следовать структурированному процессу, чтобы изменения были корректными и минимальными.

## Responsibilities

- Описать шаги AI-агента при получении задачи.
- Определить, какие документы читать для типовых задач.
- Зафиксировать checklist перед завершением.

## Non Responsibilities

- Документ не описывает human developer workflow (см. `onboarding/development-workflow.md`).

## Design Decisions

Шаги AI-агента:

1. **Прочитать задачу** — понять scope, не расширять без запроса.
2. **Найти docs** — `documentation-catalog.md` → релевантные модули, business rules, ADR.
3. **Проверить MVP scope** — функция в mvp-scope? Если нет — сообщить пользователю.
4. **Проверить module boundaries** — какой модуль владеет изменением?
5. **Спланировать** — минимальный diff, без over-engineering.
6. **Реализовать** — следовать `coding-standards.md`, `module-rules.md`.
7. **Тестировать** — unit-тесты для бизнес-логики.
8. **Обновить docs** — если изменились контракты или правила.
9. **Проверить** — checklist ниже.

## Detailed Workflow

### 1. Task Classification

Перед чтением кода агент классифицирует задачу:

| Класс задачи | Пример | Основной риск |
| --- | --- | --- |
| Business feature | Создать standalone-группу | Нарушить MVP scope или бизнес-правила |
| Architecture change | Добавить зависимость между модулями | Создать циклическую или скрытую связанность |
| Integration change | Подключить ML endpoint | Смешать бизнес-логику и внешний API |
| Database change | Добавить таблицу debts | Нарушить владение таблицами |
| Security change | Проверка доступа к группе | Открыть доступ к чужим данным |
| Test-only change | Добавить fixtures | Закрепить неправильное поведение |
| Documentation-only change | Уточнить бизнес-правило | Внести противоречие между docs |

Если задача относится к нескольким классам, агент обязан прочитать документы для каждого класса.

### 2. Context Acquisition

Агент читает документы слоями, от общего к конкретному:

1. `docs/documentation-catalog.md` — найти релевантные документы.
2. `docs/business/mvp-scope.md` — проверить, входит ли задача в MVP.
3. `docs/business/business-rules.md` — найти применимые правила.
4. `docs/architecture/backend-architecture.md` и `docs/architecture/module-dependencies.md` — понять границы.
5. `docs/modules/{module}.md` — понять владельца логики.
6. `docs/database/*`, `docs/security/*`, `docs/integrations/*` — если задача затрагивает эти области.

AI-агент не должен начинать реализацию, пока не определены:

- модуль-владелец изменения;
- входные данные use case;
- состояние до операции;
- состояние после операции;
- ошибки и ограничения доступа;
- тестовый уровень (unit, integration, contract).

### 3. Scope Validation

Для каждой задачи агент отвечает на вопросы:

- Это нужно для основного MVP-сценария?
- Это уже описано в `mvp-scope.md`?
- Это не входит в список исключений MVP?
- Это не требует Kafka, микросервисов, СБП или сложной ролей?
- Можно ли реализовать проще без потери бизнес-смысла?

Если ответ неочевиден, агент не реализует молча. Он формулирует короткий вопрос пользователю или предлагает обновить documentation/ADR.

### 4. Ownership Decision

Правило владельца:

- `auth` владеет аутентификацией и токенами.
- `users` владеет профилем и Telegram identity.
- `groups` владеет группами и членством.
- `events` владеет мероприятием, плательщиком и жизненным циклом мероприятия.
- `receipts` владеет позициями, выбором позиций и результатом чека.
- `debts` владеет расчетом долга и статусом долга.
- `payments` владеет платежной операцией и подтверждениями.
- `files` владеет uploaded files и storage metadata.
- `notifications` владеет уведомлениями.
- `integrations` владеет внешними API adapters.
- `common` владеет только доменно-нейтральными primitives.
- `app` владеет wiring, configuration и запуском.

Если изменение кажется «общим», агент должен доказать, что оно не принадлежит конкретному модулю. По умолчанию доменная логика не попадает в `common`.

### 5. Implementation Planning

Минимальный план перед кодом:

1. Какие документы подтверждают поведение.
2. Какой модуль меняется.
3. Какие public contracts меняются.
4. Какие таблицы или миграции нужны.
5. Какие тесты докажут поведение.
6. Какие docs нужно обновить после изменения.

Для documentation-only задач вместо кода агент должен проверять:

- все required sections есть;
- текст отвечает на `что`, `зачем`, `почему`, `ограничения`, `будущее`;
- нет общих фраз без проектного смысла;
- есть edge cases и explicit non-responsibilities;
- связанные документы указаны корректно.

Какие docs читать:

| Задача | Документы |
| --- | --- |
| Новый endpoint | modules/{module}.md, security/authorization.md, architecture/request-flow.md |
| Бизнес-правило | business/business-rules.md, business/domain-model.md |
| Новая таблица | database/table-catalog.md, database/naming-conventions.md, modules/{module}.md |
| Интеграция | integrations/{service}.md, modules/integrations/integrations.md |
| Платежи | architecture/payment-flow.md, modules/payments.md, modules/debts.md |
| Группы | modules/groups.md, business/business-rules.md |
| Тесты | testing/testing-strategy.md, testing/test-data.md |

## AI Output Rules

Когда агент завершает задачу, ответ должен содержать:

- что изменено на уровне поведения или документации;
- какие файлы были ключевыми;
- что не выполнялось (например, тесты не запускались);
- оставшиеся риски, если они есть.

Для code review агент должен сначала перечислять риски и баги, затем краткое резюме. Для implementation task — сначала результат, затем проверки.

## Common AI Mistakes To Avoid

| Ошибка | Почему опасно | Правильное действие |
| --- | --- | --- |
| Использовать текущий Maven-скелет как финальную архитектуру | Скелет может быть устаревшим | Свериться с `documentation-catalog.md` и `backend-architecture.md` |
| Добавить Kafka для уведомлений | Kafka запрещена на MVP | Использовать in-process events или прямой вызов |
| Положить payment logic в debts | Смешивает расчет и закрытие долга | `debts` считает, `payments` закрывает через подтверждения |
| Сделать чек обязательным | Ручные позиции — базовый сценарий | Чек опционален |
| Считать Telegram username идентификатором | Username меняется | Использовать Telegram user id + backend user id |
| Хранить реквизиты на мероприятии | Решение MVP: профиль пользователя | Читать `business-rules.md` и `modules/users.md` |
| Закрыть долг после `перевел` | Нужен `получил` от плательщика | Следовать `payment-flow.md` |
| Прочитать чужую таблицу напрямую | Нарушает module ownership | Использовать public contract модуля |

Checklist перед завершением:
- [ ] Изменения в рамках одного модуля (или согласованы cross-module contracts).
- [ ] Нет forbidden patterns.
- [ ] Unit-тесты для бизнес-логики.
- [ ] Docs обновлены если нужно.
- [ ] Минимальный diff.

## Constraints

- Не создавать код без чтения docs.
- Не расширять scope задачи.
- Не коммитить без запроса.

## Future Evolution

- Automated doc cross-reference validation.
- AI task templates per module.

## Related Documents

- `docs/onboarding/development-workflow.md`
- `docs/testing/testing-strategy.md`
- `docs/ai/project-rules.md`
- `docs/ai/coding-standards.md`
