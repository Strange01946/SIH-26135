package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.dto.response.OperationalDataQualityScorecardResponse.DomainCleanlinessMetric;
import in.gov.sih.sih26135.dto.response.OperationalDataQualityScorecardResponse.FailingRuleMetric;
import in.gov.sih.sih26135.util.OperationalMetricsMath;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Parameterized SQL aggregation repository for Phase 28.7 operational dashboard endpoints.
 *
 * <p>Uses {@link NamedParameterJdbcTemplate} to execute database-side aggregations with strict
 * tenant isolation predicates inside every aggregate query while preserving the 137 Spring Data JPA
 * repository interfaces and 2 {@code @Query} annotation invariants.
 */
@Repository
public class OperationalDashboardQueryRepository {

  public record EffectiveDashboardScope(
      boolean nationwideUnrestricted,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      Long employerId
  ) {
    public boolean isEmployerScoped() {
      return employerId != null;
    }

    public boolean isProviderScoped() {
      return trainingProviderId != null;
    }
  }

  public record IssueSeverityAndStatusAggregate(
      Map<String, Long> byStatus,
      Map<String, Long> bySeverity,
      long unruledActiveCount
  ) {}

  public record ResolutionDurationAggregate(
      long resolvedCount,
      BigDecimal totalSeconds,
      long reopenedCount
  ) {}

  public record BatchAggregateResult(
      Map<String, Long> batchesByStatus,
      Map<String, Long> recordsByStatus,
      long completedBatchesInWindow,
      long successfulRowsProcessedInWindow,
      long timedCompletedBatches,
      long timedCompletedRecordCount,
      BigDecimal totalExecutionSeconds
  ) {}

  public record IncidentAggregateResult(
      long totalIncidents,
      Map<String, Long> bySeverity,
      Map<String, Long> byCategory,
      long correlatedIncidents
  ) {}

  public record AuditMutationAggregateResult(
      long totalMutationEvents,
      long distinctEntitiesModified,
      Map<String, Long> mutationsByAction,
      long nonMutationCount
  ) {}

  private static final int TOP_FAILING_RULES_LIMIT = 10;

  private static final String DQ_ISSUE_TENANT_JOINS =
      " LEFT JOIN trainees t ON UPPER(dqi.entity_type) = 'TRAINEE' AND dqi.entity_id = t.trainee_id AND t.deleted_at IS NULL " +
          "LEFT JOIN attendance_records ar ON UPPER(dqi.entity_type) IN ('ATTENDANCERECORD', 'ATTENDANCE_RECORD') AND dqi.entity_id = ar.attendance_id " +
          "LEFT JOIN training_enrollments ar_te ON ar.enrollment_id = ar_te.enrollment_id AND ar_te.deleted_at IS NULL " +
          "LEFT JOIN training_batches ar_tb ON ar.batch_id = ar_tb.batch_id AND ar_tb.deleted_at IS NULL " +
          "LEFT JOIN training_centers ar_tc ON ar_tb.center_id = ar_tc.center_id AND ar_tc.deleted_at IS NULL " +
          "LEFT JOIN placement_records pr ON UPPER(dqi.entity_type) IN ('PLACEMENTRECORD', 'PLACEMENT_RECORD') AND dqi.entity_id = pr.placement_id AND pr.deleted_at IS NULL " +
          "LEFT JOIN employment_records er ON UPPER(dqi.entity_type) IN ('EMPLOYMENTRECORD', 'EMPLOYMENT_RECORD') AND dqi.entity_id = er.employment_id AND er.deleted_at IS NULL " +
          "LEFT JOIN training_enrollments er_te ON er.enrollment_id = er_te.enrollment_id AND er_te.deleted_at IS NULL " +
          "LEFT JOIN training_enrollments be_te ON UPPER(dqi.entity_type) IN ('BATCHENROLLMENT', 'BATCH_ENROLLMENT', 'TRAININGENROLLMENT', 'TRAINING_ENROLLMENT') AND dqi.entity_id = be_te.enrollment_id AND be_te.deleted_at IS NULL " +
          "LEFT JOIN training_batches be_tb ON be_te.batch_id = be_tb.batch_id AND be_tb.deleted_at IS NULL " +
          "LEFT JOIN training_centers be_tc ON be_tb.center_id = be_tc.center_id AND be_tc.deleted_at IS NULL ";

  private static final String IMPORT_TENANT_JOINS =
      " LEFT JOIN users ibu ON ib.initiated_by_user_id = ibu.user_id " +
          "LEFT JOIN training_providers ibutp ON ibu.training_provider_id = ibutp.provider_id " +
          "LEFT JOIN employers ibuemp ON ibu.employer_id = ibuemp.employer_id ";

  private static final String SYSTEM_EVENT_TENANT_JOINS =
      " LEFT JOIN users au ON sel.actor_user_id = au.user_id " +
          "LEFT JOIN users eu ON UPPER(sel.entity_type) = 'USER' AND sel.entity_id = eu.user_id " +
          "LEFT JOIN import_batches ib ON UPPER(sel.entity_type) IN ('IMPORTBATCH', 'IMPORT_BATCH') AND sel.entity_id = ib.import_batch_id " +
          "LEFT JOIN users ibu ON ib.initiated_by_user_id = ibu.user_id " +
          "LEFT JOIN training_providers tp ON UPPER(sel.entity_type) IN ('TRAININGPROVIDER', 'TRAINING_PROVIDER') AND sel.entity_id = tp.provider_id " +
          "LEFT JOIN employers emp ON UPPER(sel.entity_type) = 'EMPLOYER' AND sel.entity_id = emp.employer_id " +
          "LEFT JOIN training_providers autp ON au.training_provider_id = autp.provider_id " +
          "LEFT JOIN employers auemp ON au.employer_id = auemp.employer_id " +
          "LEFT JOIN training_providers eutp ON eu.training_provider_id = eutp.provider_id " +
          "LEFT JOIN employers euemp ON eu.employer_id = euemp.employer_id " +
          "LEFT JOIN training_providers ibutp ON ibu.training_provider_id = ibutp.provider_id " +
          "LEFT JOIN employers ibuemp ON ibu.employer_id = ibuemp.employer_id ";

