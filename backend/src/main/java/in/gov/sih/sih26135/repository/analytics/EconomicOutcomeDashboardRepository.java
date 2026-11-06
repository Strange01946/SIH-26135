package in.gov.sih.sih26135.repository.analytics;

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
    String sql = """
        SELECT
          e.employer_id,
          e.employer_code,
          e.employer_name,
          v.placement_count,
          v.trainee_count,
          v.joined_count,
          v.join_rate_pct,
          CASE WHEN sal.salary_contributors >= 5 THEN v.avg_joining_salary ELSE NULL END AS avg_joining_salary
        FROM vw_employer_hiring_summary v
        JOIN employers e ON e.employer_id = v.employer_id
        LEFT JOIN (
          SELECT employer_id, COUNT(DISTINCT trainee_id) AS salary_contributors
          FROM vw_placement_fact
          WHERE employer_id IS NOT NULL AND joining_salary IS NOT NULL
          GROUP BY employer_id
        ) sal ON sal.employer_id = e.employer_id
        WHERE e.deleted_at IS NULL
        ORDER BY v.joined_count DESC, v.placement_count DESC, e.employer_code ASC, e.employer_id ASC
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
    ));
  }

  /**
   * Executes formal Query Q08 to retrieve national employment mix and spell duration analytics.
   *
   * @return employment mix projection
   */
  public EmploymentMixProjection getFormalQ08EmploymentMix() {
    String sql = """
        SELECT
          COUNT(*) AS employment_spell_count,
          COUNT(DISTINCT trainee_id) AS trainee_count,
          SUM(CASE WHEN is_current = 1 THEN 1 ELSE 0 END) AS current_spell_count,
          SUM(CASE WHEN is_wage_employment = 1 THEN 1 ELSE 0 END) AS wage_spell_count,
          SUM(CASE WHEN is_self_employment = 1 THEN 1 ELSE 0 END) AS self_employment_spell_count,
          SUM(CASE WHEN is_apprenticeship = 1 THEN 1 ELSE 0 END) AS apprenticeship_spell_count,
          SUM(CASE WHEN is_internship = 1 THEN 1 ELSE 0 END) AS internship_spell_count,
          SUM(CASE WHEN is_current = 1 AND is_wage_employment = 1 THEN 1 ELSE 0 END) AS current_wage_count,
          SUM(CASE WHEN is_current = 1 AND is_self_employment = 1 THEN 1 ELSE 0 END) AS current_self_employment_count,
          AVG(duration_days) AS avg_duration_days,
          AVG(CASE WHEN is_current = 1 THEN duration_days END) AS avg_current_spell_duration_days
        FROM vw_employment_fact
        """;

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
    ));
  }

  /**
   * Executes formal Query Q09 to retrieve engagement type category breakdown.
   *
   * @return list of category breakdown projections
   */
  public List<EmploymentCategoryBreakdownProjection> getFormalQ09CategoryBreakdown() {
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
          SUM(ef.is_current) AS current_spell_count,
          AVG(ef.duration_days) AS avg_duration_days
        FROM vw_employment_fact ef
        LEFT JOIN ref_engagement_type ret ON ret.type_code = ef.engagement_type_code
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
    ));
  }

  /**
   * Executes formal Query Q10 to retrieve aggregate wage progression and salary change KPIs.
   *
   * @return wage progression projection
   */
  public WageProgressionProjection getFormalQ10WageProgression() {
    String sql = """
        SELECT
          COUNT(*) AS employment_spell_count,
          COUNT(DISTINCT CASE WHEN first_recorded_salary IS NOT NULL THEN trainee_id ELSE NULL END) AS first_salary_count,
          COUNT(DISTINCT CASE WHEN latest_recorded_salary IS NOT NULL THEN trainee_id ELSE NULL END) AS latest_salary_count,
          AVG(first_recorded_salary) AS avg_first_recorded_salary,
          AVG(latest_recorded_salary) AS avg_latest_recorded_salary,
          AVG(salary_change_amount) AS avg_salary_change_amount,
          AVG(salary_change_pct) AS avg_salary_change_pct
        FROM vw_salary_progression_fact
        """;

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new WageProgressionProjection(
        getNullableLong(rs, "employment_spell_count"),
        getNullableLong(rs, "first_salary_count"),
        getNullableLong(rs, "latest_salary_count"),
        getScaledBigDecimal(rs, "avg_first_recorded_salary"),
        getScaledBigDecimal(rs, "avg_latest_recorded_salary"),
        getScaledBigDecimal(rs, "avg_salary_change_amount"),
        getScaledBigDecimal(rs, "avg_salary_change_pct")
    ));
  }

  /**
   * Retrieves salary observation counts grouped by the mandated salary brackets.
   * Excludes null salaries and partitions non-null observations with explicit boundaries.
   *
   * @return list of salary bracket count projections
   */
  public List<SalaryBracketCountProjection> getSalaryDistributionCounts() {
    String sql = """
        SELECT
          CASE
            WHEN latest_recorded_salary < 15000 THEN 'BELOW_15K'
            WHEN latest_recorded_salary <= 25000 THEN 'FROM_15K_TO_25K'
            WHEN latest_recorded_salary <= 50000 THEN 'FROM_25K_TO_50K'
            ELSE 'ABOVE_50K'
          END AS bracket_code,
          COUNT(*) AS count
        FROM vw_salary_progression_fact
        WHERE latest_recorded_salary IS NOT NULL
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
    ));
  }

  /**
   * Executes formal Query Q11 to retrieve course-level wage progression and salary change.
   *
   * @return list of course wage progression projections
   */
  public List<CourseWageProgressionProjection> getFormalQ11CourseWageProgression() {
    String sql = """
        SELECT
          sp.course_id,
          c.course_code,
          c.course_name,
          COUNT(*) AS spell_count,
          CASE WHEN COUNT(DISTINCT CASE WHEN sp.first_recorded_salary IS NOT NULL THEN sp.trainee_id ELSE NULL END) >= 5
               THEN AVG(sp.first_recorded_salary) ELSE NULL END AS avg_first_recorded_salary,
          CASE WHEN COUNT(DISTINCT CASE WHEN sp.latest_recorded_salary IS NOT NULL THEN sp.trainee_id ELSE NULL END) >= 5
               THEN AVG(sp.latest_recorded_salary) ELSE NULL END AS avg_latest_recorded_salary,
          CASE WHEN COUNT(DISTINCT CASE WHEN sp.salary_change_pct IS NOT NULL THEN sp.trainee_id ELSE NULL END) >= 5
               THEN AVG(sp.salary_change_pct) ELSE NULL END AS avg_salary_change_pct
        FROM vw_salary_progression_fact sp
        LEFT JOIN courses c ON c.course_id = sp.course_id
        WHERE sp.course_id IS NOT NULL
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
    ));
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
