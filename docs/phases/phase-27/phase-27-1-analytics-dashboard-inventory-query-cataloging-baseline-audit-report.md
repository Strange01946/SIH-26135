# Phase 27.1 — Analytics & Dashboard Inventory, Query Cataloging & Baseline Audit Report

**Date:** November 4, 2026  
**System:** SIH-26135 Centralized Placement & Tracking System  
**Module:** `sih26135-backend` (Spring Boot 3.4.10 / Java 17 / Hibernate 6.6.29.Final / MySQL 8.0.46)  
**Repository Location:** `E:\project` (Backend: `E:\project\backend`)  
**Git Branch:** `main` (HEAD commit: `9543d03 Complete Phase 26.13 validation and pagination regression testing`)  
**Audit Type:** Read-Only Inventory, Query Cataloging & Performance Baseline Audit  
**Database Target:** `SIH26135_TEST` (`spring.jpa.hibernate.ddl-auto=validate`)  
**Verdict:** `READY FOR PHASE 27.2`

---

## 1. Phase Objective & Final Verdict

### 1.1 Objective
Phase 27.1 executes an evidence-driven, comprehensive, read-only audit of the existing analytical data assets, JPA persistence layer, and REST interfaces in the SIH-26135 backend. The objective is to establish an empirical technical baseline for Phase 27 (Analytics & Dashboard API Layer, Phases 27.2–27.17) by:
1. Cataloging all 22 formal dashboard analytical SQL queries defined in `database/phase-18-dashboard-analytical-queries.sql` (Q01–Q22).
2. Auditing all 21 pre-computed MySQL 8 analytical views (`vw_*_fact` and `vw_*_summary`).
3. Auditing all 24 analytics JPA entities/IDs and 21 analytics repositories.
4. Auditing the 2 existing analytics controllers (`AnalyticsFactController` and `AnalyticsSummaryController`).
5. Profiling query execution plans for all 22 formal queries using MySQL 8 `EXPLAIN` against the dedicated test catalog `SIH26135_TEST`.
6. Verifying the complete backend test suite baseline against reference figures (1,022 passing tests).
7. Identifying all gaps in pagination, query volume bounding, parameter sanitization, and organizational tenancy scoping.

### 1.2 Final Verdict
**`READY FOR PHASE 27.2`**  
The technical baseline is fully inventoried, profiled, and verified. Exactly 22 formal analytical queries, 21 database views, 24 JPA entities/IDs, 21 repositories, and 165 controller endpoints were audited. Execution plans for 100% of the queries (22 of 22) were generated via MySQL 8 `EXPLAIN`, identifying 5 High-risk and 17 Medium-risk query structures. The test baseline of 1,022 passing tests (0 failures, 0 errors, 0 skipped) was independently verified against `SIH26135_TEST` with zero production database access and zero schema modifications.

---

## 2. Initial Git Branch & Working-Tree State

- **Branch:** `main`
- **HEAD Commit:** `9543d03 Complete Phase 26.13 validation and pagination regression testing`
- **Initial Working Tree Status (`git status`):**
  - Unstaged deletions: 21 phase documentation files moved out of repository root and `docs/` in the preceding organization task.
  - Untracked directories: `docs/phases/` (containing organized `phase-25/` and `phase-26/` documentation).
  - No application code, tests, migrations, or database configurations were modified or staged.

---

## 3. Repository & Technology Reconnaissance

| Component | Verified Technology / Version | Verification Method |
|---|---|---|
| **Java Platform** | Java 17.0.19 (Eclipse Adoptium OpenJDK 64-Bit Server VM) | Maven runtime logs & `java -version` |
| **Framework** | Spring Boot 3.4.10 | `pom.xml`, test banner, application context |
| **ORM / JPA** | Hibernate ORM core 6.6.29.Final | HikariCP bootstrap logs |
| **Connection Pool**| HikariCP 5.1.0 | HikariPool-1 to HikariPool-10 initialization |
| **Database Server**| MySQL Community Server 8.0.46 (Win64 on x86_64) | `mysql.exe --version`, `SELECT @@version` |
| **Active Catalog** | `SIH26135_TEST` (Strictly isolated; production `SIH26135` untouched) | `MysqlConnectionTest`, `DATABASE()` |
| **DDL Invariant**  | `spring.jpa.hibernate.ddl-auto=validate` | `application.properties` (main & test) |

---

## 4. Expected vs. Actual Inventory Counts

| Component Category | Expected Inventory | Actual Observed Inventory | Discrepancy & Explanation | Status |
|---|---:|---:|---|:---:|
| **Analytical Database Views** | 21 | 21 | None. Exactly 11 fact views and 10 summary views exist in `SIH26135_TEST`. | **MATCH** |
| **Analytics Entities & Composite IDs** | 24 | 24 | None. Exactly 21 `@Immutable` entity classes and 3 composite ID classes. | **MATCH** |
| **Analytics Repositories** | 21 | 21 | None. 21 Spring Data JPA repository interfaces in `repository.analytics`. | **MATCH** |
| **Analytics Controllers** | 2 | 2 | None. `AnalyticsFactController` and `AnalyticsSummaryController`. | **MATCH** |
| **Executive / Persona Dashboard Controllers** | 0 | 0 | None exist. Confirmed production gap to be addressed in Phase 27. | **GAP CONFIRMED** |
| **Formal Analytical SQL Queries** | 22 | 22 | None. Exactly 22 queries (Q01–Q22) in `phase-18-dashboard-analytical-queries.sql`. | **MATCH** |
| **Total Controller Endpoints (Analytics)** | Unspecified | 165 | 103 endpoints in `AnalyticsFactController`, 62 in `AnalyticsSummaryController`. | **AUDITED** |
| **Unbounded List Endpoints** | Unspecified | 124 | 124 endpoints return unbounded `List<T>` (87 in `AnalyticsFactController`, 37 in `AnalyticsSummaryController`). Exactly 0 endpoints accept `Pageable`. | **CRITICAL DEFECT** |
| **Passing Test Suite Baseline** | 1,022 | 1,022 | None. Full backend test suite executed: 1,022 passed, 0 failed, 0 skipped. | **MATCH** |