  private static final String AUDIT_LOG_TENANT_JOINS =
      " LEFT JOIN users au ON a.actor_user_id = au.user_id " +
          "LEFT JOIN training_providers autp ON au.training_provider_id = autp.provider_id " +
          "LEFT JOIN employers auemp ON au.employer_id = auemp.employer_id " +
          "LEFT JOIN users eu ON UPPER(a.entity_type) = 'USER' AND a.entity_id = eu.user_id " +
          "LEFT JOIN training_providers eutp ON eu.training_provider_id = eutp.provider_id " +
          "LEFT JOIN employers euemp ON eu.employer_id = euemp.employer_id " +
          "LEFT JOIN trainees atr ON UPPER(a.entity_type) IN ('TRAINEE', 'TRAINING') AND a.entity_id = atr.trainee_id " +
          "LEFT JOIN placement_records apr ON UPPER(a.entity_type) IN ('PLACEMENT', 'PLACEMENTRECORD', 'PLACEMENT_RECORD') AND a.entity_id = apr.placement_id " +
          "LEFT JOIN employment_records aer ON UPPER(a.entity_type) IN ('EMPLOYMENT', 'EMPLOYMENTRECORD', 'EMPLOYMENT_RECORD') AND a.entity_id = aer.employment_id " +
          "LEFT JOIN training_enrollments aer_te ON aer.enrollment_id = aer_te.enrollment_id " +
          "LEFT JOIN training_enrollments ate ON UPPER(a.entity_type) IN ('BATCHENROLLMENT', 'BATCH_ENROLLMENT', 'TRAININGENROLLMENT', 'TRAINING_ENROLLMENT') AND a.entity_id = ate.enrollment_id " +
          "LEFT JOIN training_batches ate_tb ON ate.batch_id = ate_tb.batch_id " +
          "LEFT JOIN training_centers ate_tc ON ate_tb.center_id = ate_tc.center_id " +
          "LEFT JOIN import_batches aib ON UPPER(a.entity_type) IN ('IMPORTBATCH', 'IMPORT_BATCH') AND a.entity_id = aib.import_batch_id " +
          "LEFT JOIN users aibu ON aib.initiated_by_user_id = aibu.user_id " +
          "LEFT JOIN training_providers aibutp ON aibu.training_provider_id = aibutp.provider_id " +
          "LEFT JOIN employers aibuemp ON aibu.employer_id = aibuemp.employer_id " +
          "LEFT JOIN training_providers atp ON UPPER(a.entity_type) IN ('TRAININGPROVIDER', 'TRAINING_PROVIDER') AND a.entity_id = atp.provider_id " +
          "LEFT JOIN employers aemp ON UPPER(a.entity_type) = 'EMPLOYER' AND a.entity_id = aemp.employer_id ";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public OperationalDashboardQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // =========================================================================
  // 1. Data Quality Scorecard Aggregations
  // =========================================================================

  /**
   * Computes current domain cleanliness across the active, non-deleted eligible population and all
   * currently active unresolved issues ({@code st.is_open_flag = 1}), regardless of when those
   * issues were originally detected. Historical as-of snapshots are never fabricated.
   */
  public List<DomainCleanlinessMetric> aggregateDomainCleanliness(EffectiveDashboardScope scope) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    bindScopeParams(params, scope);

    List<String> branches = new ArrayList<>();

    if (!scope.isEmployerScoped()) {
      branches.add(
          "SELECT 'Trainee' AS entity_type, " +
              "(SELECT COUNT(DISTINCT t.trainee_id) FROM trainees t WHERE t.deleted_at IS NULL " + traineeFilterSql(scope, "t") + ") AS eligible_count, " +
              "(SELECT COUNT(DISTINCT t.trainee_id) FROM data_quality_issues dqi " +
              " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
              " JOIN trainees t ON UPPER(dqi.entity_type) = 'TRAINEE' AND dqi.entity_id = t.trainee_id AND t.deleted_at IS NULL " +
              " WHERE st.is_open_flag = 1 " + traineeFilterSql(scope, "t") + ") AS affected_count, " +
              "(SELECT COUNT(dqi.data_quality_issue_id) FROM data_quality_issues dqi " +
              " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
              " JOIN trainees t ON UPPER(dqi.entity_type) = 'TRAINEE' AND dqi.entity_id = t.trainee_id AND t.deleted_at IS NULL " +
              " WHERE st.is_open_flag = 1 " + traineeFilterSql(scope, "t") + ") AS active_issue_count");
    }

    branches.add(
        "SELECT 'PlacementRecord' AS entity_type, " +
            "(SELECT COUNT(DISTINCT pr.placement_id) FROM placement_records pr WHERE pr.deleted_at IS NULL " + placementFilterSql(scope, "pr") + ") AS eligible_count, " +
            "(SELECT COUNT(DISTINCT pr.placement_id) FROM data_quality_issues dqi " +
            " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
            " JOIN placement_records pr ON UPPER(dqi.entity_type) IN ('PLACEMENTRECORD', 'PLACEMENT_RECORD') AND dqi.entity_id = pr.placement_id AND pr.deleted_at IS NULL " +
            " WHERE st.is_open_flag = 1 " + placementFilterSql(scope, "pr") + ") AS affected_count, " +
            "(SELECT COUNT(dqi.data_quality_issue_id) FROM data_quality_issues dqi " +
            " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
            " JOIN placement_records pr ON UPPER(dqi.entity_type) IN ('PLACEMENTRECORD', 'PLACEMENT_RECORD') AND dqi.entity_id = pr.placement_id AND pr.deleted_at IS NULL " +
            " WHERE st.is_open_flag = 1 " + placementFilterSql(scope, "pr") + ") AS active_issue_count");

