# Phase 25.13 — Workflow Error & Recovery Behaviour Report

## 1. Executive Summary & Status

- **Phase:** Phase 25.13 — Workflow Error & Recovery Behaviour (Final Validation Allowlist Hardening)
- **Repository Root:** `E:\project`
- **Backend Root:** `E:\project\backend`
- **Java Base Package:** `in.gov.sih.sih26135`
- **Branch:** `main`
- **Database Catalog Utilized:** `SIH26135_TEST` (Zero connections or operations against production catalog `SIH26135`)
- **Schema Status:** Frozen — 144 base tables and 21 views preserved with zero DDL or schema alterations
- **Hibernate Configuration:** `ddl-auto=validate` strictly preserved
- **Final Status:** **READY FOR INDEPENDENT REVIEW**

Following independent review feedback regarding validation allowlist hardening, Phase 25.13 implemented strictly anchored validation message templates and comprehensive edge-case sanitization across API responses and application logs:
1. **Narrowed, Strictly Anchored Validation Allowlist:**
   - Audited `SAFE_STANDARD_VALIDATION_PATTERN` in [`GlobalExceptionHandler.java`](backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java).
   - Removed the permissive wildcard `numeric value out of bounds.*`. Replaced it with the strictly anchored, exact Bean Validation `@Digits` pattern: `numeric value out of bounds( \(<[0-9]+ digits>\.<[0-9]+ digits> expected\))?`.
   - Ensured **every** permissible template in `SAFE_STANDARD_VALIDATION_PATTERN` is strictly anchored from start (`^`) to end (`$`) without arbitrary trailing text.
   - Any message attempting to append custom or unverified text (even if prefixed with an approved template phrase) fails the pattern match and falls back to safe code-derived feedback (`field + ": " + getSafeValidationMessage(code)` or `property + ": " + getSafeConstraintMessage(violation)`), such as `"field: is invalid"`, `"numeric value out of bounds"`, or `"must not be blank"`.
2. **Missing & Single-Digit Edge-Case Coverage:**
   - Implemented regression tests in [`GlobalExceptionHandlerErrorMappingTest.java`](backend/src/test/java/in/gov/sih/sih26135/exception/GlobalExceptionHandlerErrorMappingTest.java):
     - **Null `invalidValue` with custom suffix:** A constraint violation beginning with `numeric value out of bounds:` followed by arbitrary payload `CUSTOM_OUT_OF_BOUNDS_PAYLOAD_789` and `getInvalidValue() == null`. Proved that the arbitrary payload is stripped from response details and server logs, falling back to safe property feedback (`"capacity: is invalid"`).
     - **Single-digit rejected value with appended text:** A `FieldError` with single-digit rejected value `5` and custom message `"must be greater than 0 - CUSTOM_APPENDED_NOTE_456"`. Proved that the single-digit filter in `containsRejectedValue()` cannot bypass sanitization because the strictly anchored pattern rejects the appended text, returning `"retryCount: is invalid"` and keeping the text out of logs.
     - **Single-digit invalidValue on constraint violation:** A `ConstraintViolation` with `invalidValue == 0` and custom message `"must be greater than 0 with leaked param CUSTOM_LEAKED_DETAIL_123"`. Proved that `"durationWeeks: is invalid"` is returned and `CUSTOM_LEAKED_DETAIL_123` never appears in response details or server logs.
