# Backend JWT Token Infrastructure — Phase 24.3

## 1. Overview
The JWT Token Infrastructure provides a reusable, self-contained, and production-grade token issuance, parsing, and validation foundation for the SIH 26135 platform. It establishes the cryptographic token engine that will be leveraged by subsequent authentication and authorization layers (Phase 24.4+).

---

## 2. Cryptographic Specifications

- **Signing Algorithm:** HMAC SHA-256 (`HS256`).
- **Cryptographic Key Material:** Minimum 256 bits (32 bytes) of high-entropy key material.
- **Key Representation:** Initialized and cached in memory as a `javax.crypto.SecretKey` instance using `io.jsonwebtoken.security.Keys.hmacShaKeyFor(...)`.
- **Unconditional Startup Key Validation:**
  - Validated unconditionally on application startup inside `@PostConstruct init()`.
  - Application startup fails immediately with `IllegalStateException` if `JWT_SECRET` is missing, blank, or fewer than 32 bytes (256 bits).
  - **Zero Default Secrets:** No fallback development keys, dummy secrets, or auto-generated keys are permitted in version control or runtime memory.

---

## 3. Configuration & Environment Variables

Configuration is externalized in `application.properties` under the `security.jwt` prefix and mapped to [`JwtProperties.java`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/config/JwtProperties.java):

| Property | Environment Variable | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `security.jwt.secret` | `JWT_SECRET` | *(None — Required)* | Cryptographic signing key (minimum 32 bytes). |
| `security.jwt.issuer` | `JWT_ISSUER` | `in.gov.sih.sih26135` | Official token issuer identifier. |
| `security.jwt.access-token-expiration-ms` | `JWT_ACCESS_TOKEN_EXPIRATION_MS` | `900000` (15 min) | Lifespan for authenticated API access tokens. |
| `security.jwt.refresh-token-expiration-ms` | `JWT_REFRESH_TOKEN_EXPIRATION_MS` | `604800000` (7 days) | Lifespan for session renewal refresh tokens. |

---

## 4. Token Types & Claim Design

Tokens are structurally distinguishable via the standard claim `token_type`:

### A. Access Token (`token_type: "access"`)
- **Purpose:** Carried in `Authorization: Bearer <token>` headers for authenticated API requests.
- **Lifespan:** Short-lived (15 minutes default).
- **Claims Included:**
  - `sub`: Primary key identifier of the user (e.g., `"101"`).
  - `uid`: Numeric user ID (`Long`).
  - `username`: Login handle string.
  - `authorities`: Collection of granted role and permission codes (e.g., `["ROLE_super_admin", "user.manage"]`).
  - `token_type`: `"access"`.
  - `iss`: Configured issuer (`in.gov.sih.sih26135`).
  - `iat`: Timestamp of issuance.
  - `exp`: Timestamp of expiration.
  - `jti`: Unique UUID string for token uniqueness tracking.

### B. Refresh Token (`token_type: "refresh"`)
- **Purpose:** Used strictly to renew expired access tokens.
- **Lifespan:** Long-lived (7 days default).
- **Claims Included:**
  - `sub`: Primary key identifier of the user (e.g., `"101"`).
  - `uid`: Numeric user ID (`Long`).
  - `token_type`: `"refresh"`.
  - `iss`: Configured issuer.
  - `iat`: Timestamp of issuance.
  - `exp`: Timestamp of expiration.
  - `jti`: Unique UUID string.
- **Design Principle:** Intentionally excludes `username` and `authorities` to prevent privilege drift and ensure user authorization state is re-evaluated upon token refresh.

---

## 5. Subject Representation & Sensitive Data Exclusion

### Subject Representation
The JWT subject (`sub`) is strictly mapped to the immutable numeric user primary key `String.valueOf(User.getId())`.
- **Rationale:** Using immutable primary keys avoids coupling identity tokens to mutable identifiers (such as username or email) or exposing sensitive personally identifiable information (PII).

### Intentionally Excluded Information
The following elements are strictly forbidden from token payloads:
- Passwords or password hashes.
- Aadhaar numbers, national identity identifiers, or bank accounts.
- OTP values or transient security codes.
- Database credentials, API keys, or operational secrets.

---

## 6. Token Validation & Error Handling

[`JwtTokenProvider.java`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/jwt/JwtTokenProvider.java) performs strict validation against signature tampering, issuer mismatch, structural malformation, expiration, and all required application claims.

### Mandatory Claims Validation
Every parsed token must contain all of the following claims:
- `sub`: Non-blank subject
- `uid`: Non-null numeric user ID
- `token_type`: Non-blank token type (`access` or `refresh`)
- `jti`: Non-blank unique token ID
- `iss`: Non-blank issuer matching configured platform issuer
- `iat`: Non-null issued-at timestamp
- `exp`: Non-null expiration timestamp

If any required claim is absent or blank, validation fails immediately with `ErrorCode.MISSING_REQUIRED_CLAIM`.
When token-type-specific validation is requested (`expectedTokenType`), a type mismatch or missing token type produces `ErrorCode.INVALID_TOKEN_TYPE` or `ErrorCode.MISSING_REQUIRED_CLAIM`.

### Error Code Mapping

| Cause / Exception | Mapped ErrorCode | Description |
| :--- | :--- | :--- |
| `ExpiredJwtException` | `EXPIRED` | Token has exceeded its expiration timestamp. |
| `SecurityException` | `INVALID_SIGNATURE` | HMAC signature does not match key material. |
| `MalformedJwtException` | `MALFORMED` | Token is not formatted as valid compact JWT. |
| `UnsupportedJwtException` | `UNSUPPORTED` | Unsupported JWT format or algorithm header. |
| `IncorrectClaimException` | `INVALID_ISSUER` | Token issuer does not match platform issuer. |
| `MissingClaimException` / Missing claim | `MISSING_REQUIRED_CLAIM` | Required claim (`sub`, `uid`, `token_type`, `jti`, `iss`, `iat`, `exp`) is absent. |
| Type mismatch | `INVALID_TOKEN_TYPE` | Attempting to use a refresh token as an access token or vice versa. |

---

## 7. Architectural Boundaries & Deferred Scope

### Provided in Phase 24.3
- JJWT 0.12.6 dependencies (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`).
- `JwtProperties` configuration class with 256-bit key strength validation.
- Unconditional `@PostConstruct` startup validation of `JWT_SECRET`.
- Comprehensive claims validation (`sub`, `uid`, `token_type`, `jti`, `iss`, `iat`, `exp`).
- `JwtValidationException` domain exception hierarchy.
- `JwtTokenProvider` component for token generation, validation, and claim extraction.

### Deferred to Phase 24.4+
- `SecurityFilterChain` and `OncePerRequestFilter` JWT authentication filter (Phase 24.4).
- Authentication REST endpoints: `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/me` (Phase 24.5).
- HTTP 401 Unauthorized / 403 Forbidden entry points and handlers (Phase 24.6).
- Method-level authorization via `@PreAuthorize` (Phase 24.7).
- Account lockout and login attempt audit counters (Phase 24.8).
- Production CORS, CSRF, and HTTP security headers (Phase 24.9).
- Integration testing and security verification (Phase 24.10).

---

## 8. External Service Dependencies
**No external service/account/API is required.** Token issuance, parsing, and cryptographic verification operate entirely in-process using standard JVM cryptography and the JJWT library.
