# Phase 28.8 — Role-Based Tenancy Isolation, PII Sanitization & Security Hardening Implementation Report

**Phase Status:** Implementation & Verification Complete  
**Target Branch:** `main`  
**Backend Root:** `E:\project\backend`  
**Verification Database:** `SIH26135_TEST` (`spring.jpa.hibernate.ddl-auto=validate`)  
**Final Verdict:** **READY FOR INDEPENDENT REVIEW**

---

## 1. Executive Summary

Phase 28.8 implements comprehensive tenant isolation, sensitive credential/PII sanitization, and collection pagination hardening for the operational and core training domain infrastructure of the SIH-26135 Spring Boot backend. The operational services (`AuditLogService`, `SystemEventLoggingService`, `DataQualityRuleService`, `DataQualityIssueService`, `DataQualityWorkflowService`, `DataQualityIssueEventService`, `ImportBatchService`, `ImportRecordService`, and `OperationalDashboardService`) and provider-owned domain services (`TrainingCenterService`, `TrainingBatchService`) now enforce authoritative national, state, district, and training-provider ownership boundaries via `WorkflowSecurityScopeValidator` and SQL-level predicates before pagination and aggregation.

A centralized, null-safe, idempotent, and bounded payload masker (`AuditPayloadMasker`) was implemented to redact passwords, password hashes (bcrypt/argon2), JWTs, Bearer/Basic credentials, API keys, client secrets, refresh/access tokens, and JDBC connection strings with `[REDACTED]`, while masking Aadhaar numbers as `XXXX-XXXX-1234` and PAN identifiers as `XXXXXX123X`. Read-time sanitization guarantees that historical unmasked database entries in `audit_logs`, `system_event_logs`, `data_quality_issues`, `data_quality_issue_events`, and `import_records` are masked in all API responses while preserving the strict physical immutability of historical audit records. Collection endpoints enforce bounded pagination (`size` 1..100, `page >= 0`), allowlisted sort fields, stable tie-breakers, and safe large-page offset short-circuiting.

**Key Verification Outcomes:**
- **Zero Regressions & New Test Suite:** 13 net new unit and integration tests added across `AuditPayloadMaskerUnitTest` (5 tests) and `OperationalHardeningSecurityAndPrivacyTest` (8 tests).
- **Full Backend Regression Run:** `1,972` tests run (`+13` over the Phase 28.7 baseline of `1,959`), `0` failures, `0` errors, `0` skipped (`Total time: 05:47 min`, `BUILD SUCCESS`) against `SIH26135_TEST`.
- **Architectural Invariants Preserved:** `137` Spring Data JPA repository interfaces (`Found 137 JPA repository interfaces.`), `2` `@Query` annotations, `64` `@RestController` classes, `668` contract-audited endpoint mappings (`ResponseEnvelopeAlignmentContractTest`), `spring.jpa.hibernate.ddl-auto=validate`, zero DDL alterations, zero production `SIH26135` access, and clean Git diff (`git diff --check` exit `0`).

---

## 2. Existing Security Architecture Inspection

Before implementation, the existing security, tenancy, and sanitization infrastructure was inspected:

1. **`WorkflowSecurityScopeValidator` (Phase 24–28):**
   - Authoritative identity and tenancy resolver: inspects `SecurityContextHolder`, validates explicit administrative roles (`ROLE_super_admin`, `ROLE_government_admin`), and verifies authoritative `users` table records.
   - `validateNationwideScope()`: verifies the caller holds `ROLE_super_admin` or `ROLE_government_admin`, resolves the DB user record, and confirms all organizational tenancy columns (`state_id`, `district_id`, `training_provider_id`, `employer_id`) and trainee profile affiliations are strictly `NULL`.
   - `validateDashboardProviderScope(providerId)`: throws `AccessDeniedException` (HTTP 403) if caller does not own the requested provider.
   - `validateStateScope(stateId)` and `validateDistrictScope(districtId)`: enforce state administrative scope and district matches for administrative users.
2. **Existing Sanitizers and Handlers:**
   - `SystemEventSanitizer`: strips CRLF, control characters, stack traces, and forged correlation tags before attaching authoritative server correlation prefixes.
   - `ImportErrorSanitizer`: sanitizes row-level CSV/JSON parsing and constraint violation messages before storing quarantine entries.
   - `AuditTrailEventListener`: intercepts JPA lifecycle mutations and formats `change_summary`. Contract tests (`AuditTrailEventListenerTest`) assert historical format compatibility.
3. **Controller and Query Repository Layer:**
   - Audited controllers (`AuditLogController`, `SystemEventLogController`, `DataQualityRuleController`, `DataQualityIssueController`, `DataQualityIssueEventController`, `ImportBatchController`, `ImportRecordController`, `OperationalDashboardController`, `TrainingCenterController`, `TrainingBatchController`, `CourseController`) expose both paginated endpoints (`PagedResponse`) and legacy query-param collection endpoints (`ApiResponse<List<...>>`).
   - Query repositories (`AuditLogQueryRepository`, `SystemEventLogQueryRepository`, `OperationalDashboardQueryRepository`) execute direct SQL via `JdbcTemplate` / `NamedParameterJdbcTemplate`, requiring SQL-level tenancy predicates.

---

## 3. Operational Endpoint Inventory

The 11 operational and training domain controllers expose the following endpoints:

1. **`AuditLogController` (`/api/v1/audit-logs`):**
   - `GET /{id}`
   - `GET /` (paginated multi-criteria search)
   - `GET ?actorUserId=...`
   - `GET ?auditActionId=...`
   - `GET ?entityType=...&entityId=...`
   - `GET ?entityType=...`
   - `GET ?correlationId=...`
   - `GET ?start=...&end=...`
   - `GET /entities/{type}/{id}/timeline`
   - `POST /` (manual audit log recording)
