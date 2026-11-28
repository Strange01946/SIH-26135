package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.repository.analytics.util.AnalyticsQueryFilterBuilder;
import in.gov.sih.sih26135.repository.analytics.projection.CourseWageProgressionProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmployerHiringProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmploymentCategoryBreakdownProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmploymentMixProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SalaryBracketCountProjection;
import in.gov.sih.sih26135.repository.analytics.projection.WageProgressionProjection;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository executing formal analytical queries Q07–Q11 for employer hiring,
 * wage progression, economic uplift, and employment mix analytics.
 * Backed by {@link JdbcTemplate} to preserve Spring Data JPA repository invariants.
 */
@Repository
public class EconomicOutcomeDashboardRepository {

  private final JdbcTemplate jdbcTemplate;

  public EconomicOutcomeDashboardRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Executes formal Query Q07 to retrieve employer hiring performance leaderboard.
   * Excludes soft-deleted employers and applies deterministic tie-breaking.
   *
   * @return list of employer hiring performance projections
   */
  public List<EmployerHiringProjection> getFormalQ07EmployerLeaderboard() {
    return getFormalQ07EmployerLeaderboard(null);
  }

  public List<EmployerHiringProjection> getFormalQ07EmployerLeaderboard(AnalyticsFilterCriteria criteria) {
    var filter = cohort(criteria, "pf");
    String sql = """
        SELECT
          e.employer_id,
          e.employer_code,
          e.employer_name,
          COUNT(*) AS placement_count,
          COUNT(DISTINCT pf.trainee_id) AS trainee_count,
          SUM(pf.joined_flag) AS joined_count,
          SUM(pf.joined_flag) / COUNT(*) * 100 AS join_rate_pct,
          AVG(IF(pf.currency_code = 'INR' AND sf.frequency_code = 'MONTHLY', pf.joining_salary, NULL)) AS avg_joining_salary
        FROM vw_placement_fact pf
        JOIN employers e ON e.employer_id = pf.employer_id
        LEFT JOIN ref_salary_frequency sf ON sf.salary_frequency_id = pf.salary_frequency_id
        WHERE e.deleted_at IS NULL
        """ + filter.getSqlFragment() + """
        GROUP BY e.employer_id, e.employer_code, e.employer_name
        ORDER BY joined_count DESC, placement_count DESC, e.employer_code ASC, e.employer_id ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new EmployerHiringProjection(
        rs.getLong("employer_id"),
        rs.getString("employer_code"),
        rs.getString("employer_name"),
        getNullableLong(rs, "placement_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "joined_count"),
        getScaledBigDecimal(rs, "join_rate_pct"),
        getScaledBigDecimal(rs, "avg_joining_salary")
    ), filter.getParameters().toArray());
  }

  /**
   * Executes formal Query Q08 to retrieve national employment mix and spell duration analytics.
   *
   * @return employment mix projection
   */
  public EmploymentMixProjection getFormalQ08EmploymentMix() {
    return getFormalQ08EmploymentMix(null);
  }

  public EmploymentMixProjection getFormalQ08EmploymentMix(AnalyticsFilterCriteria criteria) {
    var filter = cohort(criteria, "ef");
    String sql = """
        SELECT
          COUNT(*) AS employment_spell_count,
          COUNT(DISTINCT trainee_id) AS trainee_count,
          %s AS current_spell_count,
          %s AS wage_spell_count,
          %s AS self_employment_spell_count,
          %s AS apprenticeship_spell_count,
          %s AS internship_spell_count,
          %s AS current_wage_count,
          %s AS current_self_employment_count,
          AVG(duration_days) AS avg_duration_days,
          AVG(CASE WHEN is_current = 1 THEN duration_days END) AS avg_current_spell_duration_days
        FROM vw_employment_fact ef WHERE 1 = 1
        """.formatted(countWhere("is_current = 1"), countWhere("is_wage_employment = 1"),
            countWhere("is_self_employment = 1"), countWhere("is_apprenticeship = 1"), countWhere("is_internship = 1"),
            countWhere("is_current = 1 AND is_wage_employment = 1"), countWhere("is_current = 1 AND is_self_employment = 1"))
        + filter.getSqlFragment();

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new EmploymentMixProjection(
        getNullableLong(rs, "employment_spell_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "current_spell_count"),
        getNullableLong(rs, "wage_spell_count"),
        getNullableLong(rs, "self_employment_spell_count"),
        getNullableLong(rs, "apprenticeship_spell_count"),
        getNullableLong(rs, "internship_spell_count"),
        getNullableLong(rs, "current_wage_count"),
        getNullableLong(rs, "current_self_employment_count"),
        getScaledBigDecimal(rs, "avg_duration_days"),
        getScaledBigDecimal(rs, "avg_current_spell_duration_days")
    ), filter.getParameters().toArray());
  }

  /**
   * Executes formal Query Q09 to retrieve engagement type category breakdown.
   *
   * @return list of category breakdown projections
   */
  public List<EmploymentCategoryBreakdownProjection> getFormalQ09CategoryBreakdown() {
    return getFormalQ09CategoryBreakdown(null);
  }

  public List<EmploymentCategoryBreakdownProjection> getFormalQ09CategoryBreakdown(AnalyticsFilterCriteria criteria) {
    var filter = cohort(criteria, "ef");
    String sql = """
        SELECT
          ef.engagement_type_code,
          COALESCE(ret.type_name, ef.engagement_type_code) AS engagement_type_name,
          ef.is_wage_employment,
          ef.is_self_employment,
          ef.is_apprenticeship,
          ef.is_internship,
          COUNT(*) AS spell_count,
          COUNT(DISTINCT ef.trainee_id) AS trainee_count,
          %s AS current_spell_count,
          AVG(ef.duration_days) AS avg_duration_days
        FROM vw_employment_fact ef
        LEFT JOIN ref_engagement_type ret ON ret.type_code = ef.engagement_type_code
        WHERE 1 = 1
        """.formatted(countWhere("ef.is_current = 1")) + filter.getSqlFragment() + """
        GROUP BY
          ef.engagement_type_code,
          ret.type_name,
          ef.is_wage_employment,
          ef.is_self_employment,
          ef.is_apprenticeship,
          ef.is_internship
        ORDER BY spell_count DESC, ef.engagement_type_code ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new EmploymentCategoryBreakdownProjection(
        rs.getString("engagement_type_code"),
        rs.getString("engagement_type_name"),
        rs.getBoolean("is_wage_employment"),
        rs.getBoolean("is_self_employment"),
        rs.getBoolean("is_apprenticeship"),
        rs.getBoolean("is_internship"),
        getNullableLong(rs, "spell_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "current_spell_count"),
        getScaledBigDecimal(rs, "avg_duration_days")
    ), filter.getParameters().toArray());
  }