    branches.add(
        "SELECT 'EmploymentRecord' AS entity_type, " +
            "(SELECT COUNT(DISTINCT er.employment_id) FROM employment_records er " +
            " LEFT JOIN training_enrollments er_te ON er.enrollment_id = er_te.enrollment_id AND er_te.deleted_at IS NULL " +
            " WHERE er.deleted_at IS NULL " + employmentFilterSql(scope, "er", "er_te") + ") AS eligible_count, " +
            "(SELECT COUNT(DISTINCT er.employment_id) FROM data_quality_issues dqi " +
            " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
            " JOIN employment_records er ON UPPER(dqi.entity_type) IN ('EMPLOYMENTRECORD', 'EMPLOYMENT_RECORD') AND dqi.entity_id = er.employment_id AND er.deleted_at IS NULL " +
            " LEFT JOIN training_enrollments er_te ON er.enrollment_id = er_te.enrollment_id AND er_te.deleted_at IS NULL " +
            " WHERE st.is_open_flag = 1 " + employmentFilterSql(scope, "er", "er_te") + ") AS affected_count, " +
            "(SELECT COUNT(dqi.data_quality_issue_id) FROM data_quality_issues dqi " +
            " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
            " JOIN employment_records er ON UPPER(dqi.entity_type) IN ('EMPLOYMENTRECORD', 'EMPLOYMENT_RECORD') AND dqi.entity_id = er.employment_id AND er.deleted_at IS NULL " +
            " LEFT JOIN training_enrollments er_te ON er.enrollment_id = er_te.enrollment_id AND er_te.deleted_at IS NULL " +
            " WHERE st.is_open_flag = 1 " + employmentFilterSql(scope, "er", "er_te") + ") AS active_issue_count");

    if (!scope.isEmployerScoped()) {
      branches.add(
          "SELECT 'AttendanceRecord' AS entity_type, " +
              "(SELECT COUNT(DISTINCT ar.attendance_id) FROM attendance_records ar " +
              " LEFT JOIN training_enrollments ar_te ON ar.enrollment_id = ar_te.enrollment_id AND ar_te.deleted_at IS NULL " +
              " JOIN training_batches ar_tb ON ar.batch_id = ar_tb.batch_id AND ar_tb.deleted_at IS NULL " +
              " JOIN training_centers ar_tc ON ar_tb.center_id = ar_tc.center_id AND ar_tc.deleted_at IS NULL " +
              " WHERE 1=1 " + attendanceFilterSql(scope, "ar_te", "ar_tb", "ar_tc") + ") AS eligible_count, " +
              "(SELECT COUNT(DISTINCT ar.attendance_id) FROM data_quality_issues dqi " +
              " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
              " JOIN attendance_records ar ON UPPER(dqi.entity_type) IN ('ATTENDANCERECORD', 'ATTENDANCE_RECORD') AND dqi.entity_id = ar.attendance_id " +
              " LEFT JOIN training_enrollments ar_te ON ar.enrollment_id = ar_te.enrollment_id AND ar_te.deleted_at IS NULL " +
              " JOIN training_batches ar_tb ON ar.batch_id = ar_tb.batch_id AND ar_tb.deleted_at IS NULL " +
              " JOIN training_centers ar_tc ON ar_tb.center_id = ar_tc.center_id AND ar_tc.deleted_at IS NULL " +
              " WHERE st.is_open_flag = 1 " + attendanceFilterSql(scope, "ar_te", "ar_tb", "ar_tc") + ") AS affected_count, " +
              "(SELECT COUNT(dqi.data_quality_issue_id) FROM data_quality_issues dqi " +
              " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
              " JOIN attendance_records ar ON UPPER(dqi.entity_type) IN ('ATTENDANCERECORD', 'ATTENDANCE_RECORD') AND dqi.entity_id = ar.attendance_id " +
              " LEFT JOIN training_enrollments ar_te ON ar.enrollment_id = ar_te.enrollment_id AND ar_te.deleted_at IS NULL " +
              " JOIN training_batches ar_tb ON ar.batch_id = ar_tb.batch_id AND ar_tb.deleted_at IS NULL " +
              " JOIN training_centers ar_tc ON ar_tb.center_id = ar_tc.center_id AND ar_tc.deleted_at IS NULL " +
              " WHERE st.is_open_flag = 1 " + attendanceFilterSql(scope, "ar_te", "ar_tb", "ar_tc") + ") AS active_issue_count");

      branches.add(
          "SELECT 'BatchEnrollment' AS entity_type, " +
              "(SELECT COUNT(DISTINCT te.enrollment_id) FROM training_enrollments te " +
              " JOIN training_batches te_tb ON te.batch_id = te_tb.batch_id AND te_tb.deleted_at IS NULL " +
              " JOIN training_centers te_tc ON te_tb.center_id = te_tc.center_id AND te_tc.deleted_at IS NULL " +
              " WHERE te.deleted_at IS NULL " + enrollmentFilterSql(scope, "te", "te_tc") + ") AS eligible_count, " +
              "(SELECT COUNT(DISTINCT te.enrollment_id) FROM data_quality_issues dqi " +
              " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
              " JOIN training_enrollments te ON UPPER(dqi.entity_type) IN ('BATCHENROLLMENT', 'BATCH_ENROLLMENT', 'TRAININGENROLLMENT', 'TRAINING_ENROLLMENT') AND dqi.entity_id = te.enrollment_id AND te.deleted_at IS NULL " +
              " JOIN training_batches te_tb ON te.batch_id = te_tb.batch_id AND te_tb.deleted_at IS NULL " +
              " JOIN training_centers te_tc ON te_tb.center_id = te_tc.center_id AND te_tc.deleted_at IS NULL " +
              " WHERE st.is_open_flag = 1 " + enrollmentFilterSql(scope, "te", "te_tc") + ") AS affected_count, " +
              "(SELECT COUNT(dqi.data_quality_issue_id) FROM data_quality_issues dqi " +
              " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
              " JOIN training_enrollments te ON UPPER(dqi.entity_type) IN ('BATCHENROLLMENT', 'BATCH_ENROLLMENT', 'TRAININGENROLLMENT', 'TRAINING_ENROLLMENT') AND dqi.entity_id = te.enrollment_id AND te.deleted_at IS NULL " +
              " JOIN training_batches te_tb ON te.batch_id = te_tb.batch_id AND te_tb.deleted_at IS NULL " +
              " JOIN training_centers te_tc ON te_tb.center_id = te_tc.center_id AND te_tc.deleted_at IS NULL " +
              " WHERE st.is_open_flag = 1 " + enrollmentFilterSql(scope, "te", "te_tc") + ") AS active_issue_count");
    }