2. **`SystemEventLogController` (`/api/v1/system-event-logs`):**
   - `GET /{id}`
   - `GET /` (paginated multi-criteria search)
   - `GET ?categoryId=...`
   - `GET ?severityId=...`
   - `GET ?eventCode=...`
   - `GET ?actorUserId=...`
   - `GET ?entityType=...&entityId=...`
   - `GET ?entityType=...`
   - `GET ?sourceComponent=...`
   - `GET ?start=...&end=...`
   - `POST /` (manual event recording)
3. **`DataQualityRuleController` (`/api/v1/data-quality-rules` & `/api/v1/data-quality`):**
   - `GET /{id}`
   - `GET ?ruleCode=...`
   - `GET ?targetEntityType=...`
   - `GET ?categoryId=...`
   - `GET ?severityId=...`
   - `GET ?lifecycleStatusId=...`
   - `POST /` (create rule)
   - `PUT /{id}` (update rule)
   - `DELETE /{id}` (delete rule)
   - `POST /scan` (trigger automated anomaly scan)
4. **`DataQualityIssueController` (`/api/v1/data-quality-issues`):**
   - `GET /{id}`
   - `GET ?ruleId=...`, `?categoryId=...`, `?severityId=...`, `?statusId=...`, `?sourceId=...`, `?entityType=...&entityId=...`, `?entityType=...`, `?assignedUserId=...`, `?resolvedByUserId=...`
   - `POST /` (report issue)
   - `PUT /{id}` (update issue)
   - `POST /{id}/assign`
   - `POST /{id}/start-investigation`
   - `POST /{id}/resolve`
   - `POST /{id}/dismiss`
   - `POST /{id}/reopen`
   - `GET /{id}/events`
5. **`DataQualityIssueEventController` (`/api/v1/data-quality-issue-events`):**
   - `GET /{id}`
   - `GET ?issueId=...`, `?statusId=...`, `?changedByUserId=...`
   - `POST /` (record issue event)
6. **`ImportBatchController` (`/api/v1/import-batches` & `/api/v1/import/batches`):**
   - `GET /{id}`
   - `GET ?batchCode=...`, `?statusId=...`, `?entityType=...`, `?sourceSystem=...`, `?initiatedByUserId=...`
   - `POST /` (multipart upload & stage)
   - `POST /{id}/process`
   - `POST /{id}/recover`
   - `GET /{id}/progress`
   - `GET /{id}/records` (paginated batch records)
   - `GET /{id}/quarantine` (paginated quarantined records)
   - `POST /` (legacy JSON batch creation)
   - `PUT /{id}` (legacy batch update)
   - `DELETE /{id}` (legacy batch delete)
7. **`ImportRecordController` (`/api/v1/import-records`):**
   - `GET /{id}`
   - `GET ?batchId=...&sourceRowNumber=...`
   - `GET ?batchId=...` (paginated)
   - `GET ?statusId=...` (paginated)
   - `GET ?entityType=...&entityId=...` (paginated & unpaginated)
   - `POST /` (legacy record create)
   - `PUT /{id}` (legacy record update)
   - `DELETE /{id}` (legacy record delete)
8. **`OperationalDashboardController` (`/api/v1/operational`):**
   - `GET /data-quality-scorecard`
   - `GET /import-metrics`
   - `GET /system-health`
9. **`TrainingCenterController` (`/api/v1/training-centers`):**
   - `GET /{id}`
   - `GET /` (paginated with optional code, providerId, districtId, stateId filters)
   - `POST /`, `PUT /{id}`, `DELETE /{id}`
10. **`TrainingBatchController` (`/api/v1/training-batches`):**
    - `GET /{id}`
    - `GET /` (paginated with optional code, courseId, providerId, centerId, programId, statusId filters)
    - `POST /`, `PUT /{id}`, `PUT /{id}/status`, `DELETE /{id}`
11. **`CourseController` (`/api/v1/courses`):**
    - `GET /{id}`, `GET /` (global catalog resource, platform-wide read), `POST /`, `PUT /{id}`, `DELETE /{id}`

---

## 4. Endpoint Authorization Matrix