3. **Client-Facing Exception Message Sanitization:** Hardened `handleBaseException()`. If any non-500 domain exception contains sensitive data (Aadhaar numbers, PAN identifiers, JWT/Bearer tokens, passwords, credentials, raw SQL fragments, or sensitive test sentinels), the raw exception message is stripped and replaced with a safe, canonical domain message based on the stable error code (e.g. `INVALID_IDENTITY` maps to `"Identity verification failed. Please verify the provided identity details."`), while preserving legitimate, safe domain messages and exact HTTP status codes.
4. **Comprehensive Log Sanitization:** Completely eliminated throwable stack trace logging (`log.error(..., ex)`), which previously printed full stack traces and nested causes that could expose raw SQL queries, bound parameters, passwords, or tokens. Server logs strictly record safe operational metadata: request URI, HTTP status, stable error code, exception class, and field/property names.
5. **Reconciled Database Error Mapping:** Reconciled source implementation with documentation: recognized unique constraints return HTTP 409 Conflict with domain-specific codes; unrecognized duplicate-key violations return HTTP 409 with `RESOURCE_CONFLICT`; unrecognized non-duplicate database integrity violations (foreign key failures, null constraints, check constraints) fail closed to HTTP 500 `INTERNAL_SERVER_ERROR` with a sanitized message.
6. **Honest Atomicity & Rollback Evidence Classification:** Clarified the exact nature of atomicity evidence. Real MySQL InnoDB transaction rollback is proven against test database `SIH26135_TEST` exclusively on populated reference tables (`ref_skill_gap_status` in `WorkflowTransactionRollbackIntegrationTest`). For multi-step domain workflows (placement, employment, verification, training outcomes), live database rollback is **unverified** because domain tables contain 0 baseline rows and synthesizing fixtures across 20+ FK tables is unsafe/forbidden under frozen schema constraints. The tests in [`WorkflowErrorRecoveryRegressionTest.java`](backend/src/test/java/in/gov/sih/sih26135/workflow/WorkflowErrorRecoveryRegressionTest.java) are explicitly documented as **unit exception-propagation and transaction-boundary evidence**, proving downstream short-circuiting and prevention of misleading success envelopes.
7. **Full Suite Verification:** The backend test suite executes 620 tests with **0 failures, 0 errors, and 0 skipped**. `git diff --check` passes with zero whitespace or formatting errors.

---

## 2. Audit Scope & Changed Files

### A. Modified Source Files
1. [`backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java`](backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java)
   - **Strictly Anchored Validation Pattern:**
     - Narrowed `SAFE_STANDARD_VALIDATION_PATTERN` to accept only explicitly defined, fully anchored message templates without permissive `.*` wildcards.
     - Anchored `@Digits` validation pattern to: `numeric value out of bounds( \(<[0-9]+ digits>\.<[0-9]+ digits> expected\))?`.
     - Preserved safe templates for null/blank/empty, email, URL, date, ISO date, positive/negative, range/length bounds, boolean, and numeric limits.
     - Added `case "Digits" -> "numeric value out of bounds"` in `getSafeValidationMessage()`.
   - **Echo & Input Sanitization:**
     - Defined `containsRejectedValue(String message, Object rejectedValue)`: checks whether `message` echoes `rejectedValue`.
     - Defined `isSafeStandardValidationMessage(String propertyName, String message, Object rejectedValue)`: rejects messages containing sensitive data, echoing rejected values, or failing the anchored standard template match.
     - Guarded `getSafeRejectedValue(FieldError)` and `getSafeInvalidValue(ConstraintViolation<?>)` with exception guards.
   - **Validation Error & Constraint Violation Details Sanitization:**
     - In `sanitizeFieldError()`, if `isSafeStandardValidationMessage()` fails, falls back to safe code-derived message via `getSafeValidationMessage(fieldError.getCode())`.
     - In `sanitizeConstraintViolation()`, if `isSafeStandardValidationMessage()` fails, falls back to safe constraint message via `getSafeConstraintMessage(violation)`.
   - **Sensitive Data Detection:** Implemented `containsSensitiveData(String text)` backed by `SENSITIVE_DATA_PATTERN` to inspect messages for Aadhaar numbers (12 digits), PAN formats, JWT tokens, credential/password pairs, SQL fragments, and sensitive test sentinels.
   - **Domain Exception Message Sanitization:** In `handleBaseException`, sanitized `clientMessage`: if 500 status, masked with `"An unexpected error occurred. Please try again later."`; if non-500 status and `containsSensitiveData(clientMessage)` is true, replaced with safe canonical domain message from `getSafeMessageForErrorCode(errorCode, status)`.
   - **Throwable Stack-Trace Leak Elimination:** Removed all occurrences of passing `ex` as a throwable (`log.error("Unhandled exception...", ex)` and `log.error("...path variable...", ex)`). Stack traces and nested cause messages containing raw queries, bound parameters, or credentials can no longer escape into logs.
   - **Database Integrity Violation Handling:**
     - 40+ specific unique constraints map to HTTP 409 Conflict with domain-specific conflict codes.
     - Fallback duplicate-key checks ("Duplicate entry", "duplicate key") map to HTTP 409 Conflict with `RESOURCE_CONFLICT`.
     - Unrecognized non-duplicate integrity violations (foreign key failures, null constraints, check constraints) log a sanitized error (`status=500, code=INTERNAL_SERVER_ERROR`) and return HTTP 500 `INTERNAL_SERVER_ERROR` with `"An unexpected database error occurred. Please try again later."`.
     - In `conflictResponse`, server logging records URI, status (409), and error code without logging raw database messages.
   - **Safe Operational Logging:** In all exception handlers, logging records only safe operational metadata: request URI, HTTP status, stable error code, exception class, and field/property names.

