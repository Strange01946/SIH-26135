# Phase 26.8 — Global Error Handling & Exception Mapping Hardening Report

## Executive Summary

Phase 26.8 completed the production hardening of the centralized exception-handling infrastructure in `GlobalExceptionHandler.java` and `ErrorResponse.java` across the SIH-26135 Spring Boot backend. 

All standard Spring Web and Spring MVC exception pathways were audited, verified, and hardened. Key accomplishments include:
1. Hardening request-body deserialization failures (`HttpMessageNotReadableException`) to safely classify and distinguish malformed JSON syntax, invalid enum constants, invalid date/time formats, and incompatible JSON types with field-level precision, while strictly preventing information leakage (raw passwords, tokens, Jackson internal class names, line/column offsets, and stack traces).
2. Hardening parameter type mismatches (`MethodArgumentTypeMismatchException`) to simplify expected types (e.g. enum types mapped to `"valid enum value"`) and sanitize parameter names against sensitive substrings without echoing rejected values.
3. Preserving RFC 7231 / RFC 9110 standard HTTP headers:
   - Preserving the `Allow` header on HTTP 405 (`HttpRequestMethodNotSupportedException`) using framework-provided supported HTTP methods.
   - Preserving the `Accept` header on HTTP 415 (`HttpMediaTypeNotSupportedException`) using supported media types.
4. Preserving the documented HTTP 500 fail-closed classification for `MissingPathVariableException`, preventing server-side routing/mapping defects from being improperly concealed as client errors.
5. Providing full RFC 7807 semantic alignment within the established, backward-compatible `ErrorResponse` envelope (`status`, `error` [title], `message` [detail], `path` [instance], `code`, `timestamp`, `details`), while adding non-null optional RFC 7807 `type` URI support.
6. Verification across a comprehensive regression suite of **266 tests** with **0 failures and 0 errors**.

**Final Verdict:** `READY FOR INDEPENDENT REVIEW`

---

## 1. Initial Working-Tree State & Reconnaissance Findings

### 1.1 Working-Tree State
At the beginning of Phase 26.8:
- **Branch:** `main` (clean working tree after Phase 26.7 commit `d18b914`).
- **Baseline Report:** `phase-26-7-path-query-header-validation-report.md` was inspected from repository root, verifying 55 controllers under `@Validated` and Phase 26.7 path/query/header validation baseline.

### 1.2 Technology & Framework Stack
- **Spring Boot:** `3.4.10`
- **Spring Framework:** `6.2.x`
- **Java Version:** `17`
- **Jackson:** `2.18.x`
- **Bean Validation:** Jakarta Bean Validation (Hibernate Validator 8.0.x)
- **Database Catalog:** `SIH26135_TEST` exclusively (`spring.jpa.hibernate.ddl-auto=validate`), zero interaction with production catalog `SIH26135`.

---

## 2. Existing Exception Mappings: Confirmed vs. Hardened

Before modification, all existing handlers in `GlobalExceptionHandler.java` were audited against Phase 26.8 requirements:

| Exception Class | Initial State | Phase 26.8 Audit & Hardening Action | Status |
| :--- | :--- | :--- | :---: |
| `MethodArgumentNotValidException` | Handled (400 `VALIDATION_ERROR`) | Confirmed deterministic sorting and sensitive data sanitization across field and object errors. | Preserved & Verified |
| `ConstraintViolationException` | Handled (400 / 500 fail-closed) | Confirmed strict origin validation (`TRUSTED_CONTROLLER_CLASSES`, `TRUSTED_REQUEST_DTO_CLASSES`, parameter nodes). Preserved fail-closed 500 for unverified/entity/service origins and pagination contract (`INVALID_PAGE`, `INVALID_PAGE_SIZE`). | Preserved & Verified |
| `HttpMessageNotReadableException` | Handled (400 `MALFORMED_JSON`) | **Hardened:** Inspected cause chain (`InvalidFormatException`, `MismatchedInputException`, `DateTimeParseException`, missing body). Added safe classification for enums, dates, and types with field names while preventing raw input/class leakage. Preserved `MALFORMED_JSON` error code for full client compatibility. | Hardened & Verified |
| `MethodArgumentTypeMismatchException` | Handled (400 `TYPE_MISMATCH`) | **Hardened:** Added `resolveSafeTypeName` to simplify enum types to `"valid enum value"` and sanitized parameter names against sensitive substrings. Never echoes rejected values. | Hardened & Verified |
| `MissingServletRequestParameterException` | Handled (400 `MISSING_PARAMETER`) | **Hardened:** Sanitized parameter name against sensitive data patterns. | Hardened & Verified |
| `MissingRequestHeaderException` | Handled (400 `MISSING_HEADER`) | **Hardened:** Sanitized header name against sensitive data patterns. | Hardened & Verified |
| `ServletRequestBindingException` | Unhandled (fell through to 500) | **Added:** Handled as HTTP 400 `BAD_REQUEST` for generic binding/header/cookie errors not covered by specific subclasses. | Added & Verified |
| `MissingPathVariableException` | Handled (500 `INTERNAL_SERVER_ERROR`) | **Confirmed & Documented:** Maintained HTTP 500 classification. Missing path variable indicates an internal route/template configuration mismatch in Spring MVC, not a client request defect. Completely sanitized against leaking internal variable names. | Preserved & Verified |
| `HttpRequestMethodNotSupportedException` | Handled (405 `METHOD_NOT_ALLOWED`) | **Hardened:** Added preservation of standard HTTP `Allow` header populated with `ex.getSupportedHttpMethods()`. Sanitized method string. | Hardened & Verified |
| `HttpMediaTypeNotSupportedException` | Handled (415 `UNSUPPORTED_MEDIA_TYPE`) | **Hardened:** Added preservation of standard HTTP `Accept` header populated with `ex.getSupportedMediaTypes()`. Sanitized content type. | Hardened & Verified |
| `HttpMediaTypeNotAcceptableException` | Handled (406 `NOT_ACCEPTABLE`) | Confirmed safe mapping to HTTP 406. | Preserved & Verified |
| `NoResourceFoundException` | Handled (404 `ENDPOINT_NOT_FOUND`) | Confirmed safe mapping to HTTP 404. | Preserved & Verified |

---

## 3. Exception-to-Status and Error-Code Mapping Table

