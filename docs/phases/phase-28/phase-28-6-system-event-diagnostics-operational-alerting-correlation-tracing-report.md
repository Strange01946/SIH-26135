# Phase 28.6 — System Event Diagnostics, Operational Alerting & Correlation Tracing Report

## 1. Executive Summary

Phase 28.6 implements structured system-event diagnostics, operational alerting persistence/throttling, end-to-end `X-Correlation-Id` and `X-Request-Id` propagation, and a multi-criteria, tenant-isolated diagnostic query API over `GET /api/v1/system-event-logs` in the SIH-26135 Spring Boot backend (`E:\project\backend`, branch `main`).

All implementation work, targeted correlation integrity corrections, and integration testing operated strictly within the frozen MySQL 8 schema (`ddl-auto=validate`) using `SIH26135_TEST` exclusively, with zero DDL modifications, zero production `SIH26135` database access, and zero Git state mutations. After the final edit, the focused Phase 28.6 and correlation verification run (`145` tests) passed cleanly and the complete backend regression suite passed against `SIH26135_TEST` (**1,949 tests run, 0 failures, 0 errors, 0 skipped**).

**Final Verdict:** **READY FOR INDEPENDENT REVIEW**

---

## 2. Existing Architecture Inspection Summary

Pre-implementation inspection across the repository established:

- **Schema (`database/phase-15-audit-data-quality-logs.sql`):**
  - `ref_system_event_category` seeds exactly 5 categories: `1=SECURITY`, `2=INTEGRATION`, `3=BATCH_JOB`, `4=CONFIGURATION`, `5=ACCESS`.
  - `ref_system_event_severity` seeds exactly 4 severities: `1=INFO`, `2=WARNING`, `3=ERROR`, `4=CRITICAL`.
  - `system_event_logs` contains `system_event_log_id`, `system_event_category_id`, `system_event_severity_id`, `event_code` (`VARCHAR(64)`), `actor_user_id`, `entity_type` (`VARCHAR(64)`), `entity_id`, `source_component` (`VARCHAR(64)`), `event_summary` (`VARCHAR(500)`), `occurred_at`, and `created_at`.
  - `system_event_logs` has no dedicated `correlation_id`, `request_id`, or `tenant_id` / `state_id` / `provider_id` / `employer_id` columns under the frozen schema.
- **Correlation Infrastructure (`Phase 26.10`):** `CorrelationIdFilter` and `CorrelationContextHolder` already propagated validated `X-Correlation-Id` and `X-Request-Id` on synchronous HTTP threads, while `@Async("auditTaskExecutor")` in `AsyncConfig` had no `TaskDecorator` and non-HTTP workflows had no automatic correlation setup/cleanup.
- **External Alert Channels & Rate Limiting:** No email (`JavaMailSender`), SMS, Webhook, Slack, PagerDuty, or push notification transport exists in the codebase. Rate limiting exists via `ExportRateLimiter` throwing `RateLimitExceededException`, handled in `GlobalExceptionHandler.handleRateLimitExceededException`.
- **Legacy `SystemEventLogController` / `SystemEventLogServiceImpl`:** Previously exposed only single-parameter lookup endpoints (`GET /{id}`, `GET` default, single-param routes), lacked multi-criteria filtered search, lacked DB/service tenant scoping, accepted raw unsanitized client inputs on `POST`, and did not enforce write-vs-read authority separation on `POST`.

---

## 3. SystemEventLoggingService & Schema Compatibility Design

Created `SystemEventLoggingService` (`service/SystemEventLoggingService.java`) and `SystemEventLoggingServiceImpl` (`service/impl/SystemEventLoggingServiceImpl.java`) supporting all five categories (`EventCategory`: `SECURITY`, `INTEGRATION`, `BATCH_JOB`, `CONFIGURATION`, `ACCESS`) and all four severities (`EventSeverity`: `INFO`, `WARNING`, `ERROR`, `CRITICAL`), alongside hardened `SystemEventLogServiceImpl` (`recordSystemEvent` and `recordSystemEventFromApi`):

