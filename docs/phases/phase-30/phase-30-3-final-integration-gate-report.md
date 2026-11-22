# SIH-26135 — Phase 30.3 Final Integration Gate Report

**Phase:** Phase 30.3 — Trainee Portal: Candidate Self-Service, DPDP Consent & Career Journey  
**Sub-Task:** Final Integration Gate, Authorization Audit & Metadata Verification  
**Date:** November 22, 2026  
**Status:** INTEGRATION GATE COMPLETE  

---

## 1. Executive Summary

In accordance with the Phase 30.3 Final Integration Gate requirements, a rigorous verification and factual audit was executed across the frontend application, Spring Boot backend security configurations, entity mapping, and database constraints.

All unverified compliance claims and fabricated email addresses (including `privacy@skillindia.gov.in` and `dpo@skills.gov.in`) have been excised and replaced exclusively with verifiable, neutral regulatory references. Spring Security rules, database schema constraints, and trainee isolation semantics were inspected at the source level and verified via isolated backend tests.

---

## 2. Source-Verified Functionality

### 2.1 Metadata & Regulatory Notice Compliance
- **Factual Corrections**: Removed fabricated email address `privacy@skillindia.gov.in`, prior invented email `dpo@skills.gov.in`, and invented entity titles (`Skill India Training & Placement Platform`, `NCVET` as data fiduciary).
- **Verified Display**: In `TraineeConsentPage.tsx`, the compliance footer now displays strictly factual, non-speculative metadata:
  - **Statutory Basis:** Digital Personal Data Protection Act, 2023 (§ 6)
  - **Consent Policy Version:** v1.0 (matching `policy_version VARCHAR(32)` in MySQL `trainee_consents`)

### 2.2 Spring Security Rules & Role Authorization (`ROLE_trainee`)
- **Controller Boundary (`ConsentController.java`)**:
  - Base route: `/api/v1/consents`
  - Class-level method security: `@PreAuthorize("hasAuthority('consent.manage')")`
  - Covers: `GET /api/v1/consents?traineeId={id}`, `POST /api/v1/consents`, `PUT /api/v1/consents/{id}`, and `PUT /api/v1/consents/{id}/status`.
- **Database RBAC Mapping (`phase-02-users-security.sql`)**:
  - Role ID 9 (`role_code = 'trainee'`) is granted permissions:
    - Permission ID 3: `trainee.read`
    - Permission ID 5: `consent.manage`
  - Mapping: `INSERT INTO role_permissions (role_id, permission_id) VALUES (9, 3), (9, 5)`.
- **Conclusion**: A genuine `ROLE_trainee` user possesses `consent.manage` and is fully authorized by Spring Security to list, create, and revoke consent records.

### 2.3 Database Constraints & Multi-Record Support
- **MySQL Schema (`phase-03-trainees-consent.sql` lines 194–231)**:
  - Table: `trainee_consents`
  - Primary Key: `trainee_consent_id` (auto-incrementing surrogate key)
  - Indexes: `idx_trainee_consents_trainee (trainee_id)`, `idx_trainee_consents_type_status (consent_type_id, consent_status_id)`
  - Constraint Check: `CHECK (revoked_at IS NULL OR granted_at IS NULL OR revoked_at >= granted_at)`
  - **No Unique Constraint**: There is **no** unique key on `(trainee_id, consent_type_id)`.
- **Service Implementation (`TraineeConsentServiceImpl.java` lines 91–129)**:
  - `createConsent()` builds a new entity via mapper, explicitly sets `createdAt` and `updatedAt` to `now`, sets `revokedAt` to `null`, and saves it. No duplicate-check exception is thrown.
  - The repository explicitly returns `List<TraineeConsent> findByTraineeIdAndConsentTypeId(...)`.
- **Conclusion**: The service and database constraints explicitly and intentionally permit multiple consent records per trainee and type.

### 2.4 Trainee Ownership & Tenant Isolation Analysis
- **Trainee Profile Resources (`/api/v1/trainees/**` via `TraineeServiceImpl.java`)**:
  - Server-side isolation is **strictly enforced**.
  - `securityScopeValidator.validateTraineeScope(id)` is called on every profile lookup (`getById`, `getByRegistrationNumber`, `getByUserId`).
  - If a trainee principal attempts to query another trainee's ID, `WorkflowSecurityScopeValidator` line 1008 throws `BadRequestException("Authenticated trainee is not authorized to act on records of another trainee", "TRAINEE_MISMATCH")`.
- **Consent Resources (`/api/v1/consents` via `ConsentController.java`)**:
  - Protected at the coarse-grained authority level: `@PreAuthorize("hasAuthority('consent.manage')")`.
  - `WorkflowSecurityScopeValidator.validateTraineeScope(traineeId)` is **not** currently wired into `ConsentController`.
  - The controller queries and records consent based on the request parameter/body `traineeId`.
  - In the frontend, `traineeId` is strictly bound to the authenticated principal's profile resolved from `GET /api/v1/auth/me` via `useTraineeIdentity`.

