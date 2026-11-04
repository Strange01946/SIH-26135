# Phase 27.4 — Geographic, State & District Heatmap Analytics API Verification Report

## 1. Executive Summary & Objective

This report documents the implementation and verification of Phase 27.4 (Geographic, State & District Heatmap Analytics API) for the SIH-26135 Spring Boot backend.

The primary objective of Phase 27.4 is to provide executive leadership and administrative users with consolidated geographic visibility into training outcomes, district-level performance, and regional disparities. This includes implementing `GeographicDashboardController` with endpoints for national geography overview, state-specific analytics, and district-specific analytics, backed by formal analytical Query Q06 (`vw_district_outcome_summary` joined with `districts` and `states`).

### Final Verdict: `READY FOR INDEPENDENT REVIEW`

All acceptance criteria for Phase 27.4 have been implemented and empirically verified:
- Dedicated REST Controller `GeographicDashboardController` implemented under `/api/v1/dashboards`.
- Implemented 3 analytical routes: `GET /api/v1/dashboards/geography`, `GET /api/v1/dashboards/states/{stateId}`, and `GET /api/v1/dashboards/districts/{districtId}`.
- All endpoints enforce `analytics.read` authority and return standard `ApiResponse<T>` envelopes.
- Implemented formal Query Q06 District Outcome Rankings and outcome matrix.
- Preserved strict mathematical reconciliation between district-level outcomes, state rollups, and national totals.
- All count metrics standardized to `Long`; rates standardized to `BigDecimal` (scale 2, `RoundingMode.HALF_UP`).
- Rigorous source data reconnaissance confirmed the absence of rural/urban classifications, aspirational district indicators, and population/geographic area denominators; these limitations are documented explicitly in DTO response models without fabricating demo or synthetic data.
- Dedicated Spring `@Repository` `GeographicDashboardRepository` using `JdbcTemplate` preserves all 137 Spring Data `JpaRepository` interfaces and 2 `@Query` security invariants.
- Deterministic test fixtures in `GeographicDashboardIntegrationTest` guarantee that state and district HTTP 200 endpoint paths execute on every run without modifying schema or production data.
- Zero unconditional deletes in `@BeforeEach`; pre-insertion ID collision detection fails closed without modifying occupied records, and `@AfterEach` selectively deletes only test-inserted fixtures.
- Clear separation between Q06 district-level exact integration assertions, national rate calculation unit tests (`GeographicDashboardServiceTest`), and zero-denominator null safety verification.
- 1,141 total backend tests run, 0 failures, 0 errors, 0 skipped.
- Production database `SIH26135` remained completely untouched (`SIH26135_TEST` only, with `ddl-auto=validate`).
- Zero Git commits, pushes, resets, stashes, or cleans performed.

---

## 2. Initial Git State & Working-Tree Reconnaissance

- **Git Branch:** `main`
- **Initial Working Tree:** Clean (commit corresponding to Phase 27.3 was pushed previously by user).
- **Pre-existing Modified/Untracked Files:** None prior to starting Phase 27.4.

---

## 3. Reconnaissance & Source Data Availability Findings

A comprehensive audit was performed across `database/*.sql` schema files and Java entities before designing the analytical API:

### 3.1 Formal Query Q06 Source & Schema Audit
**Source:** `database/phase-18-dashboard-analytical-queries.sql` (lines 125–147)
```sql
-- Q06 District outcome map/table
SELECT
  d.district_id,
  d.district_code,
  d.district_name,
  st.state_id,
  st.state_code,
  st.state_name,
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
FROM vw_district_outcome_summary v
JOIN districts d ON d.district_id = v.district_id
JOIN states st ON st.state_id = d.state_id
ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct ASC, v.enrollment_count DESC;
```
- **Underlying View:** `vw_district_outcome_summary` (`database/phase-16-analytical-views.sql`, lines 678–692) aggregates `vw_enrollment_outcome_fact` by `trainee_district_id AS district_id`.
- **Grain:** Distinct district level (`trainee_district_id`).
- **Denominators:** All four percentages (`completion_rate_pct`, `certification_rate_pct`, `placement_rate_pct`, `employment_rate_pct`) divide their respective numerator by `NULLIF(COUNT(*), 0)` (i.e. `enrollment_count`), multiplied by 100, rounded to 2 decimal places.
- **Ordering:** `ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct ASC, v.enrollment_count DESC`. This ascending order on employment rate is explicitly designed for priority intervention and regional disparity detection.

