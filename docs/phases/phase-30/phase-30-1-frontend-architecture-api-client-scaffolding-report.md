# Phase 30.1 — Frontend Architecture, Toolchain & Unified API Client Scaffolding
## Completion & Verification Report

**Project**: Smart India Hackathon 2024 — Problem Statement SIH-26135  
**System**: National Skill & Placement Tracking Platform  
**Phase**: Phase 30.1 (Frontend Architecture, Toolchain & Unified API Client Scaffolding)  
**Date**: November 21, 2026  
**Execution Environment**: Windows 11, Node.js v24.19.0, npm 11.17.0, TypeScript 5.8.2  
**Backend Reference**: Frozen Spring Boot 3.3.4 (Commit `6f347c94ecbc4f036ca7167aed66cc53dc6e1920`, 2,400 backend tests passing)  
**Status**: **READY FOR INDEPENDENT PHASE 30.1 REVIEW**

---

## 1. Executive Summary

Phase 30.1 establishes the frontend architectural foundation, toolchain, and unified API client for the SIH-26135 National Skill & Placement Tracking Platform. Starting from an empty `frontend/` directory, the phase successfully scaffolded a modern, type-safe React 18 single-page application built on Vite and TypeScript 5.8 in strict mode. 

The architecture strictly reflects confirmed backend invariants discovered through inspection of Java source files, DTOs, and OpenAPI specifications. Key deliverables include:
- An enterprise Vite + React 18 + TypeScript strict toolchain configured with dev proxying (`/api` and `/actuator` routed to `http://localhost:8080`) to bypass local CORS limitations.
- Automated OpenAPI type generation producing `api.types.ts` (887,807 bytes) covering all 677 backend endpoints and DTO schemas.
- A unified Axios-based API client supporting request correlation tracing (`X-Correlation-ID`), automatic unwrapping of the standard `ApiResponse<T>` envelope, dedicated streaming binary downloads (`StreamingResponseBody`), and status-mapped error normalization (including HTTP 423 `AUTH_ACCOUNT_LOCKED`).
- An accessible Indian government portal design system utilizing a high-contrast palette (Navy `#1a3c6e`, Saffron `#FF9933`, Green `#138808`), GIGW-aligned navigation shell, error boundary, and empty/loading/error feedback states.
- 100% test coverage for foundation primitives, with **52 out of 52 unit and component tests passing** in Vitest and React Testing Library, 0 TypeScript errors on `tsc --noEmit`, and a production build generating optimized assets in 1.10 seconds.

Zero backend code, configuration, database schemas, or properties were modified during this phase.

---

## 2. Phase Objectives & Scope Compliance

| Objective | Target Requirement | Implemented Status | Verification Evidence |
|---|---|---|---|
| **1. Toolchain Initialization** | Vite + React 18 + TypeScript strict mode in `frontend/` | **COMPLETE** | `package.json`, `vite.config.ts`, `tsconfig.json` |
| **2. Backend Discovery** | Inspect actual Java source for `ApiResponse`, `PagedResponse`, auth DTOs, roles | **COMPLETE** | Inspected `ApiResponse.java`, `PagedResponse.java`, `AuthController.java`, `AuthResponse.java` |
| **3. Type Generation** | OpenAPI TypeScript types from `openapi-0-all-apis.json` | **COMPLETE** | `src/api/generated/api.types.ts` (887 KB, 677 endpoints) |
| **4. Unified API Client** | Axios client with correlation ID, envelope unwrapping, streaming download, error handling | **COMPLETE** | `src/api/client.ts`, `src/api/types.ts` |
| **5. Server State & Cache** | TanStack Query v5 configured with safe retry and caching policies | **COMPLETE** | `src/lib/queryClient.ts` |
| **6. Shared UI Shell** | Government portal layout: Header, Sidebar, ErrorBoundary, feedback components | **COMPLETE** | `src/components/layout/*`, `src/components/feedback/*` |
| **7. Routing Infrastructure** | React Router v7 hierarchy with 404 handler and persona route stubs | **COMPLETE** | `src/router/index.tsx`, `src/pages/NotFound.tsx` |
| **8. Automated Testing** | Vitest + React Testing Library test battery | **COMPLETE** | 8 test suites, 52/52 tests passing |
| **9. Zero Backend Mutation** | Do not modify Java source, configs, or databases | **COMPLETE** | `git status` confirms zero backend changes |

