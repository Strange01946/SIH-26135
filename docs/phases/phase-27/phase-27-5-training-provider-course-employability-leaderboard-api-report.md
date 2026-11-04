# Phase 27.5 — Training Provider & Course Employability Leaderboard API Report

## 1. Phase Objective & Final Verdict

### 1.1 Objective
Implement institutional and curriculum performance scorecards for the SIH-26135 Spring Boot backend. This phase extends the analytics dashboard architecture established in Phases 27.1–27.4 by implementing:
1. **Training Provider Performance Leaderboard** (`GET /api/v1/dashboards/providers`) powered by formal analytical Query **Q04**.
2. **Course Outcome & Employability Leaderboard** (`GET /api/v1/dashboards/courses`) powered by formal analytical Query **Q05**.

Both endpoints enforce `@PreAuthorize("hasAuthority('analytics.read')")`, return standard `ApiResponse<LeaderboardResponse<T>>` envelopes, maintain precision and null-safety via `AnalyticsCalculationUtils`, and adhere strictly to repository and database safety invariants.

### 1.2 Final Verdict
- **Verdict:** `READY FOR INDEPENDENT REVIEW`
- **Justification:**
  - All acceptance criteria are completely satisfied.
  - Formal queries Q04 and Q05 are accurately reconciled with source analytical views `vw_provider_outcome_summary`, `vw_course_outcome_summary`, and supporting catalog tables.
  - `InstitutionalAnalyticsController` provides both endpoints with full authorization guards and standard response envelopes.
  - Zero schema changes, zero DDL migrations (`ddl-auto=validate` preserved).
  - Dedicated test catalog `SIH26135_TEST` verified by `MysqlConnectionTest.assertDedicatedTestCatalog`. Zero access to production catalog `SIH26135`.
  - Exactly 137 `JpaRepository` interfaces and 2 `@Query` declarations preserved (zero Spring Data JPA invariant violations).
  - Production REST controller count expanded cleanly from 57 to 58; endpoint count from 637 to 639.
  - 100% of all 58 controllers and 639 endpoints verified by `ResponseEnvelopeAlignmentContractTest` and `ControllerPathQueryHeaderValidationTest`.
  - Focused test suite (28 tests) and entire backend regression test suite (1,163 tests) passed cleanly with 0 failures, 0 errors.
  - `git diff --check` passed with 0 whitespace errors. Zero git stage/commit/push/reset/clean actions performed.

---

## 2. Initial Git Branch & Working-Tree State

- **Repository Root:** `E:\project`
- **Backend Directory:** `E:\project\backend`
- **Initial Branch:** `main`
- **Initial Commit Hash:** `cd7d412` ("Complete Phase 27.4 geographic dashboard analytics API")
- **Initial Working-Tree State:** Completely clean (`git status -s` yielded no output).

---

## 3. Pre-Existing Modified / Untracked Files

- No pre-existing untracked or modified files were present when Phase 27.5 commenced.
- Working tree was completely synchronized with `origin/main` at commit `cd7d412`.

---

## 4. Reconnaissance Findings

1. **Analytical SQL Catalog:**
   - Formal queries Q04 and Q05 are defined in `database/phase-18-dashboard-analytical-queries.sql` (lines 79–120).
   - Q04 targets `vw_provider_outcome_summary` joined with `training_providers`.
   - Q05 targets `vw_course_outcome_summary` joined with `courses`.
2. **Underlying Analytical Views:**
   - `vw_provider_outcome_summary` aggregates `vw_enrollment_outcome_fact` grouped by `provider_id`.
   - `vw_course_outcome_summary` aggregates `vw_enrollment_outcome_fact` grouped by `course_id`.
   - In both views, the four percentage metrics (`completion_rate_pct`, `certification_rate_pct`, `placement_rate_pct`, `employment_rate_pct`) share a common denominator: `COUNT(*) AS enrollment_count`.
3. **Curriculum & Institutional Metadata:**
   - `training_batches` contains `capacity`, `provider_id`, and `batch_status_id` referencing `ref_batch_status`.
   - Active batches are identified by `ref_batch_status.status_code IN ('OPEN', 'ONGOING')` with `deleted_at IS NULL`.
   - `courses` contains `qualification_level_id` referencing `ref_qualification_level` (`level_code`, `level_name`, `nsqf_level`).
   - Starting wage progression is stored in `vw_salary_progression_fact` (`sp.first_recorded_salary`).
