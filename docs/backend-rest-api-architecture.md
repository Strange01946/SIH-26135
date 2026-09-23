# REST API Architecture & Controller Conventions — SIH 26135

**Project:** SIH 26135 — Longitudinal Tracking & Impact Analysis  
**Phase:** 23.1 — REST API Architecture & Controller Conventions (Foundation Phase)  
**Base Package:** `in.gov.sih.sih26135`  
**Database Schema:** `SIH26135` (MySQL 8.0+, frozen across Phases 00–19)  
**Authoritative Baselines:**
- Database Schema DDL: `database/phase-00` through `phase-19` SQL
- Backend Architecture Foundation: `docs/backend-architecture.md` (Phase 20)
- Backend Cross-Domain Integration: `docs/backend-cross-domain-integration.md` (Phase 21.17)
- Backend Domain Verification: `docs/backend-domain-verification.md` (Phase 21.18)
- Application & Service Layer Architecture: `docs/backend-application-architecture.md` (Phase 22.1)

---

## 1. Executive Summary & Purpose

This document establishes the definitive architectural governance, conventions, standards, and contracts for the REST API Controller Layer (Phase 23) of the SIH 26135 backend.

Phase 23 transitions the completed, fully-tested, and verified Domain and Application / Service Layers (Phases 20–22) into standard, clean, and secure RESTful HTTP web services. All controllers implemented in subphases 23.2 onward must adhere strictly to the rules, patterns, and contracts defined herein.

### Core Architectural Principles
1. **HTTP Adapters Only**: Controllers are purely boundary presentation adapters. They translate HTTP requests into service method calls and service responses into HTTP responses.
2. **Zero Direct Persistence Access**: Controllers **never** interact with repositories (`*Repository`), `EntityManager`, or native SQL queries. All persistence operations occur exclusively through Application Services.
3. **Zero Business Logic & Invariant Enforcement**: Business rules, state transition rules, and domain invariants remain encapsulated within the Application / Service Layer. Controllers never perform domain validations or state calculations.
4. **Strict DTO Boundary**: JPA entities are never accepted as controller inputs or returned as controller outputs. All communication is strictly handled via strongly-typed request DTOs (`in.gov.sih.sih26135.dto.request`) and response DTOs / records (`in.gov.sih.sih26135.dto.response`).
5. **Unified Envelope Serialization**: All successful API responses are wrapped in the established, immutable `ApiResponse<T>` envelope.
6. **Centralized, Sanitized Error Handling**: Errors and exceptions are captured and converted into standardized `ErrorResponse` objects by the existing `GlobalExceptionHandler`. Controllers never implement custom error handling blocks that leak internal stack traces or database structures.
7. **Security Isolation**: Authentication and authorization mechanisms (Spring Security, JWT filters, role-based checks) are strictly deferred to **Phase 24**. Phase 23 controllers expose pure HTTP contracts without mocking or hardcoding security logic.

---

## 2. API Routing & Naming Conventions

### 2.1 Base Path & URL Versioning
All REST endpoints exposed by the SIH 26135 backend must share a single, uniform, versioned base URI prefix:

```
/api/v1
```

- **Prefix Consistency**: No controller may introduce alternate prefixes (e.g., `/api`, `/v2`, `/rest`, `/services`).
- **Versioning Strategy**: Path-based versioning (`/api/v1`) guarantees client predictability, forward compatibility, and operational stability.

### 2.2 Resource Naming Standards
Resource paths follow standard RESTful conventions:

1. **Plural Nouns**: Resource paths must use plural nouns representing the aggregate collection:
   - `/api/v1/users`
   - `/api/v1/trainees`
   - `/api/v1/training-enrollments`
   - `/api/v1/employment-records`
   - `/api/v1/placement-records`
   - `/api/v1/skill-gaps`
2. **Kebab-Case for Multi-Word Resources**: Paths containing multiple words must use hyphen-separated lowercase characters:
   - Correct: `/api/v1/employment-records`, `/api/v1/followup-tasks`, `/api/v1/data-quality-issues`
   - Incorrect: `/api/v1/employmentRecords`, `/api/v1/employment_records`, `/api/v1/EmploymentRecords`
3. **No Verbs in Resource Paths**: REST resources represent entities or workflow instances, not remote procedure calls:
   - Correct: `POST /api/v1/certifications`
   - Incorrect: `POST /api/v1/certifications/create`, `GET /api/v1/trainees/getById`
