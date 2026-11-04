# Phase 25.12 — Cross-Domain Data Consistency Report

**Project:** SIH-26135  
**Repository:** `E:\project` (`backend\`)  
**Java Base Package:** `in.gov.sih.sih26135`  
**Branch:** `main`  
**Production Database Catalog:** `SIH26135` (Untouched — 0 operations)  
**Test Database Catalog:** `SIH26135_TEST` (Hibernate `ddl-auto=validate`)  
**Schema Status:** Frozen — 144 base tables and 21 views  
**Phase Status:** Ready for Independent Review  

---

## 1. Executive Summary

Phase 25.12 conducted an evidence-based cross-domain data consistency review across trainee, enrollment, placement, employment, verification, historical salary, and analytics workflows in the SIH-26135 platform, resolving all blockers identified in independent source review.

The audit and verification established:
1. **Cross-Domain Relationship Integrity & Pre-Mutation Validation:** Trainee, enrollment, placement, employment, followup, and verification entities maintain referential consistency across state transitions. Mismatched parent-child or peer identifiers are rejected fail-closed upfront before any mutation or side-effect occurs.
2. **Authoritative Resolution & Unresolved Parent Fail-Closed:** If a request supplies an entity ID (such as `traineeAssessmentId` or a linked `followupTaskId`), the entity is looked up authoritatively. If unresolvable, the workflow fails closed with `ResourceNotFoundException` prior to assessment, certification, or verification mutations.
3. **Verification Linked-Task & Authoritative Association Reconciliation:**
   - In `initiateVerificationFromSurvey`, a linked task with null `traineeId` is rejected upfront with `TRAINEE_REQUIRED` before creating a verification request, matching the non-null `FollowupTask.trainee_id` constraint.
   - In `recordVerificationVerdictAndSynchronize`, the authoritative verification request (`vReq`) is reconciled with the target employment:
     - `vReq.traineeId()`: Required; null fails closed with `TRAINEE_REQUIRED`, mismatch fails closed with `TRAINEE_EMPLOYMENT_MISMATCH`.
     - `vReq.employerId()`: Because the target employment spell requires an associated employer (`emp.employerId()` NOT NULL), the resolved verification request must have an employer association; null fails closed with `EMPLOYER_REQUIRED`, and mismatch fails closed with `EMPLOYER_EMPLOYMENT_MISMATCH` before any mutation.
     - `vReq.placementRecordId()`: Distinguishes a genuinely optional association from a required or inconsistent association. When `emp.placementId() != null`, the placement association is required; a null or mismatched `vReq.placementRecordId()` fails closed with `PLACEMENT_EMPLOYMENT_MISMATCH`. When `emp.placementId() == null` (direct employment), an extraneous non-null `vReq.placementRecordId()` fails closed with `PLACEMENT_EMPLOYMENT_MISMATCH`, while a null placement is preserved as genuinely optional.
     - On any rejection, regression tests verify that zero verification, evidence, request-status, or employment-status mutations occur.
4. **Legitimate Optional Relationships vs. Invalid Associations:** When a caller supplies an optional association ID (e.g. `enrollmentId`, `placementId`, `followupTaskId`, `employerId`), it is verified against the authoritative persisted relationship. If the authoritative entity has no such relationship (null) or a different relationship, the request is rejected with a domain mismatch error. Where domain rules legitimately allow optional associations (such as a followup task with null pre-associated `surveyId`, or direct employment without placement), the caller's valid input is preserved and accepted.
5. **Read-Only Analytics Boundary:** All 21 analytical database views in `SIH26135_TEST` are mapped to `@Immutable` JPA entities with `@Transactional(readOnly = true)` query methods, preventing their use as workflow write targets. All 21 views were audited and successfully executed with `SELECT COUNT(*)` in the live test catalog.
6. **Accurate Database Evidence & Constraint Distinction:** Database verification strictly distinguishes schema metadata inspection from actual runtime constraint enforcement. While MySQL InnoDB engine-level uniqueness enforcement is demonstrated via reference table fixtures, domain table runtime duplicate rejection remains explicitly disclosed as unverified due to the 0-row baseline and foreign-key fixture constraints.

All 575 backend tests passed with zero failures and zero errors (`575/575`).

---

## 2. Baseline Audit & Schema Invariants

### 2.1 Repository & Database Isolation Baseline
- **Git State:** Baseline established on `main` branch. Zero commits, stashes, resets, or working-tree cleanups executed.
- **Production Catalog Guard:** Production catalog `SIH26135` strictly isolated (zero operations). All tests ran against `SIH26135_TEST` under strict `ddl-auto=validate`.
- **Database Schema:** 144 base tables and 21 views preserved without any DDL or migration modifications.

### 2.2 Analytical Views & Immutability Audit
All 21 views and corresponding JPA entities in `in.gov.sih.sih26135.entity.analytics` were verified:
- `vw_placement_fact`, `vw_employment_fact`, `vw_employment_retention_fact`, `vw_salary_progression_fact`
- `vw_enrollment_outcome_fact`, `vw_followup_fact`, `vw_survey_response_fact`, `vw_skill_gap_fact`
- `vw_employment_verification_fact`, `vw_employment_exit_fact`, `vw_unemployment_fact`
- `vw_district_outcome_summary`, `vw_provider_outcome_summary`, `vw_course_outcome_summary`, `vw_program_outcome_summary`
- `vw_employer_hiring_summary`, `vw_attrition_reason_summary`, `vw_unemployment_reason_summary`, `vw_followup_outcome_summary`
- `vw_skill_gap_summary`, `vw_trainee_outcome_summary`

**Persistence Guarantees:**
- Every view entity class is annotated with Hibernate `@org.hibernate.annotations.Immutable`.
- All corresponding Spring Data repositories and service layers are read-only (`@Transactional(readOnly = true)`).
- Zero workflow write paths target analytical view entities or database views.

---

## 3. Discovered Defects and Applied Corrections

| # | Domain / Workflow | Defect Identified | Correction Applied |
|---|-------------------|-------------------|--------------------|
| 1 | **Training Outcome** (`TrainingOutcomeWorkflowServiceImpl`) | If caller supplied `traineeAssessmentId`, relationship validation was bypassed if assessment was null or skipped; caller could also pass mismatched `certRequest.enrollmentId`. | Looked up assessment via `traineeAssessmentService.getById(id)` upfront; threw `ResourceNotFoundException("TraineeAssessment", "traineeAssessmentId")` if null. Validated `assessment.enrollmentId` matches target enrollment (`ENROLLMENT_ASSESSMENT_MISMATCH`), `assessment.traineeId` matches target trainee (`ENROLLMENT_TRAINEE_MISMATCH`), and `certRequest.enrollmentId` matches target enrollment (`ENROLLMENT_CERTIFICATION_MISMATCH`) before any mutations. |
| 2 | **Placement -> Employment** (`PlacementEmploymentWorkflowServiceImpl`) | Request permitted `empReq.getEmployerId()`, `empReq.getPlacementId()`, or `empReq.getEnrollmentId()` contradicting or absent from loaded `PlacementRecord`, and allowed client to pre-set `initialSalaryRequest.employmentId`. Pre-state update (`updatePlacementRecord`) executed before validating employment attributes. | Added upfront validation failing closed with `EMPLOYER_PLACEMENT_MISMATCH`, `PLACEMENT_RECORD_MISMATCH`, `ENROLLMENT_PLACEMENT_MISMATCH` (when placement has no enrollment or differs), and `EMPLOYMENT_ID_PRESET` before calling `updatePlacementRecord` or `createEmploymentRecord`. |
| 3 | **Employment Lifecycle & Unemployment** (`EmploymentLifecycleWorkflowServiceImpl`) | In `recordExitAndInitiateUnemployment`, `createExitEvent` executed before validating `unempReq.getTraineeId()`, `unempReq.getPrecedingEmploymentId()`, `unempReq.getEnrollmentId()`, and `unempReq.getPlacementId()`. Client could also pre-set `employmentExitEventId` or initial salary `employmentId`. Absent authoritative relationships were silently accepted. | Added upfront validations rejecting mismatched preceding employment ID (`EMPLOYMENT_UNEMPLOYMENT_MISMATCH`), rejecting caller-supplied enrollment/placement when employment has absent or mismatched relationships (`ENROLLMENT_EMPLOYMENT_MISMATCH`, `PLACEMENT_EMPLOYMENT_MISMATCH`), and rejecting pre-set exit/employment IDs (`EXIT_EVENT_ID_PRESET`, `EMPLOYMENT_ID_PRESET`) prior to calling `createExitEvent`. Preserved legitimate optional relationships when omitted. |
| 4 | **Verification Workflow** (`VerificationWorkflowServiceImpl`) | In `initiateVerificationFromSurvey`: linked `FollowupTask` was skipped if `traineeId` was null; `emp.traineeId()` was not null-guarded; caller could supply mismatched task/placement/employer. In `recordVerificationVerdictAndSynchronize`: `vReq.employerId()` was not null-guarded when `vReq` was resolved; `vReq.placementRecordId()` was not properly distinguished as required when employment has placement vs. optional when employment has no placement; missing authoritative associations could bypass validation. | In `initiateVerificationFromSurvey`: enforced non-null trainee on linked `FollowupTask` and `emp` (`TRAINEE_REQUIRED`); resolved linked task fail-closed (`ResourceNotFoundException`); validated placement linkage (`PLACEMENT_TASK_MISMATCH`), employer (`EMPLOYER_EMPLOYMENT_MISMATCH`), and task (`FOLLOWUP_TASK_MISMATCH`). In `recordVerificationVerdictAndSynchronize`: enforced non-null trainee on `emp` and `vReq` (`TRAINEE_REQUIRED`); enforced non-null `vReq.employerId()` (`EMPLOYER_REQUIRED`) and match against `emp.employerId()` (`EMPLOYER_EMPLOYMENT_MISMATCH`); enforced required `vReq.placementRecordId()` when `emp.placementId() != null` and rejected extraneous placement when `emp.placementId() == null` (`PLACEMENT_EMPLOYMENT_MISMATCH`), preserving null when both are absent; rejected contradictory verdict statuses (`VERIFICATION_STATUS_CONFLICT`). Ensured zero mutations occur on rejection. |
| 5 | **Followup Engagement** (`FollowupEngagementWorkflowServiceImpl`) | In `recordFollowupSurveyResponse`, `surveyReq.getFollowupTaskId()`, `surveyReq.getEnrollmentId()`, and `surveyReq.getSurveyId()` could contradict target task. Missing required parent checks and unverified caller associations. | Added upfront validations rejecting mismatched followup task ID (`FOLLOWUP_TASK_MISMATCH`) and mismatched enrollment ID (`ENROLLMENT_TASK_MISMATCH`). If task has assigned `surveyId`, enforced matching (`SURVEY_TASK_MISMATCH`) or defaulted null request survey. If task has no pre-associated survey (`task.getSurveyId() == null`), preserved legitimate optional relationship permitting the respondent-supplied survey. In `recordEngagementAttempt`, rejected mismatched followup task ID (`FOLLOWUP_TASK_MISMATCH`). |

---

## 4. Test Evidence and Verification

### 4.1 Focused Unit Regression Suite
Executed 42 unit regression tests across all 6 domains in `CrossDomainDataConsistencyRegressionTest`:
```
mvn test "-Dtest=CrossDomainDataConsistencyRegressionTest" -f backend/pom.xml
```
**Result:** `Tests run: 42, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS` in 24.126 s).
- Placement -> Employment (5 tests)
- Training Outcome (5 tests)
- Employment Lifecycle & Unemployment (6 tests)
- Verification (19 tests):
  - Linked followup task null trainee rejection (`TRAINEE_REQUIRED`)
  - Employment record null trainee rejection (`TRAINEE_REQUIRED`)
  - `vReq` null trainee rejection (`TRAINEE_REQUIRED`)
  - `vReq` null employer rejection (`EMPLOYER_REQUIRED`)
  - `vReq` employer mismatch rejection (`EMPLOYER_EMPLOYMENT_MISMATCH`)
  - `vReq` null placement when employment has placement rejection (`PLACEMENT_EMPLOYMENT_MISMATCH`)
  - `vReq` placement mismatch when employment has placement rejection (`PLACEMENT_EMPLOYMENT_MISMATCH`)
  - `vReq` extraneous placement when employment has no placement rejection (`PLACEMENT_EMPLOYMENT_MISMATCH`)
  - `vReq` and employment both null placement preserved as genuinely optional
  - Non-mutation assertions verifying `createEmploymentVerification`, `createVerificationEvidence`, `updateVerificationRequest`, and `updateEmploymentRecord` are never invoked on rejection
- Followup Engagement (6 tests)
- Analytics Immutability (1 test)

### 4.2 Database-Backed Integration Suite (`SIH26135_TEST`)
Executed `CrossDomainDatabaseConsistencyIntegrationTest` against the live MySQL `SIH26135_TEST` catalog:
```
mvn test "-Dtest=CrossDomainDatabaseConsistencyIntegrationTest" -f backend/pom.xml
```
**Result:** `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS` in 31.027 s).
1. **Catalog Isolation Guard:** Verified connection strictly targets `SIH26135_TEST` and isolates `SIH26135`.
2. **Schema Metadata Inspection:** Verified presence of unique constraints in `information_schema.TABLE_CONSTRAINTS`: `uk_salary_history_employment_from`, `uk_salary_history_employment_milestone`, `uk_employment_records_placement`, `uk_employment_records_current_per_type`, `uk_emp_verif_requests_open`, `uk_emp_verifications_current`.
3. **Database Engine Uniqueness Enforcement:** Tested that MySQL InnoDB engine rejects duplicate unique key insertions with error 1062 and Spring translates it into `DataIntegrityViolationException` using reference table `ref_skill_gap_status`.
4. **All 21 Analytics Views Query Execution:** Explicitly executed `SELECT COUNT(*)` against every single one of the 21 expected analytical views in `SIH26135_TEST`, proving all 21 views exist, are queryable, and execute valid SQL under MySQL.
5. **Read-Only Analytics JPA Annotations:** Verified Hibernate `@Immutable` annotation across all 21 analytics view entity classes.

### 4.3 Focused Cross-Domain & Workflow Suite
Executed focused workflow test suite covering transitions, consent, identity, database integration, and data consistency:
```
mvn test "-Dtest=CrossDomainDataConsistencyRegressionTest,CrossDomainDatabaseConsistencyIntegrationTest,PlacementToEmploymentTransitionTest,TrainingCompletionSkillGapResolutionTest,TrainingOutcomeWorkflowScopeTest,UnemploymentToEmploymentTransitionTest,VerificationVerdictIdentityEnforcementTest,FollowupEngagementWorkflowConsentTest" -f backend/pom.xml
```
**Result:** `Tests run: 131, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS` in 32.190 s).

### 4.4 Full Backend Test Suite
Executed the entire backend test suite:
```
mvn test -f backend/pom.xml
```
**Result:** `Tests run: 575, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS` in 01:04 min).

### 4.5 Git Hygiene & Working-Tree Inspection
```
git diff --check
```
**Result:** Exited with code 0 (zero whitespace or formatting errors).

`git status --short`:
```
 M backend/src/main/java/in/gov/sih/sih26135/service/impl/EmploymentLifecycleWorkflowServiceImpl.java
 M backend/src/main/java/in/gov/sih/sih26135/service/impl/FollowupEngagementWorkflowServiceImpl.java
 M backend/src/main/java/in/gov/sih/sih26135/service/impl/PlacementEmploymentWorkflowServiceImpl.java
 M backend/src/main/java/in/gov/sih/sih26135/service/impl/TrainingOutcomeWorkflowServiceImpl.java
 M backend/src/main/java/in/gov/sih/sih26135/service/impl/VerificationWorkflowServiceImpl.java
 M backend/src/test/java/in/gov/sih/sih26135/workflow/TrainingOutcomeWorkflowScopeTest.java
?? backend/src/test/java/in/gov/sih/sih26135/workflow/CrossDomainDataConsistencyRegressionTest.java
?? backend/src/test/java/in/gov/sih/sih26135/workflow/CrossDomainDatabaseConsistencyIntegrationTest.java
?? phase-25-12-cross-domain-data-consistency-report.md
```
Zero commits, zero pushes, and zero working tree cleanups executed.

---

## 5. Precise Evidence Boundaries & Unverified Guarantees Disclosure

To prevent overstating verification evidence, the following boundaries and limitations are explicitly disclosed:

1. **Schema Metadata Inspection vs. Runtime Constraint Enforcement:**
   - Inspection of `information_schema.TABLE_CONSTRAINTS` confirmed that the database catalog defines unique keys intended to enforce historical data preservation and current-state uniqueness (`uk_salary_history_employment_from`, `uk_salary_history_employment_milestone`, `uk_employment_records_placement`, `uk_employment_records_current_per_type`, `uk_emp_verif_requests_open`, `uk_emp_verifications_current`).
   - The duplicate-key integration test proved that MySQL InnoDB actively enforces UNIQUE constraints and Spring maps error 1062 to `DataIntegrityViolationException`. However, this was exercised using reference table `ref_skill_gap_status`.
   - **Unverified Guarantee:** Actual runtime duplicate-key rejection on domain tables (`salary_histories`, `employment_records`, `employment_verifications`) remains unverified by database-backed write tests. In `SIH26135_TEST`, parent domain tables (`trainees`, `training_enrollments`, `placement_records`, `employment_records`, `users`) contain 0 rows, and fabricating synthetic multi-table parent records across 20+ cascading foreign keys is strictly prohibited by project constraints.

2. **View Queryability vs. Analytical Output Validation After Workflow Changes:**
   - Executing `SELECT COUNT(*)` on all 21 analytical views proved that each view exists in `SIH26135_TEST`, has syntactically valid SQL definitions under MySQL 8.0, and is queryable without database engine errors.
   - **Unverified Guarantee:** Validating that analytical view outputs correctly aggregate, filter, and reflect state changes after workflow mutations (e.g. verifying `vw_placement_fact` or `vw_salary_progression_fact` output increments after placement or employment transitions) remains unverified in the live database, because underlying domain tables contain 0 rows and return count = 0. Workflow business logic consistency is verified at the unit and workflow service regression layer.

---

## 6. Changed-File Inventory

| Path | Purpose |
|------|---------|
| `backend/src/main/java/in/gov/sih/sih26135/service/impl/TrainingOutcomeWorkflowServiceImpl.java` | Upfront resolution and fail-closed validation for `traineeAssessmentId`, enrollment, and certification. |
| `backend/src/main/java/in/gov/sih/sih26135/service/impl/PlacementEmploymentWorkflowServiceImpl.java` | Upfront validation of employer, placement, enrollment, and pre-set salary employment ID. |
| `backend/src/main/java/in/gov/sih/sih26135/service/impl/EmploymentLifecycleWorkflowServiceImpl.java` | Upfront validation of preceding employment, absent/mismatched enrollment and placement IDs, and pre-set exit/employment IDs. |
| `backend/src/main/java/in/gov/sih/sih26135/service/impl/FollowupEngagementWorkflowServiceImpl.java` | Upfront validation of followup task parent, trainee, enrollment, and pre-associated vs. optional survey ID. |
| `backend/src/main/java/in/gov/sih/sih26135/service/impl/VerificationWorkflowServiceImpl.java` | Required trainee validation on linked followup task (`TRAINEE_REQUIRED`); non-null trainee validation on employment and verification request; non-null employer validation on resolved verification request (`EMPLOYER_REQUIRED`) and employer match (`EMPLOYER_EMPLOYMENT_MISMATCH`); placement association reconciliation distinguishing required placement when employment has placement from optional null when employment has no placement (`PLACEMENT_EMPLOYMENT_MISMATCH`). |
| `backend/src/test/java/in/gov/sih/sih26135/workflow/TrainingOutcomeWorkflowScopeTest.java` | Updated test stubbing for `traineeAssessmentId = 5L` to align with upfront assessment resolution. |
| `backend/src/test/java/in/gov/sih/sih26135/workflow/CrossDomainDataConsistencyRegressionTest.java` | 42 unit regression tests covering fail-closed upfront validation, absent relationship rejection, optional relationship handling, verification linked-task required association enforcement, and verification request employer/placement reconciliation. |
| `backend/src/test/java/in/gov/sih/sih26135/workflow/CrossDomainDatabaseConsistencyIntegrationTest.java` | Database-backed integration tests against `SIH26135_TEST` verifying catalog isolation, schema constraints metadata, reference table duplicate rejection, querying all 21 analytical views, and @Immutable entities. |
| `phase-25-12-cross-domain-data-consistency-report.md` | Workspace documentation of Phase 25.12 cross-domain data consistency audit, fixes, and reconciled evidence boundaries. |

---

## 7. Review Readiness Declaration

All independent review blockers and final corrections have been addressed:
- In `VerificationWorkflowServiceImpl.recordVerificationVerdictAndSynchronize`, `vReq.employerId()` validation is reconciled: because the target employment record requires an associated employer (`emp.employerId()` NOT NULL), a resolved verification request lacking an employer fails closed with `EMPLOYER_REQUIRED`, and an inconsistent employer fails closed with `EMPLOYER_EMPLOYMENT_MISMATCH`.
- `vReq.placementRecordId()` is reconciled: distinguishes a required placement (when `emp.placementId() != null`, null or mismatched `vReq.placementRecordId()` fails closed with `PLACEMENT_EMPLOYMENT_MISMATCH`) from a genuinely optional association (when `emp.placementId() == null`, an extraneous non-null placement fails closed with `PLACEMENT_EMPLOYMENT_MISMATCH`, while null is preserved).
- On all rejections, tests assert that zero verification, evidence, request-status, or employment-status mutations occur.
- 575/575 backend tests pass with zero failures and zero errors.

**READY FOR INDEPENDENT REVIEW**