---

## 5. Formal SQL Query Catalog (Q01–Q22)

The 22 formal dashboard queries declared in `database/phase-18-dashboard-analytical-queries.sql` were analyzed for underlying views, tables, calculations, and grouping logic:

| Query ID | Name / Business Meaning | Source Views & Tables | Aggregation & Calculation Logic | Grouping / Sorting Dimensions |
|---|---|---|---|---|
| **Q01** | Dashboard KPI strip | `vw_trainee_outcome_summary`, `vw_enrollment_outcome_fact`, `vw_placement_fact`, `vw_employment_fact` | 12 scalar subqueries: `COUNT(*)`, `COUNT(DISTINCT trainee_id)`, `SUM(is_completed_flag)`, `SUM(has_issued_certificate_flag)`, `SUM(joined_flag)`, `SUM(is_current)`, `SUM(is_self_employment)`, `SUM(is_apprenticeship)` | Single aggregate row; no grouping or sorting |
| **Q02** | Training pipeline conversion rates | `vw_enrollment_outcome_fact` | `COUNT(*)`, `SUM(...)`, `ROUND(SUM(...) / NULLIF(COUNT(*), 0) * 100, 2)` for completion, certification, placement, and employment rates | Single pipeline conversion summary row |
| **Q03** | Program performance with scheme codes | `vw_program_outcome_summary v`, `programs p`, `schemes s` | Selects pre-aggregated counts and rates (`completion_rate_pct`, `placement_rate_pct`, etc.) joined with program and scheme metadata | `ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC` |
| **Q04** | Training provider leaderboard | `vw_provider_outcome_summary v`, `training_providers tp` | Selects provider outcome rates and volumes joined with provider code and name | `ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC` |
| **Q05** | Course outcome table | `vw_course_outcome_summary v`, `courses c` | Selects course outcome rates and volumes joined with course code and name | `ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC` |
| **Q06** | District outcome map / table | `vw_district_outcome_summary v`, `districts d`, `states st` | Selects district outcome rates and volumes joined with district and state codes/names | `ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct ASC, v.enrollment_count DESC` |
| **Q07** | Employer hiring dashboard | `vw_employer_hiring_summary v`, `employers e` | Selects placement, joined counts, `join_rate_pct`, `avg_joining_salary` joined with employer name/code | `ORDER BY v.joined_count DESC, v.placement_count DESC` |
| **Q08** | Employment mix analysis | `vw_employment_fact` | `COUNT(*)`, `COUNT(DISTINCT trainee_id)`, `SUM(is_current)`, `SUM(is_wage_employment)`, `SUM(is_self_employment)`, `SUM(is_apprenticeship)`, `AVG(duration_days)` | Single aggregate row across all employment spells |
| **Q09** | Engagement type breakdown | `vw_employment_fact` | `COUNT(*)`, `COUNT(DISTINCT trainee_id)`, `SUM(is_current)`, `AVG(duration_days)` | `GROUP BY engagement_type_code, is_wage_employment, is_self_employment, is_apprenticeship, is_internship ORDER BY spell_count DESC` |
| **Q10** | Salary progression KPI | `vw_salary_progression_fact` | `COUNT(*)`, `AVG(first_recorded_salary)`, `AVG(latest_recorded_salary)`, `AVG(salary_change_amount)`, `AVG(salary_change_pct)` | Single row macroeconomic salary uplift metrics |
| **Q11** | Salary progression by course | `vw_salary_progression_fact sp`, `courses c` | `COUNT(*)`, `AVG(first_recorded_salary)`, `AVG(latest_recorded_salary)`, `AVG(salary_change_pct)` | `GROUP BY sp.course_id, c.course_code, c.course_name ORDER BY avg_salary_change_pct IS NULL, avg_salary_change_pct DESC` |
| **Q12** | Retention rates across milestones | `vw_employment_retention_fact` | `COUNT(*)`, `COUNT(retained_Xm_flag)`, `SUM(retained_Xm_flag)`, `ROUND(SUM(...) / NULLIF(COUNT(...), 0) * 100, 2)` for 6m, 12m, 24m, 36m | Single row multi-milestone retention survival metrics |
| **Q13** | Top current skill gaps | `vw_skill_gap_summary` | Selects `skill_id`, `skill_code`, `severity_code`, `gap_count`, `current_gap_count`, `avg_gap_level_delta` | `ORDER BY current_gap_count DESC, gap_count DESC LIMIT 20` |
| **Q14** | Skill gap severity mix & uptake | `vw_skill_gap_fact` | `COUNT(*)`, `COUNT(DISTINCT trainee_id)`, `SUM(recommendation_count)`, `SUM(accepted_recommendation_count)` where `is_current = 1` | `GROUP BY severity_code ORDER BY current_gap_count DESC` |
| **Q15** | Unemployment period KPI | `vw_unemployment_fact` | `COUNT(*)`, `COUNT(DISTINCT trainee_id)`, `SUM(is_current)`, `SUM(reemployed_flag)`, `SUM(never_preceded_by_employment_flag)`, `AVG(duration_days)` | Single row unemployment duration & re-employment metrics |
| **Q16** | Unemployment reasons distribution | `vw_unemployment_reason_summary` | Selects `period_count`, `trainee_count`, `current_period_count`, `reemployed_count`, `avg_duration_days` | `ORDER BY period_count DESC` |
| **Q17** | Exit reasons & separation nature | `vw_attrition_reason_summary` | Selects `exit_count`, `trainee_count` by exit reason, separation nature, voluntary/involuntary flags | `ORDER BY exit_count DESC` |
| **Q18** | Voluntary vs involuntary totals | `vw_attrition_reason_summary` | `SUM(CASE WHEN is_voluntary_flag = 1 THEN exit_count ELSE 0 END)`, involuntary sums, total sums | Single row total voluntary vs involuntary exit counts |
| **Q19** | Follow-up success by wave | `vw_followup_outcome_summary` | Selects `task_count`, `trainee_count`, `success_count`, `no_response_count`, `unreachable_count`, `success_rate_pct` | `ORDER BY followup_offset_months IS NULL, followup_offset_months, followup_type_code` |
| **Q20** | Survey response pipeline | `vw_survey_response_fact` | `COUNT(*)`, `COUNT(DISTINCT trainee_id)`, `SUM(is_submitted_flag)`, `ROUND(SUM(...) / NULLIF(COUNT(*), 0) * 100, 2)` | `GROUP BY survey_purpose_code, followup_offset_months ORDER BY followup_offset_months, survey_purpose_code` |
| **Q21** | Verification outcomes (all cycles) | `vw_employment_verification_fact` | `COUNT(*)`, `COUNT(DISTINCT trainee_id)`, `COUNT(DISTINCT employment_id)` | `GROUP BY record_verification_status_code, is_verified_flag ORDER BY verification_count DESC` |
| **Q22** | Current verification by method | `vw_employment_verification_fact` | `COUNT(*)`, `COUNT(DISTINCT trainee_id)` where `is_current = 1` | `GROUP BY verification_method_code, is_trainee_self_reported, is_official_flag, record_verification_status_code ORDER BY count DESC` |

