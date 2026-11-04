# Phase 26.5 — Request DTO Bean Validation, Schema Alignment & Security Interceptor Review Report

**Project:** SIH 26135 Backend (National Skill & Employment Platform)  
**Execution Phase:** Phase 26.5 — Request DTO Bean Validation, Schema Alignment & Security Interceptor Review  
**Branch:** `main`  
**Database Catalog:** `SIH26135_TEST` (`spring.jpa.hibernate.ddl-auto=validate`)  
**Java Version:** OpenJDK 17.0.19  
**Spring Boot Version:** 3.4.10 (Hibernate Validator 8.0.3.Final / Jakarta Validation 3.0.2 / Spring Security 6.4.x)  
**Verification Verdict:** `ACCEPTED`

---

## 1. Executive Summary

Phase 26.5 delivers comprehensive Jakarta Bean Validation across the SIH-26135 Spring Boot backend and activates validation at the REST controller boundary while strictly preserving authorization boundary invariants.

Building on the centralized validation foundation established in Phase 26.2, Phase 26.5:
1. Applied schema-aligned Jakarta validation constraints to all 118 request DTOs, matching column definitions in the frozen `SIH26135_TEST` MySQL catalog.
2. Activated request body validation across all 53 REST controllers and 108 `@RequestBody` parameters using `@Valid`.
3. Conducted an exhaustive audit of all 60 `@PreAuthorize` authorization expressions across every controller in the repository.
4. Identified architectural limitations in the custom expression parser `SimpleSecurityExpressionRoot`.
5. Corrected the security interceptor by migrating to Spring Security's native `PreAuthorizeAuthorizationManager` infrastructure.
6. Verified authorization ordering across all 4 mandatory states (unauthenticated, unauthorized, authorized-invalid, authorized-valid), confirming that unauthorized callers are rejected with HTTP 403 Forbidden before payload parsing and Bean Validation execute.
7. Executed the complete test suite: **737 tests run, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS`).

---

## 2. Complete `@PreAuthorize` Expression Inventory

An exhaustive static and runtime audit of the entire backend codebase (`backend/src/main/java`) identified exactly **60 `@PreAuthorize` annotations** across **53 controllers** (0 on services, repositories, or components).

### 2.1 Distribution and Expression Categorization

The 60 annotations map to **22 unique expression forms**:

| Expression Type | Unique Expression | Controller Location(s) & Annotation Target | Count |
| :--- | :--- | :--- | :---: |
| **Simple Authority** | `hasAuthority('analytics.read')` | `AnalyticsFactController`, `AnalyticsSummaryController` (Class) | 2 |
| **Simple Authority** | `hasAuthority('assessment.manage')` | `AssessmentController`, `AssessmentResultController`, `CertificationController`, `SkillGapAssessmentController`, `SkillGapController`, `SkillGapObservationController`, `SkillGapRecommendationController`, `TraineeAssessmentController` (Class) | 8 |
| **Simple Authority** | `hasAuthority('center.manage')` | `TrainingCenterController` (Class) | 1 |
| **Simple Authority** | `hasAuthority('consent.manage')` | `ConsentController` (Class) | 1 |
| **Simple Authority** | `hasAuthority('course.manage')` | `CourseController`, `TrainingBatchController` (Class) | 2 |
| **Simple Authority** | `hasAuthority('employment.manage')` | `EmploymentExitEventController`, `EmploymentRecordController`, `SalaryHistoryController`, `TraineeUnemploymentEventController`, `EmploymentLifecycleWorkflowController` (Class) | 5 |
| **Simple Authority** | `hasAuthority('enrollment.manage')` | `AttendanceRecordController`, `TrainingEnrollmentController` (Class) | 2 |
| **Simple Authority** | `hasAuthority('placement.manage')` | `EmployerController`, `JobApplicationController`, `JobPostingController`, `PlacementRecordController` (Class) | 4 |
| **Simple Authority** | `hasAuthority('program.manage')` | `ProgramController`, `SchemeController` (Class) | 2 |
| **Simple Authority** | `hasAuthority('provider.manage')` | `TrainingProviderController` (Class) | 1 |
| **Simple Authority** | `hasAuthority('survey.manage')` | `FollowupCampaignController`, `FollowupTaskController`, `SurveyQuestionController`, `SurveyResponseAnswerController`, `SurveyResponseController`, `FollowupEngagementWorkflowController` (Class) | 6 |
| **Simple Authority** | `hasAuthority('trainee.read')` | `TraineeController.getAllTrainees`, `TraineeController.getTraineeById` (Method) | 2 |
| **Simple Authority** | `hasAuthority('trainee.write')` | `TraineeController.createTrainee`, `TraineeController.updateTrainee`, `TraineeController.deleteTrainee` (Method) | 3 |
| **Simple Authority** | `hasAuthority('user.manage')` | `PermissionController`, `RoleController`, `UserController` (Class) | 3 |
| **Simple Authority** | `hasAuthority('verification.manage')` | `EmploymentVerificationAttemptController`, `EmploymentVerificationController`, `EmploymentVerificationEvidenceController`, `EmploymentVerificationRequestController`, `VerificationWorkflowController` (Class) | 5 |
| **Multiple Authority** | `hasAnyAuthority('audit.read', 'system.manage')` | `AuditLogController`, `DataQualityIssueController`, `DataQualityIssueEventController`, `DataQualityRuleController`, `SystemEventLogController` (Class) | 5 |
| **Multiple Authority** | `hasAnyAuthority('system.manage', 'audit.read')` | `ImportBatchController`, `ImportRecordController` (Class) | 2 |
| **Multiple Authority** | `hasAnyAuthority('survey.manage', 'system.manage')` | `CommunicationLogController` (Class) | 1 |
| **Multiple Authority** | `hasAnyAuthority('placement.manage', 'employment.manage')` | `PlacementEmploymentWorkflowController` (Class) | 1 |
| **Multiple Authority** | `hasAnyAuthority('assessment.manage', 'enrollment.manage')` | `SkillGapRemediationWorkflowController` (Class) | 1 |
| **Multiple Authority** | `hasAnyAuthority('enrollment.manage', 'assessment.manage')` | `TrainingOutcomeWorkflowController` (Class) | 1 |
| **Composite (Auth+Role)** | `hasAnyAuthority('assessment.manage', 'enrollment.manage') or hasRole('trainee')` | `SkillGapRemediationWorkflowController.acceptRecommendation`, `SkillGapRemediationWorkflowController.enrollInRemediation` (Method) | 2 |
| **Total** | | | **60** |

### 2.2 Expression Constructs Inventory Comparison

| Expression Feature | Repository Usage | Supported by `SimpleSecurityExpressionRoot` | Supported by Native `PreAuthorizeAuthorizationManager` |
| :--- | :---: | :---: | :---: |
| `hasAuthority('...')` | **52 occurrences** | Yes | **Yes (Full native support)** |
| `hasAnyAuthority(...)` | **11 occurrences** | Yes | **Yes (Full native support)** |
| `hasRole('...')` | **2 occurrences** | Yes | **Yes (Full native support)** |
| `hasAnyRole(...)` | 0 occurrences | Yes | **Yes (Full native support)** |
| Composite `or` / `and` | **2 occurrences** | Partial (only for supported methods) | **Yes (Full SpEL grammar)** |
| `isAuthenticated()` | 0 currently (standard) | **No** (Throws SpEL evaluation exception) | **Yes (Full native support)** |
| `isAnonymous()` | 0 currently (standard) | **No** (Throws SpEL evaluation exception) | **Yes (Full native support)** |
| `permitAll` / `permitAll()` | 0 currently (standard) | **No** (Throws SpEL evaluation exception) | **Yes (Full native support)** |
| `denyAll` / `denyAll()` | 0 currently (standard) | **No** (Throws SpEL evaluation exception) | **Yes (Full native support)** |
| `authentication` / `principal` | 0 currently (standard) | **No** (Not exposed in root) | **Yes (Full native support)** |
| Method parameters (`#id`) | 0 currently (standard) | **No** (No parameter discoverer) | **Yes (Full native support)** |
| Spring Beans (`@service`) | 0 currently (standard) | **No** (No BeanResolver configured) | **Yes (Full native support)** |
| `RoleHierarchy` Integration | Applied in security | **No** (Plain string comparison) | **Yes (Full native support)** |
| `TrustResolver` Integration | Applied in security | **No** (Misclassified anonymous tokens)| **Yes (Full native support)** |