4. **Architectural Baseline:**
   - 57 production REST controllers, 637 endpoints before Phase 27.5.
   - 137 `JpaRepository` interfaces, 2 `@Query` declarations.
   - Reusable generic container `LeaderboardResponse<T>` available from Phase 27.2.
   - `AnalyticsCalculationUtils` provides null-safe arithmetic and percentage calculations.

---

## 5. Formal Q04 SQL Definition & Source-View Reconciliation

### 5.1 Formal SQL Definition (Catalog Q04)
```sql
SELECT
  tp.provider_id,
  tp.provider_code,
  tp.provider_name,
  v.enrollment_count,
  v.trainee_count,
  v.completed_count,
  v.certified_count,
  v.joined_placement_count,
  v.employed_count,
  v.completion_rate_pct,
  v.certification_rate_pct,
  v.placement_rate_pct,
  v.employment_rate_pct
FROM vw_provider_outcome_summary v
JOIN training_providers tp ON tp.provider_id = v.provider_id
ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC;
```

### 5.2 Underlying View Reconciliation (`vw_provider_outcome_summary`)
```sql
CREATE OR REPLACE VIEW vw_provider_outcome_summary AS
SELECT
  provider_id,
  COUNT(*) AS enrollment_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_completed_flag) AS completed_count,
  SUM(has_issued_certificate_flag) AS certified_count,
  SUM(has_joined_placement_flag) AS joined_placement_count,
  SUM(has_employment_flag) AS employed_count,
  ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
  ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certification_rate_pct,
  ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
  ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
FROM vw_enrollment_outcome_fact
GROUP BY provider_id;
```

### 5.3 Active Batch Capacity Integration
Formal Q04 ranks providers on training and employment outcomes. To provide full institutional scorecard visibility without violating view immutability, active batch capacity is joined from `training_batches`:
```sql
LEFT JOIN (
  SELECT
    tb.provider_id,
    SUM(tb.capacity) AS active_batch_capacity
  FROM training_batches tb
  JOIN ref_batch_status rbs ON rbs.batch_status_id = tb.batch_status_id
  WHERE rbs.status_code IN ('OPEN', 'ONGOING')
    AND tb.deleted_at IS NULL
  GROUP BY tb.provider_id
) act_cap ON act_cap.provider_id = tp.provider_id
```
If a provider has no active batches, `active_batch_capacity` is safely represented as `0L` (via `COALESCE`).

---

## 6. Formal Q05 SQL Definition & Source-View Reconciliation

### 6.1 Formal SQL Definition (Catalog Q05)
```sql
SELECT
  c.course_id,
  c.course_code,
  c.course_name,
  v.enrollment_count,
  v.trainee_count,
  v.completed_count,
  v.certified_count,
  v.joined_placement_count,
  v.employed_count,
  v.completion_rate_pct,
  v.certification_rate_pct,
  v.placement_rate_pct,
  v.employment_rate_pct
FROM vw_course_outcome_summary v
JOIN courses c ON c.course_id = v.course_id
ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC;
```

### 6.2 Underlying View Reconciliation (`vw_course_outcome_summary`)
```sql
CREATE OR REPLACE VIEW vw_course_outcome_summary AS
SELECT
  course_id,
  COUNT(*) AS enrollment_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_completed_flag) AS completed_count,
  SUM(has_issued_certificate_flag) AS certified_count,
  SUM(has_joined_placement_flag) AS joined_placement_count,
  SUM(has_employment_flag) AS employed_count,
  ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
  ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certification_rate_pct,
  ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
  ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
FROM vw_enrollment_outcome_fact
GROUP BY course_id;
```

### 6.3 Curriculum Enhancements (Qualification Levels & Wage Data)
1. **Qualification Level Metadata:** Joined from `ref_qualification_level ql ON ql.qualification_level_id = c.qualification_level_id`, capturing `level_code`, `level_name`, and `nsqf_level`.
2. **Average Starting Wage:** Reconciled with formal Query Q11 (`vw_salary_progression_fact`), joined via subquery:
   ```sql
   LEFT JOIN (
     SELECT
       sp.course_id,
       ROUND(AVG(sp.first_recorded_salary), 2) AS avg_starting_wage
     FROM vw_salary_progression_fact sp
     WHERE sp.course_id IS NOT NULL AND sp.first_recorded_salary IS NOT NULL
     GROUP BY sp.course_id
   ) wage ON wage.course_id = c.course_id
   ```
   If a course has no salary history records, `avg_starting_wage` returns `null` (never fabricated as zero).