---

## 6. Analytical Database View Inventory (21 Views)

All 21 views are physically created in MySQL catalog `SIH26135_TEST`, confirmed by `information_schema.VIEWS`:

| View Name | View Grain / Type | Base Tables Referenced | Dependent Queries (Q01–Q22) | Application Repository |
|---|---|---|---|---|
| `vw_enrollment_outcome_fact` | Fact (1 row / enrollment) | `training_enrollments`, `ref_enrollment_status`, `trainees`, `programs`, `certifications`, `placement_records`, `employment_records` | Q01, Q02 | `EnrollmentOutcomeFactRepository` |
| `vw_placement_fact` | Fact (1 row / placement) | `placement_records`, `ref_placement_status`, `ref_joining_status`, `trainees`, `training_enrollments`, `job_postings`, `employers` | Q01 | `PlacementFactRepository` |
| `vw_employment_fact` | Fact (1 row / employment spell) | `employment_records`, `ref_engagement_type`, `ref_verification_status`, `trainees`, `employers` | Q01, Q08, Q09 | `EmploymentFactRepository` |
| `vw_salary_progression_fact` | Fact (1 row / employment spell) | `employment_records`, `trainees`, `training_enrollments` | Q10, Q11 | `SalaryProgressionFactRepository` |
| `vw_employment_retention_fact`| Fact (1 row / employment spell) | `employment_records`, `trainees` | Q12 | `EmploymentRetentionFactRepository` |
| `vw_skill_gap_fact` | Fact (1 row / trainee-skill assessment) | `trainee_skill_gap_assessments`, `skills`, `ref_skill_gap_severity`, `learning_plans` | Q14 | `SkillGapFactRepository` |
| `vw_unemployment_fact` | Fact (1 row / unemployment spell) | `trainee_unemployment_events`, `ref_unemployment_reason`, `trainees` | Q15 | `UnemploymentFactRepository` |
| `vw_employment_exit_fact` | Fact (1 row / exit event) | `employment_exit_events`, `ref_employment_exit_reason`, `ref_separation_nature` | None in Phase 18 (CRUD view) | `EmploymentExitFactRepository` |
| `vw_followup_fact` | Fact (1 row / followup task) | `followup_tasks`, `ref_followup_status`, `ref_followup_type`, `followup_attempts` | None in Phase 18 (CRUD view) | `FollowupFactRepository` |
| `vw_survey_response_fact` | Fact (1 row / survey response) | `survey_responses`, `surveys`, `ref_survey_purpose`, `trainees` | Q20 | `SurveyResponseFactRepository` |
| `vw_employment_verification_fact`| Fact (1 row / verification attempt)| `employment_verification_attempts`, `ref_verification_method`, `ref_record_verification_status` | Q21, Q22 | `EmploymentVerificationFactRepository` |
| `vw_trainee_outcome_summary` | Summary (1 row / trainee) | `trainees`, `vw_enrollment_outcome_fact`, `vw_placement_fact`, `vw_employment_fact`, `vw_unemployment_fact`, `vw_skill_gap_fact` | Q01 | `TraineeOutcomeSummaryRepository` |
| `vw_program_outcome_summary` | Summary (1 row / program) | `programs`, `schemes`, `vw_enrollment_outcome_fact` | Q03 | `ProgramOutcomeSummaryRepository` |
| `vw_provider_outcome_summary` | Summary (1 row / provider) | `training_providers`, `vw_enrollment_outcome_fact` | Q04 | `ProviderOutcomeSummaryRepository` |
| `vw_course_outcome_summary` | Summary (1 row / course) | `courses`, `vw_enrollment_outcome_fact` | Q05 | `CourseOutcomeSummaryRepository` |
| `vw_district_outcome_summary` | Summary (1 row / district) | `districts`, `states`, `vw_enrollment_outcome_fact` | Q06 | `DistrictOutcomeSummaryRepository` |
| `vw_employer_hiring_summary` | Summary (1 row / employer) | `employers`, `vw_placement_fact` | Q07 | `EmployerHiringSummaryRepository` |
| `vw_unemployment_reason_summary`| Summary (1 row / exit reason) | `ref_unemployment_reason`, `vw_unemployment_fact` | Q16 | `UnemploymentReasonSummaryRepository` |
| `vw_attrition_reason_summary` | Summary (1 row / exit-nature pair)| `ref_employment_exit_reason`, `ref_separation_nature`, `vw_employment_exit_fact` | Q17, Q18 | `AttritionReasonSummaryRepository` |
| `vw_skill_gap_summary` | Summary (1 row / skill-severity)| `skills`, `ref_skill_gap_severity`, `vw_skill_gap_fact` | Q13 | `SkillGapSummaryRepository` |
| `vw_followup_outcome_summary` | Summary (1 row / type-wave pair)| `ref_followup_type`, `vw_followup_fact` | Q19 | `FollowupOutcomeSummaryRepository` |

