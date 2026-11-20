# SIH-26135 Backend Observability & Metrics Guide
**Spring Boot Actuator, Prometheus Metrics, Custom Health Indicators & Structured Logging**

---

## 1. Executive Summary & Observability Architecture

This guide documents the enterprise observability architecture implemented in Phase 29.11 for the **SIH-26135 Spring Boot 3.4.10** backend (`sih26135-backend`).

The observability stack delivers production-grade metrics exposition, health diagnostics, structured logging, and request correlation tailored for local-PC development, testing, demonstration, and future operational monitoring.

```
                              ┌─────────────────────────────────────────────────────────────┐
                              │                   Spring Security Filter                    │
                              └──────────────────────────────┬──────────────────────────────┘
                                                             │
                              ┌──────────────────────────────┴──────────────────────────────┐
                              │                                                             │
                  [Public / Anonymous]                                           [Protected: system.manage / ROLE_ADMIN]
                              │                                                             │
         ┌────────────────────┴────────────────────┐                   ┌────────────────────┴────────────────────┐
         │                                         │                   │                                         │
┌─────────────────┐                       ┌─────────────────┐ ┌─────────────────┐                       ┌─────────────────┐
│ /actuator/health│                       │ /actuator/info  │ │/actuator/       │                       │/actuator/       │
│  (Basic Status) │                       │ (App Metadata)  │ │  prometheus     │                       │  diagnostics    │
└────────┬────────┘                       └─────────────────┘ └────────┬────────┘                       └────────┬────────┘
         │                                                             │                                         │
         ├─ /liveness (livenessState)                                  ├─ JVM & HikariCP Metrics                 ├─ databaseLatency
         └─ /readiness (readinessState, db, databaseLatency)           ├─ HTTP Server Requests                   ├─ caffeineCache
                                                                       └─ Custom Business Metrics:               ├─ bulkImportStorage
                                                                          • workflow.transition.duration         ├─ communicationGateway
                                                                          • bulk.import.rows.processed           └─ diskSpace
                                                                          • data.quality.cleanliness.index
                                                                          • communication.dispatch.total
```

### Core Design Principles
1. **Zero Database Overhead on Metrics Scrapes**: Prometheus scrapes (`/actuator/prometheus`) read O(1) in-memory atomic counters and gauges. Metrics exposition never initiates database aggregation or heavy reporting queries.
2. **Fail-Closed Security Boundary**: Public access is strictly limited to high-level health probes (`/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`) and `/actuator/info`. Detailed diagnostic components, metrics scrapers, and Prometheus endpoints require JWT authentication with `system.manage`, `ROLE_ADMIN`, or `ROLE_super_admin`.
3. **Sensitive Endpoint Exclusion**: Highly sensitive Actuator endpoints (`env`, `configprops`, `beans`, `heapdump`, `threaddump`, `logfile`, `mappings`, `shutdown`) are explicitly excluded and disabled, returning HTTP 404.
4. **Strict Zero PII**: All metric tag names and tag values use low-cardinality enumerated values (e.g., `TRAINEE`, `SUCCESS`, `SMS`). Citizen Aadhaar numbers, phone numbers, email addresses, names, and user IDs are strictly prohibited.
5. **Passive Health Inspections**: Health checks never make outbound network calls, never send SMS/WhatsApp/Email messages, and never incur third-party provider costs.

---

## 2. Actuator Endpoint Matrix & Access Control