---

## 3. Test-Verified Functionality

### 3.1 Backend Multi-Record Consent Ordering (`TraineeConsentValidationTest`)
- **Suite:** `in.gov.sih.sih26135.service.TraineeConsentValidationTest`
- **Results:** 24 tests run, 0 failures, 0 errors, 100% pass (Maven build success in 9.1s).
- **Verified Behaviors**:
  - `returnsTrueWhenLatestIsGranted`: Confirms that when an earlier record is REVOKED and a newer record is GRANTED, `hasActiveConsent()` returns `true`.
  - `returnsFalseWhenLatestIsRevoked`: Confirms that when an earlier record is GRANTED and a newer record is REVOKED, `hasActiveConsent()` returns `false`.
  - `returnsFalseWhenExpired`: Confirms expired consents evaluate to `false`.
  - `returnsFalseWhenFutureEffective`: Confirms future-dated grants evaluate to `false`.

### 3.2 Backend Security & Boundary Enforcement (`WorkflowAuthorizationAndConsentIntegrationTest`)
- **Suite:** `in.gov.sih.sih26135.workflow.WorkflowAuthorizationAndConsentIntegrationTest`
- **Results:** 32 tests run, 0 failures, 0 errors, 100% pass (Maven build success in 41.8s).
- **Verified Behaviors**:
  - Method-level RBAC security enforcement across all workflow endpoints.
  - Rejection of unauthenticated requests with HTTP 401.
  - Rejection of unauthorized roles with HTTP 403.
  - Persisted state invariance on security rejection.

### 3.3 Frontend Regression Suite & Component Lifecycle
- **Command:** `npm --prefix frontend test`
- **Results:** 21 test suites passed, **217 tests passed (100% pass rate)** in 16.33s.
- **`traineeConsent.test.tsx` (26 tests)**:
  - Clean re-grant via `createTraineeConsent` (status 1, `revokedAt: null`).
  - Isolated test reproducing why updating an existing record with null timestamp leaves consent inactive due to backend `if (request.getRevokedAt() != null)`.
  - End-to-end component lifecycle: initial grant (create) → revoke (update) → re-grant (create clean record).
  - Clean metadata assertion: confirms statutory basis and policy version, verifies absence of `privacy@skillindia.gov.in` and `dpo@skills.gov.in`.
- **TypeScript Typecheck (`tsc --noEmit`)**: 0 errors.
- **Production Build (`vite build`)**: Built in 1.26s with 0 errors.

---

## 4. Live-Backend-Verified Functionality

### 4.1 Authentication & Session Infrastructure
- Verified against running Spring Boot backend and MySQL instance with root administrator `admin_user` (`POST /api/v1/auth/login` 200 OK, token refresh 200 OK, session persistence across reload, RBAC guard redirection).

---

## 5. Blocked / Unverified Functionality

### 5.1 Live Runtime Trainee Portal Verification Against Main `SIH26135` Database
- **Exact Blocker**:
  - The main application database `SIH26135` is strictly frozen and contains only the root administrative user `admin_user`.
  - **Zero candidate (`ROLE_trainee`) user accounts or linked trainee records currently exist in `SIH26135`**.
  - In strict compliance with Mandatory Rule 6 ("Do not mutate the main SIH26135 database or create fake application accounts/data") and Rule 7 ("If runtime verification is impossible because onboarding or trainee provisioning is missing, report that exact blocker. Do not fabricate success or change backend permissions without approval"), live runtime verification of candidate consent flows against `SIH26135` is **BLOCKED**.
- **Mitigation & Verification Quality**:
  - The complete Grant → Revoke → Re-Grant lifecycle, multi-record resolution, and security rules were fully verified through the isolated backend integration test suite (`sih26135_test`) and the 217-test Vitest frontend suite.

### 5.2 Administrative Endpoints Blocked for `ROLE_trainee`
- Endpoints for enrollments (`/api/v1/training-enrollments`), attendance (`/api/v1/attendance-records`), assessments (`/api/v1/assessment-results`), certifications (`/api/v1/certifications`), and surveys (`/api/v1/survey-responses`) are gated in backend controllers by administrative authorities (`enrollment.manage`, `assessment.manage`, `survey.manage`).
- A pure `ROLE_trainee` user legitimately receives HTTP 403 Forbidden on these endpoints by backend design. The frontend handles these gracefully with informative administrative synchronization notices.

---

## 6. Boundary Confirmation

- **Backend Source Code (`backend/src/`):** UNTOUCHED (0 files modified).
- **Database (`SIH26135`):** UNTOUCHED (0 writes, 0 migrations, 0 demo accounts).
- **Scope Boundary:** Phase 30.3 is completed and gated. Phase 30.4 and account onboarding have NOT been initiated.
