package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.repository.analytics.projection.CourseEmployabilityProjection;
import in.gov.sih.sih26135.repository.analytics.projection.ProviderPerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.util.AnalyticsQueryFilterBuilder;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Dedicated analytical query repository for Institutional & Curriculum Performance Dashboards.
 * Executes formal analytical SQL queries Q04 and Q05 via high-performance {@link JdbcTemplate}
 * against immutable database views and catalog tables without altering {@code JpaRepository} contracts.
 */
@Repository
public class InstitutionalAnalyticsRepository {

  private static final String Q04_PROVIDER_LEADERBOARD_SQL = """
      SELECT
        tp.provider_id AS providerId,
        tp.provider_code AS providerCode,
        tp.provider_name AS providerName,
        v.enrollment_count AS enrollmentCount,
        v.trainee_count AS traineeCount,
        v.completed_count AS completedCount,
        v.certified_count AS certifiedCount,
        v.joined_placement_count AS joinedPlacementCount,
        v.employed_count AS employedCount,
        v.completion_rate_pct AS completionRatePct,
        v.certification_rate_pct AS certificationRatePct,
        v.placement_rate_pct AS placementRatePct,
        v.employment_rate_pct AS employmentRatePct,
        COALESCE(act_cap.active_batch_capacity, 0) AS activeBatchCapacity
      FROM vw_provider_outcome_summary v
      JOIN training_providers tp ON tp.provider_id = v.provider_id
      LEFT JOIN (
        SELECT
          tb.provider_id,
          SUM(tb.capacity) AS active_batch_capacity
        FROM training_batches tb
        JOIN ref_batch_status rbs ON rbs.batch_status_id = tb.batch_status_id
        WHERE rbs.status_code IN ('OPEN', 'ONGOING')
          AND tb.deleted_at IS NULL
        GROUP BY tb.provider_id
      ) act_cap ON act_cap.provider_id = tp.provider_id
      WHERE tp.deleted_at IS NULL
      ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC, tp.provider_code ASC, tp.provider_id ASC
      """;

  private static final String Q05_COURSE_EMPLOYABILITY_SQL = """
      SELECT
        c.course_id AS courseId,
        c.course_code AS courseCode,
        c.course_name AS courseName,
        ql.qualification_level_id AS qualificationLevelId,
        ql.level_code AS qualificationLevelCode,
        ql.level_name AS qualificationLevelName,
        ql.nsqf_level AS nsqfLevel,
        v.enrollment_count AS enrollmentCount,
        v.trainee_count AS traineeCount,
        v.completed_count AS completedCount,
        v.certified_count AS certifiedCount,
        v.joined_placement_count AS joinedPlacementCount,
        v.employed_count AS employedCount,
        v.completion_rate_pct AS completionRatePct,
        v.certification_rate_pct AS certificationRatePct,
        v.placement_rate_pct AS placementRatePct,
        v.employment_rate_pct AS employmentRatePct,
        wage.avg_starting_wage AS avgStartingWage
      FROM vw_course_outcome_summary v
      JOIN courses c ON c.course_id = v.course_id
      LEFT JOIN ref_qualification_level ql ON ql.qualification_level_id = c.qualification_level_id
      LEFT JOIN (
        SELECT
          sp.course_id,
          ROUND(AVG(sp.first_recorded_salary), 2) AS avg_starting_wage
        FROM vw_salary_progression_fact sp
        WHERE sp.course_id IS NOT NULL AND sp.first_recorded_salary IS NOT NULL
        GROUP BY sp.course_id
      ) wage ON wage.course_id = c.course_id
      WHERE c.deleted_at IS NULL
      ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC, c.course_code ASC, c.course_id ASC
      """;