### B. Modified and Added Test Files
1. [`backend/src/test/java/in/gov/sih/sih26135/exception/GlobalExceptionHandlerErrorMappingTest.java`](backend/src/test/java/in/gov/sih/sih26135/exception/GlobalExceptionHandlerErrorMappingTest.java) *(Expanded to 76 tests)*
   - Added `testConstraintViolation_numericValueOutOfBoundsWithArbitraryText_nullInvalidValue_isSanitized`: asserts message starting with `numeric value out of bounds: CUSTOM_OUT_OF_BOUNDS_PAYLOAD_789` with `invalidValue == null` is sanitized in response details (`"capacity: is invalid"`) and logs.
   - Added `testValidationException_singleDigitRejectedValueWithArbitraryAppendedText_isSanitized`: asserts single-digit rejected value `5` with message `"must be greater than 0 - CUSTOM_APPENDED_NOTE_456"` is sanitized in response details (`"retryCount: is invalid"`) and logs.
   - Added `testConstraintViolation_singleDigitInvalidValueWithArbitraryAppendedText_isSanitized`: asserts single-digit invalidValue `0` with message `"must be greater than 0 with leaked param CUSTOM_LEAKED_DETAIL_123"` is sanitized in response details (`"durationWeeks: is invalid"`) and logs.
   - Added `testValidationException_arbitraryRejectedValueEcho_isSanitizedInResponseDetailsAndLogs`: asserts `CUSTOM_USER_VALUE_123` is absent from response details and server logs, preserving `"username: is invalid"`.
   - Added `testConstraintViolation_arbitrarySubmittedValueEcho_isSanitizedInResponseDetailsAndLogs`: asserts `CUSTOM_SUBMITTED_INPUT_456` is absent from response details and server logs, preserving `"batchCode: is invalid"`.
   - Verified that unique sensitive sentinel strings and arbitrary user payloads never appear in captured logs or client response bodies across all error types.
2. [`backend/src/test/java/in/gov/sih/sih26135/workflow/WorkflowErrorRecoveryRegressionTest.java`](backend/src/test/java/in/gov/sih/sih26135/workflow/WorkflowErrorRecoveryRegressionTest.java) *(22 regression tests)*
   - Documented explicit atomicity classification: tests provide unit exception-propagation and transaction-boundary evidence, proving downstream short-circuiting and prevention of misleading success envelopes, while physical MySQL rollback for domain workflows remains unverified at the database layer.

---

## 3. Error-Handling Matrix & Reconciled API Behavior

