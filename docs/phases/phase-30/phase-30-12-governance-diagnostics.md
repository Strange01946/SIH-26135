# Phase 30.12 Governance and diagnostics

Open **Governance and diagnostics** at `/audit`. Nationwide audit readers can browse the full trail. Scoped audit readers must supply an entity type and ID, and the backend applies its existing ownership rules.

| View | Live backend contract | Behavior |
| --- | --- | --- |
| Audit trail | `GET /api/v1/audit-logs`, `GET /api/v1/audit-logs/scope` | Filters by entity type and ID, actor, action, time range, and correlation ID. **Transition** maps to `STATUS_CHANGE`. The active view refreshes every two seconds. |
| Entity diff | `AuditLogResponse.stateChange` | Trainee profile and training batch writes record before-and-after values in the same transaction as the edit. Candidate contact and identity fields show which values changed while keeping those values redacted. Historical and other summary-only entries show the recorded summary. |
| System events | `GET /api/v1/system-event-logs` | Filters the live feed by severity, event code, and exact `X-Correlation-ID`. The detail dialog shows the recorded request ID and links across matching audit and event records. |
| Anti-storm controls | `GET /api/v1/system-event-logs/throttle-status` | Authorized operators see the configured deduplication and cooldown windows and the corresponding suppression counts since process startup. |
| Actuator | `/actuator/health`, `/actuator/health/diagnostics`, `/actuator/info`, `/actuator/prometheus` | Operators see health status, authorized component checks, available build information, and selected process metrics. The existing Actuator permissions protect diagnostic details and metrics. |

## Local verification

With Spring Boot and Vite running against `SIH26135_TEST`, open the audit trail and filter by a sample training batch. Update capacity or dates in a separate browser tab through the existing training batch API. The edit appears in the audit trail after the next refresh. Open **View diff** to compare recorded values. Use the same request correlation ID to find the associated events when the backend records an incident for that request.

The browser verification exercised cross-tab updates, pagination, ISO timestamp filters, redacted candidate changes, blocked writes to existing audit entries, correlation tracing, and authorized Actuator access.
