# Phase 28.5 — Bulk Data Ingestion Pipeline, Row Quarantine & Chunked Batch Processing Report

## 1. Executive Summary

Phase 28.5 delivers a production-oriented, memory-bounded, tenant-secure bulk data ingestion pipeline for staged CSV, JSON array, and NDJSON uploads targeting `Trainee` and `PlacementRecord` entities in the SIH-26135 Spring Boot backend (`E:\project\backend`, branch `main`).

All implementation work and subsequent critical integrity, tenant-security, and crash-recovery corrections operate strictly within the frozen MySQL 8 schema (`ddl-auto=validate`) without adding tables, columns, DDL migrations, or bypassing frozen Phases 27 and 28.1–28.4 contracts. After final edits, the focused verification run passed (`82 tests, 0 failures, 0 errors, 0 skipped`) and the complete backend regression suite passed against `SIH26135_TEST` (**1,935 tests run, 0 failures, 0 errors, 0 skipped**).

**Final Verdict:** **READY FOR INDEPENDENT REVIEW**

---

## 2. Existing Architecture Inspection

Inspection of the pre-existing Phase 23 / 28.1 ingestion foundation established:

- **Controllers:** `ImportBatchController` (`/api/v1/import-batches`) and `ImportRecordController` (`/api/v1/import-records`) previously exposed only synchronous single-record JSON CRUD endpoints without multipart upload handling, execution orchestration, progress reporting, quarantine retrieval, or tenant scoping.
- **Services:** `ImportBatchServiceImpl` and `ImportRecordServiceImpl` omitted `ImportSecurityScopeValidator` tenancy checks, returned unbounded `List<ImportRecordResponse>` results on record queries, and omitted explicit `createdAt` assignment on `createImportBatch` / `createImportRecord` (despite `nullable = false` columns and no `@PrePersist` lifecycle callbacks).
- **Workflow Contract:** No `ImportBatchProcessingWorkflow` implementation existed prior to Phase 28.5; `BulkImportInitiateRequest` / `BulkImportSummaryResponse` had only DTO baseline coverage from Phase 28.1.
- **Exception Handling:** `GlobalExceptionHandler` lacked explicit `@ExceptionHandler` mappings for `MaxUploadSizeExceededException`, `MissingServletRequestPartException`, and `MultipartException`, and `application.properties` lacked `spring.servlet.multipart` and `import-pipeline` bounds.

---

## 3. Actual Schema and Status Compatibility

Inspected directly against `phase-15-audit-data-quality-logs.sql`, entity mappings (`ImportBatch`, `ImportRecord`), and `SIH26135_TEST` metadata:

- **`ref_import_batch_status`:** `(1, PENDING, is_terminal_flag=0)`, `(2, RUNNING, 0)`, `(3, COMPLETED, 1)`, `(4, FAILED, 1)`, `(5, CANCELLED, 1)`. `RUNNING` provides atomic claim state and terminal flags block post-completion mutation/reprocessing.
- **`ref_import_record_status`:** `(1, SUCCESS)`, `(2, FAILED)`, `(3, SKIPPED)`, `(4, DUPLICATE)`.
- **`import_batches`:** Stores `import_batch_id`, `batch_code` (`VARCHAR(32)`, unique `uk_import_batches_code`), `source_system`, `entity_type`, `import_batch_status_id`, `initiated_by_user_id`, `row_count`, `success_count`, `failure_count`, `started_at`, `completed_at`, `created_at`, `updated_at`, plus `CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)`.
  - **Frozen Schema Compatibility Note:** `import_batches` has neither a `staged_file_path` column nor a standalone `tenant_id` column.
  - **Implemented Compatible Design:** Staged files are resolved deterministically server-side by server-generated unique `batch_code` (`IMP-YYYYMMDD-<16HEX>`) inside server-controlled `import-pipeline.staging-dir`; tenant ownership is authoritatively derived from `initiated_by_user_id -> users.(state_id, district_id, training_provider_id, employer_id)`.
- **`import_records`:** Stores `import_record_id`, `import_batch_id`, `source_row_number` (`CHECK (source_row_number >= 1)`, unique `uk_import_records_batch_row`), `entity_type`, `entity_id`, `import_record_status_id`, `error_code` (`VARCHAR(64)`), `error_message` (`VARCHAR(500)`), `created_at`, `updated_at`.

---

## 4. Upload and Staging Architecture

`ImportBatchProcessingWorkflow.stageUpload(...)` and `ImportFileStagingService` execute staged uploads synchronously only for I/O validation and durable disk staging (never running bulk entity persistence inside the HTTP upload call):

1. Authenticates caller, verifies entity-specific write authorization (`system.manage`, `trainee.write`, or `placement.manage`), rejects `Trainee` uploads from employers (`403`), and rejects trainees/unscoped/conflicting users (`403`).
2. Rejects empty/whitespace-only payloads (`400 EMPTY_IMPORT_FILE`), missing `file` part (`400 MISSING_FILE_PART`), malformed multipart (`400 MALFORMED_MULTIPART_REQUEST`), unsupported extensions (`400 UNSUPPORTED_FILE_FORMAT`), and oversized files (`413 FILE_SIZE_LIMIT_EXCEEDED` at servlet layer `25MB`, `400 FILE_SIZE_LIMIT_EXCEEDED` at configurable streaming cap default `10485760` bytes / 10 MiB).
3. Generates collision-resistant server `batchCode` (`IMP-YYYYMMDD-<16HEX>`, max 29 chars <= 32) checked against `importBatchRepository.existsByBatchCode(...)`.
4. Streams `file.getInputStream()` in 8 KiB buffered chunks to a `.part` temp file inside normalized `import-pipeline.staging-dir`, enforces canonical child-path confinement (preventing path traversal), and atomically moves `.part` to final `<batchCode>.csv` / `<batchCode>.json`. Never calls `MultipartFile.getBytes()` and ignores client filesystem paths.
5. Registers `ImportBatch` in `PENDING` (`rowCount=0, successCount=0, failureCount=0`) inside a dedicated `REQUIRES_NEW` transaction. If persistence or staging verification fails, `deleteStagedFileQuietly(...)` deletes staged files so no orphan artifacts or false `PENDING` responses remain.

