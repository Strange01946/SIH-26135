# PHASE 25.7 — SKILL-GAP REMEDIATION WORKFLOW (CORRECTED SPECIFICATION)

## 1. Objective

Implement the Skill-Gap Remediation Workflow in the existing SIH-26135 project.

The workflow connects existing skill-gap records with relevant training recommendations, allows authorized users to accept/reject recommendations and enroll in remediation, transitions the linked skill-gap status from `OPEN` to `IN_REMEDIATION`, and integrates with the existing training-completion logic to update the associated skill-gap status to `RESOLVED` when remediation training is successfully completed.

This is an implementation phase in an existing production-oriented application, not a new standalone module. Reuse existing entities, relationships, services, controllers, DTOs, repositories, security rules, validation patterns, and response conventions wherever applicable.

## 2. Mandatory Pre-Implementation Invariants

1. **Schema Freezing (`ddl-auto=validate`):** The database schema is strictly frozen across all 144 base tables and 21 views. Zero DDL alterations, migrations, or new columns.
2. **Recommendation State Representation:** Do NOT add new columns or tables for recommendation status. The persisted schema on `skill_gap_recommendations` already models:
   - **Accept:** `is_accepted_flag = true`, `accepted_on = LocalDate.now()`, optional remarks.
   - **Reject:** `is_accepted_flag = false`, `accepted_on = null`, mandatory non-blank `remarks` explaining rejection.
   - **Enrollment / Completion:** Manifested by linked `training_enrollments` record and its completion flags.
3. **Skill-Gap Status Lifecycle (`ref_skill_gap_status`):**
   - `OPEN` (1): Initial state when gap is identified.
   - `IN_REMEDIATION` (2): Trainee enrolled in recommended course.
   - `RESOLVED` (3): Remediation course completed with passing assessment result (`is_current = false`, `resolved_on = completionDate`).
   - `CLOSED_UNRESOLVED` (4) / `SUPERSEDED` (5): Gap closed without resolution or superseded by a newer assessment.
4. **Integration with Existing Training Outcome Workflow:**
   - `TrainingOutcomeWorkflowServiceImpl.java` already implements `resolveInRemediationGapsForCompletion()` triggered by `isCompletedFlag=true` AND `isPassFlag=true`.
   - Phase 25.7 must ensure that `SkillGapRemediationWorkflowServiceImpl.enrollTraineeInRemediationCourse()` validates that the recommendation is accepted (`is_accepted_flag == true`), validates that the course teaches the required skill via `course_skills` (`CourseSkillRepository`), and transitions the linked `SkillGap` status from `OPEN` to `IN_REMEDIATION`.
5. **Phase 25.6 Security Allowlist Invariant:**
   - Any new request DTOs accepted by controller `@RequestBody` methods must be registered in `GlobalExceptionHandler.TRUSTED_REQUEST_DTO_CLASSES`.

## 3. Required Business Workflow

### A. Assessment and Skill-Gap Traceability
- Reuse existing assessment results and skill-gap records.
- Ensure remediation recommendations associate with the correct skill gap and relevant trainee.
- Validate the relationship between trainee, skill, assessment, skill-gap record, and recommended course.
- Prevent records belonging to different trainees or unrelated skills from being linked together (`SKILL_GAP_TRAINEE_MISMATCH`, `SKILL_GAP_ASSESSMENT_MISMATCH`).
- Do not create duplicate assessment or skill-gap records merely to support remediation.
- Preserve original assessment results and the history of how the skill gap was identified.

### B. Remediation Recommendation
- Reuse existing course, skill, training-provider, and eligibility information.
- Validate that the recommended course actually teaches the target skill identified in the gap using `CourseSkillRepository.findByCourseId(courseId)`.
- Reject invalid, nonexistent, incompatible, or unauthorized associations (`COURSE_SKILL_MISMATCH`, `RESOURCE_NOT_FOUND`).
- Maintain existing recommendation creation in `SkillGapRemediationWorkflowService.recordAssessmentWithGapsAndRecommendations()`.