---

## 7. Exact Metric Definitions, Denominators & Aggregation Grain

| Entity | Metric Name | Metric Semantics | Numerator | Denominator | Type / Scale |
|---|---|---|---|---|---|
| Provider | `enrollmentCount` | Total enrollment events | `COUNT(*)` in fact | None | `Long` |
| Provider | `traineeCount` | Distinct trainees trained | `COUNT(DISTINCT trainee_id)` | None | `Long` |
| Provider | `completedCount` | Completed training spells | `SUM(is_completed_flag)` | None | `Long` |
| Provider | `certifiedCount` | Certified enrollments | `SUM(has_issued_certificate_flag)` | None | `Long` |
| Provider | `joinedPlacementCount` | Trainees joining placements | `SUM(has_joined_placement_flag)` | None | `Long` |
| Provider | `employedCount` | Trainees obtaining employment | `SUM(has_employment_flag)` | None | `Long` |
| Provider | `completionRatePct` | Completion percentage | `completed_count` | `enrollment_count` | `BigDecimal` (scale 2) |
| Provider | `certificationRatePct` | Certification percentage | `certified_count` | `enrollment_count` | `BigDecimal` (scale 2) |
| Provider | `placementRatePct` | Placement joining percentage | `joined_placement_count` | `enrollment_count` | `BigDecimal` (scale 2) |
| Provider | `employmentRatePct` | Employment conversion rate | `employed_count` | `enrollment_count` | `BigDecimal` (scale 2) |
| Provider | `activeBatchCapacity` | Current active batch capacity | `SUM(tb.capacity)` (OPEN/ONGOING) | None | `Long` |
| Course | `enrollmentCount` | Course enrollment volume | `COUNT(*)` in fact | None | `Long` |
| Course | `traineeCount` | Distinct trainees in course | `COUNT(DISTINCT trainee_id)` | None | `Long` |
| Course | `employmentRatePct` | Employment conversion rate | `employed_count` | `enrollment_count` | `BigDecimal` (scale 2) |
| Course | `dropoutRatePct` | Non-completion dropout rate | `enrollment_count - completed_count` | `enrollment_count` | `BigDecimal` (scale 2) |
| Course | `avgStartingWage` | Mean starting salary | `SUM(first_recorded_salary)` | `COUNT(first_recorded_salary)` | `BigDecimal` (scale 2) |

---

## 8. Files Created and Modified with Reasons

### 8.1 Files Created
1. `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/projection/ProviderPerformanceProjection.java`
   - Analytical query projection record for formal Query Q04 results with active batch capacity.
2. `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/projection/CourseEmployabilityProjection.java`
   - Analytical query projection record for formal Query Q05 results with NSQF qualification level and wage data.
3. `backend/src/main/java/in/gov/sih/sih26135/dto/response/ProviderPerformanceResponse.java`
   - Immutable public response model representing a ranked provider scorecard.
4. `backend/src/main/java/in/gov/sih/sih26135/dto/response/CourseEmployabilityResponse.java`
   - Immutable public response model representing a ranked course outcome and employability scorecard.
5. `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/InstitutionalAnalyticsRepository.java`
   - High-performance Spring `@Repository` executing formal Q04 and Q05 queries via `JdbcTemplate` without JPA repository contract violations.
6. `backend/src/main/java/in/gov/sih/sih26135/service/InstitutionalAnalyticsService.java`
   - Service interface defining provider leaderboard, course leaderboard, and qualification grouping contracts.
7. `backend/src/main/java/in/gov/sih/sih26135/service/impl/InstitutionalAnalyticsServiceImpl.java`
   - Production service implementation handling ranking assignment, dropout rate calculations, and safe type mapping.
8. `backend/src/main/java/in/gov/sih/sih26135/controller/InstitutionalAnalyticsController.java`
   - REST controller exposing `/api/v1/dashboards/providers` and `/api/v1/dashboards/courses` under `@PreAuthorize("hasAuthority('analytics.read')")`.