4. **Hierarchical Sub-Resource Paths**: When a child resource belongs naturally to a parent aggregate and cannot exist independently, a nested sub-path may be used:
   - `/api/v1/trainees/{traineeId}/enrollments`
   - `/api/v1/employment-records/{employmentId}/salary-history`
   - `/api/v1/skill-gaps/{skillGapId}/recommendations`
   - `/api/v1/followup-tasks/{taskId}/survey-responses`

---

## 3. Package Structure & Organization

All controller classes reside within the dedicated presentation package:

```
in.gov.sih.sih26135.controller
├── UserController.java
├── TraineeController.java
├── TrainingEnrollmentController.java
├── CourseController.java
├── PlacementRecordController.java
├── EmploymentRecordController.java
├── ...
├── analytics/                             # Dedicated read-only analytics controllers
│   ├── AnalyticsFactController.java
│   └── AnalyticsSummaryController.java
└── workflow/                              # Dedicated cross-domain orchestration controllers
    ├── TrainingOutcomeWorkflowController.java
    ├── PlacementEmploymentWorkflowController.java
    ├── EmploymentLifecycleWorkflowController.java
    ├── VerificationWorkflowController.java
    ├── SkillGapRemediationWorkflowController.java
    └── FollowupEngagementWorkflowController.java
```

### Layer Coupling Matrix

| Component | May Depend On | Must NOT Depend On |
| :--- | :--- | :--- |
| **REST Controller** | Domain Service interfaces (`service.*`), Workflow Service interfaces (`service.*`), DTOs (`dto.*`), Response Envelope (`response.ApiResponse`), Exception classes (`exception.*`) | Repositories (`repository.*`), JPA Entities (`entity.*`), EntityManager, Spring Transaction (`@Transactional`), Security filters |

---

## 4. Controller Responsibilities & Design Patterns

### 4.1 Stereotype Annotations
Every controller class must be declared with:
1. `@RestController`: Configures the class as a Spring REST component where all method return values are automatically serialized into JSON HTTP response bodies.
2. `@RequestMapping("/api/v1/<resource>")`: Defines the base URI path for all endpoints in the controller.
3. Explicit constructor injection for service interfaces. Field injection (`@Autowired` on private fields) is strictly prohibited.

### 4.2 Standard Method Signatures
Controller methods must always return `ResponseEntity<ApiResponse<T>>`, where `T` is the specific response DTO record or collection.

```java
@RestController
@RequestMapping("/api/v1/trainees")
public class TraineeController {

  private final TraineeService traineeService;

  public TraineeController(TraineeService traineeService) {
    this.traineeService = traineeService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TraineeResponse>> getById(@PathVariable Long id) {
    TraineeResponse response = traineeService.getTraineeById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<TraineeResponse>> create(
      @RequestBody CreateTraineeRequest request,
      HttpServletRequest httpRequest) {
    TraineeResponse response = traineeService.createTrainee(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Trainee created successfully", response, httpRequest.getRequestURI()));
  }
}
```

---

## 5. HTTP Method Semantics & Status Conventions

Controllers must strictly map HTTP verbs according to standard REST semantics:

| HTTP Verb | Operation Semantics | Service Action | Expected Success Status | Body Required? |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | Read / Query | `getById()`, `getAll()`, `findByCriteria()` | `200 OK` | Yes (`ApiResponse<T>`) |
| `POST` | Create / Action | `create*()`, `log*()`, `transition*()` | `201 Created` | Yes (`ApiResponse<T>`) |
| `PUT` | Idempotent Update | `update*()` | `200 OK` | Yes (`ApiResponse<T>`) |
| `PATCH` | Partial Update | `update*()` (specialized state toggle) | `200 OK` | Yes (`ApiResponse<T>`) |
| `DELETE` | Soft Delete / Removal | `delete*()` | `200 OK` | Yes (`ApiResponse<Void>`) |

### Status Code Guidelines:
1. **`200 OK`**:
   - Used for all successful `GET` queries.
   - Used for all successful `PUT` or `PATCH` modifications.
   - Used for successful `DELETE` operations when an `ApiResponse` envelope message is returned.
2. **`201 Created`**:
   - Used for all successful `POST` operations that create new persistent records or workflow events.
   - The response envelope contains the newly created representation with its generated ID and timestamps.
3. **`204 No Content`**:
   - Reserved strictly for situations where an endpoint intentionally produces an empty HTTP response body.
   - *Note*: Because government and enterprise clients consume the consistent `ApiResponse<T>` envelope across all calls, `200 OK` with `ApiResponse.success("Resource deleted successfully")` is preferred over `204 No Content` for delete operations.
4. **`400 Bad Request`**:
   - Emitted when syntactic validation fails (missing required parameters, malformed JSON body) or when a domain service rejects an invalid state transition via `BadRequestException`.
