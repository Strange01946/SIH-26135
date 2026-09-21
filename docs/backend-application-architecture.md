# Backend Application & Service Layer Architecture — SIH 26135

**Project:** SIH 26135 — Longitudinal Tracking & Impact Analysis  
**Phase:** 22.1 — Application / Service Layer Architecture (Foundation Phase)  
**Base Package:** `in.gov.sih.sih26135`  
**Database Schema:** `SIH26135` (MySQL 8.0, 144 base tables, 21 analytical views)  
**Authoritative Baselines:**
- Database DDL: `database/phase-00` through `phase-19` SQL
- Backend Architecture Foundation: `docs/backend-architecture.md` (Phase 20)
- Cross-Domain Integration: `docs/backend-cross-domain-integration.md` (Phase 21.17)
- Final Backend Domain Verification: `docs/backend-domain-verification.md` (Phase 21.18)

---

## 1. Purpose

This document defines the architectural governance, layer boundaries, contracts, and engineering standards for the Application and Service Layer (Phase 22) of SIH 26135.

It serves as the definitive reference for implementing all upcoming domain services (Phases 22.2 through 22.18). It establishes strict boundaries between:
- **HTTP Presentation Layer** (Controllers)
- **Application & Business Orchestration Layer** (Services)
- **Data Persistence Layer** (Repositories)
- **Database Schema Models** (Entities)
- **External Communication Contracts** (DTOs)

The primary objectives are:
1. **Decoupling:** Complete isolation between persistence models and external API contracts.
2. **Defensive Data Privacy:** Absolute containment of sensitive information (passwords, salts, tokens, Aadhaar numbers).
3. **Transaction Safety:** Deterministic atomicity and prevention of partial-commit corruption in complex cross-domain workflows.
4. **Runtime Stability:** Zero `LazyInitializationException` errors under disabled Open-Session-In-View (`spring.jpa.open-in-view=false`).
5. **Architectural Purity:** Prevention of circular dependencies, repository leakage, and premature external library clutter.

---

## 2. Current Backend Architecture Baseline

The backend implementation through Phase 21.18 consists of **289 production Java source files** and **2 test source files**, fully verified against the frozen MySQL 8.0 schema:

### 2.1 Source Inventory
- **Base Domain Entities (121 classes):** Located in `in.gov.sih.sih26135.entity`, mapped with 100% schema fidelity to the 144 operational tables from Phases 01–15.
- **Base Domain Repositories (115 interfaces):** Located in `in.gov.sih.sih26135.repository`, extending `JpaRepository` with pure derived queries and zero destructive operations.
- **Analytics View Entities & IDs (24 classes):** Located in `in.gov.sih.sih26135.entity.analytics`, representing the 21 analytical views (`vw_...`) from Phase 16 with `@Immutable` annotations, zero `@GeneratedValue`, and composite `@IdClass`es.
- **Analytics View Repositories (21 interfaces):** Located in `in.gov.sih.sih26135.repository.analytics`, providing pure read-only query capabilities.
- **Infrastructure & Foundation Classes (8 classes):**
  - Application entry point: `Sih26135Application`
  - Success envelope: `ApiResponse<T>` (in `in.gov.sih.sih26135.response`)
  - Exception hierarchy: `BaseException`, `ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `ErrorResponse`, `GlobalExceptionHandler` (in `in.gov.sih.sih26135.exception`)

### 2.2 Core Persistence Conventions Active
- **`spring.jpa.hibernate.ddl-auto=validate`:** The database schema is permanently frozen and authoritative. The application never performs DDL modifications (`CREATE`, `ALTER`, `DROP`, `TRUNCATE`).
- **`spring.jpa.open-in-view=false`:** Open-Session-In-View is permanently disabled. The persistence session terminates strictly when execution leaves the service transactional boundary. Any access to uninitialized lazy proxies outside this boundary causes an immediate runtime `LazyInitializationException`.
- **Relationship Discipline:** 100% of `@ManyToOne` and `@OneToOne` associations enforce `fetch = FetchType.LAZY`. Zero `cascade`, zero `orphanRemoval`, and zero `@OneToMany`/`@ManyToMany` collections exist in entity classes. All foreign keys navigate unidirectionally from child to parent.

---

## 3. Target Application Architecture

The SIH 26135 backend implements a strict, multi-tier layered architecture where each layer communicates solely with its immediate neighbors through explicitly defined interfaces and contracts.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT (Browser / API)                           │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ HTTP JSON Request
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CONTROLLER PRESENTATION LAYER                      │
│  - Receives HTTP requests, path variables, and query parameters             │
│  - Triggers syntactic validation (@Valid RequestDTO)                        │
│  - Extracts caller security context / actor identity                        │
│  - Delegates immediately to Service interfaces                              │
│  - Wraps Service ResponseDTO in ApiResponse<T> with HTTP status             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ RequestDTO / Domain Parameters
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          APPLICATION & SERVICE LAYER                        │
│  - Manages transaction boundaries (@Transactional)                          │
│  - Enforces domain rules, invariants, and state transitions                 │
│  - Performs semantic validation and cross-domain integrity checks           │
│  - Coordinates multiple Repositories within an atomic unit of work          │
│  - Converts Entities to ResponseDTOs via Mappers within open session        │
│  - Throws BaseException subclasses on domain rule violations                │
│  - Prepares audit log records for cross-cutting logging                     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Managed Entities / Derived Queries
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            PERSISTENCE REPOSITORY LAYER                     │
│  - Executes Spring Data JPA derived queries against database                │
│  - Manages entity lifecycle (find, save, count, exists)                     │
│  - Zero business logic, zero @Modifying queries, zero native SQL writes     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ SQL (PreparedStatements)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MYSQL 8.0 DATABASE (SIH26135)                       │
│  - 144 Base Tables + 21 Analytical Views                                    │
│  - Final backstop for data integrity (Foreign Keys, Unique, Check)          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Inbound Data Flow
```
HTTP JSON Request ──► Controller ──[@Valid]──► RequestDTO ──► Service ──► Entity/Repository
```

### Outbound Data Flow
```
Database ──► Repository ──► Entity ──► Service ──[Mapper]──► ResponseDTO ──► Controller ──[ApiResponse<T>]──► HTTP JSON Response
```

**Foundational Rule:** JPA Entities must **NEVER** be returned from the Service layer to the Controller layer, and must **NEVER** be serialized directly as API response contracts.

---

## 4. Package Architecture & Structure

All backend application code resides under the root package `in.gov.sih.sih26135`. The package structure organizes components by functional layer:

```
in.gov.sih.sih26135
│
├── Sih26135Application.java        # Spring Boot application entry point
│
├── controller                       # REST API controllers (/api/<resource>)
│   └── (implemented incrementally per domain in Phase 22.2+)
│
├── service                          # Business logic service interfaces
│   ├── (service interfaces per domain in Phase 22.2+)
│   └── impl                         # Service implementations (@Service, @Transactional)
│
├── dto                              # API data transfer objects
│   ├── request                      # Incoming client request contracts
│   └── response                     # Outgoing server response contracts
│
├── mapper                           # Explicit Entity <-> DTO converters
│   └── (explicit Java mapper components/classes per domain)
│
├── repository                       # Spring Data JPA repositories (Phase 21.2–21.15)
│   └── analytics                    # Read-only repositories for analytical views (Phase 21.16)
│
├── entity                           # JPA persistence entities (Phase 21.2–21.15)
│   └── analytics                    # JPA immutable analytical view entities (Phase 21.16)
│
├── exception                        # Centralized exception hierarchy and handler (Phase 20)
│   ├── BaseException.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   ├── ConflictException.java
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java
│
├── response                         # Standard success response envelope (Phase 20)
│   └── ApiResponse.java
│
└── config                           # Spring configuration classes (future security, web)
```

### Package Implementation Governance
- **No Empty Marker Packages:** Directory packages will only be created on disk when concrete classes are implemented within that specific domain phase (Phases 22.2+).
- **Sub-package Discipline:** Domain DTOs and Mappers will be grouped cleanly by domain naming conventions (e.g., `TraineeRequest`, `TraineeResponse`, `TraineeMapper`) within their respective layer packages rather than fragmented across deep arbitrary hierarchies.

---

## 5. Controller Layer Responsibilities & Contracts

Controllers act strictly as HTTP presentation adapters. They bridge the HTTP protocol with the Java application service layer.

### Responsibilities:
1. **Endpoint Exposure:** Map HTTP routes following standard RESTful patterns (`/api/<resources>`).
2. **Parameter Binding:** Bind path variables (`@PathVariable`), query parameters (`@RequestParam`), and request bodies (`@RequestBody`).
3. **Input Validation Triggering:** Decorate request body parameters with `@Valid` to trigger syntactic validation before invoking the service layer.
4. **Security Context Propagation:** Extract authenticated caller information (user ID, assigned roles, client IP) from the security context and pass it as explicit method arguments to service methods.
5. **Immediate Delegation:** Forward requests to appropriate Service interfaces with zero processing or manipulation of business data.
6. **Response Envelopment:** Wrap Service output DTOs in `ApiResponse<T>` and return `ResponseEntity<ApiResponse<T>>` with appropriate HTTP status codes:
   - `200 OK`: Successful retrieval or synchronous update.
   - `201 CREATED`: Successful resource creation.
   - `204 NO_CONTENT`: Successful deletion where no response body is returned.

### Strict Prohibitions:
- **NO Business Logic:** Controllers must never contain conditional branching representing domain rules.
- **NO Repository Injection:** Controllers must never inject or access repositories directly.
- **NO Entity Exposure:** Controllers must never accept or return JPA entities.
- **NO Transaction Management:** Controllers must never be annotated with `@Transactional`.
- **NO Direct Database Interaction:** Controllers must never execute queries or manipulate database connections.

---

## 6. Service Layer Responsibilities & Contracts

The Service Layer contains all business logic, workflow orchestration, transaction boundaries, and domain rules.

### Responsibilities:
1. **Business Orchestration:** Coordinate domain workflows across multiple entities and repositories.
2. **Transaction Ownership:** Act as the sole owner of database transaction boundaries using `@Transactional`.
3. **Semantic / Business Validation:** Verify business invariants, such as:
   - Entity existence and referential validity.
   - Domain state transitions (e.g., an enrollment cannot be marked completed if attendance is below threshold).
   - Uniqueness constraints across business fields (e.g., duplicate email, duplicate batch code).
4. **Cross-Domain Integrity:** Enforce integration invariants between related domains (e.g., linking a placement record only to an enrolled trainee in the same program).
5. **Entity-to-DTO Translation:** Invoke explicit mappers to convert managed JPA entities into Response DTOs *while still within the active transactional session*.
6. **Exception Dispatch:** Throw specific subclasses of `BaseException` (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`) when validation or business constraints fail.
7. **Audit & Event Preparation:** Assemble audit event payloads and forward them to the audit logging service.

