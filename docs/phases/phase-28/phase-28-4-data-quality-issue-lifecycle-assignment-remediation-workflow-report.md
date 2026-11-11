# Phase 28.4 — Data Quality Issue Lifecycle, Assignment & Remediation Workflow Report

## 1. Executive Summary

Phase 28.4 establishes a production-grade, concurrency-safe, transactional, and auditable Issue Lifecycle, Assignment, and Remediation Workflow for the SIH-26135 data quality subsystem. Building directly upon the detection engine and deduplication mechanism delivered in Phase 28.3, this phase enables authorized administrators, state authorities, and training providers to operationalize detected anomalies through strict state transitions, assignment, investigation, resolution, dismissal, and reopening workflows.

Following independent source review, all confirmed authorization boundaries, state machine transition rules, assignee tenancy eligibility validations, and fail-open fallbacks were rigorously corrected and verified across unit, integration, and full regression test suites.

Every lifecycle transition is executed within an isolated database transaction, guarded by row-level pessimistic locking (`SELECT ... FOR UPDATE`), and atomically recorded in the `data_quality_issue_events` audit table.

### Key Achievements & Targeted Corrections

1. **Strict Read and Write Permission Separation:**
   - Enforced `@PreAuthorize("hasAuthority('system.manage')")` on all lifecycle mutation endpoints (`/assign`, `/start-investigation`, `/resolve`, `/dismiss`, `/reopen`, `POST /`, `PUT /{id}`) in `DataQualityIssueController`.
   - Preserved `@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")` for read endpoints including `GET /{id}/events`.
   - Enforced equivalent defense-in-depth authorization at the service boundary in `DataQualityWorkflowServiceImpl`: `resolveAndValidateManager()` requires `system.manage`, while `resolveAndValidateReader()` permits `audit.read || system.manage`.
   - Validated via adversarial tests that callers holding `audit.read` alone receive HTTP 403 Forbidden for all lifecycle mutations while retaining read access to events within their tenant scope.
2. **Assignee Eligibility Validation Against Issue Entity:**
   - Validated candidate assignees in `validateAssigneeEligibility()` against the underlying entity in the database (`Trainee`, `AttendanceRecord`, `PlacementRecord`, `EmploymentRecord`, `BatchEnrollment`), not merely the caller's tenancy fields.
   - Enforced unambiguous non-null assignee scope; assignees cannot have all-null tenant fields (null is not treated as unrestricted) or conflicting provider and employer associations.
   - Disallowed cross-state, cross-provider, and cross-employer assignments.
   - Enforced that while nationwide administrators may initiate assignments across tenants, the selected assignee must strictly match the issue's tenant scope.
3. **Transition Matrix & Ownership Enforcement:**
   - Formalized approved transitions: `OPEN -> ASSIGNED`, `ASSIGNED -> IN_PROGRESS`, `IN_PROGRESS -> RESOLVED`, `OPEN -> DISMISSED`, `IN_PROGRESS -> DISMISSED`, and `RESOLVED -> REOPENED`.
   - Prohibited invalid transitions with HTTP 409 Conflict (`INVALID_DQ_TRANSITION`). Specifically corrected `dismissIssue()` to accept ONLY `OPEN -> DISMISSED` and `IN_PROGRESS -> DISMISSED`, explicitly rejecting `ASSIGNED -> DISMISSED` and `REOPENED -> DISMISSED`.
   - Enforced investigator ownership: `startInvestigation()` and `resolveIssue()` require the actor to be the assigned investigator (`caller.getId().equals(issue.getAssignedUserId())`) or an authorized administrative supervisor (`isAdministrativeSupervisor(caller)`). Unauthorized actors receive HTTP 403 Forbidden.
4. **Elimination of Fail-Open Fallbacks:**
   - Made `DataQualityWorkflowService` a mandatory constructor-injected dependency in `DataQualityIssueController`; removed legacy fallbacks that could bypass workflow guards.
   - Made `JdbcTemplate` mandatory and non-null in `DataQualityWorkflowServiceImpl`, ensuring fail-closed execution without skipping pessimistic locking or tenant validation.
