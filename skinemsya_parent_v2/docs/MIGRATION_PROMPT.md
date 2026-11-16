# Промт для агента: перенос backend Skinemsya на новую архитектуру

## Контекст

Мы переносим backend-проект на новую модульную структуру по образцу референса `/Users/ivanstorozhev/Downloads/loyalty-cards/`.

- Старый код: `/Users/ivanstorozhev/IdeaProjects/Skinemsya/skinemsya_parent/` (модули `app`, `auth`, `common`, `debts`, `events`, `files`, `groups`, `integrations`, `notifications`, `payments` и др.).
- Новый код: `/Users/ivanstorozhev/IdeaProjects/Skinemsya/skinemsya_parent_v2/` — отдельный Maven-реактор, НЕ подключён к корневому `pom.xml`.
- Уже перенесены: `users`, `auth`, `integrations` (Telegram + ML), `groups`. Полный статус ищи по структуре каталогов v2.

## Железные правила

1. **Старый код не трогать.** Ничего не удалять и не изменять в `skinemsya_parent/`, корневом `pom.xml`, `checkstyle.xml`, `spotbugs-exclude.xml`. Все изменения — только в `skinemsya_parent_v2/`. В конце работы проверяй: `git status --short` должен показывать только `?? skinemsya_parent_v2/`, а `git diff --name-only` по коду — пустой (изменения `.idea/*` делает IDE, их не трогаем).
2. **Не подключать v2 в корневой pom** — пользователь скажет явно, когда переключаться.
3. **Не ослаблять функциональность.** Все бизнес-правила, транзакционные границы (`@Transactional`, `Propagation`), нативные upsert-запросы, `@SQLRestriction`, конкурентные сценарии переносятся один в один.
4. **Переносим по одному вертикальному модулю** (домен целиком: контракты → данные → REST → реализация → тесты), после каждого — зелёная сборка.
5. Когда всё перенесено и проверено — старое удаляем (отдельным шагом, по команде пользователя).

## Архитектура v2

Реактор `skinemsya-parent` → модули:

```
skinemsya_parent_v2/
├── api
│   ├── internal   — POJO, enums, внутренние сервисные интерфейсы, порты репозиториев, события, DomainException/ErrorCode
│   ├── rest       — openapi/*.yml + генерация DTO (openapi-generator, только model, generateApis=false)
│   └── external   — контракты для внешних потребителей (пока пуст)
├── data           — JPA-сущности и Liquibase-миграции, больше ничего
└── impl           — приложение, реализации, REST-контроллеры, MapStruct, конфигурация
```

Зависимости: `impl` → `data`, `api/rest`, `api/internal`; `data` → `api/internal` (только ради enums). Обратных зависимостей нет.

### Имена и пакеты

- Артефакты полные: `skinemsya-api-internal`, `skinemsya-api-rest`, `skinemsya-data`, `skinemsya-impl`.
- Базовые пакеты: `skinemsya.vse.ru.api.internal`, `skinemsya.vse.ru.api.rest`, `skinemsya.vse.ru.data`, `skinemsya.vse.ru.impl`.
- Классы раскладываем по домену: `pojo/groups`, `service/groups`, `repository/groups`, `entity/groups`, `impl/service/groups`, `impl/repository/groups`. НЕ создавать сквозных папок типа `repository/jpa` — JPA-репозитории лежат под доменом рядом с адаптерами (`impl/repository/groups/GroupJpaRepository.java` + `GroupRepositoryImpl.java`).

### Где что лежит (смотри на уже перенесённые модули как на образец)

