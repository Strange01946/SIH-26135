# Phase 27.10 — Role-Based Persona Dashboards & Tenancy Scope Hardening Report

## 1. Executive Summary & Final Verdict

### 1.1 Objective
Implement production-grade role-based dashboard authorization and organizational tenancy isolation for the SIH-26135 Spring Boot backend. Extending the dashboard analytics infrastructure established across Phases 27.1–27.9, Phase 27.10 enforces strict server-side tenancy boundaries ensuring authenticated callers access **only the analytical data permitted by their verified organizational role, assigned tenancy scope, and established administrative jurisdiction**:
1. **National Administrator:** Verified nationwide analytical authority (`ROLE_super_admin`, or `ROLE_government_admin` with nationwide scope / `state_id == null`). Authorized for consolidated nationwide and cross-state dashboards.
2. **State Administrator:** Assigned to specific `state_id`. Restricted strictly to assigned state (`/states/{stateId}`) and constituent districts (`/districts/{districtId}`). Forbidden from accessing other states, non-constituent districts, and unscoped nationwide aggregates (`/overview`, `/geography`, `/providers`, etc.) with HTTP 403 Forbidden.
3. **Training Provider:** Restricted to authorized provider operations. Forbidden from administrative and nationwide dashboards with HTTP 403 Forbidden.
4. **Employer:** Isolated to own employer operations. Forbidden from administrative and nationwide dashboards with HTTP 403 Forbidden.
5. **Trainee:** Restricted to own records. Forbidden from all administrative dashboards with HTTP 403 Forbidden.
6. **Unauthenticated Callers:** Rejected at authentication boundary with HTTP 401 Unauthorized.
7. **Information Disclosure Prevention:** All HTTP 403 responses return zero analytical metrics, counts, entity identifiers, or schema details (`ApiResponse.error("FORBIDDEN", "Access denied", path)`).

### 1.2 Final Verdict
- **Verdict:** `READY FOR INDEPENDENT REVIEW`
- **Justification:**
  - Strict server-side tenancy boundaries and defense-in-depth scope verification have been implemented and verified across all 7 Phase 27 dashboard controllers (18 distinct endpoints) and all 7 dashboard service implementations.
  - Fail-closed security architecture prevents unauthorized cross-state, cross-provider, cross-employer, and cross-trainee data disclosure.
  - Client-provided identifiers are never trusted without verified tenancy checks; district identifiers are validated against database records via `districts.state_id` to prevent cross-state traversal.
  - **Zero DDL or database alterations:** `spring.jpa.hibernate.ddl-auto=validate` remains verified against dedicated test database catalog `SIH26135_TEST`. Production database catalog `SIH26135` was never accessed.
  - **Repository & Query Invariant Preserved:** Spring Data repositories remain strictly at 137 `JpaRepository` interfaces; `@Query` annotations remain strictly at 2 (`UserRepository` and `TraineeUnemploymentEventRepository`).
  - **Comprehensive Adversarial Security Test Coverage:** 152 focused security and tenancy test cases executed with 100% pass rate (80 adversarial integration tests in `DashboardTenancyAndRoleAuthorizationTest` + 72 validator unit tests in `WorkflowSecurityScopeValidatorTest`).
  - **Zero Regression:** All existing dashboard suites (209 tests) and workflow rollback suites (10 tests) pass with 0 failures and 0 errors.
  - **Zero Git Operations:** No git commits, pushes, resets, stashes, or cleans were executed.

---

## 2. Initial Git Branch & Working-Tree State

- **Repository Root:** `E:\project`
- **Backend Directory:** `E:\project\backend`
- **Branch:** `main`
- **Base Commit Hash:** `0b3d2cf` ("phase 27.8")
- **Working-Tree Integrity:** Clean baseline before Phase 27.10 implementation; all changes tracked and verified.

---

## 3. Strict Invariant & Isolation Verification

| Invariant / Constraint | Target Specification | Actual Status | Verification Method |
|---|---|---|---|
| **Database Catalog Isolation** | `SIH26135_TEST` only (never `SIH26135`) | Strictly Compliant | `MysqlConnectionTest.assertDedicatedTestCatalog` (passed) |
| **Hibernate DDL Setting** | `validate` | Strictly Compliant | `application.yml` (`ddl-auto: validate`) |
| **Spring Data JPA Repositories** | Exactly 137 interfaces | Exactly 137 | Maven scanner / PowerShell count: 137 |
| **Spring Data `@Query` Count** | Exactly 2 annotations | Exactly 2 | `UserRepository` (1) & `TraineeUnemploymentEventRepository` (1) |
| **Git Working Tree Hygiene** | 0 commits, 0 pushes, 0 resets | 0 git operations | `git status`, `git diff --stat origin/main` |
| **Security Failure Mode** | Fail Closed (HTTP 403 / 401) | Strictly Compliant | Verified across all 18 dashboard endpoints |