5. **`404 Not Found`**:
   - Emitted when an entity identifier in the URL path variable does not exist via `ResourceNotFoundException`.
6. **`409 Conflict`**:
   - Emitted when a unique constraint or duplicate natural business key is violated via `ConflictException`.
7. **`500 Internal Server Error`**:
   - Emitted for unexpected runtime bugs, caught and sanitized by `GlobalExceptionHandler`.

---

## 6. Response Envelope Standards (`ApiResponse<T>`)

Every successful controller response must be encapsulated in `ApiResponse<T>` located in `in.gov.sih.sih26135.response.ApiResponse`.

### 6.1 Envelope Structure
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-24T03:50:00.000Z",
  "path": "/api/v1/trainees/101"
}
```

### 6.2 Factory Method Usage Rules

| Scenario | Recommended Factory Method | Sample Usage |
| :--- | :--- | :--- |
| Single resource query | `ApiResponse.ok(data)` | `return ResponseEntity.ok(ApiResponse.ok(response));` |
| List / Collection query | `ApiResponse.ok(dataList)` | `return ResponseEntity.ok(ApiResponse.ok(list));` |
| Resource creation | `ApiResponse.success(message, data, path)` | `return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created successfully", response, path));` |
| Resource update | `ApiResponse.success(message, data)` | `return ResponseEntity.ok(ApiResponse.success("Updated successfully", response));` |
| Resource deletion | `ApiResponse.success(message)` | `return ResponseEntity.ok(ApiResponse.success("Deleted successfully"));` |

**Rule**: Under no circumstances should a controller create a secondary response wrapper or return a naked DTO without `ApiResponse<T>`.

---

## 7. Error Handling Architecture

REST controllers rely entirely on the centralized `GlobalExceptionHandler` (`@RestControllerAdvice`) located in `in.gov.sih.sih26135.exception`.

### 7.1 Exception-to-HTTP Status Mapping

```
                               ┌────────────────────────────────────────────────┐
                               │             Domain/HTTP Exception              │
                               └───────────────────────┬────────────────────────┘
                                                       │
                     ┌─────────────────────────────────┼────────────────────────────────┐
                     ▼                                 ▼                                ▼
       ┌───────────────────────────┐     ┌───────────────────────────┐    ┌───────────────────────────┐
       │ ResourceNotFoundException │     │    BadRequestException    │    │     ConflictException     │
       └─────────────┬─────────────┘     └─────────────┬─────────────┘    └─────────────┬─────────────┘
                     ▼                                 ▼                                ▼
              404 NOT_FOUND                     400 BAD_REQUEST                  409 CONFLICT
                     │                                 │                                │
                     └─────────────────────────────────┼────────────────────────────────┘
                                                       │
                                                       ▼
                                       ┌───────────────────────────────┐
                                       │    ErrorResponse Envelope     │
                                       │ - timestamp                   │
                                       │ - status (4xx / 500)          │
                                       │ - error (HTTP reason)         │
                                       │ - code (e.g. TRAINEE_NOT_FND) │
                                       │ - message (sanitized)         │
                                       │ - path (request URI)          │
                                       └───────────────────────────────┘
```

### 7.2 Controller Error Rules
1. **No `try-catch` Wrappers in Controllers**: Controllers must let application exceptions bubble up naturally to `GlobalExceptionHandler`.
2. **Sanitized Payloads**: Error responses never include stack traces, internal table names, or database SQL messages.
3. **Descriptive Codes**: Application exceptions thrown by services supply unique machine-readable error codes (e.g., `TRAINEE_NOT_FOUND`, `DUPLICATE_REGISTRATION_NUMBER`, `UNEMPLOYMENT_START_BEFORE_SEPARATION`).

---

## 8. Path Variables vs. Query Parameters

Controllers must strictly differentiate between resource identification and query criteria:

### 8.1 Path Variables (`@PathVariable`)
Used strictly for **resource identification** where the URL path indicates a specific individual entity instance:
- `GET /api/v1/trainees/{id}`
- `GET /api/v1/courses/{id}`
- `PUT /api/v1/employment-records/{id}`
- `DELETE /api/v1/training-enrollments/{id}`

### 8.2 Query Parameters (`@RequestParam`)
Used strictly for **filtering, searching, and criteria selection** over a resource collection:
- Filtering by parent key: `GET /api/v1/training-enrollments?traineeId=101`
- Filtering by reference status: `GET /api/v1/placement-records?statusId=2`
- Filtering by date: `GET /api/v1/attendance-records?batchId=5&date=2026-09-24`
- Inclusion of soft-deleted records: `GET /api/v1/employment-records?includeDeleted=true`