---

## 5. Streaming Parser Implementation

`ImportPipelineProperties` configures bounded defaults (`chunkSize=100`, `maxFileSizeBytes=10485760`, `maxRowsPerBatch=10000`, `maxRowBytes=65536`). Both pull parsers implement `StreamingImportParser` (`nextRow()` yielding one bounded `ParsedImportRow` at a time without loading entire documents into heap memory):

- **`StreamingCsvRecordParser`:** Buffered 8 KiB state-machine RFC 4180 parser stripping UTF-8 BOM, validating canonicalized/aliased headers against `ImportContractSchema`, rejecting missing required/duplicate/unsupported headers (`INVALID_CSV_HEADER`), handling quoted commas, embedded newlines, escaped double-quotes (`""`), bounded max record length (`ROW_TOO_LARGE`), isolating blank lines (`SKIPPED EMPTY_ROW`) and column/quote mismatches (`MALFORMED_CSV_ROW`), and maintaining exact 1-based data row numbering.
- **`StreamingJsonRecordParser`:** Jackson `JsonFactory` pull-token parser supporting JSON arrays (`[{...}, ...]`) and NDJSON (`{...}\n{...}`). Handles `null` elements (`SKIPPED EMPTY_ROW`), non-object or nested array elements (`MALFORMED_JSON_ROW`), unsupported fields (`UNSUPPORTED_FIELD`), bounded field values (`ROW_TOO_LARGE`), and raises `ImportStreamParseException(MALFORMED_JSON_FILE)` on mid-stream syntax corruption.

---

## 6. Trainee and Placement Validation

Implemented in `ImportRowFieldParser`, `TraineeImportRowProcessor`, and `PlacementImportRowProcessor` reusing existing DTO Bean Validation (`CreateTraineeRequest`, `CreatePlacementRecordRequest`) and mappers (`TraineeMapper`, `PlacementRecordMapper`):

- **Type & Schema Checks:** Required/max-length strings (`MISSING_REQUIRED_FIELD`, `FIELD_LENGTH_EXCEEDED`), strict positive integers (`INVALID_NUMERIC_VALUE`), ISO `yyyy-MM-dd` dates (`INVALID_DATE_FORMAT`), `DECIMAL(12,2)` non-negative salaries (`INVALID_SALARY`, `INVALID_NUMERIC_VALUE`), 3-letter ISO currency code (`INVALID_CURRENCY_CODE`), and Jakarta Bean Validation constraints (`INVALID_DATE_OF_BIRTH`, `INVALID_EMAIL_FORMAT`, `INVALID_PHONE_FORMAT`, `INVALID_PINCODE`, `VALIDATION_ERROR`).
- **Business & Relational Integrity:**
  - `Trainee`: Verifies active `ref_gender`, `ref_education_level`, `ref_employment_status`, `ref_profile_status`, geographic hierarchy (`states`/`districts`/`blocks`/`locations` consistency), and optional `providerId`, `centerId`, `batchId` (`FOREIGN_KEY_NOT_FOUND`, `GEOGRAPHIC_MISMATCH`). When optional `userId` is supplied, `validateAndLoadEligibleTraineeUser(...)` executes prior to duplicate checking to enforce: user existence (`FOREIGN_KEY_NOT_FOUND`), non-deleted & unlocked account with `ref_user_status.status_code == 'ACTIVE'` (`USER_INACTIVE`), eligible active roles via `user_roles`/`roles` rejecting non-`trainee` roles (`INVALID_TRAINEE_USER`), zero `employer_id` linkage, consistent user/provider state & district geography, and authoritative row `(stateId, districtId, providerId)` scope compatibility for both scoped callers and nationwide admins (`CROSS_TENANT_REFERENCE`).
  - `PlacementRecord`: Verifies active `training_enrollments` (`FOREIGN_KEY_NOT_FOUND`, `ENROLLMENT_DELETED`), `actualJoiningDate >= offerDate` (`INVALID_JOINING_DATE`), enrollment consistency (`TRAINEE_ENROLLMENT_MISMATCH`, `COURSE_ENROLLMENT_MISMATCH`, `PROGRAM_ENROLLMENT_MISMATCH`, `PROVIDER_ENROLLMENT_MISMATCH`), posting/application consistency (`APPLICATION_TRAINEE_MISMATCH`, `APPLICATION_ENROLLMENT_MISMATCH`, `POSTING_APPLICATION_MISMATCH`, `EMPLOYER_POSTING_MISMATCH`, `JOB_ROLE_POSTING_MISMATCH`), `employer_branches -> employer` (`BRANCH_EMPLOYER_MISMATCH`), `ref_engagement_type.requires_employer` (`EMPLOYER_REQUIRED_FOR_ENGAGEMENT`), and work geography alignment.

---

## 7. Quarantine Classification

Every source row produces exactly one persisted `import_records` row classified strictly across the four `ref_import_record_status` values:

| Status | Classification Semantics | Persisted Metadata |
|---|---|---|
| `SUCCESS` | Schema, FK, chronology, uniqueness, and tenant checks passed; target entity saved | `entity_type`, committed `entity_id`, `error_code = null`, `error_message = null` |
| `FAILED` | Schema/type violation, missing/mismatched FK, chronology error, malformed CSV/JSON row, cross-tenant reference, or non-duplicate DB integrity failure | `entity_id = null`, sanitized `error_code`, single-line sanitized `error_message` (max 500 chars) |
| `DUPLICATE` | Existing DB match or intra-chunk duplicate on `registration_number`, `user_id`, identity candidate keys (phone/email/DOB/name), `placement_number`, `job_application_id`, `(enrollment_id, job_posting_id)`, or unique index `DataIntegrityViolationException` | `entity_id = null`, deterministic duplicate `error_code`, sanitized message |
| `SKIPPED` | Explicit empty-row policy: blank source CSV lines and JSON `null` array elements only (`EMPTY_ROW`). Validation/unexpected errors are never masked as `SKIPPED` | `entity_id = null`, `error_code = "EMPTY_ROW"`, sanitized policy message |

`ImportErrorSanitizer` strips newlines/tabs, bounds lengths (`64`/`500`), maps SQL constraint names cleanly, and prevents SQL fragments, stack traces, or raw PII values from entering `import_records`.

---

## 8. Chunk Transaction Boundaries

`ImportBatchProcessingWorkflowImpl.processBatch(...)` is intentionally non-transactional at method level so no transaction spans the entire file upload/stream and Hikari connection pools (`maximum-pool-size=3` in tests) are never starved:

1. **Claim Transaction (`REQUIRES_NEW`):** Locks target batch row via `SELECT import_batch_id FROM import_batches WHERE import_batch_id = ? FOR UPDATE`, refreshes entity state, verifies caller tenancy & entity write authority, transitions `PENDING -> RUNNING`, initializes `started_at`, and commits immediately.
2. **Bounded Chunk Transactions (`REQUIRES_NEW`):** Streams up to `chunkSize` (default `100`) rows per transaction, skips rows already present in `import_records` for idempotency, validates/persists domain entities + `import_records`, flushes, and clears persistence context (`entityManager.clear()`).
3. **Finalization Transaction (`REQUIRES_NEW`):** Locks batch `FOR UPDATE`, reconciles committed counts via SQL `GROUP BY`, transitions to `COMPLETED` or `FAILED`, sets `completed_at >= started_at`, and commits.

---

## 9. Chunk Failure Isolation

In `processChunkWithIsolation(...)`:

- If a 100-row chunk throws a `RuntimeException` during persistence/flush (e.g., race-condition unique constraint violation or unexpected DB error), Spring cleanly rolls back **only that chunk's `REQUIRES_NEW` transaction**. All earlier committed chunks stay durably committed.
- Execution immediately falls back to `isolateChunkRowByRow(...)`, executing each row in its own fresh `REQUIRES_NEW` transaction (never reusing a rollback-only `EntityManager` or transaction).
- If a transient DB error (`TransientDataAccessException`) occurs, that single row retries once; persistent/integrity failures invoke `recordQuarantinedFailureAfterRollback(...)` in a clean `REQUIRES_NEW` transaction to persist sanitized `DUPLICATE`/`FAILED` status for that row while healthy sibling rows commit cleanly.

---

## 10. Idempotency and Concurrency

- **Concurrent Worker Guard & Deployment Mode (`ImportPipelineProperties.DeploymentMode`):** The supported operational deployment topology is explicitly configured as **`SINGLE_INSTANCE`** (`import-pipeline.deployment-mode=${IMPORT_PIPELINE_DEPLOYMENT_MODE:SINGLE_INSTANCE}`), consistent with JVM-local `ConcurrentMap<Long, String> activeBatchExecutions`, local `java.io.tmpdir` disk staging, and in-memory Caffeine caching. Pessimistic DB row locking (`SELECT ... FOR UPDATE`) in `claimBatchForProcessing(...)` and `recoverInterruptedBatch(...)` serializes concurrent DB claims (`PENDING -> RUNNING`, otherwise `409 IMPORT_BATCH_ALREADY_RUNNING` or `IMPORT_BATCH_TERMINAL`). Recovery never resets `RUNNING` solely from elapsed time and rejects recovery while `activeBatchExecutions` holds the batch (`409 IMPORT_BATCH_ALREADY_RUNNING`).
- **Controlled Interrupted `RUNNING` Batch Recovery (`POST /api/v1/import/batches/{id}/recover`):** Requires authorized write permission and batch ownership plus explicit operator confirmation (`confirmWorkerTerminated=true`, otherwise `400 RECOVERY_CONFIRMATION_REQUIRED`). After acquiring `FOR UPDATE`, verifies caller ownership and `RUNNING` state, then checks `properties.getDeploymentMode() == DeploymentMode.SINGLE_INSTANCE`. In `MULTI_INSTANCE` mode (or any non-`SINGLE_INSTANCE` mode), recovery **fails closed** with `409 Conflict` (`MULTI_INSTANCE_RECOVERY_UNSUPPORTED`) even if `confirmWorkerTerminated=true` and `activeBatchExecutions` is empty locally, because JVM-local tracking cannot verify that another instance is not actively executing the batch without shared lease coordination in the frozen schema. In supported `SINGLE_INSTANCE` mode, recovery reconciles committed counts via `queryCommittedCounts(batchId)`, preserves all already-committed chunks and domain entities, transitions missing-staged-file batches to terminal `FAILED` (`400 STAGED_FILE_NOT_FOUND`), and otherwise resets `RUNNING -> PENDING` while preserving original `started_at` across subsequent resume runs.
- **Per-Row Crash/Retry Idempotency:** Unique key `uk_import_records_batch_row (import_batch_id, source_row_number)` and pre-checks (`findRecordedRowNumbers(...)`, `isRowAlreadyRecorded(...)`) guarantee at-most-once processing per row number within a batch when resuming interrupted batches.
- **Cross-Batch Concurrency:** DB unique constraints (`uk_trainees_registration_number`, `uk_trainees_user_id`, `uk_placement_records_number`, `uk_placement_records_application`, `uk_placement_records_enrollment_posting`) combined with chunk rollback isolation ensure simultaneous batches with overlapping records never create duplicate rows.