---

## 7. Entity & Repository Inventory

### 7.1 Entities & Composite IDs (24 Classes in `in.gov.sih.sih26135.entity.analytics`)
All 21 entity classes are mapped with `@Entity`, explicit `@Table(name = "vw_...")`, and Hibernate `@Immutable`. Zero entities declare mutable setters or lifecycle mutations:
1. `AttritionReasonSummary.java` (Composite ID: `AttritionReasonSummaryId.java`)
2. `CourseOutcomeSummary.java` (ID: `Long courseId`)
3. `DistrictOutcomeSummary.java` (ID: `Long districtId`)
4. `EmployerHiringSummary.java` (ID: `Long employerId`)
5. `EmploymentExitFact.java` (ID: `Long exitEventId`)
6. `EmploymentFact.java` (ID: `Long employmentId`)
7. `EmploymentRetentionFact.java` (ID: `Long employmentId`)
8. `EmploymentVerificationFact.java` (ID: `Long verificationAttemptId`)
9. `EnrollmentOutcomeFact.java` (ID: `Long enrollmentId`)
10. `FollowupFact.java` (ID: `Long taskId`)
11. `FollowupOutcomeSummary.java` (Composite ID: `FollowupOutcomeSummaryId.java`)
12. `PlacementFact.java` (ID: `Long placementId`)
13. `ProgramOutcomeSummary.java` (ID: `Long programId`)
14. `ProviderOutcomeSummary.java` (ID: `Long providerId`)
15. `SalaryProgressionFact.java` (ID: `Long employmentId`)
16. `SkillGapFact.java` (ID: `Long assessmentId`)
17. `SkillGapSummary.java` (Composite ID: `SkillGapSummaryId.java`)
18. `SurveyResponseFact.java` (ID: `Long responseId`)
19. `TraineeOutcomeSummary.java` (ID: `Long traineeId`)
20. `UnemploymentFact.java` (ID: `Long eventId`)
21. `UnemploymentReasonSummary.java` (ID: `Long unemploymentReasonId`)

### 7.2 Repositories (21 Interfaces in `in.gov.sih.sih26135.repository.analytics`)
All 21 repositories extend `JpaRepository<T, ID>`.
- **Existing Query Methods:** Provide basic derived finders (e.g., `findByTraineeId`, `findByProgramId`, `findByCourseId`).
- **Pagination Support:** **ZERO** repositories declare `Pageable` parameters. All finder methods return unpaged `List<T>`.
- **Aggregation Methods:** **ZERO** repositories declare JPQL or native SQL aggregation projections matching Q01, Q02, Q08, Q09, Q10, Q11, Q12, Q14, Q15, Q18, Q20, Q21, or Q22.

---

## 8. Complete Query-to-Application Traceability Matrix (Q01–Q22)

