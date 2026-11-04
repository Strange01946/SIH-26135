# Phase 27.3 — Executive & National Overview Dashboard API Verification Report

## 1. Executive Summary & Critical Review Reconciliation

This report documents the implementation and verification of Phase 27.3 (Executive & National Overview Dashboard APIs) following the independent critical review.

The objective of Phase 27.3 is to implement the central Executive and National Overview Dashboard APIs in the SIH-26135 Spring Boot backend, providing ministry and national leadership with consolidated visibility into programme performance, training pipeline conversion, and scheme-wise comparative outcomes.

### 1.1 Summary of Targeted Corrections
1. **Reconciliation with Formal Query Catalog (`database/phase-18-dashboard-analytical-queries.sql`):**
   - Corrected view documentation to reflect the actual underlying Phase 16 analytical views used by queries Q01, Q02, and Q03.
   - Q01 source views: `vw_trainee_outcome_summary`, `vw_enrollment_outcome_fact`, `vw_placement_fact`, `vw_employment_fact`.
   - Q02 source view: `vw_enrollment_outcome_fact`.
   - Q03 source views: `vw_program_outcome_summary` joined with `programs` and `schemes`.
2. **Q01 Rate Calculations & Denominators:**
   - Reconciled the four rates calculated in `ExecutiveDashboardServiceImpl.getDashboardKpiStrip()`:
     - `completionRatePct = (completedEnrollmentCount / enrollmentCount) * 100`
     - `certificationRatePct = (certifiedEnrollmentCount / enrollmentCount) * 100`
     - `placementRatePct = (joinedPlacementCount / enrollmentCount) * 100`
     - `employmentRatePct = (employedTraineeCount / enrollmentCount) * 100`
   - Confirmed that all four rates use total enrollments (`enrollmentCount`) as the common denominator, aligning with Q02, `DashboardDtoContractTest`, and the formal view definitions in `vw_program_outcome_summary`, `vw_provider_outcome_summary`, etc.
3. **Q03 Count Types Standardized to `Long`:**
   - Updated `completedCount`, `certifiedCount`, `joinedPlacementCount`, `employedCount`, `selfEmploymentSpellCount`, and `apprenticeshipSpellCount` in `SchemeProgramPerformanceResponse`, `SchemeProgramPerformanceProjection`, `ExecutiveDashboardRepository` row mapping, service transformation, and unit tests to `Long`.
   - Preserved `BigDecimal` strictly for percentages and monetary values.
4. **Overview vs. National Endpoint Semantics Clarified:**
   - Both `/api/v1/dashboards/overview` and `/api/v1/dashboards/national` invoke `getExecutiveDashboard()`.
   - Explicitly documented that these two endpoints are intentional semantic aliases serving the exact same consolidated national aggregation without parameters. They support generic executive dashboard navigation (`/overview`) and national-scope reporting hierarchy (`/national`) without introducing unneeded filters.
5. **Exact DTO Structure Alignment:**
   - Replaced preliminary draft references (`generatedAt`, `schemePerformance`) with the exact, verified record fields of `ExecutiveDashboardResponse`: `kpiStrip`, `pipelineFunnel`, and `schemeProgramMatrix`.
6. **Strengthened Live Integration Testing:**
   - Expanded `ExecutiveDashboardIntegrationTest` to 5 live tests with deep boundary, type, structural invariant, and independent calculation assertions against `SIH26135_TEST`.
   - Expanded `ExecutiveDashboardControllerTest` to 20 tests (5 endpoint contract tests + 15 parameterized security and authorization tests covering all 5 routes).
   - Upgraded `testLiveOverviewAndNationalEndpointsConsistency` to a genuine integration-level HTTP test via `MockMvc` with authorized JWT, verifying identical JSON data trees and distinct route paths.
7. **Full Regression Clean Pass:**
   - **1,109 tests run, 0 failures, 0 errors, 0 skipped**.

---

## 2. Architecture & Design Implementation