### Strict Prohibitions:
- **NO HTTP Awareness:** Services must never accept or return HTTP-specific classes (`HttpServletRequest`, `HttpServletResponse`, `ResponseEntity`, `HttpStatus`, `HttpHeaders`).
- **NO Entity Leakage:** Services must never return unmapped JPA entities to controllers.
- **NO Raw SQL:** Services must never execute raw SQL statements or native database queries.
- **NO Direct JDBC Manipulation:** Services must never interact directly with JDBC connections, statements, or result sets.

---

## 7. Repository Layer Responsibilities & Contracts

The Repository Layer provides the data access abstraction over the MySQL 8.0 database.

### Responsibilities:
1. **Interface Contract:** Extend `JpaRepository<Entity, IdType>` and optionally `JpaSpecificationExecutor<Entity>` for dynamic filtering.
2. **Declarative Persistence:** Expose Spring Data derived query methods (`findBy...`, `existsBy...`, `countBy...`).
3. **Type-Safe Access:** Return managed entities, `Optional<Entity>`, or scalar numeric counts.

### Strict Prohibitions:
- **NO Business Logic:** Repositories must never perform calculations, validation, or business decision making.
- **NO Destructive Custom Queries:** Repositories must never declare `@Modifying` queries or custom deletion methods (`deleteBy...`). All deletion operations must be carefully orchestrated through the service layer.
- **NO Native SQL Writes:** Repositories must never execute native SQL `INSERT`, `UPDATE`, or `DELETE` statements.
- **NO Direct Controller / DTO Coupling:** Repositories must never be referenced by controllers or accept/return DTOs.

---

## 8. DTO Architecture & Data Protection

Data Transfer Objects (DTOs) represent the public contract of the SIH 26135 REST API.

### 8.1 DTO Classification
- **Request DTOs (`dto.request`):**
  - Purpose: Represent incoming client payloads.
  - Structure: Flat, strongly-typed fields matching API submission needs.
  - Annotations: Jakarta Validation constraints (`@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@Min`, `@Max`).
  - Immutability: Standard POJOs or Java 17 `record`s with complete constructor/getter access.
- **Response DTOs (`dto.response`):**
  - Purpose: Represent outgoing server payloads.
  - Structure: Clean, normalized data shapes formatted for client consumption.
  - Safe Composition: May contain nested child Response DTOs where hierarchical representations are required by the UI/client.