### 8.3 Query Parameter Rules
- **No Unsupported Parameters**: Controllers must only expose query parameters that correspond directly to existing finder methods on the underlying Application Service.
- **No In-Memory Pagination**: Do not introduce pagination query parameters (`page`, `size`) unless the service layer explicitly supports paginated retrieval.

---

## 9. Domain Controllers vs. Cross-Domain Workflow Controllers

Phase 23 separates controllers into two distinct architectural categories:

### 9.1 Domain Controllers
- **Scope**: Direct CRUD and query operations on individual domain aggregates.
- **Dependency**: Inject and call a single domain application service (e.g. `TraineeService`, `CourseService`, `EmploymentRecordService`).
- **Location**: `in.gov.sih.sih26135.controller`
- **Example Endpoints**:
  - `POST /api/v1/trainees`
  - `GET /api/v1/trainees/{id}`
  - `PUT /api/v1/trainees/{id}`
  - `DELETE /api/v1/trainees/{id}`

### 9.2 Cross-Domain Workflow Controllers
- **Scope**: Complex, multi-step business transactions spanning multiple domain aggregates.
- **Dependency**: Inject and call the Phase 22.17 workflow orchestration services.
- **Location**: `in.gov.sih.sih26135.controller.workflow`
- **Rule**: Controllers must **never** manually coordinate multiple domain services in an endpoint. All cross-domain operations must delegate to their respective workflow service.

#### The 6 Workflow Controllers & Endpoints:

| Workflow Controller | Service Injected | Base Route | Key Operations |
| :--- | :--- | :--- | :--- |
| `TrainingOutcomeWorkflowController` | `TrainingOutcomeWorkflowService` | `/api/v1/workflows/training-outcomes` | - `POST /record-assessment-and-issue-certificate`<br>- `GET /summary?traineeId={tId}&enrollmentId={eId}` |
| `PlacementEmploymentWorkflowController` | `PlacementEmploymentWorkflowService` | `/api/v1/workflows/placement-employment` | - `POST /transition-placement-to-employment`<br>- `GET /summary?traineeId={tId}` |
| `EmploymentLifecycleWorkflowController` | `EmploymentLifecycleWorkflowService` | `/api/v1/workflows/employment-lifecycle` | - `POST /record-exit-and-initiate-unemployment`<br>- `POST /transition-unemployment-to-employment`<br>- `GET /timeline?traineeId={tId}` |
| `VerificationWorkflowController` | `VerificationWorkflowService` | `/api/v1/workflows/verification` | - `POST /initiate-from-survey`<br>- `POST /record-verdict`<br>- `GET /summary?employmentId={eId}` |
| `SkillGapRemediationWorkflowController` | `SkillGapRemediationWorkflowService` | `/api/v1/workflows/skill-gap-remediation` | - `POST /record-assessment-with-gaps`<br>- `POST /enroll-remediation-course`<br>- `GET /profile?traineeId={tId}` |
| `FollowupEngagementWorkflowController` | `FollowupEngagementWorkflowService` | `/api/v1/workflows/followup-engagement` | - `POST /record-survey-response`<br>- `POST /record-engagement-attempt`<br>- `GET /history?traineeId={tId}` |

---

## 10. Analytics Read-Only API Conventions

Phase 22.16 established 21 analytical read models (11 fact projections and 10 summary aggregates) mapped to frozen MySQL analytical views.

### 10.1 Dedicated Analytics Controllers
Controllers exposing analytical views reside in `in.gov.sih.sih26135.controller.analytics`:
- `AnalyticsFactController`: Exposes the 11 analytical fact projections (`/api/v1/analytics/facts/...`).
- `AnalyticsSummaryController`: Exposes the 10 analytical summaries and leaderboards (`/api/v1/analytics/summaries/...`).

### 10.2 Strict Read-Only Semantics
1. **GET Only**: Analytics controllers expose **only** `GET` endpoints.
2. **Zero Mutations**: Zero `POST`, `PUT`, `PATCH`, or `DELETE` endpoints may be mapped to analytical views.
3. **Authoritative SQL Projections**: Analytics controllers return view data verbatim via domain services without client-side recalculation or modification.

---

## 11. Immutability, Terminal States & Deletion Rules

Controllers must strictly respect database and domain lifecycle immutability:

1. **Terminal Records**: Completed enrollments, finalized assessment scores, verified verifications, and dismissed data quality issues cannot be deleted. Any attempt to delete returns `400 Bad Request`.
2. **Immutable Audit & System Logs**: The following resources are append-only; controllers must **never** expose `DELETE` or `PUT` endpoints for them:
   - `AuditLog`
   - `SystemEventLog`
   - `DataQualityIssueEvent`
   - `EmploymentExitEvent`
   - `SalaryHistory`
   - `CommunicationLog`