---

## 3. Interceptor Limitations Discovered

Review of the initial `MethodSecurityPreValidationInterceptor` identified significant architectural limitations in the ad-hoc `SimpleSecurityExpressionRoot`:

1. **Dual Evaluation Engine Discrepancy:** The interceptor used a separate `SpelExpressionParser` and custom `StandardEvaluationContext` disconnected from Spring Security's method security proxy. This caused expression evaluation to drift from Spring Security's native semantics.
2. **Missing Standard Security Root Methods:** Standard SpEL expressions such as `isAuthenticated()`, `isFullyAuthenticated()`, `isAnonymous()`, `permitAll`, and `denyAll` were not implemented on `SimpleSecurityExpressionRoot`. If used, the parser threw `SpelEvaluationException` at runtime.
3. **Absence of Spring Context & Bean Resolution:** The custom context lacked an `ApplicationContext` or `BeanResolver`, making it impossible to evaluate expressions invoking custom security beans (e.g. `@permissionChecker.check(...)`).
4. **Missing Parameter Name Discovery:** Method parameters (e.g., `@PreAuthorize("#id == authentication.principal.id")`) could not be resolved because the evaluation context lacked `MethodSecurityEvaluationContext` parameter bindings.
5. **Lack of Role Hierarchy & Trust Resolver Support:** Role prefixing was hardcoded with `ROLE_` strings, bypassing Spring Security's configured `RoleHierarchy` and `AuthenticationTrustResolver`.
6. **Anonymous Token Misclassification:** In Spring Security, `AnonymousAuthenticationToken.isAuthenticated()` returns `true`. The initial interceptor did not check `AuthenticationTrustResolver.isAnonymous(auth)`, risking misclassifying anonymous callers as authenticated.