- **Server-Derived Metadata:** `occurred_at` and `created_at` are always generated server-side (`LocalDateTime.now()`). Category and severity enum/code values resolve authoritatively against `RefSystemEventCategoryRepository` and `RefSystemEventSeverityRepository` without hardcoded ID fallbacks. `actorUserId` is verified against `UserRepository` (`null` if absent rather than failing FK constraints).
- **Schema-Compatible Correlation Storage:** Because `system_event_logs` has no separate `correlation_id` or `request_id` column in frozen `phase-15-audit-data-quality-logs.sql`, `SystemEventSanitizer.formatCorrelatedSummary(...)` prepends a normalized marker `[cid=<correlationId>|rid=<requestId>] ` (bounded cleanly within `VARCHAR(500)`).
- **`REQUIRES_NEW` Transaction Isolation:** `SystemEventLoggingServiceImpl` uses a `TransactionTemplate` configured with `TransactionDefinition.PROPAGATION_REQUIRES_NEW` so operational events commit independently even when an outer business or batch transaction rolls back.
- **Recursion & Fallback Safety:** A `ThreadLocal<Boolean> IN_EVENT_LOGGING` re-entrancy guard suppresses recursive event persistence if persistence fails, catching exceptions after issuing a sanitized structured SLF4J log line so diagnostic logging failures never mask or corrupt upstream business responses.
- **Deduplication & Anti-Flood Throttling:** Bounded `ConcurrentHashMap` caches (evicted cleanly at `maxThrottleCacheEntries`) provide same-request/event deduplication (`dedupWindowSeconds`), rate-limit alert cooldown (`rateLimitAlertCooldownSeconds`), and per-(tenant, signal) data-quality spike cooldown (`spikeAlertCooldownSeconds`).

---

## 4. `X-Correlation-Id` & `X-Request-Id` End-to-End Propagation

- **`CorrelationContextHolder` (`correlation/CorrelationContextHolder.java`):** Added `CURRENT_REQUEST_ID` thread-local alongside `CURRENT_CORRELATION_ID`, `ContextSnapshot`, `captureSnapshot()`, `restore(ContextSnapshot)`, and `clear()`.
- **`CorrelationIdFilter` (`correlation/CorrelationIdFilter.java`):** Independently validates incoming `X-Correlation-Id` and `X-Request-Id` headers (`^[A-Za-z0-9_-]{1,64}$`), preserves both distinctly when both are valid (e.g. `trace-123` / `request-456`), preserves compatible single-header fallbacks when only one header is provided, and independently replaces invalid/oversized/missing headers with safe server UUIDs. Stores both `CORRELATION_ID_ATTRIBUTE` and `REQUEST_ID_ATTRIBUTE` on error dispatches, binds both `CorrelationContextHolder` and SLF4J MDC (`correlationId`, `requestId`), emits both response headers cleanly, and restores/clears all context in `finally`.
- **`AsyncConfig` (`config/AsyncConfig.java`):** Added `@Bean TaskDecorator correlationTaskDecorator()` wired onto `auditTaskExecutor`. Captures validated parent `correlationId` (or generates a server UUID if absent), mints a distinct per-execution async `requestId` UUID, populates worker thread MDC and `CorrelationContextHolder`, and restores/clears prior worker thread state in `finally`.
- **Non-HTTP / Background Operations:** Both `ImportBatchProcessingWorkflowImpl.processBatch(...)` and `DataQualityEngineServiceImpl.executeScan(...)` generate server-owned UUID correlation/request IDs when invoked without an active HTTP correlation context and restore/clear the thread state in `finally`.

---

## 5. Operational Triggers Implemented