| Error Category | Triggering Condition | Exception Type | HTTP Status | Client Response Payload | Server Log Content |
|---|---|---|---|---|---|
| **Resource Not Found** | Record ID (trainee, placement, employer, task) does not exist | `ResourceNotFoundException` | 404 Not Found | `{"success": false, "code": "RESOURCE_NOT_FOUND", "message": "...", "timestamp": ...}` | `WARN` with URI, status=404, code=RESOURCE_NOT_FOUND, exception class; no user credentials |
| **Relationship Mismatch** | Valid record exists but does not belong to specified parent | `BadRequestException` | 400 Bad Request | `{"success": false, "code": "INVALID_RELATIONSHIP", "message": "...", "timestamp": ...}` | `WARN` with URI, status=400, code, exception class; no PII |
| **Sensitive Domain Error** | Domain exception containing Aadhaar, credentials, or sensitive sentinels | `BaseException` (with sensitive text) | 400 / 409 / etc. | `{"success": false, "code": "<CODE>", "message": "<Canonical Safe Message>", "timestamp": ...}` | `WARN` with URI, status, code, exception class; **sensitive message stripped** |
| **Malformed Body** | Unparsable JSON syntax or unreadable HTTP body | `HttpMessageNotReadableException` | 400 Bad Request | `{"success": false, "code": "MALFORMED_JSON", "message": "Malformed or unreadable request body", "timestamp": ...}` | `WARN` with URI, status=400, code=MALFORMED_JSON; **zero request body text logged** |
| **Type Mismatch** | Query/path parameter value cannot be converted to target type | `MethodArgumentTypeMismatchException` | 400 Bad Request | `{"success": false, "code": "TYPE_MISMATCH", "message": "Invalid parameter value for '...'. Expected type: ...", "timestamp": ...}` | `WARN` with URI, parameter name, status=400, code=TYPE_MISMATCH, expectedType; **rejected value omitted** |
| **Request Validation** | DTO `@Valid` validation fails on method argument | `MethodArgumentNotValidException` | 400 Bad Request | `{"success": false, "code": "VALIDATION_ERROR", "message": "Request validation failed", "details": ["field: safe message"], "timestamp": ...}` | `WARN` with URI, status=400, code=VALIDATION_ERROR, `invalidFields=[...]`; **rejected values and raw messages omitted** |
| **Constraint Violation** | Method parameter `@Validated` constraint violation | `ConstraintViolationException` (client origin) | 400 Bad Request | `{"success": false, "code": "VALIDATION_ERROR", "message": "Request validation failed", "details": ["prop: safe message"], "timestamp": ...}` | `WARN` with URI, status=400, code=VALIDATION_ERROR, `invalidProperties=[...]`; **rejected values and raw messages omitted** |
| **Recognized Duplicate Constraint** | Unique DB constraint violated matching known domain constraint | `DataIntegrityViolationException` (known constraint) | 409 Conflict | `{"success": false, "code": "<DOMAIN_CONFLICT_CODE>", "message": "<Sanitized domain explanation>", "timestamp": ...}` | `WARN` with URI, status=409, code; **raw SQL, table names, and parameters excluded** |
| **Generic Duplicate Fallback** | Unrecognized unique constraint matching "Duplicate entry" / "duplicate key" | `DataIntegrityViolationException` (generic duplicate) | 409 Conflict | `{"success": false, "code": "RESOURCE_CONFLICT", "message": "A resource with the specified unique attributes already exists", "timestamp": ...}` | `WARN` with URI, status=409, code=RESOURCE_CONFLICT; **raw SQL and table names excluded** |
| **Unrecognized Non-Duplicate Integrity Violation** | Foreign key failure, null constraint violation, check constraint | `DataIntegrityViolationException` (non-duplicate) | 500 Internal Server Error | `{"success": false, "code": "INTERNAL_SERVER_ERROR", "message": "An unexpected database error occurred. Please try again later.", "timestamp": ...}` | `ERROR` with URI, status=500, code=INTERNAL_SERVER_ERROR; **raw SQL, constraints, and tables excluded** |
| **Authorization / Consent Denial** | Caller lacks role, tenant scope, or consent is missing/revoked | `AccessDeniedException` / `ConsentRequiredException` | 403 Forbidden | `{"success": false, "code": "ACCESS_DENIED" / "CONSENT_REQUIRED", "message": "...", "timestamp": ...}` | `WARN` with user principal and denied action; no credentials logged |
| **Unexpected System Failure** | Uncaught runtime error, NullPointerException, DB connection failure | `RuntimeException` / `Exception` | 500 Internal Server Error | `{"success": false, "code": "INTERNAL_SERVER_ERROR", "message": "An unexpected error occurred. Please try again later.", "timestamp": ...}` | `ERROR` with URI, status=500, code=INTERNAL_SERVER_ERROR, exception class; **throwable stack trace omitted** |

---

## 4. Retry-Safety Matrix

Every core workflow write operation in the backend was audited to classify its safety under retry or network replay conditions:

| Operation | Service & Method | Retry Classification | Enforcement Mechanism | Behavior on Replay / Retry |
|---|---|---|---|---|
| **Placement to Employment** | `PlacementEmploymentWorkflowServiceImpl.transitionPlacementToEmployment` | **Conflict / Idempotent Replay** | `findByPlacementRecordId` check + Unique DB constraint `uk_employment_records_placement` | First execution creates employment record and initial salary history. Subsequent replay is rejected upfront with 409 `DUPLICATE_PLACEMENT_EMPLOYMENT`. If initial execution rolled back due to error, retrying with corrected data succeeds safely. |
| **Assessment & Certification** | `TrainingOutcomeWorkflowServiceImpl.recordAssessmentResultAndIssueCertificate` | **Conflict / Idempotent Replay** | `assessmentResultRepository.existsByTraineeAssessmentId` + DB constraint `uk_assessment_results_attempt` | First execution records assessment result, marks enrollment completed, issues certificate, and auto-resolves skill gaps. Replay is rejected upfront with 409 `ASSESSMENT_RESULT_ALREADY_EXISTS`. If rolled back, retrying is safe. |
| **Employment Exit** | `EmploymentLifecycleWorkflowServiceImpl.recordExitAndInitiateUnemployment` | **Conflict / Idempotent Replay** | Active status check + DB constraint `uk_employment_exit_events_employment` | First execution sets `isCurrent = false`, creates exit event, and initiates unemployment record. Replay is rejected with 409 `EXIT_EVENT_ALREADY_EXISTS` or 400 `EMPLOYMENT_ALREADY_INACTIVE`. Rolled back attempt retries safely. |
| **Unemployment to Employment** | `EmploymentLifecycleWorkflowServiceImpl.transitionUnemploymentToEmployment` | **Conflict / Idempotent Replay** | `isCurrent = true` check + `uk_employment_records_placement` | First execution marks unemployment record inactive with end date and creates new employment record. Replay is rejected with 409 `UNEMPLOYMENT_EVENT_NOT_ACTIVE`. Rolled back attempt retries safely. |
| **Verification Request Initiation** | `VerificationWorkflowServiceImpl.initiateVerificationFromSurvey` | **Conflict / Idempotent Replay** | Verification request status check + DB constraint `uk_emp_verif_requests_open` | First execution creates verification request. Replay is rejected upfront with 409 `OPEN_REQUEST_EXISTS` or `SURVEY_ALREADY_INITIATED`. Rolled back attempt retries safely. |
| **Verification Verdict Synchronization** | `VerificationWorkflowServiceImpl.recordVerificationVerdictAndSynchronize` | **Conflict / Idempotent Replay** | Closed request check + DB constraints `uk_emp_verifications_current` / `uk_emp_verifications_request` | First execution writes verification verdict, creates evidence, updates request status to CLOSED, and updates employment record. Replay is rejected with 400 `VERIFICATION_REQUEST_ALREADY_CLOSED` or 409 `ACTIVE_VERIFICATION_ALREADY_EXISTS`. Rolled back attempt retries safely. |
| **Follow-up Survey Response** | `FollowupEngagementWorkflowServiceImpl.recordFollowupSurveyResponse` | **Conflict / Idempotent Replay** | Response existence check + DB constraint `uk_survey_responses_attempt` | First execution persists survey response. Replay is rejected upfront or at DB boundary with 409 `DUPLICATE_SURVEY_RESPONSE`. Rolled back attempt retries safely. |
| **Follow-up Engagement Attempt** | `FollowupEngagementWorkflowServiceImpl.recordEngagementAttempt` | **Conflict / Idempotent Replay** (when `providerMessageId` supplied); **Not safe to retry automatically** (when omitted) | Provider message uniqueness `uk_communication_logs_provider_message` | When caller supplies unique `providerMessageId`, replay is caught with 409 `DUPLICATE_PROVIDER_MESSAGE_ID`. If caller generates a random ID or omits it on retry, a duplicate communication log row is inserted. Clients must supply idempotent provider IDs on retries. |
| **Skill Gap Remediation Assessment** | `SkillGapRemediationWorkflowServiceImpl.recordSkillGapAssessmentWithGaps` | **Conflict / Idempotent Replay** | Gap existence checks + DB constraint `uk_skill_gaps_current` | First execution records assessment observations and creates active skill gaps. Replay is rejected with 409 `ACTIVE_SKILL_GAP_ALREADY_EXISTS`. Rolled back attempt retries safely. |

---

## 5. Transaction Atomicity & Partial-Success Evidence Reconciliation

### A. Distinction Between Exception-Propagation Evidence and Database Rollback Proof
Independent review required an accurate distinction between unit exception propagation and live database rollback:
- **Database Engine Rollback Proof:**
  - Real MySQL InnoDB transaction rollback is verified against test database `SIH26135_TEST` in [`WorkflowTransactionRollbackIntegrationTest.java`](backend/src/test/java/in/gov/sih/sih26135/workflow/WorkflowTransactionRollbackIntegrationTest.java).
  - This integration test operates on reference tables (`ref_skill_gap_status`) that contain valid baseline rows. It proves that Spring's `@Transactional` rollback mechanism successfully directs the MySQL InnoDB storage engine to undo DML operations when an exception occurs.
- **Domain Workflow Atomicity (Classified as Unverified at Database Layer):**
  - In `SIH26135_TEST`, transactional domain tables (e.g. `employment_records`, `employment_salary_history`, `trainee_assessments`, `certificates`, `employment_verification_evidence`) contain 0 baseline rows.
  - Fabricating synthetic domain fixtures across 20+ mandatory foreign keys is unsafe and prohibited by frozen schema constraints.
  - Consequently, **live MySQL transaction rollback across multi-table domain workflows remains unverified at the database layer.**