### 8.2 Defensive Data Protection (Anti-Leakage)
Under no circumstances may Response DTOs expose:
1. **Password Hashes & Salts:** Fields such as `password_hash`, `salt`, or password reset tokens.
2. **Security & Session Tokens:** Refresh tokens, internal authentication nonces, or private API keys.
3. **Sensitive Citizen Identifiers:** Unmasked Aadhaar numbers, biometric indicators, or private banking details.
4. **Internal Database Machinery:** Hibernate proxy metadata, version stamps, or internal sequencing keys where irrelevant to consumers.

### 8.3 Lazy-Loading Defense (`open-in-view=false`)
Because `spring.jpa.open-in-view=false` is enforced:
- If a controller attempts to serialize an entity directly with Jackson, any lazy relationship that was not fetched within the service transaction will trigger:
  ```
  org.hibernate.LazyInitializationException: could not initialize proxy - no Session
  ```
- DTOs prevent this completely: Mappers read all required fields while the session is open inside the service layer, copying values into plain DTO fields. The controller receives an independent, fully-materialized DTO object that can be safely serialized to JSON anywhere.

---

## 9. Entity Layer Responsibilities & Persistence Encapsulation

JPA Entities in `in.gov.sih.sih26135.entity` represent the internal relational database schema of `SIH26135`.

### Responsibilities:
1. **Schema Mirroring:** Mirror the 144 base tables and 21 views with 100% fidelity.
2. **Explicit Metadata:** Use explicit `@Table`, `@Column`, `@JoinColumn`, and `@IdClass` annotations matching MySQL DDL.
3. **Lazy Associations:** Maintain unidirectional `FetchType.LAZY` associations.
4. **Encapsulation:** Entities are strictly internal to the persistence and service layers. They must never cross the service boundary outwards.

---

## 10. Explicit Mapping Strategy

### 10.1 Selected Strategy: Explicit Java Mappers
Entity-to-DTO and DTO-to-Entity translation will be handled exclusively using **Explicit Java Mappers** implemented as dedicated Spring `@Component` classes or static helper classes.

### 10.2 Architectural Comparison & Justification
| Strategy | Build Complexity | Transparency & Debuggability | Runtime Safety under OSIV=false | Extra Dependencies | Decision |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **Explicit Java Mappers** | **Zero (Standard Java)** | **100% Clear & Step-through Debuggable** | **Absolute (Explicit Field Access)** | **None (0 dependencies)** | **SELECTED** |
| **MapStruct** | Requires APT compiler plugin & generated sources | Hidden generated code, compilation overhead | Moderate (risk of implicit lazy navigation) | `mapstruct`, `mapstruct-processor` | REJECTED |
| **ModelMapper / Orika** | None | Low (black-box runtime reflection, hard to debug) | Dangerous (reflection triggers lazy proxy loading) | `modelmapper` | REJECTED |

### 10.3 Mapper Design Conventions
- **Component Declaration:** Mappers may be annotated with `@Component` for dependency injection into services, or structured as pure static utility classes where stateless.
- **Explicit Field Assignment:** Every field is mapped explicitly:
  ```java
  public UserResponse toResponse(User entity) {
      if (entity == null) {
          return null;
      }
      return new UserResponse(
          entity.getUserId(),
          entity.getUsername(),
          entity.getEmail(),
          entity.getFullName(),
          entity.getMobileNumber(),
          entity.getUserStatus() != null ? entity.getUserStatus().getStatusCode() : null,
          entity.getCreatedAt()
      );
  }
  ```
- **Null-Safety:** All mappers must handle null input arguments gracefully, returning `null` without throwing `NullPointerException`.
- **Reference Resolution:** When mapping from Request DTO to Entity, foreign key references are resolved in the Service layer via the respective repository before assigning to the entity.

---

## 11. Three-Tier Validation Architecture

