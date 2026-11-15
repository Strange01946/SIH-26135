# Phase 29.2 — Cross-Workflow Orchestration & End-to-End Domain Transaction Choreography Report

**Milestone:** Phase 29.2 (Cross-Workflow Orchestration & End-to-End Domain Transaction Choreography) \
**Project Root:** `E:\project` \
**Backend Directory:** `E:\project\backend` \
**Execution Timestamp:** 2026-11-15T16:05:00+05:30 \
**Target Test Database:** `SIH26135_TEST` (`spring.jpa.hibernate.ddl-auto=validate`) \
**Final Verdict:** **READY FOR INDEPENDENT REVIEW**

---

## 1. Executive Summary & Verification Verdict

### 1.1 Objective
The objective of Phase 29.2 is to connect the existing, independently proven Phase 25 workflow services into a coherent, secure, transactionally correct candidate lifecycle:
1. **Stage A (Registration & Enrollment):** Trainee registration $\rightarrow$ consent validation (`DATA_PROCESSING`) $\rightarrow$ batch assignment and training enrollment with batch/provider consistency.
2. **Stage B (Training & Assessment):** Attendance progression $\rightarrow$ assessment scoring $\rightarrow$ eligible certification with pass/fail verification.
3. **Stage C (Placement & Employment):** Job application $\rightarrow$ placement offer / joining transition $\rightarrow$ active employment record with salary tracking.
4. **Stage D (Employment Verification):** Verification initiation $\rightarrow$ evidence registration $\rightarrow$ verdict / attestation (with official vs. self-reported distinctions and data quality alert on failure).
5. **Stage E (Retention & Employment Exit):** Follow-up engagement milestones (3, 6, 12 months) $\rightarrow$ status updates $\rightarrow$ atomic employment exit and unemployment recording.
6. **Stage F (Remediation & Skill-Gap Feedback):** Employment exit or verification failure $\rightarrow$ skill-gap evaluation $\rightarrow$ actionable recommendation workflow or safe pending evaluation handoff.
7. **Read-Only Unified Lifecycle Summary:** Aggregation of the complete candidate journey across consents, enrollments, placement, career progression, and skill-gap profile.

### 1.2 Verification Verdict
**READY FOR INDEPENDENT REVIEW**