  /**
   * Executes formal Query Q10 to retrieve aggregate wage progression and salary change KPIs.
   *
   * @return wage progression projection
   */
  public WageProgressionProjection getFormalQ10WageProgression() {
    return getFormalQ10WageProgression(null);
  }

  public WageProgressionProjection getFormalQ10WageProgression(AnalyticsFilterCriteria criteria) {
    var filter = cohort(criteria, "sp");
    String sql = """
        SELECT
          COUNT(*) AS employment_spell_count,
          COUNT(DISTINCT IF(%1$s, sp.trainee_id, NULL)) AS first_salary_count,
          COUNT(DISTINCT IF(%2$s, sp.trainee_id, NULL)) AS latest_salary_count,
          AVG(IF(%1$s, sp.first_recorded_salary, NULL)) AS avg_first_recorded_salary,
          AVG(IF(%2$s, sp.latest_recorded_salary, NULL)) AS avg_latest_recorded_salary,
          AVG(IF(%1$s AND %2$s, sp.salary_change_amount, NULL)) AS avg_salary_change_amount,
          AVG(IF(%1$s AND %2$s, sp.salary_change_pct, NULL)) AS avg_salary_change_pct
        """.formatted(FIRST_MONTHLY, LATEST_MONTHLY) + SALARY_SOURCE + filter.getSqlFragment();

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new WageProgressionProjection(
        getNullableLong(rs, "employment_spell_count"),
        getNullableLong(rs, "first_salary_count"),
        getNullableLong(rs, "latest_salary_count"),
        getScaledBigDecimal(rs, "avg_first_recorded_salary"),
        getScaledBigDecimal(rs, "avg_latest_recorded_salary"),
        getScaledBigDecimal(rs, "avg_salary_change_amount"),
        getScaledBigDecimal(rs, "avg_salary_change_pct")
    ), filter.getParameters().toArray());
  }

  /**
   * Retrieves salary observation counts grouped by the mandated salary brackets.
   * Excludes null salaries and partitions non-null observations with explicit boundaries.
   *
   * @return list of salary bracket count projections
   */
  public List<SalaryBracketCountProjection> getSalaryDistributionCounts() {
    return getSalaryDistributionCounts(null);
  }

  public List<SalaryBracketCountProjection> getSalaryDistributionCounts(AnalyticsFilterCriteria criteria) {
    var filter = cohort(criteria, "sp");
    String sql = """
        SELECT
          CASE
            WHEN latest_recorded_salary < 15000 THEN 'BELOW_15K'
            WHEN latest_recorded_salary <= 25000 THEN 'FROM_15K_TO_25K'
            WHEN latest_recorded_salary <= 50000 THEN 'FROM_25K_TO_50K'
            ELSE 'ABOVE_50K'
          END AS bracket_code,
          COUNT(DISTINCT sp.trainee_id) AS count
        """ + SALARY_SOURCE + " AND " + LATEST_MONTHLY + "\n" + filter.getSqlFragment() + """
        GROUP BY
          CASE
            WHEN latest_recorded_salary < 15000 THEN 'BELOW_15K'
            WHEN latest_recorded_salary <= 25000 THEN 'FROM_15K_TO_25K'
            WHEN latest_recorded_salary <= 50000 THEN 'FROM_25K_TO_50K'
            ELSE 'ABOVE_50K'
          END
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new SalaryBracketCountProjection(
        rs.getString("bracket_code"),
        getNullableLong(rs, "count")
    ), filter.getParameters().toArray());
  }

  /**
   * Executes formal Query Q11 to retrieve course-level wage progression and salary change.
   *
   * @return list of course wage progression projections
   */
  public List<CourseWageProgressionProjection> getFormalQ11CourseWageProgression() {
    return getFormalQ11CourseWageProgression(null);
  }

  public List<CourseWageProgressionProjection> getFormalQ11CourseWageProgression(AnalyticsFilterCriteria criteria) {
    var filter = cohort(criteria, "sp");
    String sql = """
        SELECT
          sp.course_id, c.course_code, c.course_name,
          COUNT(*) AS spell_count,
          AVG(IF(%1$s, sp.first_recorded_salary, NULL)) AS avg_first_recorded_salary,
          AVG(IF(%2$s, sp.latest_recorded_salary, NULL)) AS avg_latest_recorded_salary,
          AVG(IF(%1$s AND %2$s, sp.salary_change_pct, NULL)) AS avg_salary_change_pct
        """.formatted(FIRST_MONTHLY, LATEST_MONTHLY)
        + SALARY_SOURCE.replace("WHERE tr.deleted_at IS NULL", "JOIN courses c ON c.course_id = sp.course_id WHERE tr.deleted_at IS NULL AND c.deleted_at IS NULL")
        + filter.getSqlFragment() + """
        GROUP BY sp.course_id, c.course_code, c.course_name
        ORDER BY avg_salary_change_pct IS NULL, avg_salary_change_pct DESC, c.course_code ASC, sp.course_id ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new CourseWageProgressionProjection(
        rs.getLong("course_id"),
        rs.getString("course_code"),
        rs.getString("course_name"),
        getNullableLong(rs, "spell_count"),
        getScaledBigDecimal(rs, "avg_first_recorded_salary"),
        getScaledBigDecimal(rs, "avg_latest_recorded_salary"),
        getScaledBigDecimal(rs, "avg_salary_change_pct")
    ), filter.getParameters().toArray());
  }

  private static final String FIRST_MONTHLY = "first_sh.currency_code = 'INR' AND first_freq.frequency_code = 'MONTHLY'";
  private static final String LATEST_MONTHLY = "latest_sh.currency_code = 'INR' AND sp.latest_salary_frequency_code = 'MONTHLY'";
  private static final String SALARY_SOURCE = """
      FROM vw_salary_progression_fact sp
      JOIN trainees tr ON tr.trainee_id = sp.trainee_id
      LEFT JOIN salary_history first_sh ON first_sh.employment_id = sp.employment_id
          AND first_sh.effective_from = sp.first_salary_effective_from
      LEFT JOIN ref_salary_frequency first_freq ON first_freq.salary_frequency_id = first_sh.salary_frequency_id
      LEFT JOIN salary_history latest_sh ON latest_sh.employment_id = sp.employment_id
          AND latest_sh.effective_from = sp.latest_salary_effective_from
      WHERE tr.deleted_at IS NULL
      """;

  private static AnalyticsQueryFilterBuilder.FilterPredicateResult cohort(AnalyticsFilterCriteria criteria, String alias) {
    var filter = AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    if (!filter.hasPredicates()) {
      return filter;
    }
    return new AnalyticsQueryFilterBuilder.FilterPredicateResult(
        " AND EXISTS (SELECT 1 FROM vw_enrollment_outcome_fact v WHERE v.enrollment_id = "
            + alias + ".enrollment_id " + filter.getSqlFragment() + ")", filter.getParameters());
  }

  private static String countWhere(String condition) {
    return "COALESCE(SUM(IF(" + condition + ", 1, 0)), 0)";
  }

  private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private static BigDecimal getScaledBigDecimal(ResultSet rs, String column) throws SQLException {
    BigDecimal val = rs.getBigDecimal(column);
    if (val == null) {
      return null;
    }
    return val.setScale(AnalyticsCalculationUtils.DEFAULT_PERCENTAGE_SCALE, AnalyticsCalculationUtils.DEFAULT_ROUNDING_MODE);
  }
}