- **Unit Exception-Propagation Evidence:**
  - The tests in [`WorkflowErrorRecoveryRegressionTest.java`](backend/src/test/java/in/gov/sih/sih26135/workflow/WorkflowErrorRecoveryRegressionTest.java) use mocked service dependencies.
  - These tests prove that when an intermediate write (e.g. salary insertion, certificate issuance, evidence creation, task update) throws an exception, the service **does not catch, suppress, or swallow the exception**. The unchecked exception propagates immediately to Spring's transaction manager (which triggers transaction abort) and to `GlobalExceptionHandler`.
  - These unit tests demonstrate that the service execution short-circuits and that the controller **never returns a misleading partial-success response** (`ApiResponse.success`). They do NOT claim physical MySQL rollback proof for those domain tables.

### B. Transaction Boundaries and Propagation Inspection
Inspection of all workflow service implementations confirmed:
- `PlacementEmploymentWorkflowServiceImpl`: `@Transactional(readOnly = true)` at class level; `transitionPlacementToEmployment` is explicitly `@Transactional` with default propagation `REQUIRED`.
- `TrainingOutcomeWorkflowServiceImpl`: `@Transactional(readOnly = true)` at class level; `recordAssessmentResultAndIssueCertificate` is explicitly `@Transactional` (`REQUIRED`).
- `EmploymentLifecycleWorkflowServiceImpl`: Both `recordExitAndInitiateUnemployment` and `transitionUnemploymentToEmployment` are explicitly `@Transactional` (`REQUIRED`).
- `VerificationWorkflowServiceImpl`: `@Transactional(readOnly = true)` at class level; both `initiateVerificationFromSurvey` and `recordVerificationVerdictAndSynchronize` are explicitly `@Transactional` (`REQUIRED`).
- `FollowupEngagementWorkflowServiceImpl`: Both `recordEngagementAttempt` and `recordFollowupSurveyResponse` are explicitly `@Transactional` (`REQUIRED`).
- `SkillGapRemediationWorkflowServiceImpl`: `recordSkillGapAssessmentWithGaps` is explicitly `@Transactional` (`REQUIRED`).

All workflow write methods participate in Spring container-managed transactions with standard rollback rules (`rollbackFor = RuntimeException.class`).

---

## 6. Sensitive-Logging & Response Sanitization Evidence

### A. Vulnerability Remediation Summary
| Component | Prior Implementation | Remediation | Protected Data Types |
|---|---|---|---|
| `handleBaseException` (Response) | Returned `ex.getMessage()` unconditionally for non-500 | Sanitizes via `containsSensitiveData()`. Replaces with canonical message from `getSafeMessageForErrorCode()` | Aadhaar numbers, PAN, tokens, credentials, salaries, verification evidence |
| `handleValidationException` (Response) | Returned raw `fieldError.getDefaultMessage()` | Sanitizes via `containsRejectedValue()` and anchored `isSafeStandardValidationMessage()`. Replaces with safe code-derived feedback (`field + ": is invalid"`, `"must not be blank"`, etc.) | Arbitrary user inputs, rejected values, custom messages with appended text |
| `handleConstraintViolation` (Response) | Returned raw `violation.getMessage()` | Sanitizes via `containsRejectedValue()` and anchored `isSafeStandardValidationMessage()`. Replaces with `property + ": is invalid"` | Parameter payloads, identity sentinels, arbitrary submitted values, null invalidValue custom text |
| `handleGenericException` (Logs) | `log.error(..., ex)` passed throwable | `log.error("Unhandled exception...: status={}, code={}, exception={}", ...)` | Raw SQL, table names, database credentials, connection strings, stack frames |
| `handleMissingPathVariable` (Logs) | `log.error(..., ex)` passed throwable | `log.error("Server-side path variable...: status={}, code={}, variable='{}', exception={}", ...)` | Stack traces, internal controller reflection details |
| `handleBaseException` (Logs) | `log.warn(..., clientMessage)` logged exception message | `log.warn("Application exception on {}: status={}, code={}, exception={}", ...)` | Sensitive identity data (Aadhaar, PAN), financial figures (salaries), credentials |
| `handleValidationException` (Logs) | `log.warn(..., validationErrors)` logged messages with rejected values | `log.warn("Validation error on {}: status={}, code={}, invalidFields={}", ...)` | Request-controlled input values, rejected payload values |
| `handleConstraintViolation` (Logs) | `log.warn(..., validationErrors)` logged violation messages | `log.warn("Request constraint violation on {}: status={}, code={}, invalidProperties={}", ...)` | Request-controlled parameter values |
| `handleMessageNotReadable` (Logs) | `log.warn(..., ex.getMessage())` logged Jackson excerpts | `log.warn("Malformed HTTP message on {}: status={}, code={}", ...)` | Plaintext passwords, JWT tokens, Aadhaar, raw unparsed JSON |
| `handleMethodArgumentTypeMismatch` (Logs) | `log.warn(..., ex.getMessage())` logged user input | `log.warn("Parameter type mismatch...: parameter='{}', status={}, code={}, expectedType={}", ...)` | Query parameter values, salaries, tokens |
| `handleHttpMediaTypeNotAcceptable` (Logs) | `log.warn(..., ex.getMessage())` logged header string | `log.warn("Media type not acceptable on {}: status={}, code={}", ...)` | Request-controlled `Accept` header values |
| `handleDataIntegrityViolation` (Logs) | Unrecognized violations logged `ex.getMessage()` prematurely | Sanitized to `log.error("Unrecognized database integrity violation...: status={}, code={}", ...)` | Raw SQL statements, bound parameter values, internal table/column names |