| Endpoint Route & Pattern | HTTP Method | Required Authority | Tenancy Scope Enforcement |
|:---|:---:|:---|:---|
| `/api/v1/audit-logs/{id}` | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin OR entity-level tenant ownership check (`validateEntityAccess`) |
| `/api/v1/audit-logs` (search) | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin OR SQL-scoped to caller tenant (`AuditLogQueryRepository`) |
| `/api/v1/audit-logs?actorUserId=...` | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin only (`403` for scoped callers) |
| `/api/v1/audit-logs?auditActionId=...` | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin only (`403` for scoped callers) |
| `/api/v1/audit-logs?entityType=...&entityId=...` | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin OR entity-level ownership validation |
| `/api/v1/audit-logs?entityType=...` | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin only (`403` for scoped callers) |
| `/api/v1/audit-logs?correlationId=...` | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin only (`403` for scoped callers) |
| `/api/v1/audit-logs?start=...&end=...` | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin OR SQL-scoped date search |
| `/api/v1/audit-logs/entities/{type}/{id}/timeline` | `GET` | `audit.read` \| `system.manage` | Verified nationwide admin OR entity-level tenant ownership check |
| `/api/v1/audit-logs` | `POST` | `system.manage` | Restricted to nationwide admin and authorized manual actions (`UPDATE`, `EXPORT`) |
| `/api/v1/system-event-logs/{id}` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Verified nationwide admin OR tenant-scoped event ownership check |
| `/api/v1/system-event-logs` (search) | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Verified nationwide admin OR SQL-scoped to caller tenant |
| `/api/v1/system-event-logs?categoryId=...` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Filtered within caller tenant scope |
| `/api/v1/system-event-logs?severityId=...` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Filtered within caller tenant scope |
| `/api/v1/system-event-logs?eventCode=...` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Filtered within caller tenant scope |
| `/api/v1/system-event-logs?actorUserId=...` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Filtered within caller tenant scope |
| `/api/v1/system-event-logs?entityType=...&entityId=...` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Verified nationwide admin OR entity-level ownership validation |
| `/api/v1/system-event-logs?entityType=...` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Filtered within caller tenant scope |
| `/api/v1/system-event-logs?sourceComponent=...` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Filtered within caller tenant scope |
| `/api/v1/system-event-logs?start=...&end=...` | `GET` | `audit.read` \| `system.manage` \| `system.event.read` \| `system.event.manage` | Filtered within caller tenant scope |
| `/api/v1/system-event-logs` | `POST` | `system.manage` \| `system.event.manage` | Validates caller scope, sanitizes summary, prevents forged correlation tags |
| `/api/v1/data-quality-rules/{id}` | `GET` | `audit.read` \| `system.manage` | Global catalog read (permitted for all authorized audit/system users) |
| `/api/v1/data-quality-rules` | `POST` | `system.manage` | Strict administrative privilege separation (`audit.read` alone denied) |
| `/api/v1/data-quality-rules/{id}` | `PUT` | `system.manage` | Strict administrative privilege separation |
| `/api/v1/data-quality-rules/{id}` | `DELETE` | `system.manage` | Strict administrative privilege separation |
| `/api/v1/data-quality/scan` | `POST` | `audit.read` \| `system.manage` | Tenant-scoped anomaly detection scan |
| `/api/v1/data-quality-issues/{id}` | `GET` | `audit.read` \| `system.manage` | Enforces tenant scope against issue's underlying entity |
| `/api/v1/data-quality-issues` (query params) | `GET` | `audit.read` \| `system.manage` | Filtered within caller tenant scope via `filterIssuesInScope` |
| `/api/v1/data-quality-issues` | `POST` | `system.manage` | Enforces tenant ownership of referenced entity |
| `/api/v1/data-quality-issues/{id}` | `PUT` | `system.manage` | Enforces tenant ownership before issue mutation |
| `/api/v1/data-quality-issues/{id}/assign` | `POST` | `system.manage` | Enforces assignee eligibility and tenant alignment |
| `/api/v1/data-quality-issues/{id}/start-investigation` | `POST` | `system.manage` | Enforces tenant ownership of issue |
| `/api/v1/data-quality-issues/{id}/resolve` | `POST` | `system.manage` | Enforces tenant ownership; masks resolution notes at write time |
| `/api/v1/data-quality-issues/{id}/dismiss` | `POST` | `system.manage` | Enforces tenant ownership; masks dismissal remarks at write time |
| `/api/v1/data-quality-issues/{id}/reopen` | `POST` | `system.manage` | Enforces tenant ownership |
| `/api/v1/data-quality-issues/{id}/events` | `GET` | `audit.read` \| `system.manage` | Enforces tenant ownership of underlying issue |
| `/api/v1/data-quality-issue-events/{id}` | `GET` | `audit.read` \| `system.manage` | Enforces tenant ownership of underlying issue |
| `/api/v1/data-quality-issue-events` (query params) | `GET` | `audit.read` \| `system.manage` | Filtered within caller tenant scope via `filterIssueEventsInScope` |
| `/api/v1/data-quality-issue-events` | `POST` | `system.manage` | Requires `system.manage`; validates issue scope; masks remarks at write time |
| `/api/v1/import-batches/{id}` | `GET` | Ingestion read authorities | Enforces batch tenant access (`validateCallerCanAccessBatch`) |
| `/api/v1/import-batches` (query params) | `GET` | Ingestion read authorities | SQL-scoped via `appendBatchTenantWhereClause` + `LIMIT 100` |
| `/api/v1/import-batches` (upload) | `POST` | Ingestion write authorities | Enforces upload persona boundaries (`resolveAndValidateUploadOrProcessCaller`) |
| `/api/v1/import-batches/{id}/process` | `POST` | `system.manage` \| `trainee.write` \| `placement.manage` | Validates batch ownership; blocks cross-tenant execution |
| `/api/v1/import-batches/{id}/recover` | `POST` | `system.manage` \| `trainee.write` \| `placement.manage` | Validates batch ownership; requires confirmed terminated worker |
| `/api/v1/import-batches/{id}/progress` | `GET` | Ingestion read authorities | Enforces batch ownership before reporting counters |
| `/api/v1/import-batches/{id}/records` | `GET` | Ingestion read authorities | Enforces batch ownership; paginated bounded records |
| `/api/v1/import-batches/{id}/quarantine` | `GET` | Ingestion read authorities | Enforces batch ownership; paginated bounded quarantine |
| `/api/v1/import-records/{id}` | `GET` | Ingestion read authorities | Enforces batch ownership |
| `/api/v1/import-records?batchId=...&sourceRowNumber=...` | `GET` | Ingestion read authorities | Enforces batch ownership |
| `/api/v1/import-records?batchId=...` | `GET` | Ingestion read authorities | Enforces batch ownership; bounded pagination |
| `/api/v1/import-records?statusId=...` | `GET` | Ingestion read authorities | SQL-scoped to caller tenant batches (`queryScopedRecordsPage`) |
| `/api/v1/import-records?entityType=...&entityId=...` | `GET` | Ingestion read authorities | Enforces batch/entity ownership |
| `/api/v1/operational/data-quality-scorecard` | `GET` | Operational analytics authorities | Authoritative tenant-scoped aggregation (`OperationalDashboardQueryRepository`) |
| `/api/v1/operational/import-metrics` | `GET` | Operational analytics authorities | Authoritative tenant-scoped aggregation |
| `/api/v1/operational/system-health` | `GET` | Operational analytics authorities | Authoritative tenant-scoped aggregation |
| `/api/v1/training-centers/{id}` | `GET` | `center.manage` | Enforces provider ownership (cross-provider denied even in same state/district) |
| `/api/v1/training-centers` | `GET` | `center.manage` | SQL-scoped to caller provider/state/district before pagination/count |
| `/api/v1/training-centers` | `POST` | `center.manage` | Validates caller ownership of center provider, state, and district |
| `/api/v1/training-centers/{id}` | `PUT` | `center.manage` | Validates caller ownership before center mutation |
| `/api/v1/training-centers/{id}` | `DELETE` | `center.manage` | Validates caller ownership before soft deletion |
| `/api/v1/training-batches/{id}` | `GET` | `course.manage` | Enforces provider and center ownership (cross-provider denied) |
| `/api/v1/training-batches` | `GET` | `course.manage` | SQL-scoped to caller provider/state/district before pagination/count |
| `/api/v1/training-batches` | `POST` | `course.manage` | Validates caller ownership of batch provider and center |
| `/api/v1/training-batches/{id}` | `PUT` | `course.manage` | Validates caller ownership before batch mutation |
| `/api/v1/training-batches/{id}/status` | `PUT` | `course.manage` | Validates caller ownership before lifecycle status change |
| `/api/v1/training-batches/{id}` | `DELETE` | `course.manage` | Validates caller ownership before soft deletion |
| `/api/v1/courses/{id}` | `GET` | `course.manage` | Global catalog read (courses table is unowned; batches are scoped) |
| `/api/v1/courses` | `GET` | `course.manage` | Global catalog read |