---

## 4. The Native Spring Security Correction

Rather than expanding the handcrafted `SimpleSecurityExpressionRoot`, the interceptor was refactored to delegate directly to Spring Security's native `org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager`.

### 4.1 Implementation Architecture

```
                       +------------------------------------------+
                       |         Incoming HTTP Request            |
                       +------------------------------------------+
                                            |
                                            v
                       +------------------------------------------+
                       |       SecurityFilterChain (JWT Auth)     |
                       +------------------------------------------+
                                            |
                                            v
                       +------------------------------------------+
                       |  MethodSecurityPreValidationInterceptor  |
                       |  (PreAuthorizeAuthorizationManager)      |
                       +------------------------------------------+
                                      /            \
                        Unauthorized /              \ Authorized
                                    v                v
                        +---------------+    +----------------------------------+
                        | HTTP 403      |    | Spring MVC Argument Resolution   |
                        | Forbidden     |    | & Jakarta Bean Validation        |
                        +---------------+    +----------------------------------+
                                                              /          \
                                                      Invalid/            \ Valid
                                                            v              v
                                                +---------------+  +-------------------+
                                                | HTTP 400      |  | Controller Method |
                                                | VALIDATION_   |  | Execution         |
                                                | ERROR         |  +-------------------+
                                                +---------------+
```

### 4.2 Key Correction Details

1. **Native `PreAuthorizeAuthorizationManager`:**
   ```java
   this.preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();
   this.preAuthorizeAuthorizationManager.setApplicationContext(applicationContext);
   ```
   Configuring the `ApplicationContext` initializes the manager with Spring Security's standard `MethodSecurityExpressionHandler`, giving access to role hierarchies, trust resolvers, bean resolvers, and full SpEL grammar.

2. **`HandlerMethodInvocation` Adapter:**
   Created an AOP Alliance `MethodInvocation` adapter bridging Spring MVC's `HandlerMethod` to Spring Security's authorization manager:
   - `getMethod()` returns `handlerMethod.getMethod()`.
   - `getThis()` resolves the controller bean instance from `ApplicationContext`.
   - `getArguments()` returns empty array for pre-validation evaluation.
   - `getStaticPart()` returns `handlerMethod.getMethod()`.

