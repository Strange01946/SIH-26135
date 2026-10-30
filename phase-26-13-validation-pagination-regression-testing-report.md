# Phase 26.13 — Validation & Pagination Regression Testing Completion Report

**Date:** October 30, 2026  
**System:** SIH-26135 Centralized Placement & Tracking System  
**Module:** `sih26135-backend` (Spring Boot 3.4.10 / Java 17)  
**Database Target:** `SIH26135_TEST` (`spring.jpa.hibernate.ddl-auto=validate`)  
**Verdict:** `READY FOR INDEPENDENT REVIEW`

---

## 1. Executive Summary & Phase Verdict

Phase 26.13 implements comprehensive automated regression tests across the SIH-26135 Spring Boot backend for:
- Request DTO Bean Validation (valid, missing, blank, min, max, patterns, cross-field sequences, and custom validators).
- Pagination boundaries, limits, and zero-based page navigation.
- Sorting allowlists, default behaviors, explicit ASC/DESC directions, case normalization, and fail-closed sensitive sort term suppression.
- Malformed request payloads, missing bodies, and type conversion errors.
- Centralized error response envelopes (`ErrorResponse`) and dual correlation headers (`X-Correlation-Id`, `X-Request-Id`).
- Non-execution of controller business logic when request validation or binding fails.