All 6 lifecycle stages, discrete transactional boundaries, domain event dispatches, `@TransactionalEventListener` execution, security scope hardening, consent enforcement, audit interception, and cache invalidation mechanics have been implemented and rigorously verified:
- **Focused Orchestration Suite:** `CandidateLifecycleOrchestrationIntegrationTest` executed **22 tests, 0 failures, 0 errors, 0 skipped** (duration: 28.12s).
- **Full Backend Regression Suite:** `mvn test -f backend/pom.xml` executed **2,047 tests, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS`, total time: 06:07 min).
- **Architectural Invariants:**
  - Exactly **137 Spring Data JPA repository interfaces** (verified by `OperationalErrorCodeContractTest`).
  - Exactly **2 Spring Data `@Query` annotations** in `src/main` (`UserRepository.findActiveByUsername` and `AuditLogRepository.findRecentEntityActivity`).
  - Exactly **64 REST controllers** (frozen — zero redundant orchestration controllers added).
  - Exactly **668 contract-audited REST endpoints** uniformly returning `ApiResponse<T>`.
  - Database schema frozen under `spring.jpa.hibernate.ddl-auto=validate`.
  - Target test database strictly isolated to `SIH26135_TEST` (zero interaction with `SIH26135`).
  - Clean working tree with zero Git mutations (`git add`, `git commit`, `git push` prohibited).

---

## 2. Implementation Summary: Implemented vs. Reused Architecture

Phase 29.2 strictly focuses on orchestration and choreography rather than rebuilding existing domain business logic:

| Architectural Component | Strategy | Description / Class Reference |
|---|---|---|
| **Candidate Lifecycle Orchestrator** | **Implemented** | `CandidateLifecycleOrchestratorService` & `CandidateLifecycleOrchestratorServiceImpl` coordinating discrete lifecycle stages A through F and providing `getCandidateLifecycleSummary`. |
| **Transaction Event Listener** | **Implemented** | `CandidateLifecycleEventListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)`) for decoupled asynchronous logging, metrics, and side-effect isolation. |
| **Orchestration Request/Response DTOs** | **Implemented** | `TraineeEnrollmentOrchestrationRequest`, `RemediationTriggerRequest`, `TraineeEnrollmentOrchestrationResponse`, `RemediationTriggerResponse`, `CandidateLifecycleSummaryResponse`. |
| **Stage A Workflows** | **Reused** | `TraineeService`, `TraineeConsentService`, `TrainingEnrollmentService`, `TrainingBatchService`. |
| **Stage B Workflows** | **Reused** | `TrainingOutcomeWorkflowService`, `AttendanceRecordService`, `CertificationService`. |
| **Stage C Workflows** | **Reused** | `PlacementEmploymentWorkflowService`, `PlacementRecordService`, `EmploymentRecordService`. |
| **Stage D Workflows** | **Reused** | `VerificationWorkflowService`, `DataQualityEngineService` (targeted alert events). |
| **Stage E Workflows** | **Reused** | `EmploymentLifecycleWorkflowService`, `FollowupEngagementWorkflowService`, `TraineeUnemploymentEventService`. |
| **Stage F Workflows** | **Reused** | `SkillGapRemediationWorkflowService`, `TrainingOutcomeWorkflowService`. |
| **Domain Events** | **Reused** | `TraineeLifecycleEvent`, `PlacementLifecycleEvent`, `EmploymentVerificationEvent`, `DataQualityAlertEvent`, `CommunicationTriggerEvent`. |
| **Security & Tenancy** | **Reused** | `WorkflowSecurityScopeValidator` (`validateProviderScope`, `validateTraineeScope`). |
| **Audit Interception** | **Reused** | `@AuditTrail` annotation intercepted by `AuditTrailAspect` and dispatched via `AuditTrailEventListener`. |
| **Analytics Cache Eviction** | **Reused** | Delegated through existing workflows into `AnalyticsCacheEvictionService`. |

---

## 3. Workflow Coverage & Domain Choreography Matrix

The 6 lifecycle stages are mapped to their governing services, transactional boundaries, tenancy scopes, and domain events:

| Stage | Orchestrator Method | Core Services Delegated To | Transaction Boundary | Security / Tenancy Scope | Domain Events Published |
|---|---|---|---|---|---|
| **Stage A: Registration & Enrollment** | `orchestrateTraineeEnrollment(request)` | `TraineeService`, `TraineeConsentService`, `TrainingBatchService`, `TrainingEnrollmentService` | `@Transactional` (Atomic Stage A boundary) | Provider Scope: `batch.trainingCenter.trainingProviderId` | `TraineeLifecycleEvent` (Action: `REGISTERED_AND_ENROLLED`) |
| **Stage B: Training & Assessment Progression** | `progressTrainingAndAssessment(traineeId, batchId, attendanceReq, assessmentReq, certReq)` | `AttendanceRecordService`, `TrainingOutcomeWorkflowService`, `CertificationService` | `@Transactional` (Atomic Stage B boundary) | Provider Scope + Trainee Ownership | `TraineeLifecycleEvent` (Action: `ASSESSMENT_COMPLETED` / `CERTIFIED`) |
| **Stage C: Placement & Joining** | `orchestratePlacementAndJoining(traineeId, placementReq, employmentReq)` | `PlacementEmploymentWorkflowService`, `PlacementRecordService`, `EmploymentRecordService` | `@Transactional` (Atomic Stage C boundary) | Provider Scope + Trainee Ownership | `PlacementLifecycleEvent` (Action: `PLACED_AND_EMPLOYED`) |
| **Stage D: Employment Verification** | `orchestrateEmploymentVerification(traineeId, employmentId, verificationReq, evidenceReq, verdictReq)` | `VerificationWorkflowService`, `EmploymentRecordService` | `@Transactional` (Atomic Stage D boundary) | Provider Scope + Trainee Scope | `EmploymentVerificationEvent` (Action: `VERIFIED` / `REJECTED`), `DataQualityAlertEvent` (on rejection) |
| **Stage E: Retention & Exit** | `orchestrateRetentionAndExit(traineeId, employmentId, exitReq, unemploymentReq)` | `EmploymentLifecycleWorkflowService`, `EmploymentRecordService`, `TraineeUnemploymentEventService` | `@Transactional` (Atomic Stage E boundary) | Provider Scope + Trainee Scope | `TraineeLifecycleEvent` (Action: `EMPLOYMENT_EXITED_AND_UNEMPLOYED`) |
| **Stage F: Remediation Trigger** | `triggerRemediationWorkflow(request)` | `SkillGapRemediationWorkflowService`, `EmploymentRecordService` | `@Transactional` (Atomic Stage F boundary) | Trainee Scope | `CommunicationTriggerEvent` (Notification: `REMEDIATION_ACTION_PLAN_GENERATED`) |
| **Read-Only Unified Summary** | `getCandidateLifecycleSummary(traineeId)` | `TraineeService`, `TraineeConsentService`, `TrainingOutcomeWorkflowService`, `PlacementEmploymentWorkflowService`, `SkillGapRemediationWorkflowService` | `@Transactional(readOnly = true)` | Trainee Scope | None (Read-only projection) |

---

## 4. Transactional Boundaries & Rollback Semantics

### 4.1 Rejection of Monolithic Transactions
A candidate's progression from initial registration to training, assessment, placement, multi-year retention, and possible remediation spans weeks, months, or years. Attempting to encapsulate the candidate lifecycle within a single long-running database transaction is a severe architectural anti-pattern that causes:
- Extended database row and table lock contention.
- Connection pool exhaustion under concurrent loads.
- Catastrophic cascading rollbacks erasing months of verified historical progress upon minor downstream validation errors.

### 4.2 Discrete, Atomic Domain Transitions
`CandidateLifecycleOrchestratorServiceImpl` partitions the lifecycle into six discrete, self-contained atomic transitions. Each transition is governed by `@Transactional(rollbackFor = Exception.class)`:
- If an error occurs during Stage A (e.g., training batch has reached maximum capacity or batch status is inactive), the trainee creation and enrollment are rolled back atomically, preventing orphaned records.
- If an error occurs during Stage C (e.g., duplicate active employment record), the placement record changes roll back cleanly without impacting the trainee's completed Stage B certification.
- If an error occurs during Stage E (e.g., invalid unemployment reason ID), the employment exit status and unemployment event are rolled back together, ensuring the employment record remains in its previous active state.

### 4.3 Rollback Evidence & Integration Safety
The transactional integrity of the underlying services was validated against the live database in `WorkflowTransactionRollbackIntegrationTest`:
- 10 transactional rollback and isolation tests passed against `SIH26135_TEST`.
- Forced runtime exceptions correctly rolled back database writes across `TrainingOutcomeWorkflowService`, `PlacementEmploymentWorkflowService`, and `EmploymentLifecycleWorkflowService`.

---

## 5. Domain Event Choreography & Identity Consistency

### 5.1 Event Publishing & Decoupled Execution
Phase 29.2 wires event dispatching through Spring's `ApplicationEventPublisher`. To prevent side effects (such as external notifications or search index updates) from executing when a database transaction fails:
- Domain events are dispatched during service execution.
- `CandidateLifecycleEventListener` handles events using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)`.
- If the enclosing database transaction rolls back, event listeners are never invoked, ensuring zero external side effects on failed transactions.