### 3.2 Geographic Hierarchy & Relationships
- **States:** `states` table (`state_id`, `state_code`, `state_name`, `lgd_code`, `lifecycle_status_id`, `deleted_at`).
- **Districts:** `districts` table (`district_id`, `state_id`, `district_code`, `district_name`, `lgd_code`, `lifecycle_status_id`, `deleted_at`).
  - Foreign key `fk_districts_state` links `districts.state_id` to `states.state_id`.
- **Training Centers:** `training_centers` table (`center_id`, `provider_id`, `center_code`, `center_name`, `state_id`, `district_id`, `location_id`, `operational_capacity`, `deleted_at`).
  - Linked to `states` via `state_id` and to `districts` via `district_id`.
  - Linked to `locations` via optional `location_id`.

### 3.3 Data Availability Audit for Regional Disparity Indicators
1. **Rural vs. Urban Classification:**
   - `districts` table: **No** rural/urban column.
   - `training_centers` table: **No** rural/urban column.
   - `trainees` table: **No** rural/urban column.
   - `locations` table has `location_type_id` referencing `ref_location_type` (`CITY`, `TOWN`, `VILLAGE`, `URBAN_LOCAL_BODY`, `OTHER`), but `locations` represents arbitrary geographic points without a verified binary rural/urban classification flag.
   - **Conclusion:** Reliable rural vs. urban classification does NOT exist in the source schema. Omitted and explicitly documented.
2. **Aspirational District Indicators:**
   - Neither `districts` nor any master table contains an aspirational district column or mapping table.
   - **Conclusion:** Aspirational district indicator does NOT exist in the source schema. Omitted and explicitly documented.
3. **District-Level Population & Density Denominators:**
   - Neither `districts` nor `states` contains population census or geographic area (sq km) figures.
   - **Conclusion:** Population-based enrollment density is NOT supported by source data. Omitted and explicitly documented.
4. **Underserved Regions:**
   - The verified schema DOES support defensible indicators:
     - Identification of districts with **zero recorded enrollments** (`districtsWithZeroEnrollment`).
     - Identification of districts with **zero operational training centers** (`districtsWithoutTrainingCenters`).
     - Employment and placement performance gaps between lowest and highest performing districts (`employmentRateGapPct`, `placementRateGapPct`).

---

## 4. Architecture & Component Design

```
+-------------------------------------------------------------------------------------------------+
|                                 GeographicDashboardController                                   |
|               (/api/v1/dashboards/geography, /states/{stateId}, /districts/{districtId})        |
|                                [PreAuthorize("hasAuthority('analytics.read')")]                 |
+-----------------------------------------------+-------------------------------------------------+
                                                |
                                                v
+-------------------------------------------------------------------------------------------------+
|                                  GeographicDashboardService                                     |
|                               (GeographicDashboardServiceImpl)                                  |
|       - Uses AnalyticsCalculationUtils for null-safe BigDecimal (scale 2, RoundingMode.HALF_UP) |
|       - Reconciles state totals with district metrics mathematically                            |
+-----------------------------------------------+-------------------------------------------------+
                                                |
                                                v
+-------------------------------------------------------------------------------------------------+
|                                 GeographicDashboardRepository                                   |
|                           (JdbcTemplate over MySQL Analytical Views & Tables)                   |
|                  vw_district_outcome_summary | districts | states | training_centers            |
+-------------------------------------------------------------------------------------------------+
```

### 4.1 Repository Architecture & Static Analysis Invariant Preservation
In Phase 26.11, `FilterSearchSortingSecurityTest` locked in:
- Exactly 137 Spring Data `JpaRepository` interfaces.
- Exactly 2 annotated `@Query` declarations across all repository interfaces.

