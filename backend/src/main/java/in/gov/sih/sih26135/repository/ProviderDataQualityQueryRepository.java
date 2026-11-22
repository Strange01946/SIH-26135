package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.dto.response.CenterScorecardResponse;
import in.gov.sih.sih26135.dto.response.ProviderDataQualityIssueResponse;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import in.gov.sih.sih26135.util.OperationalMetricsMath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Parameterized SQL aggregation and query repository for Phase 30.5 Training Provider Data Quality
 * and Center Scorecards. Enforces strict provider-level tenant isolation inside all queries.
 */
@Repository
public class ProviderDataQualityQueryRepository {

  private static final String CLEANLINESS_FORMULA =
      "(eligiblePopulation - distinctAffectedEntities) / eligiblePopulation * 100";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public ProviderDataQualityQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final String BASE_ISSUE_JOINS =
      " FROM data_quality_issues dqi " +
      " LEFT JOIN data_quality_rules r ON dqi.data_quality_rule_id = r.data_quality_rule_id " +
      " JOIN ref_data_quality_category c ON dqi.data_quality_category_id = c.data_quality_category_id " +
      " JOIN ref_data_quality_severity s ON dqi.data_quality_severity_id = s.data_quality_severity_id " +
      " JOIN ref_data_quality_issue_status st ON dqi.data_quality_issue_status_id = st.data_quality_issue_status_id " +
      " LEFT JOIN trainees t ON UPPER(dqi.entity_type) = 'TRAINEE' AND dqi.entity_id = t.trainee_id AND t.deleted_at IS NULL " +
      " LEFT JOIN training_enrollments t_te ON UPPER(dqi.entity_type) = 'TRAINEE' AND t.trainee_id = t_te.trainee_id AND t_te.deleted_at IS NULL AND t_te.provider_id = :providerId " +
      " LEFT JOIN training_enrollments te ON UPPER(dqi.entity_type) IN ('BATCHENROLLMENT', 'BATCH_ENROLLMENT', 'TRAININGENROLLMENT', 'TRAINING_ENROLLMENT') AND dqi.entity_id = te.enrollment_id AND te.deleted_at IS NULL " +
      " LEFT JOIN attendance_records ar ON UPPER(dqi.entity_type) IN ('ATTENDANCERECORD', 'ATTENDANCE_RECORD') AND dqi.entity_id = ar.attendance_id " +
      " LEFT JOIN training_batches ar_tb ON ar.batch_id = ar_tb.batch_id AND ar_tb.deleted_at IS NULL " +
      " LEFT JOIN training_batches tb ON UPPER(dqi.entity_type) IN ('TRAININGBATCH', 'TRAINING_BATCH', 'BATCH') AND dqi.entity_id = tb.batch_id AND tb.deleted_at IS NULL " +
      " LEFT JOIN training_batches ref_tb ON COALESCE(te.batch_id, ar.batch_id, t_te.batch_id) = ref_tb.batch_id AND ref_tb.deleted_at IS NULL " +
      " LEFT JOIN training_centers tc ON COALESCE(te.center_id, tb.center_id, ref_tb.center_id, t_te.center_id) = tc.center_id AND tc.deleted_at IS NULL ";

  private static final String PROVIDER_OWNERSHIP_WHERE =
      " WHERE ((UPPER(dqi.entity_type) = 'TRAINEE' AND t_te.provider_id = :providerId) " +
      "    OR (UPPER(dqi.entity_type) IN ('BATCHENROLLMENT', 'BATCH_ENROLLMENT', 'TRAININGENROLLMENT', 'TRAINING_ENROLLMENT') AND te.provider_id = :providerId) " +
      "    OR (UPPER(dqi.entity_type) IN ('ATTENDANCERECORD', 'ATTENDANCE_RECORD') AND ar_tb.provider_id = :providerId) " +
      "    OR (UPPER(dqi.entity_type) IN ('TRAININGBATCH', 'TRAINING_BATCH', 'BATCH') AND tb.provider_id = :providerId)) ";

  public Page<ProviderDataQualityIssueResponse> findProviderIssues(
      Long providerId,
      String severity,
      String category,
      String status,
      String entityType,
      Long centerId,
      Boolean activeOnly,
      Pageable pageable) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("providerId", providerId);

    StringBuilder filterClause = new StringBuilder();