The application enforces validation across three distinct defensive tiers:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ TIER 1: SYNTACTIC / FORMAT VALIDATION (Controller & Request DTO)            │
│ - Validates data formats, presence, string lengths, ranges, and patterns    │
│ - Enforced by Jakarta Bean Validation (@NotNull, @Size, @Email, @Pattern)   │
│ - Handled before entering business service methods                          │
│ - Failures produce HTTP 400 with VALIDATION_ERROR and field error list      │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Validated RequestDTO
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ TIER 2: SEMANTIC / BUSINESS VALIDATION (Service Layer)                      │
│ - Validates business invariants, state machine rules, and entity existence  │
│ - Enforced in Java service methods                                          │
│ - Checks: duplicate records, valid status transitions, foreign key existence│
│ - Failures throw ResourceNotFoundException (404), BadRequestException (400),│
│   or ConflictException (409)                                                │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Managed Entity Operations
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ TIER 3: RELATIONAL INTEGRITY DEFENSE (MySQL 8.0 Database)                   │
│ - Physical database constraints act as the absolute final backstop          │
│ - Constraints: NOT NULL, UNIQUE KEY, FOREIGN KEY, CHECK, generated columns  │
│ - Prevents data corruption even in unexpected concurrent race conditions    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Dependency Note:
`spring-boot-starter-validation` provides Jakarta Bean Validation support. When request DTO validation annotations are introduced in Phase 22.2+, this dependency will be formally incorporated into `pom.xml`.

---

## 12. Transaction Management Architecture

Database transactions are managed declaratively using Spring's `@Transactional` annotation.

### 12.1 Transaction Rules
1. **Service Layer Ownership:** `@Transactional` belongs exclusively in the Service Layer.
   - Controllers must **never** declare `@Transactional`.
   - Repositories must **never** declare `@Transactional` for business workflows.
2. **Read-Only Operations:** Query and retrieval service methods must declare:
   ```java
   @Transactional(readOnly = true)
   ```
   - *Benefits:* Disables Hibernate session dirty checking, provides significant memory and CPU optimization, and signals read-only intent to the MySQL JDBC driver.
3. **Mutating Operations:** Create, update, and delete service methods must declare:
   ```java
   @Transactional
   ```
   - *Behavior:* Enforces ACID atomicity. Any unchecked exception (`RuntimeException` or `Error`) triggers an automatic database rollback.
4. **Cross-Domain Workflow Boundaries:** When a business operation spans multiple domain entities or repositories (e.g., verifying employment and updating trainee status), a single orchestrating service method provides the outer transaction boundary. All nested repository operations join this single transaction (`Propagation.REQUIRED`), preventing partial updates.

---

## 13. Exception Handling & Error Pipeline

The backend utilizes the centralized exception infrastructure established in Phase 20:

```
Service / Domain Layer
    │
    ├─► throws ResourceNotFoundException ──┐
    ├─► throws BadRequestException       ──┤
    ├─► throws ConflictException         ──┤
    └─► throws BaseException             ──┤
                                           ▼
Controller Layer                 GlobalExceptionHandler (@RestControllerAdvice)
    │                                      │
    ├─► MethodArgumentNotValidException ───┤
    ├─► HttpMessageNotReadableException ───┤
    └─► Unhandled Exception ───────────────┘
                                           │
                                           ▼
                               Produces ErrorResponse
                                           │
                                           ▼
                             HTTP Client (Sanitized JSON)
```

### 13.1 Exception Status Mapping
| Exception Class | HTTP Status | Error Code | Common Use Case |
| :--- | :---: | :--- | :--- |
| `ResourceNotFoundException` | `404 NOT FOUND` | `RESOURCE_NOT_FOUND` | Trainee, User, Course, or Batch ID does not exist |
| `BadRequestException` | `400 BAD REQUEST` | `BAD_REQUEST` | Business rule violated, invalid state transition |
| `ConflictException` | `409 CONFLICT` | `RESOURCE_CONFLICT` | Duplicate email, duplicate registration number, state conflict |
| `MethodArgumentNotValidException` | `400 BAD REQUEST` | `VALIDATION_ERROR` | Request DTO fails syntactic validation (`@NotNull`, etc.) |
| `Exception` (Generic fallback) | `500 INTERNAL_SERVER_ERROR` | `INTERNAL_SERVER_ERROR` | Unexpected runtime error |

### 13.2 Privacy & Security in Error Responses
- **No Stack Traces:** Stack traces are logged on the server at `ERROR` level but are **never** returned to API clients.
- **No SQL Leakage:** Database constraint names, SQL statements, and column names are stripped before returning `ErrorResponse`.
- **No Sensitive Value Interpolation:** `ResourceNotFoundException` formats messages as `"<Resource> not found with <FieldName>"`, deliberately omitting the actual query parameter value to prevent PII or credential reflection.

---

## 14. Standard API Response Architecture (`ApiResponse<T>`)