| Endpoint Path | Purpose | Access Level | Authorization Required | Details Returned |
|:---|:---|:---|:---|:---|
| `/actuator/health` | Overall system health status | Public / Anonymous | None | `{"status":"UP"}` (Sanitized; zero component details) |
| `/actuator/health` | Authorized system health | Authenticated | `ROLE_ADMIN`, `ROLE_super_admin` | Full component tree (`db`, `databaseLatency`, `caffeineCache`, etc.) |
| `/actuator/health/liveness` | Container/process liveness | Public / Anonymous | None | `{"status":"UP"}` (Liveness probe) |
| `/actuator/health/readiness` | Traffic readiness probe | Public / Anonymous | None | `{"status":"UP"}` (Checks DB connection & readiness) |
| `/actuator/health/diagnostics`| Operational dependency health | Authenticated | `system.manage`, `ROLE_ADMIN` | Detailed latency, cache, storage & gateway diagnostics |
| `/actuator/info` | Application build & runtime info| Public / Anonymous | None | `{"app": {...}}` |
| `/actuator/prometheus` | Prometheus exposition scraper | Authenticated | `system.manage`, `ROLE_ADMIN`, `ROLE_super_admin` | Prometheus exposition format (text/plain) |
| `/actuator/metrics/**` | Micrometer metrics browser | Authenticated | `system.manage`, `ROLE_ADMIN` | JSON metric exploration |
| `/actuator/env`, `/beans`, `/heapdump`, etc. | Sensitive internals | **Disabled** | Excluded via properties | **HTTP 404 Not Found** |

---

## 3. Local-PC Demonstration & Scrape Instructions

### Step 1: Verify Public Health & Probes (No Authentication)

Test high-level health without any bearer token:
```bash
curl -X GET http://localhost:8080/actuator/health
```
**Expected Response:**
```json
{"status":"UP"}
```
*(Notice that internal component details, database names, and disk paths are completely hidden from anonymous callers).*

Test liveness and readiness probes:
```bash
curl -X GET http://localhost:8080/actuator/health/liveness
curl -X GET http://localhost:8080/actuator/health/readiness
```
**Expected Response:**
```json
{"status":"UP"}
```

---

### Step 2: Obtain an Administrative JWT Bearer Token

Log in as the system administrator using the standard authentication endpoint:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "root_admin",
    "password": "AuthoritativeAdminPassword123!"
  }'
```
**Response Extract:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJyb290X2FkbWluIiwicm9sZXMiOlsic3lzdGVtLm1hbmFnZSIsIlJPTEVfQURNSU4iXSwiaWF0IjoxNzg0ODAwMDAwLCJleHAiOjE3ODQ4MDM2MDB9...",
    "tokenType": "Bearer"
  }
}
```
Store the access token in an environment variable:
```bash
export AUTH_TOKEN="<accessToken_value>"
```

---

### Step 3: Scrape Prometheus Metrics (Authenticated)

Attempting to scrape `/actuator/prometheus` without a token fails with **HTTP 401 Unauthorized**:
```bash
curl -i -X GET http://localhost:8080/actuator/prometheus
# HTTP/1.1 401 Unauthorized
```

Non-administrative users (such as data entry clerks) receive **HTTP 403 Forbidden**:
```bash
curl -i -X GET http://localhost:8080/actuator/prometheus \
  -H "Authorization: Bearer <CLERK_TOKEN>"
# HTTP/1.1 403 Forbidden
```