### Key Metrics Summary
- **Historical Baseline Test Count:** 645 tests (Phase 26.1 baseline).
- **Pre-Phase Test Count:** 977 tests (Phase 26.12 completion).
- **New Regression Tests Added:** **45 dedicated tests** in `ValidationAndPaginationRegressionTest.java`.
- **Current Full Test Suite Count:** **1,022 tests**.
- **Test Execution Result:** **1,022 tests run, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS`).
- **Production Code Changes:** **0 lines modified** (existing Bean Validation, `PaginationUtils`, and `GlobalExceptionHandler` implementations were empirically verified as 100% compliant).
- **Database Safety:** All tests executed strictly against dedicated catalog `SIH26135_TEST`; production database `SIH26135` had zero connections or access.
- **Git Working Tree:** Clean, unstaged, uncommitted (`git diff --check` passed cleanly).

### Verdict
**`READY FOR INDEPENDENT REVIEW`** — All 45 newly added regression tests and all 977 prior regression tests pass with 100% success against `SIH26135_TEST`.

---

## 2. Initial Working-Tree State & Reconnaissance Findings

### 2.1 Initial Working-Tree State
Prior to test authoring, the working tree was verified on branch `main` at commit `55a3e1a Complete Phase 26.12 response envelope alignment and backward compatibility`:
- `git status --short`: Empty (working tree clean).
- No uncommitted, stashed, or unstaged modifications existed.

### 2.2 Framework & Dependency Verification
- **Spring Boot Version:** `3.4.10`
- **Java Version:** `17` (OpenJDK 17.0.19)
- **Bean Validation Engine:** Hibernate Validator `8.0.3.Final` / Jakarta Validation API `3.0.2` via `org.springframework.boot:spring-boot-starter-validation`.
- **Database Schema Validation:** `spring.jpa.hibernate.ddl-auto=validate` targeting `SIH26135_TEST`.

### 2.3 Existing Validation & Pagination Architecture Audited
1. **Controller Activation:** All 55 production REST controllers are annotated with `@Validated` (enforcing path, query, and header constraints), while request bodies are validated at the parameter boundary using `@Valid @RequestBody <DtoClass> request`.
2. **Centralized Error Handling:** `GlobalExceptionHandler.java` catches:
   - `MethodArgumentNotValidException` (DTO body validation) -> HTTP 400 Bad Request, code `VALIDATION_ERROR`, message `"Request validation failed"`, with deterministically sorted `details`.
   - `HandlerMethodValidationException` (parameter validation) -> HTTP 400 with specialized codes `INVALID_PAGE`, `INVALID_PAGE_SIZE`, or `VALIDATION_ERROR`.
   - `MethodArgumentTypeMismatchException` (type conversion failures) -> HTTP 400 with code `TYPE_MISMATCH` and safe message indicating expected type.
   - `HttpMessageNotReadableException` (malformed JSON, unparseable date/time, missing body) -> HTTP 400 with code `MALFORMED_JSON` and sanitized diagnostic details.
   - `BadRequestException` (thrown by `PaginationUtils` for sort/page boundary violations) -> HTTP 400 with codes `INVALID_PAGE`, `INVALID_PAGE_SIZE`, `INVALID_SORT_FIELD`, `INVALID_SORT_DIRECTION`.
3. **Sensitive Data Masking (Phase 26.9):** Any exception message containing sensitive terms (such as "password", "token", "secret") is automatically masked to `"The request is invalid"` to prevent credential or token reflection.
4. **Pagination Contract (`PaginationUtils.java`):**
   - Zero-based page indexing (`MIN_PAGE = 0`, `DEFAULT_PAGE = 0`).
   - Page size bounds: `MIN_SIZE = 1`, `DEFAULT_SIZE = 20`, `MAX_SIZE = 100`.
   - Bounded sort parameters: `MAX_SORT_FIELD_LENGTH = 64`, `MAX_SORT_DIRECTION_LENGTH = 16`.
   - Sensitive sort term filter: Fail-closed rejection of terms matching `password`, `secret`, `token`, `vault`, etc., returning generic message `"Invalid sort field provided"`.
   - Secondary tie-breaker: Automatically appends `id ASC` to primary sort for deterministic ordering across pagination windows.

---

## 3. DTOs and Endpoints Covered by Regression Tests

| # | Request DTO | Tested Endpoint | HTTP Method | Validation Constraints Verified |
|---|---|---|---|---|
| 1 | `LoginRequest` | `/api/v1/auth/login` | POST | `@NotBlank` on `usernameOrEmail`, `@NotBlank` on `password`, password anti-echo, controller `BAD_REQUEST` handling. |
| 2 | `CreateCourseRequest` | `/api/v1/courses` | POST | `@NotBlank`, `@Size(max=32)` on `courseCode`, `@NotBlank`, `@Size(max=200)` on `courseName`, `@PositiveOrZero` on `durationHours`, boundary values (32 chars, 0 hours). |
| 3 | `CreateEmployerRequest` | `/api/v1/employers` | POST | `@Email` on `contactEmail`, `@Pattern` on `contactPhone` (10-15 digits), `@Pattern` on `pincode` (6 digits not starting with 0), valid boundary inputs. |
| 4 | `CreateTrainingBatchRequest` | `/api/v1/training-batches` | POST | `@DateSequence(startField="startDate", endField="endDate")` cross-field sequence, `@Positive` on `capacity`. |
| 5 | `CreateJobPostingRequest` | `/api/v1/job-postings` | POST | `@NumberRange(minField="minSalary", maxField="maxSalary")`, `@DateSequence(startField="postedDate", endField="closingDate")`. |
| 6 | `CreateTraineeRequest` | `/api/v1/trainees` | POST | `@ValidDateOfBirth` custom validator rejecting future dates of birth and underage (<15 years) trainees. |
| 7 | Core Pagination | `/api/v1/courses` | GET | `page`, `size`, `sort`, `direction`, bounds (`0 <= page`, `1 <= size <= 100`), non-integer inputs, out-of-bounds page, empty page. |
| 8 | Path Parameter | `/api/v1/courses/{id}` | GET | `@Positive Long id`, non-numeric conversion (`TYPE_MISMATCH`), negative integer constraint (`VALIDATION_ERROR`). |

---

## 4. Test Scenarios Added & Rationale

The newly added test suite [`ValidationAndPaginationRegressionTest.java`](backend/src/test/java/in/gov/sih/sih26135/validation/ValidationAndPaginationRegressionTest.java) contains 45 focused test cases grouped into 5 nested classes:

### 4.1 DTO Request Body Bean Validation (15 Tests)
1. `testLoginRequest_blankUsernameRejected`: Verifies blank `usernameOrEmail` is rejected with HTTP 400 `BAD_REQUEST`.
2. `testLoginRequest_blankPasswordRejected`: Verifies blank `password` is rejected with HTTP 400 `BAD_REQUEST` and sanitized message `"The request is invalid"`.
3. `testLoginRequest_dtoBeanValidationViolations`: Tests Jakarta `Validator` directly on `LoginRequest("", "")`, confirming 2 violations on `usernameOrEmail` and `password`.
4. `testLoginRequest_passwordNeverEchoedInError`: Verifies that sensitive passwords submitted in invalid requests are strictly masked and never echoed in the response envelope.
5. `testCreateCourseRequest_missingRequiredFieldsRejected`: Verifies missing required fields fail controller validation and trigger HTTP 400 `VALIDATION_ERROR`.
6. `testCreateCourseRequest_multipleViolationsAlphabeticalOrder`: Confirms that 3 simultaneous violations (`courseCode`, `courseName`, `durationHours`) are returned in deterministic alphabetical order.
7. `testCreateCourseRequest_oversizedCourseCodeRejected`: Rejects `courseCode` with 33 characters (max 32).
8. `testCreateCourseRequest_negativeDurationHoursRejected`: Rejects negative `durationHours` via `@PositiveOrZero`.
9. `testCreateCourseRequest_boundaryValuesAccepted`: Proves boundary inputs (`courseCode` exactly 32 chars, `durationHours = 0`) pass Bean Validation cleanly.
10. `testCreateEmployerRequest_invalidPatternsRejected`: Rejects invalid email (`"not-an-email"`), short phone (`"123"`), and leading-zero pincode (`"012345"`).
11. `testCreateEmployerRequest_validPatternBoundariesAccepted`: Confirms valid patterns (`"9876543210"`, `"400001"`, valid email) pass validation.
12. `testCreateTrainingBatchRequest_invertedDatesRejected`: Verifies `@DateSequence` cross-field validator rejects `endDate` preceding `startDate`.
13. `testCreateTrainingBatchRequest_nonPositiveCapacityRejected`: Verifies `@Positive` rejects `capacity = 0`.
14. `testCreateJobPostingRequest_invertedSalaryAndDatesRejected`: Rejects `maxSalary < minSalary` (`@NumberRange`) and `closingDate < postedDate` (`@DateSequence`).
15. `testCreateTraineeRequest_invalidDateOfBirthRejected`: Rejects future DOB and underage (<15 years) DOB via `@ValidDateOfBirth`.
*Controller Business Logic Guard:* Mockito `verify(courseService, never()).createCourse(any())` confirms zero service execution when request validation fails.

### 4.2 Pagination Boundaries & Limits (11 Tests)
16. `testPagination_firstPageZeroBased`: Verifies `page=0` is accepted, returning `page: 0`, `first: true`.
17. `testPagination_middlePageNavigation`: Verifies `page=1, size=2` returns `first: false`, `hasPrevious: true`.
18. `testPagination_outOfBoundsPageReturnsEmptyArray`: Verifies `page=9999` returns HTTP 200 with `content: []`, `first: false`, `last: true`, `hasNext: false`, `hasPrevious: true`.
19. `testPagination_negativePageRejected`: Verifies `page=-1` is rejected with HTTP 400 `INVALID_PAGE` and message `"Page index must not be less than zero"`.
20. `testPagination_defaultPageSizeApplied`: Verifies omitted `size` defaults to 20.
21. `testPagination_minimumPageSizeAccepted`: Verifies `size=1` is accepted.
22. `testPagination_maximumPageSizeAccepted`: Verifies `size=100` is accepted.
23. `testPagination_oversizedPageSizeRejected`: Verifies `size=101` is rejected with HTTP 400 `INVALID_PAGE_SIZE` and message `"Page size must not exceed 100"`.
24. `testPagination_zeroPageSizeRejected`: Verifies `size=0` is rejected with HTTP 400 `INVALID_PAGE_SIZE` and message `"Page size must be at least 1"`.
25. `testPagination_negativePageSizeRejected`: Verifies `size=-5` is rejected with HTTP 400 `INVALID_PAGE_SIZE`.
26. `testPagination_fullMetadataIntegrity`: Asserts the presence and types of all 9 fields (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `hasNext`, `hasPrevious`).

### 4.3 Sorting Boundaries & Security Allowlists (9 Tests)
27. `testSorting_defaultSortingApplied`: Verifies default sorting when `sort` parameter is omitted.
28. `testSorting_explicitAscSorting`: Verifies `sort=courseCode&direction=ASC` succeeds.
29. `testSorting_explicitDescSorting`: Verifies `sort=courseCode&direction=DESC` succeeds.
30. `testSorting_caseInsensitiveAndAliases`: Verifies `coursecode`, `COURSECODE`, `course_code`, and `courseCode` aliases resolve cleanly.
31. `testSorting_unsupportedSortPropertyRejected`: Verifies non-allowlisted property (`unsupportedPropertyXYZ`) is rejected with HTTP 400 `INVALID_SORT_FIELD`.
32. `testSorting_sensitiveSortFieldsRejectedAntiEcho`: Rejects `passwordHash`, `secret`, `token`, `aadhaarVaultRef` fail-closed with generic message `"Invalid sort field provided"`, code `INVALID_SORT_FIELD`, without echoing the input.
33. `testSorting_unsupportedDirectionRejected`: Rejects `direction=SIDEWAYS` with HTTP 400 `INVALID_SORT_DIRECTION`.
34. `testSorting_sqlInjectionSortRejected`: Rejects SQL injection attempts (`id;DROP TABLE`, `id' OR '1'='1`, `id,asc`) with HTTP 400 `INVALID_SORT_FIELD`.
35. `testSorting_tieBreakerStability`: Verifies that `PaginationUtils` automatically attaches secondary sort on `id ASC` for stable pagination.

### 4.4 Malformed Request Bodies & Type Conversion (8 Tests)
36. `testMalformedJson_syntaxErrorReturnsMalformedJson`: Unclosed JSON string returns HTTP 400 `MALFORMED_JSON` with message `"Malformed or unreadable request body"`.
37. `testEmptyRequestBody_returnsMalformedJson`: Empty string body returns HTTP 400 `MALFORMED_JSON`.
38. `testTypeConversion_stringWhereNumberExpectedReturnsMalformedJson`: String in integer field returns HTTP 400 `MALFORMED_JSON`.
39. `testTypeConversion_nonNumericPathVariable`: Non-numeric path variable (`/api/v1/courses/not-a-number`) returns HTTP 400 `TYPE_MISMATCH` with message `"Invalid parameter value for 'id'"`.
40. `testPathVariableConstraint_negativeIdRejected`: Negative path variable (`/api/v1/courses/-1`) returns HTTP 400 `VALIDATION_ERROR`.
41. `testQueryParamTypeMismatch_nonIntegerPage`: Non-integer page (`page=abc`) returns HTTP 400 `TYPE_MISMATCH`.
42. `testQueryParamTypeMismatch_nonIntegerSize`: Non-integer size (`size=xyz`) returns HTTP 400 `TYPE_MISMATCH`.
43. `testSecurityPrivacy_noStackTraceOrInternalDetailsLeaked`: Verifies error responses never leak stack traces (`at in.gov.sih`), framework class names (`org.springframework`, `com.fasterxml`), or raw SQL statements (`SELECT`, `INSERT`).

### 4.5 API Response Envelope Contracts (2 Tests)
44. `testErrorResponseEnvelopeContract`: Confirms error envelopes serialize `status`, `error`, `code`, `message`, `path`, `timestamp`, `correlationId`, and include `X-Correlation-Id` and `X-Request-Id` headers.
45. `testPaginatedSuccessEnvelopeContract`: Confirms paginated success responses serialize `ApiResponse<PagedResponse<T>>` with dual correlation headers.

---

## 5. Files Changed & Justification

| File | Status | Scope | Justification |
|---|---|---|---|
| `backend/src/test/java/in/gov/sih/sih26135/validation/ValidationAndPaginationRegressionTest.java` | **Added** | Test Scope Only | Contains all 45 automated regression tests fulfilling Phase 26.13 requirements. |
| Production code files (`src/main/...`) | **Unchanged** | Production Scope | 0 lines changed. All production validation, pagination, and sorting behavior was verified to be strictly compliant. |

---

## 6. Execution Commands & Verification Evidence

### 6.1 Targeted Regression Suite
```powershell
mvn -f backend/pom.xml test -Dtest=ValidationAndPaginationRegressionTest
```
**Output:**
```text
[INFO] Running in.gov.sih.sih26135.validation.ValidationAndPaginationRegressionTest
...
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 24.50 s -- in Phase 26.13 Validation & Pagination Regression Testing
[INFO] Results:
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  46.528 s
[INFO] ------------------------------------------------------------------------
```

### 6.2 Full Backend Test Suite Execution
```powershell
mvn -f backend/pom.xml test
```
**Output:**
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
...
[INFO] Results:
[INFO] 
[INFO] Tests run: 1022, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  01:37 min
[INFO] Finished at: 2026-10-30T18:40:39+05:30
[INFO] ------------------------------------------------------------------------
```

