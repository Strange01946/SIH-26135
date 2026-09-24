# Backend Cross-Domain Workflow REST API Architecture

**Project:** SIH 26135 — Longitudinal Tracking & Impact Analysis  
**Phase:** 23.16 — Cross-Domain Workflow REST Controllers  
**Base Package:** `in.gov.sih.sih26135.controller.workflow`  
**API Base Path:** `/api/v1/workflows`  
**Database Schema:** `SIH26135` (ddl-auto=validate, strictly frozen)  

---

## 1. Executive Summary & Architectural Principles

Phase 23.16 delivers the REST API controller layer that exposes the cross-domain workflow orchestration services established in Phase 22.17.

While domain-level controllers (`in.gov.sih.sih26135.controller.*`) handle discrete aggregate operations and direct queries, cross-domain workflow controllers expose multi-entity transactional workflows that span multiple domain aggregates.

### Core Architectural Invariants:
1. **HTTP/Protocol Boundary Only**: Controllers perform only HTTP binding, syntactic parameter checks, response enveloping (`ApiResponse<T>`), and status code emission.
2. **Zero In-Controller Business Logic**: Controllers contain zero calculations, state manipulation, or workflow branching. All orchestration is delegated to the respective Phase 22.17 workflow service.
3. **Zero Repository / Entity Access**: Controllers must never inject repositories, `EntityManager`, or domain entities directly.
4. **No Controller-Level Transactions**: Controllers do not use `@Transactional`. Atomicity and transaction boundaries are encapsulated within the application service layer.
5. **Unified Response Envelope**: Every endpoint wraps responses in `ApiResponse<T>`, returning `201 Created` for creations/transitions and `200 OK` for reads.
6. **Centralized Exception Propagation**: Domain and validation exceptions (`BadRequestException`, `ResourceNotFoundException`, `ConflictException`) are thrown directly to `GlobalExceptionHandler`.
7. **Strict Query Parameter Validation**: Endpoints reject any undeclared or unexpected query parameters with `UNSUPPORTED_PARAMETER`. Pure body `POST` endpoints reject any query parameters.

---

## 2. Workflow Controller Inventory

| # | Controller Class | Injected Workflow Service | Base Route | Total Endpoints |
|---|:---|:---|:---|:---:|
| 1 | `TrainingOutcomeWorkflowController` | `TrainingOutcomeWorkflowService` | `/api/v1/workflows/training-outcomes` | 2 (1 POST, 1 GET) |
| 2 | `PlacementEmploymentWorkflowController` | `PlacementEmploymentWorkflowService` | `/api/v1/workflows/placement-employment` | 2 (1 POST, 1 GET) |
| 3 | `EmploymentLifecycleWorkflowController` | `EmploymentLifecycleWorkflowService` | `/api/v1/workflows/employment-lifecycle` | 3 (2 POST, 1 GET) |
| 4 | `VerificationWorkflowController` | `VerificationWorkflowService` | `/api/v1/workflows/verification` | 3 (2 POST, 1 GET) |
| 5 | `SkillGapRemediationWorkflowController` | `SkillGapRemediationWorkflowService` | `/api/v1/workflows/skill-gap-remediation` | 3 (2 POST, 1 GET) |
| 6 | `FollowupEngagementWorkflowController` | `FollowupEngagementWorkflowService` | `/api/v1/workflows/followup-engagement` | 3 (2 POST, 1 GET) |
| **Total** | **6 Controllers** | **6 Workflow Services** | | **14 Endpoints** |

---

## 3. Detailed Controller Specifications

### 3.1 TrainingOutcomeWorkflowController
- **Package**: `in.gov.sih.sih26135.controller.workflow`
- **Base Route**: `/api/v1/workflows/training-outcomes` (alias: `/api/v1/workflows/training-outcome`)
- **Underlying Coordinated Services**: `TrainingEnrollmentService`, `TraineeAssessmentService`, `AssessmentResultService`, `CertificationService`