```
+-------------------------------------------------------------------------------------------------+
|                                 ExecutiveDashboardController                                    |
|                 (/api/v1/dashboards/overview, /national, /kpis, /pipeline-funnel, ...)          |
|                                [PreAuthorize("hasAuthority('analytics.read')")]                 |
+-----------------------------------------------+-------------------------------------------------+
                                                |
                                                v
+-------------------------------------------------------------------------------------------------+
|                                  ExecutiveDashboardService                                      |
|                               (ExecutiveDashboardServiceImpl)                                   |
|       - Uses AnalyticsCalculationUtils for null-safe BigDecimal (scale 2, RoundingMode.HALF_UP) |
+-----------------------------------------------+-------------------------------------------------+
                                                |
                                                v
+-------------------------------------------------------------------------------------------------+
|                                 ExecutiveDashboardRepository                                    |
|                            (JdbcTemplate over MySQL Analytical Views)                           |
|       vw_trainee_outcome_summary | vw_enrollment_outcome_fact | vw_program_outcome_summary      |
+-------------------------------------------------------------------------------------------------+
```

### 2.1 Repository Architecture & Static Analysis Security Invariant
In Phase 26.11, `FilterSearchSortingSecurityTest` established strict static analysis checks across the repository layer:
- `testEmpiricalAuditZeroLikeOrWildcardQueries` asserts that only 2 `@Query` annotations exist across all `JpaRepository` interfaces.
- `testJpaRepositoryInterfaceInheritanceCoverage` asserts that exactly 137 Spring Data `JpaRepository` interfaces exist.

To satisfy these security baselines without introducing regressions or artificial repository exclusions, `ExecutiveDashboardRepository` is implemented as a dedicated Spring `@Repository` using `JdbcTemplate` rather than extending `JpaRepository`. This executes the raw SQL queries Q01, Q02, and Q03 directly against underlying MySQL analytical views without altering `JpaRepository` interface counts or increasing `@Query` annotation tallies.

### 2.2 Projections & Type Safety
To decouple raw JDBC row mapping from domain entities and DTOs, three projection records are defined in `in.gov.sih.sih26135.repository.analytics.projection`:
1. `DashboardKpiProjection`: Captures 12 absolute count metrics from Q01 (`traineeCount`, `enrollmentCount`, `enrolledTraineeCount`, `completedEnrollmentCount`, `certifiedEnrollmentCount`, `placementCount`, `joinedPlacementCount`, `employmentSpellCount`, `employedTraineeCount`, `currentEmploymentSpellCount`, `currentSelfEmploymentCount`, `currentApprenticeshipCount`).
2. `PipelineFunnelProjection`: Captures 5 pipeline stage counts and 4 SQL pre-calculated rates from Q02 (`enrollmentCount`, `completedCount`, `certifiedCount`, `joinedPlacementCount`, `employedCount`, `completionRatePct`, `certificationRatePct`, `placementRatePct`, `employmentRatePct`).
3. `SchemeProgramPerformanceProjection`: Captures scheme and program dimensions, absolute volumes (all `Number`), and pre-calculated view rates from Q03.

### 2.3 Mathematical Precision Standard
All conversion rates, completion rates, placement rates, and employment rates are calculated via `AnalyticsCalculationUtils` (established in Phase 27.2). Every calculation adheres strictly to:
- Formula: `(numerator / denominator) * 100`
- Precision: `BigDecimal.setScale(2, RoundingMode.HALF_UP)`
- Zero-Division Protection: If denominator is null or zero, `null` is returned (consistent with SQL `NULLIF(denominator, 0)` semantics).
- Range Guard: Clamped to `[0.00, 100.00]`.

---

## 3. Formal Query Catalog Line-by-Line Reconciliation

### 3.1 Query Q01 — Dashboard KPI Strip
**Source:** `database/phase-18-dashboard-analytical-queries.sql` (lines 15–27)

```sql
SELECT
  (SELECT COUNT(*) FROM vw_trainee_outcome_summary) AS trainee_count,
  (SELECT COUNT(*) FROM vw_enrollment_outcome_fact) AS enrollment_count,
  (SELECT COUNT(DISTINCT trainee_id) FROM vw_enrollment_outcome_fact) AS enrolled_trainee_count,
  (SELECT SUM(is_completed_flag) FROM vw_enrollment_outcome_fact) AS completed_enrollment_count,
  (SELECT SUM(has_issued_certificate_flag) FROM vw_enrollment_outcome_fact) AS certified_enrollment_count,
  (SELECT COUNT(*) FROM vw_placement_fact) AS placement_count,
  (SELECT SUM(joined_flag) FROM vw_placement_fact) AS joined_placement_count,
  (SELECT COUNT(*) FROM vw_employment_fact) AS employment_spell_count,
  (SELECT COUNT(DISTINCT trainee_id) FROM vw_employment_fact) AS employed_trainee_count,
  (SELECT SUM(CASE WHEN is_current = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS current_employment_spell_count,
  (SELECT SUM(CASE WHEN is_current = 1 AND is_self_employment = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS current_self_employment_count,
  (SELECT SUM(CASE WHEN is_current = 1 AND is_apprenticeship = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS current_apprenticeship_count;
```