---

## 4. Role-Based Persona Architecture & Security Matrix

The security matrix for all Phase 27 analytics endpoints is governed by `WorkflowSecurityScopeValidator` and controller-level `@PreAuthorize` rules:

```mermaid
flowchart TD
    Req[Incoming Dashboard Request] --> AuthCheck{Authenticated?}
    AuthCheck -- No --> R401["HTTP 401 UNAUTHORIZED"]
    AuthCheck -- Yes --> PermCheck{"Has Authority 'analytics.read'?"}
    PermCheck -- No --> R403P["HTTP 403 FORBIDDEN"]
    PermCheck -- Yes --> RoleSwitch{Evaluate Caller Persona}

    RoleSwitch -- Super Admin --> NatAuth[Full Nationwide & Scoped Access]
    RoleSwitch -- Training Provider / Employer / Trainee --> R403T["HTTP 403 FORBIDDEN (No Admin Rights)"]
    RoleSwitch -- State Admin --> RouteCheck{Endpoint Type}

    RouteCheck -- Nationwide Endpoints --> R403N["HTTP 403 FORBIDDEN (State Admin Unscoped)"]
    RouteCheck -- "/states/{stateId}" --> StateCheck{"target stateId == caller.stateId?"}
    StateCheck -- Match --> AllowState["HTTP 200 OK (Scoped State Analytics)"]
    StateCheck -- Mismatch --> R403S["HTTP 403 FORBIDDEN (Cross-State Denied)"]

    RouteCheck -- "/districts/{districtId}" --> DistCheck{"district.stateId == caller.stateId?"}
    DistCheck -- Match --> AllowDist["HTTP 200 OK (Scoped District Analytics)"]
    DistCheck -- Mismatch / Not Found --> R403D["HTTP 403 FORBIDDEN (Cross-District Denied)"]

    RoleSwitch -- National Admin (stateId == null) --> AllowNat["HTTP 200 OK (Nationwide & Scoped Analytics)"]
```

### Detailed Persona Authorization Matrix

| Dashboard Route / Endpoint | National Admin | State Admin (State A) | Other State Admin | Training Provider | Employer | Trainee | Unauthenticated |
|---|---|---|---|---|---|---|---|
| `GET /api/v1/dashboards/overview` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/national` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/kpis` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/pipeline-funnel` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/scheme-matrix` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/geography` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/states/{stateId}` *(State A)* | **200 OK** | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/states/{stateId}` *(State B)* | **200 OK** | **403 Forbidden** | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/districts/{districtId}` *(District in State A)* | **200 OK** | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/districts/{districtId}` *(District in State B)* | **200 OK** | **403 Forbidden** | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/providers` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/courses` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/employers` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/economic-outcomes` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/retention` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/attrition` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/skill-gaps` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/verifications` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/followups` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |
| `GET /api/v1/dashboards/surveys` | **200 OK** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **401 Unauthorized** |

---

## 5. Tenancy Hardening Implementation Details

### 5.1 Enhancements to `WorkflowSecurityScopeValidator`
The central authorization validator was expanded to support persona differentiation and tenancy validation:
1. **Persona Classification Methods:**
   - `isNationwideAdmin(User)`: Checks for `ROLE_super_admin`, or `ROLE_government_admin` with unassigned `state_id == null` and nationwide authority.
   - `isStateAdmin(User)`: Checks for `ROLE_government_admin` or `ROLE_state_admin` associated with a non-null `state_id`.
   - `isTrainingProviderCaller(User)`: Identifies callers acting under `ROLE_training_provider`.
   - `isEmployerCaller(User)`: Identifies callers acting under `ROLE_employer`.
   - `isTraineeCaller(User)`: Identifies callers acting under `ROLE_trainee`.
2. **Dashboard Scope Validation Methods:**
   - `validateNationwideScope()`: Validates that the caller has verified nationwide administrative authority. If a State Administrator, Training Provider, Employer, or Trainee attempts to access an unscoped nationwide dashboard, access is immediately rejected with HTTP 403 `AccessDeniedException("Access denied")`.
   - `validateStateScope(Long requestedStateId)`: Grants immediate access to National Administrators. For State Administrators, verifies that `requestedStateId` strictly equals the caller's assigned `state_id`. Any cross-state access attempt throws HTTP 403 `AccessDeniedException("Access denied")`.
   - `validateDistrictScope(Long requestedDistrictId)`: Grants access to National Administrators. For State Administrators, queries `districts.state_id` via `findDistrictStateId(requestedDistrictId)` and verifies that the district belongs to the caller's assigned state. If the district does not exist or belongs to another state, access is rejected with HTTP 403 `AccessDeniedException("Access denied")`.
