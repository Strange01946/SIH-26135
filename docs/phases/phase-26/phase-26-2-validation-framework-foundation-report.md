# Phase 26.2 — Validation Framework Foundation Report

**Project:** SIH-26135  
**Repository:** `E:\project`  
**Backend Location:** `E:\project\backend`  
**Branch:** `main`  
**HEAD Commit:** `d538c173be9ff5bbd03dcaa996cdd014baedf0d1`  
**Phase Date:** 2026-10-27  
**Target Test Database:** `SIH26135_TEST` (MySQL 8.0.46, 144 base tables, 21 views, `spring.jpa.hibernate.ddl-auto=validate`)  
**Production Database Boundary:** `SIH26135` (Untouched, Zero connections, Zero queries, Zero mutations)  
**Report Status:** `READY FOR INDEPENDENT REVIEW`  

---

## 1. Executive Summary and Phase Verdict

Phase 26.2 establishes the foundational Bean Validation infrastructure for the SIH-26135 Spring Boot 3.4.10 backend. Building directly upon the findings of the Phase 26.1 audit, this phase delivers a robust, secure, and production-ready validation pipeline without prematurely applying validation rules across all request DTOs or introducing breaking changes.

### Key Deliverables Completed

1. **Explicit Validation Dependency:**
   - Added `org.springframework.boot:spring-boot-starter-validation` as an explicit dependency in `backend/pom.xml`, managed via Spring Boot parent BOM.
   - Verified clean resolution: Hibernate Validator `8.0.3.Final` and Jakarta Validation API `3.0.2`.

2. **Centralized Exception Handling & Sanitization:**
   - Enhanced `GlobalExceptionHandler.java` to handle:
     - `MethodArgumentNotValidException` (DTO body validation via `@Valid @RequestBody`), supporting both field errors (`FieldError`) and object/class-level errors (`ObjectError`).
     - `HandlerMethodValidationException` (Spring 6.1/6.2 parameter validation via `getValueResults()` and `getBeanResults()`, with dynamic reflection parameter name discovery).
     - `ConstraintViolationException` (raw constraint violation fallback).
   - Enforced **deterministic alphabetical sorting** across violation details and invalid parameter/field lists.
   - Preserved **anti-echo sanitization** and sensitive data masking (`SENSITIVE_DATA_PATTERN`, `isSafeStandardValidationMessage`) to prevent secret or raw payload leakage.
   - Standardized the response contract to HTTP 400 Bad Request, code `VALIDATION_ERROR`, message `"Request validation failed"`.

3. **Controller Boundary Activation Guidelines:**
   - Established the standard pattern for activating request validation: `@Valid @RequestBody <DtoClass> request`.
   - Documented explicit guidance against prematurely annotating existing controllers with class-level `@Validated` to protect existing proxy semantics and endpoint behavior.

4. **Comprehensive Test Suite & Empirical Verification:**
   - Created `ValidationFrameworkFoundationTest.java` (8 tests) verifying:
     - Application context Bean Validation provider initialization.
     - Controller-boundary rejection of invalid payloads before business logic executes.
     - Execution of controller business logic when payloads are valid.
     - Deterministic alphabetical sorting of multiple validation violations.
     - Anti-echo sanitization and masking of sensitive tokens.
     - Mapping of Spring 6.2 `HandlerMethodValidationException` to HTTP 400 `VALIDATION_ERROR`.
     - Preservation of Spring Security authentication (`401`) and authorization (`403`) boundaries.
     - Preservation of non-validation error mappings (e.g., malformed JSON mapping to `MALFORMED_JSON`).
   - Verified that all 89 existing exception-handling regression tests (`ApiLevelExceptionHandlerTest` and `GlobalExceptionHandlerErrorMappingTest`) pass without modification.
   - Executed full backend test suite: **653 tests executed, 653 passed, 0 failures, 0 errors, 0 skipped** (up from 645 baseline, +8 new tests).

5. **Integrity Boundaries:**
   - Database schema untouched (`spring.jpa.hibernate.ddl-auto=validate` confirmed passing against `SIH26135_TEST`).
   - Production database `SIH26135` strictly isolated (zero connections).
   - Git working tree remains unstaged and uncommitted.

### Phase Verdict
**READY FOR INDEPENDENT REVIEW.** All validation framework foundation objectives are fulfilled with zero regressions and complete empirical test proof.

---