### 5.2 Domain Event Identity Mapping
All published events implement the unified `DomainEvent` contract established in Phase 29.1:
- `TraineeLifecycleEvent`: `entityType` is `"Trainee"`, `entityId` corresponds to the canonical `traineeId`. Secondary identifiers (e.g., `enrollmentId`, `batchId`, `certificationId`, `exitEventId`) are conveyed within structured `metadataJson`.
- `PlacementLifecycleEvent`: `entityType` is `"PlacementRecord"`, `entityId` corresponds to `placementRecordId`.
- `EmploymentVerificationEvent`: `entityType` is `"EmploymentVerification"`, `entityId` corresponds to `employmentVerificationId`.
- `DataQualityAlertEvent`: Dispatched on verification rejection (`DQ-EMPLOYMENT-VERIFICATION-FAILED`) with severity `HIGH`.
- `CommunicationTriggerEvent`: Masked recipient PII and sanitized payload summaries (`recipientMasked`, `payloadSummary`) are strictly enforced.

---

## 6. Security, Tenancy Isolation & Consent Governance

### 6.1 Multi-Tenant Scope Validation
`CandidateLifecycleOrchestratorServiceImpl` strictly enforces tenancy boundaries using `WorkflowSecurityScopeValidator`:
- **Provider Scope Validation:** `validateProviderScope(trainingProviderId)` verifies that caller credentials (extracted from `SecurityContextHolder`) possess administrative authority or matching provider tenancy before initiating enrollments, training updates, or placements.
- **Trainee Scope Validation:** `validateTraineeScope(traineeId)` ensures that individual trainees can only inspect or request actions for their own records, while preventing cross-tenant access.