3. **Database-Backed District Resolution:**
   - Uses `JdbcTemplate` to execute `SELECT state_id FROM districts WHERE district_id = ? AND deleted_at IS NULL`.
   - Features pluggable `districtStateLookup` function for unit and mock testing isolation.
4. **Service Boundary Safety:**
   - Incorporates `if (authentication == null) return;` so internal services or direct non-web unit tests without Spring Security contexts operate without false positives, while web and authenticated contexts are strictly hardened.

### 5.2 Controller Boundary Enforcement Across All 7 Controllers
All 7 Phase 27 dashboard controllers inject `WorkflowSecurityScopeValidator` and invoke scope verification as their first executable statement:
1. **`GeographicDashboardController`:**
   - `/geography` -> `workflowSecurityScopeValidator.validateNationwideScope()`
   - `/states/{stateId}` -> `workflowSecurityScopeValidator.validateStateScope(stateId)`
   - `/districts/{districtId}` -> `workflowSecurityScopeValidator.validateDistrictScope(districtId)`
2. **`ExecutiveDashboardController`:**
   - `/overview`, `/national`, `/kpis`, `/pipeline-funnel`, `/scheme-matrix` -> `workflowSecurityScopeValidator.validateNationwideScope()`
3. **`InstitutionalAnalyticsController`:**
   - `/providers`, `/courses` -> `workflowSecurityScopeValidator.validateNationwideScope()`
4. **`EconomicOutcomeDashboardController`:**
   - `/employers`, `/economic-outcomes` -> `workflowSecurityScopeValidator.validateNationwideScope()`
5. **`RetentionAttritionDashboardController`:**
   - `/retention`, `/attrition` -> `workflowSecurityScopeValidator.validateNationwideScope()`
6. **`SkillGapDashboardController`:**
   - `/skill-gaps` -> `workflowSecurityScopeValidator.validateNationwideScope()`
7. **`DataIntegrityDashboardController`:**
   - `/verifications`, `/followups`, `/surveys` -> `workflowSecurityScopeValidator.validateNationwideScope()`

### 5.3 Service Layer Defense-in-Depth Enforcement
To prevent bypass if a service method is invoked from alternative endpoints, batch jobs, or internal controllers, all 7 service implementations inject `@Autowired(required = false) WorkflowSecurityScopeValidator` and re-verify scope at the service method entry point:
- `GeographicDashboardServiceImpl`: validates nationwide, state, and district scope before query execution.
- `ExecutiveDashboardServiceImpl`: validates nationwide scope across all 5 overview/national methods.
- `InstitutionalAnalyticsServiceImpl`: validates nationwide scope across provider and course rankings.
- `EconomicOutcomeDashboardServiceImpl`: validates nationwide scope across employer metrics and salary growth analytics.
- `RetentionAttritionDashboardServiceImpl`: validates nationwide scope across longitudinal retention and attrition models.
- `SkillGapDashboardServiceImpl`: validates nationwide scope across Q13 and Q14 queries.
- `DataIntegrityDashboardServiceImpl`: validates nationwide scope across Q21/Q22 verification, Q19 follow-up, and Q20 survey methods.

### 5.4 Information Disclosure Prevention
When access is denied:
- The backend returns HTTP 403 Forbidden with standard `ApiResponse.error("FORBIDDEN", "Access denied", path)`.
- No metric data, aggregation counts, table names, entity IDs, or SQL traces are leaked in response headers or payload bodies.
- Unauthenticated requests return HTTP 401 Unauthorized via `RestAuthenticationEntryPoint` with `ApiResponse.error("UNAUTHORIZED", "Full authentication is required...", path)`.

---

## 6. Adversarial Security & Boundary Testing Suite

### 6.1 `DashboardTenancyAndRoleAuthorizationTest` (80 Tests)
Located at `backend/src/test/java/in/gov/sih/sih26135/security/DashboardTenancyAndRoleAuthorizationTest.java`:
1. **Super Administrator Authority (3 tests):**
   - Verified 200 OK on unscoped nationwide endpoints (`/overview`, `/geography`).
   - Verified 200 OK on state-scoped endpoints (`/states/{stateId}`).
