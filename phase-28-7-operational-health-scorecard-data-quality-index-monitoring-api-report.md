# Phase 28.7 — Operational Health Scorecard, Data Quality Index & Monitoring API Implementation Report

**Phase Status:** Implementation & Verification Complete  
**Target Branch:** `main`  
**Backend Root:** `E:\project\backend`  
**Verification Database:** `SIH26135_TEST` (`spring.jpa.hibernate.ddl-auto=validate`)  
**Final Verdict:** **READY FOR INDEPENDENT REVIEW**

---

## 1. Executive Summary

Phase 28.7 implements the read-only operational health analytics, Data Quality Index (DQI), bulk ingestion metrics, and system health monitoring REST API in `E:\project\backend` under `OperationalDashboardController` (`/api/v1/operational`). The implementation aggregates existing MySQL 8 operational and core domain tables strictly inside the frozen database schema, enforces tenant isolation and role-based access control at both the controller/service boundary and inside every SQL aggregation predicate, guarantees deterministic decimal precision using `BigDecimal` with `RoundingMode.HALF_UP` (scale `2`), and eliminates division-by-zero and double-counting anomalies.

**Key Verification Outcomes:**
- **3 New Read-Only Operational Endpoints:**
  - `GET /api/v1/operational/data-quality-scorecard`
  - `GET /api/v1/operational/import-metrics`
  - `GET /api/v1/operational/system-health`
- **Focused Phase 28.7 & Contract Test Run:** `60` tests run (`OperationalDashboardMathUnitTest`, `OperationalDashboardIntegrationTest`, `ResponseEnvelopeAlignmentContractTest`, `ControllerPathQueryHeaderValidationTest`), `0` failures, `0` errors, `0` skipped (`Total time: 01:42 min`, `BUILD SUCCESS`).
- **Full Backend Regression Run:** `1,959` tests run (`+10` net new tests over the `1,949`-test Phase 28.6 baseline), `0` failures, `0` errors, `0` skipped (`Total time: 06:17 min`, `BUILD SUCCESS`) against `SIH26135_TEST`.
- **Architecture & Schema Invariants Preserved:** `137` Spring Data JPA repository interfaces (`Found 137 JPA repository interfaces.`), `2` `@Query` annotations, `64` `@RestController` classes, `668` contract-audited Spring endpoint mappings, `spring.jpa.hibernate.ddl-auto=validate`, zero DDL changes, zero production `SIH26135` access, and clean Git diff (`git diff --check` exit `0`).

---

## 2. Existing Schema and Analytics Inspection

Before implementation, existing operational tables, reference catalogs, security validators, math utilities, and Phase 28.1 baseline contracts were inspected:

1. **Frozen Operational Schema (`database/phase-15-audit-data-quality-logs.sql` & `database/phase-07-training-enrollment-attendance.sql`):**
   - **Data Quality:** `data_quality_rules`, `data_quality_issues` (`entity_type`, `entity_id`, `data_quality_rule_id`, `data_quality_issue_status_id`, `data_quality_severity_id`, `detected_at`, `resolved_at`), `data_quality_issue_events` (`changed_at`, `data_quality_issue_status_id`), `ref_data_quality_severity` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), and `ref_data_quality_issue_status` (`is_open_flag = 1` for `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `REOPENED`; `is_open_flag = 0` for `RESOLVED`, `DISMISSED`).
   - **Bulk Ingestion:** `import_batches` (`initiated_by_user_id`, `import_batch_status_id`, `started_at`, `completed_at`, `created_at`), `import_records` (`import_batch_id`, `import_record_status_id`), `ref_import_batch_status` (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`), and `ref_import_record_status` (`SUCCESS`, `FAILED`, `SKIPPED`, `DUPLICATE`).
   - **System Events & Audit Trail:** `system_event_logs` (`actor_user_id`, `entity_type`, `entity_id`, `event_summary`, `occurred_at`, `system_event_category_id`, `system_event_severity_id`), `ref_system_event_category` (`SECURITY`, `INTEGRATION`, `BATCH_JOB`, `CONFIGURATION`, `ACCESS`), `ref_system_event_severity` (`INFO`, `WARNING`, `ERROR`, `CRITICAL`), `audit_logs` (`actor_user_id`, `entity_type`, `entity_id`, `audit_action_id`, `occurred_at`), and `ref_audit_action` (`CREATE`, `UPDATE`, `DELETE`, `SOFT_DELETE`, `RESTORE`, `STATUS_CHANGE`, `EXPORT`).
   - **Operational Domain Entities:** `trainees` (`deleted_at`), `placement_records` (`deleted_at`), `employment_records` (`deleted_at`), `attendance_records` (timestamped via `created_at`, no `deleted_at` column), and `training_enrollments` (`deleted_at`).
2. **Existing Analytics & Phase 28.1 Contracts:**
   - Inspected `AnalyticsCalculationUtils`, `AnalyticsFilterCriteria`, `AnalyticsQueryFilterBuilder`, `AnalyticsPrivacySuppressionService`, `WorkflowSecurityScopeValidator`, and the Phase 28.1 architecture inventory (`docs/phases/phase-28/phase-28-1-operational-baseline-audit-architecture-inventory-dto-specifications-report.md`).
   - Confirmed Phase 27 statistical disclosure control (`k=5`) applies to person-level demographic breakdowns, whereas Phase 28.7 operational scorecards compute operational health counts under strict administrative/organizational RBAC with zero PII exposure.
   - Confirmed Phase 28.1 DTO inventory (`DataQualityHealthScorecardResponse`, `BulkImportSummaryResponse`, `OperationalDiagnosticsSummaryResponse`) was left intact without breaking changes while introducing dedicated, strongly typed Phase 28.7 dashboard response records (`OperationalDataQualityScorecardResponse`, `OperationalImportMetricsResponse`, `OperationalSystemHealthResponse`).

