# Implementation Report: Phase 2 — Auth & Users

## Metadata

| Поле | Значение |
| --- | --- |
| Этап | 2 — Auth & Users |
| Дата | 2026-06-15 |
| Статус | completed |
| Агент / автор | Cursor AI Agent |

---

## Прочитанные документы

### Базовые (обязательные)

- [x] `docs/ai/context-guide.md`
- [x] `docs/documentation-catalog.md`
- [x] `docs/business/mvp-scope.md`
- [x] `docs/architecture/system-overview.md`
- [x] `docs/architecture/backend-architecture.md`
- [x] `docs/architecture/module-dependencies.md`
- [x] `docs/roadmap/mvp-roadmap.md`

### Документы этапа

- [x] `docs/integrations/telegram.md`
- [x] `docs/modules/users/users.md`
- [x] `docs/modules/auth/auth.md`
- [x] `docs/security/jwt.md`
- [x] `docs/database/table-catalog.md`
- [x] `docs/database/naming-conventions.md`
- [x] `docs/testing/integration-tests.md`
- [x] `docs/adr/ADR-0006-jwt-authentication.md`

---

## Scope

### In scope (реализовано)

**Модуль `integrations`:**
- `TelegramInitDataValidator` + HMAC-SHA256 валидация init data (Telegram WebApp spec)
- `TelegramIdentity`, `TelegramIntegrationProperties`
- Unit-тесты: valid signature, invalid signature, expired `auth_date`

**Модуль `users`:**
- Liquibase: таблицы `users`, `user_profiles`
- JPA entities, repositories, MapStruct `UserMapper`
- `UserService` / `UserServiceImpl` — `upsertFromTelegram`, profile CRUD
- API: `GET /api/v1/users/me`, `PUT /api/v1/users/me/profile`
- Unit-тест `UserServiceTest`

**Модуль `auth`:**
- Liquibase: таблица `refresh_tokens`
- `JwtTokenService` (HS256, jjwt), `RefreshTokenService` (SHA-256 hash, rotation)
- `AuthService`, `AuthController` — `POST /api/v1/auth/telegram`, `POST /api/v1/auth/refresh`
- `JwtAuthenticationFilter`, `JwtProperties`, `AuthConfiguration`
- Unit-тесты: `JwtTokenServiceTest`, `RefreshTokenServiceTest`

**Модуль `common`:**
- `AuthenticatedUser` — общий security principal (избегает циклической зависимости `users` → `auth`)

**Модуль `app` (wiring):**
- Подключены `auth`, `users`, `integrations`, `spring-boot-starter-data-jpa`
- `JpaConfig` — `@EntityScan`, `@EnableJpaRepositories`
- `SecurityConfig` — JWT filter, public `/api/v1/auth/**`, `authenticated()` для остального
- Liquibase master includes для users + auth changesets
- `application-test.yml` — test JWT secret и bot token
- Исключён `UserDetailsServiceAutoConfiguration` (нет auto-generated password в логах)

**Тесты:**
- `AuthFlowIntegrationTest` — полный auth flow (Testcontainers PostgreSQL)
- `TelegramInitDataTestHelper` — генерация подписанной init data
- `GlobalExceptionHandlerTest` — `@ActiveProfiles("test")`, profile `webmvc-test` для slice-теста
- `LiquibaseMigrationTest` — проверка всех трёх changesets

### Out of scope (сознательно не делалось)

- Groups, events, authorization по membership
- `POST /auth/logout` endpoint
- RS256, device management, rate limiting
- Реальный Telegram Bot API (только init data validation)
- WireMock для Telegram

---

## Изменённые файлы

### Модуль `integrations`

| Файл | Действие | Описание |
| --- | --- | --- |
| `integrations/pom.xml` | modified | Зависимости: `common`, Spring Boot, Jackson |
| `integrations/.../TelegramInitDataValidator.java` | added | Интерфейс валидации |
| `integrations/.../TelegramIdentity.java` | added | Domain record |
| `integrations/.../TelegramInitDataValidatorImpl.java` | added | HMAC-SHA256 реализация |
| `integrations/.../TelegramIntegrationProperties.java` | added | `@ConfigurationProperties` |
| `integrations/.../IntegrationsConfiguration.java` | added | Enable properties |
| `integrations/.../TelegramInitDataValidatorTest.java` | added | Unit + test helper |