### C. Recommendation Decision (Accept / Reject)
- Implement recommendation decision API / service methods:
  - `acceptRecommendation(Long recommendationId, TraineeRecommendationDecisionRequest request)`:
    - Verifies recommendation exists.
    - Asserts recommendation is currently pending (not already accepted or rejected).
    - Sets `isAcceptedFlag = true`, `acceptedOn = LocalDate.now()`, updates `remarks`.
  - `rejectRecommendation(Long recommendationId, TraineeRecommendationDecisionRequest request)`:
    - Verifies recommendation exists.
    - Requires non-blank rejection reason in `remarks`.
    - Sets `isAcceptedFlag = false`, `acceptedOn = null`.
    - A rejected recommendation CANNOT subsequently be enrolled.

### D. Remediation Enrollment
- In `SkillGapRemediationWorkflowService.enrollTraineeInRemediationCourse()`:
  - Verify trainee, skill-gap record, recommendation, and course form a valid and consistent relationship.
  - Assert recommendation has been accepted (`is_accepted_flag == true`). Reject enrollment on unaccepted/rejected recommendations with `RECOMMENDATION_NOT_ACCEPTED`.
  - Validate that the course in the enrollment request matches `recommended_course_id`.
  - Validate that the course covers the gap's skill via `course_skills`.
  - Create and persist `TrainingEnrollment` via `TrainingEnrollmentService`.
  - Transactionally update the linked `SkillGap` status from `OPEN` to `IN_REMEDIATION` (status ID 2).
  - Prevent duplicate active remediation enrollments for a gap already in remediation (`DUPLICATE_REMEDIATION_ENROLLMENT`).

### E. Skill-Gap Status Update & Completion Integration
- When training is completed:
  - Verified by existing `TrainingOutcomeWorkflowServiceImpl`: when `isCompletedFlag == true` AND `isPassFlag == true`, matching `IN_REMEDIATION` gaps for the trainee are updated to `RESOLVED` with `resolved_on` and `is_current = false`.
  - If training fails (`isPassFlag == false`) or trainee drops out, gap remains `IN_REMEDIATION` or returns to `OPEN` (never marked `RESOLVED`).

### F. History and Audit Preservation
- Preserve historical assessments and observations as immutable records.
- Past skill gaps marked `is_current = false` remain preserved in `skill_gaps` for longitudinal auditing.
- Preserved audit fields: `created_at`, `updated_at`, `created_by_user_id`.

## 4. API and Security Requirements
- Expose recommendation decision and profile endpoints in `SkillGapRemediationWorkflowController`:
  - `POST /api/v1/workflows/skill-gap-remediation/recommendations/{id}/accept`
  - `POST /api/v1/workflows/skill-gap-remediation/recommendations/{id}/reject`
  - Existing `POST /api/v1/workflows/skill-gap-remediation/enroll-remediation-course`
  - Existing `POST /api/v1/workflows/skill-gap-remediation/record-assessment-with-gaps`
  - Existing `GET /api/v1/workflows/skill-gap-remediation/profile?traineeId={id}`
- Secure with `@PreAuthorize("hasAnyAuthority('assessment.manage', 'enrollment.manage')")`.
- Register all new request DTOs in `GlobalExceptionHandler.TRUSTED_REQUEST_DTO_CLASSES`.

## 5. Testing Requirements
- Unit tests for recommendation state transitions (accept, reject, invalid re-accept, rejected enrollment guard).
- Integration tests for `SkillGapRemediationWorkflowController` and `SkillGapRemediationWorkflowService`.
- Multi-entity relationship validation tests (course-skill relevance, trainee ownership).
- End-to-end full test suite pass via plain `mvn test` (0 failures, 0 errors, 0 skipped).