9. `backend/src/test/java/in/gov/sih/sih26135/service/InstitutionalAnalyticsServiceTest.java`
   - Unit tests covering projection mapping, sequential ranking, tie-breaking, dropout rate calculation, wage null-safety, and qualification grouping.
10. `backend/src/test/java/in/gov/sih/sih26135/controller/InstitutionalAnalyticsControllerTest.java`
    - MockMvc controller tests verifying HTTP 200 envelopes, 401 unauthenticated, and 403 unauthorized responses.
11. `backend/src/test/java/in/gov/sih/sih26135/service/InstitutionalAnalyticsIntegrationTest.java`
    - Live database integration tests verifying real Q04 and Q05 SQL execution, metric bounds, and REST endpoint integration against `SIH26135_TEST`.

### 8.2 Files Modified
1. `backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java`
   - Added `InstitutionalAnalyticsController.class` to `TRUSTED_CONTROLLER_CLASSES` allowlist.
2. `backend/src/test/java/in/gov/sih/sih26135/dto/response/DashboardDtoContractTest.java`
   - Added serialization, getters, and immutability tests for `ProviderPerformanceResponse` and `CourseEmployabilityResponse`.
3. `backend/src/test/java/in/gov/sih/sih26135/response/ResponseEnvelopeAlignmentContractTest.java`
   - Updated controller count assertion from 57 to 58, and endpoint count assertion from 637 to 639.
4. `backend/src/test/java/in/gov/sih/sih26135/validation/ControllerPathQueryHeaderValidationTest.java`
   - Updated controller count assertion from 57 to 58.

---

## 9. Controller Routes & Authorization Behavior

| HTTP Method | Route | Authorization | Response Envelope | Success Code |
|---|---|---|---|---|
| `GET` | `/api/v1/dashboards/providers` | `hasAuthority('analytics.read')` | `ApiResponse<LeaderboardResponse<ProviderPerformanceResponse>>` | 200 OK |
| `GET` | `/api/v1/dashboards/courses` | `hasAuthority('analytics.read')` | `ApiResponse<LeaderboardResponse<CourseEmployabilityResponse>>` | 200 OK |

### Authorization & Error Enforcement
- **Unauthenticated Requests:** Rejected immediately by Spring Security filter chain with HTTP 401 Unauthorized (`RestAuthenticationEntryPoint`).
- **Forbidden Requests:** Authenticated users lacking `analytics.read` (e.g. operators with `trainee.read`) receive HTTP 403 Forbidden (`RestAccessDeniedHandler`).
- **Authorized Requests:** Users with `analytics.read` receive HTTP 200 with standard `ApiResponse` envelope including `success: true`, descriptive message, requested URI `path`, and payload `data`.

---

## 10. Repository Architecture & SQL Implementation

`InstitutionalAnalyticsRepository` uses parameterized queries and explicit `RowMapper` mapping through Spring's `JdbcTemplate`.

### Key SQL Implementation Highlights:
1. **Soft-Delete Exclusions:** Providers with `tp.deleted_at IS NOT NULL` and courses with `c.deleted_at IS NOT NULL` are strictly excluded.
2. **Deterministic Tie-Breaking:** In addition to formal `v.employment_rate_pct DESC, v.enrollment_count DESC`, secondary ordering by `code ASC, id ASC` is added to prevent non-deterministic leaderboard jitter.
3. **Capacity Coalescing:** Providers without active batches return `0L` active capacity rather than SQL null.
4. **Wage Isolation:** Average starting wage aggregates `first_recorded_salary` from `vw_salary_progression_fact` only for rows with non-null salaries, preserving null when no salary records exist.

---

## 11. DTO & Projection Design

- **Counts:** Strictly typed as `Long` across all responses (`enrollmentCount`, `traineeCount`, `completedCount`, `certifiedCount`, `joinedPlacementCount`, `employedCount`, `activeBatchCapacity`).
- **Rates & Wages:** Strictly typed as `BigDecimal` with scale 2 and `RoundingMode.HALF_UP`.
- **Ranks:** 1-based `Integer` sequential rank assigned post-query in the service layer.
- **Envelope Reuse:** Reuses generic `LeaderboardResponse<T>` established in Phase 27.2:
  - `rankingMetric`: `"employment_rate_pct DESC, enrollment_count DESC"`
  - `totalItems`: exact count of items in collection
  - `items`: immutable list of item DTOs

---

## 12. Service-Layer Calculations & Mapping

