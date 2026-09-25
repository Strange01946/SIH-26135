# Backend Authorization & Access Control Architecture (Phase 24.7)

## 1. Authorization Architecture Overview

The SIH 26135 backend implements a stateless, declarative, server-side authorization architecture enforced at the Spring MVC controller method boundary via Spring Security's Method Security infrastructure.

```
                              ┌──────────────────────────────────────────┐
                              │           Incoming HTTP Request          │
                              └────────────────────┬─────────────────────┘
                                                   │
                                                   ▼
                              ┌──────────────────────────────────────────┐
                              │       SecurityFilterChain (Phase 24.4)   │
                              │  - Permit public: /auth/login, /refresh  │
                              │  - Authenticate Bearer JWT token         │
                              └────────────────────┬─────────────────────┘
                                                   │
                         [SecurityContextHolder: UserPrincipal + GrantedAuthorities]
                                                   │
                                                   ▼
                              ┌──────────────────────────────────────────┐
                              │       @EnableMethodSecurity Proxy        │
                              │        PreAuthorizeAuthorizationManager  │
                              └────────────┬─────────────────────────────┘
                                           │
                         ┌─────────────────┴─────────────────┐
                         ▼                                   ▼
                [Authority Granted]                 [Authority Missing]
                         │                                   │
                         ▼                                   ▼
              ┌─────────────────────┐             ┌─────────────────────┐
              │ Controller Method   │             │ AccessDeniedException│
              │ Business Execution  │             └──────────┬──────────┘
              └─────────────────────┘                        │
                                                             ▼
                                                  ┌─────────────────────┐
                                                  │GlobalExceptionHandler│
                                                  │(Rethrows to Security)│
                                                  └──────────┬──────────┘
                                                             │
                                                             ▼
                                                  ┌─────────────────────┐
                                                  │RestAccessDenied     │
                                                  │Handler (Phase 24.6) │
                                                  │-> HTTP 403 Forbidden│
                                                  └─────────────────────┘
```

Authorization operates on the collection of `GrantedAuthority` objects populated during JWT authentication in `JwtAuthenticationFilter`. Each authenticated `UserPrincipal` holds effective authorities derived from the user's active role assignments and role-to-permission mappings stored in the MySQL database schema (`users`, `user_roles`, `roles`, `role_permissions`, `permissions`).

---

## 2. Role Authority Format

Roles in the database (`roles.role_code`) are converted into Spring Security role authorities following standard Spring Security conventions:
- **Prefix**: `ROLE_`
- **Case**: Lowercase role code as defined in schema
- **Examples**:
  - `ROLE_super_admin`
  - `ROLE_government_admin`
  - `ROLE_state_director`
  - `ROLE_nsdc_admin`
  - `ROLE_ssc_admin`
  - `ROLE_training_provider_admin`
  - `ROLE_training_center_admin`
  - `ROLE_assessment_agency_admin`
  - `ROLE_assessor`
  - `ROLE_employer_admin`
  - `ROLE_recruiter`
  - `ROLE_verification_agency`
  - `ROLE_auditor`
  - `ROLE_trainee`
  - `ROLE_call_center_executive`
  - `ROLE_helpdesk_agent`

When role-level checks are evaluated, expressions use `hasRole('super_admin')` or `hasAuthority('ROLE_super_admin')`.

---

## 3. Permission Authority Format