    String sql = String.join(" UNION ALL ", branches);
    return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
      String entityType = rs.getString("entity_type");
      long eligible = rs.getLong("eligible_count");
      long affected = rs.getLong("affected_count");
      long activeIssues = rs.getLong("active_issue_count");
      BigDecimal dqiPct = OperationalMetricsMath.dataCleanlinessIndex(eligible, affected);
      return new DomainCleanlinessMetric(entityType, eligible, affected, activeIssues, dqiPct);
    });
  }

  public IssueSeverityAndStatusAggregate aggregateActiveIssuesByStatusAndSeverity(
      EffectiveDashboardScope scope,
      LocalDateTime windowStart,
      LocalDateTime windowEndExclusive,
      boolean timeWindowApplied) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    bindScopeParams(params, scope);
    StringBuilder where = new StringBuilder(" WHERE st.is_open_flag = 1 ");
    if (timeWindowApplied) {
      params.addValue("winStart", Timestamp.valueOf(windowStart));
      params.addValue("winEnd", Timestamp.valueOf(windowEndExclusive));
      where.append(" AND dqi.detected_at >= :winStart AND dqi.detected_at < :winEnd ");
    }
    where.append(dqIssueScopePredicate(scope));

    String sql =
        "SELECT st.status_code, sev.severity_code, " +
            "COUNT(dqi.data_quality_issue_id) AS cnt, " +
            "SUM(CASE WHEN dqi.data_quality_rule_id IS NULL THEN 1 ELSE 0 END) AS unruled_cnt " +
            "FROM data_quality_issues dqi " +
            "JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
            "JOIN ref_data_quality_severity sev ON dqi.data_quality_severity_id = sev.data_quality_severity_id " +
            DQ_ISSUE_TENANT_JOINS +
            where +
            " GROUP BY st.status_code, sev.severity_code";

    Map<String, Long> byStatus = initOrderedCounts("OPEN", "ASSIGNED", "IN_PROGRESS", "REOPENED");
    Map<String, Long> bySeverity = initOrderedCounts("LOW", "MEDIUM", "HIGH", "CRITICAL");
    long[] unruledTotal = new long[]{0L};

    jdbcTemplate.query(sql, params, rs -> {
      String statusCode = normalizeUpper(rs.getString("status_code"));
      String severityCode = normalizeUpper(rs.getString("severity_code"));
      long cnt = rs.getLong("cnt");
      long unruled = rs.getLong("unruled_cnt");
      if (byStatus.containsKey(statusCode)) {
        byStatus.put(statusCode, OperationalMetricsMath.safeAdd(byStatus.get(statusCode), cnt));
      }
      if (bySeverity.containsKey(severityCode)) {
        bySeverity.put(severityCode, OperationalMetricsMath.safeAdd(bySeverity.get(severityCode), cnt));
      }
      unruledTotal[0] = OperationalMetricsMath.safeAdd(unruledTotal[0], unruled);
    });

    return new IssueSeverityAndStatusAggregate(byStatus, bySeverity, unruledTotal[0]);
  }

  public List<FailingRuleMetric> findTopFailingRules(
      EffectiveDashboardScope scope,
      LocalDateTime windowStart,
      LocalDateTime windowEndExclusive,
      boolean timeWindowApplied) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    bindScopeParams(params, scope);
    StringBuilder where = new StringBuilder(" WHERE st.is_open_flag = 1 AND r.data_quality_rule_id IS NOT NULL ");
    if (timeWindowApplied) {
      params.addValue("winStart", Timestamp.valueOf(windowStart));
      params.addValue("winEnd", Timestamp.valueOf(windowEndExclusive));
      where.append(" AND dqi.detected_at >= :winStart AND dqi.detected_at < :winEnd ");
    }
    where.append(dqIssueScopePredicate(scope));
    params.addValue("limitVal", TOP_FAILING_RULES_LIMIT);

    String sql =
        "SELECT r.data_quality_rule_id, r.rule_code, r.rule_name, r.target_entity_type, " +
            "COUNT(dqi.data_quality_issue_id) AS active_issue_count, " +
            "COUNT(DISTINCT CASE WHEN dqi.entity_id IS NOT NULL THEN CONCAT(UPPER(dqi.entity_type), ':', dqi.entity_id) END) AS distinct_affected_entities " +
            "FROM data_quality_issues dqi " +
            "JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
            "JOIN data_quality_rules r ON dqi.data_quality_rule_id = r.data_quality_rule_id " +
            DQ_ISSUE_TENANT_JOINS +
            where +
            " GROUP BY r.data_quality_rule_id, r.rule_code, r.rule_name, r.target_entity_type " +
            " ORDER BY active_issue_count DESC, distinct_affected_entities DESC, r.rule_code ASC, r.data_quality_rule_id ASC " +
            " LIMIT :limitVal";

    return jdbcTemplate.query(sql, params, (rs, rowNum) -> new FailingRuleMetric(
        rs.getLong("data_quality_rule_id"),
        rs.getString("rule_code"),
        rs.getString("rule_name"),
        rs.getString("target_entity_type"),
        rs.getLong("active_issue_count"),
        rs.getLong("distinct_affected_entities")
    ));
  }

  public ResolutionDurationAggregate aggregateResolutionDuration(
      EffectiveDashboardScope scope,
      LocalDateTime windowStart,
      LocalDateTime windowEndExclusive,
      boolean timeWindowApplied) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    bindScopeParams(params, scope);
    if (timeWindowApplied) {
      params.addValue("winStart", Timestamp.valueOf(windowStart));
      params.addValue("winEnd", Timestamp.valueOf(windowEndExclusive));
    }

    String resolvedWindowClause = timeWindowApplied
        ? " AND COALESCE(res_evt.max_resolved_at, dqi.resolved_at) >= :winStart AND COALESCE(res_evt.max_resolved_at, dqi.resolved_at) < :winEnd "
        : "";
    String scopeClause = dqIssueScopePredicate(scope);

    String resolvedSql =
        "SELECT COUNT(*) AS resolved_count, " +
            "COALESCE(SUM(TIMESTAMPDIFF(MICROSECOND, dqi.detected_at, COALESCE(res_evt.max_resolved_at, dqi.resolved_at))), 0) AS total_micros " +
            "FROM data_quality_issues dqi " +
            "JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
            "LEFT JOIN ( " +
            "  SELECT ev.data_quality_issue_id, MAX(ev.changed_at) AS max_resolved_at " +
            "  FROM data_quality_issue_events ev " +
            "  JOIN ref_data_quality_issue_status evs ON ev.data_quality_issue_status_id = evs.data_quality_issue_status_id " +
            "  WHERE UPPER(evs.status_code) = 'RESOLVED' " +
            "  GROUP BY ev.data_quality_issue_id " +
            ") res_evt ON res_evt.data_quality_issue_id = dqi.data_quality_issue_id " +
            DQ_ISSUE_TENANT_JOINS +
            " WHERE UPPER(st.status_code) = 'RESOLVED' " +
            " AND dqi.detected_at IS NOT NULL " +
            " AND dqi.resolved_at IS NOT NULL " +
            " AND COALESCE(res_evt.max_resolved_at, dqi.resolved_at) >= dqi.detected_at " +
            resolvedWindowClause +
            scopeClause;

    long[] resolvedCount = new long[]{0L};
    BigDecimal[] totalSeconds = new BigDecimal[]{BigDecimal.ZERO};

    jdbcTemplate.query(resolvedSql, params, rs -> {
      resolvedCount[0] = rs.getLong("resolved_count");
      BigDecimal micros = rs.getBigDecimal("total_micros");
      totalSeconds[0] = OperationalMetricsMath.microsecondsToExactSeconds(micros);
    });

    String reopenedWindowClause = timeWindowApplied
        ? " AND dqi.detected_at >= :winStart AND dqi.detected_at < :winEnd "
        : "";
    String reopenedSql =
        "SELECT COUNT(*) AS reopened_count " +
            "FROM data_quality_issues dqi " +
            "JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
            DQ_ISSUE_TENANT_JOINS +
            " WHERE UPPER(st.status_code) = 'REOPENED' " +
            reopenedWindowClause +
            scopeClause;

    Long reopened = jdbcTemplate.queryForObject(reopenedSql, params, Long.class);
    return new ResolutionDurationAggregate(
        resolvedCount[0],
        resolvedCount[0] > 0L ? totalSeconds[0] : null,
        reopened != null ? reopened : 0L
    );
  }

  // =========================================================================
  // 2. Import Metrics Aggregations
  // =========================================================================

  public BatchAggregateResult aggregateImportMetrics(
      EffectiveDashboardScope scope,
      LocalDateTime windowStart,
      LocalDateTime windowEndExclusive) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    bindScopeParams(params, scope);
    params.addValue("winStart", Timestamp.valueOf(windowStart));
    params.addValue("winEnd", Timestamp.valueOf(windowEndExclusive));

    String tenantClause = importScopePredicate(scope);

    String batchStatusSql =
        "SELECT rbs.status_code, COUNT(ib.import_batch_id) AS cnt " +
            "FROM import_batches ib " +
            "JOIN ref_import_batch_status rbs ON ib.import_batch_status_id = rbs.import_batch_status_id " +
            IMPORT_TENANT_JOINS +
            " WHERE ib.created_at >= :winStart AND ib.created_at < :winEnd " +
            tenantClause +
            " GROUP BY rbs.status_code";

    Map<String, Long> batchesByStatus = initOrderedCounts("PENDING", "RUNNING", "COMPLETED", "FAILED", "CANCELLED");
    jdbcTemplate.query(batchStatusSql, params, rs -> {
      String code = normalizeUpper(rs.getString("status_code"));
      long cnt = rs.getLong("cnt");
      batchesByStatus.put(code, OperationalMetricsMath.safeAdd(batchesByStatus.getOrDefault(code, 0L), cnt));
    });

    String recordStatusSql =
        "SELECT rrs.status_code, COUNT(ir.import_record_id) AS cnt " +
            "FROM import_records ir " +
            "JOIN ref_import_record_status rrs ON ir.import_record_status_id = rrs.import_record_status_id " +
            "JOIN import_batches ib ON ir.import_batch_id = ib.import_batch_id " +
            IMPORT_TENANT_JOINS +
            " WHERE ib.created_at >= :winStart AND ib.created_at < :winEnd " +
            tenantClause +
            " GROUP BY rrs.status_code";

    Map<String, Long> recordsByStatus = initOrderedCounts("SUCCESS", "FAILED", "DUPLICATE", "SKIPPED");
    jdbcTemplate.query(recordStatusSql, params, rs -> {
      String code = normalizeUpper(rs.getString("status_code"));
      long cnt = rs.getLong("cnt");
      recordsByStatus.put(code, OperationalMetricsMath.safeAdd(recordsByStatus.getOrDefault(code, 0L), cnt));
    });

    // Operational completion count: batches actually COMPLETED inside [winStart, winEnd)
    String completedInWindowSql =
        "SELECT COUNT(ib.import_batch_id) " +
            "FROM import_batches ib " +
            "JOIN ref_import_batch_status rbs ON ib.import_batch_status_id = rbs.import_batch_status_id " +
            IMPORT_TENANT_JOINS +
            " WHERE UPPER(rbs.status_code) = 'COMPLETED' " +
            " AND ib.completed_at IS NOT NULL " +
            " AND ib.completed_at >= :winStart AND ib.completed_at < :winEnd " +
            tenantClause;
    Long completedInWindowObj = jdbcTemplate.queryForObject(completedInWindowSql, params, Long.class);
    long completedBatchesInWindow = completedInWindowObj != null ? completedInWindowObj : 0L;

    // Operational row-processing throughput numerator: authoritative SUCCESS rows whose record creation/processing
    // timestamp falls inside [winStart, winEnd), irrespective of when the parent batch was staged.
    String successfulRowsInWindowSql =
        "SELECT COUNT(ir.import_record_id) " +
            "FROM import_records ir " +
            "JOIN ref_import_record_status rrs ON ir.import_record_status_id = rrs.import_record_status_id " +
            "JOIN import_batches ib ON ir.import_batch_id = ib.import_batch_id " +
            IMPORT_TENANT_JOINS +
            " WHERE UPPER(rrs.status_code) = 'SUCCESS' " +
            " AND ir.created_at >= :winStart AND ir.created_at < :winEnd " +
            tenantClause;
    Long successfulRowsObj = jdbcTemplate.queryForObject(successfulRowsInWindowSql, params, Long.class);
    long successfulRowsProcessedInWindow = successfulRowsObj != null ? successfulRowsObj : 0L;

    // Execution throughput: COMPLETED batches finished inside [winStart, winEnd) with positive execution duration
    // paired with committed records belonging to that exact completed-batch population.
    String timingSql =
        "SELECT COUNT(ib.import_batch_id) AS timed_batches, " +
            "COALESCE(SUM(TIMESTAMPDIFF(MICROSECOND, ib.started_at, ib.completed_at)), 0) AS total_exec_micros " +
            "FROM import_batches ib " +
            "JOIN ref_import_batch_status rbs ON ib.import_batch_status_id = rbs.import_batch_status_id " +
            IMPORT_TENANT_JOINS +
            " WHERE UPPER(rbs.status_code) = 'COMPLETED' " +
            " AND ib.completed_at IS NOT NULL " +
            " AND ib.completed_at >= :winStart AND ib.completed_at < :winEnd " +
            " AND ib.started_at IS NOT NULL AND ib.completed_at > ib.started_at " +
            tenantClause;

    long[] timedBatches = new long[]{0L};
    BigDecimal[] execSeconds = new BigDecimal[]{BigDecimal.ZERO};
    jdbcTemplate.query(timingSql, params, rs -> {
      timedBatches[0] = rs.getLong("timed_batches");
      execSeconds[0] = OperationalMetricsMath.microsecondsToExactSeconds(rs.getBigDecimal("total_exec_micros"));
    });

    long timedRecordCount = 0L;
    if (timedBatches[0] > 0L) {
      String timedRecordsSql =
          "SELECT COUNT(ir.import_record_id) " +
              "FROM import_records ir " +
              "JOIN import_batches ib ON ir.import_batch_id = ib.import_batch_id " +
              "JOIN ref_import_batch_status rbs ON ib.import_batch_status_id = rbs.import_batch_status_id " +
              IMPORT_TENANT_JOINS +
              " WHERE UPPER(rbs.status_code) = 'COMPLETED' " +
              " AND ib.completed_at IS NOT NULL " +
              " AND ib.completed_at >= :winStart AND ib.completed_at < :winEnd " +
              " AND ib.started_at IS NOT NULL AND ib.completed_at > ib.started_at " +
              tenantClause;
      Long trc = jdbcTemplate.queryForObject(timedRecordsSql, params, Long.class);
      timedRecordCount = trc != null ? trc : 0L;
    }

    return new BatchAggregateResult(
        batchesByStatus,
        recordsByStatus,
        completedBatchesInWindow,
        successfulRowsProcessedInWindow,
        timedBatches[0],
        timedRecordCount,
        timedBatches[0] > 0L ? execSeconds[0] : BigDecimal.ZERO
    );
  }

  // =========================================================================
  // 3. System Health Aggregations
  // =========================================================================

  public IncidentAggregateResult aggregateSystemIncidents(
      EffectiveDashboardScope scope,
      LocalDateTime windowStart,
      LocalDateTime windowEndExclusive) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    bindScopeParams(params, scope);
    params.addValue("winStart", Timestamp.valueOf(windowStart));
    params.addValue("winEnd", Timestamp.valueOf(windowEndExclusive));

    String tenantClause = systemEventScopePredicate(scope);

    String sql =
        "SELECT c.category_code, s.severity_code, COUNT(sel.system_event_log_id) AS cnt, " +
            "SUM(CASE WHEN sel.event_summary LIKE '[cid=%' THEN 1 ELSE 0 END) AS correlated_cnt " +
            "FROM system_event_logs sel " +
            "JOIN ref_system_event_category c ON sel.system_event_category_id = c.system_event_category_id " +
            "JOIN ref_system_event_severity s ON sel.system_event_severity_id = s.system_event_severity_id " +
            SYSTEM_EVENT_TENANT_JOINS +
            " WHERE sel.occurred_at >= :winStart AND sel.occurred_at < :winEnd " +
            tenantClause +
            " GROUP BY c.category_code, s.severity_code";

    Map<String, Long> bySeverity = initOrderedCounts("INFO", "WARNING", "ERROR", "CRITICAL");
    Map<String, Long> byCategory = initOrderedCounts("SECURITY", "INTEGRATION", "BATCH_JOB", "CONFIGURATION", "ACCESS");
    long[] totals = new long[]{0L, 0L};

    jdbcTemplate.query(sql, params, rs -> {
      String catCode = normalizeUpper(rs.getString("category_code"));
      String sevCode = normalizeUpper(rs.getString("severity_code"));
      long cnt = rs.getLong("cnt");
      long corr = rs.getLong("correlated_cnt");
      totals[0] = OperationalMetricsMath.safeAdd(totals[0], cnt);
      totals[1] = OperationalMetricsMath.safeAdd(totals[1], corr);
      bySeverity.put(sevCode, OperationalMetricsMath.safeAdd(bySeverity.getOrDefault(sevCode, 0L), cnt));
      byCategory.put(catCode, OperationalMetricsMath.safeAdd(byCategory.getOrDefault(catCode, 0L), cnt));
    });

    return new IncidentAggregateResult(totals[0], bySeverity, byCategory, totals[1]);
  }

  public AuditMutationAggregateResult aggregateAuditMutations(
      EffectiveDashboardScope scope,
      LocalDateTime windowStart,
      LocalDateTime windowEndExclusive) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    bindScopeParams(params, scope);
    params.addValue("winStart", Timestamp.valueOf(windowStart));
    params.addValue("winEnd", Timestamp.valueOf(windowEndExclusive));

    String tenantClause = auditLogScopePredicate(scope);

    String actionSql =
        "SELECT r.action_code, COUNT(a.audit_log_id) AS cnt " +
            "FROM audit_logs a " +
            "JOIN ref_audit_action r ON a.audit_action_id = r.audit_action_id " +
            AUDIT_LOG_TENANT_JOINS +
            " WHERE a.occurred_at >= :winStart AND a.occurred_at < :winEnd " +
            tenantClause +
            " GROUP BY r.action_code";

    Map<String, Long> mutationsByAction = initOrderedCounts(
        "CREATE", "UPDATE", "DELETE", "SOFT_DELETE", "RESTORE", "STATUS_CHANGE");
    long[] totalMutations = new long[]{0L};
    long[] nonMutations = new long[]{0L};

    jdbcTemplate.query(actionSql, params, rs -> {
      String actionCode = normalizeUpper(rs.getString("action_code"));
      long cnt = rs.getLong("cnt");
      if (mutationsByAction.containsKey(actionCode)) {
        mutationsByAction.put(actionCode, OperationalMetricsMath.safeAdd(mutationsByAction.get(actionCode), cnt));
        totalMutations[0] = OperationalMetricsMath.safeAdd(totalMutations[0], cnt);
      } else {
        nonMutations[0] = OperationalMetricsMath.safeAdd(nonMutations[0], cnt);
      }
    });

    String distinctEntitiesSql =
        "SELECT COUNT(DISTINCT CASE WHEN a.entity_id IS NOT NULL " +
            "THEN CONCAT(UPPER(a.entity_type), ':', a.entity_id) " +
            "ELSE CONCAT(UPPER(a.entity_type), ':UNKEYED:', a.audit_log_id) END) AS distinct_entities " +
            "FROM audit_logs a " +
            "JOIN ref_audit_action r ON a.audit_action_id = r.audit_action_id " +
            AUDIT_LOG_TENANT_JOINS +
            " WHERE a.occurred_at >= :winStart AND a.occurred_at < :winEnd " +
            " AND UPPER(r.action_code) IN ('CREATE', 'UPDATE', 'DELETE', 'SOFT_DELETE', 'RESTORE', 'STATUS_CHANGE') " +
            tenantClause;

    Long distinctEntities = jdbcTemplate.queryForObject(distinctEntitiesSql, params, Long.class);
    return new AuditMutationAggregateResult(
        totalMutations[0],
        distinctEntities != null ? distinctEntities : 0L,
        mutationsByAction,
        nonMutations[0]
    );
  }

  // =========================================================================
  // Tenant-Scope SQL Predicate Builders
  // =========================================================================

  private static void bindScopeParams(MapSqlParameterSource params, EffectiveDashboardScope scope) {
    if (scope.stateId() != null) {
      params.addValue("stateId", scope.stateId());
    }
    if (scope.districtId() != null) {
      params.addValue("districtId", scope.districtId());
    }
    if (scope.trainingProviderId() != null) {
      params.addValue("providerId", scope.trainingProviderId());
    }
    if (scope.employerId() != null) {
      params.addValue("employerId", scope.employerId());
    }
  }

  private static String traineeFilterSql(EffectiveDashboardScope scope, String alias) {
    StringBuilder sb = new StringBuilder();
    if (scope.stateId() != null) {
      sb.append(" AND ").append(alias).append(".state_id = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(" AND ").append(alias).append(".district_id = :districtId ");
    }
    if (scope.trainingProviderId() != null) {
      sb.append(" AND EXISTS (SELECT 1 FROM training_enrollments pte WHERE pte.trainee_id = ")
          .append(alias).append(".trainee_id AND pte.provider_id = :providerId AND pte.deleted_at IS NULL) ");
    }
    return sb.toString();
  }

  private static String placementFilterSql(EffectiveDashboardScope scope, String alias) {
    StringBuilder sb = new StringBuilder();
    if (scope.employerId() != null) {
      sb.append(" AND ").append(alias).append(".employer_id = :employerId ");
    }
    if (scope.trainingProviderId() != null) {
      sb.append(" AND ").append(alias).append(".provider_id = :providerId ");
    }
    if (scope.stateId() != null) {
      sb.append(" AND ").append(alias).append(".work_state_id = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(" AND ").append(alias).append(".work_district_id = :districtId ");
    }
    return sb.toString();
  }

  private static String employmentFilterSql(EffectiveDashboardScope scope, String erAlias, String teAlias) {
    StringBuilder sb = new StringBuilder();
    if (scope.employerId() != null) {
      sb.append(" AND ").append(erAlias).append(".employer_id = :employerId ");
    }
    if (scope.trainingProviderId() != null) {
      sb.append(" AND ").append(teAlias).append(".provider_id = :providerId ");
    }
    if (scope.stateId() != null) {
      sb.append(" AND ").append(erAlias).append(".work_state_id = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(" AND ").append(erAlias).append(".work_district_id = :districtId ");
    }
    return sb.toString();
  }

  private static String attendanceFilterSql(
      EffectiveDashboardScope scope, String teAlias, String tbAlias, String tcAlias) {
    StringBuilder sb = new StringBuilder();
    if (scope.trainingProviderId() != null) {
      sb.append(" AND COALESCE(").append(teAlias).append(".provider_id, ").append(tbAlias).append(".provider_id) = :providerId ");
    }
    if (scope.stateId() != null) {
      sb.append(" AND ").append(tcAlias).append(".state_id = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(" AND ").append(tcAlias).append(".district_id = :districtId ");
    }
    return sb.toString();
  }

  private static String enrollmentFilterSql(EffectiveDashboardScope scope, String teAlias, String tcAlias) {
    StringBuilder sb = new StringBuilder();
    if (scope.trainingProviderId() != null) {
      sb.append(" AND ").append(teAlias).append(".provider_id = :providerId ");
    }
    if (scope.stateId() != null) {
      sb.append(" AND ").append(tcAlias).append(".state_id = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(" AND ").append(tcAlias).append(".district_id = :districtId ");
    }
    return sb.toString();
  }

  private static String dqIssueScopePredicate(EffectiveDashboardScope scope) {
    StringBuilder sb = new StringBuilder();
    if (scope.employerId() != null) {
      sb.append(
          " AND ((UPPER(dqi.entity_type) IN ('PLACEMENTRECORD', 'PLACEMENT_RECORD') AND pr.employer_id = :employerId) " +
              "OR (UPPER(dqi.entity_type) IN ('EMPLOYMENTRECORD', 'EMPLOYMENT_RECORD') AND er.employer_id = :employerId)) ");
    }
    if (scope.trainingProviderId() != null) {
      sb.append(
          " AND ((UPPER(dqi.entity_type) = 'TRAINEE' AND t.trainee_id IS NOT NULL AND EXISTS (SELECT 1 FROM training_enrollments pte WHERE pte.trainee_id = t.trainee_id AND pte.provider_id = :providerId AND pte.deleted_at IS NULL)) " +
              "OR (UPPER(dqi.entity_type) IN ('ATTENDANCERECORD', 'ATTENDANCE_RECORD') AND ar.attendance_id IS NOT NULL AND COALESCE(ar_te.provider_id, ar_tb.provider_id) = :providerId) " +
              "OR (UPPER(dqi.entity_type) IN ('PLACEMENTRECORD', 'PLACEMENT_RECORD') AND pr.provider_id = :providerId) " +
              "OR (UPPER(dqi.entity_type) IN ('EMPLOYMENTRECORD', 'EMPLOYMENT_RECORD') AND er_te.provider_id = :providerId) " +
              "OR (UPPER(dqi.entity_type) IN ('BATCHENROLLMENT', 'BATCH_ENROLLMENT', 'TRAININGENROLLMENT', 'TRAINING_ENROLLMENT') AND be_te.provider_id = :providerId)) ");
    }
    if (scope.stateId() != null) {
      sb.append(
          " AND COALESCE(" +
              "t.state_id, " +
              "ar_tc.state_id, " +
              "pr.work_state_id, " +
              "er.work_state_id, " +
              "be_tc.state_id" +
              ") = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(
          " AND COALESCE(" +
              "t.district_id, " +
              "ar_tc.district_id, " +
              "pr.work_district_id, " +
              "er.work_district_id, " +
              "be_tc.district_id" +
              ") = :districtId ");
    }
    return sb.toString();
  }

  private static String importScopePredicate(EffectiveDashboardScope scope) {
    StringBuilder sb = new StringBuilder();
    if (scope.employerId() != null) {
      sb.append(" AND ibu.employer_id = :employerId ");
    }
    if (scope.trainingProviderId() != null) {
      sb.append(" AND ibu.training_provider_id = :providerId ");
    }
    if (scope.stateId() != null) {
      sb.append(" AND COALESCE(ibu.state_id, ibutp.state_id, ibuemp.state_id) = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(" AND COALESCE(ibu.district_id, ibutp.district_id, ibuemp.district_id) = :districtId ");
    }
    return sb.toString();
  }

  private static String systemEventScopePredicate(EffectiveDashboardScope scope) {
    StringBuilder sb = new StringBuilder();
    if (scope.employerId() != null) {
      sb.append(
          " AND COALESCE(" +
              "ibu.employer_id, " +
              "CASE WHEN UPPER(sel.entity_type) = 'EMPLOYER' THEN sel.entity_id ELSE NULL END, " +
              "eu.employer_id, " +
              "au.employer_id" +
              ") = :employerId ");
    }
    if (scope.trainingProviderId() != null) {
      sb.append(
          " AND COALESCE(" +
              "ibu.training_provider_id, " +
              "CASE WHEN UPPER(sel.entity_type) IN ('TRAININGPROVIDER', 'TRAINING_PROVIDER') THEN sel.entity_id ELSE NULL END, " +
              "eu.training_provider_id, " +
              "au.training_provider_id" +
              ") = :providerId ");
    }
    if (scope.stateId() != null) {
      sb.append(
          " AND COALESCE(" +
              "ibu.state_id, ibutp.state_id, ibuemp.state_id, " +
              "CASE WHEN UPPER(sel.entity_type) = 'STATE' THEN sel.entity_id ELSE NULL END, " +
              "tp.state_id, emp.state_id, " +
              "eu.state_id, eutp.state_id, euemp.state_id, " +
              "au.state_id, autp.state_id, auemp.state_id" +
              ") = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(
          " AND COALESCE(" +
              "ibu.district_id, ibutp.district_id, ibuemp.district_id, " +
              "tp.district_id, emp.district_id, " +
              "eu.district_id, eutp.district_id, euemp.district_id, " +
              "au.district_id, autp.district_id, auemp.district_id" +
              ") = :districtId ");
    }
    return sb.toString();
  }

  private static String auditLogScopePredicate(EffectiveDashboardScope scope) {
    StringBuilder sb = new StringBuilder();
    if (scope.employerId() != null) {
      sb.append(
          " AND COALESCE(" +
              "apr.employer_id, aer.employer_id, aibu.employer_id, eu.employer_id, aemp.employer_id, " +
              "au.employer_id" +
              ") = :employerId ");
    }
    if (scope.trainingProviderId() != null) {
      sb.append(
          " AND COALESCE(" +
              "apr.provider_id, aer_te.provider_id, ate.provider_id, aibu.training_provider_id, eu.training_provider_id, atp.provider_id, " +
              "au.training_provider_id" +
              ") = :providerId ");
    }
    if (scope.stateId() != null) {
      sb.append(
          " AND COALESCE(" +
              "atr.state_id, apr.work_state_id, aer.work_state_id, ate_tc.state_id, " +
              "aibu.state_id, aibutp.state_id, aibuemp.state_id, atp.state_id, aemp.state_id, " +
              "eu.state_id, eutp.state_id, euemp.state_id, " +
              "au.state_id, autp.state_id, auemp.state_id" +
              ") = :stateId ");
    }
    if (scope.districtId() != null) {
      sb.append(
          " AND COALESCE(" +
              "atr.district_id, apr.work_district_id, aer.work_district_id, ate_tc.district_id, " +
              "aibu.district_id, aibutp.district_id, aibuemp.district_id, atp.district_id, aemp.district_id, " +
              "eu.district_id, eutp.district_id, euemp.district_id, " +
              "au.district_id, autp.district_id, auemp.district_id" +
              ") = :districtId ");
    }
    return sb.toString();
  }

  private static Map<String, Long> initOrderedCounts(String... keys) {
    Map<String, Long> map = new LinkedHashMap<>();
    for (String key : keys) {
      map.put(key, 0L);
    }
    return map;
  }

  private static String normalizeUpper(String raw) {
    return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
  }
}