#### Endpoints:
1. `POST /api/v1/workflows/training-outcomes/record-assessment-and-issue-certificate`
   - **Description**: Records an assessment result and conditionally issues a certificate and updates enrollment status.
   - **Request Body**: `RecordAssessmentAndIssueCertificateRequest`
   - **Response**: `ApiResponse<RecordAssessmentAndIssueCertificateResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Passing flag verification and trainee-enrollment ownership enforced by service.

2. `GET /api/v1/workflows/training-outcomes/summary`
   - **Description**: Retrieves aggregate training outcome summary for a trainee and enrollment.
   - **Query Parameters**:
     - `traineeId` (Long, required)
     - `enrollmentId` (Long, required)
   - **Response**: `ApiResponse<TraineeTrainingOutcomeSummaryResponse>`
   - **HTTP Status**: `200 OK`
   - **Validation**: Strict compound query parameter validation (`traineeId`, `enrollmentId`).

---

### 3.2 PlacementEmploymentWorkflowController
- **Package**: `in.gov.sih.sih26135.controller.workflow`
- **Base Route**: `/api/v1/workflows/placement-employment` (alias: `/api/v1/workflows/placement-employments`)
- **Underlying Coordinated Services**: `PlacementRecordService`, `EmploymentRecordService`, `SalaryHistoryService`

#### Endpoints:
1. `POST /api/v1/workflows/placement-employment/transition-placement-to-employment`
   - **Description**: Transitions confirmed placement record to an active employment record and logs initial salary history.
   - **Request Body**: `PlacementToEmploymentTransitionRequest`
   - **Response**: `ApiResponse<PlacementToEmploymentTransitionResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Placement-trainee matching and joining status synchronization enforced by service.

2. `GET /api/v1/workflows/placement-employment/summary`
   - **Description**: Retrieves aggregate placement and employment history for a trainee.
   - **Query Parameters**:
     - `traineeId` (Long, required)
   - **Response**: `ApiResponse<TraineePlacementEmploymentSummaryResponse>`
   - **HTTP Status**: `200 OK`
   - **Validation**: Strict single query parameter validation (`traineeId`).

---

### 3.3 EmploymentLifecycleWorkflowController
- **Package**: `in.gov.sih.sih26135.controller.workflow`
- **Base Route**: `/api/v1/workflows/employment-lifecycle` (alias: `/api/v1/workflows/employment-lifecycles`)
- **Underlying Coordinated Services**: `EmploymentRecordService`, `EmploymentExitEventService`, `TraineeUnemploymentEventService`, `SalaryHistoryService`

#### Endpoints:
1. `POST /api/v1/workflows/employment-lifecycle/record-exit-and-initiate-unemployment`
   - **Description**: Records employment exit event and initiates trainee unemployment tracking spell.
   - **Request Body**: `EmploymentExitAndUnemploymentRequest`
   - **Response**: `ApiResponse<EmploymentExitAndUnemploymentResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Separation date sequence and trainee consistency enforced by service.

2. `POST /api/v1/workflows/employment-lifecycle/transition-unemployment-to-employment`
   - **Description**: Concludes an unemployment spell and transitions trainee into a new active employment record with initial salary.
   - **Request Body**: `UnemploymentToEmploymentTransitionRequest`
   - **Response**: `ApiResponse<UnemploymentToEmploymentTransitionResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Preceding unemployment binding and date sequence enforced by service.

3. `GET /api/v1/workflows/employment-lifecycle/timeline`
   - **Description**: Retrieves comprehensive longitudinal career timeline (employments, exit events, unemployment spells) for a trainee.
   - **Query Parameters**:
     - `traineeId` (Long, required)
   - **Response**: `ApiResponse<TraineeCareerTimelineResponse>`
   - **HTTP Status**: `200 OK`
   - **Validation**: Strict single query parameter validation (`traineeId`).

---

### 3.4 VerificationWorkflowController
- **Package**: `in.gov.sih.sih26135.controller.workflow`
- **Base Route**: `/api/v1/workflows/verification` (alias: `/api/v1/workflows/verifications`)
- **Underlying Coordinated Services**: `EmploymentRecordService`, `FollowupTaskService`, `SurveyResponseService`, `EmploymentVerificationRequestService`, `EmploymentVerificationAttemptService`, `EmploymentVerificationService`, `EmploymentVerificationEvidenceService`

#### Endpoints:
1. `POST /api/v1/workflows/verification/initiate-from-survey`
   - **Description**: Initiates employment verification request originating from follow-up survey response.
   - **Request Body**: `InitiateVerificationFromSurveyRequest`
   - **Response**: `ApiResponse<EmploymentVerificationRequestResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Survey trainee ownership matching employment trainee enforced by service.

2. `POST /api/v1/workflows/verification/record-verdict`
   - **Description**: Records verification verdict, attaches evidence, and synchronizes status with request and employment records.
   - **Request Body**: `RecordVerificationVerdictRequest`
   - **Response**: `ApiResponse<RecordVerificationVerdictResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Multi-evidence linking and status synchronization enforced by service.

