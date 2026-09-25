# Backend Account Lockout, Brute-Force Protection & Security Audit Architecture (Phase 24.8)

## 1. Lockout Policy

The SIH 26135 backend implements a production-grade account lockout and brute-force protection policy to mitigate online credential-guessing attacks against the authentication endpoints:

- **Maximum Consecutive Failed Attempts**: 5 attempts (configurable via `security.lockout.max-failed-attempts`).
- **Timed Lockout Duration**: 15 minutes (configurable via `security.lockout.lock-duration`).
- **State Enforcement**: Tracked on the `users` table using existing schema columns:
  - `failed_login_count` (SMALLINT UNSIGNED)
  - `locked_until` (DATETIME)
  - `user_status_id` (foreign key to `ref_user_status`: 2 = `ACTIVE`, 3 = `LOCKED`)
  - `last_login_at` (DATETIME)
- **Startup Validation**: Startup sanity checks in `SecurityLockoutProperties` guarantee `max-failed-attempts > 0` and `lock-duration > 0`.

---

## 2. Failed Login Flow

When a user submits credentials to `POST /api/v1/auth/login`:

1. **Non-Existent Identifier**:
   - Querying `findByUsernameOrEmailForUpdate` returns empty.
   - A `LOGIN_FAILURE` security event is recorded with `actorUserId = null`, `entityId = null`, and sanitized summary `"Authentication failed: invalid credentials"`.
   - The application immediately throws `InvalidCredentialsException` resulting in generic HTTP 401 with code `INVALID_CREDENTIALS` and message `"Invalid username/email or password"`.
   - No hint is given regarding whether the username or email exists in the system.

2. **Soft-Deleted User**:
   - `user.getDeletedAt() != null`.
   - A `LOGIN_FAILURE` security event is recorded with sanitized summary `"Authentication failed: invalid credentials"`.
   - Throws `InvalidCredentialsException` (HTTP 401 generic failure).

3. **Account Currently Locked**:
   - `user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())`.
   - Password verification is **bypassed**.
   - Failed attempt counter is not incremented or reset.
   - A `LOGIN_BLOCKED_LOCKED_ACCOUNT` security event is recorded with sanitized summary `"Login blocked: account is currently locked"`.
   - Throws `InvalidCredentialsException` (HTTP 401 generic failure).
   - The client is not informed of remaining lock duration or exact account state.

4. **Invalid Credentials**:
   - `passwordEncoder.matches(rawPassword, user.getPasswordHash())` evaluates to `false`.
   - `failed_login_count` is incremented by 1.
   - If `failed_login_count >= maxFailedAttempts`:
     - `locked_until` is set to `LocalDateTime.now().plus(lockDuration)`.
     - `user_status_id` transitions to `LOCKED` (status code 3).
     - Both `LOGIN_FAILURE` (`"Authentication failed: invalid credentials"`) and `ACCOUNT_LOCKED` (`"Account locked after repeated authentication failures"`) security events are persisted.
     - An `AuditLog` entry (action: `UPDATE`) is recorded on entity `User` with change summary `"Account locked after repeated authentication failures"`.
   - If `failed_login_count < maxFailedAttempts`:
     - Updated counter is saved to database.
     - A `LOGIN_FAILURE` security event is persisted (`"Authentication failed: invalid credentials"`).
   - Throws `InvalidCredentialsException` without revealing current counter, threshold, or password validity.

---

## 3. Successful Login Flow

When password verification succeeds:

1. `user.failed_login_count` is reset to `0`.
2. `user.locked_until` is cleared to `null`.
3. `user.last_login_at` is set to `LocalDateTime.now()`.
4. `user.user_status_id` is verified/set to `ACTIVE` (status code 2).
5. The updated entity is persisted transactionally.
6. A `LOGIN_SUCCESS` security event is recorded in `system_event_logs` with summary `"Authentication successful"`.
7. Role and permission authorities are compiled into the `authorities` collection.
8. Signed JWT access token (15m expiry) and refresh token (7d expiry) are generated and returned in `AuthResponse`.

---

## 4. Expired Lock Behavior

When an account with a previously active lock attempts authentication after `locked_until <= now()`:

- **Recovery from `LOCKED`**:
  - If the user's status was `LOCKED` due to prior consecutive failed attempts, the expired lock condition is recognized.
  - The status is restored to `ACTIVE`.
  - `locked_until` is set to `null` and `failed_login_count` is reset to `0`.
  - An `ACCOUNT_UNLOCKED` security audit event is logged with summary `"Account lock expired; account restored to active state"`.
  - The authentication attempt is allowed to proceed with credential verification.

- **Protection Against Inactive Status Mutation**:
  - Accounts with statuses `DISABLED`, `ARCHIVED`, or `PENDING` are **never** reactivated by lock expiration.
  - If a user is marked `DISABLED` or `ARCHIVED`, the authentication process rejects the attempt with `LOGIN_FAILURE` and throws `InvalidCredentialsException`, keeping the existing status unchanged.

---

## 5. Authoritative Reference Data Resolution & Explicit Failure Policy

- **Authoritative Resolution**:
  - All reference metadata (`RefSystemEventCategory`, `RefSystemEventSeverity`, `RefAuditAction`, `RefUserStatus`) is resolved dynamically from the authoritative database reference tables.
  - Strictly **zero** hardcoded ID fallbacks (no `1L`, `2L`, `3L`).
  - Strictly **zero** synthetic in-memory reference entities created when a lookup fails.
  - If required reference records are missing from the database, the operation fails explicitly with `ResourceNotFoundException`.

