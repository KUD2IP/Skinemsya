# Implementation Report Template

Используйте этот шаблон для отчёта о завершении каждого этапа MVP. Скопируйте файл как `phase-{N}-{name}.md` и заполните все разделы.

---

## Metadata

| Поле | Значение |
| --- | --- |
| Этап | {N} — {Name} |
| Дата | {YYYY-MM-DD} |
| Статус | completed / partial / blocked |
| Агент / автор | {name} |

---

## Прочитанные документы

### Базовые (обязательные)

- [ ] `docs/ai/context-guide.md`
- [ ] `docs/documentation-catalog.md`
- [ ] `docs/business/mvp-scope.md`
- [ ] `docs/architecture/system-overview.md`
- [ ] `docs/architecture/backend-architecture.md`
- [ ] `docs/architecture/module-dependencies.md`
- [ ] `docs/roadmap/mvp-roadmap.md`

### Документы этапа

- [ ] {path/to/doc1.md}
- [ ] {path/to/doc2.md}
- [ ] ...

---

## Scope

### In scope (реализовано)

- ...

### Out of scope (сознательно не делалось)

- ...

---

## Изменённые файлы

| Файл | Действие | Описание |
| --- | --- | --- |
| `path/to/file` | added / modified / deleted / renamed | краткое описание |

---

## Архитектурные решения

### {Решение 1}

**Контекст:** ...

**Решение:** ...

**Обоснование (docs):** ссылка на `docs/...`

### {Решение 2}

...

---

## Тесты

### Добавленные тест-классы

| Класс | Модуль | Тип | Что проверяет |
| --- | --- | --- | --- |
| `MoneyTest` | common | unit | ... |

### Результаты

```text
{вывод mvn test / mvn clean install}
```

---

## Проверки

| Команда | Результат |
| --- | --- |
| `mvn clean install` | PASS / FAIL |
| `docker compose up -d` | PASS / FAIL / N/A |
| `mvn spring-boot:run ...` | PASS / FAIL / N/A |
| `curl http://localhost:8080/actuator/health` | PASS / FAIL / N/A |

---

## Known gaps / Tech debt

- ...

---

## Критерий завершения этапа

**Из roadmap:** {цитата критерия из mvp-roadmap.md}

**Статус:** выполнен / не выполнен

**Комментарий:** ...

---

## Следующий этап

**Этап {N+1} — {Name}** — не начинать без подтверждения пользователя.

Ожидаемый scope: ...

---

## Stop

Этап завершён. Ожидаю подтверждения перед переходом к Этапу {N+1}.
