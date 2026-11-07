package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.export.DsdpExportRow;
import in.gov.sih.sih26135.dto.response.export.ProviderScorecardExportRow;
import in.gov.sih.sih26135.dto.response.export.QprExportRow;
import in.gov.sih.sih26135.repository.analytics.util.AnalyticsQueryFilterBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Dedicated repository for Government MIS Report Generation and Analytical Exports.
 * Executes bounded, deterministic chunked queries via {@link JdbcTemplate} against
 * authoritative views and master tables without mutating JPA contracts.
 */
@Repository
public class AnalyticsExportRepository {

  private final JdbcTemplate jdbcTemplate;

  public AnalyticsExportRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // =========================================================================
  // 1. DSDP Queries
  // =========================================================================

  public record DistrictMetadata(
      Long districtId,
      String districtCode,
      String districtName,
      Long stateId,
      String stateCode,
      String stateName
  ) {}

  public Optional<DistrictMetadata> findDistrictMetadata(Long districtId) {
    String sql = """
        SELECT
          d.district_id AS districtId,
          d.district_code AS districtCode,
          d.district_name AS districtName,
          st.state_id AS stateId,
          st.state_code AS stateCode,
          st.state_name AS stateName
        FROM districts d
        JOIN states st ON st.state_id = d.state_id
        WHERE d.district_id = ? AND d.deleted_at IS NULL
        """;
    try {
      return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rn) -> new DistrictMetadata(
          rs.getLong("districtId"),
          rs.getString("districtCode"),
          rs.getString("districtName"),
          rs.getLong("stateId"),
          rs.getString("stateCode"),
          rs.getString("stateName")
      ), districtId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public int countDsdpRows(Long districtId, AnalyticsFilterCriteria criteria) {
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String sql = """
        SELECT COUNT(DISTINCT c.course_id)
        FROM vw_enrollment_outcome_fact v
        JOIN courses c ON c.course_id = v.course_id
        WHERE v.trainee_district_id = ?
        """ + filter.getSqlFragment() + """
          AND c.deleted_at IS NULL
        """;
    List<Object> params = new ArrayList<>();
    params.add(districtId);
    params.addAll(filter.getParameters());
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params.toArray());
    return count != null ? count : 0;
  }

  public List<DsdpExportRow> fetchDsdpRows(
      Long districtId,
      DistrictMetadata meta,
      AnalyticsFilterCriteria criteria,
      int limit,
      int offset) {
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    AnalyticsQueryFilterBuilder.FilterPredicateResult salaryFilter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v_sal");

    String sql = """
        SELECT
          c.course_id AS courseId,
          c.course_code AS courseCode,
          c.course_name AS courseName,
          sec.sector_name AS sectorName,
          COUNT(*) AS enrollmentCount,
          COUNT(DISTINCT v.trainee_id) AS traineeCount,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) = 0 THEN 0
            WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.is_completed_flag)
            ELSE NULL
          END AS completedCount,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) = 0 THEN 0
            WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_issued_certificate_flag)
            ELSE NULL
          END AS certifiedCount,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) = 0 THEN 0
            WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_joined_placement_flag)
            ELSE NULL
          END AS joinedPlacementCount,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) = 0 THEN 0
            WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_employment_flag)
            ELSE NULL
          END AS employedCount,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
            WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2)
            ELSE NULL
          END AS completionRatePct,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
            WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2)
            ELSE NULL
          END AS certificationRatePct,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
            WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2)
            ELSE NULL
          END AS placementRatePct,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
            WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2)
            ELSE NULL
          END AS employmentRatePct,
          wage.avg_starting_wage AS avgStartingWage
        FROM vw_enrollment_outcome_fact v
        JOIN courses c ON c.course_id = v.course_id
        LEFT JOIN sectors sec ON sec.sector_id = c.sector_id
        LEFT JOIN (
          SELECT
            v_sal.course_id,
            CASE WHEN COUNT(DISTINCT sp.trainee_id) >= 5
                 THEN ROUND(AVG(sp.first_recorded_salary), 2)
                 ELSE NULL END AS avg_starting_wage
          FROM vw_salary_progression_fact sp
          JOIN vw_enrollment_outcome_fact v_sal ON v_sal.enrollment_id = sp.enrollment_id
          WHERE sp.first_recorded_salary IS NOT NULL
            AND v_sal.trainee_district_id = ?
        """ + salaryFilter.getSqlFragment() + """
          GROUP BY v_sal.course_id
        ) wage ON wage.course_id = c.course_id
        WHERE v.trainee_district_id = ?
        """ + filter.getSqlFragment() + """
          AND c.deleted_at IS NULL
        GROUP BY c.course_id, c.course_code, c.course_name, sec.sector_name, wage.avg_starting_wage
        ORDER BY enrollmentCount DESC, c.course_code ASC
        LIMIT ? OFFSET ?
        """;

    List<Object> params = new ArrayList<>();
    params.add(districtId);
    params.addAll(salaryFilter.getParameters());
    params.add(districtId);
    params.addAll(filter.getParameters());
    params.add(limit);
    params.add(offset);

    return jdbcTemplate.query(sql, (rs, rn) -> new DsdpExportRow(
        meta.districtId(),
        meta.districtCode(),
        meta.districtName(),
        meta.stateId(),
        meta.stateCode(),
        meta.stateName(),
        rs.getLong("courseId"),
        rs.getString("courseCode"),
        rs.getString("courseName"),
        rs.getString("sectorName"),
        (Number) rs.getObject("enrollmentCount") != null ? ((Number) rs.getObject("enrollmentCount")).longValue() : 0L,
        (Number) rs.getObject("traineeCount") != null ? ((Number) rs.getObject("traineeCount")).longValue() : 0L,
        (Number) rs.getObject("completedCount") != null ? ((Number) rs.getObject("completedCount")).longValue() : null,
        (Number) rs.getObject("certifiedCount") != null ? ((Number) rs.getObject("certifiedCount")).longValue() : null,
        (Number) rs.getObject("joinedPlacementCount") != null ? ((Number) rs.getObject("joinedPlacementCount")).longValue() : null,
        (Number) rs.getObject("employedCount") != null ? ((Number) rs.getObject("employedCount")).longValue() : null,
        rs.getBigDecimal("completionRatePct"),
        rs.getBigDecimal("certificationRatePct"),
        rs.getBigDecimal("placementRatePct"),
        rs.getBigDecimal("employmentRatePct"),
        rs.getBigDecimal("avgStartingWage")
    ), params.toArray());
  }