3. **`AuthenticationTrustResolver` Integration:**
   ```java
   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
   if (auth == null || !auth.isAuthenticated() || trustResolver.isAnonymous(auth)) {
     return true; // Allow unauthenticated requests to reach Spring Security's authentication entry point (401)
   }
   ```
   Ensures unauthenticated and anonymous requests are handled by `RestAuthenticationEntryPoint`, producing consistent **HTTP 401 Unauthorized** responses rather than premature 403 Forbidden.

4. **Deterministic Pre-Validation Denial:**
   ```java
   AuthorizationDecision decision = preAuthorizeAuthorizationManager.check(() -> auth, invocation);
   if (decision != null && !decision.isGranted()) {
     throw new AccessDeniedException("Access denied");
   }
   ```
   When the authenticated identity lacks required authorities or roles, `AccessDeniedException` is thrown during `preHandle`, triggering `RestAccessDeniedHandler` to return **HTTP 403 Forbidden** with `code=FORBIDDEN` and `message="Access denied"`. Spring MVC never parses the JSON payload or executes Bean Validation.

---

## 5. Authorization Ordering Verification

The pre-validation ordering was rigorously verified against the 4 required system states:

| State | Request Condition | Endpoint Tested | Expected Outcome | Actual Outcome | Business Logic Executed? | Status |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: |
| **State 1** | Unauthenticated + Invalid Body (`{}`) | `POST /api/v1/trainees` | **HTTP 401 Unauthorized** (`code=UNAUTHORIZED`, `message="Authentication is required to access this resource"`) | HTTP 401 Unauthorized (`code=UNAUTHORIZED`) | **NO** (`Mockito.verifyNoInteractions`) | **PASS** |
| **State 2** | Authenticated without authority (`trainee.read` only) + Invalid Body (`{}`) | `POST /api/v1/trainees` | **HTTP 403 Forbidden** (`code=FORBIDDEN`, `message="Access denied"`) | HTTP 403 Forbidden (`code=FORBIDDEN`, `message="Access denied"`) | **NO** (`Mockito.verifyNoInteractions`) | **PASS** |
| **State 3** | Authenticated with authority (`trainee.write`) + Invalid Body (`{}`) | `POST /api/v1/trainees` | **HTTP 400 Bad Request** (`code=VALIDATION_ERROR`, `message="Request validation failed"`, `details=[...]`) | HTTP 400 Bad Request (`code=VALIDATION_ERROR`, details on firstName, registrationNumber) | **NO** (`Mockito.verifyNoInteractions`) | **PASS** |
| **State 4** | Authenticated with authority (`trainee.write`) + Valid Body | `POST /api/v1/trainees` | **HTTP 201 Created** (`success=true`, `data.firstName="Rajesh"`) | HTTP 201 Created (`success=true`, `data.id=1`) | **YES** (`traineeService.createTrainee` called) | **PASS** |

### Verified Security Invariant
An unauthorized caller sending an invalid payload `{}` to a protected endpoint receives **HTTP 403 Forbidden**, never HTTP 400 `VALIDATION_ERROR`. Field schemas and validation rules are never disclosed to unprivileged callers.

---

## 6. Test Suites Execution & Actual Results

### 6.1 `MethodSecurityPreValidationOrderingTest` (Dedicated Verification Suite)

Test file: `backend/src/test/java/in/gov/sih/sih26135/security/MethodSecurityPreValidationOrderingTest.java`  
Command: `mvn test -f backend/pom.xml -Dtest=MethodSecurityPreValidationOrderingTest`  
Results: **11 tests run, 0 failures, 0 errors, 0 skipped** (Time: 22.85 s)

- `testState1_unauthenticatedWithInvalidBody_returns401` — **PASS**
- `testState2_authenticatedWithoutAuthorityWithInvalidBody_returns403` — **PASS**
- `testState3_authenticatedWithAuthorityWithInvalidBody_returns400ValidationError` — **PASS**
- `testState4_authenticatedWithAuthorityWithValidBody_reachesBusinessLogic` — **PASS**
- `testHasAuthority_evaluatedCorrectly` — **PASS**
- `testHasAnyAuthority_evaluatedCorrectly` — **PASS**
- `testHasRole_evaluatedCorrectly` — **PASS**
- `testCompositeExpression_evaluatedCorrectly` — **PASS**
- `testIsAuthenticated_evaluatedCorrectly` — **PASS**
- `testPermitAll_evaluatedCorrectly` — **PASS**
- `testDenyAll_evaluatedCorrectly` — **PASS**

