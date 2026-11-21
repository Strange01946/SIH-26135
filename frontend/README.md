# SIH-26135 Frontend — National Skill & Placement Tracking Platform

React 19 + TypeScript strict frontend foundation for the Smart India Hackathon (SIH-26135) National Skill & Placement Tracking Platform.

## Prerequisites

- Node.js 20+ (tested on Node v24.19.0)
- npm 10+ (tested on npm 11.17.0)
- Backend running on `http://localhost:8080` (SIH-26135 Spring Boot 3.3.4)

## Getting Started

```bash
cd frontend
npm install
npm run dev
```

The application will start on `http://localhost:5173`.
All backend requests to `/api` and `/actuator` are automatically proxied to `http://localhost:8080`.

## Available Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Starts Vite development server at `http://localhost:5173` |
| `npm run build` | Runs TypeScript compilation (`tsc`) and Vite production bundle |
| `npm run preview` | Previews the production build locally |
| `npm run typecheck` | Validates TypeScript types across the entire project (`tsc --noEmit`) |
| `npm run test` | Executes the Vitest unit and integration test battery |
| `npm run test:watch` | Runs Vitest in watch mode |
| `npm run test:coverage`| Runs Vitest with coverage report |
| `npm run generate:types` | Regenerates TypeScript API types from OpenAPI spec (`../docs/openapi/openapi-0-all-apis.json`) |

## Architecture & Tech Stack

- **Framework**: React 19 + Vite
- **Language**: TypeScript 5.8 (Strict mode enabled)
- **Routing**: React Router v7 (`react-router-dom`)
- **Server State**: TanStack Query v5 (`@tanstack/react-query`)
- **HTTP Client**: Axios with request/response interceptors
- **Styling**: Tailwind CSS with custom Indian Government design tokens (Navy `#1a3c6e`, Saffron `#FF9933`, Green `#138808`)
- **Testing**: Vitest + React Testing Library + `@testing-library/jest-dom` + `jsdom`
- **Validation**: Zod + React Hook Form + `@hookform/resolvers`
- **Notifications**: Sonner toast system
- **Contract Sync**: `openapi-typescript` for automated type generation from Springdoc OpenAPI specifications

## API Integration Architecture

- **Dev Server Proxy**: `/api/*` and `/actuator/*` forwarded to `http://localhost:8080` (no CORS headers needed in development).
- **Correlation Tracing**: Every outbound HTTP request automatically receives a freshly minted `X-Correlation-ID` header (UUID v4) matching the backend Spring Boot correlation filter.
- **Envelope Handling**: Axios response interceptor unwraps standard backend `ApiResponse<T>` envelopes (`{ success, message, data, timestamp, code, path, correlationId }`).
- **Binary/Streaming Downloads**: Dedicated `apiDownload` utility bypasses JSON unwrapping for `StreamingResponseBody` endpoints (CSV and JSON analytics exports).
- **Error Normalization**: Maps standard HTTP status codes (400, 401, 403, 404, 409, 423, 500, 503) into typed `ApiError` structures, specifically handling HTTP 423 Account Lockout (`AUTH_ACCOUNT_LOCKED`).

## Directory Structure

```
frontend/
├── dist/                   # Production build outputs
├── public/                 # Static assets (favicons, icons)
├── src/
│   ├── api/
│   │   ├── client.ts       # Axios instance, interceptors, error normalizers, HTTP methods
│   │   ├── types.ts        # ApiResponse<T>, PagedResponse<T>, ApiError, Auth DTOs
│   │   └── generated/      # Generated TypeScript types from OpenAPI spec (api.types.ts)
│   ├── components/
│   │   ├── feedback/       # ErrorBoundary, LoadingSpinner, EmptyState, ErrorState
│   │   └── layout/         # AppLayout, Header, Sidebar
│   ├── lib/
│   │   ├── queryClient.ts  # TanStack QueryClient with retry & cache policies
│   │   └── utils.ts        # cn helper, formatters (Indian currency, numbers, dates)
│   ├── pages/
│   │   ├── NotFound.tsx    # 404 page with return to dashboard
│   │   └── placeholder/    # Placeholder page component for persona routes
│   ├── router/
│   │   └── index.tsx       # React Router setup with route hierarchy and fallbacks
│   ├── styles/
│   │   └── globals.css     # Tailwind directives, component layer, design tokens
│   ├── __tests__/          # Vitest test battery
│   │   ├── setup.ts        # Test environment setup with jest-dom matchers
│   │   ├── App.test.tsx    # Root application smoke tests
│   │   ├── api/            # API client and type contract tests
│   │   ├── components/     # Feedback and layout component tests
│   │   ├── lib/            # Utilities and query client configuration tests
│   │   └── router/         # Routing and navigation tests
│   ├── App.tsx             # Root component with providers (QueryClient, Router, Toaster)
│   ├── main.tsx            # Application entry point
│   └── vite-env.d.ts       # Vite client types
├── .env.example            # Environment variable template
├── .npmrc                  # npm configuration
├── package.json            # Dependencies and scripts
├── tailwind.config.ts      # Tailwind configuration with theme extensions
├── tsconfig.json           # TypeScript configuration (strict: true)
├── vite.config.ts          # Vite configuration with proxy and path aliases
└── vitest.config.ts        # Vitest configuration with jsdom and path aliases
```