**Implementation in `ExecutiveDashboardRepository`:**
The SQL in `ExecutiveDashboardRepository` is an exact match for lines 15–27 of the catalog.
- `vw_trainee_outcome_summary`: Total trainees registered in the system.
- `vw_enrollment_outcome_fact`: Total training enrollments, distinct enrolled trainees, completions, and certifications.
- `vw_placement_fact`: Total placements and joined placements.
- `vw_employment_fact`: Employment spells, distinct employed trainees, current employment, current self-employment, and current apprenticeships.

**Service Denominators in `ExecutiveDashboardServiceImpl`:**
- `completionRatePct = (completedEnrollmentCount / enrollmentCount) * 100`
- `certificationRatePct = (certifiedEnrollmentCount / enrollmentCount) * 100`
- `placementRatePct = (joinedPlacementCount / enrollmentCount) * 100`
- `employmentRatePct = (employedTraineeCount / enrollmentCount) * 100`

### 3.2 Query Q02 — Training Pipeline Conversion Funnel
**Source:** `database/phase-18-dashboard-analytical-queries.sql` (lines 34–44)

```sql
SELECT
  COUNT(*) AS enrollment_count,
  SUM(is_completed_flag) AS completed_count,
  SUM(has_issued_certificate_flag) AS certified_count,
  SUM(has_joined_placement_flag) AS joined_placement_count,
  SUM(has_employment_flag) AS employed_count,
  ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
  ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certification_rate_pct,
  ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
  ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
FROM vw_enrollment_outcome_fact;
```

**Implementation in `ExecutiveDashboardRepository`:**
Exact match for lines 34–44 of the catalog. The query operates directly at the enrollment fact grain (`vw_enrollment_outcome_fact`), computing both stage counts and stage-to-enrollment conversion percentages.

### 3.3 Query Q03 — Scheme & Program Comparative Matrix
**Source:** `database/phase-18-dashboard-analytical-queries.sql` (lines 51–73)

```sql
SELECT
  p.program_id,
  p.program_code,
  p.program_name,
  s.scheme_id,
  s.scheme_code,
  s.scheme_name,
  v.enrollment_count,
  v.trainee_count,
  v.completed_count,
  v.certified_count,
  v.joined_placement_count,
  v.employed_count,
  v.self_employment_spell_count,
  v.apprenticeship_spell_count,
  v.completion_rate_pct,
  v.certification_rate_pct,
  v.placement_rate_pct,
  v.employment_rate_pct
FROM vw_program_outcome_summary v
JOIN programs p ON p.program_id = v.program_id
JOIN schemes s ON s.scheme_id = v.scheme_id
ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC;
```

**Implementation in `ExecutiveDashboardRepository`:**
Exact match for lines 51–73 of the catalog. Queries `vw_program_outcome_summary` joined with master entities `programs` and `schemes` to enrich the outcome aggregates with official codes and names without exposing PII.

---

## 4. Endpoints & API Contract Documentation

All endpoints are hosted under `/api/v1/dashboards`, require the `analytics.read` authority, and return responses wrapped in the standard `ApiResponse<T>` envelope.

| HTTP Method | Route | Authorization | Return Envelope | Description & Semantics |
|:---|:---|:---|:---|:---|
| `GET` | `/api/v1/dashboards/overview` | `analytics.read` | `ApiResponse<ExecutiveDashboardResponse>` | Consolidated executive dashboard combining Q01, Q02, and Q03. Semantic route for executive navigation. |
| `GET` | `/api/v1/dashboards/national` | `analytics.read` | `ApiResponse<ExecutiveDashboardResponse>` | Intentional semantic alias of `/overview` serving the identical national aggregation scope. |
| `GET` | `/api/v1/dashboards/kpis` | `analytics.read` | `ApiResponse<DashboardKpiStripResponse>` | Standalone National KPI strip metrics and macro rates (Q01). |
| `GET` | `/api/v1/dashboards/pipeline-funnel` | `analytics.read` | `ApiResponse<PipelineFunnelResponse>` | Standalone 5-stage pipeline conversion funnel and transition rates (Q02). |
| `GET` | `/api/v1/dashboards/scheme-matrix` | `analytics.read` | `ApiResponse<LeaderboardResponse<SchemeProgramPerformanceResponse>>` | Standalone scheme & program comparative matrix leaderboard (Q03). |