### B. Sentinel & Arbitrary Input Test Inventory in `LogSanitizationRegressionTests`
1. `testConstraintViolation_numericValueOutOfBoundsWithArbitraryText_nullInvalidValue_isSanitized` — Verifies message starting with `numeric value out of bounds:` followed by arbitrary payload `CUSTOM_OUT_OF_BOUNDS_PAYLOAD_789` with `invalidValue == null` is sanitized in response details (`"capacity: is invalid"`) and logs.
2. `testValidationException_singleDigitRejectedValueWithArbitraryAppendedText_isSanitized` — Verifies single-digit rejected value `5` with message `"must be greater than 0 - CUSTOM_APPENDED_NOTE_456"` is sanitized in response details (`"retryCount: is invalid"`) and logs.
3. `testConstraintViolation_singleDigitInvalidValueWithArbitraryAppendedText_isSanitized` — Verifies single-digit invalidValue `0` with message `"must be greater than 0 with leaked param CUSTOM_LEAKED_DETAIL_123"` is sanitized in response details (`"durationWeeks: is invalid"`) and logs.
4. `testValidationException_arbitraryRejectedValueEcho_isSanitizedInResponseDetailsAndLogs` — Verifies arbitrary harmless-looking rejected value `CUSTOM_USER_VALUE_123` is absent from response details and server logs, preserving `"username: is invalid"`.
5. `testConstraintViolation_arbitrarySubmittedValueEcho_isSanitizedInResponseDetailsAndLogs` — Verifies arbitrary submitted value `CUSTOM_SUBMITTED_INPUT_456` is absent from response details and server logs, preserving `"batchCode: is invalid"`.
6. `testMalformedJson_sentinelsDoNotLeakInLogsOrResponse` — Verifies `SENTINEL_PASSWORD_SECRET_RAW_987654` and `SENTINEL_TOKEN_SECRET_RAW_ABCDEF` are absent from both logs and response.
7. `testTypeMismatch_salarySentinelDoesNotLeakInLogsOrResponse` — Verifies `SENTINEL_SALARY_VALUE_999999.50` is absent from both logs and response.
8. `testDomainException_identitySentinelDoesNotLeakInLogsOrResponse` — Verifies `SENTINEL_AADHAAR_9999_8888_7777` is absent from server logs AND client response (`response.getBody().getMessage()`).
9. `testBaseException500_secretSentinelDoesNotLeakInLogsOrResponse` — Verifies `SENTINEL_INTERNAL_ENCRYPTION_KEY_SECRET_789` is masked in both client response and server logs.
10. `testDataIntegrityViolation_recognizedDuplicate_sentinelsDoNotLeak` — Verifies `SENTINEL_SQL_INSERT_INTO_EMPLOYMENTS_PLACEMENTS_999` and `SENTINEL_BOUND_PARAM_PLC_12345` do not leak in logs or response.
11. `testDataIntegrityViolation_unrecognizedNonDuplicate_sentinelsDoNotLeak` — Verifies `SENTINEL_SQL_INSERT_INTO_USERS_CREDENTIALS_888` and `SENTINEL_BOUND_PASSWORD_HASH_ABC` do not leak and response returns HTTP 500 `INTERNAL_SERVER_ERROR`.
12. `testUnexpectedException_credentialSentinelsDoNotLeakInLogsOrResponse` — Verifies `SENTINEL_RAW_DB_PASSWORD_SUPER_SECRET_999` and `SENTINEL_RAW_SQL_SELECT_ALL_FROM_CONFIDENTIAL_VAULT` do not escape via stack traces.
13. `testValidationException_sanitizesFieldErrorsInLogs` — Verifies rejected salary sentinel `SENTINEL_REJECTED_SALARY_999999` in custom message is omitted from logs AND response `details` (`"amount: is invalid"`).
14. `testConstraintViolation_sentinelInMessageDoesNotLeakInLogsOrResponse` — Verifies `SENTINEL_CONSTRAINT_AADHAAR_9999_8888_7777` in custom violation message is omitted from logs AND response `details` (`"aadhaarNumber: is invalid"`).