## 2. Repository and Git Baseline

- **Repository Root:** `E:\project`
- **Backend Directory:** `E:\project\backend`
- **Git Branch:** `main`
- **HEAD Commit:** `d538c173be9ff5bbd03dcaa996cdd014baedf0d1`
- **Working Tree Cleanliness:**
  - `git diff --check` passed with zero errors or whitespace violations.
  - Zero staged changes (`Changes to be committed: none`).
  - Zero commits or pushes executed.

### Git Working Tree Status
```
Changes not staged for commit:
	modified:   backend/pom.xml
	modified:   backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java
	modified:   backend/src/main/java/in/gov/sih/sih26135/service/impl/SkillGapAssessmentServiceImpl.java
	modified:   backend/src/main/java/in/gov/sih/sih26135/service/impl/SkillGapObservationServiceImpl.java
	modified:   backend/src/main/java/in/gov/sih/sih26135/service/impl/SkillGapRecommendationServiceImpl.java
	modified:   backend/src/main/java/in/gov/sih/sih26135/service/impl/SkillGapServiceImpl.java
	modified:   backend/src/test/java/in/gov/sih/sih26135/workflow/WorkflowTransactionRollbackIntegrationTest.java

Untracked files:
	backend/src/test/java/in/gov/sih/sih26135/validation/
	phase-25-15-workflow-implementation-review-freeze-report.md
	phase-26-1-api-validation-pagination-audit-report.md
	phase-26-2-validation-framework-foundation-report.md
```

---

## 3. Dependency Architecture & Resolution

### Explicit Dependency Declaration
In `backend/pom.xml`, `spring-boot-starter-validation` was added under `<dependencies>`, directly following `spring-boot-starter-web`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### BOM Version Governance
- No explicit `<version>` tag is declared in `backend/pom.xml`.
- Version resolution is managed by `spring-boot-starter-parent` (`3.4.10`).
- Maven dependency resolution confirms:
  ```
  [INFO] +- org.springframework.boot:spring-boot-starter-validation:jar:3.4.10:compile
  [INFO] |  +- org.apache.tomcat.embed:tomcat-embed-el:jar:10.1.47:compile
  [INFO] |  \- org.hibernate.validator:hibernate-validator:jar:8.0.3.Final:compile
  [INFO] |     +- jakarta.validation:jakarta.validation-api:jar:3.0.2:compile
  [INFO] |     +- org.jboss.logging:jboss-logging:jar:3.6.1.Final:compile
  [INFO] |     \- com.fasterxml:classmate:jar:1.7.0:compile
  ```
- **Context Presence:** In `ValidationFrameworkFoundationTest`, the `jakarta.validation.Validator` bean was asserted to be present in the Spring application context, backed by `org.hibernate.validator.internal.engine.ValidatorImpl`.

---

## 4. Controller Boundary Validation Activation Pattern & Guidelines

### Request Body Validation Activation Pattern
For incoming JSON request payloads bound to DTOs via `@RequestBody`, Bean Validation is activated by placing Jakarta's `@Valid` annotation on the method parameter:

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<TraineeResponse>> createTrainee(
    @Valid @RequestBody CreateTraineeRequest request) {
  // Controller business logic executes only if validation passes
  return ResponseEntity.status(HttpStatus.CREATED)
      .body(ApiResponse.success(traineeService.createTrainee(request)));
}
```

### Method Parameter Validation Pattern (Spring 6.1/6.2)
In Spring Boot 3.4 / Spring Framework 6.2, method parameter validation on `@PathVariable` or `@RequestParam` can trigger `HandlerMethodValidationException`:
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<TraineeResponse>> getTraineeById(
    @PathVariable("id") @Positive(message = "id must be greater than 0") Long id) {
  return ResponseEntity.ok(ApiResponse.success(traineeService.getTraineeById(id)));
}
```

### Architectural Guardrails & Phase 26.5 Boundary
1. **No Indiscriminate Class-Level `@Validated`:**
   - Adding `@Validated` at the controller class level creates an AOP proxy (`MethodValidationPostProcessor`) around the controller bean. If controllers also use `@PreAuthorize` or custom proxies, this can alter proxy types (CGLIB vs JDK dynamic proxies) or impact existing reflection-based inspection.
   - Therefore, class-level `@Validated` must NOT be blindly added across existing controllers in Phase 26.2.
