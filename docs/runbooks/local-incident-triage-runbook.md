# SIH-26135 Local Incident Triage & Troubleshooting Runbook

**Document**: Local Incident Triage & Diagnostic Runbook  
**Target Environment**: Windows 11 / Host Workstation (Local-PC Execution)  
**Applicability**: Backend Engineers, Support Evaluators, System Administrators  
**Last Verified**: November 2026 (Phase 29.14)

---

## 1. Executive Summary & Diagnostic Philosophy

This runbook establishes structured triage workflows for diagnosing and resolving local operational anomalies using the native Spring Boot 3.4.10 Actuator, Micrometer Prometheus metrics, and structured logging capabilities hardened in Phase 29.11.

### Diagnostic Golden Rules:
1. **Never Disable Security to Troubleshoot**: Never bypass JWT authentication, remove tenant filters, or permit unauthenticated administrative access.
2. **Never Send Real Messages to Test Gateways**: Keep `communication.mode=MOCK` active during local troubleshooting. Never incur third-party SMS/WhatsApp/Email costs to verify gateway logic.
3. **Diagnose Pool Exhaustion Root Causes**: Do not arbitrarily raise HikariCP connection limits without identifying long-running transactions, slow queries, or unclosed result sets.
4. **Use Structured Diagnostics First**: Leverage `/actuator/health/diagnostics` and `/actuator/prometheus` before reading raw logs.

---

## 2. Incident Triage Matrix

```
                               ┌─────────────────────────────────────────┐
                               │           ANOMALY DETECTED              │
                               └────────────────────┬────────────────────┘
                                                    │
                 ┌──────────────────────────────────┴──────────────────────────────────┐
                 │                                                                     │
        [Process Down / Crashed]                                              [Process UP / Errors]
                 │                                                                     │
       ┌─────────┴─────────┐                                                 ┌─────────┴─────────┐
       │ Section 3:        │                                                 │ Query Actuator:   │
       │ Startup Failures  │                                                 │ /actuator/health  │
       └───────────────────┘                                                 └─────────┬─────────┘
                                                                                       │
                      ┌─────────────────────────────────┬──────────────────────────────┴──────────────────────────────┐
                      │                                 │                                                             │
             [Status: DOWN / Latency]          [Import / DQ Failures]                                        [Auth / 401 / 403]
                      │                                 │                                                             │
            ┌─────────┴─────────┐             ┌─────────┴─────────┐                                         ┌─────────┴─────────┐
            │ Section 5:        │             │ Section 7:        │                                         │ Section 10:       │
            │ Hikari & DB       │             │ Ingestion & DQ    │                                         │ Security & Access │
            └───────────────────┘             └───────────────────┘                                         └───────────────────┘
```

---

## 3. Incident A: Application Startup Failures

### 3.1 Port Conflict (`Address already in use: bind`)
- **Symptom**: `Web server failed to start. Port 8080 was already in use.`
- **Triage**:
  ```powershell
  # Locate the PID occupying port 8080
  netstat -ano | findstr :8080
  # Example output: TCP 0.0.0.0:8080 ... LISTENING 14220
  ```
- **Remediation**:
  ```powershell
  # Terminate the lingering process
  Stop-Process -Id 14220 -Force
  ```

### 3.2 Missing or Weak JWT Secret
- **Symptom**: `IllegalStateException: JWT secret is not configured. Environment variable JWT_SECRET must be provided with at least 32 bytes (256 bits)`
- **Triage**: Inspect `$env:JWT_SECRET` in the active shell session.
- **Remediation**: Set a cryptographically secure 256-bit secret (minimum 32 UTF-8 characters) by prompting interactively or generating via PowerShell:
  ```powershell
  # Prompt interactively:
  $env:JWT_SECRET = Read-Host "Enter JWT Secret (minimum 32 characters / 256 bits)"
  # Or generate a cryptographically random 256-bit secret for local session:
  # $env:JWT_SECRET = [System.Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
  ```

### 3.3 Database Connectivity Failure
- **Symptom**: `Communications link failure` or `CannotCreateTransactionException`.
- **Triage**: Check whether the local MySQL 8.0 service is active:
  ```powershell
  Get-Service -Name "MySQL80"
  ```
- **Remediation**: Start the service if stopped:
  ```powershell
  Start-Service -Name "MySQL80"
  ```

### 3.4 Schema Validation Failure
- **Symptom**: `Schema-validation: missing table [trainees]` or `missing column`.
- **Triage**: The application enforces `spring.jpa.hibernate.ddl-auto=validate`. The target database schema lacks tables.
- **Remediation**: Run database initialization scripts in `database/` against `SIH26135` or point to `SIH26135_TEST`.