---

## 7. Test Inventory & Execution Summary

### A. Test Execution Results
All tests were executed against MySQL catalog `SIH26135_TEST` under `ddl-auto=validate`:

1. **Exception Handler Mapping & Log-Sanitization Tests:**
   ```powershell
   mvn test "-Dtest=GlobalExceptionHandlerErrorMappingTest" -f backend/pom.xml
   ```
   - **Result:** `Tests run: 76, Failures: 0, Errors: 0, Skipped: 0` (Total time: 30.690 s)

2. **API-Level Exception Handler & Controller Classification Tests:**
   ```powershell
   mvn test "-Dtest=ApiLevelExceptionHandlerTest" -f backend/pom.xml
   ```
   - **Result:** `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0` (Total time: 21.49 s)

3. **Workflow Error & Recovery Regression Tests:**
   ```powershell
   mvn test "-Dtest=WorkflowErrorRecoveryRegressionTest" -f backend/pom.xml
   ```
   - **Result:** `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0` (Total time: 37.013 s)

4. **Combined API & Workflow Integration Test Execution:**
   ```powershell
   mvn test "-Dtest=ApiLevelExceptionHandlerTest,WorkflowErrorRecoveryRegressionTest" -f backend/pom.xml
   ```
   - **Result:** `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0` (Total time: 29.133 s)

5. **Full Backend Test Suite:**
   ```powershell
   mvn test -f backend/pom.xml
   ```
   - **Result:** `Tests run: 620, Failures: 0, Errors: 0, Skipped: 0` (Total time: 01:04 min)

---

## 8. Database Catalog Used & Limitations Disclosure

- **Catalogs:**
  - Production database `SIH26135` was **strictly untouched** (0 connections, 0 queries, 0 DDL, 0 DML).
  - Test database `SIH26135_TEST` was connected via HikariCP under `ddl-auto=validate`. All 144 base tables and 21 views validated successfully.
- **Database Test Scope & Honest Evidence Disclosures:**
  - `WorkflowTransactionRollbackIntegrationTest` executes real MySQL transaction rollbacks against populated reference tables (`ref_skill_gap_status`) in `SIH26135_TEST`, proving engine-level InnoDB rollback capabilities under Spring `@Transactional`.
  - In `SIH26135_TEST`, transactional domain tables currently contain 0 baseline rows. Because domain records require extensive foreign key hierarchies across 20+ base tables, synthesizing artificial domain fixtures was deliberately avoided to preserve data integrity and comply with frozen schema constraints.
  - Multi-step rollback guarantees across complex domain tables are therefore classified as **unverified at the physical database layer**, and are supported at the unit level by exception-propagation and transaction-boundary evidence.

---

## 9. Final Git Status Confirmation

- **Working Tree State:**
  - Modified: [`backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java`](backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java)
  - Modified: [`backend/src/test/java/in/gov/sih/sih26135/exception/GlobalExceptionHandlerErrorMappingTest.java`](backend/src/test/java/in/gov/sih/sih26135/exception/GlobalExceptionHandlerErrorMappingTest.java)
  - Untracked: [`backend/src/test/java/in/gov/sih/sih26135/workflow/WorkflowErrorRecoveryRegressionTest.java`](backend/src/test/java/in/gov/sih/sih26135/workflow/WorkflowErrorRecoveryRegressionTest.java)
  - Untracked: [`phase-25-13-workflow-error-recovery-report.md`](phase-25-13-workflow-error-recovery-report.md)
- **Git Checkpoint Actions:**
  - `git diff --check`: Passed with zero whitespace, trailing space, or formatting errors.
  - **No staging (`git add`), no commits (`git commit`), and no pushes (`git push`) were performed.** All Git checkpoint operations are reserved for manual execution by the user after independent review.