Scrape `/actuator/prometheus` with the administrative token:
```bash
curl -X GET http://localhost:8080/actuator/prometheus \
  -H "Authorization: Bearer $AUTH_TOKEN"
```
**Sample Output:**
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Survivor Space",} 1.048576E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen",} 7.340032E7
jvm_memory_used_bytes{area="nonheap",id="Metaspace",} 8.1256448E7
# HELP hikaricp_connections Active, idle, and total connections
# TYPE hikaricp_connections gauge
hikaricp_connections{pool="HikariPool-1",} 10.0
# HELP workflow_transition_duration_seconds Execution duration of Phase 25 workflow transitions
# TYPE workflow_transition_duration_seconds summary
workflow_transition_duration_seconds_count{outcome="SUCCESS",transition="APPROVE_CLAIM",workflow_type="DISBURSEMENT",} 12.0
workflow_transition_duration_seconds_sum{outcome="SUCCESS",transition="APPROVE_CLAIM",workflow_type="DISBURSEMENT",} 0.456
# HELP bulk_import_rows_processed_total Total number of bulk import rows processed across ingestion pipelines
# TYPE bulk_import_rows_processed_total counter
bulk_import_rows_processed_total{entity_type="TRAINEE",outcome="SUCCESS",} 450.0
bulk_import_rows_processed_total{entity_type="TRAINEE",outcome="FAILED",} 12.0
# HELP data_quality_cleanliness_index Overall system Data Quality Cleanliness Index percentage (0.0 - 100.0)
# TYPE data_quality_cleanliness_index gauge
data_quality_cleanliness_index 96.84
# HELP communication_dispatch_total Total communication dispatch attempts partitioned by channel and outcome
# TYPE communication_dispatch_total counter
communication_dispatch_total{channel="SMS",outcome="SUCCESS",} 85.0
communication_dispatch_total{channel="WHATSAPP",outcome="BLOCKED_NO_CONSENT",} 3.0
```

---

### Step 4: Inspect Authorized Health & Diagnostics

Inspect the full health breakdown:
```bash
curl -X GET http://localhost:8080/actuator/health \
  -H "Authorization: Bearer $AUTH_TOKEN"
```
**Sample Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "databaseLatency": {
      "status": "UP",
      "details": {
        "latencyMs": 4,
        "thresholdMs": 500,
        "query": "SELECT 1"
      }
    },
    "caffeineCache": {
      "status": "UP",
      "details": {
        "configuredCachesCount": 9,
        "aggregateHitRatePct": 85.42,
        "totalHits": 1024,
        "totalMisses": 175,
        "totalEstimatedEntries": 48
      }
    },
    "bulkImportStorage": {
      "status": "UP",
      "details": {
        "storageConfigured": true,
        "usableSpaceGiB": 51.69,
        "thresholdGiB": 1.0,
        "usableSpaceBytes": 55500726272,
        "thresholdBytes": 1073741824
      }
    },
    "communicationGateway": {
      "status": "UP",
      "details": {
        "mode": "MOCK",
        "emailProviderType": "AUTO",
        "liveOverrideEnabled": false,
        "providers": {
          "smsGatewayConfigured": true,
          "whatsAppGatewayConfigured": true,
          "smtpConfigured": true,
          "sendgridConfigured": false
        }
      }
    }
  }
}
```

---

## 4. Custom Health Indicators Reference

### 1. `DatabaseLatencyHealthIndicator` (`databaseLatency`)
- **Location**: `in.gov.sih.sih26135.actuator.health.DatabaseLatencyHealthIndicator`
- **Health Group**: Included in `readiness` and `diagnostics`.
- **Mechanism**: Executes a lightweight monotonic `SELECT 1` ping using `JdbcTemplate`.
- **Latency SLA**: Configurable via `management.health.database.latency-threshold-ms` (default: `500` ms).
- **Behavior**: If the database query executes within the threshold, returns `UP`. If latency exceeds the threshold, returns `WARNING` (with latency details) to signal slow queries without bringing the application pod DOWN. Records latency via `database.query.latency` timer.

### 2. `CaffeineCacheHealthIndicator` (`caffeineCache`)
- **Location**: `in.gov.sih.sih26135.actuator.health.CaffeineCacheHealthIndicator`
- **Health Group**: Included in `diagnostics`.
- **Mechanism**: Inspects all active Spring Caffeine caches (`analytics_state_aggregates`, `analytics_district_aggregates`, `analytics_summaries`, etc.).
- **Metrics Collected**: Estimated entry count, hit count, miss count, eviction count, and aggregate hit rate percentage.
- **Fail-Safe Design**: Never returns `DOWN` on empty caches or a 0% hit rate, as cache misses are normal in early-lifecycle application runs.

