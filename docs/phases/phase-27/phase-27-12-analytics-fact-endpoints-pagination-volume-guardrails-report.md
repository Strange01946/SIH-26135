# Phase 27.12 — Analytics Fact Endpoints Pagination & Volume Guardrails Report

## 1. Executive Summary

Phase 27.12 addresses a critical scalability and performance risk in the SIH-26135 analytical fact infrastructure. Prior to this phase, all 11 low-level analytical fact collection endpoints exposed via `AnalyticsFactController.java` executed unbounded `findAll()` database queries, returning unbounded `List<T>` payloads. In large-scale government deployments with tens of thousands of trainees and millions of employment and exit events, unbounded retrieval risked severe JVM heap exhaustion, GC pauses, and denial-of-service vulnerabilities.

Phase 27.12 successfully replaces unbounded in-memory collection retrieval with **database-level, deterministic, bounded pagination guardrails** across all 11 analytical fact endpoints. Every fact collection route now returns `ApiResponse<PagedResponse<T>>`, enforces zero-based page indexing (`page >= 0`), strict bounded page sizes (`1 <= size <= 100`, default `size = 20`), sort property allowlists with custom primary key tie-breaking, strict query parameter whitelisting (`validatePaginationParametersOnly`), and nationwide tenancy authorization (`scopeValidator.validateNationwideScope()`).

All 43 focused tests in `AnalyticsFactPaginationTest.java` passed cleanly, and all 269 regression tests across pagination, validation, tenancy authorization, and multi-dimensional filtering passed with zero failures. Architectural invariants—including exactly 137 `JpaRepository` interfaces, exactly 2 `@Query` annotations, zero DDL mutations, and strict `SIH26135_TEST` database isolation—remain 100% intact.

---

## 2. Endpoint Inventory & Coverage Matrix

The following matrix documents all 11 analytical fact collection endpoints refactored under Phase 27.12:

| # | Endpoint Route | Analytical Fact Entity | Underlying DB View | Spring Data Repository | Authoritative Unique Key | Sort Allowlist Constant | Tenancy Scope Guard | Test Status |
|---|---|---|---|---|---|---|---|---|
| 1 | `GET /api/v1/analytics/facts/employment-exits` | `EmploymentExitFact` | `vw_employment_exit_fact` | `EmploymentExitFactRepository` | `employmentExitEventId` | `EMPLOYMENT_EXIT_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 2 | `GET /api/v1/analytics/facts/employments` | `EmploymentFact` | `vw_employment_fact` | `EmploymentFactRepository` | `employmentId` | `EMPLOYMENT_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 3 | `GET /api/v1/analytics/facts/employment-retentions` | `EmploymentRetentionFact` | `vw_employment_retention_fact` | `EmploymentRetentionFactRepository` | `employmentId` | `EMPLOYMENT_RETENTION_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 4 | `GET /api/v1/analytics/facts/employment-verifications` | `EmploymentVerificationFact` | `vw_employment_verification_fact` | `EmploymentVerificationFactRepository` | `employmentVerificationId` | `EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 5 | `GET /api/v1/analytics/facts/enrollment-outcomes` | `EnrollmentOutcomeFact` | `vw_enrollment_outcome_fact` | `EnrollmentOutcomeFactRepository` | `enrollmentId` | `ENROLLMENT_OUTCOME_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 6 | `GET /api/v1/analytics/facts/followups` | `FollowupFact` | `vw_followup_fact` | `FollowupFactRepository` | `followupTaskId` | `FOLLOWUP_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 7 | `GET /api/v1/analytics/facts/placements` | `PlacementFact` | `vw_placement_fact` | `PlacementFactRepository` | `placementId` | `PLACEMENT_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 8 | `GET /api/v1/analytics/facts/salary-progressions` | `SalaryProgressionFact` | `vw_salary_progression_fact` | `SalaryProgressionFactRepository` | `employmentId` | `SALARY_PROGRESSION_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 9 | `GET /api/v1/analytics/facts/skill-gaps` | `SkillGapFact` | `vw_skill_gap_fact` | `SkillGapFactRepository` | `skillGapId` | `SKILL_GAP_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 10 | `GET /api/v1/analytics/facts/survey-responses` | `SurveyResponseFact` | `vw_survey_response_fact` | `SurveyResponseFactRepository` | `surveyResponseId` | `SURVEY_RESPONSE_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |
| 11 | `GET /api/v1/analytics/facts/unemployments` | `UnemploymentFact` | `vw_unemployment_fact` | `UnemploymentFactRepository` | `unemploymentEventId` | `UNEMPLOYMENT_FACT_SORT_FIELDS` | `validateNationwideScope()` | **PASS** (200 OK / Paged) |

---

## 3. Database-Level Pagination & Volume Guardrails Architecture