### Модуль `users`

| Файл | Действие | Описание |
| --- | --- | --- |
| `users/pom.xml` | modified | JPA, web, validation, MapStruct |
| `users/.../20250616-001-users.xml` | added | Liquibase: users, user_profiles |
| `users/.../User.java`, `UserProfile.java`, `PaymentDetails.java` | added | Domain |
| `users/.../UserService.java`, `UserServiceImpl.java` | added | Application layer |
| `users/.../UserEntity.java`, `UserProfileEntity.java` | added | JPA + JSONB mapping |
| `users/.../UserRepository.java`, `UserProfileRepository.java` | added | Repositories |
| `users/.../UserMapper.java` | added | MapStruct |
| `users/.../UserController.java` | added | GET/PUT users/me |
| `users/.../UserServiceTest.java` | added | Unit test |

### Модуль `auth`

| Файл | Действие | Описание |
| --- | --- | --- |
| `auth/pom.xml` | modified | users, integrations, jjwt, JPA, security |
| `auth/.../20250616-002-refresh-tokens.xml` | added | Liquibase: refresh_tokens |
| `auth/.../TokenPair.java` | added | Domain record |
| `auth/.../JwtTokenService.java` | added | Access token create/parse |
| `auth/.../RefreshTokenService.java` | added | Issue, rotate, revoke |
| `auth/.../AuthService.java` | added | authenticate, refresh |
| `auth/.../AuthController.java` | added | REST endpoints |
| `auth/.../JwtAuthenticationFilter.java` | added | Bearer JWT filter |
| `auth/.../JwtProperties.java`, `AuthConfiguration.java` | added | Config |
| `auth/.../RefreshTokenEntity.java`, `RefreshTokenRepository.java` | added | Persistence |
| `auth/.../JwtTokenServiceTest.java` | added | Unit test |
| `auth/.../RefreshTokenServiceTest.java` | added | Unit test |

### Модуль `common`

| Файл | Действие | Описание |
| --- | --- | --- |
| `common/pom.xml` | modified | `spring-security-core` |
| `common/.../AuthenticatedUser.java` | added | Shared security principal |

### Модуль `app`

| Файл | Действие | Описание |
| --- | --- | --- |
| `app/pom.xml` | modified | +auth, users, integrations, data-jpa |
| `app/.../JpaConfig.java` | added | EntityScan + JpaRepositories |
| `app/.../SecurityConfig.java` | modified | JWT filter chain, profile `!webmvc-test` |
| `app/.../db.changelog-master.xml` | modified | Includes users + auth migrations |
| `app/.../application.yml` | modified | JPA validate, exclude UserDetailsServiceAutoConfiguration |
| `app/.../application-test.yml` | modified | JWT_SECRET, TELEGRAM_BOT_TOKEN для тестов |
| `app/.../AuthFlowIntegrationTest.java` | added | Integration test auth flow |
| `app/.../TelegramInitDataTestHelper.java` | added | Test helper |
| `app/.../GlobalExceptionHandlerTest.java` | modified | test + webmvc-test profiles |
| `app/.../WebMvcTestSecurityConfig.java` | added | Permit-all для slice-теста |
| `app/.../LiquibaseMigrationTest.java` | modified | Проверка 3 changesets |

---

## Архитектурные решения

### AuthenticatedUser в `common`

**Контекст:** `UserController` нужен principal с `userId`, но `users` не должен зависеть от `auth`.

**Решение:** `AuthenticatedUser` вынесен в `common.security`.

**Обоснование:** `docs/architecture/module-dependencies.md` — `users` зависит только от `common`.

### Refresh token как opaque UUID + SHA-256 hash

**Контекст:** Хранение refresh token в БД.

**Решение:** Raw token — UUID, в БД только SHA-256 hash; rotation при refresh.

**Обоснование:** `docs/security/jwt.md`, `docs/adr/ADR-0006-jwt-authentication.md`.

