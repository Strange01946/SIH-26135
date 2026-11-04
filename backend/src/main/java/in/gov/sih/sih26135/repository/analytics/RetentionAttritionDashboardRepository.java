package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.repository.analytics.projection.AttritionReasonProjection;
import in.gov.sih.sih26135.repository.analytics.projection.RetentionSurvivalProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SeparationAnalysisProjection;
import in.gov.sih.sih26135.repository.analytics.projection.UnemploymentReasonProjection;
import in.gov.sih.sih26135.repository.analytics.projection.UnemploymentSummaryProjection;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository executing formal analytical queries Q12, Q15, Q16, Q17, and Q18
 * for longitudinal retention survival, unemployment duration, re-employment conversion,
 * and employment exit/attrition root-cause analytics.
 *
 * <p>Implemented via {@link JdbcTemplate} to preserve Spring Data JPA repository invariants.
 */
@Repository
public class RetentionAttritionDashboardRepository {

  private final JdbcTemplate jdbcTemplate;

  public RetentionAttritionDashboardRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Executes formal Query Q12 to retrieve multi-milestone retention survival metrics.
   * NULL flags representing milestones not yet reached are safely excluded from observable counts.
   *
   * @return retention survival projection
   */
  public RetentionSurvivalProjection getFormalQ12RetentionSurvival() {
    String sql = """
        SELECT
          COUNT(*) AS employment_spell_count,
          COUNT(retained_6m_flag) AS observable_6m_count,
          SUM(retained_6m_flag) AS retained_6m_count,
          ROUND(SUM(retained_6m_flag) / NULLIF(COUNT(retained_6m_flag), 0) * 100, 2) AS retention_6m_pct,
          COUNT(retained_12m_flag) AS observable_12m_count,
          SUM(retained_12m_flag) AS retained_12m_count,
          ROUND(SUM(retained_12m_flag) / NULLIF(COUNT(retained_12m_flag), 0) * 100, 2) AS retention_12m_pct,
          COUNT(retained_24m_flag) AS observable_24m_count,
          SUM(retained_24m_flag) AS retained_24m_count,
          ROUND(SUM(retained_24m_flag) / NULLIF(COUNT(retained_24m_flag), 0) * 100, 2) AS retention_24m_pct,
          COUNT(retained_36m_flag) AS observable_36m_count,
          SUM(retained_36m_flag) AS retained_36m_count,
          ROUND(SUM(retained_36m_flag) / NULLIF(COUNT(retained_36m_flag), 0) * 100, 2) AS retention_36m_pct
        FROM vw_employment_retention_fact
        """;

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new RetentionSurvivalProjection(
        getNullableLong(rs, "employment_spell_count"),
        getNullableLong(rs, "observable_6m_count"),
        getNullableLong(rs, "retained_6m_count"),
        getScaledBigDecimal(rs, "retention_6m_pct"),
        getNullableLong(rs, "observable_12m_count"),
        getNullableLong(rs, "retained_12m_count"),
        getScaledBigDecimal(rs, "retention_12m_pct"),
        getNullableLong(rs, "observable_24m_count"),
        getNullableLong(rs, "retained_24m_count"),
        getScaledBigDecimal(rs, "retention_24m_pct"),
        getNullableLong(rs, "observable_36m_count"),
        getNullableLong(rs, "retained_36m_count"),
        getScaledBigDecimal(rs, "retention_36m_pct")
    ));
  }

  /**
   * Executes formal Query Q15 to retrieve aggregate unemployment metrics, spell counts,
   * duration statistics, and re-employment conversion from {@code vw_unemployment_fact}.
   *
   * @return unemployment summary projection
   */
  public UnemploymentSummaryProjection getFormalQ15UnemploymentSummary() {
    String sql = """
        SELECT
          COUNT(*) AS unemployment_period_count,
          COUNT(DISTINCT trainee_id) AS trainee_count,
          SUM(is_current) AS current_unemployment_count,
          SUM(reemployed_flag) AS reemployed_count,
          SUM(never_preceded_by_employment_flag) AS never_preceded_by_employment_count,
          AVG(duration_days) AS avg_duration_days,
          AVG(CASE WHEN is_current = 1 THEN duration_days END) AS avg_open_period_duration_days
        FROM vw_unemployment_fact
        """;

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new UnemploymentSummaryProjection(
        getNullableLong(rs, "unemployment_period_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "current_unemployment_count"),
        getNullableLong(rs, "reemployed_count"),
        getNullableLong(rs, "never_preceded_by_employment_count"),
        getScaledBigDecimal(rs, "avg_duration_days"),
        getScaledBigDecimal(rs, "avg_open_period_duration_days")
    ));
  }

  /**
   * Executes formal Query Q16 to retrieve unemployment reason breakdowns,
   * period counts, affected trainee counts, ongoing counts, and re-employment observations.
   *
   * @return list of unemployment reason projections ordered deterministically
   */
  public List<UnemploymentReasonProjection> getFormalQ16UnemploymentReasons() {
    String sql = """
        SELECT
          ur.unemployment_reason_id,
          ur.unemployment_reason_code,
          COALESCE(rur.reason_name, ur.unemployment_reason_code) AS reason_name,
          ur.period_count,
          ur.trainee_count,
          ur.current_period_count,
          ur.reemployed_count,
          ur.avg_duration_days
        FROM vw_unemployment_reason_summary ur
        LEFT JOIN ref_unemployment_reason rur ON rur.unemployment_reason_id = ur.unemployment_reason_id
        ORDER BY ur.period_count DESC, ur.unemployment_reason_id ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new UnemploymentReasonProjection(
        rs.getLong("unemployment_reason_id"),
        rs.getString("unemployment_reason_code"),
        rs.getString("reason_name"),
        getNullableLong(rs, "period_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "current_period_count"),
        getNullableLong(rs, "reemployed_count"),
        getScaledBigDecimal(rs, "avg_duration_days")
    ));
  }

  /**
   * Executes formal Query Q17 to retrieve exit reasons, separation nature classifications,
   * and voluntary vs involuntary distribution from {@code vw_attrition_reason_summary}.
   *
   * @return list of attrition reason projections ordered deterministically
   */
  public List<AttritionReasonProjection> getFormalQ17AttritionReasons() {
    String sql = """
        SELECT
          a.employment_exit_reason_id,
          a.exit_reason_code,
          COALESCE(xr.reason_name, a.exit_reason_code) AS exit_reason_name,
          a.separation_nature_id,
          a.separation_nature_code,
          COALESCE(sn.nature_name, a.separation_nature_code) AS separation_nature_name,
          a.is_voluntary_flag,
          a.is_involuntary_flag,
          a.exit_count,
          a.trainee_count
        FROM vw_attrition_reason_summary a
        LEFT JOIN ref_employment_exit_reason xr ON xr.employment_exit_reason_id = a.employment_exit_reason_id
        LEFT JOIN ref_separation_nature sn ON sn.separation_nature_id = a.separation_nature_id
        ORDER BY a.exit_count DESC, a.employment_exit_reason_id ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new AttritionReasonProjection(
        rs.getLong("employment_exit_reason_id"),
        rs.getString("exit_reason_code"),
        rs.getString("exit_reason_name"),
        getNullableLong(rs, "separation_nature_id"),
        rs.getString("separation_nature_code"),
        rs.getString("separation_nature_name"),
        getNullableBoolean(rs, "is_voluntary_flag"),
        getNullableBoolean(rs, "is_involuntary_flag"),
        getNullableLong(rs, "exit_count"),
        getNullableLong(rs, "trainee_count")
    ));
  }

  /**
   * Executes formal Query Q18 to retrieve voluntary vs involuntary separation totals
   * from {@code vw_attrition_reason_summary}.
   *
   * @return separation analysis projection
   */
  public SeparationAnalysisProjection getFormalQ18SeparationTotals() {
    String sql = """
        SELECT
          SUM(CASE WHEN is_voluntary_flag = 1 THEN exit_count ELSE 0 END) AS voluntary_exit_count,
          SUM(CASE WHEN is_involuntary_flag = 1 THEN exit_count ELSE 0 END) AS involuntary_exit_count,
          SUM(CASE WHEN is_voluntary_flag = 0 AND is_involuntary_flag = 0 THEN exit_count ELSE 0 END) AS other_exit_count,
          SUM(exit_count) AS total_exit_count
        FROM vw_attrition_reason_summary
        """;

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new SeparationAnalysisProjection(
        getNullableLong(rs, "voluntary_exit_count"),
        getNullableLong(rs, "involuntary_exit_count"),
        getNullableLong(rs, "other_exit_count"),
        getNullableLong(rs, "total_exit_count")
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

  private Boolean getNullableBoolean(ResultSet rs, String columnLabel) throws SQLException {
    boolean value = rs.getBoolean(columnLabel);
    return rs.wasNull() ? null : value;
  }
}