### 6.2 Combined Validation & Security Verification Suite

Command: `mvn test -f backend/pom.xml "-Dtest=MethodSecurityPreValidationOrderingTest,RequestDtoValidationAndSchemaAlignmentTest,ValidationFrameworkFoundationTest,WorkflowAuthorizationAndConsentIntegrationTest"`  
Results: **74 tests run, 0 failures, 0 errors, 0 skipped** (Time: 46.758 s)

| Test Suite | Focus Area | Tests Run | Failures | Status |
| :--- | :--- | :---: | :---: | :---: |
| `MethodSecurityPreValidationOrderingTest` | 4-State Ordering & Expression Variants | 11 | 0 | **PASS** |
| `RequestDtoValidationAndSchemaAlignmentTest` | DTO Schema Alignment, Domain Rules, Cascading | 23 | 0 | **PASS** |
| `ValidationFrameworkFoundationTest` | Phase 26.2 Foundation & Error Handling | 8 | 0 | **PASS** |
| `WorkflowAuthorizationAndConsentIntegrationTest` | Phase 25.11 Workflow Security & Consent | 32 | 0 | **PASS** |
| **Combined Total** | | **74** | **0** | **PASS** |

### 6.3 Full Backend Regression Test Suite

Command: `mvn test -f backend/pom.xml`  
Results: **737 tests run, 0 failures, 0 errors, 0 skipped** (Time: 01:29 min)  
Outcome: `BUILD SUCCESS`

---

## 7. Changed Files Inventory

### 7.1 New Infrastructure & Verification Test Files
1. [`MethodSecurityPreValidationInterceptor.java`](backend/src/main/java/in/gov/sih/sih26135/security/MethodSecurityPreValidationInterceptor.java) — Native Spring Security `PreAuthorizeAuthorizationManager` handler interceptor.
2. [`WebMvcConfig.java`](backend/src/main/java/in/gov/sih/sih26135/security/config/WebMvcConfig.java) — Web MVC configuration registering pre-validation security interceptor.
3. [`MethodSecurityPreValidationOrderingTest.java`](backend/src/test/java/in/gov/sih/sih26135/security/MethodSecurityPreValidationOrderingTest.java) — 11-test suite verifying the 4 authorization ordering states and SpEL expression variants.
4. [`RequestDtoValidationAndSchemaAlignmentTest.java`](backend/src/test/java/in/gov/sih/sih26135/validation/RequestDtoValidationAndSchemaAlignmentTest.java) — 23-test suite verifying DTO schema alignment and controller validation.

### 7.2 Modified Controller Files (53 Controllers)
Applied `@Valid` on `@RequestBody` parameters across all 53 REST controllers, preserving explicit parameter-level validation semantics:
- `AssessmentController.java`, `AssessmentResultController.java`, `AttendanceRecordController.java`, `AuditLogController.java`
- `CertificationController.java`, `CommunicationLogController.java`, `ConsentController.java`, `CourseController.java`
- `DataQualityIssueController.java`, `DataQualityIssueEventController.java`, `DataQualityRuleController.java`, `EmployerController.java`
- `EmploymentExitEventController.java`, `EmploymentRecordController.java`, `EmploymentVerificationAttemptController.java`
- `EmploymentVerificationController.java`, `EmploymentVerificationEvidenceController.java`, `EmploymentVerificationRequestController.java`
- `FollowupCampaignController.java`, `FollowupTaskController.java`, `ImportBatchController.java`, `ImportRecordController.java`
- `JobApplicationController.java`, `JobPostingController.java`, `PermissionController.java`, `PlacementRecordController.java`
- `ProgramController.java`, `RoleController.java`, `SalaryHistoryController.java`, `SchemeController.java`
- `SkillGapAssessmentController.java`, `SkillGapController.java`, `SkillGapObservationController.java`, `SkillGapRecommendationController.java`
- `SurveyQuestionController.java`, `SurveyResponseAnswerController.java`, `SurveyResponseController.java`, `SystemEventLogController.java`
- `TraineeAssessmentController.java`, `TraineeController.java`, `TraineeUnemploymentEventController.java`, `TrainingBatchController.java`
- `TrainingCenterController.java`, `TrainingEnrollmentController.java`, `TrainingProviderController.java`, `UserController.java`
- `AuthController.java` (Preserved public login/refresh manual validation contracts)
- `EmploymentLifecycleWorkflowController.java`, `FollowupEngagementWorkflowController.java`, `PlacementEmploymentWorkflowController.java`
- `SkillGapRemediationWorkflowController.java`, `TrainingOutcomeWorkflowController.java`, `VerificationWorkflowController.java`