5. **Protection of Lifecycle History on Generic PUT:**
   - Enforced strict immutability of lifecycle metadata on generic `PUT /api/v1/data-quality-issues/{id}` (`updateIssue`).
   - Direct status alteration is rejected with HTTP 400 Bad Request and error code `LIFECYCLE_BYPASS_PROHIBITED`.
   - Direct assignment alteration is rejected with HTTP 400 Bad Request and error code `ASSIGNMENT_BYPASS_PROHIBITED`.
   - Direct resolution alteration is rejected with HTTP 400 Bad Request and error code `RESOLUTION_BYPASS_PROHIBITED`.
6. **Append-Only Event Trail Auditability:**
   - Transition events (`ASSIGNMENT`, `INVESTIGATION`, `RESOLUTION`, `DISMISSAL`, `REOPEN`) are inserted atomically alongside issue state updates using supported schema columns (`data_quality_issue_id`, `data_quality_issue_status_id`, `changed_by_user_id`, `changed_at`, `remarks`, `created_at`).
   - Comprehensive event history endpoint (`GET /api/v1/data-quality-issues/{id}/events`) exposes chronologically ordered events.
7. **Synergy with Phase 28.3 Deduplication:**
   - Active issues (`ASSIGNED`, `IN_PROGRESS`, `REOPENED`) retain `is_open_flag = 1`. Scans by `DataQualityIssueRegistrationService` correctly detect active issues and return `RegistrationStatus.DEDUPLICATED` without creating duplicate records.
8. **Architectural & Schema Integrity:**
   - Schema validation (`ddl-auto=validate`) verified against `SIH26135_TEST` catalog with zero schema alterations.
   - Exactly 137 Spring Data JPA repository interfaces and exactly 2 `@Query` annotations preserved.
   - Exactly 63 production controllers verified; all 659 audited endpoints return standard `ApiResponse` envelopes.
   - Full regression suite achieved 100% pass rate: **1,882 / 1,882 tests passed** (0 failures, 0 errors, 0 skipped).

---

## 2. Architecture & State Machine Specification

### 2.1 State Transition Flow

The following state machine governs the lifecycle of every `DataQualityIssue`:

```mermaid
flowchart TD
    OPEN(["OPEN (Reported / Detected)"]) -->|assignIssue| ASSIGNED["ASSIGNED (Allocated to Investigator)"]
    ASSIGNED -->|startInvestigation| IN_PROGRESS["IN_PROGRESS (Under Active Investigation)"]
    IN_PROGRESS -->|resolveIssue| RESOLVED(["RESOLVED (Remediated)"])
    RESOLVED -->|reopenIssue| REOPENED(["REOPENED (Re-evaluated / Regressed)"])
    REOPENED -->|assignIssue| ASSIGNED
    
    OPEN -->|dismissIssue| DISMISSED(["DISMISSED (False Positive / Rejected)"])
    IN_PROGRESS -->|dismissIssue| DISMISSED

    classDef terminal fill:#f9f,stroke:#333,stroke-width:2px;
    classDef intermediate fill:#bbf,stroke:#333,stroke-width:1px;
    classDef initial fill:#dfd,stroke:#333,stroke-width:2px;
    
    class OPEN initial;
    class ASSIGNED,IN_PROGRESS,REOPENED intermediate;
    class RESOLVED,DISMISSED terminal;
```

### 2.2 Transition Matrix & Invariants