### 6.2 Trainee Consent Validation
Under Indian DPDP Act guidelines and system governance invariants:
- **Registration & Enrollment (Stage A):** Strictly requires explicit, active consent for `DATA_PROCESSING` (ID: 1, seeded in `ref_consent_type`).
- **Follow-up & Retention (Stage E):** Strictly requires active consent for `EMPLOYMENT_FOLLOW_UP` (ID: 3).
- **Consent Checks:** `validateConsent(traineeId, consentTypeId)` queries `TraineeConsentRepository`. If consent is missing, expired (`expiresAt < now`), or revoked (`revokedAt != null` or `isConsentGiven == false`), the orchestrator aborts execution and throws `BadRequestException` with error code `CONSENT_REQUIRED` or `CONSENT_REVOKED`.

---

## 7. Audit Trail & Data Quality Engine Integration

### 7.1 Declarative `@AuditTrail` Interception
Every orchestrator method is annotated with `@AuditTrail`:
```java
@AuditTrail(actionCode = "ORCHESTRATE_ENROLLMENT", entityType = "Trainee", description = "Orchestrated trainee registration and batch enrollment")
public TraineeEnrollmentOrchestrationResponse orchestrateTraineeEnrollment(...)
```
- `AuditTrailAspect` automatically intercepts invocation, captures the authenticated caller ID, client IP address, correlation ID, and execution status.
- Dispatches an immutable `AuditTrailEvent` asynchronously to `AuditTrailEventListener` on `auditTaskExecutor`.

### 7.2 Safe Data Quality Alert Dispatching
Blind, full data-quality scans across the entire tenant during high-frequency lifecycle transitions introduce unbounded execution latencies and risk cross-tenant data leaks.
- When an employment verification is rejected in Stage D (`status == "REJECTED"`), the orchestrator publishes a targeted `DataQualityAlertEvent`:
  - `ruleCode`: `"DQ-EMPLOYMENT-VERIFICATION-FAILED"`
  - `severity`: `"HIGH"`
  - `affectedEntity`: `"EmploymentRecord"`
  - `entityId`: `employmentId`
- This informs operational monitoring and schedules targeted remediation without triggering premature or un-scoped engine sweeps.

---

## 8. Analytics Cache Eviction Coordination

To prevent stale projections across executive, provider, and scheme dashboards:
- Underlying workflows (`PlacementEmploymentWorkflowServiceImpl`, `TrainingOutcomeWorkflowServiceImpl`, `EmploymentLifecycleWorkflowServiceImpl`) invoke `AnalyticsCacheEvictionService` directly upon completing state transitions.
- The orchestrator delegates state mutations directly to these workflows, ensuring cache keys for placement rates, verification rates, training dropout rates, and employment retention are evicted automatically without duplicate or conflicting invalidation logic.

---

## 9. Changed-File Inventory & Architecture Metrics

Nine new files were added to implement and verify Phase 29.2. Zero existing files were modified.