1. **Trigger 1 — Account Lockouts (`AuthenticationServiceImpl` + `SystemEventLogServiceImpl`):**
   - `AuthenticationServiceImpl` continues emitting `LOGIN_FAILURE` (`SECURITY`/`WARNING`), `ACCOUNT_LOCKED` (`SECURITY`/`ERROR`), and `LOGIN_BLOCKED_LOCKED_ACCOUNT` (`SECURITY`/`WARNING`) with `[cid=...|rid=...]` correlation enrichment applied in `SystemEventLogServiceImpl.recordSystemEvent`.
   - **Critical Integration Dependency Resolved:** End-to-end HTTP login lockout testing against MySQL (`POST /api/v1/auth/login`) revealed that `AuthenticationServiceImpl.recordAuditLog(...)` previously called `auditLogService.recordAuditLog(...)`, which (since Phase 28.2 hardened `recordAuditLog` for authenticated API callers) threw `AccessDeniedException` inside the unauthenticated `/api/v1/auth/login` transaction, causing Spring to roll back the user lockout status, failed attempt count, and `ACCOUNT_LOCKED` event. Updated `AuthenticationServiceImpl.recordAuditLog(...)` to invoke `auditLogService.recordInternalAuditLog(...)` (and updated `AccountLockoutAndSecurityAuditTest` mock verification accordingly), preserving frozen Phase 28.2 `AuditLogServiceImpl` unchanged while enabling genuine persistent account lockouts end to end.
2. **Trigger 2 — Rate-Limit Violations (`GlobalExceptionHandler` + `ExportRateLimiter`):**
   - Instrumented `GlobalExceptionHandler.handleRateLimitExceededException(...)` (the sole existing rate-limiter rejection path; no new rate limiter introduced) to emit throttled `ACCESS`/`WARNING` events (`EXPORT_RATE_LIMIT_EXCEEDED`) capturing `actorUserId` from `WorkflowSecurityScopeValidator` (when authenticated), sanitized request URI path (without query strings/tokens), `X-Correlation-Id`, and cooldown suppression so bursts preserve HTTP `429` + `Retry-After` without DB log amplification.
3. **Trigger 3 — Phase 28.5 Failed Batch Jobs (`ImportBatchProcessingWorkflowImpl`):**
   - Emits a single `BATCH_JOB`/`ERROR` (`IMPORT_BATCH_FAILED` / `IMPORT_FINALIZATION_FAILED` / `IMPORT_STAGING_FAILED`) event in an isolated `REQUIRES_NEW` transaction when a batch terminates in `FAILED` state, encounters finalization error, or cannot stage cleanly. `COMPLETED` batches with row-level quarantined failures (`FAILED`/`SKIPPED`/`DUPLICATE` `import_records`) do **not** emit job-failure alerts.
4. **Trigger 4 — Phase 28.3 Critical Data-Quality Spikes (`DataQualityEngineServiceImpl` + `DataQualityEngineProperties`):**
   - Evaluates newly created anomaly counts against configurable thresholds (`data-quality.spike-new-issues-threshold=20`, `data-quality.spike-high-severity-threshold=10`, `data-quality.spike-critical-severity-threshold=1`, `data-quality.spike-alert-cooldown-seconds=300`) and emits tenant-scoped `DATA_QUALITY_CRITICAL_SPIKE` (`INTEGRATION`, `CRITICAL` or `ERROR`) via cooldown-protected `logDataQualitySpike(...)`.
   - **Critical Integration Dependency Resolved:** Resolved `LazyInitializationException` when inspecting detached `DataQualityRule.dataQualitySeverity` proxies outside a Hibernate session in `DataQualityEngineServiceImpl` via `resolveRuleSeverityCode(...)` (checking `Hibernate.isInitialized(severity)` and safely falling back to FK `severity.getId()`).

---

## 6. Operational Alerting & External Delivery Limitation Statement

- **Implemented Alerting Behavior:** Critical operational incidents (account lockouts, throttled rate-limit rejections, terminal failed batch jobs, and threshold-breaching data-quality spikes) are durably persisted to `system_event_logs` in independent `REQUIRES_NEW` transactions, emitted as structured single-line SLF4J log records with `correlationId` and `requestId`, and protected by bounded in-memory deduplication and cooldown windows (`system-events.*` and `data-quality.spike-alert-cooldown-seconds`).
- **Explicit Limitation Disclosure:** No external outbound notification channel (email SMTP/`JavaMailSender`, SMS gateway, Slack, PagerDuty, or HTTP webhook dispatcher) exists in the SIH-26135 codebase, and no unrequested external delivery client or DDL table was invented in Phase 28.6. Alerting in this phase therefore consists of durable structured persistence, structured server logs, queryable `GET /api/v1/system-event-logs` diagnostics, and anti-flood suppression ready for future notification subscribers if authorized in a later phase.

