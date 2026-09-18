# Backend Domain Implementation Architecture — SIH 26135

This document establishes the architecture conventions, layer boundaries, and design standards for all upcoming backend domain implementation phases (Phase 21.2 through Phase 21.18).

---

## 1. Core Architectural Principles

1. **Database is the Single Source of Truth**:
   - The MySQL database schema is finalized across 144 base tables and 21 analytical views.
   - The authoritative schema definitions reside exclusively in [`database/`](../database/) (Phases 00–19 SQL).
   - The backend strictly adapts to the database. The database never adapts to the backend.
   - Hibernate schema validation (`spring.jpa.hibernate.ddl-auto=validate`) remains permanently enforced. The backend never executes DDL (`CREATE`, `ALTER`, `DROP`, `TRUNCATE`).
2. **Domain-by-Domain Incremental Implementation**:
   - Domain modules are developed strictly in roadmap order (21.2 Users/Roles, 21.3 Trainees/Consent, etc.).
   - No forward leakage or premature implementation of future domain tables, APIs, or services.
3. **Strict Layer Decoupling**:
   - Controllers handle HTTP concerns and delegate immediately to services.
   - Services execute business rules, coordinate repositories, and manage transactions.
   - Repositories manage database queries and persistence operations.
   - JPA Entities are persistence models and must **never** be exposed directly as API response contracts.

---

## 2. Package Structure & Responsibilities

All backend application code resides under the root package `in.gov.sih.sih26135`:

```
in.gov.sih.sih26135
│
├── Sih26135Application.java   # Spring Boot entry point
├── entity                      # JPA persistence entities mapped to SIH26135 tables
├── repository                  # Spring Data JPA repositories
├── dto                         # Domain API contracts (Request DTOs & Response DTOs)
├── mapper                      # Explicit Entity <-> DTO converters
├── service                     # Business logic and transaction boundaries
├── controller                  # Spring REST controllers (/api/<resource>)
├── exception                   # Centralized exception hierarchy and global handler
└── response                    # Standard success envelope (ApiResponse<T>)
```

*Note: Package directories are created only when domain code is implemented within that phase, avoiding empty marker packages.*

### Package Responsibilities

| Package | Layer Role | Input | Output | Dependencies Allowed |
| :--- | :--- | :--- | :--- | :--- |
| `controller` | REST API endpoint presentation | HTTP Request, Request DTO | `ResponseEntity<ApiResponse<T>>` | `service`, `dto`, `response` |
| `service` | Business logic & transaction boundary | Request DTO, primitive domain types | Response DTO, domain entities | `repository`, `entity`, `dto`, `mapper`, `exception` |
| `repository` | Persistence access | Entity queries, IDs | JPA Entities, Optional/Lists | Spring Data JPA, `entity` |
| `entity` | JPA database mapping | Database rows | Managed entity instances | Jakarta Persistence, Hibernate types |
| `dto` | Public API request/response contracts | JSON payloads / validated fields | Immutable data carriers | Jakarta Validation, Jackson |
| `mapper` | Clean object translation | Entity or DTO | DTO or Entity | `entity`, `dto` |
| `exception` | Application domain error hierarchy | Error conditions | `ErrorResponse` payload | Spring Web, standard logging |
| `response` | Unified success envelope | Domain DTO data | Serialized JSON response | Jackson |

---

## 3. Layered Request & Response Lifecycle

### 3.1 Inbound Request Processing Flow
```
Client HTTP Request
    │
    ▼
Controller (@RestController)
    │  - Validates request payload (@Valid RequestDTO)
    │  - Delegates to Service method
    ▼
Service Layer (@Service)
    │  - Executes business logic within @Transactional boundary
    │  - Coordinates queries via Repository
    ▼
Repository Layer (@Repository)
    │  - Executes JPA / database query
    ▼
MySQL Database (SIH26135)
```

### 3.2 Outbound Response Processing Flow
```
MySQL Database (SIH26135)
    │
    ▼
Repository Layer
    │  - Returns managed Entity / Entities
    ▼
Service Layer
    │  - Converts Entity to Response DTO via explicit Mapper
    ▼
Controller Layer
    │  - Wraps Response DTO in ApiResponse<T>
    │  - Returns ResponseEntity<ApiResponse<T>> with appropriate HTTP status
    ▼
Client HTTP Response
```

### 3.3 Exception & Error Processing Flow
```
Controller / Service / Persistence
    │  - Throws BaseException subclass (e.g. ResourceNotFoundException, BadRequestException)
    │    or standard validation / HTTP exception
    ▼
GlobalExceptionHandler (@RestControllerAdvice)
    │  - Intercepts exception centrally
    │  - Logs domain errors at WARN, system errors at ERROR
    │  - Generates sanitized ErrorResponse
    ▼
Client HTTP Error Response (4xx / 5xx with sanitized JSON)
```

---

## 4. Entity & Persistence Conventions

1. **No Global `BaseEntity`**:
   - Across the 144 tables in `SIH26135`, primary key names, audit columns, status flags, and timestamps differ by domain requirements.
   - Do **not** create a global `BaseEntity`, `AbstractEntity`, or `AuditableEntity` superclass.
   - Every entity explicitly maps its corresponding table definition from `E:\project\database`.
2. **Exact Schema Fidelity**:
   - Explicit table name: `@Table(name = "<table_name>")`.
   - Explicit column names: `@Column(name = "<column_name>", nullable = ..., length = ...)`.
   - Primary Keys: map actual database key (typically `@GeneratedValue(strategy = GenerationType.IDENTITY)` with `Long` for `BIGINT UNSIGNED`).
   - Exact Column Types:
     - `CHAR(N)`: Map using `@JdbcTypeCode(SqlTypes.CHAR)` (e.g. `pincode CHAR(6)`).
     - `SMALLINT UNSIGNED`: Map using `@JdbcTypeCode(SqlTypes.SMALLINT)` with Java `Integer`.
     - `TINYINT(1)`: Map to Java `Boolean`.
     - `DATETIME`: Map to `LocalDateTime` or `Instant`.
     - `DATE`: Map to `LocalDate`.