In `InstitutionalAnalyticsServiceImpl`:
1. **Sequential Rank Assignment:** Traverses the repository projection list in order and assigns `i + 1` as the `rank`.
2. **Derived Dropout Rate:**
   $$\text{dropoutCount} = \max(0, \text{enrollmentCount} - \text{completedCount})$$
   $$\text{dropoutRatePct} = \text{AnalyticsCalculationUtils.calculatePercentage}(\text{dropoutCount}, \text{enrollmentCount})$$
   If `completedCount` is null but `completionRatePct` exists, falls back to:
   $$\text{dropoutRatePct} = 100.00 - \text{completionRatePct}$$
   If `enrollmentCount` is zero or null, safely returns `null`.
3. **No Redundant Re-Aggregation:** Pre-computed view percentages (`completionRatePct`, `certificationRatePct`, `placementRatePct`, `employmentRatePct`) are mapped directly from the projection without erroneous averaging.

---

## 13. Ranking & Tie-Breaking Semantics

- **Primary Sort Key:** Employment conversion rate (`employment_rate_pct DESC`).
- **Null Handling:** Null employment rates sort last (`employment_rate_pct IS NULL` ascending).
- **Secondary Sort Key:** Total enrollment volume (`enrollment_count DESC`).
- **Deterministic Tertiary Sort Key:** Entity code ascending (`provider_code ASC` / `course_code ASC`), then primary key ascending.
- **Rank Property:** In the serialized JSON payload, `rank` represents the 1-based sequential position on the leaderboard.

---

## 14. Null, Zero-Denominator & Empty-Result Behavior

1. **Zero Enrollments:** Denominators with 0 safely return `null` rate percentages via SQL `NULLIF(COUNT(*), 0)` and `AnalyticsCalculationUtils.calculatePercentage(..., 0L)`.
2. **Missing Wage Records:** If a course has no salary history records in `vw_salary_progression_fact`, `avgStartingWage` returns `null` (not `0.00`).
3. **Empty Collections:** If no providers or courses match the query, the API returns HTTP 200 with `totalItems: 0` and an empty list `items: []` (never null, never 500).

---

## 15. Qualification-Level Grouping Behavior

- `InstitutionalAnalyticsService.getCourseLeaderboardByQualificationLevel()` groups the ranked course items by `qualificationLevelName` into a `LinkedHashMap<String, List<CourseEmployabilityResponse>>`.
- Unassigned courses map safely to `"Unspecified"`.
- Within each group, original leaderboard ranks and relative order are fully preserved.

---

## 16. Tests Executed, Exact Commands & Results

### 16.1 MySQL Catalog Safeguard Verification
```bash
mvn test "-Dtest=MysqlConnectionTest" -f backend/pom.xml
```
- **Result:** Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS, 30.7s).
- **Target:** Verified dedicated test catalog `SIH26135_TEST`.

### 16.2 Focused Institutional Analytics Tests
```bash
mvn test "-Dtest=InstitutionalAnalyticsServiceTest,InstitutionalAnalyticsControllerTest,InstitutionalAnalyticsIntegrationTest,DashboardDtoContractTest" -f backend/pom.xml
```
- **Result:** Tests run: 28, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS, 56.8s).
- **Breakdown:**
  - `InstitutionalAnalyticsControllerTest`: 4 tests (including parameterized suites), 0 failures.
  - `InstitutionalAnalyticsServiceTest`: 8 tests, 0 failures.
  - `InstitutionalAnalyticsIntegrationTest`: 6 live database tests against `SIH26135_TEST`, 0 failures.
  - `DashboardDtoContractTest`: 10 tests, 0 failures.

### 16.3 Reflection, Contract & Existing Dashboard Regression Tests
```bash
mvn test "-Dtest=ResponseEnvelopeAlignmentContractTest,ControllerPathQueryHeaderValidationTest,FilterSearchSortingSecurityTest,ExecutiveDashboardControllerTest,ExecutiveDashboardServiceTest,ExecutiveDashboardIntegrationTest,GeographicDashboardControllerTest,GeographicDashboardServiceTest,GeographicDashboardIntegrationTest" -f backend/pom.xml
```
- **Result:** Tests run: 150, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS, 58.7s).
- **Coverage:** Confirmed exactly 58 controllers and 639 endpoints adhere to envelope contract.