All REST controllers return responses encapsulated in the standardized `ApiResponse<T>` envelope located in `in.gov.sih.sih26135.response`:

### 14.1 Structure
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-21T11:30:00Z",
  "path": "/api/v1/trainees/101"
}
```

### 14.2 Standard Controller Return Patterns
```java
// 1. Single Resource Retrieval (200 OK)
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<TraineeResponse>> getById(@PathVariable Long id) {
    TraineeResponse response = traineeService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
}

// 2. Resource Creation (201 CREATED)
@PostMapping
public ResponseEntity<ApiResponse<TraineeResponse>> create(@Valid @RequestBody CreateTraineeRequest request) {
    TraineeResponse created = traineeService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Trainee created successfully", created));
}

// 3. Collection Retrieval (200 OK)
@GetMapping
public ResponseEntity<ApiResponse<List<TraineeResponse>>> getAll() {
    List<TraineeResponse> list = traineeService.getAll();
    return ResponseEntity.ok(ApiResponse.ok(list));
}

// 4. Action / Void Operation (200 OK)
@PostMapping("/{id}/deactivate")
public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
    traineeService.deactivate(id);
    return ResponseEntity.ok(ApiResponse.success("Trainee deactivated successfully"));
}
```

---

## 15. Operational vs. Analytics Read/Write Separation

The backend enforces a strict architectural separation between the **Operational Domain** and the **Analytics Read Layer**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           OPERATIONAL DOMAIN (Phases 01–15)                 │
│  - Tables: 144 operational MySQL tables                                     │
│  - Operations: Read / Write / Update transactional CRUD                     │
│  - Entities: in.gov.sih.sih26135.entity.*                                   │
│  - Repositories: in.gov.sih.sih26135.repository.*                           │
│  - Services: Manage business workflows and state transitions                │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        ANALYTICS READ LAYER (Phase 16 & 21.16)               │
│  - Views: 21 pre-computed MySQL analytical views (vw_..._fact, vw_..._sum) │
│  - Operations: PURE READ ONLY                                               │
│  - Entities: in.gov.sih.sih26135.entity.analytics.* (@Immutable)           │
│  - Repositories: in.gov.sih.sih26135.repository.analytics.*                 │
│  - Services: Reporting and dashboard aggregation services only              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Safety Guarantees:
1. **Physical Read-Only Views:** Analytical views in MySQL are defined using aggregation, window functions, and multi-table joins. They are physically non-updatable.
2. **JPA Immutability:** All 21 analytics view entities are marked with `@org.hibernate.annotations.Immutable`. Hibernate will throw an exception if an entity modification is attempted.
3. **Service Isolation:** Operational domain services must never attempt to insert, update, or delete analytics view entities. Analytics query services perform read-only queries exclusively.

---

## 16. Analytics Layer Isolation Strategy

To prevent reporting queries from degrading operational OLTP performance:
- Analytics repositories are segregated in the `in.gov.sih.sih26135.repository.analytics` sub-package.
- Analytics entities are segregated in `in.gov.sih.sih26135.entity.analytics`.
- Future analytics services (e.g., `DashboardQueryService`) will be isolated in `in.gov.sih.sih26135.service.analytics`.
- Analytics operations will execute with `@Transactional(readOnly = true)` to avoid acquiring unnecessary locks or participating in operational transactions.

---

## 17. Security Boundary & Context Propagation

Security architecture follows the standard Spring Security pipeline (to be fully implemented in dedicated security phases):

```
Client Request 
    │
    ▼
Security Filter Chain (JWT Validation, Authentication)
    │  - Validates bearer token / session
    │  - Populates SecurityContextHolder with AuthenticatedPrincipal
    ▼
Controller Layer
    │  - Extracts Actor Identity (e.g. CurrentUser / userId / roles)
    │  - Passes actor context as explicit method arguments to Service
    ▼
Service Layer
    │  - Enforces domain-level authorization (e.g. Is user authorized for center X?)
    │  - Attaches actor user ID to audit records (AuditLog.actor_user_id)
    ▼
