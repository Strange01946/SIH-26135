package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.repository.analytics.projection.SkillGapSeverityMixProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SkillGapSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.TopSkillGapProjection;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository executing formal analytical queries Q13 and Q14
 * for skill-gap rankings, deficiency levels, severity mix, and remediation uptake.
 *
 * <p>Implemented via {@link JdbcTemplate} to preserve Spring Data JPA repository invariants.
 */
@Repository
public class SkillGapDashboardRepository {

  private final JdbcTemplate jdbcTemplate;

  public SkillGapDashboardRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Executes formal Query Q13 to retrieve the top 20 current skill gaps,
   * joining {@code skills} for human-readable skill names and ordering deterministically.
   *
   * @return list of top skill gap projections
   */
  public List<TopSkillGapProjection> getFormalQ13TopSkillGaps() {
    String sql = """
        SELECT
          v.skill_id,
          v.skill_code,
          s.skill_name,
          v.skill_gap_severity_id,
          v.severity_code,
          v.gap_count,
          v.trainee_count,
          v.current_gap_count,
          v.avg_gap_level_delta
        FROM vw_skill_gap_summary v
        LEFT JOIN skills s ON s.skill_id = v.skill_id
        ORDER BY v.current_gap_count DESC, v.gap_count DESC, v.skill_id ASC, v.skill_gap_severity_id ASC
        LIMIT 20
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new TopSkillGapProjection(
        getNullableLong(rs, "skill_id"),
        rs.getString("skill_code"),
        rs.getString("skill_name"),
        getNullableLong(rs, "skill_gap_severity_id"),
        rs.getString("severity_code"),
        getNullableLong(rs, "gap_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "current_gap_count"),
        getScaledBigDecimal(rs, "avg_gap_level_delta")
    ));
  }

  /**
   * Executes formal Query Q14 to retrieve skill gap severity distribution,
   * affected trainee counts, and recommendation assignment & acceptance uptake mix.
   *
   * @return list of severity mix projections
   */
  public List<SkillGapSeverityMixProjection> getFormalQ14SeverityAndUptakeMix() {
    String sql = """
        SELECT
          v.severity_code,
          COALESCE(sev.severity_name, v.severity_code) AS severity_name,
          COUNT(*) AS current_gap_count,
          COUNT(DISTINCT v.trainee_id) AS trainee_count,
          COALESCE(SUM(v.recommendation_count), 0) AS recommendation_count,
          COALESCE(SUM(v.accepted_recommendation_count), 0) AS accepted_recommendation_count
        FROM vw_skill_gap_fact v
        LEFT JOIN ref_skill_gap_severity sev ON sev.severity_code = v.severity_code
        WHERE v.is_current = 1
        GROUP BY v.severity_code, sev.severity_name, sev.severity_rank
        ORDER BY current_gap_count DESC, sev.severity_rank ASC, v.severity_code ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new SkillGapSeverityMixProjection(
        rs.getString("severity_code"),
        rs.getString("severity_name"),
        getNullableLong(rs, "current_gap_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "recommendation_count"),
        getNullableLong(rs, "accepted_recommendation_count")
    ));
  }

  /**
   * Executes overall summary aggregation over {@code vw_skill_gap_fact} for current skill gaps.
   *
   * @return overall summary projection
   */
  public SkillGapSummaryProjection getFormalQ14OverallSummary() {
    String sql = """
        SELECT
          COUNT(*) AS total_current_gaps,
          COUNT(DISTINCT trainee_id) AS total_affected_trainees,
          COALESCE(SUM(recommendation_count), 0) AS total_recommendations,
          COALESCE(SUM(accepted_recommendation_count), 0) AS total_accepted_recommendations
        FROM vw_skill_gap_fact
        WHERE is_current = 1
        """;

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new SkillGapSummaryProjection(
        getNullableLong(rs, "total_current_gaps"),
        getNullableLong(rs, "total_affected_trainees"),
        getNullableLong(rs, "total_recommendations"),
        getNullableLong(rs, "total_accepted_recommendations")
    ));
  }

  private Long getNullableLong(ResultSet rs, String columnLabel) throws SQLException {
    long value = rs.getLong(columnLabel);
    return rs.wasNull() ? null : value;
  }

  private BigDecimal getScaledBigDecimal(ResultSet rs, String columnLabel) throws SQLException {
    BigDecimal value = rs.getBigDecimal(columnLabel);
    if (value == null) {
      return null;
    }
    return value.setScale(AnalyticsCalculationUtils.DEFAULT_PERCENTAGE_SCALE, AnalyticsCalculationUtils.DEFAULT_ROUNDING_MODE);
  }
}