---

## 3. Controller / Service / Repository Architecture

Phase 28.7 follows the platform's layered architecture and preserves Spring Data JPA repository invariants:

- **Controller:** `in.gov.sih.sih26135.controller.OperationalDashboardController`
  - Annotated with `@Validated`, `@RestController`, `@RequestMapping("/api/v1/operational")`, and class-level `@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage', 'analytics.read', 'system.event.read', 'system.event.manage')")`.
  - Exposes three `GET` endpoints returning `ResponseEntity<ApiResponse<...>>`:
    1. `GET /api/v1/operational/data-quality-scorecard` -> `OperationalDataQualityScorecardResponse`
    2. `GET /api/v1/operational/import-metrics` -> `OperationalImportMetricsResponse`
    3. `GET /api/v1/operational/system-health` -> `OperationalSystemHealthResponse`
  - Accepts optional query parameters `startDate`, `endDate`, `stateId`, `districtId`, `trainingProviderId` (with `@Positive` on all `Long` ID parameters) and rejects any unknown query parameter with HTTP `400` (`UNSUPPORTED_PARAMETER`).
- **Filter Contract:** `in.gov.sih.sih26135.dto.request.OperationalDashboardFilter`
  - Validates positive IDs and date/date-time window parameters via `parseAndValidate(...)`, enforcing shared half-open `[windowStart, windowEndExclusive)` semantics.
- **Service Interface & Implementation:** `OperationalDashboardService` and `OperationalDashboardServiceImpl` (`@Service`, `@Transactional(readOnly = true)`)
  - Resolves and validates caller identity, permissions, persona role, and tenant scope before executing any aggregation query.
  - Combines repository aggregates with `OperationalMetricsMath` and constructs immutable response records.
- **Query Repository:** `in.gov.sih.sih26135.repository.OperationalDashboardQueryRepository` (`@Repository`)
  - Uses `NamedParameterJdbcTemplate` exclusively (not a Spring Data JPA repository interface and zero `@Query` annotations), executing parameterized SQL aggregations directly in MySQL 8 with tenant predicates embedded in every query.

---

## 4. Exact DQI Formula and Denominator

The **Data Quality Index** (`dataCleanlinessIndexPct`) measures the proportion of eligible in-scope operational entities without any active unresolved data quality issue:

$$\text{dataCleanlinessIndexPct} = \text{round}_{\text{HALF\_UP}, 2}\!\left(\frac{\text{eligiblePopulation} - \min(\text{eligiblePopulation}, \text{distinctAffectedEntities})}{\text{eligiblePopulation}} \times 100\right)$$

- **Zero / Empty Population Handling:** If `eligiblePopulation <= 0` (or `null`), `dataCleanlinessIndexPct` returns `null` rather than dividing by zero or reporting an artificial `100.00%` score; `cleanEntitiesEstimate` returns `0`.
- **Eligible Denominator (`eligiblePopulation`):** Sum of `COUNT(DISTINCT id)` across each authorized domain cohort (`aggregateDomainCleanliness`), excluding soft-deleted rows where `deleted_at` exists:
  - **All non-employer scopes** (`Nationwide`, `State`, `District`, `Training Provider`): 5 domains — `Trainee`, `PlacementRecord`, `EmploymentRecord`, `AttendanceRecord`, `BatchEnrollment` (`training_enrollments`).
  - **Employer scope:** Strictly restricted to the 2 employer-owned domains — `PlacementRecord` and `EmploymentRecord` (trainees, attendance, and batch enrollments are omitted from both numerator and denominator for employer callers).
- **Affected Numerator (`distinctAffectedEntities`):** For each domain branch, counts `COUNT(DISTINCT entity_id)` joined to active unresolved issues (`ref_data_quality_issue_status.is_open_flag = 1`) on that non-deleted entity table. An entity with multiple open issues across multiple rules is counted **at most once** within its domain, and domain primary keys are disjoint per branch, guaranteeing zero double-counting and bounding `0 <= distinctAffectedEntities <= eligiblePopulation` (and `0.00 <= dataCleanlinessIndexPct <= 100.00`).
- **Current Cohort Semantics (Date-Filter Safety):** `aggregateDomainCleanliness(scope)` evaluates strictly against the **current** non-deleted entity cohort and **all currently active unresolved issues (`st.is_open_flag = 1`)**, regardless of whether `dqi.detected_at` preceded `windowStart` (`cleanlinessScope = "CURRENT_COHORT_ACTIVE_ISSUES"`). Historical as-of snapshots are never fabricated, and entities with pre-window unresolved issues are never misclassified as clean when querying a filtered activity window.
- **Period-Filtered Activity Distinction:** `periodMetricsScope` (`"WINDOWED_ISSUE_ACTIVITY"` vs `"ALL_CURRENT_ISSUE_ACTIVITY"`) and `topRulesRankingBasis` (`"ACTIVE_UNRESOLVED_DETECTED_IN_WINDOW"` vs `"ACTIVE_UNRESOLVED_ISSUES"`) explicitly distinguish current DQI cleanliness from newly detected active issues, top failing rules, and resolutions occurring inside `[windowStart, windowEndExclusive)`.
- **Per-Domain Breakdown & Transparency Fields:** `domainBreakdown` returns `{entityType, eligibleCount, distinctAffectedCount, activeIssueCount, cleanlinessIndexPct}` per current entity cohort; `cleanlinessIndexFormula` documents `"(eligiblePopulation - distinctAffectedEntities) / eligiblePopulation * 100"`; `cleanEntitiesEstimate` reports `eligiblePopulation - min(eligiblePopulation, distinctAffectedEntities)`; `unruledActiveIssues` reports active issues where `data_quality_rule_id IS NULL`.