  private static final RowMapper<ProviderPerformanceProjection> PROVIDER_ROW_MAPPER = (rs, rowNum) ->
      new ProviderPerformanceProjection(
          rs.getLong("providerId"),
          rs.getString("providerCode"),
          rs.getString("providerName"),
          (Number) rs.getObject("enrollmentCount"),
          (Number) rs.getObject("traineeCount"),
          (Number) rs.getObject("completedCount"),
          (Number) rs.getObject("certifiedCount"),
          (Number) rs.getObject("joinedPlacementCount"),
          (Number) rs.getObject("employedCount"),
          rs.getBigDecimal("completionRatePct"),
          rs.getBigDecimal("certificationRatePct"),
          rs.getBigDecimal("placementRatePct"),
          rs.getBigDecimal("employmentRatePct"),
          (Number) rs.getObject("activeBatchCapacity")
      );

  private static final RowMapper<CourseEmployabilityProjection> COURSE_ROW_MAPPER = (rs, rowNum) ->
      new CourseEmployabilityProjection(
          rs.getLong("courseId"),
          rs.getString("courseCode"),
          rs.getString("courseName"),
          (Long) rs.getObject("qualificationLevelId"),
          rs.getString("qualificationLevelCode"),
          rs.getString("qualificationLevelName"),
          (Integer) rs.getObject("nsqfLevel"),
          (Number) rs.getObject("enrollmentCount"),
          (Number) rs.getObject("traineeCount"),
          (Number) rs.getObject("completedCount"),
          (Number) rs.getObject("certifiedCount"),
          (Number) rs.getObject("joinedPlacementCount"),
          (Number) rs.getObject("employedCount"),
          rs.getBigDecimal("completionRatePct"),
          rs.getBigDecimal("certificationRatePct"),
          rs.getBigDecimal("placementRatePct"),
          rs.getBigDecimal("employmentRatePct"),
          rs.getBigDecimal("avgStartingWage")
      );

  private final JdbcTemplate jdbcTemplate;

  public InstitutionalAnalyticsRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Q04: Training provider performance leaderboard ordered by employment rate descending,
   * tie-broken by enrollment volume descending.
   */
  public List<ProviderPerformanceProjection> getFormalQ04ProviderLeaderboard() {
    return jdbcTemplate.query(Q04_PROVIDER_LEADERBOARD_SQL, PROVIDER_ROW_MAPPER);
  }

  /**
   * Q05: Course outcome and employability scorecard ordered by employment conversion descending,
   * tie-broken by enrollment volume descending.
   */
  public List<CourseEmployabilityProjection> getFormalQ05CourseEmployability() {
    return jdbcTemplate.query(Q05_COURSE_EMPLOYABILITY_SQL, COURSE_ROW_MAPPER);
  }