---

## 5. WorkflowSecurityScopeValidator Integration

The existing `WorkflowSecurityScopeValidator` was reused without creating competing authentication or authorization abstractions:
- Integrated directly into `AuditLogServiceImpl`, `SystemEventLogServiceImpl`, `DataQualityIssueScopeSupport`, `DataQualityIssueEventServiceImpl`, `ImportSecurityScopeValidator`, and `TrainingTenantScopeSupport`.
- Centralizes authoritative user entity lookup (`resolveAuthenticatedUser()`) from the database, failing closed if the user record does not exist or is inactive/deleted (`user_status_id != 1` or `deleted_at IS NOT NULL`).
- Standardizes persona scope validation:
  - `validateDashboardProviderScope(providerId)`: throws `AccessDeniedException` if caller is not affiliated with the specified training provider.
  - `validateDashboardEmployerScope(employerId)`: throws `AccessDeniedException` if caller is not affiliated with the specified employer.
  - `validateDashboardTraineeScope(traineeId)`: throws `AccessDeniedException` if caller is not the authenticated trainee.
  - `validateStateScope(stateId)`: validates that a state administrator's assigned `state_id` matches the target entity state.

---

## 6. National Admin Verification

Nationwide access across operational and domain endpoints is restricted to verified nationwide administrative callers:
- `WorkflowSecurityScopeValidator.isNationwideAdmin()` validates:
  1. Caller holds explicit `ROLE_super_admin` or `ROLE_government_admin`.
  2. Authoritative database record in `users` has `state_id IS NULL`, `district_id IS NULL`, `training_provider_id IS NULL`, and `employer_id IS NULL`.
  3. Caller user is not affiliated with any trainee record (`traineeRepository.findByUserId(userId).isEmpty()`).
- Callers holding `ROLE_government_admin` or `ROLE_super_admin` who possess a non-null `state_id`, `district_id`, or `training_provider_id` are classified as scoped administrators and are denied unrestricted nationwide access (`403 Forbidden`).
- Unscoped actor/action/entity-type audits (`/api/v1/audit-logs?actorUserId=...`, `?auditActionId=...`, `?entityType=...`, `?correlationId=...`) strictly verify `isCallerNationwideAdmin()` and reject non-nationwide callers with HTTP `403`.

---

## 7. State and District Isolation

Strict geographical boundaries are enforced across all operational queries and core training resources:
- **State Isolation:** Callers with `ROLE_state_admin` and assigned `state_id` can only read and query entities within their state. Access attempts to entities in another state (e.g. State A querying State B audit logs, training centers, batches, or system events) return HTTP `403 Forbidden`.
- **District Officers:** Callers with `ROLE_district_officer` or `ROLE_government_officer` and assigned `district_id` are restricted strictly to their assigned district.
- **Audit Log Verification (`validateEntityAccess`):** `AuditLogServiceImpl.validateStateOrDistrictAdminAccess` verifies that district officers can only access audit entries for entities matching their assigned `district_id`.
- **Operational Dashboards & Query Repositories:** Embed `state_id = :stateId` and `district_id = :districtId` directly in SQL queries, guaranteeing zero cross-district leakage.

---

## 8. Provider Center and Course Ownership

Training providers are isolated by organizational boundaries rather than geographical boundaries alone:
- **Same-State / Same-District Provider Isolation:** In `TrainingTenantScopeSupport`, `TrainingCenterServiceImpl`, and `TrainingBatchServiceImpl`, a training provider (e.g. Provider A1) is restricted strictly to centers and batches where `provider_id = callerScope.trainingProviderId()`. Attempts by Provider A1 to access Provider A2's centers or batches—even when both providers operate within the exact same state and district—return HTTP `403 Forbidden`.
- **Center Ownership on Batch Operations:** Creating or updating a training batch requires validating that both the batch's `provider_id` and the associated training center's `provider_id` match the caller's authorized provider ID (`validateBatchOwnership`).
- **Course Catalog vs Batches:** `CourseController` manages the global qualification catalog (`courses` table has no `provider_id`). Batches associated with courses (`GET /api/v1/training-batches?courseId=...`) are strictly filtered by caller provider ID, preventing providers from viewing competitor batch schedules for the same course.