| Initial State | Target State | Endpoint Trigger | Mandatory Request Fields | Invariants & Business Validations |
| :--- | :--- | :--- | :--- | :--- |
| `OPEN` | `ASSIGNED` | `POST /{id}/assign` | `assignedUserId` | Assignee must exist, be active, not be a trainee, have non-null unambiguous tenancy, and match entity tenant scope. Status updated to `ASSIGNED`. Appends event. |
| `ASSIGNED` | `IN_PROGRESS` | `POST /{id}/start-investigation` | `remarks` (optional, max 500) | Caller must be assigned investigator or supervisor/admin. Status updated to `IN_PROGRESS`. Appends event. |
| `IN_PROGRESS` | `RESOLVED` | `POST /{id}/resolve` | `resolutionNotes` (non-blank, max 500) | Caller must be assigned investigator or supervisor/admin. Server derives `resolved_by_user_id` and `resolved_at`. Enforces `resolved_at >= detected_at`. Sets `is_open_flag = 0`. Appends event. |
| `OPEN` | `DISMISSED` | `POST /{id}/dismiss` | `remarks` (non-blank, max 500) | Caller must possess authorized tenant or admin scope. Sets `is_open_flag = 0`. Appends event. |
| `IN_PROGRESS` | `DISMISSED` | `POST /{id}/dismiss` | `remarks` (non-blank, max 500) | Caller must be assigned investigator or supervisor/admin. Sets `is_open_flag = 0`. Appends event. |
| `RESOLVED` | `REOPENED` | `POST /{id}/reopen` | `reopeningReason` (non-blank, max 500) | Caller must possess authorized tenant or admin scope. Clears `resolved_by_user_id`, `resolved_at`, and `resolution_notes`. Sets `is_open_flag = 1`. Appends event. |
| `REOPENED` | `ASSIGNED` | `POST /{id}/assign` | `assignedUserId` | Re-assigned to eligible investigator. Status updated to `ASSIGNED`. Appends event. |

### 2.3 Prohibited State Transitions (409 Conflict)

Any transition not explicitly permitted in the matrix is immediately rejected with HTTP 409 Conflict and error code `INVALID_DQ_TRANSITION`:

- `OPEN -> RESOLVED`: Issues cannot be directly resolved without investigation.
- `OPEN -> IN_PROGRESS`: Issues must first be assigned to an investigator.
- `ASSIGNED -> RESOLVED`: Investigation must formally begin before resolution.
- `ASSIGNED -> DISMISSED`: Assigned issues must either start investigation or be re-assigned; direct dismissal prohibited.
- `REOPENED -> DISMISSED`: Reopened issues must be re-assigned or investigated; direct dismissal prohibited.
- `RESOLVED -> DISMISSED`: A resolved issue cannot be dismissed; it must be reopened if invalid.
- `DISMISSED -> RESOLVED` / `DISMISSED -> REOPENED`: Dismissed issues are permanently terminal.
- `IN_PROGRESS -> ASSIGNED`: Reassignment must not bypass workflow state.

---

## 3. Concurrency, Locking & Audit Trail

### 3.1 Row-Level Pessimistic Locking

To prevent race conditions, concurrent conflicting state transitions, or simultaneous resolutions:
1. Every state transition begins by executing a pessimistic lock query via `JdbcTemplate`:
   ```sql
   SELECT data_quality_issue_id, data_quality_issue_status_id, is_open_flag, detected_at,
          entity_type, entity_id, assigned_user_id, resolved_by_user_id, resolved_at, resolution_notes
   FROM data_quality_issues
   WHERE data_quality_issue_id = ? FOR UPDATE
   ```
2. The issue record is loaded, and its current status is verified within the lock.
3. If another transaction has modified the issue state concurrently, the incoming transaction observes the updated status and rejects invalid transitions deterministically.

### 3.2 Atomic Event Logging (`data_quality_issue_events`)

Every successful lifecycle transition inserts an audit record into `data_quality_issue_events` within the same database transaction:
- `data_quality_issue_id`: ID of the target issue.
- `data_quality_issue_status_id`: Target status ID.
- `changed_by_user_id`: ID of the authenticated caller.
- `changed_at`: Timestamp of the transition.
- `remarks`: Transition rationale, notes, or assignment remarks (up to 500 characters).
- `created_at`: Creation timestamp.

### 3.3 Database Check Constraint Compliance

The schema contains the strict MySQL check constraint:
```sql
CONSTRAINT `ck_dq_issues_resolved_at` CHECK (`resolved_at` >= `detected_at`)
```
Application logic rigorously validates this temporal invariant:
- Server assigns `resolved_at = LocalDateTime.now()`.
- If `resolved_at.isBefore(detectedAt)`, the operation fails with HTTP 400 Bad Request, safeguarding the database constraint.
- When an issue is reopened, `resolved_by_user_id`, `resolved_at`, and `resolution_notes` are set to `NULL`, which is schema-valid and satisfies the check constraint while preserving previous resolution details in `data_quality_issue_events`.