2. **Decoupling Foundation from DTO Rollout:**
   - In accordance with Phase 26 design, DTO-specific validation annotations (`@NotBlank`, `@Size`, `@Pattern`, `@NotNull`) are scheduled for systematic implementation in **Phase 26.5**.
   - Phase 26.2 establishes the framework, exception handlers, and security boundaries so that subsequent subphases can activate validation cleanly without unexpected runtime exceptions.

---

## 5. Centralized Validation Error Handling Architecture

`GlobalExceptionHandler.java` was augmented to provide centralized, secure handling for all validation failure modes.

### 1. `MethodArgumentNotValidException` Handling
Activated when `@Valid @RequestBody <Dto>` fails validation.
- Extracts `FieldError` list and `ObjectError` (global/class-level errors).
- Passes field errors through `sanitizeFieldError(FieldError)`.
- Passes global errors through newly implemented `sanitizeObjectError(ObjectError)`.
- Sorts `validationErrors` details alphabetically to guarantee deterministic JSON output across environments.
- Sorts `invalidFields` alphabetically.
- Returns `ErrorResponse` (HTTP 400 Bad Request, code `VALIDATION_ERROR`, message `"Request validation failed"`).

### 2. `HandlerMethodValidationException` Handling
Spring Framework 6.1/6.2 standard exception for controller method parameter validation.
- Uses non-deprecated Spring 6.2 APIs: `ex.getValueResults()` (for method parameter constraints) and `ex.getBeanResults()` (for cascading bean parameters).
- Handles parameter name discovery: if `MethodParameter.getParameterName()` is null (such as when reflection metadata has not been resolved), invokes `result.getMethodParameter().initParameterNameDiscovery(new StandardReflectionParameterNameDiscoverer())`.
- For each resolvable error:
  - If `FieldError`, delegates to `sanitizeFieldError()`.
  - If `ObjectError`, delegates to `sanitizeObjectError()`.
  - Otherwise, delegates to newly implemented `sanitizeParameterError(paramName, resolvable, rejectedValue)`.
- Sorts `validationErrors` and `invalidParams` alphabetically.
- Returns identical `ErrorResponse` contract (HTTP 400 Bad Request, code `VALIDATION_ERROR`).

### 3. `ConstraintViolationException` Handling
Handles constraint violations emanating from repository, service, or standalone validator calls.
- Distinguishes request parameter violations from internal state violations.
- Sorts `validationErrors` and `invalidProperties` alphabetically.
- Returns identical `ErrorResponse` contract (HTTP 400 Bad Request, code `VALIDATION_ERROR`).

### 4. Anti-Echo Sanitization & Sensitive Data Masking
- Evaluates field names and error messages against `SENSITIVE_DATA_PATTERN` (password, secret, token, apiKey, authorization, cvv, ssn, pan, aadhar).
- Evaluates error messages against `SAFE_STANDARD_VALIDATION_PATTERN`.
- If a message contains non-standard text, raw input echoes, or sensitive tokens, it is masked to a safe, generic fallback (e.g., `"email: is invalid"` or `"field: must be a well-formed email address"`), preventing reflected XSS or secret disclosure.

---

## 6. Test Verification & Empirical Evidence

### Dedicated Framework Integration Tests
`in.gov.sih.sih26135.validation.ValidationFrameworkFoundationTest` was authored and executed:

| Test ID | Method Name | Description | Result |
| :--- | :--- | :--- | :---: |
| 1 | `testValidationDependencyAndContextInitialization` | Verifies `jakarta.validation.Validator` is instantiated and backed by Hibernate Validator | **PASSED** |
| 2 | `testControllerBoundary_invalidPayload_rejectsBeforeBusinessLogic` | Verifies HTTP 400 rejection and ensures controller business logic is never invoked | **PASSED** |
| 3 | `testControllerBoundary_validPayload_executesBusinessLogic` | Verifies valid payload passes validation and executes controller business logic | **PASSED** |
| 4 | `testMultipleViolations_produceDeterministicSortedDetails` | Asserts exact complete alphabetically ordered details array and verifies identical repeatability across duplicate requests | **PASSED** |
| 5 | `testValidationSanitization_masksSensitiveTokensAndRejectedEchoes` | Verifies sentinel tokens (`SENTINEL_TOKEN_SECRET_9999_EXPOSED`) are masked | **PASSED** |
| 6 | `testHandlerMethodValidationException_mapsToHttp400` | Verifies Spring 6.2 `HandlerMethodValidationException` maps to HTTP 400 `VALIDATION_ERROR` | **PASSED** |
| 7 | `testSecurityAndAuthorization_preserved` | Verifies unauthenticated (401) and unauthorized (403) security boundaries remain intact | **PASSED** |
| 8 | `testNonValidationErrors_preserved` | Verifies malformed JSON continues mapping to HTTP 400 with code `MALFORMED_JSON` | **PASSED** |