---

## 5. Active Issue Classification

Active data quality issues are classified in `aggregateActiveIssuesByStatusAndSeverity`:
- **Active Predicate:** Joins `data_quality_issues dqi` to `ref_data_quality_issue_status st` and filters strictly on `st.is_open_flag = 1`. Consequently:
  - **Included (`activeIssuesByStatus`):** `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `REOPENED` (initialized in deterministic key order with `0L` defaults).
  - **Excluded:** `RESOLVED` and `DISMISSED` (`is_open_flag = 0`).
- **Severity Breakdown (`activeIssuesBySeverity`):** Groups active issues (`st.is_open_flag = 1`) by `sev.severity_code` across `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` (all initialized to `0L`).
- **No Event Inflation:** Counts query `data_quality_issues` directly rather than joining `data_quality_issue_events`, preventing audit history events from inflating active issue counts.
- **Totals:** `totalActiveIssues` equals the sum of counts across all active statuses, matching the sum across all active severities.

---

## 6. Top Failing Rule Definition

`findTopFailingRules` ranks active unresolved data quality rules (`st.is_open_flag = 1 AND r.data_quality_rule_id IS NOT NULL`) under the caller's effective tenant scope:
- **Metrics per Rule:** `ruleId` (`data_quality_rule_id`), `ruleCode`, `ruleName`, `targetEntityType`, `activeIssueCount` (`COUNT(dqi.data_quality_issue_id)`), and `distinctAffectedEntities` (`COUNT(DISTINCT CONCAT(UPPER(dqi.entity_type), ':', dqi.entity_id))`).
- **Exclusion of Unruled Issues:** Manual/import issues with `data_quality_rule_id IS NULL` are excluded from `topFailingRules` and counted separately in `unruledActiveIssues`.
- **Deterministic Ordering & Bound:** Ordered by `active_issue_count DESC, distinct_affected_entities DESC, r.rule_code ASC, r.data_quality_rule_id ASC` with `LIMIT 10` (`TOP_FAILING_RULES_LIMIT = 10`).
- **Ranking Basis Disclosure:** `topRulesRankingBasis` explicitly reports `"ACTIVE_UNRESOLVED_DETECTED_IN_WINDOW"` when a date filter is supplied or `"ACTIVE_UNRESOLVED_ISSUES"` otherwise.

---

## 7. Resolution Time Computation

`aggregateResolutionDuration` computes mean remediation latency strictly over resolved issues:
- **Eligibility Filter:** `UPPER(st.status_code) = 'RESOLVED' AND dqi.detected_at IS NOT NULL AND dqi.resolved_at IS NOT NULL`, excluding `DISMISSED`, `REOPENED`, open statuses, and invalid timestamps where resolution precedes detection.
- **Effective Resolution Timestamp:** `COALESCE(MAX(resolved_event.changed_at), dqi.resolved_at)` where `resolved_event` scans `data_quality_issue_events` entries whose status is `RESOLVED`. Because `MAX(ev.changed_at)` is pre-aggregated in a grouped derived subquery (`GROUP BY ev.data_quality_issue_id`), multiple historical transitions never duplicate issue rows.
- **Precision & Formula:** Sums exact elapsed duration in microseconds inside SQL (`TIMESTAMPDIFF(MICROSECOND, dqi.detected_at, effective_resolved_at)`), converts total microseconds to exact decimal seconds (`totalMicros / 1,000,000`), and performs a single `BigDecimal` division via `OperationalMetricsMath.averageHoursFromSeconds`:
  $$\text{averageResolutionTimeHours} = \text{round}_{\text{HALF\_UP}, 2}\!\left(\frac{\text{totalSeconds}}{\text{resolvedIssueCount} \times 3600}\right)$$
- **Zero-Resolved Guard & Reopened Visibility:** When `resolvedIssueCount == 0`, `averageResolutionTimeHours` returns `null` (with `resolutionTimeUnit = "HOURS"`). `reopenedIssueCount` is reported separately (`UPPER(st.status_code) = 'REOPENED'`).

---

## 8. Import Throughput Definition

Import monitoring (`GET /api/v1/operational/import-metrics`) strictly separates **creation-cohort counts/ratios** (Section 9–10, anchored to `import_batches.created_at`) from **operational completion and processing throughput metrics** (anchored to actual completion/processing timestamps inside `[windowStart, windowEndExclusive)`):

1. **Completed Batches in Window & Calendar Completion Rate (`completedBatchesInWindow`, `batchesPerWindowHour`):**
   - Counts batches matching `UPPER(rbs.status_code) = 'COMPLETED' AND ib.completed_at IS NOT NULL AND ib.completed_at >= :winStart AND ib.completed_at < :winEnd` under caller tenant scope (regardless of whether the batch was created in a prior window; batches created in window but completed after `winEnd` are excluded).
   - `batchesPerWindowHour = OperationalMetricsMath.ratePerHour(completedBatchesInWindow, windowSeconds)`
2. **Successful Rows Processed in Window & Window Row Rate (`successfulRowsProcessedInWindow`, `successfulRowsPerWindowHour`):**
   - Counts committed `import_records` matching `UPPER(rrs.status_code) = 'SUCCESS' AND ir.created_at >= :winStart AND ir.created_at < :winEnd` joined to parent `ib` tenant scope. Because `ImportBatchProcessingWorkflowImpl` persists each row's authoritative `import_records.created_at` when that chunk commits, rows processed today under an overnight or recovered batch are attributed to today's window even when `import_batches.created_at` was yesterday.
   - `successfulRowsPerWindowHour = OperationalMetricsMath.ratePerHour(successfulRowsProcessedInWindow, windowSeconds)`
3. **Execution-Time Throughput (`timedCompletedBatchesCount`, `timedCompletedRecordsCount`, `processedRowsPerExecutionHour`, unit `"ROWS_PER_HOUR"`):**
   - **Eligible Timed Batch Cohort:** Batches matching `UPPER(rbs.status_code) = 'COMPLETED' AND ib.completed_at IS NOT NULL AND ib.completed_at >= :winStart AND ib.completed_at < :winEnd AND ib.started_at IS NOT NULL AND ib.completed_at > ib.started_at` under tenant scope.
   - **Denominator (`totalExecutionSeconds`):** `SUM(TIMESTAMPDIFF(MICROSECOND, ib.started_at, ib.completed_at))` converted cleanly to exact decimal seconds via `OperationalMetricsMath.microsecondsToExactSeconds(...)`.
   - **Compatible Numerator (`timedCompletedRecordsCount`):** `COUNT(ir.import_record_id)` across all committed `import_records` belonging to that exact timed `COMPLETED` batch cohort, ensuring numerator and denominator cover identical processing populations.
   - Formula:
     $$\text{processedRowsPerExecutionHour} = \text{round}_{\text{HALF\_UP}, 2}\!\left(\frac{\text{timedCompletedRecordsCount} \times 3600}{\text{totalExecutionSeconds}}\right)$$
   - When `timedCompletedBatchesCount == 0` or `totalExecutionSeconds <= 0`, `processedRowsPerExecutionHour` safely returns `null`.

---

## 9. Row Outcome Ratio Formulas

Committed row outcomes from `import_records` (for `import_batches` whose `created_at` falls within `[windowStart, windowEndExclusive)` under tenant scope) are grouped by `ref_import_record_status.status_code` into `successRecordCount` (`SUCCESS`), `failedRecordCount` (`FAILED`), `duplicateRecordCount` (`DUPLICATE`), and `skippedRecordCount` (`SKIPPED`), with `totalProcessedRecords` equal to their sum:
- `successRatioPct = OperationalMetricsMath.percentage(successRecordCount, totalProcessedRecords)`
- `failureRatioPct = OperationalMetricsMath.percentage(failedRecordCount, totalProcessedRecords)`
- `duplicateRatioPct = OperationalMetricsMath.percentage(duplicateRecordCount, totalProcessedRecords)`
- `skippedRatioPct = OperationalMetricsMath.percentage(skippedRecordCount, totalProcessedRecords)`

Each percentage computes `(count * 100) / totalProcessedRecords` with `RoundingMode.HALF_UP` at scale `2`, and returns `null` when `totalProcessedRecords == 0`.

---

## 10. Batch Status Classification

Batches whose `ib.created_at` lies inside `[windowStart, windowEndExclusive)` are grouped by `ref_import_batch_status.status_code` (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`), initializing all five keys to `0L`:
- `activeBatches = PENDING + RUNNING`
- `terminalBatches = COMPLETED + FAILED + CANCELLED`
- `totalBatches = activeBatches + terminalBatches`
- Individual counts (`pendingBatches`, `runningBatches`, `completedBatches`, `failedBatches`, `cancelledBatches`) and the immutable map `batchesByStatus` are returned in `OperationalImportMetricsResponse`.

