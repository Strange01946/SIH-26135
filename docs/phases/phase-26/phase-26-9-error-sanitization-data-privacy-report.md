# Phase 26.9 — Error Sanitization, Data Privacy & Information Leakage Prevention Report

## Executive Summary

Phase 26.9 hardens the **SIH-26135 Spring Boot backend** against sensitive information leakage through API error responses. Following rigorous review and final hardening corrections, the implementation establishes comprehensive **safe-by-construction** sanitization guarantees:
- Client-facing error responses are constructed exclusively from explicit code-derived allowlists, approved static domain messages, and safe contract templates.
- Arbitrary exception text, SQL queries, database credentials, server paths, and raw exception details are never exposed directly to clients.
- Internal server errors (HTTP 500) strictly fail closed to generic safe messages without inspecting internal exception causes, database errors, or stack traces.
- Anti-echo protection is strengthened across all validation pathways: rejected user inputs (including single digits and booleans) are never reflected in error messages. Collection traversal at the maximum depth boundary (`MAX_COLLECTION_DEPTH = 3`) strictly fails closed on unexamined branches, while identity-based cycle detection prevents `StackOverflowError`.
- Request URI sanitization is made strictly safe by construction:
  1. Never returns a raw request URI merely because its path segments are numeric or allowlisted.
  2. Resolves `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE` to return the valid trusted route template instead of the raw URI.
  3. If no trusted route template is available (such as unmapped endpoints or unmatched methods), returns `"/sanitized-path"` by default.
  4. Never assumes numeric identifiers are safe; treats them as user-controlled path values so they never appear raw in returned error paths.
  5. Strips query parameters by default.
  6. Enforces that route templates are checked against trusted API prefixes (`/api/v1/`, `/api/auth/`, `/api/test/`) and rejects templates containing sensitive data.
- Regular expressions act as a defense-in-depth safeguard at the final response boundary rather than a sole gatekeeper.

All modifications preserve established API contracts, HTTP status codes, response envelope structures (`ErrorResponse`), security filters, tenant isolation, and logging capabilities. Comprehensive regression testing across the entire backend verified **886 tests passing with 0 failures, 0 errors, and 0 skipped**.

---

## 1. Baseline Reconciliation & Review Corrections

- **Repository Root:** `E:\project`
- **Backend Path:** `E:\project\backend`
- **Current Git Branch:** `main` (commit `cd2ea40`)
- **Database Catalog:** Strictly targeted test catalog `SIH26135_TEST` with `spring.jpa.hibernate.ddl-auto=validate`. Zero schema modifications, migrations, or DDL executions.
- **Git State:** Read-only compliance; zero git mutations performed.
- **Review Corrections Addressed:**
  1. *Anti-Echo Fail-Closed Traversal at Depth Boundary:*
     - When collection recursion reaches `MAX_COLLECTION_DEPTH = 3`, unexamined nested collections or arrays strictly fail closed (return `true`), ensuring uninspected branches cannot silently produce a "safe" result.
     - Preserved leaf element examination at `depth = 3`.
     - Cycle detection utilizes an identity-based visited set (`Collections.newSetFromMap(new IdentityHashMap<>())`) to safely handle self-referential or cyclic collections without `StackOverflowError`.
     - Supports primitive arrays, Object arrays, and all `Iterable<?>` implementations (such as `List`, `Set`).
  2. *Strict Safe-by-Construction URI Sanitization & Numeric Path Privacy:*
     - Eliminated static segment allowlists and numeric exemptions that previously permitted raw URIs to leak.
     - Resolves `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE` to return safe route templates (e.g. `/api/v1/job-postings/{id}`, `/api/v1/users/{id}`).
     - Unmapped routes, unmatched methods, or missing templates fail closed to `"/sanitized-path"`.
     - Numeric identifiers (short and long numeric IDs) are treated as user-controlled path values and never appear raw in error responses.
     - Query parameters are stripped by default (`indexOf('?')`).
     - Route templates are validated against trusted API prefixes (`/api/v1/`, `/api/auth/`, `/api/test/`) and checked for sensitive data patterns.
  3. *Preservation of Security, Status Codes & Headers:*
     - All error responses route through `buildSanitizedResponse`, preserving required RFC 7807 headers (`Allow`, `Accept`).
     - Safe client message resolution (`resolveSafeClientMessage`) guarantees unknown error codes fail closed to generic status fallbacks.