Permissions in the database (`permissions.permission_code`) represent granular functional capabilities.
- **Prefix**: *None* (plain string matching database `permission_code`)
- **Format**: `<module>.<action>` in lowercase dot-notation
- **The 17 Canonical Database Permissions**:
  1. `system.manage`: System configuration and administrative utilities.
  2. `user.manage`: Create, update, and manage users, roles, and security assignments.
  3. `trainee.read`: Query and view trainee profiles and demographics.
  4. `trainee.write`: Create, modify, and delete trainee records.
  5. `consent.manage`: Record, verify, and revoke trainee consent agreements.
  6. `program.manage`: Maintain government schemes and training programs.
  7. `provider.manage`: Register and administer training providers and accreditations.
  8. `center.manage`: Maintain training centers and facility records.
  9. `course.manage`: Manage courses, curricula, and training batches.
  10. `enrollment.manage`: Enroll trainees, track progress, and log attendance records.
  11. `assessment.manage`: Record assessments, test results, certifications, and skill gaps.
  12. `placement.manage`: Administer employers, job postings, applications, and placement offers.
  13. `employment.manage`: Manage employment histories, salary progressions, exits, and unemployment records.
  14. `verification.manage`: Coordinate and execute third-party employment verifications and evidence reviews.
  15. `survey.manage`: Conduct longitudinal follow-up campaigns, tasks, questionnaires, and responses.
  16. `analytics.read`: Query analytical fact views, summary cubes, and KPI aggregations.
  17. `audit.read`: Inspect system event audit trails, change logs, and data quality issues.

All 17 permissions are evaluated via `hasAuthority('<permission_code>')`.

---

## 4. Method-Level Authorization Approach

1. **Configuration**:
   - `SecurityConfig` is annotated with `@EnableMethodSecurity(prePostEnabled = true)`.
   - Spring Security creates runtime AOP proxies wrapping all Spring MVC `@RestController` beans.
   - Authorization evaluation occurs immediately before method execution (`@PreAuthorize`).

2. **Declaration Strategy**:
   - **Class-Level `@PreAuthorize`**: Applied to controllers where all endpoints share uniform permission requirements (44 standard domain controllers, 2 analytics controllers, and 6 workflow controllers).
   - **Method-Level `@PreAuthorize`**: Applied to controllers requiring differential read vs. write authorization (specifically `TraineeController`, which separates `trainee.read` for `GET` operations and `trainee.write` for `POST`/`PUT`/`DELETE` mutations).

3. **Global Exception Propagation**:
   - `GlobalExceptionHandler` explicitly intercepts Spring Security's `AccessDeniedException` and `AuthenticationException` and **rethrows** them.
   - Rethrowing guarantees that Spring Security's filter-chain exception handling infrastructure (`RestAccessDeniedHandler` and `RestAuthenticationEntryPoint`) processes security rejections and returns standardized, sanitized JSON error responses without masking them as generic HTTP 500 errors.

---

## 5. Complete Controller Authorization Mapping (53 Protected Controllers)