| Q# | Query Business Meaning | Target View(s) | Entity / Repository | Existing Service Method | Existing Route | Mapping Status | Key Implementation Gaps |
|---|---|---|---|---|---|:---:|---|
| **Q01** | Dashboard KPI strip | `vw_trainee_outcome_summary`, `vw_enrollment_outcome_fact`, `vw_placement_fact`, `vw_employment_fact` | Fact & Summary Repositories | None | None | **Missing** | No composite KPI DTO; no service aggregation method; no `/api/v1/dashboards/overview` route. |
| **Q02** | Training pipeline conversion rates | `vw_enrollment_outcome_fact` | `EnrollmentOutcomeFact` | `EnrollmentOutcomeFactService` (unaggregated read only) | None | **Missing** | No pipeline funnel DTO; no conversion rate calculations; no endpoint. |
| **Q03** | Program performance with scheme codes | `vw_program_outcome_summary`, `programs`, `schemes` | `ProgramOutcomeSummary` | `ProgramOutcomeSummaryService.getAllProgramOutcomesOrderByEnrollmentCountDesc()` | `GET /api/v1/analytics/summaries/program-outcomes/leaderboard` | **Partial** | Does not join `programs`/`schemes` for code/name enrichment; sorted by `enrollmentCount DESC` instead of `employment_rate_pct DESC`; unbounded `List<T>`. |
| **Q04** | Training provider leaderboard | `vw_provider_outcome_summary`, `training_providers` | `ProviderOutcomeSummary` | `ProviderOutcomeSummaryService.getAllProviderOutcomesOrderByEnrollmentCountDesc()` | `GET /api/v1/analytics/summaries/provider-outcomes/leaderboard` | **Partial** | Sorted by `enrollmentCount DESC` instead of `employment_rate_pct DESC`; missing provider code/name join; unbounded `List<T>`. |
| **Q05** | Course outcome table | `vw_course_outcome_summary`, `courses` | `CourseOutcomeSummary` | `CourseOutcomeSummaryService.getAllCourseOutcomesOrderByEnrollmentCountDesc()` | `GET /api/v1/analytics/summaries/course-outcomes/leaderboard` | **Partial** | Sorted by `enrollmentCount DESC` instead of `employment_rate_pct DESC`; missing course code/name join; unbounded `List<T>`. |
| **Q06** | District outcome map/table | `vw_district_outcome_summary`, `districts`, `states` | `DistrictOutcomeSummary` | `DistrictOutcomeSummaryService.getAllDistrictOutcomesOrderByEnrollmentCountDesc()` | `GET /api/v1/analytics/summaries/district-outcomes/leaderboard` | **Partial** | Missing state/district name join; sorted by enrollment count rather than rate ASC/DESC; unbounded `List<T>`. |
| **Q07** | Employer hiring dashboard | `vw_employer_hiring_summary`, `employers` | `EmployerHiringSummary` | `EmployerHiringSummaryService.getAllEmployerHiringSummariesOrderByJoinedCountDesc()` | `GET /api/v1/analytics/summaries/employer-hirings/leaderboard` | **Partial** | Missing employer code/name join; missing employer ownership/tenancy filtering; unbounded `List<T>`. |
| **Q08** | Employment mix analysis | `vw_employment_fact` | `EmploymentFact` | None | None | **Missing** | No employment mix aggregate DTO; no service method; no dashboard endpoint. |
| **Q09** | Engagement type breakdown | `vw_employment_fact` | `EmploymentFact` | None | None | **Missing** | No group-by aggregation query in repository; no dashboard endpoint. |
| **Q10** | Salary progression KPI | `vw_salary_progression_fact` | `SalaryProgressionFact` | None | None | **Missing** | No salary progression DTO; no aggregate average calculation; no dashboard endpoint. |
| **Q11** | Salary progression by course | `vw_salary_progression_fact`, `courses` | `SalaryProgressionFact` | None | None | **Missing** | No course-level wage growth join query; no dashboard endpoint. |
| **Q12** | Retention rates across milestones | `vw_employment_retention_fact` | `EmploymentRetentionFact` | None | None | **Missing** | No milestone survival DTO; no retention calculation query; no dashboard endpoint. |
| **Q13** | Top current skill gaps | `vw_skill_gap_summary` | `SkillGapSummary` | `SkillGapSummaryService.getAllSkillGapSummariesOrderByCurrentGapCountDesc()` | `GET /api/v1/analytics/summaries/skill-gaps/leaderboard` | **Partial** | Does not enforce `LIMIT 20`; returns unbounded `List<T>`. |
| **Q14** | Skill gap severity mix & uptake | `vw_skill_gap_fact` | `SkillGapFact` | None | None | **Missing** | No group-by severity query; no recommendation uptake calculations; no dashboard endpoint. |
| **Q15** | Unemployment period KPI | `vw_unemployment_fact` | `UnemploymentFact` | None | None | **Missing** | No unemployment KPI DTO; no duration average query; no dashboard endpoint. |
| **Q16** | Unemployment reasons distribution | `vw_unemployment_reason_summary` | `UnemploymentReasonSummary` | `UnemploymentReasonSummaryService.getAllUnemploymentReasonSummariesOrderByPeriodCountDesc()` | `GET /api/v1/analytics/summaries/unemployment-reasons/leaderboard` | **Partial** | Returns unbounded `List<T>` without pagination or bounds. |
| **Q17** | Exit reasons & separation nature | `vw_attrition_reason_summary` | `AttritionReasonSummary` | `AttritionReasonSummaryService.getAllAttritionReasonSummariesOrderByExitCountDesc()` | `GET /api/v1/analytics/summaries/attrition-reasons/leaderboard` | **Partial** | Returns unbounded `List<T>` without pagination or bounds. |
| **Q18** | Voluntary vs involuntary totals | `vw_attrition_reason_summary` | `AttritionReasonSummary` | None | None | **Missing** | No aggregate totals query summing flags; no service method; no dashboard endpoint. |
| **Q19** | Follow-up success by wave | `vw_followup_outcome_summary` | `FollowupOutcomeSummary` | `FollowupOutcomeSummaryService.getAllFollowupOutcomeSummariesOrderBySuccessRatePctDesc()` | `GET /api/v1/analytics/summaries/followup-outcomes/leaderboard` | **Partial** | Sorted by `successRatePct DESC` rather than chronologically by offset wave; unbounded `List<T>`. |
| **Q20** | Survey response pipeline | `vw_survey_response_fact` | `SurveyResponseFact` | None | None | **Missing** | No group-by query in repository; no survey pipeline DTO; no dashboard endpoint. |
| **Q21** | Verification outcomes (all cycles) | `vw_employment_verification_fact` | `EmploymentVerificationFact` | None | None | **Missing** | No verification outcome group-by query; no dashboard endpoint. |
| **Q22** | Current verification by method | `vw_employment_verification_fact` | `EmploymentVerificationFact` | None | None | **Missing** | No current verification method group-by query; no dashboard endpoint. |

**Traceability Summary:**
- **Complete Mappings:** **0** (0.0%)
- **Partial Mappings:** **9** (40.9%)
- **Missing Mappings:** **13** (59.1%)

---

## 9. Analytics Controller Audit

### 9.1 `AnalyticsFactController.java` Audit
- **Location:** `in.gov.sih.sih26135.controller.analytics.AnalyticsFactController` (1,256 lines).
- **Base Route:** `/api/v1/analytics/facts`
- **Total Endpoints:** **103 endpoints**.
- **Scope Covered:** Exposes atomic fact rows across all 11 fact views (`/employment-exits`, `/employments`, `/retentions`, `/verifications`, `/enrollments`, `/followups`, `/placements`, `/salary-progressions`, `/skill-gaps`, `/survey-responses`, `/unemployments`).
- **Endpoint Structure:**
  - **87 Collection Endpoints (100% Unbounded):**
    - 11 `GET /<resource>` all-rows base collection endpoints returning all rows in unbounded `List<...FactResponse>` with `validateNoQueryParameters(httpRequest)`.
    - 76 `GET /<resource>?<filter>=<val>` filtered query-parameter endpoints returning unbounded `List<...FactResponse>` with strict mutually-exclusive `params` annotations (e.g. `params = {"traineeId", "!employmentId", ...}`).
  - **16 Single-Object Endpoints:**
    - 11 `GET /<resource>/{id}` path-variable endpoints returning a single entity response by primary key.
    - 5 `GET /<resource>?<uniqueNumber>=<val>` query-parameter endpoints returning a single entity response by unique business identifier (`employmentNumber`, `verificationNumber`, `enrollmentNumber`, `placementNumber`, `skillGapNumber`).
