# Phase 27.8 — Skill Gap, Industry Demand Alignment & Remediation Analytics API Report

## 1. Phase Objective & Final Verdict

### 1.1 Objective
Implement skill-gap analytics, industry-demand alignment, and remediation uptake analytics for the SIH-26135 Spring Boot backend. Extending the dashboard analytics architecture established in Phases 27.1–27.7 and incorporating targeted corrections from technical reviews, this phase delivers:
1. **Top Current Skill Gaps Analytics** powered by formal analytical Query **Q13**.
2. **Skill Gap Severity Mix & Remediation Uptake Analytics** powered by formal analytical Query **Q14**.
3. **Consolidated Skill Gap Dashboard API** (`GET /api/v1/dashboards/skill-gaps`) exposed via `SkillGapDashboardController`.

The endpoint strictly enforces `@PreAuthorize("hasAuthority('analytics.read')")`, wraps responses in standardized `ApiResponse<T>` envelopes, guarantees arithmetic precision using `BigDecimal` (scale 2, `RoundingMode.HALF_UP`) and `AnalyticsCalculationUtils`, and adheres strictly to repository, security, and database safety invariants.

### 1.2 Final Verdict
- **Verdict:** `READY FOR INDEPENDENT REVIEW`
- **Justification:**
  - All Phase 27.8 acceptance criteria and targeted review corrections are fully addressed and verified.
  - **Q13 Sector/Role Investigation:** Verified that formal Query Q13 operates on `vw_skill_gap_summary`, which aggregates strictly by skill and severity code. Schema investigation confirmed that skills have an N:M relationship with job roles (`job_role_skills`) and courses (`course_skills`), while `skill_gap_assessments.job_role_id` is optional and absent on non-role assessments. Attempting to force-join sector or role dimensions into Q13 would alter skill-gap ranking semantics and induce cartesian duplication. Consequently, permanently null fields (`sectorId`, `sectorName`, `jobRoleId`, `jobRoleName`) have been cleanly removed from `TopSkillGapResponse`, avoiding artificial mappings while preserving genuine Q13 metrics.
  - **Q14 Recommendation Assignment Rate Investigation:** Verified that the underlying `skill_gap_recommendations` table records remediation actions created for skill gaps, but does not track recommendation assignment workflows (lacks assigned user, assignment timestamp, or assignment status) or completion dates. To avoid fabricating an artificial denominator, recommendation creation count (`recommendationCount`) is strictly distinguished from trainee acceptance (`acceptedRecommendationCount`, `acceptanceRatePct`). No unsupported assignment-rate percentage is claimed.
  - **Q14 Severity Reference Reconciliation:** Verified all 5 canonical severity categories from `ref_skill_gap_severity`: `NONE` (rank 1), `LOW` (rank 2), `MODERATE` (rank 3), `HIGH` (rank 4), and `CRITICAL` (rank 5). Documented that `HIGH` and `CRITICAL` represent distinct deficiency tiers (3 levels vs 4 levels) and must not be merged. Preserved source-defined severity codes and names without silent renaming.
  - **Nationwide Distinct Trainee Aggregation Correction & Missing Summary Edge Case:** Corrected service logic in `SkillGapDashboardServiceImpl` to ensure nationwide distinct trainee count is never calculated by summing severity-level distinct counts, nor defaulted to `0L` when aggregate summary data is missing. Trainees can have skill gaps spanning multiple severity categories, so summing category-level distinct counts overstates `totalAffectedTrainees`. The authoritative nationwide distinct count from `getFormalQ14OverallSummary()` (`COUNT(DISTINCT trainee_id)`) is preserved. If the summary projection is missing (`summaryProjection == null`), `totalAffectedTrainees` explicitly evaluates to `null` across all branches (whether severity query results are populated, empty, or null) because a missing aggregate projection cannot prove zero distinct trainees. Normal empty-database behavior (`0L`) is strictly maintained when the repository returns a valid zero-valued summary projection.
  - `SkillGapDashboardController` exposes `GET /api/v1/dashboards/skill-gaps` with `@Validated`, method security guards (`hasAuthority('analytics.read')`), and standard `ApiResponse` envelope.
  - Zero schema changes and zero DDL migrations (`ddl-auto=validate` strictly preserved).
  - Dedicated test catalog `SIH26135_TEST` verified by `MysqlConnectionTest.assertDedicatedTestCatalog`. Zero access to production catalog `SIH26135`.
  - Exactly 137 `JpaRepository` interfaces and 2 `@Query` declarations preserved (zero Spring Data JPA invariant violations).
  - Production REST controller count expanded cleanly from 60 to 61; endpoint count from 643 to 644.
  - 100% of all 61 controllers and 644 endpoints verified by `ResponseEnvelopeAlignmentContractTest` and `ControllerPathQueryHeaderValidationTest`.
  - Focused test suite (40 tests), existing dashboard regression suite (149 tests), and full backend regression test suite (1,245 tests) passed cleanly with 0 failures, 0 errors.
  - `git diff --check` passed with 0 whitespace errors. Zero git stage/commit/push/reset/clean actions performed.