| # | Controller Class | Base Route | Required Authority / Expression |
|---|---|---|---|
| 1 | `UserController` | `/api/v1/users` | `hasAuthority('user.manage')` |
| 2 | `RoleController` | `/api/v1/roles` | `hasAuthority('user.manage')` |
| 3 | `PermissionController` | `/api/v1/permissions` | `hasAuthority('user.manage')` |
| 4 | `TraineeController` (Reads) | `/api/v1/trainees` (`GET`) | `hasAuthority('trainee.read')` |
| 5 | `TraineeController` (Writes) | `/api/v1/trainees` (`POST`, `PUT`, `DELETE`) | `hasAuthority('trainee.write')` |
| 6 | `ConsentController` | `/api/v1/consents` | `hasAuthority('consent.manage')` |
| 7 | `ProgramController` | `/api/v1/programs` | `hasAuthority('program.manage')` |
| 8 | `SchemeController` | `/api/v1/schemes` | `hasAuthority('program.manage')` |
| 9 | `TrainingProviderController` | `/api/v1/training-providers` | `hasAuthority('provider.manage')` |
| 10 | `TrainingCenterController` | `/api/v1/training-centers` | `hasAuthority('center.manage')` |
| 11 | `CourseController` | `/api/v1/courses` | `hasAuthority('course.manage')` |
| 12 | `TrainingBatchController` | `/api/v1/training-batches` | `hasAuthority('course.manage')` |
| 13 | `TrainingEnrollmentController` | `/api/v1/training-enrollments` | `hasAuthority('enrollment.manage')` |
| 14 | `AttendanceRecordController` | `/api/v1/attendance-records` | `hasAuthority('enrollment.manage')` |
| 15 | `AssessmentController` | `/api/v1/assessments` | `hasAuthority('assessment.manage')` |
| 16 | `AssessmentResultController` | `/api/v1/assessment-results` | `hasAuthority('assessment.manage')` |
| 17 | `TraineeAssessmentController` | `/api/v1/trainee-assessments` | `hasAuthority('assessment.manage')` |
| 18 | `CertificationController` | `/api/v1/certifications` | `hasAuthority('assessment.manage')` |
| 19 | `SkillGapController` | `/api/v1/skill-gaps` | `hasAuthority('assessment.manage')` |
| 20 | `SkillGapAssessmentController` | `/api/v1/skill-gap-assessments` | `hasAuthority('assessment.manage')` |
| 21 | `SkillGapObservationController` | `/api/v1/skill-gap-observations` | `hasAuthority('assessment.manage')` |
| 22 | `SkillGapRecommendationController` | `/api/v1/skill-gap-recommendations` | `hasAuthority('assessment.manage')` |
| 23 | `EmployerController` | `/api/v1/employers` | `hasAuthority('placement.manage')` |
| 24 | `JobPostingController` | `/api/v1/job-postings` | `hasAuthority('placement.manage')` |
| 25 | `JobApplicationController` | `/api/v1/job-applications` | `hasAuthority('placement.manage')` |
| 26 | `PlacementRecordController` | `/api/v1/placement-records` | `hasAuthority('placement.manage')` |
| 27 | `EmploymentRecordController` | `/api/v1/employment-records` | `hasAuthority('employment.manage')` |
| 28 | `SalaryHistoryController` | `/api/v1/salary-histories` | `hasAuthority('employment.manage')` |
| 29 | `EmploymentExitEventController` | `/api/v1/employment-exit-events` | `hasAuthority('employment.manage')` |
| 30 | `TraineeUnemploymentEventController` | `/api/v1/trainee-unemployment-events` | `hasAuthority('employment.manage')` |
| 31 | `EmploymentVerificationController` | `/api/v1/employment-verifications` | `hasAuthority('verification.manage')` |
| 32 | `EmploymentVerificationRequestController` | `/api/v1/employment-verification-requests` | `hasAuthority('verification.manage')` |
| 33 | `EmploymentVerificationAttemptController` | `/api/v1/employment-verification-attempts` | `hasAuthority('verification.manage')` |
| 34 | `EmploymentVerificationEvidenceController` | `/api/v1/employment-verification-evidences` | `hasAuthority('verification.manage')` |
| 35 | `FollowupCampaignController` | `/api/v1/followup-campaigns` | `hasAuthority('survey.manage')` |
| 36 | `FollowupTaskController` | `/api/v1/followup-tasks` | `hasAuthority('survey.manage')` |
| 37 | `SurveyQuestionController` | `/api/v1/survey-questions` | `hasAuthority('survey.manage')` |
| 38 | `SurveyResponseController` | `/api/v1/survey-responses` | `hasAuthority('survey.manage')` |
| 39 | `SurveyResponseAnswerController` | `/api/v1/survey-response-answers` | `hasAuthority('survey.manage')` |
| 40 | `AuditLogController` | `/api/v1/audit-logs` | `hasAnyAuthority('audit.read', 'system.manage')` |
| 41 | `SystemEventLogController` | `/api/v1/system-event-logs` | `hasAnyAuthority('audit.read', 'system.manage')` |
| 42 | `DataQualityRuleController` | `/api/v1/data-quality-rules` | `hasAnyAuthority('audit.read', 'system.manage')` |
| 43 | `DataQualityIssueController` | `/api/v1/data-quality-issues` | `hasAnyAuthority('audit.read', 'system.manage')` |
| 44 | `DataQualityIssueEventController` | `/api/v1/data-quality-issue-events` | `hasAnyAuthority('audit.read', 'system.manage')` |
| 45 | `ImportBatchController` | `/api/v1/import-batches` | `hasAnyAuthority('system.manage', 'audit.read')` |
| 46 | `ImportRecordController` | `/api/v1/import-records` | `hasAnyAuthority('system.manage', 'audit.read')` |
| 47 | `AnalyticsFactController` | `/api/v1/analytics/facts` | `hasAuthority('analytics.read')` |
| 48 | `AnalyticsSummaryController` | `/api/v1/analytics/summaries` | `hasAuthority('analytics.read')` |
| 49 | `EmploymentLifecycleWorkflowController` | `/api/v1/workflows/employment-lifecycle` | `hasAuthority('employment.manage')` |
| 50 | `FollowupEngagementWorkflowController` | `/api/v1/workflows/followup-engagement` | `hasAuthority('survey.manage')` |
| 51 | `PlacementEmploymentWorkflowController` | `/api/v1/workflows/placement-employment` | `hasAnyAuthority('placement.manage', 'employment.manage')` |
| 52 | `SkillGapRemediationWorkflowController` | `/api/v1/workflows/skill-gap-remediation` | `hasAnyAuthority('assessment.manage', 'enrollment.manage')` |
| 53 | `TrainingOutcomeWorkflowController` | `/api/v1/workflows/training-outcome` | `hasAnyAuthority('enrollment.manage', 'assessment.manage')` |
| 54 | `VerificationWorkflowController` | `/api/v1/workflows/verification` | `hasAuthority('verification.manage')` |