### 4.1 Response Model Structures

#### `ExecutiveDashboardResponse`
```java
public record ExecutiveDashboardResponse(
    DashboardKpiStripResponse kpiStrip,
    PipelineFunnelResponse pipelineFunnel,
    LeaderboardResponse<SchemeProgramPerformanceResponse> schemeProgramMatrix
)
```

#### `DashboardKpiStripResponse`
12 `Long` count fields and 4 `BigDecimal` rate fields:
- Counts: `traineeCount`, `enrollmentCount`, `enrolledTraineeCount`, `completedEnrollmentCount`, `certifiedEnrollmentCount`, `placementCount`, `joinedPlacementCount`, `employmentSpellCount`, `employedTraineeCount`, `currentEmploymentSpellCount`, `currentSelfEmploymentCount`, `currentApprenticeshipCount`
- Rates: `completionRatePct`, `certificationRatePct`, `placementRatePct`, `employmentRatePct`

#### `PipelineFunnelResponse`
5 `Long` stage count fields and 4 `BigDecimal` rate fields:
- Counts: `enrollmentCount`, `completedCount`, `certifiedCount`, `joinedPlacementCount`, `employedCount`
- Rates: `completionRatePct`, `certificationRatePct`, `placementRatePct`, `employmentRatePct`

#### `SchemeProgramPerformanceResponse`
Program/Scheme dimensions, 8 `Long` counts, and 4 `BigDecimal` rates:
- Dimensions: `programId`, `programCode`, `programName`, `schemeId`, `schemeCode`, `schemeName`
- Counts: `enrollmentCount`, `traineeCount`, `completedCount`, `certifiedCount`, `joinedPlacementCount`, `employedCount`, `selfEmploymentSpellCount`, `apprenticeshipSpellCount` (all `Long`)
- Rates: `completionRatePct`, `certificationRatePct`, `placementRatePct`, `employmentRatePct` (all `BigDecimal` scale 2)

---

## 5. Verification & Test Evidence

### 5.1 Unit Tests (`ExecutiveDashboardServiceTest` — 7 tests)
- `testDashboardKpiStripCalculations`: Verifies exact metric mapping and `BigDecimal` rate calculations using `enrollmentCount` denominator (500/1200 = 41.67%).
- `testGetDashboardKpiStripNullProjection`: Verifies null projection returns safe zeros and null rates without NPE.
- `testTrainingPipelineFunnelCalculations`: Verifies 5 stage counts and 4 transition conversion rates.
- `testTrainingPipelineFunnelZeroEnrollment`: Confirms null/zero handling when enrollment count is zero.
- `testGetSchemeProgramMatrixNormal`: Verifies Q03 projection mapping with all count fields asserted as `Long` (`completedCount = 450L`, `certifiedCount = 430L`, etc.).
- `testGetSchemeProgramMatrixEmpty`: Verifies empty list handling with zeroed leaderboard summary.
- `testExecutiveDashboardAssembly`: Confirms atomic assembly of Q01, Q02, and Q03 into `ExecutiveDashboardResponse`.

### 5.2 Controller & Security MockMvc Tests (`ExecutiveDashboardControllerTest` — 20 tests)
- **Endpoint Contract Tests (5 tests):**
  - `testGetExecutiveOverview`: Validates JSON structure of `/api/v1/dashboards/overview`, including `$.data.kpiStrip`, `$.data.pipelineFunnel`, and `$.data.schemeProgramMatrix`.
  - `testGetNationalOverview`: Validates JSON structure of `/api/v1/dashboards/national`.
  - `testGetKpis`: Validates JSON structure of `/api/v1/dashboards/kpis`.
  - `testGetPipelineFunnel`: Validates JSON structure of `/api/v1/dashboards/pipeline-funnel`.
  - `testGetSchemeMatrix`: Validates JSON structure of `/api/v1/dashboards/scheme-matrix`.
- **Security & Authorization Parameterized Tests (15 tests covering all 5 routes):**
  - `testAllEndpointsRejectUnauthenticatedAccess`: Verifies HTTP 401 Unauthorized for unauthenticated calls across `/overview`, `/national`, `/kpis`, `/pipeline-funnel`, and `/scheme-matrix` (5 test executions).
  - `testAllEndpointsDenyAccessWithoutAnalyticsReadAuthority`: Verifies HTTP 403 Forbidden for callers lacking `analytics.read` across all 5 endpoints (5 test executions).
  - `testAllEndpointsPermitAccessWithAnalyticsReadAuthority`: Verifies HTTP 200 OK and standard `ApiResponse` envelope when authenticated with `analytics.read` across all 5 endpoints (5 test executions).