| Exception | HTTP Status | Error Code | Safe Message Pattern / Response Detail | Framework Headers |
| :--- | :---: | :---: | :--- | :--- |
| `MethodArgumentNotValidException` | `400 Bad Request` | `VALIDATION_ERROR` | `"Request validation failed"`, `details: ["<field>: <sanitized message>"]` | None |
| `ConstraintViolationException` (Client Parameter/DTO) | `400 Bad Request` | `VALIDATION_ERROR` | `"Request validation failed"`, `details: ["<param>: <sanitized message>"]` | None |
| `ConstraintViolationException` (Pagination single bounds) | `400 Bad Request` | `INVALID_PAGE` / `INVALID_PAGE_SIZE` | `"Page index must not be less than zero"`, `"Page size must be at least 1"` | None |
| `ConstraintViolationException` (Unverified / Entity / Service) | `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | `"An unexpected error occurred. Please try again later."` (Fail-closed) | None |
| `HttpMessageNotReadableException` (Malformed Syntax) | `400 Bad Request` | `MALFORMED_JSON` | `"Malformed or unreadable request body"` | None |
| `HttpMessageNotReadableException` (Invalid Enum in Body) | `400 Bad Request` | `MALFORMED_JSON` | `"Invalid enum value for field '<name>'"` (or in request body) | None |
| `HttpMessageNotReadableException` (Invalid Date in Body) | `400 Bad Request` | `MALFORMED_JSON` | `"Invalid date/time format for field '<name>'"` | None |
| `HttpMessageNotReadableException` (Incompatible Type) | `400 Bad Request` | `MALFORMED_JSON` | `"Invalid value format for field '<name>'"` | None |
| `HttpMessageNotReadableException` (Missing Body) | `400 Bad Request` | `MALFORMED_JSON` | `"Required request body is missing"` | None |
| `MethodArgumentTypeMismatchException` | `400 Bad Request` | `TYPE_MISMATCH` | `"Invalid parameter value for '<name>'. Expected type: <safeType>"` | None |
| `MissingServletRequestParameterException` | `400 Bad Request` | `MISSING_PARAMETER` | `"Required parameter '<name>' is missing"` | None |
| `MissingRequestHeaderException` | `400 Bad Request` | `MISSING_HEADER` | `"Required header '<name>' is missing"` | None |
| `ServletRequestBindingException` | `400 Bad Request` | `BAD_REQUEST` | `"Invalid request binding or missing parameter/header"` | None |
| `MissingPathVariableException` | `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | `"An unexpected error occurred. Please try again later."` | None |
| `HttpRequestMethodNotSupportedException` | `405 Method Not Allowed` | `METHOD_NOT_ALLOWED` | `"HTTP method '<method>' is not supported for this endpoint"` | `Allow: <methods>` |
| `HttpMediaTypeNotSupportedException` | `415 Unsupported Media Type` | `UNSUPPORTED_MEDIA_TYPE` | `"Content type '<contentType>' is not supported"` | `Accept: <mediaTypes>` |
| `HttpMediaTypeNotAcceptableException` | `406 Not Acceptable` | `NOT_ACCEPTABLE` | `"Requested media type is not acceptable"` | None |
| `NoResourceFoundException` | `404 Not Found` | `ENDPOINT_NOT_FOUND` | `"No static resource or endpoint found for path: <path>"` | None |
| `AccessDeniedException` | Re-thrown | N/A | Delegated to Spring Security (`RestAccessDeniedHandler` -> 403) | None |
| `AuthenticationException` | Re-thrown | N/A | Delegated to Spring Security (`RestAuthenticationEntryPoint` -> 401) | None |
| `DataIntegrityViolationException` (Unique Violation) | `409 Conflict` | `<DOMAIN_CONFLICT_CODE>` | Domain-specific safe constraint message (e.g. `DUPLICATE_ENROLLMENT`) | None |
| `DataIntegrityViolationException` (FK / Unrecognized) | `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | `"An unexpected database error occurred. Please try again later."` | None |
| `Exception` (Unhandled Fallback) | `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | `"An unexpected error occurred. Please try again later."` | None |

---

## 4. `ErrorResponse` Structure & RFC 7807 Compatibility Analysis