Repository Layer
```

### Phase 22.1 Scope Boundary:
- Spring Security filters, JWT parsers, and login controllers are **NOT** implemented in Phase 22.1.
- Service contracts will be designed to accept caller actor user IDs (`Long actorUserId`) where needed for operational audit tracking, ensuring seamless integration once security is activated.

---

## 18. Cross-Domain Service Orchestration Principles

Phase 21.17 audited and documented the 74 cross-domain relationships across SIH 26135 (`docs/backend-cross-domain-integration.md`). Future domain services must orchestrate operations along these six primary workflow chains:

1. **Chain A: Trainee &rarr; Training &rarr; Outcome**
   - Entities: `Trainee` &rarr; `TrainingEnrollment` &rarr; `AttendanceRecord` &rarr; `Assessment` &rarr; `TraineeAssessment` &rarr; `AssessmentResult` &rarr; `Certification`.
   - Rule: Certification creation must verify passing `AssessmentResult` and active `TrainingEnrollment`.
2. **Chain B: Trainee &rarr; Placement &rarr; Employment**
   - Entities: `JobPosting` &rarr; `JobApplication` &rarr; `PlacementRecord` &rarr; `PlacementOffer` &rarr; `EmploymentRecord` &rarr; `SalaryHistory`.
   - Rule: `EmploymentRecord` creation links back to the originating `PlacementRecord` and verifies employer branch identity.
3. **Chain C: Trainee &rarr; Follow-Up &rarr; Verification**
   - Entities: `FollowupCampaign` &rarr; `FollowupTask` &rarr; `SurveyResponse` &rarr; `EmploymentVerificationRequest` &rarr; `EmploymentVerification` &rarr; `EmploymentVerificationAttempt` &rarr; `EmploymentVerificationEvidence`.
   - Rule: Verification requests originate from follow-up survey responses or active employment records.
4. **Chain D: Trainee &rarr; Skill Gap**
   - Entities: `SkillGapAssessment` &rarr; `SkillGap` &rarr; `SkillGapObservation` &rarr; `SkillGapRecommendation`.
   - Rule: Skill gaps evaluate baseline course skills against required job role competencies.
5. **Chain E: Employment &rarr; Exit &rarr; Unemployment**
   - Entities: `EmploymentExitEvent` &rarr; `TraineeUnemploymentEvent`.
   - Rule: Logging an employment exit transitions the trainee into an unemployment tracking spell until succeeding employment is verified.
6. **Chain F: Cross-Cutting Audit & Quality Logging**
   - Entities: `AuditLog`, `DataQualityRule`, `DataQualityIssue`, `DataQualityIssueEvent`, `SystemEventLog`, `ImportBatch`, `ImportRecord`.
   - Rule: High-risk domain mutations (status transitions, verification determinations, exit events) emit asynchronous or synchronous audit log entries without breaking domain decoupling.

---

## 19. Architectural Dependency Rules

To maintain strict modularity, dependencies between architectural layers must adhere to the following rules:

### 19.1 Permitted Dependency Directions
```
Controller ──────► Service
    │                 │
    ▼                 ▼
   DTO ◄─────────── Mapper ──────────► Entity
                      │                   ▲
                      ▼                   │
                  Repository ─────────────┘