To adhere strictly to these invariants without causing regressions, `GeographicDashboardRepository` is implemented as a dedicated Spring `@Repository` using `JdbcTemplate`. It does not extend `JpaRepository` and introduces zero `@Query` annotations.

### 4.2 Projections (`in.gov.sih.sih26135.repository.analytics.projection`)
1. `DistrictPerformanceProjection`: Raw row mapping for Query Q06 (16 fields).
2. `StatePerformanceProjection`: Raw row mapping for state-level aggregations (15 fields).
3. `TrainingCenterSummaryProjection`: Raw row mapping for training center summaries in a district (5 fields).

### 4.3 Response DTOs (`in.gov.sih.sih26135.dto.response`)
1. `DistrictPerformanceResponse`: Immutable record representing a district outcome row with `rank`.
2. `StatePerformanceResponse`: Immutable record representing state outcome summary with `districtCount` and `trainingCenterCount`.
3. `TrainingCenterSummaryResponse`: Immutable record representing non-PII training center operational details.
4. `RegionalDisparityResponse`: Disparity metrics including top/lowest performing districts, outcome gaps, zero-activity counts, and `dataAvailabilityNotes`.
5. `GeographicOverviewResponse`: National overview combining national totals, state summaries, district rankings, and regional disparity.
6. `StateGeographicResponse`: State-specific analytics with summary KPIs, in-state district rankings, and state disparity.
7. `DistrictGeographicResponse`: District-specific analytics with parent state, performance KPIs, national rank, state rank, and training center breakdown.

---

## 5. Endpoints & API Contract Documentation

All endpoints are hosted under `/api/v1/dashboards`, require the `analytics.read` authority, and return responses wrapped in `ResponseEntity<ApiResponse<T>>`.

| HTTP Method | Route | Authorization | Return Envelope | Description & Semantics |
|:---|:---|:---|:---|:---|
| `GET` | `/api/v1/dashboards/geography` | `analytics.read` | `ApiResponse<GeographicOverviewResponse>` | National geographic distribution, state rollups, Q06 district rankings, and regional disparity overview. |
| `GET` | `/api/v1/dashboards/states/{stateId}` | `analytics.read` | `ApiResponse<StateGeographicResponse>` | State-specific geographic analytics, in-state district leaderboard, and training center distributions. |
| `GET` | `/api/v1/dashboards/districts/{districtId}` | `analytics.read` | `ApiResponse<DistrictGeographicResponse>` | District-specific outcomes, national and within-state ranks, and operational training center distributions. |

### 5.1 Error Handling & Validation
- **Path Variable Validation:** Parameters `@PathVariable @Positive Long stateId` and `@PathVariable @Positive Long districtId` are validated via Spring MVC/Validation. Invalid values (`<= 0` or non-numeric) return HTTP 400 with `ErrorResponse` (`VALIDATION_ERROR` or `TYPE_MISMATCH`).
- **Resource Not Found:** If a requested `stateId` or `districtId` does not exist in `states` or `districts` tables, `ResourceNotFoundException` is thrown, returning HTTP 404 with `ErrorResponse` (`RESOURCE_NOT_FOUND`).
- **Trusted Controller Allowlist:** `GeographicDashboardController.class` was added to `TRUSTED_CONTROLLER_CLASSES` in `GlobalExceptionHandler` to ensure client parameter constraint violations return HTTP 400 instead of failing closed to 500.

---

## 6. Mathematical Formulas & Precision Standards

1. **District-Level Rates (Query Q06):**
   - `completionRatePct = (completedCount / enrollmentCount) * 100`
   - `certificationRatePct = (certifiedCount / enrollmentCount) * 100`
   - `placementRatePct = (joinedPlacementCount / enrollmentCount) * 100`
   - `employmentRatePct = (employedCount / enrollmentCount) * 100`
2. **State & National Aggregation Rollups:**
   - State count metrics are the exact sum of district count metrics.
   - National count metrics are the exact sum of state count metrics.
   - Rates are calculated via `AnalyticsCalculationUtils.calculatePercentage(numerator, enrollmentCount)` with scale 2 and `RoundingMode.HALF_UP`.
   - If `enrollmentCount == 0L`, rates safely evaluate to `null` (matching SQL `NULLIF` semantics).