- **Key Vulnerabilities Identified:**
  1. **Unbounded Result Sets:** All 87 collection endpoints (11 all-rows + 76 filtered) return raw `List<...FactResponse>`. Exactly **0 endpoints accept `Pageable` or `size` limits**. On a production database with hundreds of thousands of enrollments or spells, invoking `GET /api/v1/analytics/facts/enrollments` will trigger a full table scan and exhaust JVM heap memory.
  2. **Missing Ownership / Tenancy Scoping:** Any user holding the broad authority `analytics.read` can view every fact record across all training providers, employers, and trainees nationwide.

### 9.2 `AnalyticsSummaryController.java` Audit
- **Location:** `in.gov.sih.sih26135.controller.analytics.AnalyticsSummaryController` (715 lines).
- **Base Route:** `/api/v1/analytics/summaries`
- **Total Endpoints:** **62 endpoints**.
- **Scope Covered:** Exposes pre-aggregated summary rows across all 10 summary views (`/attrition-reasons`, `/course-outcomes`, `/district-outcomes`, `/employer-hirings`, `/followup-outcomes`, `/program-outcomes`, `/provider-outcomes`, `/skill-gaps`, `/trainee-outcomes`, `/unemployment-reasons`).
- **Endpoint Structure:**
  - **37 Collection Endpoints (100% Unbounded):**
    - 10 `GET /<resource>` all-rows base collection endpoints returning all aggregate rows in unbounded `List<...SummaryResponse>`.
    - 9 `GET /<resource>/leaderboard` leaderboard collection endpoints returning sorted summary rows in unbounded `List<...SummaryResponse>` (attrition reasons, course outcomes, district outcomes, employer hirings, followup outcomes, program outcomes, provider outcomes, skill gaps, unemployment reasons; trainee outcomes does not declare a `/leaderboard` route).
    - 18 filtered query-parameter collection endpoints returning unbounded `List<...SummaryResponse>` (e.g., filtering by `isVoluntaryFlag`, `stateId`, `districtId`, `severityCode`, `schemeId`, `offsetMonths`).
  - **25 Single-Object Endpoints:**
    - 10 path-variable ID endpoints returning a single `...SummaryResponse` (7 single-key paths: `/{courseId}`, `/{districtId}`, `/{employerId}`, `/{programId}`, `/{providerId}`, `/{traineeId}`, `/{id}`, and 3 composite-key paths: `/{exitReasonId}/{separationNatureId}`, `/{typeCode}/{offsetMonths}`, `/{skillId}/{severityId}`).
    - 15 query-parameter single-object lookups returning a single `...SummaryResponse` by primary/composite key or unique code (e.g. `?courseId=`, `?programId=`, `?districtId=`, `?employerId=`, `?traineeId=`, `?unemploymentReasonId=`, `?code=`, `?reasonCode=`, `?exitReasonId=&separationNatureId=`, `?employmentExitReasonId=&separationNatureId=`, `?typeCode=&offsetMonths=`, `?followupTypeId=&offsetMonths=`, `?skillId=&severityId=`, `?skillId=&skillGapSeverityId=`).
- **Key Vulnerabilities Identified:**
  1. **Unbounded Collection Delivery:** All 37 collection endpoints return unbounded `List<...SummaryResponse>` with zero pagination. Exactly 0 collection endpoints declare `Pageable` parameters or result size limits.
  2. **Inverted or Naive Leaderboard Sorts:** Leaderboards sort exclusively by `enrollmentCount DESC` (e.g., in course, provider, and district outcomes), ignoring employment rates or chronological waves.
  3. **Absence of Join Enrichment:** Raw entity responses return foreign IDs (`courseId`, `providerId`, `employerId`) without joining parent reference tables for human-readable codes and names.

---

## 10. Pagination & Unbounded-Query Findings

```
Total Controller Endpoints Audited: 165
├── Endpoints returning unbounded List<T>: 124 (75.2%)
│   ├── AnalyticsFactController: 87 unbounded collection endpoints
│   │   ├── 11 all-rows base collection endpoints
│   │   └── 76 filtered collection endpoints
│   └── AnalyticsSummaryController: 37 unbounded collection endpoints
│       ├── 10 all-rows base collection endpoints
│       ├── 9 leaderboard collection endpoints
│       └── 18 filtered collection endpoints
└── Endpoints returning single object: 41 (24.8%)
    ├── AnalyticsFactController: 16 single-object endpoints
    │   ├── 11 path-variable ID endpoints (/{id})
    │   └── 5 unique identifier query-parameter endpoints
    └── AnalyticsSummaryController: 25 single-object endpoints
        ├── 10 path-variable ID endpoints
        └── 15 primary/composite key query-parameter endpoints

Endpoints supporting Spring Data Pageable: 0 (0.0%)
Endpoints with result size ceiling / limit: 0 (0.0%)
```

**Risk Assessment:** **CRITICAL**.  
The current analytics controllers violate the Phase 26 pagination standards (`PaginationUtils`, page size limits $1 \le \text{size} \le 100$, zero-based indexing). A single unrestricted request to high-volume fact views (`vw_enrollment_outcome_fact`, `vw_placement_fact`, `vw_employment_fact`) poses an immediate Denial-of-Service (DoS) and Out-Of-Memory (OOM) vulnerability.

---

## 11. Filter & Search Sanitization Findings