---

## 11. Incident Frequency Definition

System health incidents (`aggregateSystemIncidents`) query `system_event_logs sel` joined to `ref_system_event_category` and `ref_system_event_severity` within `sel.occurred_at >= :winStart AND sel.occurred_at < :winEnd` and tenant scope:
- `totalIncidents`: Total persisted `system_event_logs` rows in scope and window.
- `incidentsBySeverity`: Counts keyed by `INFO`, `WARNING`, `ERROR`, `CRITICAL` (mirrored in `infoEventCount`, `warningEventCount`, `errorEventCount`, `criticalEventCount`).
- `incidentsByCategory`: Counts keyed by `SECURITY`, `INTEGRATION`, `BATCH_JOB`, `CONFIGURATION`, `ACCESS`.
- `correlatedIncidentsCount`: Count of persisted incidents carrying a Phase 28.6 correlation prefix (`sel.event_summary LIKE '[cid=%'`).
- `suppressionDisclosure`: Explains explicitly that incident counts reflect persisted `system_event_logs` records after Phase 28.6 deduplication and cooldown suppression.

---

## 12. Critical Error Velocity Formula

Critical and error velocities (`criticalEventsPerHour`, `errorEventsPerHour`, with `velocityUnit = "EVENTS_PER_HOUR"`) are computed separately via `OperationalMetricsMath.ratePerHour`:
$$\text{criticalEventsPerHour} = \text{round}_{\text{HALF\_UP}, 2}\!\left(\frac{\text{criticalEventCount} \times 3600}{\text{windowDurationExactSeconds}}\right)$$
$$\text{errorEventsPerHour} = \text{round}_{\text{HALF\_UP}, 2}\!\left(\frac{\text{errorEventCount} \times 3600}{\text{windowDurationExactSeconds}}\right)$$