---

## 11. Batch Finalization

`finalizeBatch(...)` locks the batch row `FOR UPDATE`, computes final committed statistics, and enforces terminal invariants:

- **Terminal Status Selection:** Sets `FAILED` (`is_terminal_flag=1`) if a fatal stream/parser/row-limit/unexpected error occurred, staged file is missing during recovery (`STAGED_FILE_NOT_FOUND`), OR zero rows were recorded (`rowCount == 0`, returning `400 NO_DATA_ROWS`); otherwise sets `COMPLETED` (`is_terminal_flag=1`) — including mixed or all-invalid row batches where every row received an explicit quarantined `import_records` outcome. An interrupted batch (`RUNNING`) or missing-staged-file batch can never be marked `COMPLETED`.
- **Timestamp Chronology (`ck_import_batches_dates`):** Preserves/sets `started_at` and clamps `completed_at` so `completed_at >= started_at` always holds.
- **Finalization Fallback:** `markBatchFailedSafely(...)` catches finalization errors and attempts a clean `FAILED` transition so batches do not remain stuck in `RUNNING`.

---

## 12. Counter Reconciliation

Rather than trusting mutable in-memory Java accumulators (which can drift upon chunk rollbacks or single-row recovery), `queryCommittedCounts(batchId)` queries committed MySQL state directly:

```sql
SELECT rirs.status_code AS status_code, COUNT(*) AS cnt
FROM import_records ir
JOIN ref_import_record_status rirs ON ir.import_record_status_id = rirs.import_record_status_id
WHERE ir.import_batch_id = ?
GROUP BY rirs.status_code
```

Reconciled assignments stored on `import_batches`:
- `row_count = SUCCESS + FAILED + DUPLICATE + SKIPPED`
- `success_count = SUCCESS`
- `failure_count = FAILED`
- Invariant satisfied: `success_count + failure_count <= row_count`, while `ImportBatchProgressResponse` exposes all four counts (`rowCount`, `successCount`, `failureCount`, `duplicateCount`, `skippedCount`).

---

## 13. Security and Tenant Isolation

Enforced consistently at both controller (`@PreAuthorize`) and service (`ImportSecurityScopeValidator`) boundaries across upload, processing, progress, records, quarantine, and legacy CRUD endpoints:

| Caller Persona | Allowed Operations | Tenant & Data Boundaries |
|---|---|---|
| Nationwide Admin (`ROLE_super_admin` / verified `ROLE_government_admin`) | Upload, process, progress, records, quarantine across tenants | Verified via `WorkflowSecurityScopeValidator.isNationwideAdmin()` |
| State Admin (`ROLE_state_admin` / gov officer without district) | Trainee + Placement within own `state_id` | Batch access & row `stateId`/work geography restricted to caller `state_id` |
| District Officer (`ROLE_district_officer`) | Trainee + Placement within own `(state_id, district_id)` | Restricted to exact caller state + district (`CROSS_TENANT_REFERENCE` otherwise) |
| Training Provider (`ROLE_training_provider`) | Trainee + Placement (`trainee.write` / `placement.manage`) | Restricted to active caller `training_provider_id` & provider state |
| Employer (`ROLE_employer`) | Placement only (`placement.manage`) | `Trainee` upload/read/access rejected (`403`); placement rows must match caller `employer_id` |
| Audit Read-Only (`audit.read` without write authority) | Scoped read (`/progress`, `/records`, `/quarantine`, CRUD reads) | All mutations (`upload`, `process`, CRUD `POST`/`PUT`/`DELETE`) rejected (`403`) |
| Trainee / Unscoped / Conflicting Scope / Inactive | None | Fail closed (`403 AccessDeniedException`) |

`ImportBatchServiceImpl` and `ImportRecordServiceImpl` lock batches `FOR UPDATE` (`lockAndLoadBatchForMutation`), enforce `system.manage` and tenant write scope, block generic `POST`/`PUT`/`DELETE` fabrication of execution statuses, counters (`rowCount`, `successCount`, `failureCount`), or timestamps (`IMPORT_LIFECYCLE_BYPASS_PROHIBITED`, `IMPORT_COUNTER_MANIPULATION_PROHIBITED`), protect `RUNNING`, terminal, and staged/workflow-recorded batches (`IMPORT_BATCH_RUNNING_PROTECTED`, `WORKFLOW_BATCH_PROTECTED`, `WORKFLOW_RECORD_PROTECTED`, `WORKFLOW_RECORD_IMMUTABLE`), forbid updating/deleting workflow or `SUCCESS` records, and verify non-deleted in-scope `Trainee`/`PlacementRecord` ownership before accepting legacy unstaged `SUCCESS` records (`UNVERIFIABLE_SUCCESS_ENTITY`, `ENTITY_NOT_FOUND_FOR_SUCCESS_RECORD`, `INVALID_RECORD_OUTCOME`), while preserving safe empty unstaged `PENDING -> CANCELLED` transitions and empty deletions. Specifically, for provider-scoped callers referencing a `Trainee` in `verifyCommittedEntityExistsAndInScope(...)`, state/district matching alone is never treated as proof of ownership; actual provider ownership must be verified via active `training_enrollments` (`te.provider_id = callerProviderId AND te.deleted_at IS NULL`) or linked active `users.training_provider_id == callerProviderId` (with conflicting `user_provider_id` rejected).