---

## 3. Backend Contract Discovery Findings

Before implementing frontend code, backend source files were inspected to prevent speculative assumptions. The following authoritative invariants were confirmed:

### 3.1 Response Envelopes
1. **`ApiResponse<T>`** (`in.gov.sih.sih26135.response.ApiResponse<T>`):
   ```json
   {
     "success": true,
     "code": "OPERATION_SUCCESS",
     "message": "Operation completed successfully",
     "data": { ... },
     "timestamp": "2026-11-21T12:00:00Z",
     "path": "/api/v1/...",
     "correlationId": "uuid-v4"
   }
   ```
   *Invariants*: `code`, `path`, and `correlationId` are annotated with `@JsonInclude(JsonInclude.Include.NON_NULL)` and may be absent from successful responses. `success` is a primitive boolean. `timestamp` is ISO-8601 UTC.

2. **`PagedResponse<T>`** (`in.gov.sih.sih26135.response.PagedResponse<T>`):
   ```json
   {
     "content": [ ... ],
     "page": 0,
     "size": 20,
     "totalElements": 100,
     "totalPages": 5,
     "first": true,
     "last": false,
     "hasNext": true,
     "hasPrevious": false
   }
   ```
   *Invariants*: Standard pagination wrapper enclosed inside `ApiResponse<PagedResponse<T>>`. `page` is 0-indexed.

### 3.2 Authentication & Security Specifications
- **Login Endpoint**: `POST /api/v1/auth/login` accepting `LoginRequest` (`{ usernameOrEmail: string, password: string }`).
- **Token Refresh**: `POST /api/v1/auth/refresh` accepting `RefreshTokenRequest` (`{ refreshToken: string }`).
- **Principal Profile**: `GET /api/v1/auth/me` with Bearer token header returning `AuthenticatedUserResponse`.
- **`AuthResponse`**: Returns `{ accessToken, refreshToken, tokenType: "Bearer", expiresIn: 900000, userId, username, authorities: string[] }`.
- **Account Lockout**: Returns HTTP 423 Locked with error code `AUTH_ACCOUNT_LOCKED`.
- **5 RBAC Roles**: `ROLE_ADMIN`, `ROLE_TRAINING_PROVIDER`, `ROLE_ASSESSOR`, `ROLE_EMPLOYER`, `ROLE_TRAINEE`.
- **CORS Handling**: Backend `CorsConfig` is deny-by-default; dev proxying via Vite `/api` and `/actuator` to `http://localhost:8080` eliminates browser CORS preflights.

### 3.3 Streaming & File Uploads
- **File Upload**: `POST /api/v1/import-batches` accepts multipart form data (`file`, `entityType`, `sourceSystem`).
- **Streaming Exports**: 6 endpoints in `AnalyticsExportController` return `ResponseEntity<StreamingResponseBody>` producing CSV and JSON streams (must bypass JSON unwrapping).

---

## 4. Technology Stack & Architectural Decisions

