package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.repository.analytics.projection.FollowupOverallSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.FollowupWaveProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SurveyOverallSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SurveyResponsePipelineProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationMethodEfficacyProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationOutcomeProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationOverallSummaryProjection;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository executing formal analytical queries Q19, Q20, Q21, and Q22
 * for follow-up outreach success, survey response pipeline, verification funnel,
 * and verification method efficacy.
 *
 * <p>Implemented via {@link JdbcTemplate} to preserve Spring Data JPA repository invariants.
 */
@Repository
public class DataIntegrityDashboardRepository {

  private final JdbcTemplate jdbcTemplate;

  public DataIntegrityDashboardRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Executes formal Query Q21 to retrieve verification outcomes across all verification cycles.
   *
   * @return list of verification outcome projections ordered by volume
   */
  public List<VerificationOutcomeProjection> getFormalQ21VerificationOutcomes() {
    String sql = """
        SELECT
          v.record_verification_status_code,
          COALESCE(rvs.status_name, v.record_verification_status_code) AS status_name,
          v.is_verified_flag,
          COUNT(*) AS verification_count,
          COUNT(DISTINCT v.trainee_id) AS trainee_count,
          COUNT(DISTINCT v.employment_id) AS employment_count
        FROM vw_employment_verification_fact v
        LEFT JOIN ref_record_verification_status rvs
          ON rvs.status_code = v.record_verification_status_code
        GROUP BY v.record_verification_status_code, rvs.status_name, v.is_verified_flag, rvs.sort_order
        ORDER BY verification_count DESC, rvs.sort_order ASC, v.record_verification_status_code ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new VerificationOutcomeProjection(
        rs.getString("record_verification_status_code"),
        rs.getString("status_name"),
        rs.getInt("is_verified_flag") == 1,
        getNullableLong(rs, "verification_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "employment_count")
    ));
  }

  /**
   * Executes formal Query Q22 to retrieve current/latest verification per employment spell, by method.
   *
   * @return list of current verification method projections
   */
  public List<VerificationMethodEfficacyProjection> getFormalQ22VerificationMethodEfficacy() {
    String sql = """
        SELECT
          v.verification_method_code,
          COALESCE(m.method_name, v.verification_method_code) AS method_name,
          v.is_trainee_self_reported,
          v.is_official_flag,
          v.record_verification_status_code,
          COUNT(*) AS current_verification_count,
          COUNT(DISTINCT v.trainee_id) AS trainee_count
        FROM vw_employment_verification_fact v
        LEFT JOIN ref_employment_verification_method m
          ON m.method_code = v.verification_method_code
        WHERE v.is_current = 1
        GROUP BY
          v.verification_method_code,
          m.method_name,
          v.is_trainee_self_reported,
          v.is_official_flag,
          v.record_verification_status_code,
          m.sort_order
        ORDER BY current_verification_count DESC, m.sort_order ASC, v.verification_method_code ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new VerificationMethodEfficacyProjection(
        rs.getString("verification_method_code"),
        rs.getString("method_name"),
        rs.getInt("is_trainee_self_reported") == 1,
        rs.getInt("is_official_flag") == 1,
        rs.getString("record_verification_status_code"),
        getNullableLong(rs, "current_verification_count"),
        getNullableLong(rs, "trainee_count")
    ));
  }

  /**
   * Executes overall summary aggregation over {@code vw_employment_verification_fact}.
   *
   * @return authoritative overall verification summary
   */
  public VerificationOverallSummaryProjection getFormalQ21Q22OverallSummary() {
    String sql = """
        SELECT
          COUNT(*) AS total_verifications,
          COUNT(DISTINCT employment_id) AS total_employments,
          COUNT(DISTINCT trainee_id) AS total_trainees,
          SUM(CASE WHEN record_verification_status_code = 'VERIFIED' THEN 1 ELSE 0 END) AS verified_count,
          SUM(CASE WHEN is_official_flag = 1 AND record_verification_status_code = 'VERIFIED' THEN 1 ELSE 0 END) AS official_verified_count,
          SUM(CASE WHEN is_trainee_self_reported = 1 THEN 1 ELSE 0 END) AS self_reported_count,
          SUM(CASE WHEN record_verification_status_code = 'PENDING' THEN 1 ELSE 0 END) AS pending_count,
          SUM(CASE WHEN record_verification_status_code = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count,
          SUM(CASE WHEN record_verification_status_code = 'UNVERIFIED' THEN 1 ELSE 0 END) AS unverified_count
        FROM vw_employment_verification_fact
        """;

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new VerificationOverallSummaryProjection(
        getNullableLong(rs, "total_verifications"),
        getNullableLong(rs, "total_employments"),
        getNullableLong(rs, "total_trainees"),
        getNullableLong(rs, "verified_count"),
        getNullableLong(rs, "official_verified_count"),
        getNullableLong(rs, "self_reported_count"),
        getNullableLong(rs, "pending_count"),
        getNullableLong(rs, "rejected_count"),
        getNullableLong(rs, "unverified_count")
    ));
  }

  /**
   * Executes formal Query Q19 to retrieve longitudinal follow-up outreach success by wave.
   *
   * @return list of follow-up wave projections ordered by wave offset and sort order
   */
  public List<FollowupWaveProjection> getFormalQ19FollowupSuccessByWave() {
    String sql = """
        SELECT
          s.followup_type_code,
          COALESCE(fty.type_name, s.followup_type_code) AS type_name,
          s.followup_offset_months,
          s.task_count,
          s.trainee_count,
          s.success_count,
          s.no_response_count,
          s.unreachable_count,
          s.success_rate_pct
        FROM vw_followup_outcome_summary s
        LEFT JOIN ref_followup_type fty
          ON fty.type_code = s.followup_type_code
        ORDER BY s.followup_offset_months IS NULL, s.followup_offset_months ASC, fty.sort_order ASC, s.followup_type_code ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new FollowupWaveProjection(
        rs.getString("followup_type_code"),
        rs.getString("type_name"),
        getNullableInteger(rs, "followup_offset_months"),
        getNullableLong(rs, "task_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "success_count"),
        getNullableLong(rs, "no_response_count"),
        getNullableLong(rs, "unreachable_count"),
        getScaledBigDecimal(rs, "success_rate_pct")
    ));
  }

  /**
   * Executes overall summary aggregation over {@code vw_followup_fact}.
   *
   * @return authoritative nationwide follow-up summary
   */
  public FollowupOverallSummaryProjection getFormalQ19OverallSummary() {
    String sql = """
        SELECT
          COUNT(*) AS total_tasks,
          COUNT(DISTINCT trainee_id) AS total_trainees,
          SUM(CASE WHEN is_open_flag = 0 THEN 1 ELSE 0 END) AS completed_tasks,
          SUM(CASE WHEN is_open_flag = 1 THEN 1 ELSE 0 END) AS pending_tasks,
          SUM(CASE WHEN is_success_flag = 1 THEN 1 ELSE 0 END) AS successful_contacts,
          SUM(CASE WHEN is_no_response_flag = 1 THEN 1 ELSE 0 END) AS no_response_count,
          SUM(CASE WHEN is_unreachable_flag = 1 THEN 1 ELSE 0 END) AS unreachable_count
        FROM vw_followup_fact
        """;

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new FollowupOverallSummaryProjection(
        getNullableLong(rs, "total_tasks"),
        getNullableLong(rs, "total_trainees"),
        getNullableLong(rs, "completed_tasks"),
        getNullableLong(rs, "pending_tasks"),
        getNullableLong(rs, "successful_contacts"),
        getNullableLong(rs, "no_response_count"),
        getNullableLong(rs, "unreachable_count")
    ));
  }

  /**
   * Executes formal Query Q20 to retrieve post-training survey response pipeline by purpose and wave.
   *
   * @return list of survey pipeline projections ordered by wave offset and purpose
   */
  public List<SurveyResponsePipelineProjection> getFormalQ20SurveyResponsePipeline() {
    String sql = """
        SELECT
          v.survey_purpose_code,
          COALESCE(sp.purpose_name, v.survey_purpose_code) AS purpose_name,
          v.followup_offset_months,
          COUNT(*) AS response_row_count,
          COUNT(DISTINCT v.trainee_id) AS trainee_count,
          SUM(v.is_submitted_flag) AS submitted_count,
          ROUND(SUM(v.is_submitted_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS submission_rate_pct
        FROM vw_survey_response_fact v
        LEFT JOIN ref_survey_purpose sp
          ON sp.purpose_code = v.survey_purpose_code
        GROUP BY v.survey_purpose_code, sp.purpose_name, v.followup_offset_months, sp.sort_order
        ORDER BY v.followup_offset_months IS NULL, v.followup_offset_months ASC, sp.sort_order ASC, v.survey_purpose_code ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new SurveyResponsePipelineProjection(
        rs.getString("survey_purpose_code"),
        rs.getString("purpose_name"),
        getNullableInteger(rs, "followup_offset_months"),
        getNullableLong(rs, "response_row_count"),
        getNullableLong(rs, "trainee_count"),
        getNullableLong(rs, "submitted_count"),
        getScaledBigDecimal(rs, "submission_rate_pct")
    ));
  }

  /**
   * Executes overall summary aggregation over {@code vw_survey_response_fact}.
   *
   * @return authoritative nationwide survey response summary
   */
  public SurveyOverallSummaryProjection getFormalQ20OverallSummary() {
    String sql = """
        SELECT
          COUNT(*) AS total_response_rows,
          COUNT(DISTINCT trainee_id) AS total_trainees,
          SUM(is_submitted_flag) AS submitted_count,
          SUM(CASE WHEN is_submitted_flag = 0 THEN 1 ELSE 0 END) AS pending_count
        FROM vw_survey_response_fact
        """;

    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new SurveyOverallSummaryProjection(
        getNullableLong(rs, "total_response_rows"),
        getNullableLong(rs, "total_trainees"),
        getNullableLong(rs, "submitted_count"),
        getNullableLong(rs, "pending_count")
    ));
  }

  private Long getNullableLong(ResultSet rs, String columnLabel) throws SQLException {
    long value = rs.getLong(columnLabel);
    return rs.wasNull() ? null : value;
  }

  private Integer getNullableInteger(ResultSet rs, String columnLabel) throws SQLException {
    int value = rs.getInt(columnLabel);
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