1. **Parameter Mutual Exclusion:** Both controllers enforce strict parameter isolation via `validateOnlyQueryParameter` and `validateNoQueryParameters`. Ambiguous or injected extra query parameters are rejected with HTTP 400 `BadRequestException`.
2. **Identifier Validation:** All path variables and query parameter IDs are annotated with Jakarta validation `@Positive Long id`.
3. **Missing Wildcard & Slicing Sanitization:** The controllers do not support keyword matching or dynamic date-range slicing (`startDate`, `endDate`, fiscal year). In future phases where multi-criteria slicing is introduced, Phase 26.11 `SearchSanitizationUtils` must be strictly applied.

---

## 12. Authentication, Authorization & Tenant-Scope Findings

1. **Class-Level Authority Check:** Both `AnalyticsFactController` and `AnalyticsSummaryController` enforce:
   ```java
   @PreAuthorize("hasAuthority('analytics.read')")
   ```
   Unauthenticated calls correctly return HTTP 401 Unauthorized; authenticated calls lacking `analytics.read` return HTTP 403 Forbidden.
2. **Absence of Tenancy & Ownership Scoping:**
   - The controllers currently lack any method-level or service-level integration with [`WorkflowSecurityScopeValidator.java`](file:///e:/project/backend/src/main/java/in/gov/sih/sih26135/security/WorkflowSecurityScopeValidator.java).
   - A Training Provider authenticated with `analytics.read` can view facts and summary aggregates for competing training providers.
   - An Employer authenticated with `analytics.read` can inspect hiring summaries and placement facts for other employers.
   - No state-level or district-level geographic boundary filters exist.
3. **Data Disclosure Risk:** High-volume analytics endpoints expose granular facts (salary progressions, verification attempts, unemployment events) across tenant boundaries without ownership checks.

---

## 13. MySQL Version & Database Target Verification

- **MySQL Version:** `8.0.46` (MySQL Community Server - GPL, Win64 on x86_64).
- **Target Catalog:** Dedicated test catalog `SIH26135_TEST`.
- **Runtime Catalog Assertion:** Guarded by `MysqlConnectionTest.assertDedicatedTestCatalog`, strictly verifying:
  ```java
  assertThat(connection.getCatalog()).isEqualToIgnoringCase("SIH26135_TEST");
  ```
- **Production Safety:** Production database `SIH26135` was **NEVER** connected to or queried.

---

## 14. EXPLAIN Execution-Plan Findings (Q01–Q22)

Execution plans were generated for all 22 queries using `EXPLAIN` and `EXPLAIN FORMAT=TREE` targeting `SIH26135_TEST`.

```
========================================================================================
MySQL 8 EXPLAIN Profiling Summary (22 Formal Queries)
========================================================================================
Q#   Plan Profile                           Access Types             Temp Table Filesort Risk
----------------------------------------------------------------------------------------
Q01  12 scalar subqueries over views        index,ref,eq_ref,ALL     Yes        No       MEDIUM
Q02  Aggregate scan over enrollment fact    const,ref,eq_ref,ALL     Yes        No       MEDIUM
Q03  View aggregate joined with 2 tables    const,ref,eq_ref,ALL     Yes        Yes      HIGH
Q04  View aggregate joined with provider    const,ref,eq_ref,ALL     Yes        Yes      HIGH
Q05  View aggregate joined with course      const,ref,eq_ref,ALL     Yes        Yes      HIGH
Q06  View aggregate joined with dist/state  const,ref,eq_ref,ALL     Yes        Yes      HIGH
Q07  View aggregate joined with employer    ref,ALL,eq_ref           Yes        Yes      MEDIUM
Q08  Single fact view aggregate scan        ALL,eq_ref               No         No       MEDIUM
Q09  Fact view group-by engagement flags    ALL,eq_ref               Yes        Yes      MEDIUM
Q10  Fact view aggregate salary averages    ref,ALL,eq_ref           No         Yes      MEDIUM
Q11  Fact view joined with courses table    ref,ALL,eq_ref           Yes        Yes      HIGH
Q12  Fact view milestone retention counts   ALL,eq_ref               No         No       MEDIUM
Q13  Summary view top 20 limit scan         ref,ALL,eq_ref,index     No         Yes      MEDIUM
Q14  Fact view severity mix & uptake        ref,eq_ref,index         Yes        Yes      MEDIUM
Q15  Fact view unemployment averages        ALL,eq_ref               No         No       MEDIUM
Q16  Summary view unemployment reasons      ALL,eq_ref               No         Yes      MEDIUM
Q17  Summary view attrition reasons         ALL,eq_ref               No         Yes      MEDIUM
Q18  Summary view voluntary sums            ALL,eq_ref               No         Yes      MEDIUM
Q19  Summary view follow-up wave scan       ALL,eq_ref               Yes        Yes      MEDIUM
Q20  Fact view survey response pipeline     ALL,eq_ref               Yes        Yes      MEDIUM
Q21  Fact view verification outcomes        ALL,eq_ref               Yes        Yes      MEDIUM
Q22  Fact view verification by method       ref,eq_ref               Yes        Yes      MEDIUM
========================================================================================
```

---

## 15. Performance Risk Classification & Evidence

### 15.1 High-Risk Queries (5 Queries: Q03, Q04, Q05, Q06, Q11)
- **Root Cause:**
  - Queries join complex analytical views (`vw_program_outcome_summary`, `vw_provider_outcome_summary`, `vw_course_outcome_summary`, `vw_district_outcome_summary`, `vw_salary_progression_fact`) with domain base tables (`programs`, `schemes`, `training_providers`, `courses`, `districts`, `states`).
  - Underlying views perform multi-table outer joins and subquery aggregations over high-volume transactional tables (`training_enrollments`, `certifications`, `placement_records`, `employment_records`).
  - The outer query performs an `ORDER BY v.employment_rate_pct DESC, v.enrollment_count DESC`, forcing MySQL to materialize an internal temporary table (`Using temporary`) and perform a disk/memory filesort (`Using filesort`) across all aggregated view rows.
- **Remediation Priority:** High. Requires Spring Cache / Caffeine caching (Phase 27.13) to avoid repeated view joins, along with pagination limits (Phase 27.12).

### 15.2 Medium-Risk Queries (17 Queries: Q01, Q02, Q07, Q08, Q09, Q10, Q12, Q13, Q14, Q15, Q16, Q17, Q18, Q19, Q20, Q21, Q22)
- **Root Cause:**
  - Full view scans (`ALL`) over fact views (`vw_employment_fact`, `vw_employment_retention_fact`, `vw_unemployment_fact`) without date-range or tenant bounding.
  - Q01 executes 12 independent scalar subqueries within a single `SELECT` block, triggering 12 separate view scans.
- **Remediation Priority:** Medium. Aggregate KPI responses must be cached with 15-minute TTLs and scoped by organizational boundaries.

---

## 16. Test Baseline Verification

### 16.1 Dedicated Test Execution
- **`MysqlConnectionTest` Execution:**
  - Command: `mvn test -Dtest=MysqlConnectionTest -f backend/pom.xml`
  - Outcome: 2 tests run, 0 failures, 0 errors, 0 skipped.
  - Verified: Catalog isolation guard active; 144 BASE TABLES and 21 VIEWS verified in `SIH26135_TEST`.
- **`BusinessWorkflowReadModelIntegrationTest` Execution:**
  - Command: `mvn test -Dtest=BusinessWorkflowReadModelIntegrationTest -f backend/pom.xml`
  - Outcome: 8 tests run, 0 failures, 0 errors, 0 skipped.
  - Verified: All 21 analytical views exist and are successfully queryable (`SELECT * FROM <view> LIMIT 1`).

### 16.2 Full Backend Regression Test Suite Execution
- **Command:** `mvn test -f backend/pom.xml`
- **Execution Time:** 01:41 min
- **Results:**
  ```
  [INFO] Results:
  [INFO] 
  [INFO] Tests run: 1022, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```
- **Discrepancy:** Exactly **0 discrepancies**. The reference baseline of 1,022 passing tests is 100% verified.

---

## 17. Database Safety & Schema Protection Confirmation

- **Schema Immutability:** `spring.jpa.hibernate.ddl-auto=validate` remained strictly enforced across all 4 properties locations.
- **Zero Schema Mutations:** Exactly 0 DDL statements, migrations, or table alterations were executed.
- **Zero Production Database Connections:** Connection URLs strictly pointed to `SIH26135_TEST`. Production catalog `SIH26135` had 0 connections and 0 mutations.
- **Read-Only Invariant:** No `INSERT`, `UPDATE`, or `DELETE` statements were issued against analytical views or tables. Only `EXPLAIN` and read-only test queries were executed.

---

## 18. Files Created & Modification Confirmation

- **Application Code Modified:** **NONE** (0 source files changed).
- **Test Code Modified:** **NONE** (0 test files changed).
- **Database Schema Modified:** **NONE** (0 SQL files changed).
- **New Files Created:**
  - Workspace Report: `docs/phases/phase-27/phase-27-1-analytics-dashboard-inventory-query-cataloging-baseline-audit-report.md`
  - Brain Artifact: `phase-27-1-analytics-dashboard-inventory-query-cataloging-baseline-audit-report.md`
  - Scratch Analysis Scripts: Located strictly within `<appDataDir>/brain/<conversation-id>/scratch/` (`explain_queries.py`, `analyze_explain.py`, `audit_controllers.py`, `generate_traceability_matrix.py`).

---

## 19. Git Diff & Hygiene Checks

- `git diff --check`: Exit code `0` (Zero whitespace or syntax errors).
- `git diff --cached --check`: Exit code `0` (Clean).
- Zero git commit, push, stage, reset, stash, or clean actions were executed.

---

## 20. Critical Findings & Recommended Priorities for Phase 27.2–27.17

1. **Phase 27.2 (DTOs & Math Utilities):** Define strongly-typed DTOs for KPI strips, conversion funnels, and leaderboards. Implement `AnalyticsCalculationUtils` for null-safe `BigDecimal` rate rounding (preventing division by zero).
2. **Phase 27.3 (Executive & National Overview):** Implement `ExecutiveDashboardController` implementing Q01 (KPI strip), Q02 (Pipeline funnel), and Q03 (Program matrix).
3. **Phase 27.4–27.9 (Domain Dashboards):** Implement dedicated controllers for Geographic Heatmaps (Q06), Provider/Course Leaderboards (Q04, Q05), Employer & Economic Outcomes (Q07, Q10, Q11), Retention & Attrition (Q12, Q15–Q18), Skill Gaps (Q13, Q14), and Verification SLA (Q21, Q22).
4. **Phase 27.10 (Tenancy Hardening):** Integrate `WorkflowSecurityScopeValidator` into all dashboard routes to restrict training providers to their own batches and employers to their own job postings.
5. **Phase 27.12 (Fact & Summary Pagination):** Refactor the 87 unbounded collection endpoints in `AnalyticsFactController` and 37 unbounded collection endpoints in `AnalyticsSummaryController` to accept `Pageable` and return `ApiResponse<PagedResponse<T>>` with maximum page size ceiling of 100.
6. **Phase 27.13 (Caching):** Configure Spring Cache / Caffeine with tiered TTLs (15 min for national overview, 5 min for district tables) to resolve the High-risk temporary table / filesort overhead observed in Q03–Q06 and Q11.
7. **Phase 27.14 (k-Anonymity):** Implement small-cell count suppression ($N < 5$) for demographic query slices.

---

## 21. Confirmation of Constraints

- [x] No application source code modified.
- [x] No test code modified.
- [x] No database schema or view modified.
- [x] No production database connection attempted or executed.
- [x] No Git commit, push, reset, or checkout performed.
- [x] Full test baseline independently verified (1,022 tests passed).

---

**Final Audit Verdict:** `READY FOR PHASE 27.2`