    if (severity != null && !severity.isBlank()) {
      String normSev = severity.trim().toUpperCase(Locale.ROOT);
      if ("CRITICAL".equals(normSev)) {
        filterClause.append(" AND UPPER(s.severity_code) = 'CRITICAL' ");
      } else if ("MAJOR".equals(normSev) || "HIGH".equals(normSev)) {
        filterClause.append(" AND UPPER(s.severity_code) IN ('HIGH', 'MAJOR') ");
      } else if ("MINOR".equals(normSev) || "LOW".equals(normSev) || "MEDIUM".equals(normSev)) {
        filterClause.append(" AND UPPER(s.severity_code) IN ('LOW', 'MEDIUM', 'MINOR') ");
      } else {
        params.addValue("severity", normSev);
        filterClause.append(" AND UPPER(s.severity_code) = :severity ");
      }
    }

    if (category != null && !category.isBlank()) {
      String normCat = category.trim().toUpperCase(Locale.ROOT);
      if ("COMPLETENESS".equals(normCat) || "MISSING".equals(normCat)) {
        filterClause.append(" AND UPPER(c.category_code) IN ('COMPLETENESS', 'MISSING') ");
      } else if ("CONSISTENCY".equals(normCat) || "INCONSISTENT".equals(normCat)) {
        filterClause.append(" AND UPPER(c.category_code) IN ('CONSISTENCY', 'INCONSISTENT') ");
      } else if ("VALIDITY".equals(normCat) || "INVALID".equals(normCat)) {
        filterClause.append(" AND UPPER(c.category_code) IN ('VALIDITY', 'INVALID') ");
      } else if ("DUPLICATE".equals(normCat)) {
        filterClause.append(" AND UPPER(c.category_code) = 'DUPLICATE' ");
      } else {
        params.addValue("category", normCat);
        filterClause.append(" AND UPPER(c.category_code) = :category ");
      }
    }

    if (status != null && !status.isBlank()) {
      String normStat = status.trim().toUpperCase(Locale.ROOT);
      if ("OPEN".equals(normStat)) {
        filterClause.append(" AND UPPER(st.status_code) = 'OPEN' ");
      } else if ("INVESTIGATING".equals(normStat) || "IN_PROGRESS".equals(normStat)) {
        filterClause.append(" AND UPPER(st.status_code) IN ('IN_PROGRESS', 'ASSIGNED') ");
      } else if ("RESOLVED".equals(normStat)) {
        filterClause.append(" AND UPPER(st.status_code) = 'RESOLVED' ");
      } else if ("FALSE_POSITIVE".equals(normStat) || "SUPPRESSED".equals(normStat) || "DISMISSED".equals(normStat)) {
        filterClause.append(" AND UPPER(st.status_code) = 'DISMISSED' ");
      } else if ("REOPENED".equals(normStat)) {
        filterClause.append(" AND UPPER(st.status_code) = 'REOPENED' ");
      } else {
        params.addValue("status", normStat);
        filterClause.append(" AND UPPER(st.status_code) = :status ");
      }
    }

    if (entityType != null && !entityType.isBlank()) {
      String normEntity = entityType.trim().toUpperCase(Locale.ROOT);
      params.addValue("entityType", normEntity);
      filterClause.append(" AND UPPER(dqi.entity_type) = :entityType ");
    }

    if (centerId != null && centerId > 0) {
      params.addValue("centerId", centerId);
      filterClause.append(" AND COALESCE(te.center_id, tb.center_id, ref_tb.center_id, t_te.center_id) = :centerId ");
    }

    if (activeOnly != null) {
      if (activeOnly) {
        filterClause.append(" AND st.is_open_flag = 1 ");
      } else {
        filterClause.append(" AND st.is_open_flag = 0 ");
      }
    }