---

## 2. Initial Git Branch & Working-Tree State

- **Repository Root:** `E:\project`
- **Backend Directory:** `E:\project\backend`
- **Initial Branch:** `main`
- **Initial Commit Hash:** `a91df00` ("Complete Phase 27.7 longitudinal retention and employment attrition analytics")
- **Initial Working-Tree State:** Completely clean.

---

## 3. Targeted Investigations & Architectural Findings

### 3.1 Q13 — Sector and Role Grouping Feasibility

1. **Inspection of Analytical Views and Tables:**
   - Source view `vw_skill_gap_summary` aggregates strictly by `skill_id, skill_code, skill_gap_severity_id, severity_code`.
   - Source table `skill_gaps` links to `skill_gap_assessments a`. While `a.job_role_id` and `a.course_id` exist, they are nullable and context-dependent. Non-role assessments (e.g. self-report, course evaluation, certification, manual review) have `job_role_id = NULL`.
   - Core catalog table `skills` has `skill_category_id`, but does NOT belong to a single sector or job role. Relationships `job_role_skills` and `course_skills` are many-to-many.
2. **Impact on Aggregation and Ranking:**
   - An N:M join to `job_role_skills` would artificially multiply a single skill gap across every job role requiring that skill, inflating `gap_count`, `current_gap_count`, and `trainee_count`.
   - Filtering on `vw_skill_gap_fact.job_role_id` would discard non-role-based skill gaps, corrupting Q13's nationwide top-20 ranking.
3. **Resolution:**
   - No artificial sector/role mapping was fabricated. Permanently null fields (`sectorId`, `sectorName`, `jobRoleId`, `jobRoleName`) were removed from `TopSkillGapResponse`. The API exposes genuine, verified skill-gap ranking metrics directly supported by formal Query Q13.

### 3.2 Q14 — Recommendation Assignment Rate Feasibility

1. **Inspection of `skill_gap_recommendations` Schema:**
   - Columns: `skill_gap_recommendation_id`, `skill_gap_id`, `recommendation_number`, `skill_gap_action_type_id`, `recommended_course_id`, `recommended_skill_id`, `is_accepted_flag`, `accepted_on`, `remarks`, `created_by_user_id`, `created_at`, `updated_at`.
   - Missing fields: There is no `assigned_to_user_id`, `assigned_on`, `assignment_status_id`, or `completed_on`.
2. **Analysis of Numerator and Denominator:**
   - A recommendation row represents a remediation action created for a skill gap. Multiple recommendations can be created for a single skill gap (`uk_sgr_number`).
   - Dividing `recommendation_count` by `current_gap_count` is an intensity ratio (which can exceed 100%), not an assignment rate.
   - Tracking whether a recommendation was assigned separate from creation does not exist in the schema.
3. **Resolution:**
   - Recommendation creation count (`recommendationCount`) is strictly distinguished from trainee acceptance (`acceptedRecommendationCount`).
   - The acceptance percentage is calculated as `(acceptedRecommendationCount / recommendationCount) * 100` via `AnalyticsCalculationUtils.calculatePercentage`.
   - No fabricated assignment rate is claimed or exposed.

### 3.3 Q14 — Severity Reference Categories Reconciliation