---

## 4. Security, Authorization & Tenancy Boundaries

Issue lifecycle endpoints enforce multi-tenant isolation via `WorkflowSecurityScopeValidator` and service-level verification:

1. **Permission Separation:**
   - Management operations (`/assign`, `/start-investigation`, `/resolve`, `/dismiss`, `/reopen`, `POST /`, `PUT /{id}`) strictly require `system.manage`.
   - Read operations (`GET /{id}`, `GET /`, `GET /{id}/events`) permit `audit.read` or `system.manage`.
2. **Nationwide Administrators (`ROLE_super_admin`, unrestricted `ROLE_government_admin`):**
   - Unrestricted authority across all issues, entities, and states.
   - May assign issues across tenants, but the target assignee must match the issue's tenant scope.
3. **State Administrators (`ROLE_state_admin`):**
   - Restricted to issues associated with entities located within their assigned `state_id`.
   - Entity tenancy is verified by inspecting underlying tables (`trainees`, `attendance_records`, `placement_records`, `employment_records`, `batch_enrollments`).
   - Blocked from operating on out-of-scope issues (HTTP 403 Forbidden).
4. **Training Providers (`ROLE_training_provider`):**
   - Restricted to issues affecting trainees, batches, or centers owned by their `training_provider_id`.
   - Blocked from accessing or resolving other providers' issues.
5. **Employers (`ROLE_employer`):**
   - Restricted to placement or employment verification issues associated with their `employer_id`.
6. **Trainees (`ROLE_trainee`):**
   - Strictly denied from all data quality management operations (HTTP 403 Forbidden).
   - Ineligible to be assigned any data quality issues.
7. **Assignee Tenancy Validation:**
   - An issue cannot be assigned to an unscoped user (null tenancy fields) or a user whose tenancy does not match the issue's entity scope (`ASSIGNEE_SCOPE_MISMATCH`, 400 Bad Request).
   - Assignees must be active accounts.

---

## 5. API Endpoints & Request/Response Contracts

### 5.1 Assign Issue
- **Method / Path:** `POST /api/v1/data-quality-issues/{id}/assign`
- **Security:** `@PreAuthorize("hasAuthority('system.manage')")`
- **Request Parameters / Body:** Supports both request body `{"assignedUserId": 42}` and backward-compatible query param `?assignedUserId=42`.
- **Response:** `200 OK` with enveloped `ApiResponse<DataQualityIssueResponse>`.

### 5.2 Start Investigation
- **Method / Path:** `POST /api/v1/data-quality-issues/{id}/start-investigation`
- **Security:** `@PreAuthorize("hasAuthority('system.manage')")`
- **Request Body:**
  ```json
  {
    "remarks": "Reviewing biometric attendance discrepancies for Batch 102."
  }
  ```
- **Response:** `200 OK` with enveloped `ApiResponse<DataQualityIssueResponse>`.

### 5.3 Resolve Issue
- **Method / Path:** `POST /api/v1/data-quality-issues/{id}/resolve`
- **Security:** `@PreAuthorize("hasAuthority('system.manage')")`
- **Request Body:**
  ```json
  {
    "resolutionNotes": "Verified candidate Aadhaar and updated matching phone number."
  }
  ```
- **Response:** `200 OK` with enveloped `ApiResponse<DataQualityIssueResponse>`.

### 5.4 Dismiss Issue
- **Method / Path:** `POST /api/v1/data-quality-issues/{id}/dismiss`
- **Security:** `@PreAuthorize("hasAuthority('system.manage')")`
- **Request Body:**
  ```json
  {
    "remarks": "False positive: candidate has authorized exemption under Section 4."
  }
  ```
- **Response:** `200 OK` with enveloped `ApiResponse<DataQualityIssueResponse>`.

