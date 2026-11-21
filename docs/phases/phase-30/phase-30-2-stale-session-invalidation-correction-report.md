# Phase 30.2 — Stale Session Invalidation & Cross-Session Replay Prevention Report

**Project**: National Skill & Placement Tracking Platform (SIH-26135)  
**Phase**: Phase 30.2 Corrections  
**Date**: November 2026  
**Status**: COMPLETED — READY FOR INDEPENDENT VERIFICATION AUDIT  
**Scope**: React + TypeScript Frontend (`frontend/src/api/client.ts`) & Test Battery  

---

## 1. Executive Summary

This investigation inspected `frontend/src/api/client.ts`, focusing on the Axios HTTP 401 response interceptor and its refresh-failure catch handlers. 

Two critical cross-session race conditions were investigated:
1. **Stale Session Invalidation**: Whether an outdated request or delayed refresh failure could invoke `clearSession()` after a newer user session has already been established.
2. **Cross-Session Request Replay**: Whether a delayed request originating from an older user session could be replayed with a newly authenticated user's access token upon receiving an HTTP 401 response.

Both vulnerabilities were **CONFIRMED** in the uncorrected implementation and resolved using the existing `sessionGeneration` counter from `sessionManager.ts`. 

A battery of **4 new deterministic race-condition tests** was added to `clientInterceptors.test.ts`, raising the total test count to **132 passing tests across 14 test suites** (`100% pass rate`). Static typechecking passed with **0 errors**, production compilation completed in **1.28s**, and zero backend or database mutations were made.

---

## 2. Exact Findings