1. **Complete Reference Hierarchy (`ref_skill_gap_severity`):**
   - Rank 1: `NONE` ("No gap / meets requirement")
   - Rank 2: `LOW` ("Low", representing the Minor tier in business requirements)
   - Rank 3: `MODERATE` ("Moderate")
   - Rank 4: `HIGH` ("High")
   - Rank 5: `CRITICAL` ("Critical")
2. **Distinct Business Semantics:**
   - `HIGH` (shortage delta 3) and `CRITICAL` (shortage delta 4) are distinct categories in the schema and are not merged.
   - `NONE` (delta 0) can legitimately appear in findings where proficiency meets requirements and `is_current = 1`.
   - `LOW` is preserved with its source code `"LOW"` and name `"Low"`, avoiding silent renaming.
3. **Query Implementation:**
   - Q14 groups by `v.severity_code, sev.severity_name, sev.severity_rank` and orders by `current_gap_count DESC, sev.severity_rank ASC, v.severity_code ASC`. All 5 canonical categories are preserved without filtering or dropping records.

### 3.4 Nationwide Distinct Trainee Aggregation Correction & Missing Summary Edge Case

1. **Problem Analysis:**
   - In `SkillGapDashboardServiceImpl.getSkillGapDashboard()`, a fallback routine previously summed `s.traineeCount()` across `severityProjections` when `summaryProjection == null`.
   - Because each severity-level `traineeCount` measures distinct trainees within that severity category, trainees with gaps in multiple categories were counted multiple times.
   - Furthermore, in the edge case where `summaryProjection == null` and `severityProjections` was empty or null, the fallback defaulted `totalAffectedTrainees = 0L`. A missing aggregate projection does not prove zero distinct trainees; fabricating `0L` when data is missing is analytically invalid.
2. **Corrected Architecture:**
   - The authoritative repository query `getFormalQ14OverallSummary()` executes `COUNT(DISTINCT trainee_id)` over the complete current skill gap population in SQL, guaranteeing single-counting of trainees regardless of severity breadth.
   - In `SkillGapDashboardServiceImpl`, the invalid summing of trainee counts across severity groups was completely removed.
   - Whenever `summaryProjection == null`, `totalAffectedTrainees` strictly evaluates to `null`, regardless of whether `severityProjections` is populated, empty, or null.
   - Mutually exclusive partition metrics (`currentGapCount`, `recommendationCount`, `acceptedRecommendationCount`) may safely sum when severity items are present because each current skill gap belongs to exactly one severity. When severity items are absent, these partition metrics default to `0L`.
   - When the test database is empty, the SQL aggregate query returns a non-null projection with zero totals (`0L`), preserving normal zero-baseline behavior (`totalAffectedTrainees = 0L`).

---

## 4. Formal Q13 and Q14 SQL Definitions & Reconciliation

### 4.1 Query Q13 — Top Current Skill Gaps
```sql
SELECT
  v.skill_id,
  v.skill_code,
  s.skill_name,
  v.skill_gap_severity_id,
  v.severity_code,
  v.gap_count,
  v.trainee_count,
  v.current_gap_count,
  v.avg_gap_level_delta
FROM vw_skill_gap_summary v
LEFT JOIN skills s ON s.skill_id = v.skill_id
ORDER BY v.current_gap_count DESC, v.gap_count DESC, v.skill_id ASC, v.skill_gap_severity_id ASC
LIMIT 20;
```

### 4.2 Query Q14 — Severity Mix and Recommendation Uptake
```sql
SELECT
  v.severity_code,
  COALESCE(sev.severity_name, v.severity_code) AS severity_name,
  COUNT(*) AS current_gap_count,
  COUNT(DISTINCT v.trainee_id) AS trainee_count,
  COALESCE(SUM(v.recommendation_count), 0) AS recommendation_count,
  COALESCE(SUM(v.accepted_recommendation_count), 0) AS accepted_recommendation_count
FROM vw_skill_gap_fact v
LEFT JOIN ref_skill_gap_severity sev ON sev.severity_code = v.severity_code
WHERE v.is_current = 1
GROUP BY v.severity_code, sev.severity_name, sev.severity_rank
ORDER BY current_gap_count DESC, sev.severity_rank ASC, v.severity_code ASC;
```