### 5.5 Reopen Issue
- **Method / Path:** `POST /api/v1/data-quality-issues/{id}/reopen`
- **Security:** `@PreAuthorize("hasAuthority('system.manage')")`
- **Request Body:**
  ```json
  {
    "reopeningReason": "Audit review showed secondary enrollment was not terminated."
  }
  ```
- **Response:** `200 OK` with enveloped `ApiResponse<DataQualityIssueResponse>`.

### 5.6 Issue Event History
- **Method / Path:** `GET /api/v1/data-quality-issues/{id}/events`
- **Security:** `@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")`
- **Response:** `200 OK` with enveloped `ApiResponse<List<DataQualityIssueEventResponse>>`.

---

## 6. Implementation & Correction Summary

### 6.1 Modified Components

| Component / File | Purpose & Corrections |
| :--- | :--- |
| `DataQualityIssueController.java` | Injected mandatory `DataQualityWorkflowService`; removed fail-open legacy fallbacks; applied `@PreAuthorize("hasAuthority('system.manage')")` to all write endpoints. |
| `DataQualityWorkflowServiceImpl.java` | Enforced read/write permission separation (`system.manage` vs `audit.read`); implemented entity-level `validateAssigneeEligibility()`; restricted `dismissIssue()` to OPEN and IN_PROGRESS; enforced investigator ownership with supervisor override in `startInvestigation()`, `resolveIssue()`, and `dismissIssue()`; made `JdbcTemplate` mandatory; eliminated fail-open bypasses. |
| `DataQualityIssueServiceImpl.java` | Protected lifecycle history against generic `PUT /{id}` updates (`LIFECYCLE_BYPASS_PROHIBITED`, `ASSIGNMENT_BYPASS_PROHIBITED`, `RESOLUTION_BYPASS_PROHIBITED`). |
| `DataQualityWorkflowServiceTest.java` | Expanded unit and state machine test suite to 52 tests covering permission separation, assignee scope matching, invalid dismissals (409), investigator ownership (403), supervisor overrides, and event persistence. |
| `DataQualityWorkflowApiTest.java` | Expanded integration test suite to 26 tests covering live MySQL verification of permission separation, adversarial nationwide-admin assignment to out-of-scope users, generic PUT lifecycle protections, check constraints, and Phase 28.3 deduplication synergy. |
| `ResponseEnvelopeAlignmentContractTest.java` | Audited and verified all 63 production REST controllers and 659 endpoints returning standard `ApiResponse` envelope. |

---

## 7. Verification & Test Evidence

### 7.1 Unit & State Machine Test Suite (`DataQualityWorkflowServiceTest`)

- Total Tests: **52**
- Status: **PASSED (100%)**
- Test Coverage Breakdown:
  - Transition validity: All allowable state transitions validated.
  - Transition rejection: Every disallowed transition confirmed to throw `InvalidDataQualityTransitionException` (409 Conflict), including `ASSIGNED -> DISMISSED` and `REOPENED -> DISMISSED`.
  - Investigator ownership: `startInvestigation()` and `resolveIssue()` restrict execution to assigned investigator or administrative supervisor (403 Forbidden for unauthorized callers).
  - Validation guards: Null, blank, whitespace-only, and oversized (>500 chars) notes/remarks rejected with `BadRequestException` (400 Bad Request).
  - Assignee eligibility: Inactive users, trainee accounts, unscoped users, and cross-tenant assignees rejected with `BadRequestException` (`ASSIGNEE_SCOPE_MISMATCH`).
  - Read/write permission separation: Verified `system.manage` enforcement for managers and `audit.read` permission for event readers within tenant scope.
  - Event trail: Event generation verified with exact status mappings, actor attribution, and remarks.

### 7.2 Integration Test Suite (`DataQualityWorkflowApiTest`)