3. **Soft Delete Preservation**: Where domain services implement soft-delete (`deletedAt`), deletion sets the timestamp. Controllers do not execute physical removal and reject modifications on soft-deleted entities.

---

## 12. Validation Strategy at Controller Boundary

Request validation is divided into two distinct responsibilities:

1. **Boundary / Syntactic Validation (Controller Layer)**:
   - Ensuring HTTP request body is non-null.
   - Ensuring required path variables and query parameters are present and parsable into `Long`, `Integer`, or `LocalDate`.
   - Handling malformed JSON formats (handled automatically via `HttpMessageNotReadableException`).
2. **Domain / Semantic Validation (Application Service Layer)**:
   - Cross-domain entity consistency (e.g. trainee ownership matches across enrollment and placement).
   - Temporal sequence validation (e.g. `startDate <= endDate`, `separationDate >= employmentStartDate`).
   - Natural business key uniqueness checks.
   - Reference catalog validity.

---

## 13. Logging & Privacy Standards

All controllers must adhere to enterprise government security and logging standards:

1. **SLF4J Logger**: Declared statically per controller:
   ```java
   private static final Logger log = LoggerFactory.getLogger(TraineeController.class);
   ```
2. **No Sensitive Field Logging**: Under no circumstances may controllers log:
   - Passwords or password hashes
   - Authentication tokens or JWTs
   - API keys or secrets
   - Plaintext Aadhaar or national identity numbers
   - Personal PII (phone numbers, personal emails) in debug/info strings
3. **Safe Operational Logging**: Controllers log resource IDs, endpoint paths, and caller actions:
   ```java
   log.info("Request received to fetch employment record id: {}", id);
   ```

---

## 14. Transaction & Security Boundaries

### 14.1 Zero Transactions in Controllers
- Controllers must **never** declare `@Transactional`.
- Transaction boundaries reside exclusively inside Application and Workflow services. This guarantees that transactions do not span HTTP serialization or network socket writes.

### 14.2 Security Layer Deferral (Phase 24)
- All security concerns (Spring Security, Bearer tokens, JWT filters, authentication endpoints, and permission interceptors) are explicitly deferred to **Phase 24**.
- Controllers in Phase 23 will not implement mock authentication headers, synthetic user sessions, or hardcoded security contexts.

---

## 15. Standard Controller Documentation Template

When implementing controllers in Phase 23.2 onward, each class and endpoint must be documented following this template:

```java
/**
 * REST controller for managing Trainee domain entities.
 *
 * <p>Base Route: /api/v1/trainees
 * Consumes: in.gov.sih.sih26135.dto.request.CreateTraineeRequest, UpdateTraineeRequest
 * Produces: in.gov.sih.sih26135.dto.response.TraineeResponse
 */
@RestController
@RequestMapping("/api/v1/trainees")
public class TraineeController {
  ...
  /**
   * Retrieves a trainee by primary key identifier.
   *
   * @param id primary key identifier of the trainee
   * @return 200 OK with TraineeResponse enveloped in ApiResponse
   * @throws ResourceNotFoundException (404) if trainee does not exist
   * @throws BadRequestException (400) if ID is invalid
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TraineeResponse>> getById(@PathVariable Long id) {
    ...
  }
}
```

---

## 16. Phased Implementation Roadmap for Phase 23

The upcoming controller implementation phases will proceed in strict domain order:

- **23.2**: Users, Roles, and Permission Controllers
- **23.3**: Trainee and Consent Controllers
- **23.4**: Program, Scheme, and Provider Controllers
- **23.5**: Training Center, Course, and Batch Controllers
- **23.6**: Skill and Job Role Controllers
- **23.7**: Enrollment and Attendance Controllers
- **23.8**: Assessment and Certification Controllers
- **23.9**: Employer, Job Posting, and Placement Controllers
- **23.10**: Employment Record and Salary History Controllers
- **23.11**: Follow-Up Campaign, Task, and Survey Controllers
- **23.12**: Employment Verification Controllers
- **23.13**: Skill Gap Assessment and Recommendation Controllers
- **23.14**: Unemployment and Attrition Controllers
- **23.15**: Audit, Data Quality, and System Log Controllers
- **23.16**: Analytics Fact and Summary Read-Only Controllers
- **23.17**: Cross-Domain Workflow Orchestration Controllers
- **23.18**: Phase 23 REST API Final Verification