---

## 6. Public Endpoints & Unrestricted Authenticated Endpoints

The following routes are explicitly exempt from domain authorization:
1. **Public Authentication Endpoints** (Configured via `SecurityFilterChain.permitAll()`):
   - `POST /api/v1/auth/login`: Public endpoint for credential authentication.
   - `POST /api/v1/auth/refresh`: Public endpoint for token renewal via refresh token.
2. **Infrastructure Endpoints** (Configured via `SecurityFilterChain.permitAll()`):
   - `GET /actuator/health`: System liveness and health probe.
   - `/error`: Spring Boot standard internal error dispatch.
3. **General Authenticated Endpoints** (Requires valid JWT authentication, but no specific business permission):
   - `GET /api/v1/auth/me`: Self-identity profile inspection. Any authenticated principal may inspect their own claims, active role, and granted authorities.

---

## 7. 401 Unauthorized vs. 403 Forbidden Security Flow

The system maintains a strict, unambiguous separation between authentication failures and authorization denials:

| Condition | Cause | Handler Component | HTTP Status | Response Code | Response Message |
|---|---|---|---|---|---|
| **Missing or Invalid Token** | Request without Bearer header, expired token, malformed signature, or non-existent user | `RestAuthenticationEntryPoint` | `401 Unauthorized` | `UNAUTHORIZED` | "Authentication is required to access this resource" |
| **Insufficient Authority** | Authenticated user lacks required permission or role authority | `RestAccessDeniedHandler` | `403 Forbidden` | `FORBIDDEN` | "Access denied" |

### Information Leakage Defense
Neither the 401 nor the 403 handler reveals internal authorization details, stack traces, missing permission names, or database metadata to clients.

---

## 8. Analytics Authorization Model

Analytical endpoints expose aggregated outcome metrics and relational fact projections:
- **Base Paths**: `/api/v1/analytics/facts` and `/api/v1/analytics/summaries`
- **Required Authority**: `analytics.read`
- **Roles with Access**:
  - `super_admin`: Granted all permissions (including `analytics.read`).
  - `government_admin`: Granted permissions 2 through 17 (including `analytics.read`).
  - `state_director`, `nsdc_admin`, `ssc_admin`: Granted `analytics.read`.
  - `auditor`: Granted `analytics.read`.
- **Restricted Access**: Operational roles without analytical reporting requirements (e.g. `trainee`, `call_center_executive`, `helpdesk_agent`) lack `analytics.read` and receive `403 Forbidden`.

