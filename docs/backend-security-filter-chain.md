# Backend Security Filter Chain & JWT Authentication Filter — Phase 24.4

## 1. Overview
Phase 24.4 establishes the core Spring Security HTTP request processing pipeline for the SIH 26135 backend. It integrates the cryptographic token infrastructure created in Phase 24.3 into a stateless filter chain that intercepts requests, authenticates valid access tokens, and enforces the boundary between public and protected endpoints.

---

## 2. Component Responsibilities

### A. `SecurityFilterChain` ([`SecurityConfig.java`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/config/SecurityConfig.java))
- Configures the HTTP security filter chain.
- Enforces stateless session management (`SessionCreationPolicy.STATELESS`).
- Disables HTTP Basic and Form Login interactive mechanisms.
- Disables session-bound CSRF protection for stateless Bearer token APIs.
- Defines URL patterns for public versus authenticated access.
- Positions `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

### B. `JwtAuthenticationFilter` ([`JwtAuthenticationFilter.java`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/jwt/JwtAuthenticationFilter.java))
- Extends `OncePerRequestFilter` to ensure single execution per HTTP request dispatch.
- Extracts bearer tokens from the `Authorization` header.
- Validates the token strictly as an **ACCESS** token using `JwtTokenProvider`.
- Extracts user identity (`uid`, `username`) and granted authorities (`authorities`).
- Constructs a lightweight `UserPrincipal` and an authenticated `UsernamePasswordAuthenticationToken`.
- Populates Spring Security's `SecurityContextHolder`.

---

## 3. Authorization Header & Token Handling

### Expected Format
```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

### Request Processing Matrix
| Scenario | Filter Action | SecurityContext State |
| :--- | :--- | :--- |
| **Missing Header** | Continues filter chain. | Unauthenticated (anonymous). |
| **Non-Bearer Header** | Continues filter chain. | Unauthenticated (anonymous). |
| **Blank Bearer Token** | Continues filter chain. | Unauthenticated (anonymous). |
| **Valid Access Token** | Parses claims, verifies token_type=`access`. | Authenticated `UserPrincipal`. |
| **Valid Refresh Token** | `parseAndValidateToken(token, "access")` throws `INVALID_TOKEN_TYPE`. Context cleared. | Unauthenticated (anonymous). |
| **Expired / Tampered Token** | Throws `JwtValidationException`. Context cleared. | Unauthenticated (anonymous). |
| **Already Authenticated** | Skips re-authentication to prevent redundant parsing. | Existing authentication preserved. |

> **Critical Rule:** Refresh tokens (`token_type: "refresh"`) are strictly rejected by `JwtAuthenticationFilter` and cannot be used to authenticate API requests.

---

## 4. Security Context & Principal Design

### Principal Implementation ([`UserPrincipal.java`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/UserPrincipal.java))
- An immutable Java record implementing `java.security.Principal` and `Serializable`.
- Exposes:
  - `id()` / `getId()`: User primary key (`Long`).
  - `username()` / `getUsername()` / `getName()`: Login handle (`String`).
- **Data Isolation:** Contains zero sensitive fields (no passwords, hashes, PII, Aadhaar numbers, database credentials, or secret keys).
- **Zero Database Overhead:** Populated entirely from verified JWT claims without issuing database queries.

### Authentication Object
```java
UsernamePasswordAuthenticationToken.authenticated(
    principal,
    null, // Credentials explicitly set to null
    authorities // Converted to List<SimpleGrantedAuthority>
)
```

---

## 5. HTTP Endpoint Security Boundary

The security filter chain enforces a strict two-tier access model:

### A. Public Endpoints (`permitAll()`)
- `/api/v1/auth/**`: Reserved for future authentication endpoints (login, refresh) to be implemented in Phase 24.5.
- `/actuator/health`: Operational health check endpoint.
- `/error`: Internal servlet error dispatching.

### B. Authenticated Endpoints (`authenticated()`)
- All other endpoints (all Phase 23 business REST controllers, cross-domain workflows, and administrative endpoints).
- Requests lacking a valid JWT access token are blocked at the Spring Security filter boundary.

---

## 6. Architectural Boundaries & Deferred Scope

| Scope Item | Current Status (Phase 24.4) | Target Subphase |
| :--- | :--- | :--- |
| **Authentication Filter Chain** | **Implemented** (Stateless, Bearer access tokens) | Phase 24.4 |
| **Public vs Protected Boundary** | **Implemented** (`/api/v1/auth/**` public, business APIs authenticated) | Phase 24.4 |
| **Authentication REST Endpoints** | *Deferred* (no `/login`, `/refresh`, `/me`) | Phase 24.5 |
| **401 / 403 Response Envelope** | *Deferred* (Spring default responses used; custom ApiResponse handler deferred) | Phase 24.6 |
| **Role & Permission Authorization** | *Deferred* (no `@PreAuthorize`, `hasRole()`, or `hasAuthority()`) | Phase 24.7 |
| **Account Lockout & Audit Counters** | *Deferred* (`failed_login_count`, `locked_until`) | Phase 24.8 |
| **Production CORS & Security Headers** | *Deferred* (CORS origin whitelist, HSTS, CSP) | Phase 24.9 |
| **Security Verification** | *Deferred* (End-to-end integration tests) | Phase 24.10 |

---

## 7. External Dependencies
**No external service/account/API is required.** All request interception, token validation, and SecurityContext management execute entirely in-process using Spring Security and the local JVM.