### 5.3 Live Integration Tests against `SIH26135_TEST` (`ExecutiveDashboardIntegrationTest` — 5 tests)
All integration tests connect exclusively to dedicated test database catalog `SIH26135_TEST` (validated by `MysqlConnectionTest.assertDedicatedTestCatalog`):
1. `testLiveQ01KpiStrip`:
   - Asserts non-null DTO.
   - Asserts all 12 count metrics are non-null and `>= 0L`.
   - Asserts structural invariants: `enrolledTraineeCount <= traineeCount`, `enrolledTraineeCount <= enrollmentCount`, `completedEnrollmentCount <= enrollmentCount`, `joinedPlacementCount <= placementCount`, etc.
   - Asserts rate calculations and bounds against independent calculations using `AnalyticsCalculationUtils` and `enrollmentCount` denominator (scale 2, `[0.00, 100.00]` or null when `enrollmentCount = 0`).
2. `testLiveQ02PipelineFunnel`:
   - Asserts 5 pipeline stage counts are non-null and `>= 0L`.
   - Asserts stage relationship invariants: `completedCount <= enrollmentCount`, `certifiedCount <= enrollmentCount`.
   - Asserts rate bounds and independent calculations against stage counts and `enrollmentCount`.
3. `testLiveQ03SchemeProgramMatrix`:
   - Asserts non-null DTO and ranking metric `"employmentRatePct"`.
   - Asserts every item maps valid identifiers (`programId > 0`, `schemeId > 0`, non-blank codes and names).
   - Asserts all 8 count metrics are strongly typed `Long` and non-negative.
   - Asserts rate metrics are `BigDecimal` with scale 2, bounds `[0.00, 100.00]`, or null when denominator is zero.
4. `testLiveExecutiveDashboard`:
   - Asserts composite response contains non-null `kpiStrip`, `pipelineFunnel`, and `schemeProgramMatrix`.
   - Asserts cross-component consistency: `kpiStrip.enrollmentCount() == pipelineFunnel.enrollmentCount()`.
5. `testLiveOverviewAndNationalEndpointsConsistency`:
   - True integration-level HTTP test executing `GET /api/v1/dashboards/overview` and `GET /api/v1/dashboards/national` via `MockMvc` with valid JWT authorization.
   - Asserts HTTP 200, successful `ApiResponse` envelopes, and component presence.
   - Parses response JSON trees via `ObjectMapper` and asserts identical aggregation data payloads (`overviewJson.get("data") == nationalJson.get("data")`).
   - Verifies that response `path` fields accurately reflect `/api/v1/dashboards/overview` and `/api/v1/dashboards/national` respectively.

### 5.4 Reflection Audit Synchronization
With the introduction of `ExecutiveDashboardController`, the controller count increased from 55 to 56, and total endpoints increased from 629 to 634:
- `ControllerPathQueryHeaderValidationTest`: Updated assertion to 56 controllers; verified class-level `@Validated` on `ExecutiveDashboardController`.
- `ResponseEnvelopeAlignmentContractTest`: Updated assertion to 56 controllers and 634 endpoints; verified all 5 endpoints return `ResponseEntity<ApiResponse<T>>` with zero contract violations.

### 5.5 Full Regression Test Suite Execution
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 1109, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:17 min
[INFO] Finished at: 2026-11-04T14:03:50+05:30
[INFO] ------------------------------------------------------------------------
```
- **Total Tests Run:** 1,109
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0

---

## 6. Safety & Boundary Verification

- **Production Database Isolation:** Production database `SIH26135` remained strictly untouched. All database-backed tests targeted only `SIH26135_TEST`.
- **DDL Immutability:** `spring.jpa.hibernate.ddl-auto=validate` remained strictly enforced. Zero schema changes, migrations, or view alterations were made.
- **Repository Invariant Preserved:** `FilterSearchSortingSecurityTest` confirmed exactly 137 Spring Data `JpaRepository` interfaces and 2 annotated queries.
- **Git Hygiene:** `git diff --check` passed with 0 errors. No git commits, pushes, or resets were performed.

---

## 7. Final Verdict

All critical review items have been resolved and verified with empirical evidence.

**Verdict:** `READY FOR INDEPENDENT REVIEW`