---

## 9. Employer and Trainee Handling

- **Employer Persona:** Callers holding `ROLE_employer` with `employer_id` are restricted to employer-scoped entities (`placement_records`, `employment_records`, `employment_verifications`). Attempts by employer callers to access training centers, training batches, or bulk-import trainee records return HTTP `403 Forbidden`.
- **Trainee Persona:** Trainee callers are restricted to their own individual profile and enrollments (`validateDashboardTraineeScope`). Attempts to query administrative collections, training centers, training batches, or generic audit logs return HTTP `403 Forbidden`.

---

## 10. Unscoped Caller Rejection

Callers with valid authentication but missing or ambiguous organizational context fail closed:
- **Missing Tenancy:** Callers with administrative roles but missing required tenancy assignments (e.g., a state admin with `state_id = NULL`, or a district officer with `district_id = NULL`) fail validation and receive HTTP `403 Forbidden`.
- **Ambiguous / Conflicting Tenancy:** Callers possessing conflicting tenancy attributes (e.g., both `state_id` and `training_provider_id`, or both `training_provider_id` and `employer_id`) are rejected with HTTP `403 Forbidden`.
- **Deleted / Inactive Accounts:** Users with `deleted_at IS NOT NULL` or `user_status_id != 1` are immediately rejected with HTTP `403 Forbidden`.

---

## 11. SQL and Service-Level Tenant Isolation

Tenant filtering is applied at the SQL query level before pagination offsets and aggregations are computed:
- **Audit Logs:** `AuditLogQueryRepository.appendTenantPredicates` injects entity-specific join and `WHERE` clauses for training centers, training batches, trainees, placements, and import batches.
- **System Event Logs:** `SystemEventLogQueryRepository.appendTenantPredicates` joins `training_centers tc` on `entity_id` when `entity_type = 'TrainingCenter'`, restricting provider callers to `tc.provider_id = ?`.
- **Import Batches:** `ImportBatchServiceImpl.getBatchesBy*` uses `findScopedBatches` with `appendBatchTenantWhereClause`, retrieving scoped batch IDs via SQL before hydration.
- **Training Centers & Batches:** `TrainingCenterServiceImpl.queryScopedCentersPage` and `TrainingBatchServiceImpl.queryScopedBatchesPage` execute `SELECT COUNT(*)` and `SELECT id ... LIMIT ? OFFSET ?` with tenant predicates (`tc.provider_id = ?`, `tc.state_id = ?`), guaranteeing that `totalElements` and page contents are strictly tenant-isolated.

---

## 12. AuditPayloadMasker Design

`in.gov.sih.sih26135.security.AuditPayloadMasker` is a thread-safe, Spring-managed `@Component` providing static masking methods:
- **Method Signatures:**
  - `mask(String input)`: masks credentials, tokens, Aadhaar, and PAN using default max length (8,192).
  - `maskBounded(String input, int maxLength)`: masks and strictly truncates output to `maxLength`.
  - `maskPreservingCorrelation(String input)`: extracts leading `[cid=...|rid=...]` or `[cid=...]` prefix, masks the body, and reattaches the prefix.
  - `maskPreservingCorrelationBounded(String input, int maxLength)`: correlation-preserving with strict length bounding.
  - `maskPlainText(String text)`: applies regex-based credential and PII sanitization to unstructured text.
- **JSON Traversal:** Detects JSON objects (`{...}`) and arrays (`[...]`), parses into a Jackson `JsonNode` tree, traverses up to depth 16, replaces values of sensitive keys with `[REDACTED]`, and recursively masks text nodes for embedded Aadhaar/PAN.
- **Malformed JSON Fallback:** If JSON parsing fails (e.g. truncated or invalid JSON), the masker safely falls back to `maskPlainText` without throwing exceptions or leaking secrets.
- **Idempotency:** Applying `mask()` multiple times produces identical output without creating recursive redactions like `[[REDACTED]]` or `XXXX-XXXX-XXXX-XXXX-1234`.

---

## 13. Credential and Token Redaction

The following credential formats are redacted with `[REDACTED]`:
- **Passwords & Hashes:** Fields matching `password`, `passwd`, `pwd`, `password_hash`, `new_password`, `old_password`; bcrypt hashes (`$2a$`, `$2b$`, `$2y$`) and Argon2 hashes (`$argon2id$`, `$argon2i$`, `$argon2d$`).
- **JWTs:** Strings matching the RFC 7519 structure `eyJ...` with header, payload, and signature segments.
- **Authorization Tokens:** `Bearer <token>` and `Basic <token>` authorization headers.
- **API Keys & Secrets:** `apiKey`, `clientSecret`, `secretKey`, `accessToken`, `refreshToken`, `idToken`, `authToken`, `privateKey`, `passphrase`.
- **JDBC Connection Strings:** URLs matching `jdbc:[a-z0-9:]+://...`.

---

## 14. Aadhaar Masking

Aadhaar numbers are masked by replacing the first 8 digits with `XXXX-XXXX-` while preserving the final 4 digits (`XXXX-XXXX-1234`):
- **Hyphenated:** `1234-5678-1234` -> `XXXX-XXXX-1234`
- **Whitespace-Separated:** `2345 6789 0123` -> `XXXX-XXXX-0123`
- **Continuous (12 digits):** `345678901234` -> `XXXX-XXXX-1234`
- **Labeled Aadhaar:** `aadhaar: 109876543210` -> `aadhaar: XXXX-XXXX-3210`
- **Non-Interference:** Standard 10-digit phone numbers and timestamps remain unmodified.