3. **Regional Disparity Metrics:**
   - `employmentRateGapPct = max(employmentRatePct) - min(employmentRatePct)`
   - `placementRateGapPct = max(placementRatePct) - min(placementRatePct)`
   - `districtsWithZeroEnrollment = COUNT(d) WHERE NOT EXISTS (SELECT 1 FROM vw_district_outcome_summary)`
   - `districtsWithoutTrainingCenters = COUNT(d) WHERE NOT EXISTS (SELECT 1 FROM training_centers)`

### 6.1 Distinction Between Integration & Unit Rate Verifications
- **District-Level Q06 Rates (Integration Test):** In `GeographicDashboardIntegrationTest.testLiveQ06DistrictOutcomes`, every district row returned by the live database view `vw_district_outcome_summary` is independently verified:
  - If `enrollmentCount > 0L`, all 4 rates are compared directly with `AnalyticsCalculationUtils.calculatePercentage` using `isEqualByComparingTo` and assert scale 2.
  - If `enrollmentCount == 0L`, all 4 rates assert `isNull()`.
- **National Rate Formulas (Unit Tests):** In `GeographicDashboardServiceTest`, multiple unit tests mock non-zero enrollment figures, completions, certifications, placements, and employment spells, independently asserting that:
  - `overallCompletionRatePct`, `overallCertificationRatePct`, `overallPlacementRatePct`, and `overallEmploymentRatePct` are strictly derived via `AnalyticsCalculationUtils` and correctly formatted to scale 2.
  - Boundary zero-denominator conditions evaluate safely to `null`.
- **National Rates in Live Integration Test:** In `GeographicDashboardIntegrationTest.testLiveNationalGeographicOverview`, if `totalEnrollments > 0L`, all 4 national rate formulas are independently checked against `AnalyticsCalculationUtils`. If `totalEnrollments == 0L` (e.g. empty test catalog), the test strictly verifies that all 4 national rates evaluate safely to `null` without throwing division-by-zero exceptions.

---

## 7. Deterministic Fixture Management, Collision Protection & Selective Cleanup

In `GeographicDashboardIntegrationTest`, deterministic test fixtures are managed safely without altering schema or deleting pre-existing test data:
1. **Zero Unconditional Deletes in `@BeforeEach`:**
   - Prior unconditional DELETE statements for fixture IDs were completely eliminated.
   - The test never blindly executes `DELETE` statements on startup.
2. **Pre-test Existing Data Discovery:**
   - In `@BeforeEach`, the test first inspects whether an active state with active districts already exists in `SIH26135_TEST`:
     ```sql
     SELECT DISTINCT s.state_id FROM states s JOIN districts d ON d.state_id = s.state_id
     WHERE s.deleted_at IS NULL AND d.deleted_at IS NULL ORDER BY s.state_id ASC LIMIT 1
     ```
   - If found, `resolvedStateId` and `resolvedDistrictId` bind to the existing records, setting insertion flags to `false`.
3. **Fail-Closed Collision Verification Before Insertion:**
   - If no state with districts exists, before attempting to insert deterministic fixtures, the test queries:
     ```sql
     SELECT COUNT(*) FROM states WHERE state_id = 99991
     SELECT COUNT(*) FROM districts WHERE district_id = 99991
     ```
   - If either ID is occupied by a pre-existing record, the test immediately fails with an `IllegalStateException` ("Cannot insert test state/district fixture: ID 99991 is already occupied by a pre-existing record..."). It refuses to overwrite, modify, or delete that pre-existing record.
4. **Selective Insertion & Guaranteed Execution:**
   - Only when verified unoccupied does the test insert:
     - State `99991` (`TS` / `Deterministic Integration State 99991`) with `stateFixtureInserted = true`.
     - District `99991` (`INT_TEST_D99991` / `Deterministic Integration District 99991`) with `districtFixtureInserted = true`.
   - `testLiveHttpStateGeographyEndpoint` executes HTTP GET `/api/v1/dashboards/states/{stateId}` unconditionally and verifies HTTP 200, `success=true`, and all payload fields.
   - `testLiveHttpDistrictGeographyEndpoint` executes HTTP GET `/api/v1/dashboards/districts/{districtId}` unconditionally and verifies HTTP 200, `success=true`, and all payload fields.
   - No `if (!states.isEmpty())` escape hatch is used; test assertions cannot be silently bypassed.