- Total Tests: **26**
- Status: **PASSED (100%)**
- Execution Environment: Spring Boot Test with live MySQL `SIH26135_TEST` catalog.
- Test Coverage Breakdown:
  - Full end-to-end lifecycle: `OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> REOPENED`.
  - Read/Write permission separation: Callers with `audit.read` alone rejected with 403 Forbidden across all lifecycle mutations, while permitted to view events within tenant scope.
  - Adversarial assignment tests: Nationwide admin attempting assignment to an assignee outside issue tenant scope rejected with 400 Bad Request (`ASSIGNEE_SCOPE_MISMATCH`); unscoped assignee rejected.
  - Dismissal constraints: Rejection of `ASSIGNED -> DISMISSED` and `REOPENED -> DISMISSED` with 409 Conflict.
  - Generic PUT lifecycle protections: Rejection of direct status (`LIFECYCLE_BYPASS_PROHIBITED`), assignment (`ASSIGNMENT_BYPASS_PROHIBITED`), and resolution (`RESOLUTION_BYPASS_PROHIBITED`) alterations.
  - Database check constraint: Verified MySQL `ck_dq_issues_resolved_at` constraint actively blocks `resolved_at < detected_at`.
  - Phase 28.3 Deduplication synergy: Active statuses (`ASSIGNED`, `IN_PROGRESS`, `REOPENED`) properly deduplicated during scans.

### 7.3 Architectural Invariant Verification

1. **JPA Repository Count:**
   - Scanned: **137 JPA repository interfaces**
   - Verified: Exactly 137 interfaces preserved (0 added).
2. **`@Query` Annotation Invariant:**
   - Scanned: **2 `@Query` annotations** across entire codebase (`TraineeUnemploymentEventRepository:19`, `UserRepository:22`).
   - Verified: Exactly 2 annotations preserved (0 added).
3. **Production Controller Count & Envelope Alignment:**
   - Scanned: **63 production REST controllers** across all modules.
   - Scanned: **659 endpoint methods**.
   - Verified: 100% of endpoints return `ResponseEntity<ApiResponse<T>>` or `ResponseEntity<StreamingResponseBody>`.
   - Contract test: `ResponseEnvelopeAlignmentContractTest` passed.

### 7.4 Full Regression Test Results

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
...
[INFO] Results:
[INFO] 
[INFO] Tests run: 1882, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  05:24 min
[INFO] Finished at: 2026-11-11T20:50:40+05:30
[INFO] ------------------------------------------------------------------------
```

---

## 8. Architectural Invariants Compliance Matrix

| Invariant / Constraint | Requirement | Actual Status | Evidence |
| :--- | :--- | :--- | :--- |
| **Database Catalog Isolation** | Strictly `SIH26135_TEST` | **COMPLIANT** | Zero queries to `SIH26135`. `spring.jpa.hibernate.ddl-auto=validate` confirmed. |
| **Database Schema** | Zero schema modifications | **COMPLIANT** | No tables, columns, indexes, or constraints added or modified. |
| **JPA Repositories** | Exactly 137 interfaces | **COMPLIANT** | Verified by Spring Data scanner and `OperationalErrorCodeContractTest`. |
| **`@Query` Annotations** | Exactly 2 annotations | **COMPLIANT** | Verified by `OperationalErrorCodeContractTest`. |
| **REST Controllers** | Exactly 63 controllers | **COMPLIANT** | Verified by `ResponseEnvelopeAlignmentContractTest`. |
| **API Envelope Uniformity** | 100% `ApiResponse` | **COMPLIANT** | All 659 endpoints validated by `ResponseEnvelopeAlignmentContractTest`. |
| **MySQL Check Constraint** | `ck_dq_issues_resolved_at` | **COMPLIANT** | Verified by unit tests, integration tests, and schema constraint checks. |
| **Full Regression Suite** | 100% Pass Rate | **COMPLIANT** | **1,882 / 1,882 tests passing** (0 failures, 0 errors, 0 skipped). |
| **Git Hygiene** | Zero commits, push, stash, or reset | **COMPLIANT** | Working directory clean; no git commits created. |

---

## 9. Review Status & Sign-off

Phase 28.4 — Data Quality Issue Lifecycle, Assignment & Remediation Workflow is **COMPLETED**, **VERIFIED**, and **READY FOR INDEPENDENT REVIEW**.