---

## 7. Diagnostic API (`GET /api/v1/system-event-logs`), Filtering, Security & Tenant Isolation

- **Endpoints (`SystemEventLogController`):** Preserves class-level route `/api/v1/system-event-logs` and existing route count (665 mapping annotations across 63 `@RestController`s).
  - Multi-criteria `GET /api/v1/system-event-logs` supports: `categoryId`, `categoryCode`, `severityId`, `severityCode`, `eventCode`, `correlationId`, `requestId`, `actorUserId`, `entityType`, `entityId`, `sourceComponent`, `start`, `end`, `page`, `size`, `sort`, and `direction`.
  - Unknown query parameters (including client-supplied tenant-widening params such as `stateId`, `districtId`, `providerId`, `employerId`) are rejected with `400 Bad Request` (`UNSUPPORTED_PARAMETER`).
  - Invalid `categoryCode` (`INVALID_EVENT_CATEGORY`), `severityCode` (`INVALID_EVENT_SEVERITY`), malformed `correlationId`/`requestId` (`INVALID_CORRELATION_ID`/`INVALID_REQUEST_ID`), `start > end` (`INVALID_DATE_RANGE`), and non-allowlisted `sort` fields fail fast with `400 Bad Request`.
  - Single-parameter compatibility routes (`?categoryId=...`, `?severityId=...`, etc.) include `!requestId`, `!sourceComponent`, `!categoryCode`, `!severityCode` parameter exclusions so existing Phase 26.8/26.10 contract tests remain intact while applying identical tenant isolation at the service boundary.
- **RBAC & Fail-Closed Authorization (`SystemEventLogController` & `SystemEventLogServiceImpl`):**
  - Read operations require any of `audit.read`, `system.manage`, `system.event.read`, `system.event.manage`; unauthenticated requests return `401`.
  - `POST /api/v1/system-event-logs` requires `system.manage` or `system.event.manage` (`audit.read` alone returns `403`). Append-only immutability is preserved (no `PUT`/`PATCH`/`DELETE`).
  - `ROLE_trainee` callers, deleted users, users with conflicting persona roles, non-nationwide users with missing or ambiguous (`rootCount != 1`, provider/employer + geographic `state_id`/`district_id`) tenancy fail closed with `403 AccessDeniedException`.
- **DB/Service-Boundary Tenant Isolation (`SystemEventLogQueryRepository`):**
  - Because `system_event_logs` stores no native tenant columns, `SystemEventLogQueryRepository` uses parameterized `JdbcTemplate` queries joining `users` (`actor_user_id` and `entity_type='USER'`), `import_batches` (`entity_type='IMPORTBATCH'`), `training_providers`, and `employers` to enforce SQL-level `WHERE` predicates scoped strictly to the caller's verified `state_id`, `(state_id, district_id)`, `training_provider_id`, or `employer_id` — including across `?correlationId=...`, `?requestId=...`, and `GET /api/v1/system-event-logs/{id}` lookups (`403` on cross-tenant ID).
  - Canonical prefix matching enforces position-0 bracket anchoring (`[cid=...]` for `correlationId`, and leading `[cid=%|rid=%]%` with `SUBSTRING_INDEX(SUBSTRING_INDEX(sel.event_summary, ']', 1), '|rid=', -1) = ?` for `requestId`) so body text can never spoof tracing filters.
  - Using `@Repository` with `JdbcTemplate` keeps Spring Data JPA interface count (`137`) and `@Query` annotation count (`2`) unchanged.

---

## 8. Sanitization, Anti-Forgery, Bounded Sizing & Configuration