---

## 4. Incident B: JVM Memory Pressure & Garbage Collection

### 4.1 Diagnostic Metrics Inspection
Query memory metrics via Prometheus exposition (requires Bearer token with `system.manage` or `ROLE_ADMIN`):

```powershell
curl -H "Authorization: Bearer $token" http://localhost:8080/actuator/prometheus | findstr "jvm_memory"
```

Key gauges to monitor:
- `jvm_memory_used_bytes{area="heap"}`
- `jvm_memory_max_bytes{area="heap"}`
- `jvm_gc_pause_seconds_count`
- `jvm_gc_pause_seconds_sum`

### 4.2 Out-of-Memory Symptoms & Remediation
- **Symptom**: `java.lang.OutOfMemoryError: Java heap space` during large analytics aggregation or batch ingestion.
- **Remediation**:
  1. Increase JVM maximum heap allocation when launching:
     ```powershell
     java -Xms1024m -Xmx2048m -jar backend/target/sih26135-backend-0.1.0-SNAPSHOT.jar
     ```
  2. Verify that bulk ingestion batch chunk size does not exceed limits ([application.properties](file:///e:/project/backend/src/main/resources/application.properties#L69-L71)):
     `import-pipeline.chunk-size=100`, `import-pipeline.max-rows-per-batch=10000`.

---

## 5. Incident C: HikariCP Connection Pool Exhaustion

### 5.1 Diagnostic Gauges
Check HikariCP connection pool health:

```powershell
curl -H "Authorization: Bearer $token" http://localhost:8080/actuator/prometheus | findstr "hikaricp"
```

| Metric Name | Normal Value | Alert / Warning Condition | Root Cause Analysis |
|:---|:---:|:---:|:---|
| `hikaricp_connections_active` | 1–3 | 10 (Max Pool Size) | All pool connections busy. |
| `hikaricp_connections_pending` | 0 | > 0 | Threads blocked waiting for an available DB connection. |
| `hikaricp_connections_timeout_total` | 0 | Increasing | Requests failing with `ConnectionTimeoutException`. |
| `hikaricp_connections_idle` | 7–9 | 0 | Zero idle connections remaining. |

### 5.2 Root Cause Remediation
> [!WARNING]
> Do NOT arbitrarily increase pool size beyond 10 without investigating slow queries. Raising pool size merely shifts contention to the MySQL engine.

1. **Inspect Active MySQL Queries**:
   Connect using your configured login-path or prompt securely for credentials without echoing to shell history:
   ```powershell
   # Prompt securely or use login path
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "SHOW FULL PROCESSLIST;"
   ```
2. **Identify Long-Running Transactions**: Look for queries in `Sending data` or `Locked` state for > 5 seconds.
3. **Verify Open-in-View**: Ensure `spring.jpa.open-in-view=false` ([application.properties:12](file:///e:/project/backend/src/main/resources/application.properties#L12)) to prevent connections being held open across the entire MVC view render cycle.

---

## 6. Incident D: Database Latency Degradation

### 6.1 Diagnostic Inspection
The application features a dedicated `DatabaseLatencyHealthIndicator`:

```powershell
curl -H "Authorization: Bearer $token" http://localhost:8080/actuator/health/diagnostics
```

Response snippet:
```json
{
  "status": "UP",
  "components": {
    "databaseLatency": {
      "status": "UP",
      "details": {
        "latencyMs": 4.12,
        "thresholdMs": 500
      }
    }
  }
}
```

### 6.2 Health Semantics & Thresholds
- **Threshold**: Configured via `management.health.database.latency-threshold-ms=500` ([line 50](file:///e:/project/backend/src/main/resources/application.properties#L50)).
- **Semantics**: If ping latency exceeds 500ms, the `databaseLatency` component changes to `DOWN`. Readiness probes (`/actuator/health/readiness`) report DOWN, signaling that the service should not receive heavy traffic until DB latency recovers.

---

## 7. Incident E: Bulk Import & Ingestion Failures

### 7.1 Diagnostic Gauges & Storage Space
Inspect storage health and import counters:

```powershell
curl -H "Authorization: Bearer $token" http://localhost:8080/actuator/prometheus | findstr "bulk_import"
```

- `bulk_import_rows_processed_total{entityType="TRAINEE",outcome="FAILURE"}`
- `bulk_import_duration_seconds_count`
- Component: `/actuator/health/diagnostics` -> `bulkImportStorage` (Threshold: 1 GB free space required).

### 7.2 Common Bulk Import Root Causes
1. **Low Disk Space**: If the temporary upload volume has < 1 GB free, `BulkImportStorageHealthIndicator` reports DOWN, rejecting new multipart uploads.
2. **Row Byte Overflows**: Maximum permitted row size is 64 KB (`import-pipeline.max-row-bytes=65536`). Oversized rows trigger quarantine.
3. **Chunk Batch Errors**: Check the import batch status via `GET /api/v1/import-batches/{id}` to review error summaries and quarantined row logs.

---

## 8. Incident F: Data Quality Anomalies

### 8.1 Data Quality Index Metrics
- Gauge: `data_quality_cleanliness_index` (Normalized scale: 0.0 to 100.0).
- Spike Detection Thresholds ([application.properties:77-80](file:///e:/project/backend/src/main/resources/application.properties#L77-L80)):
  - New Issues Spike: > 20 issues within 5 minutes.
  - High Severity Spike: > 10 issues.
  - Critical Severity Spike: >= 1 issue (triggers operational alert).

### 8.2 Triage Workflow
1. Query current data quality metrics via `/actuator/prometheus`.
2. Inspect open issues via `GET /api/v1/data-quality/issues?status=OPEN`.
3. Filter by severity (`CRITICAL`, `HIGH`) to identify schema inconsistencies, invalid pincodes, or orphaned foreign keys.

---

## 9. Incident G: Communication Gateway Failures

### 9.1 Mode Verification
Ensure local testing uses mock mode:
- Configuration: `communication.mode=MOCK` ([application.properties:83](file:///e:/project/backend/src/main/resources/application.properties#L83)).
- In mock mode, alerts succeed deterministically and write to `communication_logs`.
- If set to `LIVE` without Twilio/SendGrid credentials, dispatches will fail with `GATEWAY_DISPATCH_FAILED`.

### 9.2 DPDP Consent Restrictions
- **Symptom**: `CommunicationConsentException: Dispatch denied: Trainee has not granted active consent for channel SMS`.
- **Triage**: The recipient trainee does not have an active `REF_CONSENT_STATUS = 'GRANTED'` record for the target communication channel.
- **Remediation**: Check `trainee_consents` table for the trainee ID. Do not bypass consent checks.

---

## 10. Incident H: Authentication & Authorization Failures

| HTTP Status | Error Code | Root Cause | Resolution |
|:---:|:---|:---|:---|
| **401 Unauthorized** | `TOKEN_EXPIRED` | Bearer access token expired (15m lifetime). | Call `POST /api/v1/auth/refresh` using refresh token. |
| **401 Unauthorized** | `INVALID_CREDENTIALS` | Incorrect username or password. | Re-enter credentials. Note: 5 consecutive failures triggers 15m lockout. |
| **403 Forbidden** | `ACCOUNT_LOCKED` | Account locked due to brute-force protection. | Wait 15 minutes or reset failed count in DB (`failed_login_count=0, locked_until=NULL`). |
| **403 Forbidden** | `ACCESS_DENIED` | Token lacks required role (e.g. requires `ROLE_ADMIN`). | Authenticate as an authorized administrative user. |
| **403 Forbidden** | `TENANT_ACCESS_DENIED` | User attempted to access records belonging to another state or institution. | Verify `TenantContext` claims in the JWT token. |
| **429 Too Many Requests** | `RATE_LIMIT_EXCEEDED` | IP exceeded 5 requests/minute on `/api/v1/auth/login`. | Back off request frequency. |

---

## 11. Incident I: Correlation IDs & Structured JSON Logging

### 11.1 Locating Related Log Entries
Every incoming HTTP request generates or propagates correlation headers:
- Header: `X-Correlation-Id`
- Header: `X-Request-Id`

MDC (Mapped Diagnostic Context) attaches these tokens to every log line produced during that request thread.
To trace all events associated with a failed transaction:
```powershell
Get-Content -Path "backend\logs\application.log" | Select-String "corr-abc123-xyz"
```

### 11.2 Enabling Structured JSON Logs
To stream machine-parseable JSON logs (ECS or Logstash format) without code changes:

```powershell
# Set structured logging format before launching
$env:LOGGING_STRUCTURED_FORMAT = "ecs"

# Launch Spring Boot
java -jar backend/target/sih26135-backend-0.1.0-SNAPSHOT.jar
```

Output log entries will render with JSON fields:
```json
{"@timestamp":"2026-11-21T15:30:00.123Z","log.level":"INFO","message":"Request completed","correlationId":"corr-abc123-xyz","service.name":"sih26135-backend"}
```
*(To return to human-readable console logs, unset `$env:LOGGING_STRUCTURED_FORMAT = ""`)*.