### 4.1 Current Envelope Structure
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
  private String type;             // Optional RFC 7807 URI (omitted when null)
  private LocalDateTime timestamp; // ISO-8601 timestamp extension
  private int status;              // HTTP status code (RFC 7807 'status')
  private String error;            // HTTP reason phrase (RFC 7807 'title')
  private String code;             // Machine-readable code extension
  private String message;          // Occurrence-specific message (RFC 7807 'detail')
  private String path;             // Request URI (RFC 7807 'instance')
  private List<String> details;    // Field/parameter violation list extension
}
```

### 4.2 Semantic Mapping to RFC 7807
RFC 7807 defines standard members for problem details objects:

| RFC 7807 Standard Member | SIH-26135 Envelope Field | Semantic Equivalence | Analysis & Compatibility Rationale |
| :--- | :--- | :--- | :--- |
| `status` (number) | `status` (int) | **Exact Match** | Integer HTTP status code (e.g. `400`, `404`, `405`). Always agrees with HTTP status line. |
| `title` (string) | `error` (String) | **Semantic Equivalent** | Short human-readable summary of problem type (`"Bad Request"`, `"Method Not Allowed"`). Standard Spring Boot error attribute convention. |
| `detail` (string) | `message` (String) | **Semantic Equivalent** | Human-readable explanation specific to this occurrence (`"Request validation failed"`, `"Invalid parameter value..."`). |
| `instance` (string/URI) | `path` (String) | **Semantic Equivalent** | URI reference identifying specific request occurrence (`"/api/v1/courses"`). |
| `type` (string/URI) | `type` (String) | **Direct Support** | Defaults to `null` and is omitted by `@JsonInclude(NON_NULL)`. Configurable to RFC 7807 URI (e.g. `"about:blank"` or `"https://api.sih26135.gov.in/errors/..."`). |
| *Extension Members* | `code`, `timestamp`, `details` | **Compliant Extensions** | RFC 7807 explicitly allows problem details to be extended with additional members. `code` provides machine-readable categorization; `details` provides sorted violation messages. |

### 4.3 Breaking Change Analysis & Compatibility Tradeoffs
1. **Field Renaming Tradeoff:** Renaming `error` -> `title`, `message` -> `detail`, and `path` -> `instance` would break all existing frontend clients and dozens of integration tests that assert `jsonPath("$.error")`, `jsonPath("$.message")`, and `jsonPath("$.path")`.
2. **Field Duplication Tradeoff:** Emitting both `error` and `title`, `message` and `detail`, and `path` and `instance` would create conflicting and redundant representations in the JSON payload, directly violating the instruction *"Avoid conflicting representations of the same status or message"*.
3. **Smallest Backward-Compatible Solution:** 
   - Retain the established `ErrorResponse` field names (`status`, `error`, `message`, `path`, `code`, `timestamp`, `details`).
   - Add optional RFC 7807 `type` field support (omitted when null, serialized when populated).
   - Ensure the semantic integrity of each field aligns with RFC 7807 specifications.

### 4.4 Actual Serialized Response Verification
Verification in `GlobalErrorHandlingHardeningTest.testErrorResponse_rfc7807SerializationCompatibility`:
```json
{
  "timestamp": "2026-10-28T20:08:58.212",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/courses",
  "details": [
    "code: must not be blank"
  ]
}
```
When configured with explicit `type`:
```json
{
  "type": "https://api.sih26135.gov.in/errors/resource-not-found",
  "timestamp": "2026-10-28T20:08:58.214",
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "message": "The requested resource was not found",
  "path": "/api/v1/courses/999"
}
```

---

## 5. Header Preservation Behavior

### 5.1 HTTP 405 Method Not Allowed (`Allow` Header)
- **Standard:** RFC 7231 Section 6.5.5 / RFC 9110 Section 15.5.6 requires that an origin server generating a 405 response **MUST** send an `Allow` header field containing a list of the target resource's currently supported methods.
- **Implementation:**
  ```java
  HttpHeaders headers = new HttpHeaders();
  if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
    headers.setAllow(ex.getSupportedHttpMethods());
  }
  return new ResponseEntity<>(errorResponse, headers, HttpStatus.METHOD_NOT_ALLOWED);
  ```
- **Verified Behavior:**
  Request `PATCH /api/test/hardening/resource` produces:
  - Status: `405 Method Not Allowed`
  - Header: `Allow: GET, POST`
  - Body: Uniform `ErrorResponse` with `code: "METHOD_NOT_ALLOWED"`.

### 5.2 HTTP 415 Unsupported Media Type (`Accept` Header)
- **Standard:** RFC 7231 Section 6.5.11 / RFC 9110 Section 15.5.16 allows origin servers responding with 415 to specify which media types are acceptable for request payloads using the `Accept` header.
- **Implementation:**
  ```java
  HttpHeaders headers = new HttpHeaders();
  if (ex.getSupportedMediaTypes() != null && !ex.getSupportedMediaTypes().isEmpty()) {
    headers.setAccept(ex.getSupportedMediaTypes());
  }
  return new ResponseEntity<>(errorResponse, headers, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  ```
- **Verified Behavior:**
  Request `POST /api/test/hardening/resource` with `Content-Type: application/xml` produces:
  - Status: `415 Unsupported Media Type`
  - Header: `Accept: application/json`
  - Body: Uniform `ErrorResponse` with `code: "UNSUPPORTED_MEDIA_TYPE"`.

---

## 6. Analysis of `MissingPathVariableException`

### 6.1 Spring MVC Semantics
In Spring MVC, `MissingPathVariableException` is thrown when a controller handler method parameter declares `@PathVariable("varName")`, but the URI pattern in the controller's `@RequestMapping` or `@GetMapping` **fails to declare `{varName}`**.
For example:
```java
@GetMapping("/users")
public User getUser(@PathVariable("id") Long id) { ... }
```
When a client requests `GET /users`, Spring matches the route `/users`, but when resolving arguments from URI template variables, `"id"` is missing from the template variables map.

### 6.2 Client Error vs. Server Defect Rationale
1. **Client Cannot Cause This in Valid Routes:** For a properly configured route `@GetMapping("/users/{id}")`, an HTTP request to `/users/` will either match a different handler or throw `NoResourceFoundException` (HTTP 404). The client cannot trigger `MissingPathVariableException` through normal parameter omission.
2. **Framework Alignment:** Spring Framework's own `ResponseEntityExceptionHandler` explicitly classifies `MissingPathVariableException` as `HttpStatus.INTERNAL_SERVER_ERROR` (HTTP 500).
3. **Information Disclosure Prevention:** Returning HTTP 400 with a message like `"Path variable 'internalSecretVar' is missing"` would reveal internal controller method parameter names to external callers and falsely blame the client for a server-side routing defect.
4. **Hardened Decision:** `GlobalExceptionHandler.java` logs `MissingPathVariableException` as an error (`"Server-side path variable configuration mismatch..."`) and returns sanitized HTTP 500 `INTERNAL_SERVER_ERROR` with `"An unexpected error occurred. Please try again later."` without leaking variable names.

---

## 7. Security & Anti-Echo Sanitization

1. **Anti-Echo Safeguards:**
   - In `HttpMessageNotReadableException`, neither Jackson's raw exception message, nor line/column offsets, nor rejected JSON tokens are returned. Field paths are extracted via Jackson's AST reference chain without echoing rejected values.
   - In `MethodArgumentTypeMismatchException`, `ex.getValue()` is strictly ignored; only parameter names and safe type names are returned.
2. **Sensitive Keyword Masking:**
   - Text inspected for `SENTINEL_*`, Aadhaar 12-digit patterns, PAN 10-char patterns, JWT bearer tokens, SQL keywords, and secret/password keywords.
   - If detected, error messages are masked with generic catalog messages.
3. **Fail-Closed Architecture Safeguards:**
   - `ConstraintViolationException` requires verified inclusion in `TRUSTED_CONTROLLER_CLASSES` or `TRUSTED_REQUEST_DTO_CLASSES`. Violations originating from entity, repository, service, or unknown classes immediately fail closed as HTTP 500.

---

## 8. Files Changed

1. **`backend/src/main/java/in/gov/sih/sih26135/exception/ErrorResponse.java`**
   - Added optional RFC 7807 `type` field with getter and setter.
   - Ensured `type` is omitted during JSON serialization when null via `@JsonInclude(NON_NULL)`.
2. **`backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java`**
   - Added `resolveSafeTypeName`, `isDateTimeType`, `hasCauseOfType`, and `extractJacksonFieldPath` helpers.
   - Hardened `handleMessageNotReadable` to safely inspect and classify enum, date/time, incompatible type, and missing body failures with field details.
   - Hardened `handleMethodArgumentTypeMismatch` to simplify enum types to `"valid enum value"` and sanitize parameter names.
   - Hardened `handleMethodNotSupported` to preserve HTTP `Allow` header with `headers.setAllow(ex.getSupportedHttpMethods())`.
   - Hardened `handleHttpMediaTypeNotSupported` to preserve HTTP `Accept` header with `headers.setAccept(ex.getSupportedMediaTypes())`.
   - Added `handleServletRequestBinding` mapping generic `ServletRequestBindingException` to HTTP 400 `BAD_REQUEST`.
   - Sanitized `handleMissingServletRequestParameter` and `handleMissingRequestHeader` against sensitive parameter names.
3. **`backend/src/test/java/in/gov/sih/sih26135/exception/GlobalExceptionHandlerErrorMappingTest.java`**
   - Added 6 new unit tests for enum/date/type deserialization, missing body, `ServletRequestBindingException`, and enum type mismatch sanitization.
   - Updated HTTP 405 and HTTP 415 tests to verify `Allow` and `Accept` header preservation.
4. **`backend/src/test/java/in/gov/sih/sih26135/exception/GlobalErrorHandlingHardeningTest.java` (New Test Suite)**
   - 16 comprehensive Spring MVC tests verifying end-to-end exception processing, `Allow`/`Accept` headers, RFC 7807 JSON structure, DTO validation, and parameter conversion.

---

## 9. Test Verification Matrix

All 266 tests across the entire validation and exception handling test suite executed and passed with 100% success:

| Test Suite Class | Tests Run | Failures | Errors | Result |
| :--- | :---: | :---: | :---: | :---: |
| `GlobalErrorHandlingHardeningTest` (New Phase 26.8 Suite) | 16 | 0 | 0 | **PASS** |
| `GlobalExceptionHandlerErrorMappingTest` | 84 | 0 | 0 | **PASS** |
| `ControllerPathQueryHeaderValidationTest` (Phase 26.7) | 28 | 0 | 0 | **PASS** |
| `CrossFieldAndBusinessConstraintValidationTest` (Phase 26.6) | 22 | 0 | 0 | **PASS** |
| `RequestDtoValidationAndSchemaAlignmentTest` (Phase 26.5) | 45 | 0 | 0 | **PASS** |
| `ValidationFrameworkFoundationTest` (Phase 26.2) | 8 | 0 | 0 | **PASS** |
| `CoreDomainResourcePaginationTest` (Phase 26.3) | 25 | 0 | 0 | **PASS** |
| `OperationalLogEventPaginationTest` (Phase 26.4) | 25 | 0 | 0 | **PASS** |
| `ApiLevelExceptionHandlerTest` (API Integration) | 13 | 0 | 0 | **PASS** |
| **Combined Full Suite** | **266** | **0** | **0** | **100% PASS** |

### Execution Commands:
- Hardening Suite: `mvn test "-Dtest=GlobalErrorHandlingHardeningTest"` -> **16 tests run, 0 failures, 0 errors, BUILD SUCCESS**
- Error Mapping Suite: `mvn test "-Dtest=GlobalExceptionHandlerErrorMappingTest"` -> **84 tests run, 0 failures, 0 errors, BUILD SUCCESS**
- Combined Regression Suite: `mvn test "-Dtest=GlobalErrorHandlingHardeningTest,GlobalExceptionHandlerErrorMappingTest,ControllerPathQueryHeaderValidationTest,CrossFieldAndBusinessConstraintValidationTest,RequestDtoValidationAndSchemaAlignmentTest,ValidationFrameworkFoundationTest,CoreDomainResourcePaginationTest,OperationalLogEventPaginationTest,ApiLevelExceptionHandlerTest"` -> **266 tests run, 0 failures, 0 errors, BUILD SUCCESS (Time: 01:01 min)**

---

## 10. Database Safety & Git Integrity Confirmations

1. **Database Safety:**
   - Production database `SIH26135` was **NEVER** accessed, queried, or connected to.
   - Database-backed integration tests strictly targeted dedicated test catalog `SIH26135_TEST`, verified by `MysqlConnectionTest.assertDedicatedTestCatalog`.
   - Database schema remains 100% frozen with `spring.jpa.hibernate.ddl-auto=validate`. Zero DDL, zero migrations, zero persistent seed data created.
2. **Git Hygiene & Working-Tree State:**
   - Zero Git mutation commands (`git add`, `git commit`, `git push`, `git reset`, `git stash`, `git clean`) were executed.
   - `git diff --check` completed with **zero** whitespace or syntax errors.
   - Only the designated exception handling and test files were modified or created.

---

## 11. Final Verdict

**Verdict:** `READY FOR INDEPENDENT REVIEW`