---

## 2. Threat Model & Sanitization Architecture

The architecture employs a two-tier security model:

```
[ Incoming Request Failure / Exception ]
                 │
                 ▼
[ Tier 1: Safe-by-Construction Message Generation ]
  ├── 5xx Server Error? ───────────────► Return generic "An unexpected error occurred."
  ├── Approved Static Domain Message? ─► Return verified domain template
  ├── Known Error Code? ───────────────► Return code-derived allowlist message
  └── Unknown Error Code? ─────────────► Return generic HTTP status fallback
                 │
                 ▼
[ Anti-Echo & Validation Sanitization ]
  ├── Check rejected user values ──────► Strip user input, fail closed to safe constraint code message
  ├── Identity cycle tracking ─────────► IdentityHashMap visited set prevents circular recursion
  ├── Fail-closed depth boundary ──────► Depth >= 3 unexamined collections fail closed (return true)
  └── Unrecognized constraint code? ───► Fall back to "is invalid"
                 │
                 ▼
[ Safe-by-Construction URI Sanitization ]
  ├── Strip query parameters ──────────► Suppress tokens and sensitive query params by default
  ├── Resolve route template ──────────► HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
  ├── Trusted prefix & sensitivity ────► Verify /api/v1/, /api/auth/, /api/test/ & check regex
  ├── Missing / untrusted template ────► Fail closed to "/sanitized-path"
  └── Numeric IDs in path ─────────────► Never returned raw; route template /sanitized-path used
                 │
                 ▼
[ Tier 2: Final Sanitization Boundary (Defense-in-Depth) ]
  ├── SENSITIVE_DATA_PATTERN regex ────► Masks Aadhaar, PAN, JWT, secrets, JDBC, SQL, paths, classes, stack traces, IPs
  └── Header Preservation ─────────────► Retains RFC 7807 headers (Allow, Accept)
                 │
                 ▼
[ Client-Facing ErrorResponse Envelope ]
```

### 2.1. Sensitive Data Pattern Matrix (Defense-in-Depth)