3. `GET /api/v1/workflows/verification/summary`
   - **Description**: Retrieves aggregate employment verification summary (requests, attempts, verifications) for an employment record.
   - **Query Parameters**:
     - `employmentId` (Long, required)
   - **Response**: `ApiResponse<EmploymentVerificationSummaryResponse>`
   - **HTTP Status**: `200 OK`
   - **Validation**: Strict single query parameter validation (`employmentId`).

---

### 3.5 SkillGapRemediationWorkflowController
- **Package**: `in.gov.sih.sih26135.controller.workflow`
- **Base Route**: `/api/v1/workflows/skill-gap-remediation` (alias: `/api/v1/workflows/skill-gap-remediations`)
- **Underlying Coordinated Services**: `SkillGapAssessmentService`, `SkillGapService`, `SkillGapRecommendationService`, `TrainingEnrollmentService`

#### Endpoints:
1. `POST /api/v1/workflows/skill-gap-remediation/record-assessment-with-gaps`
   - **Description**: Records skill gap assessment, identified skill gap items, and recommended interventions in one atomic action.
   - **Request Body**: `RecordSkillGapAssessmentWithGapsRequest`
   - **Response**: `ApiResponse<RecordSkillGapAssessmentWithGapsResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Non-empty gap list and trainee consistency enforced by service.

2. `POST /api/v1/workflows/skill-gap-remediation/enroll-remediation-course`
   - **Description**: Enrolls trainee in recommended remediation course and synchronizes recommendation status.
   - **Request Body**: `SkillGapRemediationEnrollmentRequest`
   - **Response**: `ApiResponse<SkillGapRemediationEnrollmentResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Course ID verification and recommendation status updates enforced by service.

3. `GET /api/v1/workflows/skill-gap-remediation/profile`
   - **Description**: Retrieves comprehensive skill gap profile (assessments, current gaps, recommendations) for a trainee.
   - **Query Parameters**:
     - `traineeId` (Long, required)
   - **Response**: `ApiResponse<TraineeSkillGapProfileResponse>`
   - **HTTP Status**: `200 OK`
   - **Validation**: Strict single query parameter validation (`traineeId`).

---

### 3.6 FollowupEngagementWorkflowController
- **Package**: `in.gov.sih.sih26135.controller.workflow`
- **Base Route**: `/api/v1/workflows/followup-engagement` (alias: `/api/v1/workflows/followup-engagements`)
- **Underlying Coordinated Services**: `FollowupTaskService`, `SurveyResponseService`, `CommunicationLogService`

#### Endpoints:
1. `POST /api/v1/workflows/followup-engagement/record-survey-response`
   - **Description**: Records follow-up survey response and synchronizes associated task status and outcome.
   - **Request Body**: `RecordFollowupSurveyResponseRequest`
   - **Response**: `ApiResponse<RecordFollowupSurveyResponseResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Task trainee matching and task completion synchronization enforced by service.

2. `POST /api/v1/workflows/followup-engagement/record-engagement-attempt`
   - **Description**: Records communication attempt log and updates task contact channel and status.
   - **Request Body**: `RecordEngagementAttemptRequest`
   - **Response**: `ApiResponse<RecordEngagementAttemptResponse>`
   - **HTTP Status**: `201 Created`
   - **Validation**: No query parameters permitted. Task trainee matching and channel assignment enforced by service.

3. `GET /api/v1/workflows/followup-engagement/history`
   - **Description**: Retrieves comprehensive longitudinal engagement history (tasks and survey responses) for a trainee.
   - **Query Parameters**:
     - `traineeId` (Long, required)
   - **Response**: `ApiResponse<TraineeEngagementHistoryResponse>`
   - **HTTP Status**: `200 OK`
   - **Validation**: Strict single query parameter validation (`traineeId`).

---

## 4. Query Parameter Validation Conventions

All workflow controllers enforce strict query parameter checking:
- **Zero-parameter POST requests**: If query parameters are present on requests expecting only JSON payloads, a `BadRequestException("Unsupported query parameters: ...", "UNSUPPORTED_PARAMETER")` is thrown.
- **Single-parameter GET requests**: Validated with `validateOnlyQueryParameter(request, "paramName")`.
- **Compound-parameter GET requests**: Validated with `validateCompoundQueryParameters(request, Set.of("param1", "param2"))`.
- Missing required query parameters are automatically caught by Spring MVC with HTTP 400.

---

## 5. Summary of Phase 23.16 Deliverables

- **Controllers Created**: 6
- **Endpoints Implemented**: 14 (8 POST, 6 GET)
- **Repository Injections in Controllers**: 0 (Direct service delegation only)
- **Database Schema Changes**: 0
- **Entity Modifications**: 0
- **Test File Modifications**: 0