---

## 15. PAN Masking

Permanent Account Number (PAN) identifiers (5 uppercase letters, 4 digits, 1 uppercase letter) are masked as `XXXXXX123X`, masking the first 5 letters and 4th digit while preserving digits 1–3:
- **Standard Format:** `ABCDE1234F` -> `XXXXXX123X`
- **Labeled Format:** `pan_no: pqrst5678z` -> `pan_no: XXXXXX567X`
- **Non-Interference:** Ordinary English words, batch codes (`BAT-9741`), and hex strings remain untouched.

---

## 16. Write-Time Sanitization

Masking is applied at the point of ingestion and write-time persistence:
- **Audit Logs:** `AuditLogServiceImpl.recordAuditLog` and `recordInternalAuditLog` apply `AuditPayloadMasker.maskBounded(..., 1000)` to `changeSummary` before entity persistence.
- **System Events:** `SystemEventSanitizer.sanitizeMessage` invokes `AuditPayloadMasker.maskBounded(..., 500)` before formatting with canonical server correlation headers.
- **Data Quality Issues:** `DataQualityIssueServiceImpl.reportIssue` and `updateIssue` mask `issueSummary`.
- **Issue Workflow & Events:** `DataQualityWorkflowServiceImpl` and `DataQualityIssueEventServiceImpl` mask `resolutionNotes` and `remarks`.

---

## 17. Read-Time Sanitization

Read-time mapping ensures that responses returned by the API never expose unmasked sensitive data:
- **Entity Mappers:**
  - `AuditLogMapper`: `AuditPayloadMasker.mask(entity.getChangeSummary())`
  - `SystemEventLogMapper`: `AuditPayloadMasker.maskPreservingCorrelationBounded(entity.getEventSummary(), 500)`
  - `DataQualityIssueMapper`: `AuditPayloadMasker.mask(...)` on `issueSummary` and `resolutionNotes`
  - `DataQualityIssueEventMapper`: `AuditPayloadMasker.mask(...)` on `remarks`
  - `ImportRecordMapper`: `AuditPayloadMasker.mask(...)` on `errorMessage`
- **Query Repositories:** `AuditLogQueryRepository` and `SystemEventLogQueryRepository` apply `AuditPayloadMasker` directly inside JDBC `RowMapper` implementations for paginated searches and entity timeline reconstructions.

---

## 18. Historical Audit Privacy

The platform enforces database immutability while guaranteeing privacy on read:
- **No In-Place Modification of Historical Records:** `audit_logs` is an immutable, append-only table. No `UPDATE` or `DELETE` statements are executed against historical audit rows.
- **Sanitized Projection:** When historical audit records containing plain-text passwords, tokens, Aadhaar, or PAN are fetched via `GET /api/v1/audit-logs/{id}`, paginated search, or timeline API, `AuditPayloadMasker` redacts them dynamically in the DTO response.
- **Direct Database Invariant:** Verified in `OperationalHardeningSecurityAndPrivacyTest.masksHistoricalUnmaskedRowsWithoutMutatingDb`: the underlying row in MySQL retains its original historical bits for forensic non-repudiation while the REST response returned to clients contains `[REDACTED]`, `XXXX-XXXX-0123`, and `XXXXXX123X`.

---

## 19. Pagination Audit

All collection endpoints across the operational and domain controllers were audited and hardened:
- **Bounds Validation:** All paginated endpoints enforce `@Min(0) int page` and `@Min(1) @Max(100) int size`. Requests with `page < 0`, `size < 1`, or `size > 100` are rejected with HTTP `400 Bad Request` (`INVALID_PAGE` or `INVALID_PAGE_SIZE`).
- **Sort Allowlisting:** Sort fields are validated against strict controller-specific allowlists in `PaginationUtils`. Disallowed or unknown sort properties are rejected with HTTP `400` (`INVALID_SORT_FIELD`).
- **Stable Tie-Breaking:** All paginated queries append an authoritative primary-key tie-breaker (`id ASC` or `id DESC`) to prevent pagination drift across concurrent inserts.
- **Large Page Short-Circuiting:** In `AuditLogQueryRepository`, `SystemEventLogQueryRepository`, `ImportRecordServiceImpl`, `TrainingCenterServiceImpl`, and `TrainingBatchServiceImpl`, queries short-circuit when `total == 0 || pageable.getOffset() >= total`, returning an empty page without executing unbounded database scans.
- **Hibernate Integer Overflow Protection:** `PaginationUtils.executeSafeJpaPage` handles extremely large page indices (`page = 2147483647`) where `getOffset() > Integer.MAX_VALUE`, avoiding Hibernate `Query.setFirstResult(int)` overflow exceptions.

---

## 20. Changed Files