- `CRITICAL` and `ERROR` counts are **never merged**, and `WARNING`/`INFO` events are excluded from both velocities.
- `windowDurationExactSeconds` is derived identically from `[windowStart, windowEndExclusive)` (`Duration.toNanos() / 1_000_000_000`). Zero or non-positive durations return `null`.

---

## 13. Audit Mutation Classification

Audit mutation monitoring (`aggregateAuditMutations`) inspects `audit_logs a` joined to `ref_audit_action r` over `a.occurred_at >= :winStart AND a.occurred_at < :winEnd` under tenant scope:
- **Mutation Actions Included (`auditMutationVolume` & `auditMutationsByAction`):** `CREATE`, `UPDATE`, `DELETE`, `SOFT_DELETE`, `RESTORE`, `STATUS_CHANGE`.
- **Non-Mutation Actions Excluded (`auditNonMutationCount`):** Other reference action codes (such as `EXPORT`) are accumulated separately into `auditNonMutationCount` and excluded from `auditMutationVolume` and `distinctEntitiesModified`.
- **Distinct Entities Modified (`distinctEntitiesModified`):**
  `COUNT(DISTINCT CASE WHEN a.entity_id IS NOT NULL THEN CONCAT(UPPER(a.entity_type), ':', a.entity_id) ELSE CONCAT(UPPER(a.entity_type), ':UNKEYED:', a.audit_log_id) END)` filtered strictly to the six mutation action codes.

---

## 14. `BigDecimal` Computation Utilities

`in.gov.sih.sih26135.util.OperationalMetricsMath` centralizes all operational math with `DEFAULT_SCALE = 2` and `DEFAULT_ROUNDING = RoundingMode.HALF_UP`:
- `percentage(Long numerator, Long denominator [, int scale])`: Multiplies by `100` prior to division; returns `null` if operands are `null`, `numerator < 0`, or `denominator <= 0`.
- `dataCleanlinessIndex(Long eligiblePopulation, Long distinctAffectedEntities)`: Clamps affected entities to `[0, eligiblePopulation]` and delegates `(eligible - affected) / eligible * 100`; returns `null` when `eligiblePopulation <= 0`.
- `averageHoursFromSeconds(BigDecimal totalSeconds, Long count)`: Divides `totalSeconds` by `count * 3600` (`null` if `count <= 0` or `totalSeconds < 0`).
- `ratePerHour(Long count, BigDecimal durationSeconds)`: Multiplies `count * 3600` prior to dividing by `durationSeconds` (`null` if `count < 0` or `durationSeconds <= 0`).
- `hoursFromSeconds(BigDecimal durationSeconds)` & `microsecondsToExactSeconds(BigDecimal microseconds)`: Exact microsecond-to-second conversion without premature rounding, and scale-`2` `HALF_UP` conversion to hours.
- `safeAdd(Long a, Long b)`: Null-safe `Math.addExact`.

---

## 15. Time-Window Semantics

`OperationalDashboardFilter` enforces uniform half-open `[windowStart, windowEndExclusive)` semantics across all three endpoints:
- **Date-Only Inputs (`YYYY-MM-DD`):** `windowStart = startDate.atStartOfDay()`, `windowEndExclusive = endDate.plusDays(1).atStartOfDay()`. An event at `endDate 23:59:59` is included; an event at `(endDate + 1) 00:00:00` is excluded.
- **Date-Time Inputs (`YYYY-MM-DDTHH:mm:ss`):** Exact `[startDateTime, endDateTime)`, rejecting `startDateTime >= endDateTime` with HTTP `400` (`INVALID_DATE_RANGE`).
- **Validation Rules:** Partial bounds (only `startDate` or only `endDate`), mixed formats (date + date-time), `startDate > endDate`, and malformed temporal strings are rejected with HTTP `400` (`INVALID_DATE_RANGE` or `INVALID_FILTER_PARAMETER`).
- **Default Behavior When Omitted:** Sets `[today - 29 days at 00:00:00, tomorrow at 00:00:00)` (a 30-day default window) for time-windowed import and system health metrics (`windowDurationHours = 720.00`), while `getDataQualityScorecard` sets `timeWindowApplied = false` (leaving `windowStart`/`windowEndExclusive` `null` and evaluating current lifetime active/resolved state unless an explicit window is supplied).

---

## 16. Tenant Scope and RBAC

Authorization and tenant isolation are enforced **fail-closed** at three layers:
1. **HTTP Security & Method Security:** Unauthenticated requests receive `401`. Authenticated callers must possess at least one of `'audit.read'`, `'system.manage'`, `'analytics.read'`, `'system.event.read'`, `'system.event.manage'` at both `@PreAuthorize` on `OperationalDashboardController` and inside `OperationalDashboardServiceImpl.resolveAndValidateScope(...)`. Callers lacking required authority, deleted users, unresolvable user identities, and `ROLE_trainee` callers are rejected with `403 Forbidden`.
2. **Caller Persona & Filter Validation:** Using `WorkflowSecurityScopeValidator` and `User` record inspection:
   - **Conflicting / Unscoped Callers:** Callers with missing tenant roots, multiple conflicting persona roles, conflicting `provider/employer + districtId`, or `state + provider/employer` scope mismatches are rejected fail-closed (`403`).
   - **Nationwide Admins:** May query unfiltered or slice by validated `stateId`, `districtId`, `trainingProviderId` (non-existent IDs or state/district/provider parent mismatches return `400 INVALID_FILTER_PARAMETER`).
   - **State Admins:** Constrained to `caller.getStateId()`; cross-state `stateId`, cross-state `districtId`, or cross-state `trainingProviderId` fail with `403`.
   - **District Officers:** Constrained to `caller.getStateId()` + `caller.getDistrictId()`; cross-district or cross-state filters fail with `403`.
   - **Training Providers:** Constrained strictly to `caller.getTrainingProviderId()`; cannot supply `stateId`, `districtId`, or another `trainingProviderId` (`403`). Even within the same state, Provider A cannot observe Provider B's trainees (resolved via active `training_enrollments`), attendance, placements, enrollments, import batches, or incidents.
   - **Employers:** Constrained strictly to `caller.getEmployerId()`; cannot supply geographic or provider filters (`403`); scorecard cohorts include only employer-owned `PlacementRecord` and `EmploymentRecord`.