3. **Conservative Relationship Mapping**:
   - Default fetch mode: **LAZY** on all `@ManyToOne`, `@OneToOne`, and collection relationships.
   - Never use `FetchType.EAGER` by default.
   - Always specify explicit foreign key column: `@JoinColumn(name = "<fk_column>")`.
   - Never use `CascadeType.ALL` blindly. Avoid cascading destructive operations (`REMOVE`, `DELETE`) on government, master, or audit entities.
   - Prefer unidirectional relationships unless bidirectional traversal is demonstrably required.
4. **Lookup / Reference Tables**:
   - Tables prefixed with `ref_` (e.g., `ref_gender`, `ref_user_status`, `ref_education_level`) are database master tables.
   - Do **not** substitute lookup tables with arbitrary Java enums.
   - Reference tables must be modeled as database-backed entities when referenced.

---

## 5. Repository Layer Conventions

1. Repositories must extend `JpaRepository<Entity, IdType>`.
2. Repositories contain persistence methods and query derivations only.
3. Zero business logic inside repositories.
4. Controllers must **never** inject repositories directly. All access must traverse the service layer.
5. Do not introduce speculative or unused custom queries; create queries only when required by active domain service use cases.

---

## 6. Service Layer Conventions

1. Services must be annotated with `@Service`.
2. All business logic, entity coordination, and domain validations reside in services.
3. **Transaction Management**:
   - Service classes or methods performing database modifications must declare `@Transactional`.
   - Read-only operations should declare `@Transactional(readOnly = true)` for performance and session optimization.
4. **HTTP Isolation**:
   - Services must never accept or return `HttpServletRequest`, `HttpServletResponse`, `ResponseEntity`, or Spring Web objects.
   - Services interact with DTOs and domain entities, throwing domain exceptions (`BaseException` subclasses) on violations.

---

## 7. DTO & Mapper Conventions

1. **Strict DTO / Entity Separation**:
   - JPA entities represent the internal database state and must never be exposed directly in controller endpoints.
   - Client requests are accepted via dedicated Request DTOs (e.g., `<Action><Entity>Request`).
   - Responses are returned via dedicated Response DTOs (e.g., `<Entity>Response`).
2. **Explicit Java Mappers**:
   - Mapping between Entities and DTOs is implemented via clean, explicit Java methods (static helper methods or dedicated Spring components).
   - No third-party mapping reflection libraries (no MapStruct, ModelMapper, Lombok, etc.) to keep builds simple, transparent, and reproducible.
   - Mappers perform data transformation without triggering unintended lazy loading outside transaction boundaries.

---

## 8. Response & Error Handling Standards

1. **Success Responses**:
   - All REST controllers wrap successful responses in `ApiResponse<T>`:
     ```java
     return ResponseEntity.ok(ApiResponse.success(responseDto));
     return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(responseDto, "Resource created successfully"));
     ```
   - Standard payload attributes: `success: true`, `message`, `data`, `timestamp`, `path`.
   - HTTP status remains on the HTTP response header; status codes are not redundantly duplicated inside `ApiResponse`.
2. **Error Responses**:
   - All errors are intercepted by `GlobalExceptionHandler` and serialized as `ErrorResponse`.
   - Domain exceptions extend `BaseException` with explicit HTTP status and error codes:
     - `ResourceNotFoundException` -> 404 NOT FOUND (`RESOURCE_NOT_FOUND`)
     - `BadRequestException` -> 400 BAD REQUEST (`BAD_REQUEST`)
     - `ConflictException` -> 409 CONFLICT (`CONFLICT`)
3. **Data Privacy & Sanitization**:
   - Client-facing error responses must never expose SQL details, database column names, database credentials, environment variables, internal paths, or stack traces.
   - `ResourceNotFoundException` messages format as `"%s not found with %s"` (resource name and field name only); dynamic field values (PII, emails, phone numbers) are never interpolated into client messages.
   - Unexpected server errors (500) return a generic sanitized message to the client, while full stack traces are logged internally at `ERROR` level.

---

## 9. REST API Conventions

1. **URL Paths**:
   - REST endpoints follow the resource pattern: `/api/<resources>` (plural lowercase noun, e.g., `/api/users`, `/api/trainees`, `/api/programs`).
   - Premature API version prefixes (such as `/api/v1`) are intentionally avoided until project requirements mandate multi-version routing.
2. **HTTP Verb Semantics**:
   - `GET`: Read resource or collection. Safe and idempotent.
   - `POST`: Create new resource or initiate non-idempotent operation.
   - `PUT` / `PATCH`: Update resource.
   - `DELETE`: Remove resource only where domain business rules explicitly permit deletion. Never expose hard DELETE on historical, government, or audit records.

---

## 10. Architectural Boundaries

1. **Security Boundary**:
   - Security, authentication, JWT tokens, role guards, and passwords are implemented in dedicated security/user domain phases following the Phase 02 database design.
   - No ad-hoc security filters or auth logic are introduced prematurely.
2. **Analytics Boundary**:
   - The 21 analytical views created in Phase 16 are reserved for the Analytics & Reporting read layer in Phase 21.16.
   - Domain modules must not prematurely map analytical views.
3. **Audit Boundary**:
   - Audit logging tables and data quality tables from Phase 15 are handled in Phase 21.15.
   - Domain modules must not invent separate auditing subsystems.