### Finding 1: Delayed Refresh Failure or Outdated Request Terminating a Newer Session
- **Status**: **CONFIRMED**
- **Source Inspection (`client.ts`)**:
  - In `apiClient.interceptors.response.use`, when an HTTP 401 response occurred:
    ```ts
    // UNCORRECTED:
    if (isSafeMethod || isExplicitlyIdempotent) {
      try {
        const newAccessToken = await refreshAccessToken();
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch {
        clearSession(); // <-- UNCONDITIONAL CLEAR
        return Promise.reject(normalizeAxiosError(axiosError));
      }
    } else {
      refreshAccessToken().catch(() => clearSession()); // <-- UNCONDITIONAL CLEAR
      return Promise.reject(normalizeAxiosError(axiosError));
    }
    ```
  - **The Flaw**: If User 1 dispatched a request that encountered a delayed 401, and in the interim User 1 logged out and User 2 logged in:
    1. The 401 interceptor triggered for User 1's request.
    2. If `refreshAccessToken()` rejected (e.g., because User 1's refresh token was revoked or `sessionManager` detected session invalidation), the `catch` block in `client.ts` executed `clearSession()`.
    3. `clearSession()` wiped out User 2's active session, cleared User 2's in-memory token, and cleared the TanStack query cache, logging User 2 out prematurely.

### Finding 2: Cross-Session Request Replay with Newer User's Access Token
- **Status**: **CONFIRMED**
- **Source Inspection (`client.ts`)**:
  - In `apiClient.interceptors.request.use`, outgoing requests were not stamped with the active session generation.
  - When an HTTP 401 arrived for a request dispatched under Session 1:
    - If Session 2 (e.g. an Administrator) had since been established, `hasSession()` evaluated to `true`.
    - If the request was a safe read (`GET`), `client.ts` called `refreshAccessToken()` (obtaining Session 2's token), set `originalRequest.headers['Authorization'] = Bearer ${newAccessToken}`, and dispatched `apiClient(originalRequest)`.
  - **The Flaw**: A private query dispatched by User 1 was replayed to the server under User 2's identity and permissions, violating user boundary isolation.

---

## 3. Minimal Correction Applied

The smallest, targeted correction was applied to `frontend/src/api/client.ts` leveraging the existing `getSessionGeneration()` mechanism:

### 3.1 Request Interceptor Stamping
In `apiClient.interceptors.request.use`:
```ts
// Attach Bearer token from session manager if present
const token = getSessionAccessToken();
if (token && !config.headers['Authorization']) {
  config.headers['Authorization'] = `Bearer ${token}`;
}

// Record session generation at dispatch time to prevent cross-session replay or stale invalidation
const requestWithGen = config as InternalAxiosRequestConfig & { _sessionGeneration?: number };
if (requestWithGen._sessionGeneration === undefined) {
  requestWithGen._sessionGeneration = getSessionGeneration();
}

return config;
```

### 3.2 Response Interceptor Stale-Request Guard & Invalidation Protection
In `apiClient.interceptors.response.use`:
```ts
const currentGeneration = getSessionGeneration();
const requestGeneration = (originalRequest as InternalAxiosRequestConfig & { _sessionGeneration?: number })
  ?._sessionGeneration;

// Invariant 1: If request originated from an older session generation, do NOT replay or refresh.
// An outdated request from a previous session must NEVER be replayed with a newer user's token,
// and must NEVER trigger session clearing on a newer session.
if (requestGeneration !== undefined && requestGeneration !== currentGeneration) {
  return Promise.reject(normalizeAxiosError(axiosError));
}

// Do not attempt refresh for auth endpoints themselves to prevent loops
if (!isAuthEndpoint && hasSession()) {
  originalRequest._retry = true;

  const method = (originalRequest.method ?? 'get').toLowerCase();
  const isSafeMethod = ['get', 'head', 'options'].includes(method);
  const isExplicitlyIdempotent = originalRequest.headers?.['X-Idempotent'] === 'true';

  if (isSafeMethod || isExplicitlyIdempotent) {
    try {
      const newAccessToken = await refreshAccessToken();

      // Invariant 2: Verify session was not logged out or replaced while refresh was in-flight
      if (getSessionGeneration() !== currentGeneration) {
        return Promise.reject(normalizeAxiosError(axiosError));
      }

      originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
      return apiClient(originalRequest);
    } catch {
      // Invariant 3: Only clear session if session has not already been superseded by another login/logout
      if (getSessionGeneration() === currentGeneration) {
        clearSession();
      }
      return Promise.reject(normalizeAxiosError(axiosError));
    }
  } else {
    // Invariant 4: For non-idempotent mutation requests (POST, PATCH, PUT, DELETE), do not replay.
    // Guard the background refresh catch handler so it cannot wipe out a newer session.
    refreshAccessToken().catch(() => {
      if (getSessionGeneration() === currentGeneration) {
        clearSession();
      }
    });
    return Promise.reject(normalizeAxiosError(axiosError));
  }
}
```

---

## 4. Test Evidence & Reproduction Battery

Four new deterministic race-condition tests were added to `frontend/src/__tests__/auth/clientInterceptors.test.ts`:

1. **`prevents cross-session replay: request from older session is NOT replayed with newly authenticated user token`**:
   - User 1 logs in (Gen 1).
   - User 1 dispatches `apiGet('/api/v1/user1-resource')`.
   - User 1 logs out and User 2 logs in with distinct tokens (Gen 3).
   - User 1's delayed request receives HTTP 401.
   - **Verified Invariants**:
     - `refreshAccessToken` is NOT called.
     - User 1's request is NOT replayed (`mock.history.get.length === 1`).
     - User 2's tokens remain active (`user2-access-token`) and `hasSession()` remains `true`.

2. **`prevents stale refresh failure from calling clearSession on a newer session`**:
   - User 1 logs in.
   - User 1 sends a request that triggers 401.
   - `refreshAccessToken()` returns a pending promise.
   - While refresh is pending on the network, User 1 logs out and User 2 logs in (`user2-fresh-token`).
   - The pending refresh fails/rejects with an error.
   - **Verified Invariants**:
     - `clearSession` is NOT called on User 2's session.
     - User 2's tokens remain active and uncorrupted.

3. **`aborts request replay if user logs out while token refresh is in flight`**:
   - Request sent and 401 received.
   - `refreshAccessToken()` is held in flight.
   - User logs out (`clearSession()`).
   - Refresh completes with a token.
   - **Verified Invariants**:
     - Replay is aborted (`mock.history.get.length === 1`).
     - Stale request rejects with HTTP 401.
     - Session remains unauthenticated.

4. **`rejects delayed 401 arriving after logout without triggering refresh or redundant session clearance`**:
   - Request dispatched; user logs out before server responds.
   - Delayed HTTP 401 arrives.
   - **Verified Invariants**:
     - Does not call `refreshAccessToken`.
     - Does not invoke redundant `clearSession`.
     - Rejects cleanly.

### 4.1 Test Execution Results
```
Test Files  14 passed (14)
     Tests  132 passed (132)
  Duration  13.42s
```

- `npm run typecheck`: **0 errors**
- `npm run build`: built in **1.28s** (0 errors)

---

## 5. Remaining Unverified Behavior & Governance Audit

### 5.1 Live Backend Integration Status
- **Port 8080 Check**: Port 8080 TCP connection test failed (`WARNING: TCP connect to (127.0.0.1 : 8080) failed`). The Spring Boot backend service is offline.
- **Demo Data Prohibition**: Per mandatory project rules, no fake application accounts were created and no test credentials were seeded into the primary MySQL `SIH26135` database.
- **Status**: **NOT VERIFIED** (backend service stopped; real authentication credentials withheld per governance mandate).

### 5.2 Governance Invariants
- **Backend Code Preservation**: 0 Java files modified (`backend/src/`).
- **Database Schema Preservation**: 0 database mutations or migrations executed.
- **Git & Packaging**: 0 git commits, 0 git pushes, and 0 ZIP archives created.
- **Scope Boundary**: Confined strictly to Phase 30.2 corrections. Phase 30.3 has NOT been started.

---

## 6. Conclusion & Sign-Off

The HTTP 401 interceptor and its refresh-failure catch handlers in `frontend/src/api/client.ts` are now fully protected against cross-session request replay and stale session invalidation races. All 132 tests pass.

**STOPPED AFTER PHASE 30.2 CORRECTIONS — READY FOR INDEPENDENT AUDIT**