`ImportRecordServiceImpl` enforces SQL-level tenant filtering (`appendBatchTenantWhereClause`) across paginated `getRecordsByStatusId` and `getRecordsByEntity` queries in addition to batch-level `validateCallerCanAccessBatch`.

---

## 14. API Contracts

`ImportBatchController` (`@RequestMapping({"/api/v1/import-batches", "/api/v1/import/batches"})`) and `ImportRecordController` (`/api/v1/import-records`) preserve the 63-controller count and wrap every endpoint in `ApiResponse<T>` / `PagedResponse<T>`:

- **`POST /api/v1/import/batches` (`multipart/form-data`):** Part `file`, params `entityType`, optional `sourceSystem` -> `201 Created` (`ApiResponse<ImportBatchResponse>` with `PENDING` status).
- **`POST /api/v1/import/batches/{id}/process`:** Atomically claims and executes chunked ingestion -> `200 OK` (`ApiResponse<ImportBatchProgressResponse>`), or `409` if already `RUNNING` / terminal (`IMPORT_BATCH_ALREADY_RUNNING` / `IMPORT_BATCH_TERMINAL`).
- **`POST /api/v1/import/batches/{id}/recover?confirmWorkerTerminated=true`:** Operator-confirmed crash recovery for interrupted `RUNNING` batches (`@PreAuthorize("hasAnyAuthority('system.manage', 'trainee.write', 'placement.manage')")`) in `SINGLE_INSTANCE` deployment -> `200 OK` (`ApiResponse<ImportBatchProgressResponse>` with reconciled committed counts and `PENDING` status ready to resume), `400 RECOVERY_CONFIRMATION_REQUIRED` without explicit confirmation, `409 IMPORT_BATCH_ALREADY_RUNNING` while an active worker holds the batch, `409 MULTI_INSTANCE_RECOVERY_UNSUPPORTED` in `MULTI_INSTANCE` mode (fail closed without shared worker coordination), or `400 STAGED_FILE_NOT_FOUND` (finalizing `FAILED`) if the staged file is unavailable in `SINGLE_INSTANCE` mode.
- **`GET /api/v1/import/batches/{id}/progress`:** Reconciled counters across all 4 row statuses (`ApiResponse<ImportBatchProgressResponse>`).
- **`GET /api/v1/import/batches/{id}/records`:** Paginated (`page >= 0`, `1 <= size <= 100`, allowlisted `sort`, optional `status=SUCCESS|FAILED|SKIPPED|DUPLICATE`).
- **`GET /api/v1/import/batches/{id}/quarantine`:** Paginated (`page >= 0`, `1 <= size <= 100`, `includeDuplicates=true|false` defaulting to `true`).
- **`GET /api/v1/import-records?batchId=...` / `?statusId=...` / `?entityType=...&entityId=...`:** Upgraded to bounded pagination (`PagedResponse<ImportRecordResponse>`) with SQL-scoped tenant isolation.
- **`GlobalExceptionHandler` Additions:** Explicit handlers for `MaxUploadSizeExceededException` (`413 FILE_SIZE_LIMIT_EXCEEDED`), `MissingServletRequestPartException` (`400 MISSING_FILE_PART`), and `MultipartException` (`400 MALFORMED_MULTIPART_REQUEST`), plus safe message mappings for all Phase 28.5 error codes.

---

## 15. Integration with Frozen Phases

- **Phase 27 Analytics & Dashboards:** Untouched.
- **Phase 28.1 Baseline & Contracts:** Reused `OperationalErrorCodes.IMPORT_BATCH_TERMINAL`, `IMPORT_ROW_LIMIT_EXCEEDED`, `ImportBatchTerminalException`, and `ImportRowLimitExceededException`; aligned endpoint envelope audit in `ResponseEnvelopeAlignmentContractTest` (`659 -> 665`).
- **Phase 28.2 Audit Infrastructure:** Untouched and reused indirectly via service/controller AOP and security context propagation.
- **Phase 28.3 & 28.4 Data Quality:** Untouched; trainee duplicate candidate normalization logic aligns cleanly with deterministic identity signal detection.

---

## 16. Changed Files

### Created Production Files (16)
- `backend/src/main/java/in/gov/sih/sih26135/config/ImportPipelineProperties.java`
- `backend/src/main/java/in/gov/sih/sih26135/dto/response/ImportBatchProgressResponse.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ImportBatchProcessingWorkflow.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportBatchProcessingWorkflowImpl.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/ImportContractSchema.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/ImportErrorSanitizer.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/ImportFileStagingService.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/ImportRowFieldParser.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/ImportSecurityScopeValidator.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/ParsedImportRow.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/PlacementImportRowProcessor.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/RowOutcomeResult.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/StreamingCsvRecordParser.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/StreamingImportParser.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/StreamingJsonRecordParser.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/TraineeImportRowProcessor.java`

### Modified Production & Config Files (10)
- `backend/src/main/java/in/gov/sih/sih26135/controller/ImportBatchController.java`
- `backend/src/main/java/in/gov/sih/sih26135/controller/ImportRecordController.java`
- `backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/in/gov/sih/sih26135/repository/ImportRecordRepository.java`
- `backend/src/main/java/in/gov/sih/sih26135/response/PaginationUtils.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ImportRecordService.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportBatchServiceImpl.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportRecordServiceImpl.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/resources/application.properties`

### Created & Modified Test Files (3)
- `backend/src/test/java/in/gov/sih/sih26135/service/ingestion/ImportPipelineStreamingAndValidationUnitTest.java` (10 tests: includes deterministic active-worker recovery guard and fail-closed `MULTI_INSTANCE` recovery rejection)
- `backend/src/test/java/in/gov/sih/sih26135/controller/ImportBatchProcessingIntegrationTest.java` (14 tests across 5 nested suites: includes legacy CRUD integrity, provider ownership verification on legacy `Trainee` `SUCCESS` records, trainee `userId` eligibility/tenant scope, interrupted `RUNNING` batch recovery, missing-staged-file `FAILED` handling, and fail-closed `MULTI_INSTANCE` recovery rejection)
- `backend/src/test/java/in/gov/sih/sih26135/response/ResponseEnvelopeAlignmentContractTest.java` (modified: audited endpoints `659 -> 665`)

