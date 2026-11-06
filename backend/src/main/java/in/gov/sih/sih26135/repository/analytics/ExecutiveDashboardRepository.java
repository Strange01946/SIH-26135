package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.repository.analytics.projection.DashboardKpiProjection;
import in.gov.sih.sih26135.repository.analytics.projection.PipelineFunnelProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SchemeProgramPerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.util.AnalyticsQueryFilterBuilder;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Dedicated analytical query repository for Executive and National Overview Dashboards.
 * Executes formal analytical SQL queries Q01, Q02, and Q03 via high-performance {@link JdbcTemplate}
 * against immutable database views and domain master tables without altering {@code JpaRepository} contracts.
 */
@Repository
public class ExecutiveDashboardRepository {

  private static final String Q01_SQL = """
      SELECT
        (SELECT COUNT(*) FROM vw_trainee_outcome_summary) AS traineeCount,
        (SELECT COUNT(*) FROM vw_enrollment_outcome_fact) AS enrollmentCount,
        (SELECT COUNT(DISTINCT trainee_id) FROM vw_enrollment_outcome_fact) AS enrolledTraineeCount,
        (SELECT SUM(is_completed_flag) FROM vw_enrollment_outcome_fact) AS completedEnrollmentCount,
        (SELECT SUM(has_issued_certificate_flag) FROM vw_enrollment_outcome_fact) AS certifiedEnrollmentCount,
        (SELECT COUNT(*) FROM vw_placement_fact) AS placementCount,
        (SELECT SUM(joined_flag) FROM vw_placement_fact) AS joinedPlacementCount,
        (SELECT COUNT(*) FROM vw_employment_fact) AS employmentSpellCount,
        (SELECT COUNT(DISTINCT trainee_id) FROM vw_employment_fact) AS employedTraineeCount,
        (SELECT SUM(CASE WHEN is_current = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS currentEmploymentSpellCount,
        (SELECT SUM(CASE WHEN is_current = 1 AND is_self_employment = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS currentSelfEmploymentCount,
        (SELECT SUM(CASE WHEN is_current = 1 AND is_apprenticeship = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS currentApprenticeshipCount
      """;

  private static final String Q02_SQL = """
      SELECT
        COUNT(DISTINCT trainee_id) AS traineeCount,
        COUNT(*) AS enrollmentCount,
        SUM(is_completed_flag) AS completedCount,
        SUM(has_issued_certificate_flag) AS certifiedCount,
        SUM(has_joined_placement_flag) AS joinedPlacementCount,
        SUM(has_employment_flag) AS employedCount,
        ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completionRatePct,
        ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certificationRatePct,
        ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placementRatePct,
        ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employmentRatePct
      FROM vw_enrollment_outcome_fact
      """;

  private static final String Q03_SQL = """
      SELECT
        p.program_id AS programId,
        p.program_code AS programCode,
        p.program_name AS programName,
        s.scheme_id AS schemeId,
        s.scheme_code AS schemeCode,
        s.scheme_name AS schemeName,
        v.enrollment_count AS enrollmentCount,
        v.trainee_count AS traineeCount,
        v.completed_count AS completedCount,
        v.certified_count AS certifiedCount,
        v.joined_placement_count AS joinedPlacementCount,
        v.employed_count AS employedCount,
        v.self_employment_spell_count AS selfEmploymentSpellCount,
        v.apprenticeship_spell_count AS apprenticeshipSpellCount,
        v.completion_rate_pct AS completionRatePct,
        v.certification_rate_pct AS certificationRatePct,
        v.placement_rate_pct AS placementRatePct,
        v.employment_rate_pct AS employmentRatePct
      FROM vw_program_outcome_summary v
      JOIN programs p ON p.program_id = v.program_id
      JOIN schemes s ON s.scheme_id = v.scheme_id
      ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC
      """;

  private final JdbcTemplate jdbcTemplate;