3. **Database Query Enforcement:** Every SQL query in `OperationalDashboardQueryRepository` injects parameterized `:stateId`, `:districtId`, `:providerId`, and `:employerId` predicates directly inside `WHERE` clauses (`dqIssueScopePredicate`, `importScopePredicate`, `systemEventScopePredicate`, `auditLogScopePredicate`, and per-domain cohort filters).

---

## 17. Privacy Protection

- Operational responses expose strictly **aggregate counts, percentages, averages, rates, rule codes, entity type labels, and window timestamps**.
- Zero PII columns (names, emails, Aadhaar/PAN, phone numbers, addresses, credentials, payload snapshots, or correlation token values) are queried, projected, or serialized in any Phase 28.7 endpoint response.
- Unauthenticated and unauthorized cross-tenant callers fail closed before aggregate DTOs are constructed, preventing cross-tenant inference via error payloads or response fields.

---

## 18. Query Performance Approach

- **Single-Pass SQL Grouping:** Aggregations (`COUNT`, `COUNT(DISTINCT ...)`, `SUM(CASE ...)`, `TIMESTAMPDIFF(MICROSECOND, ...)`, `GROUP BY`) execute inside MySQL 8 rather than loading entity/event rows into JVM memory.
- **No Cartesian Multiplication:**
  - `Trainee` provider filtering uses semi-join `EXISTS (SELECT 1 FROM training_enrollments ...)` rather than multiplying multi-enrollment trainee rows.
  - `RESOLVED` event timestamps are pre-aggregated in a grouped derived table (`GROUP BY ev.data_quality_issue_id`) before joining `data_quality_issues`.
- **Bounded Outputs:** Maps have fixed enumerated keys (e.g., 4 severities, 4 active statuses, 5 batch statuses, 4 record statuses, 5 event categories, 6 audit mutation actions), `domainBreakdown` has at most 5 entries, and `topFailingRules` enforces SQL `LIMIT 10`.

---

## 19. Changed Files

**Modified Files (2 contract test baseline alignment files):**
1. `E:/project/backend/src/test/java/in/gov/sih/sih26135/validation/ControllerPathQueryHeaderValidationTest.java` — Updated audited `@RestController` count from `63` to `64` to include `OperationalDashboardController`.
2. `E:/project/backend/src/test/java/in/gov/sih/sih26135/response/ResponseEnvelopeAlignmentContractTest.java` — Updated audited `@RestController` count (`63` -> `64`) and audited Spring endpoint mapping count (`665` -> `668`).

**New Production Files (9 files):**
1. `E:/project/backend/src/main/java/in/gov/sih/sih26135/util/OperationalMetricsMath.java`
2. `E:/project/backend/src/main/java/in/gov/sih/sih26135/dto/request/OperationalDashboardFilter.java`
3. `E:/project/backend/src/main/java/in/gov/sih/sih26135/dto/response/OperationalDataQualityScorecardResponse.java`
4. `E:/project/backend/src/main/java/in/gov/sih/sih26135/dto/response/OperationalImportMetricsResponse.java`
5. `E:/project/backend/src/main/java/in/gov/sih/sih26135/dto/response/OperationalSystemHealthResponse.java`
6. `E:/project/backend/src/main/java/in/gov/sih/sih26135/repository/OperationalDashboardQueryRepository.java`
7. `E:/project/backend/src/main/java/in/gov/sih/sih26135/service/OperationalDashboardService.java`
8. `E:/project/backend/src/main/java/in/gov/sih/sih26135/service/impl/OperationalDashboardServiceImpl.java`
9. `E:/project/backend/src/main/java/in/gov/sih/sih26135/controller/OperationalDashboardController.java`

**New Test Files (2 files):**
1. `E:/project/backend/src/test/java/in/gov/sih/sih26135/util/OperationalDashboardMathUnitTest.java`
2. `E:/project/backend/src/test/java/in/gov/sih/sih26135/controller/OperationalDashboardIntegrationTest.java`

**Synchronized Documentation Reports (2 identical files):**
1. `E:/project/phase-28-7-operational-health-scorecard-data-quality-index-monitoring-api-report.md`
2. `E:/project/docs/phases/phase-28/phase-28-7-operational-health-scorecard-data-quality-index-monitoring-api-report.md`

---

## 20. Focused Tests

Executed after the targeted Phase 28.7 metric correctness edits:
`mvn -f "E:\project\backend\pom.xml" test "-Dtest=OperationalDashboardMathUnitTest,OperationalDashboardIntegrationTest,ResponseEnvelopeAlignmentContractTest,ControllerPathQueryHeaderValidationTest"`