### 6.3 Historical Baseline Comparison
- **Phase 26.1 Baseline:** 645 tests passing.
- **Phase 26.12 Suite:** 977 tests passing.
- **Phase 26.13 Current Suite:** **1,022 tests passing (0 failures, 0 errors, 0 skipped)**.
- **Net Increase:** +45 comprehensive regression tests.

### 6.4 Git Integrity & Diff Check
```powershell
git status --short
# Output:
# ?? backend/src/test/java/in/gov/sih/sih26135/validation/ValidationAndPaginationRegressionTest.java

git diff --check
# Output: clean (zero whitespace or syntax violations)
```
Confirmation: Zero `git add`, `git commit`, `git push`, `git reset`, `git stash`, or `git clean` commands were executed.

### 6.5 Database Catalog Safety Confirmation
- Every test in `ValidationAndPaginationRegressionTest` verified via `MysqlConnectionTest.assertDedicatedTestCatalog(catalog)` that the connected catalog is strictly `SIH26135_TEST`.
- Production database `SIH26135` was never accessed or modified.
- Hibernate `ddl-auto=validate` passed cleanly without any DDL modifications or schema mutations.

---

## 7. Remaining Limitations & Deferred Work

- **No Production Defects Discovered:** The validation framework, `PaginationUtils`, and `GlobalExceptionHandler` handled all boundary, malformed, and type-conversion conditions exactly as specified in their architecture documentation. No production changes were warranted or introduced.
- **Read-Model Workflows:** Cross-domain workflow read models remain verified under their respective Phase 25 tests.

---

## 8. Final Verdict

**`READY FOR INDEPENDENT REVIEW`**
All acceptance criteria for Phase 26.13 are completely satisfied with 1,022 passing tests and zero regressions.