- **`SystemEventSanitizer` & `SystemEventLogServiceImpl`:** Strips CR/LF/control characters; removes/escapes all client-supplied `[cid=...]` / `[rid=...]` bracket tags and `|rid=` / `cid=` / `rid=` fragments from untrusted message text (`CORRELATION_BRACKET_TAG_PATTERN`, `CORRELATION_KV_FRAGMENT_PATTERN`) before prepending a single canonical `[cid=<serverCorr>|rid=<serverReq>] ` prefix derived strictly from trusted server `CorrelationContextHolder`/`MDC` (or generated server UUID when absent); redacts JDBC URLs, JWT tokens, `Bearer` headers, key-value credentials (`password`, `secret`, `token`, `jwt`, `api_key`, etc.), Aadhaar patterns (`XXXX-XXXX-<last4>`), PAN patterns (`XXXXX<digits><check>`), and stack-trace frames; normalizes/truncates `eventCode` and identifiers to `64` characters and formatted summaries to `500` characters.
- **Configuration (`application.properties` & `test/resources/application.properties`):**
  - `system-events.rate-limit-alert-cooldown-seconds=60`
  - `system-events.dedup-window-seconds=60`
  - `system-events.max-throttle-cache-entries=10000`
  - `data-quality.spike-new-issues-threshold=20`
  - `data-quality.spike-high-severity-threshold=10`
  - `data-quality.spike-critical-severity-threshold=1`
  - `data-quality.spike-alert-cooldown-seconds=300`

---

## 9. Verification Results (`SIH26135_TEST`)

- **Database Safety:** Verified `MysqlConnectionTest.assertDedicatedTestCatalog` against `SIH26135_TEST` in every integration suite `@BeforeEach`; `ddl-auto=validate` passed across every Spring Boot test context; zero DDL changes and zero production `SIH26135` access.
- **Focused Verification Run (before full suite, after final code edit):**
  - Executed `SystemEventDiagnosticsUnitTest`, `SystemEventDiagnosticsIntegrationTest`, `CorrelationTrackingIntegrationTest`, `OperationalLogEventPaginationTest`, `AccountLockoutAndSecurityAuditTest`, `OperationalErrorCodeContractTest`, `ResponseEnvelopeAlignmentContractTest`, and `ControllerPathQueryHeaderValidationTest`.
  - Result: **145 tests run, 0 failures, 0 errors, 0 skipped** (`01:54 min`, `BUILD SUCCESS`).
- **Full Backend Regression Run (`mvn -f "E:\project\backend\pom.xml" test` after final edit):**
  - Result: **1,949 tests run, 0 failures, 0 errors, 0 skipped** (`06:14 min`, `BUILD SUCCESS`).
  - Phase 28.6 tests included in full run:
    - `SystemEventDiagnosticsUnitTest`: `5` tests passed (all 5 categories x 4 severities + server metadata, secret/Aadhaar/PAN/CRLF/stack sanitization + 500-char bounding, forged `[cid=...]` / `|rid=` stripping with single canonical prefix, DB failure fallback + recursion guard + same-request dedup, and `AsyncConfig.correlationTaskDecorator()` parent/async/cleanup behavior).
    - `SystemEventDiagnosticsIntegrationTest`: `9` tests across `6` nested groups passed against live MySQL `SIH26135_TEST` (independent HTTP `X-Correlation-Id: trace-123` & `X-Request-Id: request-456` preservation across headers/context/MDC/persisted events + missing/invalid/oversized replacement, `@Async("auditTaskExecutor")` propagation, real HTTP account lockout & audit persistence, export `429` rate-limit trigger + cooldown throttling, Phase 28.5 partial quarantine vs `FAILED` batch isolation & background server correlation cleanup, Phase 28.3 data-quality spike threshold/cooldown/provider isolation, diagnostic API RBAC/multi-criteria filters/sorting/tenant isolation/400 validation/safe `POST`, and adversarial forged correlation/request marker rejection preserving tenant isolation).
    - Regression checks verified green in full run: `CorrelationTrackingIntegrationTest` (with `1.4` updated to assert distinct `X-Request-Id` preservation alongside `X-Correlation-Id` governance), `AccountLockoutAndSecurityAuditTest` (`17`), `AuditLogImmutabilityAndTimelineTest` (`23`), `OperationalLogEventPaginationTest` (`23`), `OperationalErrorCodeContractTest` (`8`), `DataQualityAnomalyDetectorsTest` (`13`), `DataQualityIssueServiceSecurityTest` (`10`), `DataQualityRuleCatalogTest` (`5`), `DataQualityDeduplicationAndScanApiTest` (`23`), `AnalyticsExportTest`, `ImportBatchProcessingIntegrationTest`, `ControllerPathQueryHeaderValidationTest`.