  public ExecutiveDashboardRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Q01: Dashboard KPI strip consolidated counts across analytical outcome views.
   */
  public DashboardKpiProjection getExecutiveDashboardKpiStrip() {
    List<DashboardKpiProjection> results = jdbcTemplate.query(Q01_SQL, KPI_ROW_MAPPER);
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Q02: Training pipeline conversion funnel stages and conversion rates.
   */
  public PipelineFunnelProjection getTrainingPipelineConversionFunnel() {
    List<PipelineFunnelProjection> results = jdbcTemplate.query(Q02_SQL, FUNNEL_ROW_MAPPER);
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Q03: Scheme & Program comparative performance matrix with display codes and employment efficacy.
   */
  public List<SchemeProgramPerformanceProjection> getSchemeProgramPerformanceMatrix() {
    return jdbcTemplate.query(Q03_SQL, MATRIX_ROW_MAPPER);
  }

  /**
   * Q01: Dashboard KPI strip consolidated counts across analytical outcome views with optional multi-dimensional filters.
   */
  public DashboardKpiProjection getExecutiveDashboardKpiStrip(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getExecutiveDashboardKpiStrip();
    }
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String sql = """
        SELECT
          COUNT(DISTINCT v.trainee_id) AS traineeCount,
          COUNT(*) AS enrollmentCount,
          COUNT(DISTINCT v.trainee_id) AS enrolledTraineeCount,
          SUM(v.is_completed_flag) AS completedEnrollmentCount,
          SUM(v.has_issued_certificate_flag) AS certifiedEnrollmentCount,
          SUM(v.placement_count) AS placementCount,
          SUM(v.joined_placement_count) AS joinedPlacementCount,
          SUM(v.employment_count) AS employmentSpellCount,
          COUNT(DISTINCT CASE WHEN v.has_employment_flag = 1 THEN v.trainee_id ELSE NULL END) AS employedTraineeCount,
          SUM(v.current_employment_count) AS currentEmploymentSpellCount,
          SUM(v.self_employment_count) AS currentSelfEmploymentCount,
          SUM(v.apprenticeship_count) AS currentApprenticeshipCount
        FROM vw_enrollment_outcome_fact v
        WHERE 1=1
        """ + filter.getSqlFragment();

    List<DashboardKpiProjection> results = jdbcTemplate.query(sql, KPI_ROW_MAPPER, filter.getParameters().toArray());
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Q02: Training pipeline conversion funnel stages and conversion rates with optional multi-dimensional filters.
   */
  public PipelineFunnelProjection getTrainingPipelineConversionFunnel(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getTrainingPipelineConversionFunnel();
    }
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String sql = """
        SELECT
          COUNT(DISTINCT v.trainee_id) AS traineeCount,
          COUNT(*) AS enrollmentCount,
          SUM(v.is_completed_flag) AS completedCount,
          SUM(v.has_issued_certificate_flag) AS certifiedCount,
          SUM(v.has_joined_placement_flag) AS joinedPlacementCount,
          SUM(v.has_employment_flag) AS employedCount,
          ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completionRatePct,
          ROUND(SUM(v.has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certificationRatePct,
          ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placementRatePct,
          ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employmentRatePct
        FROM vw_enrollment_outcome_fact v
        WHERE 1=1
        """ + filter.getSqlFragment();

    List<PipelineFunnelProjection> results = jdbcTemplate.query(sql, FUNNEL_ROW_MAPPER, filter.getParameters().toArray());
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Q03: Scheme & Program comparative performance matrix with display codes and employment efficacy with optional filters.
   */
  public List<SchemeProgramPerformanceProjection> getSchemeProgramPerformanceMatrix(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getSchemeProgramPerformanceMatrix();
    }
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String sql = """
        SELECT
          p.program_id AS programId,
          p.program_code AS programCode,
          p.program_name AS programName,
          s.scheme_id AS schemeId,
          s.scheme_code AS schemeCode,
          s.scheme_name AS schemeName,
          COUNT(*) AS enrollmentCount,
          COUNT(DISTINCT v.trainee_id) AS traineeCount,
          SUM(v.is_completed_flag) AS completedCount,
          SUM(v.has_issued_certificate_flag) AS certifiedCount,
          SUM(v.has_joined_placement_flag) AS joinedPlacementCount,
          SUM(v.has_employment_flag) AS employedCount,
          SUM(v.self_employment_count) AS selfEmploymentSpellCount,
          SUM(v.apprenticeship_count) AS apprenticeshipSpellCount,
          ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completionRatePct,
          ROUND(SUM(v.has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certificationRatePct,
          ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placementRatePct,
          ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employmentRatePct
        FROM vw_enrollment_outcome_fact v
        JOIN programs p ON p.program_id = v.program_id
        JOIN schemes s ON s.scheme_id = v.scheme_id
        WHERE 1=1
        """ + filter.getSqlFragment() + """
        GROUP BY p.program_id, p.program_code, p.program_name, s.scheme_id, s.scheme_code, s.scheme_name
        ORDER BY employmentRatePct IS NULL, employmentRatePct DESC, enrollmentCount DESC
        """;

    return jdbcTemplate.query(sql, MATRIX_ROW_MAPPER, filter.getParameters().toArray());
  }

  private static final RowMapper<DashboardKpiProjection> KPI_ROW_MAPPER = (rs, rowNum) ->
      new DashboardKpiRecord(
          (Number) rs.getObject("traineeCount"),
          (Number) rs.getObject("enrollmentCount"),
          (Number) rs.getObject("enrolledTraineeCount"),
          (Number) rs.getObject("completedEnrollmentCount"),
          (Number) rs.getObject("certifiedEnrollmentCount"),
          (Number) rs.getObject("placementCount"),
          (Number) rs.getObject("joinedPlacementCount"),
          (Number) rs.getObject("employmentSpellCount"),
          (Number) rs.getObject("employedTraineeCount"),
          (Number) rs.getObject("currentEmploymentSpellCount"),
          (Number) rs.getObject("currentSelfEmploymentCount"),
          (Number) rs.getObject("currentApprenticeshipCount")
      );

  private static final RowMapper<PipelineFunnelProjection> FUNNEL_ROW_MAPPER = (rs, rowNum) ->
      new PipelineFunnelRecord(
          (Number) rs.getObject("traineeCount"),
          (Number) rs.getObject("enrollmentCount"),
          (Number) rs.getObject("completedCount"),
          (Number) rs.getObject("certifiedCount"),
          (Number) rs.getObject("joinedPlacementCount"),
          (Number) rs.getObject("employedCount"),
          rs.getBigDecimal("completionRatePct"),
          rs.getBigDecimal("certificationRatePct"),
          rs.getBigDecimal("placementRatePct"),
          rs.getBigDecimal("employmentRatePct")
      );

  private static final RowMapper<SchemeProgramPerformanceProjection> MATRIX_ROW_MAPPER = (rs, rowNum) ->
      new SchemeProgramPerformanceRecord(
          (Long) rs.getObject("programId"),
          rs.getString("programCode"),
          rs.getString("programName"),
          (Long) rs.getObject("schemeId"),
          rs.getString("schemeCode"),
          rs.getString("schemeName"),
          (Number) rs.getObject("enrollmentCount"),
          (Number) rs.getObject("traineeCount"),
          (Number) rs.getObject("completedCount"),
          (Number) rs.getObject("certifiedCount"),
          (Number) rs.getObject("joinedPlacementCount"),
          (Number) rs.getObject("employedCount"),
          (Number) rs.getObject("selfEmploymentSpellCount"),
          (Number) rs.getObject("apprenticeshipSpellCount"),
          rs.getBigDecimal("completionRatePct"),
          rs.getBigDecimal("certificationRatePct"),
          rs.getBigDecimal("placementRatePct"),
          rs.getBigDecimal("employmentRatePct")
      );

  record DashboardKpiRecord(
      Number traineeCount,
      Number enrollmentCount,
      Number enrolledTraineeCount,
      Number completedEnrollmentCount,
      Number certifiedEnrollmentCount,
      Number placementCount,
      Number joinedPlacementCount,
      Number employmentSpellCount,
      Number employedTraineeCount,
      Number currentEmploymentSpellCount,
      Number currentSelfEmploymentCount,
      Number currentApprenticeshipCount
  ) implements DashboardKpiProjection {
    @Override public Number getTraineeCount() { return traineeCount; }
    @Override public Number getEnrollmentCount() { return enrollmentCount; }
    @Override public Number getEnrolledTraineeCount() { return enrolledTraineeCount; }
    @Override public Number getCompletedEnrollmentCount() { return completedEnrollmentCount; }
    @Override public Number getCertifiedEnrollmentCount() { return certifiedEnrollmentCount; }
    @Override public Number getPlacementCount() { return placementCount; }
    @Override public Number getJoinedPlacementCount() { return joinedPlacementCount; }
    @Override public Number getEmploymentSpellCount() { return employmentSpellCount; }
    @Override public Number getEmployedTraineeCount() { return employedTraineeCount; }
    @Override public Number getCurrentEmploymentSpellCount() { return currentEmploymentSpellCount; }
    @Override public Number getCurrentSelfEmploymentCount() { return currentSelfEmploymentCount; }
    @Override public Number getCurrentApprenticeshipCount() { return currentApprenticeshipCount; }
  }

  record PipelineFunnelRecord(
      Number traineeCount,
      Number enrollmentCount,
      Number completedCount,
      Number certifiedCount,
      Number joinedPlacementCount,
      Number employedCount,
      BigDecimal completionRatePct,
      BigDecimal certificationRatePct,
      BigDecimal placementRatePct,
      BigDecimal employmentRatePct
  ) implements PipelineFunnelProjection {
    @Override public Number getTraineeCount() { return traineeCount; }
    @Override public Number getEnrollmentCount() { return enrollmentCount; }
    @Override public Number getCompletedCount() { return completedCount; }
    @Override public Number getCertifiedCount() { return certifiedCount; }
    @Override public Number getJoinedPlacementCount() { return joinedPlacementCount; }
    @Override public Number getEmployedCount() { return employedCount; }
    @Override public BigDecimal getCompletionRatePct() { return completionRatePct; }
    @Override public BigDecimal getCertificationRatePct() { return certificationRatePct; }
    @Override public BigDecimal getPlacementRatePct() { return placementRatePct; }
    @Override public BigDecimal getEmploymentRatePct() { return employmentRatePct; }
  }

  record SchemeProgramPerformanceRecord(
      Long programId,
      String programCode,
      String programName,
      Long schemeId,
      String schemeCode,
      String schemeName,
      Number enrollmentCount,
      Number traineeCount,
      Number completedCount,
      Number certifiedCount,
      Number joinedPlacementCount,
      Number employedCount,
      Number selfEmploymentSpellCount,
      Number apprenticeshipSpellCount,
      BigDecimal completionRatePct,
      BigDecimal certificationRatePct,
      BigDecimal placementRatePct,
      BigDecimal employmentRatePct
  ) implements SchemeProgramPerformanceProjection {
    @Override public Long getProgramId() { return programId; }
    @Override public String getProgramCode() { return programCode; }
    @Override public String getProgramName() { return programName; }
    @Override public Long getSchemeId() { return schemeId; }
    @Override public String getSchemeCode() { return schemeCode; }
    @Override public String getSchemeName() { return schemeName; }
    @Override public Number getEnrollmentCount() { return enrollmentCount; }
    @Override public Number getTraineeCount() { return traineeCount; }
    @Override public Number getCompletedCount() { return completedCount; }
    @Override public Number getCertifiedCount() { return certifiedCount; }
    @Override public Number getJoinedPlacementCount() { return joinedPlacementCount; }
    @Override public Number getEmployedCount() { return employedCount; }
    @Override public Number getSelfEmploymentSpellCount() { return selfEmploymentSpellCount; }
    @Override public Number getApprenticeshipSpellCount() { return apprenticeshipSpellCount; }
    @Override public BigDecimal getCompletionRatePct() { return completionRatePct; }
    @Override public BigDecimal getCertificationRatePct() { return certificationRatePct; }
    @Override public BigDecimal getPlacementRatePct() { return placementRatePct; }
    @Override public BigDecimal getEmploymentRatePct() { return employmentRatePct; }
  }
}