### JSONB mapping для notification_settings

**Контекст:** PostgreSQL JSONB column vs Hibernate String mapping.

**Решение:** `@JdbcTypeCode(SqlTypes.JSON)` на `UserProfileEntity.notificationSettings`.

**Обоснование:** Hibernate 6 native JSON type support.

### Profile `webmvc-test` для slice-тестов

**Контекст:** `@WebMvcTest` подхватывает component scan и JWT beans.

**Решение:** `SecurityConfig` с `@Profile("!webmvc-test")`, отдельный `WebMvcTestSecurityConfig` для exception handler теста.

---

## Тесты

### Добавленные тест-классы

| Класс | Модуль | Тип | Что проверяет |
| --- | --- | --- | --- |
| `TelegramInitDataValidatorTest` | integrations | unit | HMAC validation, expiry |
| `UserServiceTest` | users | unit | upsert create/update |
| `JwtTokenServiceTest` | auth | unit | create/parse JWT |
| `RefreshTokenServiceTest` | auth | unit | issue, revoke |
| `AuthFlowIntegrationTest` | app | integration | full auth flow |
| `LiquibaseMigrationTest` | app | integration | 3 migrations applied |

### Результаты

```text
mvn clean install — BUILD SUCCESS

integrations: TelegramInitDataValidatorTest — 3 tests PASS
users:        UserServiceTest — 2 tests PASS
auth:         JwtTokenServiceTest — 3 tests PASS
              RefreshTokenServiceTest — 2 tests PASS
app:          AuthFlowIntegrationTest — 6 tests PASS
              GlobalExceptionHandlerTest — 4 tests PASS
              LiquibaseMigrationTest — 1 test PASS
              SkinemsyaApplicationTest — 1 test PASS
common:       MoneyTest, ErrorCodeTest, DomainExceptionTest — PASS
```

---

## Проверки

| Команда | Результат |
| --- | --- |
| `mvn clean install` | PASS |
| `docker compose up -d` | N/A (не запускался в среде агента) |
| `mvn spring-boot:run -pl skinemsya_parent/app` | N/A (требует `.env` с JWT_SECRET, TELEGRAM_BOT_TOKEN, DB) |
| Smoke curl auth → users/me | N/A (ручная проверка пользователем) |

### Smoke (для локальной проверки)

```bash
docker compose up -d
cp .env.example .env  # заполнить JWT_SECRET, TELEGRAM_BOT_TOKEN, DB_PASSWORD
mvn spring-boot:run -pl skinemsya_parent/app

# Сгенерировать initData через TelegramInitDataTestHelper или Mini App
curl -X POST http://localhost:8080/api/v1/auth/telegram \
  -H 'Content-Type: application/json' \
  -d '{"initData":"<signed-init-data>"}'

curl http://localhost:8080/api/v1/users/me \
  -H 'Authorization: Bearer <accessToken>'
```

---

## Known gaps / Tech debt

- Fail-fast при пустом `JWT_SECRET` / `TELEGRAM_BOT_TOKEN` в dev — только warn через `JwtTokenService` constructor (fail при старте если secret < 32 chars)
- `POST /auth/logout` не реализован (revoke доступен internal в `AuthService`)
- Нет integration-теста для `PUT /users/me/profile`
- `UserDetailsServiceAutoConfiguration` исключён глобально — при добавлении form-login потребуется явная конфигурация

---

## Критерий завершения этапа

**Из roadmap:** login через Telegram → JWT → GET /users/me

**Статус:** выполнен

**Комментарий:** Реализованы `POST /api/v1/auth/telegram` → `TokenPair` → `GET /api/v1/users/me` с `Authorization: Bearer`. Integration test `AuthFlowIntegrationTest` покрывает полный flow. `mvn clean install` проходит успешно.

---

## Следующий этап

**Этап 3 — Groups & Events** — не начинать без подтверждения пользователя.

Ожидаемый scope: группы, события, membership, authorization по группе.

---

## Stop

Этап завершён. Ожидаю подтверждения перед переходом к Этапу 3.
