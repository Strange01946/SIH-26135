# Phase 30.14 frontend–backend contract validation

The smoke runner starts the packaged Spring Boot backend on `localhost:8080` and serves the production Vite bundle on `127.0.0.1:5173`. Playwright uses the Vite proxy for real authenticated requests. The controlled error-recovery case supplies a single kind of failure, HTTP 503 for the KPI endpoint, then removes the interception and checks the live retry.

## Run locally

Use Java 17, Maven, Node.js 24.19, and MySQL on `127.0.0.1:3306`. Provision the schema and reference records in `SIH26135_TEST` before running. The runner selects an active course linked to an active program and an active state. Set `SIH_TEST_DB_USERNAME` and `SIH_TEST_DB_PASSWORD` for a local account with fixture permissions in that catalog and read access to `performance_schema` connection metadata. Ports 8080 and 5173 must be free.

Install the [India geography directory](../../../database/reference/README.md) in `SIH26135_TEST` before running the nationwide registration and MIS tests.

```powershell
Set-Location backend
mvn -q -DskipTests package
Set-Location ../frontend
npm ci
npx playwright install chromium
npm run typecheck
npm run check:contract
npm test
npm run test:e2e
```

`SIH_E2E_MYSQL`, `SIH_E2E_JAVA`, and `SIH_E2E_CHROMIUM_PATH` can point to local executables. Set `SIH_E2E_BACKEND_JAR` to test a separate build while another backend is running. Playwright filters such as `npm run test:e2e -- --grep 'provider'` run against fresh fixtures. The runner builds the demo bundle, validates live OpenAPI against the verified snapshots, proves that the owned Spring Boot datasource is connected to `SIH26135_TEST`, then commits the run's fixtures in one transaction. MySQL CLI fixture writes and the backend connection pool each check the catalog. Startup rejects a conflicting catalog setting and a pre-existing listener. Teardown stops both owned apps and verifies removal of the run's records. The runner generates fixture credentials and keeps them in its temporary directory along with upload files and browser downloads.

When the local site already occupies the normal ports, set `$env:SIH_E2E_PORT_OFFSET = '10000'` in the test terminal. The runner starts its own Spring Boot and Vite servers on ports 18080 and 15173. Use `npm run test:e2e -- --grep 'admin console'` for the Admin Console workflows.

Before running the venue booking tests, apply the additive venue booking schema from the repository root with `node database/install-test-venue-bookings.mjs`. The installer checks that the MySQL connection is using `SIH26135_TEST`. For a fresh catalogue, first install the existing schema and onboarding role seeds.

The venue booking browser tests cover temporary proposals, shared centre registration and closures, district and state review, capacity and tenant checks, concurrent approvals, postponement, cancellation, and completion. They also verify that direct centre and batch creation routes return HTTP 405 and that enrolment works after booking approval. The provider submits actual session times in India Standard Time. Each shared centre is a single reservable resource. Reservations use half-open time intervals, so one session can begin when the previous session ends. Cancellation and completion release future sessions while keeping earlier records. An approved postponement retains the same batch ID.

After an intentional API documentation change, use `npm run test:e2e -- --capture-contract` to capture JSON and YAML from a fresh, guarded Spring Boot instance. Then run `npm run generate:types`, review the contract diff, and rerun validation. The normal test command compares snapshots without rewriting them.

## Test coverage

| Workflow | Live browser assertions |
| --- | --- |
| Government MIS | Admin login, district KPI render, backend withholding for a small cohort, geographic panel |
| Provider | Batch creation, duplicate conflict, candidate lookup and enrollment, persisted enrollment |
| Assessor | Assigned roster, marks validation, finalize and issue certificate, downloaded PDF signature |
| Employer | Pending requests, multipart salary-proof upload, verified and completed state, evidence download |
| Trainee | DPDP follow-up consent, required survey answer validation, submission receipt and duplicate conflict |
| Authorization | Restricted government route and API, cross-provider batch denial |
| Sessions | Expired access rejected, two protected reads share one refresh, both use the new token |
| Intake | Multipart CSV upload, row processing, quarantine, downloaded error CSV |
| Failure recovery | Visible KPI error state, retried request returns a real successful response |
| Production bundle | Separate portal chunks, no public production source maps, cold sign-in measurements |

JSON assertions check the actual successful status, media type, response envelope, OpenAPI response schema, and workflow-critical fields. The browser fixture fails the run for unexpected console errors, runtime exceptions, resource failures, and HTTP errors. Intentional API denials and conflicts are checked with Playwright's HTTP client. Unit tests cover auth state, route guards, interceptor behavior, form validation, and rendering and recovery through the error boundary.

## Verified run

All 10 Playwright checks and 315 Vitest tests passed. TypeScript and both OpenAPI snapshots passed validation against the running backend. A separate invocation with `DB_NAME=SIH26135` was rejected before startup. Run `p314_d7ca14631e` stopped both apps and removed its fixtures.

The local-demo production build uses lazy portal routes and a separate React runtime chunk. Vite reported a 377.32 kB main entry (118.81 kB gzip) and a 218.75 kB React chunk (68.27 kB gzip). Chrome Headless Shell 159 on Windows with a Ryzen 7 7435HS measured five fresh, unthrottled browser contexts at 1360 × 900, with service workers blocked.

| Cold `/login` measurement | Minimum | Median | Maximum |
| --- | --- | --- | --- |
| First contentful paint | 124 ms | 128 ms | 372 ms |
| Page load event | 55 ms | 59 ms | 241 ms |
| JavaScript transferred | 223,349 B | 223,349 B | 223,349 B |

Phase 31 covers system-wide regression. Phase 32 covers final local-demo readiness.