| # | Information Category | Threat Vector | Pattern & Enforcement Strategy |
|---|----------------------|---------------|--------------------------------|
| 1 | **Aadhaar Numbers** | Citizen PII disclosure in verification or identity exceptions | Matches 12 contiguous digits `(?<!\d)\d{12}(?!\d)` and 4-4-4 formatted groups `(?<!\d)\d{4}[\s-]\d{4}[\s-]\d{4}(?!\d)`, including delimited/parenthesized strings. |
| 2 | **PAN Numbers** | Tax identity disclosure in registration or employer onboarding | Case-insensitive regex `(?<![A-Za-z0-9])[A-Za-z]{5}[0-9]{4}[A-Za-z](?![A-Za-z0-9])` matching Indian Permanent Account Numbers. |
| 3 | **Authentication Tokens** | Bearer tokens and JWTs in auth headers or token parsing failures | Matches `eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}...` and `Bearer\s+[A-Za-z0-9_.-]{10,}`. |
| 4 | **Passwords & Secrets** | Credentials in login payloads, config dumps, or exception causes | Matches `password`, `secret`, `api_key`, `access_token`, `refresh_token`, `client_secret`, `auth_token`, `private_key` assignments (`:=`) or quoted values. |
| 5 | **Database URLs** | Exposure of internal network hostnames, ports, and credentials | Matches `jdbc:[^\s,;]+`, `(?:mysql|postgresql|oracle|sqlserver)://...`, and parameter strings `user=...`, `password=...`. |
| 6 | **Raw SQL Queries** | SQL statement disclosure exposing database schema and table design | Matches DML/DDL commands (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `CREATE`, `TRUNCATE`) combined with clause keywords (`FROM`, `INTO`, `SET`, `TABLE`, `WHERE`, `VALUES`). |
| 7 | **Database Integrity Details** | Database constraint names, column nullability, and foreign key errors | Matches `Duplicate entry`, `foreign key constraint`, `Column '.*' cannot be null`, and constraint prefixes (`uk_`, `fk_`, `pk_`). |
| 8 | **Hibernate / JPA Internals** | Framework implementation details and internal state | Matches `org.hibernate`, `HHH\d+`, and `could not execute statement`. |
| 9 | **Filesystem Paths** | Server directory structures, username paths, and file locations | Matches Windows drive paths (`[A-Za-z]:\\[^\s,;]+`) and Unix system paths (`/(?:etc|var|usr|opt|home|app|bin|tmp)/[^\s,;]+`). |
| 10 | **Package & Class Names** | Internal architecture disclosure and reflection probing | Matches project and framework package names (`in.gov.sih`, `org.springframework`, `com.fasterxml.jackson`, `com.mysql`, `org.apache`). |
| 11 | **Stack Traces** | Source code filenames, method names, and line numbers | Matches `at ...(...java:\d+)`, `Caused by:`, and standalone source line references `[a-zA-Z0-9_$]+\.java:\d+`. |
| 12 | **Hostnames & IP Addresses** | Internal network topology and infrastructure addressing | Matches `localhost:\d+`, loopback `127.0.0.1`, RFC 1918 private IPv4 subnets (`10.x.x.x`, `172.16-31.x.x`, `192.168.x.x`), and `*.internal` domains. |

---

## 3. Implementation Details

### 3.1. Anti-Echo Traversal with Fail-Closed Depth Bounding

In `containsRejectedValue()`, collections are traversed recursively up to `MAX_COLLECTION_DEPTH = 3`. If traversal reaches the maximum depth and encounters a nested collection or array, it strictly fails closed (`return true`), ensuring uninspected branches cannot produce an unsafe negative result:

```java
private static final int MAX_COLLECTION_DEPTH = 3;

private boolean containsRejectedValue(String message, Object rejectedValue) {
  return containsRejectedValue(message, rejectedValue, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
}

private boolean containsRejectedValue(String message, Object rejectedValue, int depth, Set<Object> visited) {
  if (message == null || message.isBlank() || rejectedValue == null) {
    return false;
  }

  // Handle arrays
  if (rejectedValue.getClass().isArray()) {
    if (depth >= MAX_COLLECTION_DEPTH) {
      return true; // Fail closed on unexamined branches beyond depth boundary
    }
    if (!visited.add(rejectedValue)) {
      return false; // Guard against cyclic recursion
    }
    int len = java.lang.reflect.Array.getLength(rejectedValue);
    for (int i = 0; i < len; i++) {
      Object elem = java.lang.reflect.Array.get(rejectedValue, i);
      if (containsRejectedValue(message, elem, depth + 1, visited)) {
        return true;
      }
    }
    return false;
  }

  // Handle Iterables (List, Set, etc.)
  if (rejectedValue instanceof Iterable<?> iter) {
    if (depth >= MAX_COLLECTION_DEPTH) {
      return true; // Fail closed on unexamined branches beyond depth boundary
    }
    if (!visited.add(rejectedValue)) {
      return false; // Guard against cyclic recursion
    }
    for (Object elem : iter) {
      if (containsRejectedValue(message, elem, depth + 1, visited)) {
        return true;
      }
    }
    return false;
  }

  // Leaf evaluation: inspect primitive or string representation
  String rejectedStr = String.valueOf(rejectedValue).trim();
  if (rejectedStr.isBlank()) {
    return false;
  }
  return message.toLowerCase().contains(rejectedStr.toLowerCase());
}
```

### 3.2. Safe-by-Construction URI Sanitization (`sanitizeUri`)