### 3. `BulkImportStorageHealthIndicator` (`bulkImportStorage`)
- **Location**: `in.gov.sih.sih26135.actuator.health.BulkImportStorageHealthIndicator`
- **Health Group**: Included in `diagnostics`.
- **Mechanism**: Inspects the staging directory filesystem usable space without creating, reading, or modifying test files.
- **Storage Threshold**: Configurable via `management.health.bulk-import.min-disk-space-bytes` (default: 1 GiB = `1073741824` bytes).
- **Zero Information Leakage**: Absolute file paths and folder names are strictly omitted from health response details. Only numeric sizes and GiB values are reported.
- **Non-Disruptive Warning**: If usable space drops below 1 GiB, returns `LOW_DISK_SPACE` to trigger operator alerts without halting the JVM.

### 4. `CommunicationGatewayHealthIndicator` (`communicationGateway`)
- **Location**: `in.gov.sih.sih26135.actuator.health.CommunicationGatewayHealthIndicator`
- **Health Group**: Included in `diagnostics`.
- **Mechanism**: Purely passive configuration evaluation of Twilio (SMS & WhatsApp) and SendGrid/SMTP (Email) settings.
- **Zero Outbound Calls**: Never makes live API calls to external telecom or email providers during health evaluations, avoiding account throttling, credit consumption, or billing overhead.

---

## 5. Custom Micrometer Business Metrics Reference

| Metric Name | Type | Unit | Bounded Tags | Description |
|:---|:---|:---|:---|:---|
| `workflow.transition.duration` | Timer | Seconds | `workflow.type` (e.g. `ENROLLMENT`, `ASSESSMENT`, `DISBURSEMENT`), `transition` (e.g. `SUBMIT`, `APPROVE`), `outcome` (`SUCCESS`, `FAILURE`) | Measures execution duration and failure rates of Phase 25 workflow transitions. Captured automatically via AOP aspect `WorkflowMetricsAspect`. |
| `bulk.import.rows.processed` | Counter | Rows | `entity.type` (`TRAINEE`, `PLACEMENT`), `outcome` (`PROCESSED`, `SUCCESS`, `FAILED`, `SKIPPED`, `DUPLICATE`) | Measures ingestion throughput and validation outcomes during chunked and row-level batch processing in `ImportBatchProcessingWorkflowImpl`. |
| `data.quality.cleanliness.index` | Gauge | Percent | *None (System Aggregate)* | Exposes the system-wide Data Quality Cleanliness Index percentage (0.0 to 100.0). Maintained in an atomic in-memory reference, updated whenever DQI scorecards are computed. O(1) read latency on Prometheus scrapes. |
| `communication.dispatch.total` | Counter | Messages | `channel` (`SMS`, `WHATSAPP`, `EMAIL`), `outcome` (`SUCCESS`, `FAILURE`, `BLOCKED_NO_CONSENT`, `QUEUED`) | Tracks outbound notification attempts across channels. Accurately counts consent enforcement blocks and durable queue buffering. |
| `database.query.latency` | Timer | Seconds | *None* | Captures database ping round-trip times during health check invocations. |

---

## 6. Structured JSON Logging & MDC Correlation

### Spring Boot 3.4 Native Structured Logging
Spring Boot 3.4.10 includes built-in structured logging formatters. You can toggle structured JSON logging on the console without external logback encoders:

```properties
# Human-readable format by default (ideal for local terminal development)
logging.structured.format.console=${LOGGING_STRUCTURED_FORMAT:}
```

To enable structured JSON logging locally or in production, set the environment variable:
```bash
# Elastic Common Schema (ECS) format:
export LOGGING_STRUCTURED_FORMAT=ecs

# Or Logstash JSON format:
export LOGGING_STRUCTURED_FORMAT=logstash
```

### Sample Structured Log Entry (ECS Format)
```json
{
  "@timestamp": "2026-11-20T17:05:32.031+05:30",
  "log.level": "INFO",
  "message": "Enqueued dispatch job 412: traineeId=1005143, channel=SMS, mode=MOCK, idempotencyKey=sms-enroll-412",
  "process.thread.name": "http-nio-8080-exec-1",
  "log.logger": "in.gov.sih.sih26135.communication.queue.DurableDispatchQueueServiceImpl",
  "correlationId": "8b7bbeb4-9d82-4ed5-8ffe-34256e2883e7"
}
```