- **POJO** — `api/internal/pojo/<домен>`, Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor` (или `@Data @NoArgsConstructor @AllArgsConstructor`). Старые record'ы → Lombok-классы с getter'ами, accessor'ы `x.foo()` меняем на `x.getFoo()` во всех переносимых вызовах.
- **Enums** — `api/internal/enums/<домен>`.
- **Сервисные интерфейсы** — `api/internal/service/<домен>`.
- **Порты репозиториев** — `api/internal/repository/<домен>`, работают ТОЛЬКО с POJO (не с сущностями). Нативные upsert/проекции остаются методами порта.
- **События Spring** — `api/internal/event/<домен>`.
- **Ошибки** — НЕ плодим классы-исключения. Используем `api/internal/ex/DomainException` + `enums/ErrorCode` (NOT_FOUND, AUTHORIZATION_ERROR, DOMAIN_RULE_VIOLATION, VALIDATION_ERROR, DOMAIN_CONFLICT, INTEGRATION_ERROR, …). Старые `*Exception extends *DomainException` заменяем на `new DomainException(code, message)` с тем же HTTP-статусом и смыслом.
- **Сущности** — `data/entity/<домен>`, вручную написанные getter/setter (как `UserEntity`), JPA-аннотации копируем из старого кода.
- **Миграции** — `data/src/main/resources/db/changelog/changes/`, включаем в `db.changelog-master.xml` в хронологическом порядке. Если старый changeset затрагивает ещё не перенесённые таблицы — вырезаем из него только часть переносимого домена в новый changeset с собственным id (пример: `20261108-002-groups-invite-code.xml`).
- **REST** — спецификация `api/rest/src/main/resources/openapi/<домен>-api.yml`, DTO генерируются в `api.rest.<домен>.dto`. Сгенерированные классы НЕ коммитим в исходники (только `target/generated-sources`). Поле добавляем в executions плагина в `api/rest/pom.xml` (скопировать существующий execution). Enum внутри DTO → вложенный `XxxEnum`, маппинг в маппере через `valueOf`.
- **Реализации** — `impl/service/<домен>`, Lombok `@RequiredArgsConstructor` (или явный конструктор, если нужны `Optional<...>` зависимости).
- **Конфигурации** — `@Configuration` в `impl/configuration`; `@ConfigurationProperties` в `impl/support/properties` (record с дефолтами в compact constructor).
- **Утилиты** — `impl/support/util`, Lombok `@UtilityClass` (НЕ писать private constructor — его создаёт Lombok; Javadoc ставить до аннотации). Утилит в `api/internal` быть не должно.
- **Мапперы** — `impl/mapper/entity` (сущность ↔ POJO) и `impl/mapper/rest` (POJO ↔ DTO), MapStruct `componentModel = "spring"`, Javadoc на методах. `Instant` ↔ `OffsetDateTime` — default-метод маппера (UTC). Осторожно с erasure: два default-метода с `PageResult<X>` разных типов не компилируются — давать разные имена (`toGroupPageResponse` / `toMemberPageResponse`).
- **Контроллеры** — `impl/rest`, тонкие: проверка аутентичности через `AuthenticatedUser`, вызов сервиса, маппинг. `@AuthenticationPrincipal AuthenticatedUser`, проверка `requireUserId` как в `UserController`.
- **Кросс-доменные зависимости** — только через внутренние порты/сервисы из `api/internal` (пример: auth зависит от `AuthGroupService`, который реализован в groups как `AuthGroupServiceImpl`). Если переносимый модуль зависит от ещё не перенесённого — выносим порт в `api/internal/service/<домен>` и помечаем в отчёте, что это заглушка до переноса того модуля.

### Тестовые фабрики (test-jar, БЕЗ отдельного модуля)

- Фабрики POJO — `api/internal/src/test/java/.../api/internal/pojo/factory/ServicePojoFactory.java`.
- Фабрики сущностей — `data/src/test/java/.../data/entity/factory/EntityFactory.java`.
- Фабрики REST DTO — `api/rest/src/test/java/.../api/rest/dto/factory/RestDtoFactory.java`.
- Все три модуля собирают `test-jar` (maven-jar-plugin), `impl` подключает их как test-зависимости с `<type>test-jar</type>`.
- Фабрики НЕ должны зависеть от `impl` (иначе цикл) — если нужен хеш и т.п., дублируем private helper внутри фабрики.
- При переносе модуля дополняем все три фабрики методами `createXxx()` для новых типов.

### Javadoc

Обязателен везде: классы, публичные контракты и их методы, REST-эндпоинты, фабричные методы, тестовые классы. Пишем по-русски, коротко, как в уже перенесённом коде.

## Порядок переноса одного модуля (чеклист)

1. Прочитать старый модуль целиком: domain, application, infrastructure, api, миграции, тесты. Выписать зависимости на другие домены.
2. `api/internal`: POJO, enums, порты репозиториев, сервисные интерфейсы, события. Порты для неперенесённых соседей — тоже здесь.
3. `data`: сущности + миграции + include в master changelog.
4. `api/rest`: `<домен>-api.yml` + execution в pom. DTO-формы должны совпадать со старым API (имена полей, валидации, alias'ы через `x-field-extra-annotation` при необходимости — см. `init_data` в auth-api.yml).
5. `impl`: JPA-репозитории, адаптеры портов, entity-маппер, сервисы, rest-маппер, контроллер, конфигурация/properties.
6. Фабрики во всех трёх test-jar.
7. Тесты: портировать все старые unit-тесты модуля (accessor'ы record → getter'ы), плюс тесты мапперов по образцу `GroupEntityMapperTest`/`GroupRestMapperTest`. Тестовые классы — с Javadoc.
8. Сборка: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q -f skinemsya_parent_v2/pom.xml clean verify` — должна быть зелёной (тесты + Checkstyle + SpotBugs). Всегда с `JAVA_HOME` Java 21, иначе SpotBugs падает.
9. Проверка `git status`/`git diff` — старое не тронуто.
10. В финальном сообщении: список перенесённого, какие порты-заглушки закрыты/остались, отклонения от старого кода (если были и почему).

## Типичные грабли (уже встречались)

- SpotBugs `VA_FORMAT_STRING_USES_NEWLINE`: в format-строках использовать `%n` вместо text block с `\n`.
- Record accessor'ы: после перевода POJO на Lombok везде заменить `x.items()` → `x.getItems()` и т.д.
- `PageResult` — теперь `api/internal/pojo/common/PageResult` (record); в REST страницы маппятся в сгенерированные `PageXxxResponse` с полями `items/page/size/totalElements/totalPages`.
- Миграции, ссылающиеся на чужие таблицы, дробим (см. правило выше).
- OpenAPI `format: date-time` → `OffsetDateTime`; внутри ходим с `Instant`.

## Очередь модулей

Перенесены: `users`, `auth`, `integrations` (Telegram + ML), `groups`, `events`, `debts`, `payments`, `files`.

Следующий: **receipts** (закроет `DistributionReadinessPort`, `EventSelectionCleanupPort` и `ReceiptDataPort`, добавит `FileSharedAccessVerifier` для чеков). Затем `notifications`, остатки `common`, в конце — сборка `app`.

Начни с изучения `skinemsya_parent/events/` и уже перенесённых модулей v2 как образца стиля. Работай по чеклисту, после каждого модуля — отчёт и зелёная сборка.