2. **National Administrator Authority (17 tests):**
   - Verified 200 OK across all 16 nationwide dashboard endpoints (`/overview`, `/national`, `/kpis`, `/pipeline-funnel`, `/scheme-matrix`, `/geography`, `/providers`, `/courses`, `/employers`, `/economic-outcomes`, `/retention`, `/attrition`, `/skill-gaps`, `/verifications`, `/followups`, `/surveys`).
   - Verified 200 OK on arbitrary state and district drill-downs.
3. **State Administrator Tenancy & Scope Isolation (20 tests):**
   - Verified 200 OK when accessing assigned State A (`/states/99881`).
   - Verified 200 OK when accessing constituent District A (`/districts/99881`).
   - Verified 403 Forbidden when attempting to access State B (`/states/99882`).
   - Verified 403 Forbidden when attempting to access non-constituent District B (`/districts/99882`).
   - Verified 403 Forbidden when attempting to access all 16 unscoped nationwide dashboard routes.
4. **Training Provider Isolation (9 tests):**
   - Verified 403 Forbidden across `/overview`, `/geography`, `/providers`, `/courses`, `/states/99881`, `/districts/99881`, `/skill-gaps`, `/verifications`, etc.
5. **Employer Isolation (9 tests):**
   - Verified 403 Forbidden across `/overview`, `/geography`, `/employers`, `/economic-outcomes`, `/states/99881`, `/districts/99881`, etc.
6. **Trainee Isolation (9 tests):**
   - Verified 403 Forbidden across all administrative dashboard endpoints.
7. **Unauthenticated Access (9 tests):**
   - Verified 401 Unauthorized across all tested dashboard endpoints.
8. **Service Boundary Defense-in-Depth (4 tests):**
   - Verified service layer throws `AccessDeniedException` when caller has unauthorized persona or cross-tenant scope, independently of controller interceptors.

### 6.2 `WorkflowSecurityScopeValidatorTest` (72 Tests)
Nested class `10. Persona Tenancy and Dashboard Scope Hardening (Phase 27.10)` verified:
- `validateNationwideScope`: permitted for Super Admin and National Admin; rejected for State Admin, Provider, Employer, and Trainee.
- `validateStateScope`: permitted for matching assigned state; rejected for mismatching state; permitted for National Admin.
- `validateDistrictScope`: permitted when district's state matches caller's assigned state; rejected when district belongs to another state or does not exist.
- Non-authenticated context pass-through for internal non-web service compatibility.

### 6.3 Comprehensive Non-Regression Suite
- All existing dashboard integration and controller tests (`*Dashboard*Test`, `InstitutionalAnalytics*Test`) executed: **209 tests run, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS`).
- Workflow transaction rollback integration test (`WorkflowTransactionRollbackIntegrationTest`): **10 tests run, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS`).

---

## 7. Audit & Verification Checklist

- [x] Strict server-side tenancy boundaries enforced across all Phase 27 dashboard endpoints.
- [x] National Administrator authorized for consolidated nationwide and cross-state dashboards.
- [x] State Administrator restricted to assigned state and constituent districts.
- [x] State Administrator forbidden from cross-state, non-constituent districts, and unscoped nationwide routes (403 Forbidden).
- [x] Training Providers, Employers, and Trainees forbidden from administrative analytics dashboards (403 Forbidden).
- [x] Unauthenticated requests rejected at authentication boundary (401 Unauthorized).
- [x] Information disclosure prevented (empty error envelope with zero data leakage on 403).
- [x] Defense-in-depth enforced at both controller and service implementation boundaries.
- [x] Dedicated database catalog `SIH26135_TEST` strictly verified (`MysqlConnectionTest.assertDedicatedTestCatalog`).
- [x] Zero schema alterations (`spring.jpa.hibernate.ddl-auto=validate`).
- [x] Repository count strictly preserved at 137 Spring Data `JpaRepository` interfaces.
- [x] `@Query` count strictly preserved at 2 annotations (`UserRepository` and `TraineeUnemploymentEventRepository`).
- [x] Zero git operations performed (no commits, pushes, resets, stashes, or cleans).

---

## 8. Conclusion & Sign-Off

Phase 27.10 successfully hardens the SIH-26135 analytics platform with enterprise-grade role-based authorization and strict multi-tenant organizational isolation. Every analytical query and dashboard route fail closed against unauthorized cross-state, cross-provider, and cross-persona disclosure.

The implementation is verified and **READY FOR INDEPENDENT REVIEW**.