### Modified Existing Files (21):
1. `backend/src/main/java/in/gov/sih/sih26135/controller/DataQualityRuleController.java`: Added `@PreAuthorize("hasAuthority('system.manage')")` on `POST /`, `PUT /{id}`, `DELETE /{id}`.
2. `backend/src/main/java/in/gov/sih/sih26135/controller/DataQualityIssueEventController.java`: Added `@PreAuthorize("hasAuthority('system.manage')")` on `POST /`.
3. `backend/src/main/java/in/gov/sih/sih26135/mapper/AuditLogMapper.java`: Applied `AuditPayloadMasker.mask` to `changeSummary`.
4. `backend/src/main/java/in/gov/sih/sih26135/mapper/SystemEventLogMapper.java`: Applied `AuditPayloadMasker.maskPreservingCorrelationBounded` to `eventSummary`.
5. `backend/src/main/java/in/gov/sih/sih26135/mapper/DataQualityIssueMapper.java`: Applied `AuditPayloadMasker.mask` to `issueSummary` and `resolutionNotes`.
6. `backend/src/main/java/in/gov/sih/sih26135/mapper/DataQualityIssueEventMapper.java`: Applied `AuditPayloadMasker.mask` to `remarks`.
7. `backend/src/main/java/in/gov/sih/sih26135/mapper/ImportRecordMapper.java`: Applied `AuditPayloadMasker.mask` to `errorMessage`.
8. `backend/src/main/java/in/gov/sih/sih26135/repository/AuditLogQueryRepository.java`: Integrated `AuditPayloadMasker` into `RowMapper` and timeline reconstruction; added large-page short-circuiting.
9. `backend/src/main/java/in/gov/sih/sih26135/repository/SystemEventLogQueryRepository.java`: Integrated `AuditPayloadMasker`; normalized import batch entity type; added training center tenant joins and short-circuiting.
10. `backend/src/main/java/in/gov/sih/sih26135/response/PaginationUtils.java`: Added `executeSafeJpaPage` helper for overflow-safe JPA pagination.
11. `backend/src/main/java/in/gov/sih/sih26135/service/event/SystemEventSanitizer.java`: Integrated `AuditPayloadMasker` into event message sanitization.
12. `backend/src/main/java/in/gov/sih/sih26135/service/impl/AuditLogServiceImpl.java`: Added provider/district scope validation in `validateEntityAccess`; wired write-time masking; added training centers/batches/import batches to timeline allowlist.
13. `backend/src/main/java/in/gov/sih/sih26135/service/impl/DataQualityIssueEventServiceImpl.java`: Wired `WorkflowSecurityScopeValidator` and tenant scope validation; applied write-time masking to remarks.
14. `backend/src/main/java/in/gov/sih/sih26135/service/impl/DataQualityIssueScopeSupport.java`: Added `filterIssueEventsInScope` and `validateCallerScopeForIssue`.
15. `backend/src/main/java/in/gov/sih/sih26135/service/impl/DataQualityIssueServiceImpl.java`: Integrated write-time masking for `issueSummary`.
16. `backend/src/main/java/in/gov/sih/sih26135/service/impl/DataQualityWorkflowServiceImpl.java`: Integrated write-time masking for `resolutionNotes` and event `remarks`.
17. `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportBatchProcessingWorkflowImpl.java`: Applied `PaginationUtils.executeSafeJpaPage` to batch records and quarantine pagination.
18. `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportBatchServiceImpl.java`: Replaced in-memory filtering with SQL-level scoping (`findScopedBatches`) and bounded limit.
19. `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportRecordServiceImpl.java`: Applied `PaginationUtils.executeSafeJpaPage` to JPA queries.
20. `backend/src/main/java/in/gov/sih/sih26135/service/impl/TrainingBatchServiceImpl.java`: Wired `TrainingTenantScopeSupport` and SQL-level tenant isolation across all query and mutation methods.
21. `backend/src/main/java/in/gov/sih/sih26135/service/impl/TrainingCenterServiceImpl.java`: Wired `TrainingTenantScopeSupport` and SQL-level tenant isolation across all query and mutation methods.

### New Production Files (2):
1. `backend/src/main/java/in/gov/sih/sih26135/security/AuditPayloadMasker.java`: Centralized, null-safe, idempotent, length-bounded PII and credential masker.
2. `backend/src/main/java/in/gov/sih/sih26135/service/impl/TrainingTenantScopeSupport.java`: Reusable fail-closed tenancy scope resolver and ownership validator for training centers and batches.

### New Test Files (2):
1. `backend/src/test/java/in/gov/sih/sih26135/security/AuditPayloadMaskerUnitTest.java`: Comprehensive unit tests for credentials, hashes, JWTs, Aadhaar, PAN, nested JSON, malformed JSON, idempotency, and correlation preservation (5 tests).
2. `backend/src/test/java/in/gov/sih/sih26135/security/OperationalHardeningSecurityAndPrivacyTest.java`: Adversarial integration test suite covering role-based tenancy isolation, write-time sanitization, historical read-time privacy, and collection pagination hardening (8 tests).

---

## 21. Adversarial Security Tests

`OperationalHardeningSecurityAndPrivacyTest` verifies security boundaries against adversarial scenarios:
- **Invalid Caller States:** Verified rejection of unauthenticated requests (`401`), deleted users (`403`), unscoped users (`403`), ambiguous users with conflicting tenancy (`403`), and scoped government admins claiming nationwide access (`403`).
- **Same-State Provider Isolation:** Provider A1 and Provider A2 reside in the same state and district. Verified Provider A1 is denied access (`403`) to Provider A2's centers and batches, while paginated lists include only Provider A1's records.
- **Privilege Separation:** Read-only state analysts with `audit.read` are blocked (`403`) from modifying data quality rules or recording manual issue events.
- **Correlation Cross-Tenant Isolation:** System event queries filtering by `correlationId` return only events belonging to the caller's tenant.
- **Historical Audit Immutability:** Historical audit records with raw credentials/PII return sanitized responses while the underlying database row in `audit_logs` remains unmodified.
- **Write-Time Sanitization:** Newly created audit logs and issue events persist sanitized payloads to the database.

---

## 22. Focused Test Results

The focused Phase 28.8 and Phase 26–28 test battery was executed against `SIH26135_TEST`:

```text
mvn test -f E:\project\backend\pom.xml -Dtest=AuditPayloadMaskerUnitTest,OperationalHardeningSecurityAndPrivacyTest,SystemEventDiagnosticsUnitTest,CoreDomainResourcePaginationTest
```

