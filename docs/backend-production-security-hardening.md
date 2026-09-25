# Backend Production Security Hardening Specification

**Phase**: 24.9 — Production Security Hardening  
**Target Package**: `in.gov.sih.sih26135.security`  
**Components**: `SecurityConfig`, `CorsConfig`, `SecurityCorsProperties`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`

---

## 1. Overview & Primary Objective

Phase 24.9 establishes the production security perimeter for the SIH 26135 Spring Boot backend. Building upon the foundation of Argon2id password authentication, stateless JWT verification, method-level authorization (`@PreAuthorize`), standardized JSON 401/403 handlers, and account lockout with security auditing, this phase enforces:

1. **Explicit, Environment-Backed CORS Configuration**: Strict origin white-listing with default-deny semantics.
2. **Defensive HTTP Security Response Headers**: Standardized RFC-compliant headers protecting against clickjacking, MIME-sniffing, referrer leakage, and unauthorized framing.
3. **HTTP Strict Transport Security (HSTS)**: 1-year enforcement for HTTPS transport with subdomain coverage.
4. **Restrictive Content Security Policy (CSP)**: API-tailored `default-src 'none'; frame-ancestors 'none'; base-uri 'none'` preventing script execution or document framing.
5. **Stateless JWT Security & CSRF Defense**: Formal justification for stateless CSRF posture under bearer-token authentication.
6. **Actuator Exposure Hardening**: Minimal footprint limited strictly to `/actuator/health` with zero internal detail disclosure.
7. **Production vs. Local Development Hygiene**: Zero hardcoded credentials, zero dummy origins, zero fake secrets.

---

## 2. CORS Architecture & Allowed Origin Configuration

### 2.1 Default-Deny Security Model
Unlike developmental setups that rely on permissive wildcards (`allowedOrigins("*")`), the SIH 26135 backend adopts a **default-deny** cross-origin policy.

- If `SECURITY_CORS_ALLOWED_ORIGINS` is not defined or is left blank, `SecurityCorsProperties` resolves an empty allowed origin list (`[]`).
- Spring's `CorsConfiguration` registers this empty collection across all routes (`/**`).
- Any incoming browser request bearing an `Origin` header that does not match an explicitly allowed origin is rejected:
  - **Preflight `OPTIONS` requests** are intercepted by Spring Security's `CorsFilter` and immediately rejected with **HTTP 403 Forbidden** (with no `Access-Control-Allow-Origin` header).
  - **Actual requests** are stripped of `Access-Control-Allow-Origin`, causing browser user agents to block the response.

### 2.2 Prohibited Patterns & Startup Validation
`SecurityCorsProperties` validates configured origins in `@PostConstruct`:
1. **Wildcard Origins & Patterns Prohibited**: Any configured origin containing the wildcard character `*` (whether an exact `*`, or subdomain patterns like `https://*.example.com` or `http://*.example.com`, or TLD patterns like `https://example.*`) triggers an immediate `IllegalStateException` on application startup. Wildcard origin patterns are strictly prohibited.
2. **Explicit Origin Requirement**: Explicit origin URIs must be configured without wildcard expressions.
3. **Scheme Validation**: Every configured origin must explicitly start with `http://` or `https://` (e.g., `https://portal.sih.gov.in`).
4. **Normalization**: Trailing slashes and leading/trailing whitespace are stripped during property resolution.
5. **Credentials Safety**: Credentials remain `false` by default. If `allowCredentials` is set to `true`, wildcards are strictly barred.
6. **Header Enforcement**: The `allowedHeaders` list must explicitly include `Authorization` (required for JWT tokens) and `Content-Type` (required for JSON request bodies).
7. **Duration Enforcement**: `maxAge` must be a strictly positive duration.

### 2.3 CORS Properties Specification

All properties are environment-overridable while maintaining safe, defensive defaults:

| Property | Default Value | Environment Variable | Description |
|---|---|---|---|
| `security.cors.allowed-origins` | *empty* | `SECURITY_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins (default: empty / default-deny) |
| `security.cors.allowed-methods` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | `SECURITY_CORS_ALLOWED_METHODS` | Permitted HTTP verbs |
| `security.cors.allowed-headers` | `Authorization,Content-Type,Accept,Origin,X-Requested-With` | `SECURITY_CORS_ALLOWED_HEADERS` | Permitted incoming HTTP headers |
| `security.cors.exposed-headers` | `Authorization` | `SECURITY_CORS_EXPOSED_HEADERS` | Response headers accessible to browser JavaScript |
| `security.cors.allow-credentials` | `false` | `SECURITY_CORS_ALLOW_CREDENTIALS` | Prohibits ambient cookie sending; API uses Bearer tokens |
| `security.cors.max-age` | `3600s` | `SECURITY_CORS_MAX_AGE` | Preflight cache lifetime (1 hour) |

---

## 3. Production HTTP Security Response Headers

All HTTP responses emitted by the Spring Security pipeline (including successful 200 OK responses, 401 Unauthorized, and 403 Forbidden error envelopes) carry the following security headers:

### 3.1 Content Type Sniffing Protection
- **Header**: `X-Content-Type-Options: nosniff`
- **Purpose**: Instructs browsers not to sniff MIME types away from the declared `Content-Type`. Mitigates drive-by download and MIME-confusion attacks.

### 3.2 Anti-Clickjacking & Framing Protection
- **Header**: `X-Frame-Options: DENY`
- **CSP Equivalent**: `frame-ancestors 'none'`
- **Purpose**: Strictly prohibits embedding any API responses inside `<frame>`, `<iframe>`, `<embed>`, or `<object>` elements, preventing clickjacking attacks.

### 3.3 Referrer Policy
- **Header**: `Referrer-Policy: strict-origin-when-cross-origin`
- **Purpose**: 
  - On cross-origin HTTPS requests, sends only the origin (scheme, host, and port) as the referrer, stripping sensitive resource paths and query parameters.
  - On protocol downgrade (HTTPS to HTTP), sends no referrer at all.
  - Mitigates leakage of internal routing identifiers or confidential API paths in HTTP referrers.

### 3.4 Content Security Policy (CSP)
- **Header**: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'`
- **Rationale**: The backend is purely a headless REST API returning `application/json`. It does not serve HTML documents, scripts, stylesheets, or fonts. Therefore, the most restrictive CSP is configured:
  - `default-src 'none'`: Disallows loading any external resources.
  - `frame-ancestors 'none'`: Prohibits framing the response.
  - `base-uri 'none'`: Disallows injection of `<base>` tags.

### 3.5 HTTP Strict Transport Security (HSTS)
- **Header**: `Strict-Transport-Security: max-age=31536000 ; includeSubDomains`
- **Parameters**:
  - `max-age=31536000`: Enforces HTTPS for exactly 1 year (365 days).
  - `includeSubDomains`: Extends the HTTPS requirement to all current and future subdomains.
  - `preload`: Intentionally **omitted** (not enabled casually per security guidelines).
- **RFC 6797 Adherence**: Spring Security's `HstsHeaderWriter` emits this header **only when the request is secure** (`request.isSecure() == true`). Plain HTTP connections omit the header, conforming to RFC 6797 §7.2.

---

## 4. Stateless JWT Architecture & CSRF Posture

### 4.1 Stateless Session Management
The backend enforces `SessionCreationPolicy.STATELESS` in `SecurityConfig`:
```java
http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```
- No `HttpSession` is ever created or stored by the server.
- The `SecurityContext` is transient and request-scoped, populated on each request by `JwtAuthenticationFilter` and cleared immediately upon completion.
- No in-memory login state or server-side session cookies exist.

### 4.2 CSRF Defense Rationale
Spring Security's CSRF protection is explicitly disabled (`csrf(AbstractHttpConfigurer::disable)`):
- **Why CSRF is not applicable**: Cross-Site Request Forgery (CSRF) relies on browsers automatically sending *ambient credentials* (such as session cookies or HTTP Basic Authorization headers) with cross-origin requests.
- **Bearer Token Architecture**: The SIH 26135 backend uses Bearer tokens supplied in the HTTP `Authorization` header (`Authorization: Bearer <token>`).
- Browsers **never** automatically attach custom `Authorization` headers to cross-origin requests. Client-side JavaScript must explicitly attach the header, and any such cross-origin request is subject to preflight CORS checks.
- Zero cookies are used anywhere in the application.
- Enabling session-based CSRF protection would require storing CSRF tokens in an `HttpSession`, directly violating the stateless requirement without providing any security advantage.

---

## 5. Actuator / Health Endpoint Exposure

Actuator endpoints are strictly confined in `application.properties`:
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```
- Only `/actuator/health` is permitted publicly.
- All sensitive management endpoints (`env`, `beans`, `configprops`, `mappings`, `heapdump`, `threaddump`, `loggers`) remain completely unexposed.
- `show-details=never` ensures internal infrastructure metrics, database connectivity details, or disk thresholds are not exposed to unauthenticated probes.

---

## 6. Request Authorization Boundary

The HTTP request authorization boundary is enforced in `SecurityConfig`:

| Route | HTTP Method | Access Level | Description |
|---|---|---|---|
| `OPTIONS /**` | `OPTIONS` | Public (`permitAll()`) | Handled by `CorsFilter` for preflight validation |
| `/api/v1/auth/login` | `POST` | Public (`permitAll()`) | Initial credential verification |
| `/api/v1/auth/refresh` | `POST` | Public (`permitAll()`) | Access token renewal with refresh token |
| `/actuator/health` | `GET` | Public (`permitAll()`) | Container/platform liveness and readiness probe |
| `/error` | `*` | Public (`permitAll()`) | Spring Boot error dispatch route |
| `/api/v1/auth/me` | `GET` | Authenticated | Current authenticated user principal profile |
| `/api/v1/**` | `*` | Authenticated | All business REST APIs (Trainee, Course, etc.) |

---

## 7. Security Headers on Error Responses

When unauthorized or forbidden access occurs, Spring Security's exception handlers ensure security headers are preserved:

- **401 Unauthorized**: Intercepted by `RestAuthenticationEntryPoint`, producing a sanitized JSON payload:
  ```json
  {
    "success": false,
    "code": "UNAUTHORIZED",
    "message": "Authentication is required to access this resource",
    "data": null,
    "path": "/api/v1/trainees",
    "timestamp": "2026-09-25T19:48:08.538Z"
  }
  ```
  Retains: `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `CSP`, and `HSTS` (when secure).

- **403 Forbidden**: Intercepted by `RestAccessDeniedHandler`, producing:
  ```json
  {
    "success": false,
    "code": "FORBIDDEN",
    "message": "Access denied",
    "data": null,
    "path": "/api/v1/admin/resource",
    "timestamp": "2026-09-25T19:48:08.765Z"
  }
  ```
  Retains: `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `CSP`, and `HSTS` (when secure).

---

## 8. Security-Sensitive Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | **YES** | *None* | Mandatory 32+ byte HMAC-SHA256 signing key |
| `JWT_ISSUER` | No | `in.gov.sih.sih26135` | Registered issuer claim |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | No | `900000` (15m) | Access token lifespan |
| `JWT_REFRESH_TOKEN_EXPIRATION_MS` | No | `604800000` (7d) | Refresh token lifespan |
| `SECURITY_LOCKOUT_MAX_FAILED_ATTEMPTS` | No | `5` | Lockout threshold |
| `SECURITY_LOCKOUT_DURATION` | No | `15m` | Lockout duration |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Deployment | *empty* | Comma-separated allowed frontend origins (default: empty / default-deny) |
| `SECURITY_CORS_ALLOWED_METHODS` | No | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | Permitted HTTP verbs |
| `SECURITY_CORS_ALLOWED_HEADERS` | No | `Authorization,Content-Type,Accept,Origin,X-Requested-With` | Permitted incoming HTTP headers |
| `SECURITY_CORS_EXPOSED_HEADERS` | No | `Authorization` | Response headers accessible to browser JS |
| `SECURITY_CORS_ALLOW_CREDENTIALS` | No | `false` | Access-Control-Allow-Credentials flag |
| `SECURITY_CORS_MAX_AGE` | No | `3600s` | Preflight cache TTL |
| `DB_USERNAME` | **YES** | *None* | Database connection username |
| `DB_PASSWORD` | **YES** | *None* | Database connection password |

---

## 9. Production Deployment vs. Local Development

### 9.1 Local Development Configuration
For local development, frontends typically run on development web servers (e.g., Vite, Next.js, Angular CLI):
```bash
export SECURITY_CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:5173"
export JWT_SECRET="your-local-development-secret-key-must-be-at-least-32-bytes!"
export DB_USERNAME="root"
export DB_PASSWORD="password"
```
Under plain HTTP (`http://localhost`), HSTS headers are automatically omitted per RFC 6797, preventing browsers from forcefully redirecting local development to nonexistent local HTTPS certificates.

### 9.2 Production Deployment Configuration
In production environments:
1. `SECURITY_CORS_ALLOWED_ORIGINS` **must** be set to the exact production frontend domains:
   ```bash
   export SECURITY_CORS_ALLOWED_ORIGINS="https://portal.sih.gov.in,https://admin.sih.gov.in"
   ```
2. Production traffic must terminate TLS (HTTPS). When requests arrive with TLS termination (or behind an ingress with `X-Forwarded-Proto: https`), Spring Security automatically emits `Strict-Transport-Security`.
3. Never configure `*` or wildcard patterns for production CORS.

---

## 10. Items Intentionally Deferred

1. **Server-Side Token Revocation / Blacklist**: JWT access tokens are short-lived (15 minutes). Redis or database-backed access token blocklisting is deferred.
2. **Application-Level IP Rate Limiting**: Dedicated rate limiting (e.g., token bucket algorithms per IP) is deferred to the edge API gateway or reverse proxy (Nginx / Cloudflare / Envoy).
3. **Casually Preloading HSTS**: The HSTS `preload` directive is intentionally omitted pending formal submission to the Chromium HSTS preload list.