#### Exact and Repeatable Ordering Assertion
In Test 4 (`testMultipleViolations_produceDeterministicSortedDetails`), the `details` JSON array is extracted directly from the response and asserted against the complete, exact expected alphabetically sorted list:
1. `"age: age must be greater than 0"`
2. `"age: must be greater than or equal to 18"`
3. `"code: code must not be blank"`
4. `"email: must be a well-formed email address"`

The assertion verifies the entire list in exact sequence (`containsExactlyElementsOf`) rather than checking for individual substring occurrences. The test then repeats the identical invalid request and asserts that the resulting ordered `details` array is identical, proving 100% deterministic repeatability without relying on incidental JSON string formatting.

### Exception Handling Regression Suite
Targeted execution of existing exception tests:
- `ApiLevelExceptionHandlerTest`: 82 tests, 0 failures, 0 errors.
- `GlobalExceptionHandlerErrorMappingTest`: 7 tests, 0 failures, 0 errors.
- **Combined:** 89 tests, 100% passing.

### Full Backend Suite Execution
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 653, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:15 min
[INFO] Finished at: 2026-10-27T16:47:55+05:30
```
- **Test Count Progression:** 645 tests (Phase 26.1 baseline) -> **653 tests** (Phase 26.2 completion).
- **Net Increase:** +8 tests, 0 regressions, 0 skipped tests.

---

## 7. Database & Frozen Schema Integrity

- **Isolated Test Execution:** All database-backed tests connected exclusively to `SIH26135_TEST` on MySQL 8.0.46. Catalog isolation verified via `MysqlConnectionTest.assertDedicatedTestCatalog()`.
- **Production Isolation:** Zero connections, queries, or writes to `SIH26135`.
- **Frozen Schema Compliance:** Schema validation (`spring.jpa.hibernate.ddl-auto=validate`) passed without warning across all 144 base tables and 21 views.

---

## 8. Git Hygiene & Changes Summary

### Modified Files (Tracked)
1. [`backend/pom.xml`](file:///E:/project/backend/pom.xml#L29-L32): Added explicit `spring-boot-starter-validation` dependency.
2. [`backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java#L175-L445): Added `HandlerMethodValidationException` handler, `sanitizeObjectError`, `sanitizeParameterError`, and deterministic sorting for validation details.
3. Prior Phase 25.15 files preserved in working tree (`SkillGap*ServiceImpl.java`, `WorkflowTransactionRollbackIntegrationTest.java`).

### Created Files (Untracked)
1. [`backend/src/test/java/in/gov/sih/sih26135/validation/ValidationFrameworkFoundationTest.java`](file:///E:/project/backend/src/test/java/in/gov/sih/sih26135/validation/ValidationFrameworkFoundationTest.java): Comprehensive foundation test suite.
2. `phase-26-2-validation-framework-foundation-report.md`: Phase 26.2 completion report.

---

## 9. Forward Compatibility & Phase 26 Rollout Blueprint

The validation framework is now fully operational and ready to support subsequent subphases:
- **Phase 26.3 — Pagination & Sorting Foundation:** Define `PagedResponse<T>`, `PageMetadata`, and pagination query parameters.
- **Phase 26.4 — Standardized Error & Correlation ID Architecture:** Add correlation ID tracking (`X-Correlation-Id`) to request filters, `ApiResponse`, and `ErrorResponse`.
- **Phase 26.5 — Request DTO Validation Rollout:** Apply targeted Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@Positive`) across the 116 request DTOs identified in Phase 26.1.
- **Phase 26.6 — Controller Parameter Validation Rollout:** Apply validation annotations to controller path variables and request parameters.
- **Phase 26.7 — Repository Pagination & Sorting Implementation:** Update repository methods to return `Page<T>` and refactor unbounded collection endpoints.
- **Phase 26.8 — Phase 26 Comprehensive Review & Verification Freeze:** Full suite verification, regression freeze, and documentation sign-off.
