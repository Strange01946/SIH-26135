# SIH-26135: Skill-Training to Employment Outcome Tracking Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-2%2C012%20Passed-success.svg)](#empirical-verification--testing-baseline)
[![Hibernate](https://img.shields.io/badge/Hibernate%20DDL-validate-purple.svg)](#database-schema--isolation-governance)
[![Status](https://img.shields.io/badge/Phase%2028-Approved%20%26%20Frozen-blueviolet.svg)](#project-status-overview)

National skill-training to employment outcome tracking platform developed for government skill development programmes (Smart India Hackathon problem statement **SIH-26135**).

The platform provides longitudinal tracking of vocational trainees from enrollment and training through certification, placement, wage employment attestation, longitudinal retention, survey engagement, and data quality governance.

---

## Project Status Overview

| Metric / Dimension | Current Verified State | Status |
| :--- | :--- | :---: |
| **Active Milestone** | **Phase 28 Complete (Phases 28.1–28.10 Approved & Frozen)** | **FROZEN** |
| **Upcoming Milestone** | **Phase 29 (End-to-End Integration, External Comms, QA, Docs & Cloud Deployment)** | **READY** |
| **Head Commit** | `9d1f20b` (`main` branch) | **CLEAN** |
| **Automated Tests** | **2,012 passed, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESS`)** | **PASS** |
| **Spring Data JPA Repositories** | Exactly **137** interfaces extending `JpaRepository` | **INVARIANT PRESERVED** |
| **`@Query` Annotations in `src/main`** | Exactly **2** annotations (`UserRepository:22`, `TraineeUnemploymentEventRepository:19`) | **INVARIANT PRESERVED** |
| **Production REST Controllers** | Exactly **64** `@RestController` classes | **INVARIANT PRESERVED** |
| **Audited Endpoint Mappings** | Exactly **668** unique HTTP endpoints | **100% ENVELOPE UNIFORMITY** |
| **Database Schema** | 144 BASE TABLES, 21 VIEWS (`spring.jpa.hibernate.ddl-auto=validate`) | **FROZEN (0 DDL)** |
| **Test Database Isolation** | Dedicated `SIH26135_TEST` catalog with fail-closed runtime guard | **ISOLATED** |

---

## Repository Layout

```text
E:\project
├── backend/                  # Enterprise Spring Boot 3.4.10 backend application
│   ├── src/main/java/        # 64 Controllers, 65+ Services, 137 JPA Repositories, Entities, DTOs
│   ├── src/main/resources/   # Application configuration (ddl-auto=validate, JWT, CORS, bounds)
│   ├── src/test/java/        # 2,012 Unit, Integration, Workflow, Security & Regression tests
│   ├── src/test/resources/   # Test datasource configuration (SIH26135_TEST catalog)
│   └── pom.xml               # Maven project descriptor (Spring Boot 3.4.10, Java 17)
├── database/                 # MySQL 8.0 DDL schemas, views & test provisioning (Phases 00–19)
│   ├── phase-00 to 19.sql    # 144 Base Tables, 21 Analytical Views, Stored Procedures
│   └── provision-test-db.ps1 # Dedicated SIH26135_TEST catalog provisioning script
├── docs/                     # Architectural specifications, security designs, and phase reports
│   ├── phases/               # Phase freeze reports (Phase 25, Phase 26, Phase 27, Phase 28)
│   └── *.md                  # Architecture, JWT, CORS, RBAC, and cross-domain documentation
├── frontend/                 # Client application workspace (planned for post-backend delivery)
├── .github/                  # CI/CD and modernization automation
├── phase-28-implementation-review-freeze-report.md # Phase 28.10 root capstone freeze report
└── README.md                 # Project technical overview and roadmap
```

---

## Implemented Platform Capabilities (Phases 1–28)

### 1. Core Domain & Identity Governance (Phases 1–24)
- **Multi-Tenant Hierarchy:** Geographic scoping from National to State, District, Training Center, and Training Provider levels.
- **Trainee Lifecycle Model:** Comprehensive domain entities covering Trainees, Courses, Batches, Attendance Records, Assessments, Certifications, Employers, Job Postings, Placements, and Employment Records.
- **Security & RBAC:** Granular permission system (`WorkflowSecurityScopeValidator`), JWT access/refresh token rotation, account lockout defense (max 5 failed attempts, 15-minute lockout), and strict CORS configuration.

### 2. Business Workflows & State Machines (Phase 25)
Six fully transactional, audited business workflow state machines:
1. **Placement to Employment Transition Workflow:** Manages placement offer verification, wage tracking, and initial employment attestation.
2. **Training Outcome Workflow:** Orchestrates batch enrollment completion, assessment result calculation, and certification issuance.
3. **Employment Verification Workflow:** Multi-channel verification (employer portal, document upload, telephonic attestation) with evidence auditing.
4. **Follow-up Engagement Workflow:** Automated scheduling and tracking of 3, 6, 12, 24, and 36-month post-training survey campaigns.
5. **Skill Gap Remediation Workflow:** Diagnoses training deficiencies, links assessment observations, and generates remedial training plans.
6. **Employment Lifecycle Workflow:** Tracks wage progression, promotion events, voluntary/involuntary employment exits, and unemployment duration.

### 3. Controller Hardening & Uniform Contract Envelopes (Phase 26)
- **100% Envelope Uniformity:** All 668 endpoints return `ApiResponse<T>` or `StreamingResponseBody`. Zero bare entities, un-enveloped collections, or raw Spring Data `Page` objects.
- **Pagination & Sorting Contracts:** Standardized 9-field `PagedResponse<T>` metadata with strict boundary enforcement (`@Min(0)` for page, `@Min(1) @Max(100)` for size) and SQL injection immune sort allowlists.
- **Correlation Tracing:** Distributed request correlation via `X-Correlation-Id` and `X-Request-Id` headers with automated MDC context cleanup.

### 4. Analytical Dashboards, Caching & Statistical Disclosure Control (Phase 27)
- **22 Analytical Dashboards (Q01–Q22):** Executive overview, geographic drilldown, institutional performance, economic uplift, wage growth, attrition patterns, skill gaps, and data integrity.
- **Multi-Dimensional Slicing:** Dynamic filtering by state, district, sector, course, scheme, gender, social category, and fiscal year.
- **High-Performance Caffeine Caching:** Tiered cache TTLs (15m, 1h, 6h, 24h) with automated workflow event-driven cache invalidation.
- **$k=5$ Statistical Disclosure Control (SDC):** Cell-level suppression (`-1`), label masking (`[SUPPRESSED]`), and complementary row suppression preventing demographic reconstruction in sparse cohorts ($k < 5$).
- **Government MIS Streaming Exports:** Memory-bounded CSV and JSON streaming for statutory DSDP, QPR, and Provider Scorecard reports with sliding-window rate limiting.

### 5. Operational Governance, Data Quality & Bulk Ingestion (Phase 28)
- **Automated AOP Audit Trails:** Aspect-oriented `@AuditTrail` interception across workflow mutations, capturing caller identity, correlation ID, and entity state asynchronously via `AuditTrailEventListener`.
- **Audit Immutability & Timeline:** Physical database immutability enforced by HTTP 405 Method Not Allowed on `PUT`, `PATCH`, `DELETE` against audit logs, with bounded chronological timeline reconstruction.
- **Data Quality Rule Catalog & Anomaly Detection:** Automated scanning engine (`DataQualityEngineService`) scanning domain entities for temporal incoherence, missing data, and invalid wage records.
- **Data Quality Issue State Machine:** Formal lifecycle (`OPEN` $\rightarrow$ `ASSIGNED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `RESOLVED` / `DISMISSED`) with HTTP 409 Conflict on invalid transitions.
- **High-Throughput Bulk Ingestion:** Chunked transaction pipeline (100 rows/chunk) supporting multipart CSV, JSON, and NDJSON streaming uploads, row-level quarantine (`import_records`), sanitized error contexts, and crash recovery (`/recover`).
- **Operational Diagnostics & DQI Scorecards:** Real-time diagnostics API (`/api/v1/diagnostics/health`), structured system event logging, and mathematical Data Cleanliness Index (DQI) calculation.
- **4-Tier Operational Tenancy:** Strict isolation between same-state providers, interstate boundaries, district officer jurisdictions, and nationwide oversight.

---

## Database Schema & Isolation Governance

The SIH-26135 platform operates under strict schema freeze and isolation guarantees:

1. **Schema Source of Truth:**
   - 144 BASE TABLES and 21 VIEWS defined in MySQL 8.0.
   - Zero Flyway, Liquibase, or runtime DDL scripts.
   - `spring.jpa.hibernate.ddl-auto=validate` strictly enforced in both production (`src/main/resources`) and test (`src/test/resources`) configurations.
2. **Dedicated Test Database Isolation:**
   - Production Database: `SIH26135` (strictly protected; connecting to or mutating production database is prohibited during testing).
   - Test Database: `SIH26135_TEST` (dedicated isolated catalog).
   - Programmatic Fail-Closed Guard: `MysqlConnectionTest.assertDedicatedTestCatalog()` throws an `IllegalStateException` before any test execution if the connected catalog targets `SIH26135` or any catalog other than `SIH26135_TEST`.

---

## Backend Configuration & Getting Started

After creating the schema, install the [India geography reference directory](database/reference/README.md) in the catalog that the backend will use. Registration, provisioning, employer vacancies and MIS read this directory from MySQL.

### Prerequisites
- **Java:** JDK 17 or higher
- **Build Tool:** Apache Maven 3.9+
- **Database:** MySQL Server 8.0+

### Environment Variables

Configure the following environment variables (do not commit secrets):

| Variable | Description | Required | Default |
| :--- | :--- | :---: | :--- |
| `DB_USERNAME` | Production MySQL database username | Yes (Prod) | — |
| `DB_PASSWORD` | Production MySQL database password | Yes (Prod) | — |
| `DB_HOST` | MySQL server hostname | No | `localhost` |
| `DB_PORT` | MySQL server port | No | `3306` |
| `DB_NAME` | Production database catalog name | No | `SIH26135` |
| `SIH_TEST_DB_USERNAME` | Test database username | Yes (Test) | `${DB_USERNAME}` |
| `SIH_TEST_DB_PASSWORD` | Test database password | Yes (Test) | `${DB_PASSWORD}` |
| `SIH_TEST_DB_HOST` | Test database server hostname | No | `localhost` |
| `SIH_TEST_DB_PORT` | Test database server port | No | `3306` |
| `JWT_SECRET` | 256-bit secret key for HMAC-SHA256 JWT signing | Yes (Prod) | (Must be injected) |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed CORS origins | No | (Empty: disallows all) |
| `IMPORT_PIPELINE_CHUNK_SIZE` | Chunk transaction commit size for bulk ingestion | No | `100` |
| `IMPORT_MULTIPART_MAX_FILE_SIZE` | Maximum upload size for bulk ingestion files | No | `25MB` |

### Build & Test Commands

Run `node database/install-test-venue-bookings.mjs` from the repository root before starting the local demo backend. The installer uses the test database credentials and applies the schema to `SIH26135_TEST`. Other deployments can apply [the venue booking schema](database/phase-21-training-venue-bookings.sql), [the two-type centre constraint](database/phase-22-remove-dedicated-centers.sql), [the training requirements schema](database/phase-23-training-requirements.sql), [the skills-first training migration](database/phase-24-skills-first-training.sql), and [the government training centre migration](database/phase-25-government-training-centres.sql) in order after the existing schema and role seeds. Resolve any legacy dedicated rows before applying the second migration.

Government reviewers publish training needs and select an active shared centre in **Training Requirements**. State Administrators cover their assigned state or union territory and District Officers cover their assigned district. Providers apply through **Training Opportunities** with proposed daily sessions. Selecting a provider reserves those sessions, creates the batch, and records the application decisions in one transaction. Reviewers can assign a centre to older requirements that have no centre or booking.

Providers can submit their own training plans through **Venue Bookings** by describing the skills to teach. Venue approval creates the batch and supplies the internal references used by enrollment, assessments, and outcome reporting.

Execute the following commands from the `backend/` directory:

```bash
cd backend

# Execute the complete automated regression suite against SIH26135_TEST (2,012 tests)
mvn clean test

# Run a focused test suite (e.g., Phase 28 regression)
mvn test -Dtest=AuditDataQualityAndOperationsRegressionTest

# Package production JAR
mvn clean package -DskipTests

# Run the Spring Boot application locally
mvn spring-boot:run
```

---

## Future Roadmap: Phase 29 & Beyond

Phase 28 established the complete operational and architectural baseline. **Phase 29** is the culminating production readiness milestone:

### Phase 29 Architecture & Subphases Overview

```text
Phase 29: End-to-End Backend Integration, External Communication, Comprehensive Testing & QA,
          API Documentation & Developer Experience, Production Readiness, Deployment & Infrastructure
```

- **Pillar 1: End-to-End Backend Integration (Phases 29.1–29.2)**
  - *29.1 Integration Baseline & Contract Unification:* Full subsystem dependency graph audit, domain event envelopes (`TraineeLifecycleEvent`, etc.), and unified transaction boundaries.
  - *29.2 Cross-Workflow Orchestration Choreography:* End-to-end candidate lifecycle orchestrator linking enrollment, attendance, assessment, placement, verification, retention, and skill gap remediation.
- **Pillar 2: External Communication Engine (Phases 29.3–29.4)**
  - *29.3 Multi-Channel Provider & Consent Engine:* Pluggable email (SMTP), SMS (DLT-compliant), and WhatsApp notification providers, with mandatory pre-dispatch consent verification against `trainee_consents`.
  - *29.4 Notification Templating & Webhook DLR:* Dynamic templating, anti-spam sliding-window rate limiting, async dispatch queue with retry, MySQL `communication_logs` tracking, and inbound delivery receipt (DLR) webhook API.
- **Pillar 3: Comprehensive Testing & Quality Assurance (Phases 29.5–29.8)**
  - *29.5 End-to-End Persona Journey Testing:* Complete multi-role real-world scenario testing (Candidate, Training Provider, Employer, Government Administrator) with zero mocked DB state.
  - *29.6 Concurrency & Load Stress Testing:* Multi-threaded enrollment contention, bulk import saturation, cache invalidation bursts, and HikariCP connection pool stress resilience.
  - *29.7 Chaos Engineering & Fault Injection:* Database disconnection recovery, external gateway outages, corrupted stream handling, and anti-storm log throttling.
  - *29.8 OWASP Top 10 & Global PII Audit:* BOLA/IDOR penetration, authentication fuzzing, parameter injection testing, and automated reflection crawler verifying zero PII leakage across all 668 endpoints.
- **Pillar 4: API Documentation & Developer Experience (Phases 29.9–29.10)**
  - *29.9 Enterprise OpenAPI 3.0 / Swagger:* 5 grouped OpenAPI specifications (`GroupedOpenApi`), comprehensive operation and schema annotations, toggleable Swagger UI, and static `openapi.json` export.
  - *29.10 Developer Onboarding & Postman Tooling:* Interactive Postman/Insomnia collections with automated auth scripts, developer onboarding guide, and standardized error codes glossary.
- **Pillar 5: Production Readiness, Deployment & Infrastructure (Phases 29.11–29.14)**
  - *29.11 Observability, Health & Prometheus:* Spring Boot Actuator liveness/readiness probes, 4 custom health indicators, Micrometer Prometheus business meters, and Logback ECS JSON logging.
  - *29.12 Multi-Stage Docker & Compose Orchestration:* Unprivileged multi-stage `Dockerfile` (<250MB, Temurin JRE 17, non-root user), `.dockerignore`, and local multi-container `docker-compose.yml` (App, MySQL, MailHog, Prometheus, Grafana).
  - *29.13 Kubernetes Manifests & IaC Hardening:* Cloud-native manifests (`Deployment`, `Service`, `Ingress`, `HPA`, `PDB`, `ConfigMap`, `Secret`) and production profile tuning (`application-prod.properties`).
  - *29.14 CI/CD Pipelines & Operational Runbooks:* GitHub Actions workflows (`ci.yml`, `cd.yml`) and enterprise runbooks for zero-downtime deployment, database backup/PITR, incident triage, and disaster recovery.
- **Pillar 6: System Verification & Release Freeze (Phases 29.15–29.16)**
  - *29.15 System-Wide Verification Battery:* Full regression execution targeting **2,150+ passing tests** with 0 failures, 0 errors, 0 skipped.
  - *29.16 Final Deliverables Audit & Release Freeze:* Read-only capstone audit, `phase-29-implementation-review-freeze-report.md` authoring, byte-for-byte SHA-256 report synchronization, and production release freeze.

### Post-Backend Client Development (`frontend/`)
Following backend production freeze in Phase 29, the `frontend/` directory will house the government and public-facing portal applications:
- **Trainee Portal:** Profile management, training progress, certificate download, consent preferences, and survey responses.
- **Training Provider Portal:** Batch scheduling, attendance logging, bulk trainee CSV/JSON uploads, data quality remediation, and center performance scorecards.
- **Employer Portal:** Job vacancy postings, candidate verification, salary attestations, and retention tracking.
- **Government MIS & Administrator Portal:** National and state dashboards, district drilldowns, DQI cleanliness monitoring, streaming MIS exports, and immutable audit timeline exploration.

---

## Empirical Verification & Testing Baseline

Every release milestone requires clean automated regression verification. The active baseline established in Phase 28:

- **Total Test Cases Executed:** **2,012**
- **Failures:** **0**
- **Errors:** **0**
- **Skipped:** **0**
- **Build Status:** **BUILD SUCCESS**
- **Key Test Breakdown:**
  - `AuditDataQualityAndOperationsRegressionTest`: 33 tests across 10 operational sections (A–J)
  - Foundational Operational Suites: 40 tests (`AuditTrailAspectTest`, `AuditTrailEventListenerTest`, `WorkflowTransactionRollbackIntegrationTest`, `ImportBatchProcessingIntegrationTest`)
  - Analytical Dashboards & SDC Suites: 430+ tests (`AnalyticsAndDashboardComprehensiveRegressionTest`, `AnalyticsExportTest`, etc.)
  - Workflow State Machine Suites: 280+ tests across Phase 25 workflows
  - Validation & Security Contract Suites: 1,220+ tests verifying controller envelopes, DTO boundaries, and tenant isolation
- **Git Hygiene:** Clean working tree with zero untracked scratch files (`git diff --check` exits with 0).