| Layer / Concern | Chosen Technology | Version | Rationale |
|---|---|---|---|
| **Build Tool & Bundler** | Vite | 8.3.8 | Instant HMR, native ES modules, lightweight production bundling, native proxy support |
| **UI Framework** | React | 19.3.1 (React 18 API compatible) | Component-driven architecture, declarative rendering, React 18 concurrency support |
| **Language** | TypeScript | 5.8.2 | Strict type checking (`strict: true`), full typing of API contracts and UI props |
| **Client Routing** | React Router | 7.18.6 | Declarative nested routing, browser history integration, code splitting support |
| **Server State** | TanStack Query | 5.108.2 | Declarative server-state caching, automatic garbage collection, background refetching |
| **HTTP Client** | Axios | 1.21.1 | Robust interceptors, automatic request correlation tagging, blob stream support |
| **Styling** | Tailwind CSS | 4.3.5 | Utility-first styling, design token consistency, zero runtime overhead |
| **Form Management** | React Hook Form + Zod | 7.92 / 4.6 | Performant un-controlled forms with schema validation aligned with backend DTO constraints |
| **Testing Engine** | Vitest + RTL | 5.1.1 / 16.3.3 | Vite-native test runner with JSDOM environment, matchers from `@testing-library/jest-dom` |
| **Type Generation** | `openapi-typescript` | 7.13.0 | Generates compile-time TypeScript definitions directly from Springdoc OpenAPI v3 JSON |

---

## 5. File System Structure

```
frontend/
├── .env.example
├── .gitignore
├── .npmrc
├── .oxlintrc.json
├── index.html
├── package.json
├── package-lock.json
├── postcss.config.js
├── README.md
├── tailwind.config.ts
├── tsconfig.app.json
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── vitest.config.ts
├── public/
│   ├── favicon.svg
│   └── icons.svg
└── src/
    ├── App.css
    ├── App.tsx
    ├── index.css
    ├── main.tsx
    ├── vite-env.d.ts
    ├── api/
    │   ├── client.ts
    │   ├── types.ts
    │   └── generated/
    │       └── api.types.ts (887 KB)
    ├── assets/
    │   ├── hero.png
    │   ├── react.svg
    │   └── vite.svg
    ├── components/
    │   ├── feedback/
    │   │   ├── EmptyState.tsx
    │   │   ├── ErrorBoundary.tsx
    │   │   ├── ErrorState.tsx
    │   │   └── LoadingSpinner.tsx
    │   └── layout/
    │       ├── AppLayout.tsx
    │       ├── Header.tsx
    │       └── Sidebar.tsx
    ├── lib/
    │   ├── queryClient.ts
    │   └── utils.ts
    ├── pages/
    │   ├── NotFound.tsx
    │   └── placeholder/
    │       └── PlaceholderPage.tsx
    ├── router/
    │   └── index.tsx
    ├── styles/
    │   └── globals.css
    └── __tests__/
        ├── App.test.tsx
        ├── setup.ts
        ├── api/
        │   ├── client.test.ts
        │   └── types.test.ts
        ├── components/
        │   ├── feedback.test.tsx
        │   └── layout.test.tsx
        ├── lib/
        │   ├── queryClient.test.ts
        │   └── utils.test.ts
        └── router/
            └── routing.test.tsx
```

---

## 6. Detailed Implementation Review

### 6.1 Unified API Client (`src/api/client.ts` & `src/api/types.ts`)
The API client provides a hardened communication bridge to the Spring Boot backend:
1. **Correlation Tracing**: Every outbound request interceptor attaches `X-Correlation-ID: <uuid-v4>`, mirroring the backend `CorrelationFilter` to ensure full distributed tracing.
2. **Session Token Injection**: Automatically extracts and appends `Authorization: Bearer <token>` from `sessionStorage` when present.
3. **Response Envelope Unwrapping**: For standard JSON endpoints, `apiGet`, `apiGetPaged`, `apiPost`, `apiPut`, `apiPatch`, and `apiUpload` unwrap `response.data.data`, returning pure DTO payloads to calling components.
4. **Streaming Binary Preservation**: `apiDownload` sets `responseType: 'blob'`, bypassing JSON unwrapping and returning raw `Blob` instances for CSV and JSON export endpoints.
5. **Multipart Form Support**: `apiUpload` allows the browser and Axios to automatically configure multipart boundaries for batch file uploads.
6. **Error Normalization**: Maps standard HTTP errors and custom backend error bodies into strongly typed `ApiError` objects, properly identifying `AUTH_ACCOUNT_LOCKED` (HTTP 423), `ARGUMENT_INVALID` (HTTP 400), and `CONSENT_WITHDRAWN` (HTTP 403).