To eliminate reliance on regex matching or segment allowlists as privacy boundaries, `sanitizeUri()` operates with a safe-by-construction pipeline:

1. **Never return raw request URI:** Path segments containing numeric IDs or allowlisted strings are never returned directly to clients.
2. **Route Template Resolution:** Resolves Spring MVC's route template via `request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)`.
3. **Query Parameter Suppression:** Strips query parameters by default (`indexOf('?')`).
4. **Trusted Prefix & Sensitive Data Validation:** Validates that route templates begin with trusted API prefixes (`/api/v1/`, `/api/auth/`, `/api/test/`) and contains no sensitive data patterns.
5. **Fail-Closed Default:** If no trusted route template is available (or if validation fails), returns `"/sanitized-path"` by default.
6. **Numeric Identifier Safety:** Numeric IDs (short and long numeric identifiers) are treated as user-controlled path values and never appear raw in returned error paths.

```java
  private boolean isTrustedApiPrefix(String uri) {
    if (uri == null || uri.isBlank()) {
      return false;
    }
    return uri.startsWith("/api/v1/")
        || uri.startsWith("/api/auth/")
        || uri.startsWith("/api/test/")
        || uri.equals("/api/v1")
        || uri.equals("/api/auth")
        || uri.equals("/api/test");
  }

  private String sanitizeUri(HttpServletRequest request) {
    if (request == null) {
      return "/sanitized-path";
    }
    return sanitizeUri(request.getRequestURI(), request);
  }

  private String sanitizeUri(String rawUri) {
    return sanitizeUri(rawUri, null);
  }

  private String sanitizeUri(String rawUri, HttpServletRequest request) {
    // 1. If request is provided, resolve HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
    if (request != null) {
      Object patternAttr = request.getAttribute(
          org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      if (patternAttr instanceof String template && !template.isBlank()) {
        String cleanTemplate = template.trim();
        // Strip query parameters by default
        int queryIdx = cleanTemplate.indexOf('?');
        if (queryIdx >= 0) {
          cleanTemplate = cleanTemplate.substring(0, queryIdx).trim();
        }
        // Validate trusted API prefix and reject templates containing sensitive data
        if (isTrustedApiPrefix(cleanTemplate) && !containsSensitiveData(cleanTemplate)) {
          return cleanTemplate;
        }
      }
    }

    // 2. If no trusted route template is available, return "/sanitized-path" by default.
    return "/sanitized-path";
  }
```

### 3.3. Safe-by-Construction Client Messages (`resolveSafeClientMessage`)

Arbitrary exception messages are never exposed directly to clients. Messages are resolved deterministically:
- 5xx server errors strictly return `"An unexpected error occurred. Please try again later."`.
- Known safe domain messages and contract templates (`isApprovedSafeDomainMessage`) are preserved.
- Code-derived messages (`getSafeMessageForErrorCode`) map error codes to clean, static descriptions.
- Unknown error codes fail closed to generic HTTP status fallbacks.

---

## 4. Verification & Test Evidence

### 4.1. Targeted Regression Tests