  // =========================================================================
  // 2. QPR Queries
  // =========================================================================

  public int countQprRows() {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM states WHERE deleted_at IS NULL",
        Integer.class
    );
    return count != null ? count : 0;
  }

  public Map<String, Object> fetchQprNationalSummary(
      LocalDate startDate,
      LocalDate endDate,
      AnalyticsFilterCriteria criteria) {
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String sql = """
        SELECT
          COUNT(*) AS totalEnrollments,
          COUNT(DISTINCT v.trainee_id) AS totalTrainees,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) = 0 THEN 0
            WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.is_completed_flag)
            ELSE NULL
          END AS totalCompleted,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) = 0 THEN 0
            WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_issued_certificate_flag)
            ELSE NULL
          END AS totalCertified,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) = 0 THEN 0
            WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_joined_placement_flag)
            ELSE NULL
          END AS totalJoinedPlacement,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) = 0 THEN 0
            WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_employment_flag)
            ELSE NULL
          END AS totalEmployed,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
            WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2)
            ELSE NULL
          END AS nationalCompletionRatePct,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
            WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2)
            ELSE NULL
          END AS nationalCertificationRatePct,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
            WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2)
            ELSE NULL
          END AS nationalPlacementRatePct,
          CASE
            WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
            WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2)
            ELSE NULL
          END AS nationalEmploymentRatePct
        FROM vw_enrollment_outcome_fact v
        WHERE v.enrollment_date >= ? AND v.enrollment_date <= ?
        """ + filter.getSqlFragment();

    List<Object> params = new ArrayList<>();
    params.add(startDate);
    params.add(endDate);
    params.addAll(filter.getParameters());

    return jdbcTemplate.queryForMap(sql, params.toArray());
  }

  public List<QprExportRow> fetchQprRows(
      LocalDate startDate,
      LocalDate endDate,
      String fiscalYear,
      String quarter,
      AnalyticsFilterCriteria criteria,
      int limit,
      int offset) {
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");

    String sql = """
        SELECT
          st.state_id AS stateId,
          st.state_code AS stateCode,
          st.state_name AS stateName,
          COALESCE(v.enrollment_count, 0) AS enrollmentCount,
          COALESCE(v.trainee_count, 0) AS traineeCount,
          CASE WHEN v.state_id IS NULL THEN 0 ELSE v.completed_count END AS completedCount,
          CASE WHEN v.state_id IS NULL THEN 0 ELSE v.certified_count END AS certifiedCount,
          CASE WHEN v.state_id IS NULL THEN 0 ELSE v.joined_placement_count END AS joinedPlacementCount,
          CASE WHEN v.state_id IS NULL THEN 0 ELSE v.employed_count END AS employedCount,
          v.completion_rate_pct AS completionRatePct,
          v.certification_rate_pct AS certificationRatePct,
          v.placement_rate_pct AS placementRatePct,
          v.employment_rate_pct AS employmentRatePct
        FROM states st
        LEFT JOIN (
          SELECT
            v.trainee_state_id AS state_id,
            COUNT(*) AS enrollment_count,
            COUNT(DISTINCT v.trainee_id) AS trainee_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) = 0 THEN 0
              WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.is_completed_flag)
              ELSE NULL
            END AS completed_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) = 0 THEN 0
              WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_issued_certificate_flag)
              ELSE NULL
            END AS certified_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) = 0 THEN 0
              WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_joined_placement_flag)
              ELSE NULL
            END AS joined_placement_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) = 0 THEN 0
              WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_employment_flag)
              ELSE NULL
            END AS employed_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
              WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2)
              ELSE NULL
            END AS completion_rate_pct,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
              WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2)
              ELSE NULL
            END AS certification_rate_pct,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
              WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2)
              ELSE NULL
            END AS placement_rate_pct,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
              WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2)
              ELSE NULL
            END AS employment_rate_pct
          FROM vw_enrollment_outcome_fact v
          WHERE v.enrollment_date >= ? AND v.enrollment_date <= ?
        """ + filter.getSqlFragment() + """
          GROUP BY v.trainee_state_id
        ) v ON v.state_id = st.state_id
        WHERE st.deleted_at IS NULL
        ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, enrollmentCount DESC, st.state_code ASC
        LIMIT ? OFFSET ?
        """;

    List<Object> params = new ArrayList<>();
    params.add(startDate);
    params.add(endDate);
    params.addAll(filter.getParameters());
    params.add(limit);
    params.add(offset);

    List<QprExportRow> rows = jdbcTemplate.query(sql, (rs, rn) -> new QprExportRow(
        rs.getLong("stateId"),
        rs.getString("stateCode"),
        rs.getString("stateName"),
        fiscalYear,
        quarter,
        (Number) rs.getObject("enrollmentCount") != null ? ((Number) rs.getObject("enrollmentCount")).longValue() : 0L,
        (Number) rs.getObject("traineeCount") != null ? ((Number) rs.getObject("traineeCount")).longValue() : 0L,
        (Number) rs.getObject("completedCount") != null ? ((Number) rs.getObject("completedCount")).longValue() : null,
        (Number) rs.getObject("certifiedCount") != null ? ((Number) rs.getObject("certifiedCount")).longValue() : null,
        (Number) rs.getObject("joinedPlacementCount") != null ? ((Number) rs.getObject("joinedPlacementCount")).longValue() : null,
        (Number) rs.getObject("employedCount") != null ? ((Number) rs.getObject("employedCount")).longValue() : null,
        rs.getBigDecimal("completionRatePct"),
        rs.getBigDecimal("certificationRatePct"),
        rs.getBigDecimal("placementRatePct"),
        rs.getBigDecimal("employmentRatePct"),
        offset + rn + 1
    ), params.toArray());

    return rows;
  }

  // =========================================================================
  // 3. Provider Scorecard Queries
  // =========================================================================

  public int countProviderScorecardRows(Long stateId, Long providerId) {
    StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM training_providers tp WHERE tp.deleted_at IS NULL");
    List<Object> params = new ArrayList<>();
    if (stateId != null) {
      sb.append(" AND tp.state_id = ?");
      params.add(stateId);
    }
    if (providerId != null) {
      sb.append(" AND tp.provider_id = ?");
      params.add(providerId);
    }
    Integer count = jdbcTemplate.queryForObject(sb.toString(), Integer.class, params.toArray());
    return count != null ? count : 0;
  }

  public List<ProviderScorecardExportRow> fetchProviderScorecardRows(
      Long stateId,
      Long providerId,
      AnalyticsFilterCriteria criteria,
      int limit,
      int offset) {
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");

    StringBuilder sb = new StringBuilder("""
        SELECT
          tp.provider_id AS providerId,
          tp.provider_code AS providerCode,
          tp.provider_name AS providerName,
          st.state_name AS stateName,
          d.district_name AS districtName,
          COALESCE(v.enrollment_count, 0) AS enrollmentCount,
          COALESCE(v.trainee_count, 0) AS traineeCount,
          CASE WHEN v.provider_id IS NULL THEN 0 ELSE v.completed_count END AS completedCount,
          CASE WHEN v.provider_id IS NULL THEN 0 ELSE v.certified_count END AS certifiedCount,
          CASE WHEN v.provider_id IS NULL THEN 0 ELSE v.joined_placement_count END AS joinedPlacementCount,
          CASE WHEN v.provider_id IS NULL THEN 0 ELSE v.employed_count END AS employedCount,
          v.completion_rate_pct AS completionRatePct,
          v.certification_rate_pct AS certificationRatePct,
          v.placement_rate_pct AS placementRatePct,
          v.employment_rate_pct AS employmentRatePct,
          COALESCE(act_cap.active_batch_capacity, 0) AS activeBatchCapacity
        FROM training_providers tp
        JOIN states st ON st.state_id = tp.state_id
        JOIN districts d ON d.district_id = tp.district_id
        LEFT JOIN (
          SELECT
            v.provider_id,
            COUNT(*) AS enrollment_count,
            COUNT(DISTINCT v.trainee_id) AS trainee_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) = 0 THEN 0
              WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.is_completed_flag)
              ELSE NULL
            END AS completed_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) = 0 THEN 0
              WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_issued_certificate_flag)
              ELSE NULL
            END AS certified_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) = 0 THEN 0
              WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_joined_placement_flag)
              ELSE NULL
            END AS joined_placement_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) = 0 THEN 0
              WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) >= 5 THEN SUM(v.has_employment_flag)
              ELSE NULL
            END AS employed_count,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
              WHEN COUNT(DISTINCT CASE WHEN v.is_completed_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2)
              ELSE NULL
            END AS completion_rate_pct,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
              WHEN COUNT(DISTINCT CASE WHEN v.has_issued_certificate_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2)
              ELSE NULL
            END AS certification_rate_pct,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
              WHEN COUNT(DISTINCT CASE WHEN v.has_joined_placement_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2)
              ELSE NULL
            END AS placement_rate_pct,
            CASE
              WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) = 0 THEN 0.00
              WHEN COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id END) >= 5 THEN ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2)
              ELSE NULL
            END AS employment_rate_pct
          FROM vw_enrollment_outcome_fact v
          WHERE 1=1
        """);
    sb.append(filter.getSqlFragment());
    sb.append("""
          GROUP BY v.provider_id
        ) v ON v.provider_id = tp.provider_id
        LEFT JOIN (
          SELECT
            tb.provider_id,
            SUM(tb.capacity) AS active_batch_capacity
          FROM training_batches tb
          JOIN ref_batch_status rbs ON rbs.batch_status_id = tb.batch_status_id
          WHERE rbs.status_code IN ('OPEN', 'ONGOING') AND tb.deleted_at IS NULL
          GROUP BY tb.provider_id
        ) act_cap ON act_cap.provider_id = tp.provider_id
        WHERE tp.deleted_at IS NULL
        """);

    List<Object> params = new ArrayList<>(filter.getParameters());
    if (stateId != null) {
      sb.append(" AND tp.state_id = ?");
      params.add(stateId);
    }
    if (providerId != null) {
      sb.append(" AND tp.provider_id = ?");
      params.add(providerId);
    }

    sb.append("\nORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, enrollmentCount DESC, tp.provider_code ASC, tp.provider_id ASC\nLIMIT ? OFFSET ?\n");
    params.add(limit);
    params.add(offset);

    return jdbcTemplate.query(sb.toString(), (rs, rn) -> new ProviderScorecardExportRow(
        rs.getLong("providerId"),
        rs.getString("providerCode"),
        rs.getString("providerName"),
        rs.getString("stateName"),
        rs.getString("districtName"),
        (Number) rs.getObject("activeBatchCapacity") != null ? ((Number) rs.getObject("activeBatchCapacity")).longValue() : 0L,
        (Number) rs.getObject("enrollmentCount") != null ? ((Number) rs.getObject("enrollmentCount")).longValue() : 0L,
        (Number) rs.getObject("traineeCount") != null ? ((Number) rs.getObject("traineeCount")).longValue() : 0L,
        (Number) rs.getObject("completedCount") != null ? ((Number) rs.getObject("completedCount")).longValue() : null,
        (Number) rs.getObject("certifiedCount") != null ? ((Number) rs.getObject("certifiedCount")).longValue() : null,
        (Number) rs.getObject("joinedPlacementCount") != null ? ((Number) rs.getObject("joinedPlacementCount")).longValue() : null,
        (Number) rs.getObject("employedCount") != null ? ((Number) rs.getObject("employedCount")).longValue() : null,
        rs.getBigDecimal("completionRatePct"),
        rs.getBigDecimal("certificationRatePct"),
        rs.getBigDecimal("placementRatePct"),
        rs.getBigDecimal("employmentRatePct"),
        offset + rn + 1
    ), params.toArray());
  }
}