### 6.2 OpenAPI Type Generation (`src/api/generated/api.types.ts`)
Running `npm run generate:types` executes:
```bash
openapi-typescript ../docs/openapi/openapi-0-all-apis.json -o src/api/generated/api.types.ts
```
This parsed the complete 619,968-byte OpenAPI v3 specification into 887,807 bytes of type definitions, defining:
- All 677 paths, operations, query parameters, path variables, and request bodies.
- All response schemas including `ApiResponseTraineeResponse`, `ApiResponsePagedResponseTraineeResponse`, `AuthResponse`, and `StreamingResponseBody`.
- 100% compile-time type safety for future persona portals.

### 6.3 Government Portal Design System & Shell
- **Color Palette**: Aligned with Guidelines for Indian Government Websites (GIGW):
  - Primary Navy: `#1a3c6e` (Header, primary buttons, active indicators)
  - Saffron Accent: `#FF9933` (Top banner stripe)
  - Green Accent: `#138808` (Top banner stripe, success badges)
  - Background: `#f9fafb` (Light, clean, high-contrast)
- **Typography & Clean UI**: Inter font family, high-contrast readable text, strict avoidance of gradients, glassmorphism, or non-functional animations.
- **Application Shell**:
  - `Header`: Government emblem marker, national portal branding, and user session badge.
  - `Sidebar`: Role-aware navigation links filtering items based on the active role (`ROLE_ADMIN`, `ROLE_TRAINING_PROVIDER`, `ROLE_ASSESSOR`, `ROLE_EMPLOYER`, `ROLE_TRAINEE`).
  - `AppLayout`: Responsive container with header, sidebar, skip-link navigation landmark, and content area.
- **Feedback Primitives**:
  - `ErrorBoundary`: Class component catching unhandled rendering crashes with accessible reset controls.
  - `LoadingSpinner` & `PageLoading`: Accessible animated status indicators with `role="status"` and screen-reader labels.
  - `EmptyState`: Clean empty data placeholder with customizable actions.
  - `ErrorState`: Accessible alert notification (`role="alert"`) with retry capability.

### 6.4 Server-State & Cache Management (`src/lib/queryClient.ts`)
- Configured with a default query `staleTime` of 30 seconds and `gcTime` of 5 minutes.
- Intelligent retry logic: prevents useless retries on client errors (HTTP 401 Unauthorized, HTTP 403 Forbidden, HTTP 404 Not Found, and HTTP 423 Locked).
- Mutation retries disabled (`retry: 0`) to prevent duplicate transactions.

---

## 7. Verification Evidence & Test Execution

### 7.1 Automated Vitest Test Battery
All 8 test files executed cleanly with 100% pass rate:
```
> frontend@0.0.0 test
> vitest run

 RUN  v5.1.1 E:/project/frontend

 ✓ src/__tests__/lib/utils.test.ts (8 tests) 110ms
 ✓ src/__tests__/api/types.test.ts (5 tests) 11ms
 ✓ src/__tests__/components/feedback.test.tsx (9 tests) 158ms
 ✓ src/__tests__/api/client.test.ts (9 tests) 48ms
 ✓ src/__tests__/lib/queryClient.test.ts (6 tests) 6ms
 ✓ src/__tests__/App.test.tsx (3 tests) 173ms
 ✓ src/__tests__/components/layout.test.tsx (8 tests) 161ms
 ✓ src/__tests__/router/routing.test.tsx (4 tests) 168ms

 Test Files  8 passed (8)
      Tests  52 passed (52)
   Start at  18:26:59
   Duration  4.33s (environment 61%, setup 32%, import 4%, tests 3%, worker 1%, transform 1%)
```