- **Explicit Audit Failure Policy**:
  - Security audit and system event logging are mandatory compliance operations.
  - Failures in persisting required audit or system events are **never** caught and silently swallowed.
  - If audit persistence fails, the failure propagates immediately, preventing the application from falsely behaving as though the audit record succeeded.
  - `GlobalExceptionHandler` ensures that server-side audit failures produce sanitized, generic error responses without exposing database internals, stack traces, or SQL exception messages to clients.

---

## 6. Concurrency Strategy

To prevent race conditions, lost updates, and counter overwrite anomalies during concurrent login requests for the same account:

- **Pessimistic Row-Level Locking (`PESSIMISTIC_WRITE`)**:
  - `UserRepository.findByUsernameOrEmailForUpdate` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
  - In MySQL InnoDB, this issues a `SELECT ... FOR UPDATE` query, locking the target row for the duration of the transaction.
  - Parallel requests targeting the same user are serialized at the database row level.
  - If 5 concurrent invalid login requests arrive simultaneously, each transaction reads the committed counter increment from the prior transaction. The 5th transaction reliably locks the account, and subsequent transactions immediately hit the `isLocked` check.

- **Transactional Boundaries**:
  - `AuthenticationServiceImpl.login` is annotated with `@Transactional(noRollbackFor = {InvalidCredentialsException.class})`.
  - When `InvalidCredentialsException` is thrown, Spring commits the transaction, ensuring that counter updates, lockout timestamps, and security audit logs are persisted.
  - Unexpected system errors trigger standard rollbacks.

---

## 7. Security Audit Events Catalog

Security events are written directly to `system_event_logs` and `audit_logs` using sanitized event summaries:

| Event Code | Category | Severity | Sanitized Event Summary |
|---|---|---|---|
| `LOGIN_SUCCESS` | `SECURITY` | `INFO` | `"Authentication successful"` |
| `LOGIN_FAILURE` | `SECURITY` | `WARNING` | `"Authentication failed: invalid credentials"` |
| `ACCOUNT_LOCKED` | `SECURITY` | `ERROR` | `"Account locked after repeated authentication failures"` |
| `LOGIN_BLOCKED_LOCKED_ACCOUNT` | `SECURITY` | `WARNING` | `"Login blocked: account is currently locked"` |
| `ACCOUNT_UNLOCKED` | `SECURITY` | `INFO` | `"Account lock expired; account restored to active state"` |
| `TOKEN_REFRESH_SUCCESS` | `SECURITY` | `INFO` | `"Refresh token renewed successfully"` |
| `TOKEN_REFRESH_FAILURE` | `SECURITY` | `WARNING` | `"Refresh token validation failed"` |

In addition, an `AuditLog` entry (`action: UPDATE`, `entity: User`, summary: `"Account locked after repeated authentication failures"`) is recorded whenever an account transitions to `LOCKED`.

---

## 8. Sensitive-Data Logging Rules

All authentication logging strictly conforms to the project's zero-leakage security standard:

- **Never Logged in Application Logs or Audit Trails**:
  - Plaintext passwords
  - Password hashes (Argon2id strings)
  - JWT access tokens
  - JWT refresh tokens
  - Authorization headers
  - JWT secrets or cryptographic keys
  - Current failed-attempt count (e.g. no "attempt X of Y")
  - Maximum failed-attempt threshold
  - Exact `locked_until` timestamps
  - Configured lockout duration
- **Safe Audit Metadata**:
  - User ID (when user exists; `null` for unknown identifiers)
  - Entity type (`USER` or `User`)
  - Source component (`AUTH_SERVICE`)
  - Generalized, sanitized outcome summary
  - Timestamp of event

---

## 9. Configuration Properties

Configured in `application.properties` and backed by environment variables:

```properties
# Account Lockout & Brute-Force Protection
security.lockout.max-failed-attempts=${SECURITY_LOCKOUT_MAX_FAILED_ATTEMPTS:5}
security.lockout.lock-duration=${SECURITY_LOCKOUT_DURATION:15m}
```

Bound to `SecurityLockoutProperties` class:
- `security.lockout.max-failed-attempts` (int, default: 5)
- `security.lockout.lock-duration` (Duration, default: `PT15M` / 15 minutes)

---

## 10. CustomUserDetailsService Read-Only Architecture

- `CustomUserDetailsService` is strictly read-only (`@Transactional(readOnly = true)`).
- It verifies account active status and respects active `locked_until` timestamps by rejecting locked accounts with `UsernameNotFoundException`.
- It does **not** modify `failed_login_count`, `locked_until`, or user status, does **not** reactivate accounts, and does **not** write duplicate audit logs. All state mutations and security auditing remain exclusively owned by `AuthenticationServiceImpl`.

---

## 11. Database Schema Invariance

- **Database Changes**: **0**.
- The existing columns `failed_login_count`, `locked_until`, `last_login_at`, and `user_status_id` in table `users` (defined in `database/phase-02-users-security.sql`) are utilized without modification.
- Reference statuses in `ref_user_status` (`ACTIVE = 2`, `LOCKED = 3`, `DISABLED = 4`, `ARCHIVED = 5`) and event tables `system_event_logs` / `audit_logs` (defined in `database/phase-15-audit-data-quality-logs.sql`) are used without changes.

---

## 12. Explicitly Deferred Phase 24.9 Hardening

- Production CORS Origin Whitelist configuration
- HTTP Strict Transport Security (HSTS)
- Content Security Policy (CSP)
- X-Frame-Options and security response headers