### Refined Files in Targeted Phase 28.5 Corrections (14 Modified Code/Config/Test Files in Working Tree)
- `backend/src/main/java/in/gov/sih/sih26135/config/ImportPipelineProperties.java`
- `backend/src/main/java/in/gov/sih/sih26135/controller/ImportBatchController.java`
- `backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/in/gov/sih/sih26135/repository/ImportRecordRepository.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ImportBatchProcessingWorkflow.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportBatchProcessingWorkflowImpl.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportBatchServiceImpl.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/impl/ImportRecordServiceImpl.java`
- `backend/src/main/java/in/gov/sih/sih26135/service/ingestion/TraineeImportRowProcessor.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/in/gov/sih/sih26135/controller/ImportBatchProcessingIntegrationTest.java`
- `backend/src/test/java/in/gov/sih/sih26135/response/ResponseEnvelopeAlignmentContractTest.java`
- `backend/src/test/java/in/gov/sih/sih26135/service/ingestion/ImportPipelineStreamingAndValidationUnitTest.java`
- `backend/src/test/resources/application.properties`

---

## 17. Added Tests

- **`ImportPipelineStreamingAndValidationUnitTest` (10 unit tests across 3 groups):**
  - RFC 4180 CSV BOM stripping, quoted fields, embedded commas/newlines, escaped `""`, blank-line `SKIPPED`, malformed rows, 2,500-row streaming, invalid/duplicate/unsupported CSV headers, oversized `ROW_TOO_LARGE` rejection.
  - Streaming JSON array + NDJSON (2,500-row stream, `null -> SKIPPED`, scalar elements, unsupported fields, mid-stream corrupted JSON failure).
  - Durable staging validation, empty/unsupported/oversized rejections, path-traversal batchCode guard, mid-stream I/O temp cleanup, DB failure staging rollback cleanup, PII/SQL-free `ImportErrorSanitizer` verification, deterministic latch-held active worker blocking `recoverInterruptedBatch(..., true)` with `ConflictException` (`IMPORT_BATCH_ALREADY_RUNNING`), and fail-closed `MULTI_INSTANCE` recovery rejection (`MULTI_INSTANCE_RECOVERY_UNSUPPORTED`, `saveAndFlush` never invoked).
- **`ImportBatchProcessingIntegrationTest` (14 live `SIH26135_TEST` MySQL + `MockMvc` tests across 5 nested suites):**
  - Suite 1: Multipart staging on `/api/v1/import/batches` and `/api/v1/import-batches`, empty/unsupported/missing-part/oversized/malformed-multipart error mappings (including `MaxUploadSizeExceededException -> 413` and `MultipartException -> 400`), auth/role blocks (`401`, trainee/auditor/employer-trainee/unscoped `403`).
  - Suite 2: End-to-end mixed Trainee CSV & PlacementRecord JSON/NDJSON processing classifying `SUCCESS`, `FAILED`, `DUPLICATE`, `SKIPPED`, chronology checks, DB FK/unique validation, and sanitized quarantine payloads.
  - Suite 3: 215-row 3-chunk (`100/100/15`) batch processing isolating bad rows cleanly without aborting valid chunks, 2-thread concurrent claim race (`200` + `409`), terminal reprocessing rejection (`409 IMPORT_BATCH_TERMINAL`), concurrent overlapping-entity batch safety, mid-stream fatal JSON failure keeping Chunk 1 committed (`FAILED`), empty-row batch (`FAILED NO_DATA_ROWS`), and `completed_at >= started_at`.
  - Suite 4: Complete multi-tenant isolation across Super Admin, State A/B Admins, District A1/A2 Officers, Provider A/B, Employer A, Trainee, Audit-only, legacy CRUD restrictions, cross-tenant row quarantine (`CROSS_TENANT_REFERENCE`), and paginated `/records`, `/quarantine`, `/progress`, `/api/v1/import-records` contract/sort validation.
  - Suite 5 (`CrudIntegrityUserScopeAndRecoveryTests`, 5 tests):
    1. Blocks legacy CRUD workflow bypasses and fabricated outcomes (`IMPORT_LIFECYCLE_BYPASS_PROHIBITED`, `IMPORT_COUNTER_MANIPULATION_PROHIBITED`, `WORKFLOW_BATCH_PROTECTED`, `WORKFLOW_RECORD_PROTECTED`, `WORKFLOW_RECORD_IMMUTABLE`, `IMPORT_BATCH_RUNNING_PROTECTED`, `UNVERIFIABLE_SUCCESS_ENTITY`) while preserving safe unstaged `PENDING -> CANCELLED` and empty deletion.
    2. Enforces trainee `userId` eligibility & tenant scope across Provider A/B, District A1/A2, Nationwide Admin, and inactive/staff/employer-linked users while committing valid siblings.
    3. Verifies Chunk-1 interruption (`rowCount=100, success=100`), confirmation/cross-tenant/active recovery rejections, confirmed recovery (`RUNNING -> PENDING`) and resume to `COMPLETED` (`149` success, `1` failed, zero duplicate entities), plus missing-staged-file recovery transition to `FAILED`.
    4. Enforces verifiable provider ownership for legacy `Trainee` `SUCCESS` records on create & update (`SEED_TRAINEE_A_ID` accepted, same-state Provider C trainee & same-state unowned trainee rejected with `403`, State A/Super Admin preserved with `201`, workflow-generated records remain `409 WORKFLOW_RECORD_IMMUTABLE`).
    5. Fails closed (`409 MULTI_INSTANCE_RECOVERY_UNSUPPORTED`) on `RUNNING`-batch recovery when `DeploymentMode.MULTI_INSTANCE` is configured even with `confirmWorkerTerminated=true` and an empty local `activeBatchExecutions` map (simulating a worker potentially running on another node), preserving `RUNNING` state and Chunk 1 rows/records until supported `SINGLE_INSTANCE` recovery completes idempotently.

