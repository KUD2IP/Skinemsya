# Testing: Integration Tests

## Purpose

Документ описывает интеграционные тесты: API, security, транзакции и БД с Testcontainers.

## Context

Integration-тесты проверяют, что модули корректно работают вместе через реальный Spring context и PostgreSQL.

## Responsibilities

- Определить scope integration-тестов.
- Описать тестовые сценарии API.
- Зафиксировать подход к мокам внешних систем.

## Non Responsibilities

- Документ не описывает unit-тесты.
- Документ не тестирует реальный Telegram или ML-сервис.

## Design Decisions

Обязательные integration-тесты:

**Auth:**
- Valid init data → JWT pair.
- Invalid init data → 401.
- Refresh token → new pair.
- Expired refresh → 401.

**Groups:**
- Create standalone group → 201.
- Create from chat context → CHAT_LINKED group.
- Add member by telegram id.
- Non-member access → 403.

**Events + Receipts + Debts (happy path):**
- Create event → add positions → distribute → select → calculate debts.
- Upload receipt → mock ML → positions created.

**Payments:**
- Create payment → confirm debtor → confirm payer → debt closed.
- Confirm payer without debtor → 400.

Подход:
- `@SpringBootTest` + Testcontainers PostgreSQL.
- Liquibase migrate before tests.
- WireMock для ML-сервиса и Telegram API.
- `@Transactional` rollback или explicit cleanup per test class.
- RestAssured или MockMvc для HTTP.

## Constraints

- Integration-тесты < 5 минут суммарно в CI.
- Один PostgreSQL container per test suite.
- Внешние системы всегда мокаются.

## Future Evolution

- Test slices (`@WebMvcTest`, `@DataJpaTest`).
- Parallel test execution.
- Contract tests с реальным ML-сервисом в staging.

## Related Documents

- `docs/testing/testcontainers.md`
- `docs/architecture/request-flow.md`
- `docs/testing/test-data.md`
- `docs/security/api-security.md`