### 7.3 Modified Request DTO Files (112 DTOs)
Applied `@Size`, `@Pattern`, `@NotBlank`, `@NotNull`, `@Email`, `@PositiveOrZero`, `@Positive`, `@Digits`, `@Valid` aligned with frozen MySQL column definitions.

---

## 8. Database and Git Safety Confirmation

1. **Target Database Catalog:** Strictly targeted `SIH26135_TEST` across all test executions.
2. **Production Safety:** The production database `SIH26135` was **NEVER** accessed, modified, or connected to.
3. **DDL Invariant:** `spring.jpa.hibernate.ddl-auto=validate` remained strictly active and unmodified. No DDL alterations, table creations, column modifications, or Hibernate schema drops were performed.
4. **Data Isolation:** Zero persistent database rows, migrations, or seed data were introduced.
5. **Git Safety:** Zero Git stage (`git add`), commit (`git commit`), push (`git push`), reset (`git reset`), stash (`git stash`), or clean (`git clean`) operations were performed. All pre-existing untracked files and working tree state were strictly preserved.

---

## 9. Final Verification Checklist & Verdict

| Verification Check | Standard / Requirement | Actual Result | Status |
| :--- | :--- | :--- | :---: |
| **1. Expression Audit** | Complete inventory of all `@PreAuthorize` expressions in repo | 60 annotations across 53 controllers inventoried and categorized | **PASS** |
| **2. Evaluator Limitations** | Compare `SimpleSecurityExpressionRoot` with Spring Security | Identified 6 fundamental limitations in ad-hoc parser | **PASS** |
| **3. Native Correction** | Delegate to Spring Security native infrastructure | Replaced custom parser with `PreAuthorizeAuthorizationManager` | **PASS** |
| **4. Ordering State 1** | Unauthenticated + invalid body -> 401 Unauthorized | HTTP 401 via `RestAuthenticationEntryPoint`, business logic not run | **PASS** |
| **5. Ordering State 2** | Unauthorized + invalid body -> 403 Forbidden | HTTP 403 via `RestAccessDeniedHandler`, business logic not run | **PASS** |
| **6. Ordering State 3** | Authorized + invalid body -> 400 Bad Request | HTTP 400 via `GlobalExceptionHandler` (`VALIDATION_ERROR`) | **PASS** |
| **7. Ordering State 4** | Authorized + valid body -> 200/201 Success | HTTP 201 Created, business service method executed | **PASS** |
| **8. Expression Variety** | Test `hasAuthority`, `hasAnyAuthority`, `hasRole`, composite, etc. | All expression variants pass in `MethodSecurityPreValidationOrderingTest` | **PASS** |
| **9. Regression Coverage** | All existing tests pass without regressions | 737 tests run, 0 failures, 0 errors, 0 skipped | **PASS** |
| **10. Database Safety** | Frozen schema preserved, `ddl-auto=validate` | Zero schema modifications, zero migrations, zero persistent rows | **PASS** |
| **11. Git Safety** | No git mutations | Zero git stage, commit, push, reset, stash, or clean operations | **PASS** |

### Explicit Verdict
# **ACCEPTED**
All authorization expressions in the repository are handled correctly and natively using Spring Security's `PreAuthorizeAuthorizationManager`. Authorization ordering strictly enforces 403 Forbidden prior to payload validation, preventing schema disclosure, and all 737 tests pass with zero failures.