  /**
   * Q04: Training provider performance leaderboard with optional multi-dimensional filters.
   */
  public List<ProviderPerformanceProjection> getFormalQ04ProviderLeaderboard(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getFormalQ04ProviderLeaderboard();
    }
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String sql = """
        SELECT
          tp.provider_id AS providerId,
          tp.provider_code AS providerCode,
          tp.provider_name AS providerName,
          COUNT(*) AS enrollmentCount,
          COUNT(DISTINCT v.trainee_id) AS traineeCount,
          SUM(v.is_completed_flag) AS completedCount,
          SUM(v.has_issued_certificate_flag) AS certifiedCount,
          SUM(v.has_joined_placement_flag) AS joinedPlacementCount,
          SUM(v.has_employment_flag) AS employedCount,
          ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completionRatePct,
          ROUND(SUM(v.has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certificationRatePct,
          ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placementRatePct,
          ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employmentRatePct,
          COALESCE(act_cap.active_batch_capacity, 0) AS activeBatchCapacity
        FROM vw_enrollment_outcome_fact v
        JOIN training_providers tp ON tp.provider_id = v.provider_id
        LEFT JOIN (
          SELECT
            tb.provider_id,
            SUM(tb.capacity) AS active_batch_capacity
          FROM training_batches tb
          JOIN ref_batch_status rbs ON rbs.batch_status_id = tb.batch_status_id
          WHERE rbs.status_code IN ('OPEN', 'ONGOING')
            AND tb.deleted_at IS NULL
          GROUP BY tb.provider_id
        ) act_cap ON act_cap.provider_id = tp.provider_id
        WHERE tp.deleted_at IS NULL
        """ + filter.getSqlFragment() + """
        GROUP BY tp.provider_id, tp.provider_code, tp.provider_name, act_cap.active_batch_capacity
        ORDER BY employmentRatePct IS NULL, employmentRatePct DESC, enrollmentCount DESC, tp.provider_code ASC, tp.provider_id ASC
        """;
    return jdbcTemplate.query(sql, PROVIDER_ROW_MAPPER, filter.getParameters().toArray());
  }

  /**
   * Q05: Course outcome and employability scorecard with optional multi-dimensional filters.
   */
  public List<CourseEmployabilityProjection> getFormalQ05CourseEmployability(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getFormalQ05CourseEmployability();
    }
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    AnalyticsQueryFilterBuilder.FilterPredicateResult salaryFilter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v_sal");

    List<Object> allParams = new ArrayList<>();
    allParams.addAll(salaryFilter.getParameters());
    allParams.addAll(filter.getParameters());

    String sql = """
        SELECT
          c.course_id AS courseId,
          c.course_code AS courseCode,
          c.course_name AS courseName,
          ql.qualification_level_id AS qualificationLevelId,
          ql.level_code AS qualificationLevelCode,
          ql.level_name AS qualificationLevelName,
          ql.nsqf_level AS nsqfLevel,
          COUNT(*) AS enrollmentCount,
          COUNT(DISTINCT v.trainee_id) AS traineeCount,
          SUM(v.is_completed_flag) AS completedCount,
          SUM(v.has_issued_certificate_flag) AS certifiedCount,
          SUM(v.has_joined_placement_flag) AS joinedPlacementCount,
          SUM(v.has_employment_flag) AS employedCount,
          ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completionRatePct,
          ROUND(SUM(v.has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certificationRatePct,
          ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placementRatePct,
          ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employmentRatePct,
          wage.avg_starting_wage AS avgStartingWage
        FROM vw_enrollment_outcome_fact v
        JOIN courses c ON c.course_id = v.course_id
        LEFT JOIN ref_qualification_level ql ON ql.qualification_level_id = c.qualification_level_id
        LEFT JOIN (
          SELECT
            v_sal.course_id,
            ROUND(AVG(sp.first_recorded_salary), 2) AS avg_starting_wage
          FROM vw_salary_progression_fact sp
          JOIN vw_enrollment_outcome_fact v_sal ON v_sal.enrollment_id = sp.enrollment_id
          WHERE sp.first_recorded_salary IS NOT NULL
        """ + salaryFilter.getSqlFragment() + """
          GROUP BY v_sal.course_id
        ) wage ON wage.course_id = c.course_id
        WHERE c.deleted_at IS NULL
        """ + filter.getSqlFragment() + """
        GROUP BY c.course_id, c.course_code, c.course_name, ql.qualification_level_id, ql.level_code, ql.level_name, ql.nsqf_level, wage.avg_starting_wage
        ORDER BY employmentRatePct IS NULL, employmentRatePct DESC, enrollmentCount DESC, c.course_code ASC, c.course_id ASC
        """;
    return jdbcTemplate.query(sql, COURSE_ROW_MAPPER, allParams.toArray());
  }

  /**
   * Checks whether a training provider exists and is not soft-deleted.
   */
  public boolean existsProviderById(Long providerId) {
    if (providerId == null) {
      return false;
    }
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM training_providers WHERE provider_id = ? AND deleted_at IS NULL",
        Integer.class,
        providerId
    );
    return count != null && count > 0;
  }

  /**
   * Checks whether a course exists and is not soft-deleted.
   */
  public boolean existsCourseById(Long courseId) {
    if (courseId == null) {
      return false;
    }
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM courses WHERE course_id = ? AND deleted_at IS NULL",
        Integer.class,
        courseId
    );
    return count != null && count > 0;
  }
}
