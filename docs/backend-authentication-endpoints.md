# Backend Authentication REST Endpoints — Phase 24.5

## 1. Overview
Phase 24.5 introduces the REST authentication layer under `/api/v1/auth`, connecting incoming HTTP authentication requests to the underlying Argon2id password verification and JWT token infrastructure. It provides endpoints for user login, token refresh with rotation, and authenticated self-profile retrieval.

---

## 2. Authentication Endpoints Contract

### A. POST `/api/v1/auth/login` (Public)
- **Access Level:** Public (`permitAll()`).
- **Request Body:** [`LoginRequest`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/dto/auth/LoginRequest.java)
  ```json
  {
    "usernameOrEmail": "admin_user",
    "password": "RawPassword123"
  }
  ```
- **Response Body:** `ApiResponse<AuthResponse>`
  ```json
  {
    "success": true,
    "message": "Authentication successful",
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "userId": 1,
      "username": "admin_user",
      "authorities": ["ROLE_super_admin", "user.manage", "trainee.read"]
    },
    "timestamp": "2026-09-25T16:08:00Z",
    "path": "/api/v1/auth/login"
  }
  ```

### B. POST `/api/v1/auth/refresh` (Public)
- **Access Level:** Public (`permitAll()`).
- **Request Body:** [`RefreshTokenRequest`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/dto/auth/RefreshTokenRequest.java)
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```
- **Response Body:** `ApiResponse<AuthResponse>`
  - Issues a newly minted access token and a rotated refresh token.
  - Re-evaluates current user existence, active status, and current RBAC authorities from the database.

### C. GET `/api/v1/auth/me` (Authenticated)
- **Access Level:** Authenticated (`authenticated()`).
- **Headers:** `Authorization: Bearer <access_token>`
- **Response Body:** `ApiResponse<AuthenticatedUserResponse>`
  ```json
  {
    "success": true,
    "message": "Authenticated user profile retrieved successfully",
    "data": {
      "id": 1,
      "username": "admin_user",
      "email": "admin@sih.gov.in",
      "authorities": ["ROLE_super_admin", "user.manage", "trainee.read"],
      "organizationId": null,
      "departmentId": null,
      "stateId": null,
      "districtId": null,
      "trainingProviderId": null,
      "employerId": null
    },
    "timestamp": "2026-09-25T16:08:00Z",
    "path": "/api/v1/auth/me"
  }
  ```

---

## 3. Core Implementation Mechanics

### 1. Password Verification Flow
- User lookup is executed via `userRepository.findByUsernameOrEmail(identifier, identifier)` in a single database query.
- Incoming raw password is verified against stored Argon2id hash using Spring Security's `PasswordEncoder.matches(rawPassword, storedHash)`.
- If the user does not exist, or the password does not match, or the user is soft-deleted, or the account is inactive, a generic `InvalidCredentialsException("Invalid username/email or password")` is thrown to prevent account enumeration.

### 2. Active Account Validation
- Account status is verified against `ref_user_status`:
  - `ACTIVE` $\rightarrow$ allowed to authenticate.
  - `PENDING`, `LOCKED`, `DISABLED`, `ARCHIVED` $\rightarrow$ rejected with `InvalidCredentialsException`.
  - Soft-deleted accounts (`deleted_at != null`) $\rightarrow$ rejected with `InvalidCredentialsException`.

### 3. Authority Construction
- Authorities are retrieved dynamically:
  - User's roles via `userService.getUserRoles(userId)` $\rightarrow$ prefixed with `ROLE_` (e.g., `ROLE_super_admin`).
  - Permissions assigned to each role via `roleService.getRolePermissions(roleId)` $\rightarrow$ (e.g., `user.manage`).
- Output format: `List<String>` packed into the access token's `authorities` claim.

### 4. Refresh Token Validation & Rotation
- The incoming token is strictly validated as `token_type: "refresh"` using `jwtTokenProvider.parseAndValidateToken(rawToken, TOKEN_TYPE_REFRESH)`.
- User ID is extracted from token claims.
- The user is re-validated against the database (must exist, not soft-deleted, status is `ACTIVE`).
- Current authorities are rebuilt from database records.
- A new access token and rotated refresh token are generated and returned.
- **Stateless Lifecycle:** Refresh tokens are stateless JWTs without database persistence; server-side revocation is not implemented in this phase.

### 5. Principal Handling on `/me`
- The current user principal is injected via `@AuthenticationPrincipal UserPrincipal principal`.
- Identity (`principal.getId()`) is used to retrieve latest user profile and authorities.
- Does not accept path variables or query other users' data.

---

## 4. Route Boundary Correction in `SecurityConfig`
To protect `/api/v1/auth/me` while allowing public access to login and refresh:
```java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
    .requestMatchers("/api/v1/auth/me").authenticated()
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/error").permitAll()
    .anyRequest().authenticated()
)
```

---

## 5. Architectural Boundaries & Deferred Scope

| Scope Item | Current Status (Phase 24.5) | Deferred Subphase |
| :--- | :--- | :--- |
| **Authentication REST Endpoints** | **Implemented** (`/login`, `/refresh`, `/me`) | Phase 24.5 |
| **Route Boundary Enforcement** | **Implemented** (Public `/login`, `/refresh`; Authenticated `/me` and business APIs) | Phase 24.5 |
| **Standardized 401/403 Envelope** | *Deferred* (flows into existing `GlobalExceptionHandler` / `BaseException` handling) | Phase 24.6 |
| **Method-Level RBAC Authorization** | *Deferred* (no `@PreAuthorize` or `hasRole()` checks) | Phase 24.7 |
| **Lockout Counters & Audit Logging** | *Deferred* (`failed_login_count`, `locked_until`, `last_login_at` updates) | Phase 24.8 |
| **Production CORS & Security Headers** | *Deferred* (origin whitelisting, CSP, HSTS) | Phase 24.9 |
| **Security Verification** | *Deferred* (automated authentication testing) | Phase 24.10 |

---

## 6. External Dependency Assessment
**No external service/account/API is required.** Authentication, password verification, and token operations operate entirely in-process using Spring Security, Argon2id, and JJWT.