5. **Selective Post-test Teardown in `@AfterEach`:**
   - `@AfterEach` inspects `districtFixtureInserted` and `stateFixtureInserted`. It deletes ONLY the records inserted by the current test execution:
     - If `districtFixtureInserted == true`: `DELETE FROM districts WHERE district_id = 99991`.
     - If `stateFixtureInserted == true`: `DELETE FROM states WHERE state_id = 99991`.
   - Unrelated test-catalog data is never deleted, overwritten, or modified.
6. **Safety & Isolation:** Only `SIH26135_TEST` is touched under `MysqlConnectionTest.assertDedicatedTestCatalog` protection. Schema validation (`spring.jpa.hibernate.ddl-auto=validate`) remains fully enforced.

---

## 8. Files Changed and Purpose

| File | Type | Reason / Change |
|:---|:---|:---|
| `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/projection/DistrictPerformanceProjection.java` | Created | Projection record for Q06 district outcomes. |
| `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/projection/StatePerformanceProjection.java` | Created | Projection record for state outcome rollups. |
| `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/projection/TrainingCenterSummaryProjection.java` | Created | Projection record for training center summaries. |
| `backend/src/main/java/in/gov/sih/sih26135/dto/response/DistrictPerformanceResponse.java` | Created | DTO record for district performance with rank. |
| `backend/src/main/java/in/gov/sih/sih26135/dto/response/StatePerformanceResponse.java` | Created | DTO record for state performance with rank. |
| `backend/src/main/java/in/gov/sih/sih26135/dto/response/TrainingCenterSummaryResponse.java` | Created | DTO record for training center summary. |
| `backend/src/main/java/in/gov/sih/sih26135/dto/response/RegionalDisparityResponse.java` | Created | DTO record for regional disparity analysis and data notes. |
| `backend/src/main/java/in/gov/sih/sih26135/dto/response/GeographicOverviewResponse.java` | Created | Consolidated DTO for GET `/api/v1/dashboards/geography`. |
| `backend/src/main/java/in/gov/sih/sih26135/dto/response/StateGeographicResponse.java` | Created | Consolidated DTO for GET `/api/v1/dashboards/states/{stateId}`. |
| `backend/src/main/java/in/gov/sih/sih26135/dto/response/DistrictGeographicResponse.java` | Created | Consolidated DTO for GET `/api/v1/dashboards/districts/{districtId}`. |
| `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/GeographicDashboardRepository.java` | Created | Dedicated JdbcTemplate repository executing Q06, state rollups, and district center queries. |
| `backend/src/main/java/in/gov/sih/sih26135/service/GeographicDashboardService.java` | Created | Service interface for geographic analytics. |
| `backend/src/main/java/in/gov/sih/sih26135/service/impl/GeographicDashboardServiceImpl.java` | Created | Implementation of geographic analytics, rate calculations, ranking, and disparity derivation. |
| `backend/src/main/java/in/gov/sih/sih26135/controller/GeographicDashboardController.java` | Created | REST controller exposing `/geography`, `/states/{stateId}`, and `/districts/{districtId}`. |
| `backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java` | Modified | Added `ExecutiveDashboardController.class` and `GeographicDashboardController.class` to `TRUSTED_CONTROLLER_CLASSES`. |
| `backend/src/test/java/in/gov/sih/sih26135/service/GeographicDashboardServiceTest.java` | Created | 7 unit tests verifying service calculations, rollups, ranking, and exception throwing. |
| `backend/src/test/java/in/gov/sih/sih26135/controller/GeographicDashboardControllerTest.java` | Created | 20 MockMvc tests verifying endpoint contracts, path variable validation, 404 handling, and security. |
| `backend/src/test/java/in/gov/sih/sih26135/service/GeographicDashboardIntegrationTest.java` | Created / Strengthened | 5 live database integration tests against `SIH26135_TEST` verifying Q06, rollups, HTTP endpoints, collision-safe fixtures, and selective cleanup. |
| `backend/src/test/java/in/gov/sih/sih26135/response/ResponseEnvelopeAlignmentContractTest.java` | Modified | Updated reflection test assertions: 56 -> 57 controllers, 634 -> 637 endpoints. |
| `backend/src/test/java/in/gov/sih/sih26135/validation/ControllerPathQueryHeaderValidationTest.java` | Modified | Updated reflection test assertions: 56 -> 57 controllers. |