---

## 18. Focused Test Results

Command executed after final code/test edit:

```powershell
mvn -f "E:\project\backend\pom.xml" test "-Dtest=ImportPipelineStreamingAndValidationUnitTest,ImportBatchProcessingIntegrationTest,OperationalErrorCodeContractTest,ResponseEnvelopeAlignmentContractTest,ControllerPathQueryHeaderValidationTest"
```

Observed result: **`Tests run: 82, Failures: 0, Errors: 0, Skipped: 0`** (`BUILD SUCCESS`, total time `01:56 min`):
- `ImportPipelineStreamingAndValidationUnitTest`: **10 run** (`5 + 2 + 3`), 0 failures, 0 errors, 0 skipped
- `ImportBatchProcessingIntegrationTest`: **14 run** across 5 nested suites (`5 + 2 + 3 + 2 + 2`), 0 failures, 0 errors, 0 skipped
- `OperationalErrorCodeContractTest`: **8 run**, 0 failures, 0 errors, 0 skipped
- `ResponseEnvelopeAlignmentContractTest`: **22 run** across 7 nested groups (auditing 63 controllers and 665 endpoints), 0 failures, 0 errors, 0 skipped
- `ControllerPathQueryHeaderValidationTest`: **28 run** across 7 nested groups (validating `@Validated`, `@Positive`, `@Min(0)`, `@Min(1) @Max(100)` across all 63 controllers), 0 failures, 0 errors, 0 skipped

---

## 19. Full Regression Results

Command executed after all final edits:

```powershell
mvn -f "E:\project\backend\pom.xml" test
```

Observed result: **`Tests run: 1935, Failures: 0, Errors: 0, Skipped: 0`** (`BUILD SUCCESS`, total time `05:53 min`):
- Preserves prior baseline and all Phase 27–28 suites plus all 24 Phase 28.5 unit & live integration tests (`10 + 14`), including `AuditTrailAspectTest`, `AuditLogImmutabilityAndTimelineTest`, `AuditTrailEventListenerTest`, `DataQualityIssueServiceSecurityTest`, `DataQualityWorkflowApiTest`, and `WorkflowTransactionRollbackIntegrationTest`.

---

## 20. Database Isolation Evidence

- Test profile (`backend/src/test/resources/application.properties`) strictly binds `jdbc:mysql://${SIH_TEST_DB_HOST:localhost}:${SIH_TEST_DB_PORT:3306}/SIH26135_TEST` with `spring.jpa.hibernate.ddl-auto=validate` and Hikari pool cap (`maximum-pool-size=3`).
- Every live database test suite (including `ImportBatchProcessingIntegrationTest`, `ControllerPathQueryHeaderValidationTest`, `AnalyticsAndDashboardComprehensiveRegressionTest`, and `DataQualityWorkflowApiTest`) asserts `MysqlConnectionTest.assertDedicatedTestCatalog(catalog)` (`SIH26135_TEST`) prior to seeding/mutating test data and cleans up all test rows (`994x` fixtures, `IMP285-%` entities, test batches/records, and staged temp files) in `@BeforeEach`/`@AfterEach`.
- Zero access to production `SIH26135` occurred at any point.

---

## 21. Architectural Invariants

- **Spring Data JPA Repository Interfaces:** Exactly **137** (verified by Hibernate/Spring `RepositoryConfigurationDelegate : Finished Spring Data repository scanning ... Found 137 JPA repository interfaces`).
- **`@Query` Annotations:** Exactly **2** across `backend/src/main/java` (verified by source scan: `QUERY_COUNT=2`).
- **Production `@RestController` Count:** Exactly **63** (verified by source scan `REST_CONTROLLER_COUNT=63` and `ResponseEnvelopeAlignmentContractTest`).
- **Audited Endpoints:** **665** (`664` prior + `POST /api/v1/import/batches/{id}/recover`), all wrapped in `ApiResponse<T>` / `PagedResponse<T>`.
- **Hibernate DDL Mode:** `spring.jpa.hibernate.ddl-auto=validate` preserved in both `main` and `test` configs; zero DDL or schema modifications.

---

## 22. Git Hygiene

Executed read-only checks with zero Git mutations:
- Branch: `main` (`git branch --show-current`)
- `git diff --check`: Exit code **0** (`UNSTAGED_DIFF_CHECK_EXIT=0`)
- `git diff --cached --check`: Exit code **0** (`STAGED_DIFF_CHECK_EXIT=0`)
- `git diff --cached --stat`: Empty (nothing staged)
- Working tree changes: **14** modified code/config/test files (`16` total including both synchronized Phase 28.5 markdown reports) with zero unrelated phases or frozen components altered.

---

## 23. Known Limitations / Blockers