### 4.3 Nationwide Summary Aggregation
```sql
SELECT
  COUNT(*) AS total_current_gaps,
  COUNT(DISTINCT trainee_id) AS total_affected_trainees,
  COALESCE(SUM(recommendation_count), 0) AS total_recommendations,
  COALESCE(SUM(accepted_recommendation_count), 0) AS total_accepted_recommendations
FROM vw_skill_gap_fact
WHERE is_current = 1;
```

---

## 5. Architectural Alignment & Safety Invariants

1. **Database Safety & Isolation:**
   - Production database `SIH26135` was never connected to or modified.
   - Dedicated test database catalog `SIH26135_TEST` verified by `MysqlConnectionTest.assertDedicatedTestCatalog`.
   - `spring.jpa.hibernate.ddl-auto=validate` strictly preserved with zero schema changes.
2. **Repository Invariants:**
   - Spring Data JPA repository count remains exactly 137.
   - Spring Data JPA `@Query` count remains exactly 2.
   - Repository persistence implemented cleanly using Spring `JdbcTemplate` in `SkillGapDashboardRepository`.
3. **Controller & Envelope Architecture:**
   - Registered `SkillGapDashboardController` in `GlobalExceptionHandler.TRUSTED_CONTROLLER_CLASSES`.
   - Base route: `/api/v1/dashboards`. Endpoint: `GET /api/v1/dashboards/skill-gaps`.
   - Method security: `@PreAuthorize("hasAuthority('analytics.read')")`.
   - Response envelope: Standard `ApiResponse<SkillGapDashboardResponse>`.
   - Total production controllers: 61; audited endpoints: 644.

---

## 6. Summary of Created and Modified Files

### 6.1 Created Files
1. `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/projection/TopSkillGapProjection.java`
2. `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/projection/SkillGapSeverityMixProjection.java`
3. `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/projection/SkillGapSummaryProjection.java`
4. `backend/src/main/java/in/gov/sih/sih26135/dto/response/TopSkillGapResponse.java` (streamlined 9-field record without null sector/role fields)
5. `backend/src/main/java/in/gov/sih/sih26135/dto/response/SkillGapSeverityBreakdownResponse.java`
6. `backend/src/main/java/in/gov/sih/sih26135/dto/response/SkillGapDashboardResponse.java`
7. `backend/src/main/java/in/gov/sih/sih26135/repository/analytics/SkillGapDashboardRepository.java`
8. `backend/src/main/java/in/gov/sih/sih26135/service/SkillGapDashboardService.java`
9. `backend/src/main/java/in/gov/sih/sih26135/service/impl/SkillGapDashboardServiceImpl.java` (corrected distinct-trainee aggregation)
10. `backend/src/main/java/in/gov/sih/sih26135/controller/SkillGapDashboardController.java`
11. `backend/src/test/java/in/gov/sih/sih26135/service/SkillGapDashboardServiceTest.java` (enhanced with overlapping-trainee and canonical severity tests)
12. `backend/src/test/java/in/gov/sih/sih26135/controller/SkillGapDashboardControllerTest.java`
13. `backend/src/test/java/in/gov/sih/sih26135/service/SkillGapDashboardIntegrationTest.java` (enhanced with canonical reference assertions)

### 6.2 Modified Files
1. `backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java` (allowlist registration)
2. `backend/src/test/java/in/gov/sih/sih26135/dto/response/DashboardDtoContractTest.java` (contract tests for updated DTOs)
3. `backend/src/test/java/in/gov/sih/sih26135/response/ResponseEnvelopeAlignmentContractTest.java` (61 controllers, 644 endpoints)
4. `backend/src/test/java/in/gov/sih/sih26135/validation/ControllerPathQueryHeaderValidationTest.java` (61 controllers)

---

## 7. Verification & Test Execution Results