```
- `Controller` may depend on: `Service`, `DTO`, `ApiResponse`.
- `Service` may depend on: `Repository`, `Entity`, `DTO`, `Mapper`, `BaseException` subclasses, other domain services (orchestration).
- `Mapper` may depend on: `Entity`, `DTO`.
- `Repository` may depend on: `Entity`, Spring Data JPA.

### 19.2 Strictly Prohibited Dependency Directions
- **`Controller` &rarr; `Repository`:** Direct repository injection into controllers is strictly prohibited.
- **`Repository` &rarr; `Service` / `Controller` / `DTO`:** Repositories must have zero awareness of higher layers.
- **`Entity` &rarr; `Service` / `Controller` / `Repository` / `DTO`:** Entities are plain persistence models with zero dependencies on application services or contracts.
- **`DTO` &rarr; `Repository` / `Service` / `Entity`:** DTOs are passive data carriers and must never depend on persistence or service infrastructure.

---

## 20. Rules Preventing Circular Service Dependencies

As cross-domain workflows expand, mutual service injection represents an architectural anti-pattern (`ServiceA` injects `ServiceB`, while `ServiceB` injects `ServiceA`), causing Spring application context startup failures.

### Prevention Rules:
1. **No Bidirectional Injection:** Direct mutual `@Autowired` or constructor injection between two domain services is strictly forbidden.
2. **Orchestration Services:** When a business workflow requires operations across two peer domains (e.g., Placement and Employment), a dedicated higher-level orchestration service (e.g., `PlacementToEmploymentWorkflowService`) must be created. The orchestration service injects both peer services/repositories, maintaining a strict tree dependency hierarchy:
   ```
   PlacementToEmploymentWorkflowService
             ├──► PlacementService / PlacementRecordRepository
             └──► EmploymentService / EmploymentRecordRepository
   ```
3. **Spring Application Events:** For asynchronous, non-blocking side effects (e.g., generating an audit log or triggering a follow-up task upon course completion), services should publish lightweight Spring application events (`ApplicationEventPublisher`), completely decoupling the publisher from the subscriber.

---

## 21. Rules Preventing Entity Exposure Through APIs

JPA entities must **never** be exposed through the REST API layer:

### Mandatory Rules:
1. **No Entity in Controller Signatures:** No `@RestController` method may accept an `@Entity` class as a parameter or return an `@Entity` class in `ResponseEntity`.
2. **Mandatory DTO Return:** Every controller endpoint must accept a Request DTO (or primitive type) and return an `ApiResponse<ResponseDTO>` (or collection/primitive).
3. **Session Termination Safety:** Because `open-in-view=false`, entity objects become detached the moment execution exits the Service layer. If Jackson attempts to serialize an uninitialized lazy association on a detached entity, serialization will fail with `LazyInitializationException`.
4. **Schema Evolution Insulation:** Using DTOs allows the internal database schema to evolve (e.g., renaming columns, optimizing joins) without breaking public API contracts consumed by web or mobile clients.

---

## 22. Rules Preventing Repository & Business-Logic Leakage

1. **Controllers Must Remain Lean:** Controllers must contain only route mapping, input validation triggering, and response wrapping. A controller method should typically consist of 3–5 lines of code.
2. **Repositories Must Contain No Domain Logic:** Repositories must never perform business calculations, evaluate domain conditions, or manage multi-step operations.
3. **Validation Belongs in Services:** Business rules must reside exclusively within the Service layer to ensure consistency regardless of whether an operation is triggered via REST API, message queue, or scheduled batch job.

---

## 23. External API & Third-Party Integration Governance

Certain future features of SIH 26135 may interface with external systems, such as:
- **SMS / OTP Gateways:** (e.g., CDAC Mobile Seva, NIC SMS gateway, Twilio)
- **Email Delivery Services:** (e.g., NIC SMTP, Amazon SES)
- **WhatsApp Business Messaging:**
- **Government Identity APIs:** (e.g., DigiLocker, Aadhaar e-KYC / UIDAI APIs)
- **Payment & DBT Gateways:** (e.g., PFMS, NPCI)

### Mandatory Governance Rules:
1. **Never Hardcode Credentials:** External API keys, client secrets, auth tokens, and endpoints must **never** be hardcoded in source code or committed to git. They must be parameterized via environment variables in `application.properties`.
2. **Formal User Configuration:** When an external integration is introduced, required configuration properties and credentials must be explicitly requested from the user.
3. **Never Mock or Silently Substitute:** In production profiles, external integrations must never be silently bypassed, faked with dummy success responses, or hardcoded.
4. **Integration Interface Abstraction:** External services must be wrapped behind clean Java interfaces (e.g., `SmsNotificationGateway`, `IdentityVerificationGateway`) so that sandbox or implementation adapters can be swapped cleanly without altering core domain services.

---

## 24. Summary Checklist for Phase 22 Implementation

All future domain implementations (Phases 22.2 onward) must conform to this checklist:

- [ ] **Package Location:** Placed under appropriate sub-package of `in.gov.sih.sih26135`.
- [ ] **Layer Decoupling:** Controller &rarr; Service &rarr; Repository &rarr; Database.
- [ ] **DTO Contracts:** Request DTO for input; Response DTO for output.
- [ ] **No Entity Leakage:** Entities never cross the service boundary outwards.
- [ ] **Explicit Mappers:** Pure Java mapping methods without reflection/MapStruct.
- [ ] **Lazy Loading Safe:** All DTO fields materialized inside the service transaction.
- [ ] **Transactions:** `@Transactional` for writes; `@Transactional(readOnly = true)` for reads.
- [ ] **Error Handling:** Domain violations throw `ResourceNotFoundException`, `BadRequestException`, or `ConflictException`.
- [ ] **Standard Enveloping:** Responses wrapped in `ApiResponse<T>`.
- [ ] **No Circular Dependencies:** Mutual service injection avoided; orchestration services used.
- [ ] **Analytics Isolation:** Pure read-only queries against `entity.analytics` via `repository.analytics`.
- [ ] **Zero Database Alterations:** Schema remains frozen and managed by `database/phase-00` to `phase-19`.