- **No Confirmed Blockers.**
- **Documented Deployment & Schema Boundaries within Frozen Schema:**
  1. **Supported Deployment Topology (`SINGLE_INSTANCE`):** The Phase 28.5 ingestion pipeline (`import-pipeline.deployment-mode=SINGLE_INSTANCE`), in-JVM active-execution registry (`activeBatchExecutions`), local staging path (`java.io.tmpdir`), and in-memory Caffeine cache operate strictly within a **single-instance deployment**. Recovering an interrupted `RUNNING` batch via `POST /api/v1/import/batches/{id}/recover?confirmWorkerTerminated=true` is permitted **only** when `DeploymentMode.SINGLE_INSTANCE` is configured and no active local worker executes on that batch; when `DeploymentMode.MULTI_INSTANCE` is configured, recovery **fails closed** (`409 MULTI_INSTANCE_RECOVERY_UNSUPPORTED`) because the frozen `import_batches` schema and codebase have no distributed worker lease, heartbeat column, or shared coordination infrastructure (and session-pinned DB advisory locks like `GET_LOCK` across chunked batches are incompatible with the bounded `HikariPool` size cap `maximum-pool-size=3` and multi-connection `REQUIRES_NEW` chunk execution). Multi-instance horizontal scaling with remote `RUNNING`-batch recovery requires an approved future schema/infrastructure phase adding shared worker lease/heartbeat storage and shared object staging.
  2. `import_batches` lacks a `staged_file_path` column in frozen `phase-15` DDL, so staged uploads are stored durably in `import-pipeline.staging-dir` keyed strictly by server-generated `batch_code`.
  3. `import_batches` stores only `row_count`, `success_count`, and `failure_count` columns; `duplicateCount` and `skippedCount` are derived authoritatively on demand from `import_records` (`COUNT(*) GROUP BY status_code`) via `ImportBatchProgressResponse`, preserving `success_count + failure_count <= row_count`.

---

## 24. Acceptance Checklist

- [x] Multipart CSV/JSON upload works (`POST /api/v1/import/batches` and `/api/v1/import-batches`).
- [x] Unique `batch_code` (`IMP-YYYYMMDD-<16HEX>`) generated server-side.
- [x] Batch starts in `PENDING` (`1`) with server-verified `initiated_by_user_id`.
- [x] Uploaded file staged durably without buffering full bytes in heap memory.
- [x] Parsing is memory-bounded (8 KiB buffered RFC 4180 CSV & Jackson streaming JSON/NDJSON).
- [x] Row schema, Bean Validation, FK, chronology, and uniqueness validation implemented for `Trainee` & `PlacementRecord`.
- [x] Supplied trainee `userId` strictly validates active user status, eligible role, non-employer linkage, user/provider/row geography, and caller tenant compatibility (`CROSS_TENANT_REFERENCE`).
- [x] Every processed source row has a persisted outcome (`import_records`, max 1 per `(import_batch_id, source_row_number)`).
- [x] `SUCCESS`, `FAILED`, `DUPLICATE`, and explicit empty-row `SKIPPED` classifications work accurately.
- [x] Invalid/duplicate rows are quarantined cleanly with sanitized `error_code`/`error_message`.
- [x] Valid rows persist in bounded `REQUIRES_NEW` chunk transactions (`chunkSize = 100`).
- [x] Bad rows do not abort unrelated valid rows.
- [x] Failed chunk transactions roll back cleanly and isolate row-by-row in separate `REQUIRES_NEW` transactions.
- [x] Successful prior chunks remain committed on mid-stream/chunk failures.
- [x] Concurrent processing of the same batch prevented via `activeBatchExecutions` + DB `FOR UPDATE` claim lock (`PENDING -> RUNNING`, `409` on concurrent/terminal).
- [x] Supported deployment topology explicitly declared as `SINGLE_INSTANCE`; interrupted `RUNNING` batch recovery requires operator confirmation (`confirmWorkerTerminated=true`), blocks while active local worker executes (`409`), fails closed in `MULTI_INSTANCE` mode (`409 MULTI_INSTANCE_RECOVERY_UNSUPPORTED`), preserves committed chunks, transitions missing-staged-file batches to `FAILED`, and resumes cleanly via `source_row_number` idempotency.
- [x] Legacy CRUD (`ImportBatchServiceImpl` / `ImportRecordServiceImpl`) cannot bypass workflow states, fabricate counters/timestamps, mutate workflow/committed records, insert unverified `SUCCESS` outcomes, or claim cross-provider/unowned `Trainee` ownership via matching state/district alone.
- [x] Duplicate inserts prevented within chunk, across DB, and under concurrent batch runs.
- [x] Final counts (`row_count`, `success_count`, `failure_count`, plus `duplicateCount`, `skippedCount`) reconciled directly from committed `import_records`.
- [x] Batch final status (`COMPLETED` vs `FAILED`) strictly accurate.
- [x] Completion timestamp invariant (`completed_at >= started_at`) enforced.
- [x] Tenant authorization enforced across upload, processing, recovery, progress, records, quarantine, and legacy CRUD endpoints.
- [x] Existing API envelopes and frozen database contracts preserved.
- [x] Focused tests passed (`82 tests, 0 failures, 0 errors, 0 skipped`).
- [x] Full regression passed (`1,935 tests, 0 failures, 0 errors, 0 skipped`).
- [x] Architectural invariants preserved (`137` JPA repositories, `2` `@Query` annotations, `63` controllers, `ddl-auto=validate`).
- [x] No production database access (`SIH26135_TEST` used exclusively).
- [x] No Git mutations performed.
- [x] Both synchronized Phase 28.5 reports updated.

---

## 25. Final Verdict

**READY FOR INDEPENDENT REVIEW**

Mandatory Phase 28.5 bulk ingestion, streaming parsing, validation, trainee `userId` eligibility & tenant isolation, provider-ownership verification on legacy `Trainee` `SUCCESS` records, legacy CRUD integrity guards, row-level quarantine, chunked transaction isolation, concurrency control, deployment-safe crash recovery, counter reconciliation, and progress/record retrieval workflows are implemented, integrated within the frozen schema, and verified with zero test failures (`1,935/1,935` full regression passing).