- **Architecture Invariants:**
  - Spring Data JPA repository interfaces: `137` (`Found 137 JPA repository interfaces.` in Spring Boot test logs).
  - `@Query(` annotations: `2`.
  - `@RestController` classes: `63`.
  - Controller request-mapping annotations (`@GetMapping`/`@PostMapping`/`@PutMapping`/`@PatchMapping`/`@DeleteMapping`/`@RequestMapping`): `665`.

---

## 10. File Inventory & Git Hygiene

- **New Production Files (`5`):**
  - `backend/src/main/java/in/gov/sih/sih26135/config/SystemEventLoggingProperties.java`
  - `backend/src/main/java/in/gov/sih/sih26135/repository/SystemEventLogQueryRepository.java`
  - `backend/src/main/java/in/gov/sih/sih26135/service/SystemEventLoggingService.java`
  - `backend/src/main/java/in/gov/sih/sih26135/service/event/SystemEventSanitizer.java`
  - `backend/src/main/java/in/gov/sih/sih26135/service/impl/SystemEventLoggingServiceImpl.java`
- **Modified Production & Config Files (`14`):**
  - `backend/src/main/java/in/gov/sih/sih26135/config/AsyncConfig.java`
  - `backend/src/main/java/in/gov/sih/sih26135/config/DataQualityEngineProperties.java`
  - `backend/src/main/java/in/gov/sih/sih26135/controller/SystemEventLogController.java`
  - `backend/src/main/java/in/gov/sih/sih26135/correlation/CorrelationContextHolder.java`
  - `backend/src/main/java/in/gov/sih/sih26135/correlation/CorrelationIdFilter.java`
  - `backend/src/main/java/in/gov/sih/sih26135/dto/request/OperationalEventFilterCriteria.java`
  - `backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java`
  - `backend/src/main/java/in/gov/sih/sih26135/service/SystemEventLogService.java`
  - `backend/src/main/java/in/gov/sih/sih26135/service/auth/impl/AuthenticationServiceImpl.java`
  - `backend/src/main/java/in/gov/sih/sih26135/service/impl/DataQualityEngineServiceImpl.java`
  - `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportBatchProcessingWorkflowImpl.java`
  - `backend/src/main/java/in/gov/sih/sih26135/service/impl/SystemEventLogServiceImpl.java`
  - `backend/src/main/resources/application.properties`
  - `backend/src/test/resources/application.properties`
- **New & Modified Test Files (`4`):**
  - `backend/src/test/java/in/gov/sih/sih26135/service/event/SystemEventDiagnosticsUnitTest.java` (new)
  - `backend/src/test/java/in/gov/sih/sih26135/controller/SystemEventDiagnosticsIntegrationTest.java` (new)
  - `backend/src/test/java/in/gov/sih/sih26135/correlation/CorrelationTrackingIntegrationTest.java` (updated `1.4` assertion for independent `X-Request-Id` preservation)
  - `backend/src/test/java/in/gov/sih/sih26135/service/auth/AccountLockoutAndSecurityAuditTest.java` (updated mock verification to `recordInternalAuditLog`)
- **Report Artifacts (`2` synchronized copies):**
  - `phase-28-6-system-event-diagnostics-operational-alerting-correlation-tracing-report.md`
  - `docs/phases/phase-28/phase-28-6-system-event-diagnostics-operational-alerting-correlation-tracing-report.md`
- **Git Hygiene:** Branch `main`; `git diff --check` and `git diff --cached --check` reported zero whitespace/conflict-marker issues; no commits, stashes, resets, pushes, or branch switches were performed.