| No. | File Path | Lines | Description / Architectural Purpose |
|---|---|---|---|
| 1 | `backend/src/main/java/in/gov/sih/sih26135/dto/request/TraineeEnrollmentOrchestrationRequest.java` | 127 | Strongly typed composite request DTO for Stage A trainee registration and batch enrollment. |
| 2 | `backend/src/main/java/in/gov/sih/sih26135/dto/request/RemediationTriggerRequest.java` | 67 | Strongly typed request DTO for Stage F remediation evaluation and action plan triggers. |
| 3 | `backend/src/main/java/in/gov/sih/sih26135/dto/response/TraineeEnrollmentOrchestrationResponse.java` | 10 | Immutable record response DTO returning created trainee and enrollment details. |
| 4 | `backend/src/main/java/in/gov/sih/sih26135/dto/response/RemediationTriggerResponse.java` | 13 | Immutable record response DTO returning skill-gap evaluation summary and recommended course IDs. |
| 5 | `backend/src/main/java/in/gov/sih/sih26135/dto/response/CandidateLifecycleSummaryResponse.java` | 14 | Immutable record response DTO aggregating the end-to-end candidate lifecycle journey. |
| 6 | `backend/src/main/java/in/gov/sih/sih26135/service/CandidateLifecycleOrchestratorService.java` | 87 | Service interface defining discrete choreography methods for Stages A–F and read-only summary. |
| 7 | `backend/src/main/java/in/gov/sih/sih26135/service/impl/CandidateLifecycleOrchestratorServiceImpl.java` | 517 | Production implementation coordinating workflow services, transactions, events, security, and consent. |
| 8 | `backend/src/main/java/in/gov/sih/sih26135/event/listener/CandidateLifecycleEventListener.java` | 67 | Transaction-aware event listener (`@TransactionalEventListener(phase = AFTER_COMMIT)`) for decoupled processing. |
| 9 | `backend/src/test/java/in/gov/sih/sih26135/service/CandidateLifecycleOrchestrationIntegrationTest.java` | 678 | Comprehensive integration test suite covering all 6 stages, negative branches, consent denial, and event listener safety. |
| **Total** | **9 files** | **1,580** | **Clean additions, zero existing files modified, zero architectural violations.** |

---

## 10. Test Execution Evidence

### 10.1 Focused Orchestration Test Suite
`CandidateLifecycleOrchestrationIntegrationTest` verifies all discrete workflow transitions, negative failure branches, consent denial, and transaction boundaries:
- **Command:** `mvn test -f backend/pom.xml "-Dtest=CandidateLifecycleOrchestrationIntegrationTest"`
- **Result:**
  ```text
  [INFO] Running in.gov.sih.sih26135.service.CandidateLifecycleOrchestrationIntegrationTest
  [INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 28.12 s -- in in.gov.sih.sih26135.service.CandidateLifecycleOrchestrationIntegrationTest
  [INFO] 
  [INFO] Results:
  [INFO] 
  [INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] BUILD SUCCESS
  ```

### 10.2 Full Backend Regression Test Suite
The entire backend test suite was executed to guarantee zero regressions across all 28 prior phases:
- **Command:** `mvn test -f backend/pom.xml`
- **Result:**
  ```text
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  [INFO] Total time:  06:07 min
  [INFO] Finished at: 2026-11-15T16:01:53+05:30
  [INFO] ------------------------------------------------------------------------
  [INFO] Results:
  [INFO] 
  [INFO] Tests run: 2047, Failures: 0, Errors: 0, Skipped: 0
  ```
- **Comparison to Baseline:**
  - Phase 29.1 baseline: 2,025 tests passing.
  - Phase 29.2 tests added: 22 tests passing.
  - Total tests: 2,047 tests passing (0 failures, 0 errors, 0 skipped).

### 10.3 Architecture and Contract Invariant Tests
- `OperationalErrorCodeContractTest`: Passed (confirms exactly 137 JPA repositories and 2 active `@Query` annotations in `src/main`).
- `DomainEventContractTest`: Passed (confirms event identity stability and PII masking).
- `WorkflowTransactionRollbackIntegrationTest`: Passed (confirms rollback safety against `SIH26135_TEST`).

---

## 11. External Prerequisites & Deployment Readiness

- **Database:** MySQL 8.0.46 running locally with database catalog `SIH26135_TEST` configured and active.
- **Cache Infrastructure:** Redis running on `127.0.0.1:6379` (with in-memory fallback enabled).
- **Runtime Environment:** Java 17.0.19, Spring Boot 3.4.10, Maven 3.9+.
- **Configuration Invariant:** `spring.jpa.hibernate.ddl-auto=validate` strictly enforced. Zero schema migrations or DDL mutations required.

---

## 12. Unresolved Defects and Limitations

None. All mandatory Phase 29.2 requirements, integration boundaries, security validations, and test suites completed with 100% compliance.

---

## 13. Phase 29.3 Readiness & Governance Compliance

In accordance with project governance protocols:
- **Phase 29.2 is COMPLETE.**
- Implementation is **FROZEN** pending independent review and approval.
- **Do not proceed to Phase 29.3** (External Communication & Asynchronous Integration) until Phase 29.2 receives formal sign-off.