    String countSql = "SELECT COUNT(DISTINCT dqi.data_quality_issue_id) " + BASE_ISSUE_JOINS + PROVIDER_OWNERSHIP_WHERE + filterClause;
    Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);
    long totalElements = total != null ? total : 0L;

    if (totalElements == 0) {
      return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    String selectSql =
        "SELECT DISTINCT " +
        "  dqi.data_quality_issue_id AS id, " +
        "  dqi.data_quality_rule_id AS ruleId, " +
        "  r.rule_code AS ruleCode, " +
        "  r.rule_name AS ruleName, " +
        "  dqi.data_quality_category_id AS categoryId, " +
        "  c.category_code AS categoryCode, " +
        "  c.category_name AS categoryName, " +
        "  dqi.data_quality_severity_id AS severityId, " +
        "  s.severity_code AS severityCode, " +
        "  s.severity_name AS severityName, " +
        "  dqi.data_quality_issue_status_id AS statusId, " +
        "  st.status_code AS statusCode, " +
        "  st.status_name AS statusName, " +
        "  st.is_open_flag AS isOpen, " +
        "  dqi.entity_type AS entityType, " +
        "  dqi.entity_id AS entityId, " +
        "  COALESCE(te.center_id, tb.center_id, ref_tb.center_id, t_te.center_id) AS centerId, " +
        "  tc.center_name AS centerName, " +
        "  COALESCE(tb.batch_id, ref_tb.batch_id) AS batchId, " +
        "  COALESCE(tb.batch_code, ref_tb.batch_code) AS batchCode, " +
        "  dqi.issue_summary AS issueSummary, " +
        "  dqi.detected_at AS detectedAt, " +
        "  dqi.updated_at AS updatedAt, " +
        "  dqi.resolved_at AS resolvedAt, " +
        "  dqi.resolved_by_user_id AS resolvedByUserId, " +
        "  dqi.resolution_notes AS resolutionNotes " +
        BASE_ISSUE_JOINS +
        PROVIDER_OWNERSHIP_WHERE +
        filterClause +
        " ORDER BY dqi.detected_at DESC, dqi.data_quality_issue_id DESC " +
        " LIMIT :limit OFFSET :offset ";

    params.addValue("limit", pageable.getPageSize());
    params.addValue("offset", pageable.getOffset());

    List<ProviderDataQualityIssueResponse> content = jdbcTemplate.query(selectSql, params, new IssueRowMapper());
    return new PageImpl<>(content, pageable, totalElements);
  }

  public Optional<ProviderDataQualityIssueResponse> findProviderIssueById(Long providerId, Long issueId) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("providerId", providerId);
    params.addValue("issueId", issueId);

    String sql =
        "SELECT " +
        "  dqi.data_quality_issue_id AS id, " +
        "  dqi.data_quality_rule_id AS ruleId, " +
        "  r.rule_code AS ruleCode, " +
        "  r.rule_name AS ruleName, " +
        "  dqi.data_quality_category_id AS categoryId, " +
        "  c.category_code AS categoryCode, " +
        "  c.category_name AS categoryName, " +
        "  dqi.data_quality_severity_id AS severityId, " +
        "  s.severity_code AS severityCode, " +
        "  s.severity_name AS severityName, " +
        "  dqi.data_quality_issue_status_id AS statusId, " +
        "  st.status_code AS statusCode, " +
        "  st.status_name AS statusName, " +
        "  st.is_open_flag AS isOpen, " +
        "  dqi.entity_type AS entityType, " +
        "  dqi.entity_id AS entityId, " +
        "  COALESCE(te.center_id, tb.center_id, ref_tb.center_id, t_te.center_id) AS centerId, " +
        "  tc.center_name AS centerName, " +
        "  COALESCE(tb.batch_id, ref_tb.batch_id) AS batchId, " +
        "  COALESCE(tb.batch_code, ref_tb.batch_code) AS batchCode, " +
        "  dqi.issue_summary AS issueSummary, " +
        "  dqi.detected_at AS detectedAt, " +
        "  dqi.updated_at AS updatedAt, " +
        "  dqi.resolved_at AS resolvedAt, " +
        "  dqi.resolved_by_user_id AS resolvedByUserId, " +
        "  dqi.resolution_notes AS resolutionNotes " +
        BASE_ISSUE_JOINS +
        PROVIDER_OWNERSHIP_WHERE +
        " AND dqi.data_quality_issue_id = :issueId LIMIT 1";

    List<ProviderDataQualityIssueResponse> list = jdbcTemplate.query(sql, params, new IssueRowMapper());
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  public boolean isIssueOwnedByProvider(Long issueId, Long providerId) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("issueId", issueId);
    params.addValue("providerId", providerId);

    String sql = "SELECT COUNT(DISTINCT dqi.data_quality_issue_id) " + BASE_ISSUE_JOINS + PROVIDER_OWNERSHIP_WHERE + " AND dqi.data_quality_issue_id = :issueId";
    Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
    return count != null && count > 0;
  }

  public boolean isCenterOwnedByProvider(Long centerId, Long providerId) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("centerId", centerId);
    params.addValue("providerId", providerId);

    String sql = "SELECT COUNT(*) FROM training_centers WHERE center_id = :centerId AND provider_id = :providerId AND deleted_at IS NULL";
    Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
    return count != null && count > 0;
  }

  public CenterScorecardResponse getCenterScorecard(Long providerId, Long centerId) {
    MapSqlParameterSource centerParams = new MapSqlParameterSource();
    centerParams.addValue("centerId", centerId);
    centerParams.addValue("providerId", providerId);

    String centerSql = "SELECT center_code, center_name FROM training_centers WHERE center_id = :centerId AND provider_id = :providerId AND deleted_at IS NULL";
    Map<String, Object> centerRow;
    try {
      centerRow = jdbcTemplate.queryForMap(centerSql, centerParams);
    } catch (Exception e) {
      return null;
    }

    String centerCode = (String) centerRow.get("center_code");
    String centerName = (String) centerRow.get("center_name");

    // 1. Eligible population in this center:
    // Count distinct enrollments, attendance records, and trainees associated with this center
    String eligibleSql =
        "SELECT " +
        "  (SELECT COUNT(DISTINCT te.enrollment_id) FROM training_enrollments te WHERE te.center_id = :centerId AND te.provider_id = :providerId AND te.deleted_at IS NULL) AS eligible_enrollments, " +
        "  (SELECT COUNT(DISTINCT ar.attendance_id) FROM attendance_records ar JOIN training_batches tb ON ar.batch_id = tb.batch_id WHERE tb.center_id = :centerId AND tb.provider_id = :providerId AND tb.deleted_at IS NULL) AS eligible_attendance, " +
        "  (SELECT COUNT(DISTINCT te.trainee_id) FROM training_enrollments te WHERE te.center_id = :centerId AND te.provider_id = :providerId AND te.deleted_at IS NULL) AS eligible_trainees";

    Map<String, Object> eligibleCounts = jdbcTemplate.queryForMap(eligibleSql, centerParams);
    long eligibleEnrollments = ((Number) eligibleCounts.get("eligible_enrollments")).longValue();
    long eligibleAttendance = ((Number) eligibleCounts.get("eligible_attendance")).longValue();
    long eligibleTrainees = ((Number) eligibleCounts.get("eligible_trainees")).longValue();
    long eligiblePopulation = eligibleEnrollments + eligibleAttendance + eligibleTrainees;

    // 2. Distinct affected entities in this center with active unresolved issues (st.is_open_flag = 1)
    String affectedSql =
        "SELECT COUNT(DISTINCT CASE WHEN dqi.entity_id IS NOT NULL THEN CONCAT(UPPER(dqi.entity_type), ':', dqi.entity_id) END) " +
        BASE_ISSUE_JOINS +
        PROVIDER_OWNERSHIP_WHERE +
        " AND st.is_open_flag = 1 " +
        " AND COALESCE(te.center_id, tb.center_id, ref_tb.center_id, t_te.center_id) = :centerId ";

    Long affected = jdbcTemplate.queryForObject(affectedSql, centerParams, Long.class);
    long distinctAffectedEntities = affected != null ? affected : 0L;

    // Cleanliness index calculation via OperationalMetricsMath
    BigDecimal cleanlinessIndexPct = OperationalMetricsMath.dataCleanlinessIndex(eligiblePopulation, distinctAffectedEntities);
    long cleanEntitiesEstimate = eligiblePopulation > 0L
        ? Math.max(0L, eligiblePopulation - Math.min(eligiblePopulation, distinctAffectedEntities))
        : 0L;

    // 3. Issue breakdown by Severity and Category for this center
    String issuesSql =
        "SELECT s.severity_code, c.category_code, COUNT(dqi.data_quality_issue_id) AS cnt " +
        BASE_ISSUE_JOINS +
        PROVIDER_OWNERSHIP_WHERE +
        " AND st.is_open_flag = 1 " +
        " AND COALESCE(te.center_id, tb.center_id, ref_tb.center_id, t_te.center_id) = :centerId " +
        " GROUP BY s.severity_code, c.category_code";

    Map<String, Long> bySeverity = new LinkedHashMap<>();
    bySeverity.put("CRITICAL", 0L);
    bySeverity.put("HIGH", 0L);
    bySeverity.put("MEDIUM", 0L);
    bySeverity.put("LOW", 0L);

    Map<String, Long> byCategory = new LinkedHashMap<>();
    byCategory.put("COMPLETENESS", 0L);
    byCategory.put("CONSISTENCY", 0L);
    byCategory.put("VALIDITY", 0L);
    byCategory.put("DUPLICATE", 0L);

    long totalActiveIssues = 0L;

    List<Map<String, Object>> issueRows = jdbcTemplate.queryForList(issuesSql, centerParams);
    for (Map<String, Object> row : issueRows) {
      String sev = ((String) row.get("severity_code")).toUpperCase(Locale.ROOT);
      String cat = ((String) row.get("category_code")).toUpperCase(Locale.ROOT);
      long cnt = ((Number) row.get("cnt")).longValue();
      totalActiveIssues += cnt;

      if ("CRITICAL".equals(sev)) {
        bySeverity.put("CRITICAL", bySeverity.get("CRITICAL") + cnt);
      } else if ("HIGH".equals(sev) || "MAJOR".equals(sev)) {
        bySeverity.put("HIGH", bySeverity.get("HIGH") + cnt);
      } else if ("MEDIUM".equals(sev)) {
        bySeverity.put("MEDIUM", bySeverity.get("MEDIUM") + cnt);
      } else if ("LOW".equals(sev) || "MINOR".equals(sev)) {
        bySeverity.put("LOW", bySeverity.get("LOW") + cnt);
      }

      if ("COMPLETENESS".equals(cat) || "MISSING".equals(cat)) {
        byCategory.put("COMPLETENESS", byCategory.get("COMPLETENESS") + cnt);
      } else if ("CONSISTENCY".equals(cat) || "INCONSISTENT".equals(cat)) {
        byCategory.put("CONSISTENCY", byCategory.get("CONSISTENCY") + cnt);
      } else if ("VALIDITY".equals(cat) || "INVALID".equals(cat)) {
        byCategory.put("VALIDITY", byCategory.get("VALIDITY") + cnt);
      } else if ("DUPLICATE".equals(cat)) {
        byCategory.put("DUPLICATE", byCategory.get("DUPLICATE") + cnt);
      }
    }

    // 4. Enrollment Outcome & Completion/Dropout metrics for this center
    String enrollmentSql =
        "SELECT " +
        "  COUNT(te.enrollment_id) AS total_enrollments, " +
        "  SUM(CASE WHEN res.status_code = 'COMPLETED' OR res.is_completed_flag = 1 THEN 1 ELSE 0 END) AS completed_count, " +
        "  SUM(CASE WHEN res.status_code = 'DROPPED' THEN 1 ELSE 0 END) AS dropped_count, " +
        "  SUM(CASE WHEN res.status_code IN ('ENROLLED', 'IN_PROGRESS') THEN 1 ELSE 0 END) AS in_progress_count " +
        "FROM training_enrollments te " +
        "JOIN ref_enrollment_status res ON te.enrollment_status_id = res.enrollment_status_id " +
        "WHERE te.center_id = :centerId AND te.provider_id = :providerId AND te.deleted_at IS NULL";

    Map<String, Object> enrollmentRow = jdbcTemplate.queryForMap(enrollmentSql, centerParams);
    long totalEnrollments = ((Number) enrollmentRow.get("total_enrollments")).longValue();
    long completedEnrollments = enrollmentRow.get("completed_count") != null ? ((Number) enrollmentRow.get("completed_count")).longValue() : 0L;
    long droppedEnrollments = enrollmentRow.get("dropped_count") != null ? ((Number) enrollmentRow.get("dropped_count")).longValue() : 0L;
    long inProgressEnrollments = enrollmentRow.get("in_progress_count") != null ? ((Number) enrollmentRow.get("in_progress_count")).longValue() : 0L;

    BigDecimal completionRatePct = null;
    BigDecimal dropoutRatePct = null;
    if (totalEnrollments > 0) {
      completionRatePct = AnalyticsCalculationUtils.calculatePercentage(completedEnrollments, totalEnrollments);
      dropoutRatePct = AnalyticsCalculationUtils.calculatePercentage(droppedEnrollments, totalEnrollments);
    }

    return new CenterScorecardResponse(
        centerId,
        centerCode,
        centerName,
        providerId,
        cleanlinessIndexPct,
        CLEANLINESS_FORMULA,
        "CURRENT_COHORT_ACTIVE_ISSUES",
        eligiblePopulation,
        distinctAffectedEntities,
        cleanEntitiesEstimate,
        totalActiveIssues,
        bySeverity,
        byCategory,
        totalEnrollments,
        completedEnrollments,
        droppedEnrollments,
        inProgressEnrollments,
        completionRatePct,
        dropoutRatePct,
        LocalDateTime.now()
    );
  }

  private static class IssueRowMapper implements RowMapper<ProviderDataQualityIssueResponse> {
    @Override
    public ProviderDataQualityIssueResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
      Long id = rs.getLong("id");
      Long ruleId = rs.getObject("ruleId") != null ? rs.getLong("ruleId") : null;
      String ruleCode = rs.getString("ruleCode");
      String ruleName = rs.getString("ruleName");
      Long categoryId = rs.getLong("categoryId");
      String categoryCode = rs.getString("categoryCode");
      String categoryName = rs.getString("categoryName");
      Long severityId = rs.getLong("severityId");
      String severityCode = rs.getString("severityCode");
      String severityName = rs.getString("severityName");
      Long statusId = rs.getLong("statusId");
      String statusCode = rs.getString("statusCode");
      String statusName = rs.getString("statusName");
      boolean isOpen = rs.getInt("isOpen") == 1;
      String entityType = rs.getString("entityType");
      Long entityId = rs.getObject("entityId") != null ? rs.getLong("entityId") : null;
      Long centerId = rs.getObject("centerId") != null ? rs.getLong("centerId") : null;
      String centerName = rs.getString("centerName");
      Long batchId = rs.getObject("batchId") != null ? rs.getLong("batchId") : null;
      String batchCode = rs.getString("batchCode");
      String issueSummary = rs.getString("issueSummary");

      Timestamp detectedTs = rs.getTimestamp("detectedAt");
      LocalDateTime detectedAt = detectedTs != null ? detectedTs.toLocalDateTime() : null;
      Timestamp updatedTs = rs.getTimestamp("updatedAt");
      LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;
      Timestamp resolvedTs = rs.getTimestamp("resolvedAt");
      LocalDateTime resolvedAt = resolvedTs != null ? resolvedTs.toLocalDateTime() : null;
      Long resolvedByUserId = rs.getObject("resolvedByUserId") != null ? rs.getLong("resolvedByUserId") : null;
      String resolutionNotes = rs.getString("resolutionNotes");

      // Remediation permission evaluation based on actual backend domain capabilities
      boolean remediationPermitted = false;
      String remediationReason;

      String normEntity = entityType != null ? entityType.toUpperCase(Locale.ROOT) : "";
      if (normEntity.contains("ENROLLMENT")) {
        remediationPermitted = true;
        remediationReason = "Training enrollment record is editable within provider scope (PUT /api/v1/training-enrollments/{id}).";
      } else if (normEntity.contains("BATCH")) {
        remediationPermitted = true;
        remediationReason = "Training batch schedule and capacity are editable within provider scope (PUT /api/v1/training-batches/{id}).";
      } else if (normEntity.contains("ATTENDANCE")) {
        remediationPermitted = true;
        remediationReason = "Attendance records can be corrected via attendance workbench.";
      } else if ("TRAINEE".equals(normEntity)) {
        remediationPermitted = false;
        remediationReason = "Direct trainee demographic profile edits are restricted to candidate self-service or state administrator (trainee.write required).";
      } else {
        remediationPermitted = false;
        remediationReason = "Entity type " + entityType + " is not directly editable by training provider.";
      }

      return new ProviderDataQualityIssueResponse(
          id,
          ruleId,
          ruleCode,
          ruleName,
          categoryId,
          categoryCode,
          categoryName,
          severityId,
          severityCode,
          severityName,
          statusId,
          statusCode,
          statusName,
          isOpen,
          entityType,
          entityId,
          centerId,
          centerName,
          batchId,
          batchCode,
          issueSummary,
          detectedAt,
          updatedAt,
          resolvedAt,
          resolvedByUserId,
          resolutionNotes,
          remediationPermitted,
          remediationReason
      );
    }
  }
}