---

## 9. Verification & Test Evidence

### 9.1 Focused Geographic Test Suite (32 tests)
Executed: `mvn test "-Dtest=GeographicDashboardServiceTest,GeographicDashboardControllerTest,GeographicDashboardIntegrationTest" -f backend/pom.xml`
- `GeographicDashboardServiceTest`: 7 tests passed.
- `GeographicDashboardControllerTest`: 20 tests passed (3 contract, 8 validation/404, 9 parameterized security).
- `GeographicDashboardIntegrationTest`: 5 live integration tests passed against `SIH26135_TEST`.
  - Confirmed unconditional HTTP 200 execution for both `/states/{stateId}` and `/districts/{districtId}`.
  - Confirmed pre-insertion collision protection and selective fixture cleanup.
  - Confirmed Q06 district rate verification against `AnalyticsCalculationUtils`.
  - Confirmed national aggregation rollups and rate null-safety verification.
- **Result:** 32 tests run, 0 failures, 0 errors, 0 skipped (Time: 01:01 min).

### 9.2 Reflection & Security Audit Tests (86 tests)
- `ControllerPathQueryHeaderValidationTest` & `ResponseEnvelopeAlignmentContractTest`:
  - Executed: `mvn test "-Dtest=ControllerPathQueryHeaderValidationTest,ResponseEnvelopeAlignmentContractTest" -f backend/pom.xml`
  - **Result:** 50 tests run, 0 failures, 0 errors, 0 skipped (Time: 37.25 s).
- `FilterSearchSortingSecurityTest`:
  - Executed: `mvn test -Dtest=FilterSearchSortingSecurityTest -f backend/pom.xml`
  - **Result:** 36 tests run, 0 failures, 0 errors, 0 skipped (137 JPA repositories and 2 `@Query` annotations strictly verified; Time: 26.44 s).

### 9.3 Executive Dashboard Regression Suite (32 tests)
Executed: `mvn test "-Dtest=ExecutiveDashboardServiceTest,ExecutiveDashboardControllerTest,ExecutiveDashboardIntegrationTest" -f backend/pom.xml`
- **Result:** 32 tests run, 0 failures, 0 errors, 0 skipped (Time: 34.23 s).

### 9.4 Full Backend Regression Suite Execution (1,141 tests)
Executed: `mvn test -f backend/pom.xml`
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 1141, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:10 min
[INFO] Finished at: 2026-11-04T18:36:21+05:30
[INFO] ------------------------------------------------------------------------
```
- **Total Tests Run:** 1,141
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0

---

## 10. Safety, Invariants & Diff Hygiene

- **Database Safety:** Production database `SIH26135` was never accessed or queried. All database-backed tests targeted only `SIH26135_TEST` under `MysqlConnectionTest.assertDedicatedTestCatalog` protection.
- **DDL Immutability:** `spring.jpa.hibernate.ddl-auto=validate` remained strictly enforced. Zero schema changes, migrations, or DDL statements were executed.
- **Repository Invariant:** Exactly 137 Spring Data `JpaRepository` interfaces and 2 `@Query` declarations verified by `FilterSearchSortingSecurityTest`.
- **Git Hygiene:** `git diff --check` passed with 0 errors. No git stage, commit, push, reset, stash, or clean operations were performed.

---

## 11. Final Verdict

All Phase 27.4 objectives, query catalog reconciliations, and acceptance criteria are satisfied with comprehensive empirical evidence.

**Verdict:** `READY FOR INDEPENDENT REVIEW`