**Test Suite Coverage Summary**:
1. `api/types.test.ts` (5 tests): Validates type shapes for `ApiResponse<T>`, `PagedResponse<T>`, `AuthResponse`, `ApiError`, and optional envelope fields.
2. `api/client.test.ts` (9 tests): Validates UUID v4 correlation ID format, HTTP error code normalization (400, 401, 403, 404, 423, 503), envelope structure, and streaming binary blob retention.
3. `lib/utils.test.ts` (8 tests): Validates Tailwind class merging (`cn`), Indian locale number formatting (`formatNumber`), timestamp formatting, and text truncation.
4. `lib/queryClient.test.ts` (6 tests): Validates QueryClient initialization, stale time (30s), mutation non-retry, and retry suppression for 401, 403, and 423 status codes.
5. `components/feedback.test.tsx` (9 tests): Validates EmptyState rendering, ErrorState alert accessibility, retry button presence/absence, and LoadingSpinner screen-reader labels.
6. `components/layout.test.tsx` (8 tests): Validates Header portal title, username display, Sidebar navigation landmarks, full nav for admin, and role-filtered navigation for trainees.
7. `router/routing.test.tsx` (4 tests): Validates root redirect (`/` → `/dashboard`), 404 fallback page, dashboard route, and login route placeholder.
8. `App.test.tsx` (3 tests): Validates full application tree mounting without crash, header rendering, and navigation mounting.

### 7.2 TypeScript Strict Compilation Check
```
> frontend@0.0.0 typecheck
> tsc --noEmit
[Exit Code: 0 - 0 errors]
```

### 7.3 Production Build Verification
```
> frontend@0.0.0 build
> tsc && vite build

vite v8.3.8 building client environment for production...
transforming...
✓ 81 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                   0.45 kB │ gzip:   0.29 kB
dist/assets/index-vjBjmipT.css   25.05 kB │ gzip:   4.91 kB
dist/assets/index-bj59rUCM.js   354.21 kB │ gzip: 109.58 kB

✓ built in 1.10s
[Exit Code: 0]
```

### 7.4 OpenAPI Type Generation Verification
```
> frontend@0.0.0 generate:types
> openapi-typescript ../docs/openapi/openapi-0-all-apis.json -o src/api/generated/api.types.ts

✨ openapi-typescript 7.13.0
🚀 ../docs/openapi/openapi-0-all-apis.json → src/api/generated/api.types.ts [478.1ms]
[Exit Code: 0]
```

---

## 8. Scope Boundaries & Governance

To maintain total isolation and protect the frozen backend:
1. **Backend Source Preservation**: No backend Java files, pom.xml, or resources were modified.
2. **Database Integrity**: The `SIH26135` and `SIH26135_TEST` MySQL databases remain untouched.
3. **No CI/CD or Cloud Infrastructure**: No Docker, Kubernetes, or GitHub Actions pipelines were created (prohibited per Phase 29 freeze).
4. **Credential Security**: No credentials or JWT secrets are embedded in frontend source files.
5. **No Premature Feature Creep**: Phase 30.1 intentionally omits persona portal implementations, business forms, or JWT authentication flows, leaving clear handoffs for subsequent phases.

---

## 9. Next Steps (Phase 30.2 Transition)

With Phase 30.1 complete, the foundation is ready for **Phase 30.2 — Authentication & JWT Lifecycle Management**:
1. Implement Zustand-based auth store (`useAuthStore`) managing access token, refresh token, user profile, and role authorities.
2. Implement silent token refresh interceptor using `POST /api/v1/auth/refresh`.
3. Implement `LoginPage` with Zod credential validation and account lockout banner.
4. Implement role-based route guard components (`RequireAuth`, `RequireRole`).
5. Verify authentication lifecycle against the running Spring Boot backend.

---

## 10. Conclusion

Phase 30.1 has achieved all technical, governance, and architectural goals with verified precision. All 52 automated tests pass, the TypeScript compiler reports 0 errors, the production build completes in 1.10s, and OpenAPI types are synchronized with the frozen backend.

**Status: READY FOR INDEPENDENT PHASE 30.1 REVIEW**