---

## 9. Workflow Authorization Model

Cross-domain orchestration workflows coordinate multi-entity operations spanning across functional domains:
- **`EmploymentLifecycleWorkflowController`**: Governed by `employment.manage`. Enables career transitions, exit event recording, and re-employment tracking.
- **`FollowupEngagementWorkflowController`**: Governed by `survey.manage`. Coordinates post-placement longitudinal tracking and response capture.
- **`VerificationWorkflowController`**: Governed by `verification.manage`. Handles third-party verification triggers and audit verdicts.
- **Multi-Domain Workflow Disjunction (`hasAnyAuthority`)**:
  - **`PlacementEmploymentWorkflowController`**: Authorized for operators with either `placement.manage` OR `employment.manage`.
  - **`SkillGapRemediationWorkflowController`**: Authorized for operators with either `assessment.manage` OR `enrollment.manage`.
  - **`TrainingOutcomeWorkflowController`**: Authorized for operators with either `enrollment.manage` OR `assessment.manage`.

This disjunctive authorization allows specialized domain officers (e.g. training managers vs. placement coordinators) to invoke collaborative cross-domain lifecycles without requiring excessive super-user privileges.

---

## 10. `X-Actor-User-Id` Header Audit & Limitations

Throughout the application and REST controller layer, specific mutation endpoints accept an optional HTTP header:
`X-Actor-User-Id: <userId>`

### Security Clarification
1. **Not an Authentication Mechanism**: The `X-Actor-User-Id` header is **never** used to establish or override the authenticated identity. Authentication is derived strictly from the cryptographic JWT `sub` and `userId` claims verified by `JwtAuthenticationFilter`.
2. **Not an Authorization Bypass**: Supplying an `X-Actor-User-Id` does not grant or elevate permissions. Method security (`@PreAuthorize`) executes before the controller method is entered. If the JWT caller lacks the required authority, Spring Security rejects the request with HTTP 403, and the controller is never reached.
3. **Audit Trail Purpose Only**: The header serves exclusively as an audit trail metadata attribution parameter for recording which internal actor triggered a batch, workflow, or record update in audit logs and system event entries.

---

## 11. Object-Level / Record-Level Authorization Status

- **Status**: Explicitly **Deferred** to future functional enhancements.
- **Scope of Phase 24.7**: Phase 24.7 strictly implements **Endpoint / Method-Level Role & Permission Authorization**.
- **Deferred Considerations**:
  - Row-level access control (e.g. training center administrators restricted strictly to trainees registered in their specific center).
  - Employer-specific isolation (e.g. recruiters restricted strictly to job postings owned by their employer organization).
  - Self-service restrictions (e.g. trainees restricted to modifying solely their own profile or viewing their own assessment results).
- These multi-tenant and ownership-based predicates will be evaluated at the service and data repository layer in subsequent operational milestones.

---

## 12. Security Assumptions & Invariants

1. **Database Immutability**: No database tables, columns, indexes, views, or permission rows were added, deleted, or altered.
2. **Deterministic Authorities**: All effective authorities checked by `@PreAuthorize` map 1:1 to the 17 permissions in `database/phase-02-users-security.sql`.
3. **Stateless Enforcement**: Every HTTP request is evaluated independently without HTTP session state.
4. **Fail-Closed Default**: With `@EnableMethodSecurity`, any method protected by `@PreAuthorize` fails closed by denying access if no valid authentication or required authority is present in `SecurityContextHolder`.

---

## 13. Explicitly Deferred Security Phases

In accordance with the backend roadmap, the following security features are intentionally excluded from Phase 24.7:
- **Phase 24.8**: Account Lockout, Brute-Force Rate Limiting, Failed Login Counters (`failed_login_count`, `locked_until`), and Security Audit Event Recording.
- **Phase 24.9**: Production CORS Configuration, Content Security Policy (CSP), Strict Transport Security (HSTS), and HTTP Security Headers.