### MDC Correlation ID Lifecycle
1. **Inbound HTTP Request**: `CorrelationIdFilter` inspects incoming HTTP headers for `X-Correlation-Id`.
   - If present, the value is sanitized (alphanumeric and hyphens only).
   - If missing or invalid, a new standard UUID is generated.
2. **Context Binding**: The correlation ID is placed into SLF4J MDC under the key `correlationId`.
3. **Response Header Echo**: The correlation ID is added to the HTTP response header `X-Correlation-Id` for end-to-end tracing by callers and Postman.
4. **Async Task Propagation**: `AsyncConfig.correlationTaskDecorator()` captures the active MDC context from the dispatching thread and copies it into worker threads in `ThreadPoolTaskExecutor`, ensuring asynchronous workflows retain their correlation ID.
5. **Context Cleanup**: `CorrelationIdFilter.doFilterInternal()` cleans up MDC via `MDC.remove("correlationId")` in a `finally` block, preventing thread pool context leakage.

---

## 7. Verification & Automated Test Suites

The observability hardening implementation is verified by dedicated automated test suites in `backend/src/test/java/in/gov/sih/sih26135/actuator/`:

1. **`ActuatorSecurityAndHealthTest.java`**:
   - `testPublicHealthEndpointIsAccessibleWithoutAuth()`: Asserts anonymous `/actuator/health` returns 200 UP without leaking components or database credentials.
   - `testPublicLivenessProbeIsAccessible()` & `testPublicReadinessProbeIsAccessible()`: Asserts probe paths are accessible and healthy.
   - `testPrometheusEndpointRequiresAuthentication()`: Verifies anonymous access to `/actuator/prometheus` returns HTTP 401.
   - `testPrometheusEndpointForbiddenForNonAdminUser()`: Verifies non-admin role returns HTTP 403.
   - `testPrometheusEndpointAccessibleForAdminUser()`: Verifies admin role returns HTTP 200 with Prometheus metrics text.
   - `testHealthDetailsAccessibleForAdminUser()`: Verifies admin role receives full component details.
   - `testDiagnosticsGroupAccessibleForAdminUser()`: Verifies access to custom diagnostic health group.
   - `testSensitiveActuatorEndpointsAreExcluded()`: Asserts `env`, `beans`, `heapdump`, `configprops`, and `shutdown` return HTTP 404.
   - `testDatabaseLatencyHealthIndicator()`, `testCaffeineCacheHealthIndicator()`, `testBulkImportStorageHealthIndicator()`, `testCommunicationGatewayHealthIndicator()`: Validates diagnostic outputs.

2. **`CustomObservabilityMetricsTest.java`**:
   - `testWorkflowMetricsAspectRecordsSuccess()`: Verifies `workflow.transition.duration` timer recording on successful transitions.
   - `testWorkflowMetricsAspectRecordsFailureAndRethrows()`: Verifies failure metric recording and transaction preservation on exceptions.
   - `testImportMetricsRecorder()`: Verifies counter increments for `bulk.import.rows.processed` with bounded entity and outcome tags.
   - `testDataQualityCleanlinessIndexGauge()`: Verifies O(1) in-memory gauge updating and boundary clamping ([0.0, 100.0]).
   - `testCommunicationMetricsRecorder()`: Verifies counter increments across channels and outcomes.
   - `testZeroPiiInApplicationMetrics()`: Exhaustively asserts that no metric tag names or tag values contain citizen PII.

To execute the test suites:
```bash
mvn -f backend/pom.xml test "-Dtest=ActuatorSecurityAndHealthTest,CustomObservabilityMetricsTest,DocumentationAccessControlTest"
```