### 3.1 Unbounded Query Elimination
- Prior implementation invoked `repository.findAll().stream().map(mapper::toResponse).toList()`, which forced the database to project all rows of the analytical SQL view into memory.
- Phase 27.12 refactored the 11 service interfaces and implementations to introduce `PagedResponse<TResponse> getAll...(Pageable pageable)`.
- Service implementations execute `Page<TEntity> page = repository.findAll(pageable);` and transform to standard DTOs via `PagedResponse.from(page, mapper::toResponse)`.
- Spring Data JPA generates native SQL `SELECT ... FROM <vw_fact> ORDER BY ... LIMIT ? OFFSET ?` queries. Only the requested slice (at most 100 rows) is fetched from MySQL into heap memory.

### 3.2 Authoritative Unique ID Tie-Breaker Strategy
- Standard Spring Data entities typically declare a primary key named `id`. However, the 11 analytical fact entities map directly to analytical views with explicit domain primary keys (`employmentExitEventId`, `employmentId`, `employmentVerificationId`, `enrollmentId`, `followupTaskId`, `placementId`, `skillGapId`, `surveyResponseId`, `unemploymentEventId`).
- Attempting to sort or tie-break on property `"id"` causes Hibernate / Spring Data to throw `PropertyReferenceException: No property 'id' found for type 'EmploymentExitFact'`.
- To resolve this elegantly while maintaining 100% backward compatibility with core domain pagination, `PaginationUtils.java` was enhanced with an overloaded `buildPageable` method accepting `String idProperty`:
```java
public static Pageable buildPageable(
    int page,
    int size,
    String sort,
    String direction,
    Map<String, String> allowedSortFields,
    String defaultSortProperty,
    String idProperty,
    Sort.Direction defaultDirection,
    Sort.Direction tieBreakerDirection)
```
- In each fact sort allowlist, `"id"` aliases to the entity's authoritative ID property.
- When sorting by any other property (e.g., `traineeId`), `PaginationUtils` automatically appends a secondary stable tie-breaker: `effectiveIdProp ASC` (e.g., `primarySort.and(Sort.by(effectiveTieBreakerDir, effectiveIdProp))`), guaranteeing deterministic pagination across database pages.

### 3.3 Strict Bounds & Guardrail Limits
- **Page Index (`page`)**: Zero-based. Enforces `page >= 0`. Negative values throw `BadRequestException("Page index must not be less than zero", "INVALID_PAGE")` (HTTP 400).
- **Page Size (`size`)**: Enforces `1 <= size <= 100`. Default is 20.
  - `size < 1` throws `BadRequestException("Page size must be at least 1", "INVALID_PAGE_SIZE")` (HTTP 400).
  - `size > 100` throws `BadRequestException("Page size must not exceed 100", "INVALID_PAGE_SIZE")` (HTTP 400).
- **Sort Field (`sort`)**: Validated against entity-specific allowlists. Unknown, unapproved, or sensitive fields (e.g., `password`, `secret`, `token`, `hash`) throw `BadRequestException("Unsupported sort field...", "INVALID_SORT_FIELD")` (HTTP 400).
- **Direction (`direction`)**: Case-insensitive `ASC` or `DESC`. Invalid values throw `BadRequestException("Invalid sort direction... Must be ASC or DESC", "INVALID_SORT_DIRECTION")` (HTTP 400).

### 3.4 Strict Query Parameter Whitelisting
- In `AnalyticsFactController.java`, the 11 base collection endpoints previously used `validateNoQueryParameters(httpRequest)`.
- These endpoints now call `validatePaginationParametersOnly(httpRequest)`, which strictly allows only `{"page", "size", "sort", "direction"}`.
- Any unexpected query parameter (e.g., `limit=50`, `foo=bar`) is rejected immediately with HTTP 400 (`UNSUPPORTED_PARAMETER`), preventing parameter pollution and ambiguous dispatch.

### 3.5 Tenancy & Role-Based Scope Hardening
- Analytical fact tables represent nationwide longitudinal and operational datasets.
- Following Phase 27.10 security standards, `WorkflowSecurityScopeValidator` was injected into `AnalyticsFactController`.
- All 11 nationwide collection endpoints invoke `scopeValidator.validateNationwideScope()`.
- Unauthenticated callers receive HTTP 401 Unauthorized.
- Scoped personas (State Administrators, Training Providers, Employers, Trainees) and permission-only callers lacking verified national administrative roles are strictly blocked with HTTP 403 Forbidden and zero analytical information disclosure.

---

## 4. Verification & Testing

### 4.1 Test Suite Structure
A dedicated test suite `AnalyticsFactPaginationTest.java` was authored with 43 tests organized across 5 nested test classes:

1. **`DefaultPaginationContractTests` (11 tests)**: Parameterized test verifying all 11 endpoints return HTTP 200 with `ApiResponse<PagedResponse<T>>`, `page = 0`, `size = 20`, `totalElements >= 0`, `totalPages >= 0`, `first = true`, and non-null array content.
2. **`PaginationBoundsTests` (6 tests)**:
   - Valid boundaries: `size = 1`, `size = 100`, `page = 0`.
   - Rejections: `page = -1` (HTTP 400 `INVALID_PAGE`), `size = 0` (HTTP 400 `INVALID_PAGE_SIZE`), `size = 101` (HTTP 400 `INVALID_PAGE_SIZE`), `size = -10` (HTTP 400 `INVALID_PAGE_SIZE`).
3. **`SortingValidationTests` (6 tests)**:
   - Valid: `sort=id` (ASC), `sort=id&direction=DESC`.
   - Rejections: `sort=nonexistentColumn` (HTTP 400 `INVALID_SORT_FIELD`), `sort=password` (HTTP 400 `INVALID_SORT_FIELD`), `direction=SIDEWAYS` (HTTP 400 `INVALID_SORT_DIRECTION`), `limit=50` (HTTP 400 `UNSUPPORTED_PARAMETER`).
4. **`TenancyAndAuthorizationTests` (8 tests)**:
   - Unauthenticated -> HTTP 401 Unauthorized.
   - State Administrator -> HTTP 403 Forbidden.
   - Training Provider -> HTTP 403 Forbidden.
   - Employer -> HTTP 403 Forbidden.
   - Trainee -> HTTP 403 Forbidden.
   - Permission-only -> HTTP 403 Forbidden.
   - National Administrator -> HTTP 200 OK.
   - Super Administrator -> HTTP 200 OK.
5. **`LiveDatabaseRepositoryPaginationTests` (12 tests)**:
   - Database catalog verified as dedicated `SIH26135_TEST`.
   - All 11 repositories (`findAll(PageRequest.of(0, 5, ...))`) tested against live database views, confirming SQL `LIMIT 5 OFFSET 0` execution and bounded heap memory footprint.

### 4.2 Test Execution Results
```
[INFO] Running in.gov.sih.sih26135.controller.AnalyticsFactPaginationTest
[INFO] Tests run: 43, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 29.81 s
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4.3 Full Regression Suite
Combined execution of all pagination, validation, tenancy, security, and multi-dimensional filtering suites:
```
mvn test '-Dtest=CoreDomainResourcePaginationTest,ValidationAndPaginationRegressionTest,InstitutionalAnalyticsIntegrationTest,AnalyticsFilterCriteriaTest,AnalyticsFilterSecurityTest,AnalyticsMultiDimensionalFilteringTest,DashboardTenancyAndRoleAuthorizationTest,AnalyticsFactPaginationTest' -f backend/pom.xml
```
```
[INFO] Results:
[INFO] Tests run: 269, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  01:33 min
[INFO] ------------------------------------------------------------------------
```

---

## 5. Invariant Compliance Audit

| Requirement / Invariant | Target | Actual | Verification Method | Status |
|---|---|---|---|---|
| Dedicated Test DB Catalog | `SIH26135_TEST` only | `SIH26135_TEST` | `MysqlConnectionTest.assertDedicatedTestCatalog` | **COMPLIANT** |
| Schema / DDL Mutations | Zero | Zero | `spring.jpa.hibernate.ddl-auto=validate` passed | **COMPLIANT** |
| Spring Data JpaRepository Count | Exactly 137 | Exactly 137 | Spring Data scanning log: "Found 137 JPA repository interfaces" | **COMPLIANT** |
| Custom `@Query` Annotation Count | Exactly 2 | Exactly 2 | `UserRepository` & `TraineeUnemploymentEventRepository` only | **COMPLIANT** |
| Unbounded `findAll()` in Fact Endpoints | Zero | Zero | Audited `AnalyticsFactController.java` & services | **COMPLIANT** |
| Default Page Size | 20 | 20 | Verified via `AnalyticsFactPaginationTest` | **COMPLIANT** |
| Maximum Page Size Limit | 100 | 100 | `size > 100` rejected with HTTP 400 `INVALID_PAGE_SIZE` | **COMPLIANT** |
| Deterministic Tie-Breaker | Authoritative ID ASC | Authoritative ID ASC | Tested and verified in `PaginationUtils` | **COMPLIANT** |
| Git Operations | Zero commit, zero push | Zero | `git status --short` verified | **COMPLIANT** |

---

## 6. Verdict

**READY FOR INDEPENDENT REVIEW**

Phase 27.12 is completely implemented, rigorously tested against `SIH26135_TEST`, and verified for production readiness. Bounded database-level pagination is active across all 11 analytical fact collection endpoints, safeguarding application memory and preventing unauthorized data disclosure. We are stopping at Phase 27.12 in strict accordance with instructions.