Ran targeted tests in `GlobalExceptionHandlerErrorMappingTest` and `GlobalErrorHandlingHardeningTest`:
- **Command:** `mvn -f backend/pom.xml test "-Dtest=GlobalExceptionHandlerErrorMappingTest,GlobalErrorHandlingHardeningTest"`
- **Results:** **Tests run: 153, Failures: 0, Errors: 0, Skipped: 0.** (Total time: 30.95 s).
- **Key Regression Test Scenarios Verified:**
  - `testUriSanitization_numericPathVariable_resolvesToRouteTemplateWithoutRawId`: Proves that numeric path variables (e.g. `42`) resolve to route templates (`/api/v1/job-postings/{id}`) and never appear raw in returned error paths.
  - `testUriSanitization_longNumericIdentifier_resolvesToRouteTemplateWithoutLongId`: Proves that long numeric identifiers (`998877665544332211`) resolve to route templates (`/api/v1/trainees/{id}`) and never leak raw in error paths.
  - `testUriSanitization_numericPathVariable_missingTemplateFallsBackToSanitizedPath`: Proves that when no route template is available, numeric path variables fail closed to `"/sanitized-path"`.
  - `testUriSanitization_routeTemplateWithSensitiveData_failsClosedToSanitizedPath`: Proves that route templates containing sensitive data patterns fail closed to `"/sanitized-path"`.
  - `testUriSanitization_routeTemplateWithUntrustedPrefix_failsClosedToSanitizedPath`: Proves that route templates outside trusted API prefixes fail closed to `"/sanitized-path"`.
  - `testUriSanitization_routeTemplateWithQueryParams_stripsQueryParams`: Proves that query parameters on route templates are stripped by default.
  - `testContainsRejectedValue_nestedExactlyAtDepthBoundary_isSanitized`: Leaf element at exact depth boundary (depth = 3) is inspected and correctly sanitized.
  - `testContainsRejectedValue_nestedBeyondDepthBoundary_failsClosed`: Nested collection beyond depth limit strictly fails closed to safe fallback message.
  - `testContainsRejectedValue_unrelatedSafeMessage_isPreserved`: Unrelated safe standard validation messages are preserved without false positives.
  - `testContainsRejectedValue_iterableSetImplementation_isSanitized`: Custom `Set` and `Iterable` rejected collections are traversed and sanitized.
  - `testContainsRejectedValue_cyclicCollection_handlesWithoutStackOverflow`: Circular collection references terminate safely via identity tracking.
  - `testContainsRejectedValue_singleDigitRejectedValueEchoed_isSanitized`: Single-digit rejected values are detected and sanitized.
  - `testContainsRejectedValue_booleanFalseRejectedValueEchoed_isSanitized` & `testContainsRejectedValue_booleanTrueRejectedValueEchoed_isSanitized`: Boolean rejected values are detected and sanitized.

### 4.2. Full Backend Regression Test Suite

Executed the full test suite across all 52 test classes:
- **Command:** `mvn -f backend/pom.xml test`
- **Results:** **Tests run: 886, Failures: 0, Errors: 0, Skipped: 0.** (Total time: 2m 02s).

### 4.3. Code Formatting & Git Working-Tree Verification

- **Command:** `git diff --check`
- **Result:** Exit code 0 (zero whitespace or formatting errors).
- **Working Tree:**
  - `backend/src/main/java/in/gov/sih/sih26135/exception/GlobalExceptionHandler.java` (modified)
  - `backend/src/test/java/in/gov/sih/sih26135/ApiLevelExceptionHandlerTest.java` (modified)
  - `backend/src/test/java/in/gov/sih/sih26135/exception/GlobalErrorHandlingHardeningTest.java` (modified)
  - `backend/src/test/java/in/gov/sih/sih26135/exception/GlobalExceptionHandlerErrorMappingTest.java` (modified)
  - `backend/src/test/java/in/gov/sih/sih26135/validation/ControllerPathQueryHeaderValidationTest.java` (modified)
  - `backend/src/test/java/in/gov/sih/sih26135/validation/ValidationFrameworkFoundationTest.java` (modified)
  - `phase-26-9-error-sanitization-data-privacy-report.md` (untracked report)

---

## 5. Final Verdict

**Verdict: PASS**

The error response sanitization and data privacy implementation provides complete safe-by-construction defenses:
1. Anti-echo collection recursion strictly fails closed at the depth boundary and protects against cycles.
2. URI sanitization never returns raw request URIs based on numeric or allowlisted segments, resolves trusted route templates, strips query parameters, and fails closed to `"/sanitized-path"` by default.
3. Numeric identifiers (both short and long) are treated as user-controlled path values and never leak in returned error paths.
4. Client-facing error messages are derived strictly from code allowlists and fail closed to generic fallbacks.
5. All 886 tests across the entire backend test suite pass with 0 failures, 0 errors, and 0 skipped.