**Result:** `Tests run: 60, Failures: 0, Errors: 0, Skipped: 0`, `Total time: 01:42 min`, `BUILD SUCCESS`

| Test Suite / Group | Tests Run | Failures | Errors | Skipped | Verified Scope |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `OperationalDashboardMathUnitTest` | 4 | 0 | 0 | 0 | `0/0`, `0/N`, `N/N`, repeating decimals, `HALF_UP` rounding, DQI clamping & empty cohort `null`, duration/velocity guards, half-open window validation |
| `OperationalDashboardIntegrationTest` (3 `@Nested` groups) | 6 | 0 | 0 | 0 | Empty/clean/multi-issue scorecard & date-filtered `100`-entity / `20`-pre-window-issue DQI (non-double-counting, resolved/dismissed exclusion, same-state provider isolation) (2); creation cohort vs `completed_at`/`ir.created_at` throughput, overnight/future-completed/recovered `RUNNING` batches, mixed ratios & cross-provider isolation (2); `CRITICAL` vs `ERROR` velocity, audit mutations vs `EXPORT`, half-open boundaries, RBAC `401`/`403`/`400`, employer scope isolation (2) |
| `ResponseEnvelopeAlignmentContractTest` (7 `@Nested` groups) | 22 | 0 | 0 | 0 | Uniform `ApiResponse`/`ErrorResponse` envelope contract across 64 `@RestController`s / 668 endpoints |
| `ControllerPathQueryHeaderValidationTest` (7 `@Nested` groups) | 28 | 0 | 0 | 0 | `@Validated` across all 64 controllers, `@Positive` path/query IDs, pagination bounds, security/header boundary checks |
| **Total Focused Suite** | **60** | **0** | **0** | **0** | **All passed cleanly against `SIH26135_TEST`** |

---

## 21. Full Regression Results

Executed after the targeted Phase 28.7 metric correctness edits against `SIH26135_TEST`:
`mvn -f "E:\project\backend\pom.xml" test`

**Observed Result:**
- **Tests run:** `1,959` (Phase 28.6 baseline `1,949` + `4` Phase 28.7 math/window unit tests + `6` Phase 28.7 end-to-end integration tests)
- **Failures:** `0`
- **Errors:** `0`
- **Skipped:** `0`
- **Total time:** `06:17 min`
- **Build status:** `BUILD SUCCESS`
- **JPA Repository Scan Output:** `Found 137 JPA repository interfaces.`

---

## 22. Database Isolation

- **Test Configuration Verified (`backend/src/test/resources/application.properties`):**
  - `spring.datasource.url=${SIH_TEST_DB_URL:jdbc:mysql://${SIH_TEST_DB_HOST:localhost}:${SIH_TEST_DB_PORT:3306}/SIH26135_TEST?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Kolkata}`
  - `spring.jpa.hibernate.ddl-auto=validate`
- `OperationalDashboardIntegrationTest` explicitly asserts `MysqlConnectionTest.assertDedicatedTestCatalog(...)` and operates solely in isolated fixture ID ranges (`991x`, `99100..99199`, `987000..987999`) cleaned up in teardown.
- Production database `SIH26135` was **never accessed or modified**.
- Zero DDL statements or schema modifications were executed or added.

---

## 23. Architectural Invariants

Directly verified across `E:\project\backend\src\main\java` and Maven test execution:

| Architectural Invariant | Verified Value | Status |
| :--- | :---: | :---: |
| Spring Data JPA Repository Interfaces (Spring Boot runtime scan) | `137` | Preserved (`Found 137 JPA repository interfaces.`) |
| Declared `@Query` Annotations in `src/main/java` | `2` (`TraineeUnemploymentEventRepository`, `UserRepository`) | Preserved |
| Application `@RestController` Classes | `64` (`63` Phase 28.6 baseline + `OperationalDashboardController`) | Verified & contract-tested |
| Audited Live Spring Web Endpoints | `668` (`665` baseline + `3` Phase 28.7 GET endpoints) | Verified (`ResponseEnvelopeAlignmentContractTest`) |
| Hibernate Schema Validation | `spring.jpa.hibernate.ddl-auto=validate` | Preserved (both main and test configs) |
| Production Database `SIH26135` Access | None | Isolated to `SIH26135_TEST` |

---

## 24. Git Hygiene

- Active branch: `main` (`git branch --show-current`).
- `git diff --check`: exit code `0` (no whitespace/conflict errors).
- `git diff --cached --check`: exit code `0`.
- `git status --short -uall`: contains only the `2` modified contract test files, `9` new Phase 28.7 main source files, `2` new Phase 28.7 test files, and the synchronized Phase 28.7 reports.
- **Zero Git mutations performed:** no `git add`, `git commit`, `git push`, `git reset`, `git checkout`, `git clean`, or `git stash` commands were executed.

---

## 25. Known Limitations