### 7.1 Focused Phase 27.8 Test Suite
Command:
```bash
mvn test "-Dtest=SkillGapDashboardServiceTest,SkillGapDashboardControllerTest,SkillGapDashboardIntegrationTest,DashboardDtoContractTest" -f backend/pom.xml
```
- **Results:** `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`
- **Elapsed Time:** 81 seconds
- **Key Tests:**
  - `testOverlappingTraineesAcrossSeveritiesNotSummed`: confirms distinct categories summing to 160 trainees are not added, and authoritative SQL aggregate count of 125 is preserved.
  - `testMissingSummaryProjectionDoesNotProduceFabricatedTraineeTotal`: confirms missing summary projection with populated severity list results in `totalAffectedTrainees == null` rather than an artificial sum.
  - `testMissingSummaryProjectionWithEmptySeverityResultsReturnsNullTraineeTotal`: confirms missing summary projection with empty severity list results in `totalAffectedTrainees == null` rather than defaulting to `0L`.
  - `testAllFiveCanonicalSeverityCategoriesPreserved`: confirms all 5 reference severities (`NONE`, `LOW`, `MODERATE`, `HIGH`, `CRITICAL`) are evaluated distinctly without merging or renaming.
  - `testSkillGapDashboardEmptyResults`: confirms empty database with valid summary projection returns non-null zeros (`0L`).

### 7.2 Envelope Contract & Validation Regression Suite
Command:
```bash
mvn test "-Dtest=ResponseEnvelopeAlignmentContractTest,ControllerPathQueryHeaderValidationTest" -f backend/pom.xml
```
- **Results:** `Tests run: 50, Failures: 0, Errors: 0, Skipped: 0`
- **Audited Metrics:** 61 controllers, 644 REST endpoints inspected; zero contract violations.

### 7.3 Comprehensive Dashboard Regression Suite (Phases 27.3–27.8)
Command:
```bash
mvn test "-Dtest=ExecutiveDashboardControllerTest,ExecutiveDashboardServiceTest,ExecutiveDashboardIntegrationTest,GeographicDashboardControllerTest,GeographicDashboardServiceTest,GeographicDashboardIntegrationTest,InstitutionalAnalyticsControllerTest,InstitutionalAnalyticsServiceTest,InstitutionalAnalyticsIntegrationTest,EconomicOutcomeDashboardControllerTest,EconomicOutcomeDashboardServiceTest,EconomicOutcomeDashboardIntegrationTest,RetentionAttritionDashboardControllerTest,RetentionAttritionDashboardServiceTest,RetentionAttritionDashboardIntegrationTest,SkillGapDashboardControllerTest,SkillGapDashboardServiceTest,SkillGapDashboardIntegrationTest" -f backend/pom.xml
```
- **Results:** `Tests run: 150, Failures: 0, Errors: 0, Skipped: 0`
- **Elapsed Time:** 60 seconds

### 7.4 Full Backend Test Suite
Command:
```bash
mvn test -f backend/pom.xml
```
- **Results:** `Tests run: 1246, Failures: 0, Errors: 0, Skipped: 0`
- **Elapsed Time:** 2 minutes 44 seconds
- **Verification:** 100% test pass rate across all domain modules, workflows, security layers, analytics queries, and REST endpoints.

### 7.5 Whitespace and Git Sanity
- `git diff --check` executed with code 0 (no whitespace errors).
- Zero staged commits or branch deviations.

---

## 8. Remaining Schema Limitations

1. **No Direct Sector/Role Dimension in `vw_skill_gap_summary`:**
   Formal analytical query Q13 aggregates strictly by `skill_id, skill_code, skill_gap_severity_id, severity_code`. Because skills are mapped N:M to job roles and courses, and assessment job roles are optional, adding sector or job role to Q13 cannot be done without redefining the underlying view and query catalog or causing cartesian count inflation.
2. **No Recommendation Assignment Lifecycle:**
   Table `skill_gap_recommendations` tracks recommendation creation and trainee acceptance (`is_accepted_flag`, `accepted_on`), but does not track an intermediate assignment workflow (e.g., assignment to a counselor/instructor or trainee notification date). Recommendation assignment rate cannot be derived without schema extensions to track assignment events.

---

## 9. Conclusion & Next Steps

All targeted review corrections for Phase 27.8, including nationwide distinct trainee aggregation correctness, have been completed, verified against `SIH26135_TEST`, and documented.

The implementation is verified and ready for independent review. Execution stops here prior to Phase 27.9.