### 16.4 Full Backend Regression Suite
```bash
mvn test -f backend/pom.xml
```
- **Result:** Tests run: 1163, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS, 02:18 min).
- **Zero Regressions:** All 1,163 automated tests passed cleanly.

---

## 17. Repository & Security Invariant Verification

- **Spring Data JpaRepository Count:** Exactly 137 interfaces (verified via Spring context scanning during test bootstrap).
- **Annotated @Query Count:** Exactly 2 declarations (verified unchanged).
- **New Repositories:** Implemented as a Spring `@Repository` using `JdbcTemplate`, preserving the JPA repository count invariant.
- **Security Check:** `@PreAuthorize("hasAuthority('analytics.read')")` enforced at class level on `InstitutionalAnalyticsController`.

---

## 18. Database Safety Confirmation & Target

- **Verified Target Database:** `SIH26135_TEST`.
- **Production Database Status:** Production database `SIH26135` was never accessed, queried, or modified.
- **DDL & Schema Status:** `spring.jpa.hibernate.ddl-auto=validate` remained strictly in effect throughout all test executions. Zero schema mutations, zero migrations, zero DDL statements executed.

---

## 19. Confirmation of ddl-auto=validate Integrity

Hibernate validation confirmed across all test contexts:
`HHH000204: Processing PersistenceUnitInfo [name: default]`, `ddl-auto=validate` confirmed intact without schema alterations.

---

## 20. git diff --check Result

```bash
git diff --check
```
- **Result:** Exited with code 0. Zero trailing whitespace or formatting defects.

---

## 21. Remaining Limitations & Deferred Work

1. **Active Batch Definition:** Active batch capacity currently reflects batches in `OPEN` or `ONGOING` status with `deleted_at IS NULL`. Historical or cancelled batches are excluded.
2. **Wage Observation Scope:** Average starting wage reflects `first_recorded_salary` from `vw_salary_progression_fact`. Trainees without reported salary histories in the test catalog return `null`.
3. **Future Dashboard Phases:** Employer hiring scorecards (Q07), demographic breakdowns (Q08–Q10), and retention analytics (Q12) remain planned for subsequent phases (Phases 27.6–27.17).

---

## 22. Explicit Confirmation of Git Safety

- **Git Status:** No git stage (`git add`), commit (`git commit`), push (`git push`), stash (`git stash`), reset (`git reset`), or clean (`git clean`) operation was performed.
- All modifications and new files remain in the working tree for user review.

---

## 23. Acceptance Criteria Checklist

- [x] Formal Q04 and Q05 inspected and reconciled with source views.
- [x] `InstitutionalAnalyticsController` implemented under `/api/v1/dashboards`.
- [x] `/api/v1/dashboards/providers` works correctly and verified.
- [x] `/api/v1/dashboards/courses` works correctly and verified.
- [x] Provider leaderboard follows formal Q04 semantics.
- [x] Course employability leaderboard follows formal Q05 semantics.
- [x] Placement rate and certification rate calculated and mapped correctly.
- [x] Active batch capacity and trainee volume represented correctly.
- [x] Employment conversion and dropout rates represented correctly.
- [x] Average starting wage preserves monetary precision and source semantics.
- [x] Qualification-level grouping preserved and supported in service layer.
- [x] Counts use `Long` where appropriate.
- [x] Percentage values use `BigDecimal`, scale 2, `RoundingMode.HALF_UP`.
- [x] Missing metrics and zero denominators handled safely.
- [x] Ranking and tie-breaking follow formal query semantics.
- [x] Both endpoints enforce `analytics.read`.
- [x] Both endpoints return standard `ApiResponse<T>` envelopes.
- [x] Existing repository invariants remain unchanged (137 JpaRepositories, 2 `@Query`).
- [x] Existing Executive and Geographic Dashboard APIs remain unaffected.
- [x] Production database `SIH26135` was never accessed.
- [x] All database-backed tests targeted only `SIH26135_TEST`.
- [x] `spring.jpa.hibernate.ddl-auto=validate` remains intact.
- [x] No schema changes, migrations, or DDL introduced.
- [x] No unrelated modifications made.
- [x] No Git stage/commit/push/reset/stash/clean action performed.
- [x] Focused tests (28) and full regression tests (1,163) passed cleanly.
- [x] `git diff --check` passes with zero whitespace defects.
- [x] Detailed Phase 27.5 report created.