**Result:**
- `AuditPayloadMaskerUnitTest`: 5 tests, 0 failures, 0 errors, 0 skipped
- `OperationalHardeningSecurityAndPrivacyTest`: 8 tests, 0 failures, 0 errors, 0 skipped
- `SystemEventDiagnosticsUnitTest`: 5 tests, 0 failures, 0 errors, 0 skipped
- `CoreDomainResourcePaginationTest`: 27 tests, 0 failures, 0 errors, 0 skipped
- **Total:** `45` tests run, `0` failures, `0` errors, `0` skipped (`BUILD SUCCESS`, `Total time: 01:16 min`).

---

## 23. Full Regression Results

The complete backend regression test suite was executed against dedicated test database `SIH26135_TEST`:

```text
mvn test -f E:\project\backend\pom.xml
```

**Execution Summary:**
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
...
[INFO] Tests run: 1972, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  05:47 min
[INFO] Finished at: 2026-11-12T22:37:22+05:30
[INFO] ------------------------------------------------------------------------
```

**Net Difference:** `1,972` tests run (`+13` tests over the Phase 28.7 baseline of `1,959` tests, exactly matching the 13 new Phase 28.8 tests). Zero failures, zero errors, and zero skipped tests.

---

## 24. Database Isolation

1. **Target Catalog:** All automated test executions strictly connected to `SIH26135_TEST` (`spring.datasource.url` configured in `src/test/resources/application.properties`).
2. **Catalog Verification:** Every integration test suite verified catalog isolation via `MysqlConnectionTest.assertDedicatedTestCatalog(catalog)`.
3. **Production Database Untouched:** Production catalog `SIH26135` was never connected to, read, or modified.
4. **DDL Freezing:** Hibernate DDL validation was strictly enforced (`spring.jpa.hibernate.ddl-auto=validate`). Zero schema alterations or DDL statements were executed.

---

## 25. Architectural Invariants

| Invariant Metric | Baseline (Phase 28.7) | Observed (Phase 28.8) | Status |
|:---|:---:|:---:|:---:|
| Spring Data JPA Repositories | 137 | 137 | **PRESERVED** |
| `@Query` Annotations in `src/main` | 2 | 2 | **PRESERVED** |
| `@RestController` Classes | 64 | 64 | **PRESERVED** |
| Audited Endpoint Mappings | 668 | 668 | **PRESERVED** |
| DDL Auto Configuration | `validate` | `validate` | **PRESERVED** |
| Test Database Isolation | `SIH26135_TEST` | `SIH26135_TEST` | **PRESERVED** |
| Git Branch | `main` | `main` | **PRESERVED** |

---

## 26. Git Hygiene

Read-only Git inspection confirmed clean working tree hygiene without forbidden Git mutations:
- `git branch --show-current`: `main`
- `git diff --check`: Exit code `0` (clean, no whitespace errors or trailing carriage returns)
- `git diff --cached --check`: Exit code `0`
- Zero Git mutations executed (`git add`, `git commit`, `git push`, `git reset`, `git checkout`, `git stash`, `git clean` were never executed).
- File changes are strictly confined to the 21 modified files and 4 new files documented in Section 20.

---

## 27. Known Limitations

1. **Pre-Existing Historical Audit Immutability:** Historical audit records created prior to Phase 28.8 retain their original physical storage in `audit_logs` to maintain cryptographic and audit compliance. Protection for historical rows is guaranteed at read time via mappers and query repositories.
2. **Course Catalog Decoupling:** The `courses` platform catalog has no `provider_id` column by design (courses are educational standards). Provider isolation on courses is enforced at the batch and enrollment level (`training_batches`, `training_enrollments`).
3. **Fixed Offset Pagination Limits:** Extremely deep page requests (`page > 2,000,000`) safely short-circuit to empty results without integer overflow, but database-level performance for deep offsets is bounded by standard B-tree index traversals.

---

## 28. Acceptance Checklist

- [x] Operational endpoint security inventory completed.
- [x] Existing `WorkflowSecurityScopeValidator` reused without competing auth frameworks.
- [x] Nationwide access restricted to verified nationwide admins (`isNationwideAdmin`).
- [x] State/district isolation enforced across operational and domain endpoints.
- [x] Provider center/batch ownership enforced.
- [x] Same-state cross-provider leakage prevented.
- [x] Existing employer permissions preserved.
- [x] Unscoped callers rejected with HTTP 403.
- [x] Tenant restrictions applied before pagination and aggregation in SQL.
- [x] `AuditPayloadMasker` implemented (null-safe, idempotent, bounded, correlation-preserving).
- [x] Credentials and tokens redacted with `[REDACTED]`.
- [x] Aadhaar masked as `XXXX-XXXX-1234`.
- [x] PAN masked as `XXXXXX123X`.
- [x] Relevant audit summaries and remarks protected.
- [x] Historical sensitive responses masked without mutating immutable records.
- [x] Correlation metadata (`[cid=...|rid=...]`) preserved.
- [x] Collection pagination bounded to 1–100 with valid `page >= 0`.
- [x] Invalid pagination rejected with HTTP 400.
- [x] Existing API contracts preserved (`64` controllers, `668` endpoints).
- [x] Phase 28.1–28.7 functionality preserved.
- [x] Adversarial tests passed.
- [x] Full regression passed (`1,972` tests, `0` failures, `0` errors).
- [x] Frozen schema preserved (`ddl-auto=validate`).
- [x] No production database access (`SIH26135_TEST` only).
- [x] No Git mutations executed.
- [x] Both reports synchronized.

---

## 29. Final Verdict

**READY FOR INDEPENDENT REVIEW**

Phase 28.8 successfully implements comprehensive tenant isolation, sensitive data sanitization, and pagination hardening across all operational and core domain components. All 1,972 tests pass with zero failures and zero errors against `SIH26135_TEST`, preserving all architectural invariants and the frozen database schema.
