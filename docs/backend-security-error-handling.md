# Backend Security Error Handling — Phase 24.6

## 1. Overview
Phase 24.6 implements standardized HTTP security exception handling for the SIH 26135 Spring Boot backend. It replaces default Spring Security HTML or empty servlet error pages with uniform, sanitized JSON responses adhering to the project's standard [`ApiResponse`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/response/ApiResponse.java) envelope for:
- **401 Unauthorized**: Unauthenticated requests to protected endpoints.
- **403 Forbidden**: Authenticated requests lacking required permissions or authorities.

---

## 2. Security Exception Handlers

### A. 401 Unauthorized — [`RestAuthenticationEntryPoint`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/handler/RestAuthenticationEntryPoint.java)
- **Interface:** `org.springframework.security.web.AuthenticationEntryPoint`
- **Trigger:** Invoked when an unauthenticated client attempts to access a protected API route (e.g. missing, malformed, expired, or tampered JWT bearer token).
- **HTTP Status:** `401 Unauthorized`
- **Content-Type:** `application/json;charset=UTF-8`
- **Error Code:** `UNAUTHORIZED`
- **Sanitized Message:** `"Authentication is required to access this resource"`
- **Response Format:**
  ```json
  {
    "success": false,
    "code": "UNAUTHORIZED",
    "message": "Authentication is required to access this resource",
    "data": null,
    "timestamp": "2026-09-25T11:10:00Z",
    "path": "/api/v1/trainees"
  }
  ```
- **Information Leakage Prevention:** Strictly excludes internal stack traces, JWT parsing exceptions, token secret details, and sensitive user data.

### B. 403 Forbidden — [`RestAccessDeniedHandler`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/handler/RestAccessDeniedHandler.java)
- **Interface:** `org.springframework.security.web.access.AccessDeniedHandler`
- **Trigger:** Invoked when an authenticated user attempts an operation or endpoint for which their assigned roles and permissions are insufficient.
- **HTTP Status:** `403 Forbidden`
- **Content-Type:** `application/json;charset=UTF-8`
- **Error Code:** `FORBIDDEN`
- **Sanitized Message:** `"Access denied"`
- **Response Format:**
  ```json
  {
    "success": false,
    "code": "FORBIDDEN",
    "message": "Access denied",
    "data": null,
    "timestamp": "2026-09-25T11:10:00Z",
    "path": "/api/v1/admin/users"
  }
  ```
- **Information Leakage Prevention:** Strictly excludes role hierarchy details, required permission codes, internal method names, and database metadata.

---

## 3. ApiResponse Envelope Integration
Security handlers leverage the project's existing [`ApiResponse<T>`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/response/ApiResponse.java) envelope:
- `success`: `false` for error responses.
- `code`: Stable machine-readable error code (`UNAUTHORIZED`, `FORBIDDEN`). Excluded via `@JsonInclude(NON_NULL)` on successful responses to preserve 100% backward compatibility.
- `message`: Sanitized human-readable error description.
- `data`: `null` for error responses.
- `timestamp`: Instant representing the exact error timestamp.
- `path`: Request URI extracted dynamically from `HttpServletRequest.getRequestURI()`.

---

## 4. Protected Route & Invalid JWT Behavior

### Protected-Route Behavior
- Public endpoints (`POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `/actuator/health`, `/error`) permit unauthenticated access without triggering the entry point.
- All other endpoints (`GET /api/v1/auth/me`, business REST controllers) require authentication. Unauthenticated requests are intercepted by Spring Security's `AuthorizationFilter` and dispatched to `RestAuthenticationEntryPoint`.

### Invalid / Expired / Tampered JWT Behavior
- When a request supplies an invalid, expired, tampered, or malformed JWT (or a refresh token presented as an access token), [`JwtAuthenticationFilter`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/jwt/JwtAuthenticationFilter.java) catches `JwtValidationException` and immediately clears the security context via `SecurityContextHolder.clearContext()`.
- The request proceeds unauthenticated down the filter chain.
- If the target endpoint is protected, `AuthorizationFilter` rejects the unauthenticated request and delegates to `RestAuthenticationEntryPoint`, producing a standardized HTTP 401 response with code `UNAUTHORIZED`.
- Invalid tokens never authenticate anonymously and never return HTTP 200.

---

## 5. Distinction: Security Handlers vs GlobalExceptionHandler

| Dimension | Security Filter Errors (Phase 24.6) | Controller & Application Errors (Phase 20.5 / 24.5) |
|---|---|---|
| **Handler** | `RestAuthenticationEntryPoint` & `RestAccessDeniedHandler` | `GlobalExceptionHandler` (`@RestControllerAdvice`) |
| **Pipeline Stage** | Spring Security filter chain (before `DispatcherServlet`) | Spring MVC dispatching (inside `DispatcherServlet`) |
| **Trigger Exceptions** | `AuthenticationException`, `AccessDeniedException` | `InvalidCredentialsException`, `BadRequestException`, `ResourceNotFoundException`, etc. |
| **Example Endpoints** | Missing/invalid token on `GET /api/v1/trainees` or `GET /api/v1/auth/me` | Wrong password on `POST /api/v1/auth/login`, invalid token on `POST /api/v1/auth/refresh` |
| **Error Code** | `UNAUTHORIZED`, `FORBIDDEN` | `INVALID_CREDENTIALS`, `BAD_REQUEST`, etc. |

This boundary preserves clean separation of concerns and prevents conflicting or duplicate exception translation.

---

## 6. Integration with SecurityConfig
Configured in [`SecurityConfig.java`](file:///E:/project/backend/src/main/java/in/gov/sih/sih26135/security/config/SecurityConfig.java) via constructor injection and the Spring Security DSL:

```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .httpBasic(AbstractHttpConfigurer::disable)
    .formLogin(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .exceptionHandling(exception -> exception
        .authenticationEntryPoint(restAuthenticationEntryPoint)
        .accessDeniedHandler(restAccessDeniedHandler)
    )
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
        .requestMatchers("/api/v1/auth/me").authenticated()
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/error").permitAll()
        .anyRequest().authenticated()
    )
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

---

## 7. Deferred Security Phases
- **Phase 24.7 (Endpoint & Method-Level RBAC Authorization):** `@PreAuthorize`, `@Secured`, `hasRole()`, `hasAuthority()`, and method security configuration are deferred to Phase 24.7.
- **Phase 24.8 (Account Lockout & Login Audit Logging):** Failed attempt throttling and audit logs are deferred to Phase 24.8.
- **Phase 24.9 (CORS, CSRF & Security Header Hardening):** Production browser security headers and CORS configuration remain deferred to Phase 24.9.