1. **System Event Suppression Awareness:** `system_event_logs` metrics summarize persisted rows; repeated rapid operational triggers consolidated by Phase 28.6 deduplication and cooldown suppression are reflected once per persisted log entry (explicitly disclosed in `suppressionDisclosure`).
2. **Legacy Pre-Phase-28.6 Event Correlation Coverage:** Only `system_event_logs` rows containing the `[cid=%` correlation prefix in `event_summary` increment `correlatedIncidentsCount`; historical events persisted prior to Phase 28.6 correlation enrichment are counted in `totalIncidents` and severity/category totals without incrementing `correlatedIncidentsCount`.
3. **Resolvable Tenant Linkage for Scoped Logs & Unmatched Issue Entities:** For non-nationwide callers, `system_event_logs` and `audit_logs` without resolvable tenant/actor linkage in `SYSTEM_EVENT_TENANT_JOINS` / `AUDIT_LOG_TENANT_JOINS` (for instance, system-wide anonymous lockouts lacking user/provider/employer/state association, or `TRAINEE` audit logs without provider-scoped actor linkage) are excluded fail-closed rather than leaked across tenants, while nationwide unfiltered queries include all records in window. Likewise, `data_quality_issues` referencing non-existent/soft-deleted domain rows or entity types outside the 5 operational domains (`Trainee`, `PlacementRecord`, `EmploymentRecord`, `AttendanceRecord`, `BatchEnrollment`) are excluded from `domainBreakdown` and scoped tenant views.
4. **Timed Completion Prerequisite for Execution Throughput:** `processedRowsPerExecutionHour` requires `import_batches` in `COMPLETED` status with `started_at != NULL`, `completed_at != NULL`, and `completed_at > started_at`; batches lacking valid timing metadata still contribute to `batchesByStatus` and row outcome ratios while reporting `null` execution throughput when `timedCompletedBatchesCount == 0`.
5. **Scorecard Current-Cohort Cleanliness vs. Period Activity Filtering & Import Cohort Split:** `dataCleanlinessIndexPct` intentionally reflects current cleanliness (`cleanlinessScope = "CURRENT_COHORT_ACTIVE_ISSUES"`) rather than a fabricated historical as-of snapshot, while `totalActiveIssues` / `topFailingRules` / `resolvedIssueCount` filter issue activity to `[windowStart, windowEndExclusive)` when a date range is provided. Similarly, in `getImportMetrics`, `batchesByStatus` and committed-row ratios remain scoped to the `import_batches.created_at` creation cohort while `completedBatchesInWindow`, `successfulRowsProcessedInWindow`, and `processedRowsPerExecutionHour` are anchored to `completed_at` and `import_records.created_at`; when dates are omitted, `getImportMetrics` and `getSystemHealth` default to a trailing 30-calendar-day window (`[today - 29d 00:00:00, tomorrow 00:00:00)`).

---

## 26. Acceptance Checklist

- [x] All three operational endpoints implemented (`GET /api/v1/operational/data-quality-scorecard`, `/import-metrics`, `/system-health`).
- [x] Existing `ApiResponse` and sanitized `ErrorResponse` conventions preserved.
- [x] DQI formula explicitly defined, disclosed (`cleanlinessIndexFormula`), and mathematically bounded (`[0.00, 100.00]`).
- [x] No double-counting of affected entities (`COUNT(DISTINCT ...)` per domain + `EXISTS` for enrollments + grouped `RESOLVED` subquery).
- [x] Active issues classified strictly by `is_open_flag = 1` (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, `REOPENED`), excluding `RESOLVED`/`DISMISSED`.
- [x] Severity grouping accurate (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
- [x] Top failing rules ranked deterministically with explicit `topRulesRankingBasis` and bounded to top `10`.
- [x] Resolution time computed in microseconds and divided once into hours with `HALF_UP` scale `2`, excluding `DISMISSED`/`REOPENED`/invalid timestamps.
- [x] Import throughput separates creation-cohort metrics (`ib.created_at`) from `completed_at` completion throughput, authoritative `import_records.created_at` row throughput, and compatible `COMPLETED` execution-time `ROWS_PER_HOUR`.
- [x] `SUCCESS`, `FAILED`, `DUPLICATE`, `SKIPPED` row counts and percentages handled accurately.
- [x] Active (`PENDING + RUNNING`) and terminal (`COMPLETED + FAILED + CANCELLED`) batch counts classified accurately.
- [x] Incident frequency (`INFO`, `WARNING`, `ERROR`, `CRITICAL` and all 5 categories) and `[cid=%` correlation tracking implemented with cooldown disclosure.
- [x] Separate `CRITICAL` and `ERROR` hourly velocity formulas implemented cleanly.
- [x] Audit mutation volume (`CREATE`, `UPDATE`, `DELETE`, `SOFT_DELETE`, `RESTORE`, `STATUS_CHANGE`), distinct entities modified, and non-mutation (`EXPORT`) separation implemented.
- [x] `BigDecimal` and `RoundingMode.HALF_UP` used consistently via `OperationalMetricsMath`.
- [x] Zero denominators, `null` aggregates, and zero-duration windows return `null` safely.
- [x] Half-open `[startInclusive, endExclusive)` date filters consistent and validated across all 3 endpoints.
- [x] Tenant isolation enforced inside every SQL aggregate query across Nationwide, State, District, Provider, and Employer scopes.
- [x] Nationwide unfiltered and filtered authorization verified, with trainee and conflicting/unscoped callers rejected `403`.
- [x] Privacy protections preserved (aggregate telemetry only, zero PII fields exposed).
- [x] SQL queries remain database-side, bounded, and non-multiplying.
- [x] Focused Phase 28.7 + contract tests pass (`60/60`).
- [x] Full backend regression suite passes (`1,959/1,959`, `0` failures, `0` errors, `0` skipped).
- [x] Frozen database schema and `ddl-auto=validate` preserved (`137` JPA repositories, `2` `@Query`).
- [x] No production DB (`SIH26135`) access.
- [x] No Git mutations performed; `git diff --check` clean.
- [x] Both Phase 28.7 report files generated and byte-synchronized (`SHA-256` match verified).

---

## 27. Final Verdict

**READY FOR INDEPENDENT REVIEW**
