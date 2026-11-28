package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.repository.analytics.projection.DistrictPerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.projection.StatePerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.projection.TrainingCenterSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.util.AnalyticsQueryFilterBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Dedicated analytical query repository for Geographic, State, and District Heatmap Dashboards.
 * Executes formal analytical SQL query Q06, state outcome rollups, and district-level
 * training center distributions via high-performance {@link JdbcTemplate} against immutable
 * database views and master tables without altering {@code JpaRepository} contracts.
 */
@Repository
public class GeographicDashboardRepository {

  private static final String TRAINING_CENTERS_BY_DISTRICT_SQL = """
      SELECT
        center_id AS centerId,
        center_code AS centerCode,
        center_name AS centerName,
        operational_capacity AS operationalCapacity,
        pincode
      FROM training_centers
      WHERE district_id = ? AND deleted_at IS NULL
      ORDER BY center_name ASC
      """;

  private static final String EXISTS_STATE_SQL = """
      SELECT COUNT(*) FROM states WHERE state_id = ? AND deleted_at IS NULL
      """;

  private static final String EXISTS_DISTRICT_SQL = """
      SELECT COUNT(*) FROM districts WHERE district_id = ? AND deleted_at IS NULL
      """;

  private static final String COUNT_DISTRICTS_WITHOUT_TRAINING_CENTERS_SQL = """
      SELECT COUNT(*)
      FROM districts d
      WHERE d.deleted_at IS NULL
        AND NOT EXISTS (
          SELECT 1 FROM training_centers tc WHERE tc.district_id = d.district_id AND tc.deleted_at IS NULL
        )
      """;

  private static final String COUNT_DISTRICTS_WITH_ZERO_ENROLLMENTS_SQL = """
      SELECT COUNT(*)
      FROM districts d
      WHERE d.deleted_at IS NULL
        AND NOT EXISTS (
          SELECT 1 FROM vw_district_outcome_summary v WHERE v.district_id = d.district_id
        )
      """;

  private static final RowMapper<DistrictPerformanceProjection> DISTRICT_ROW_MAPPER = (rs, rowNum) ->
      new DistrictPerformanceProjection(
          rs.getLong("districtId"),
          rs.getString("districtCode"),
          rs.getString("districtName"),
          rs.getLong("stateId"),
          rs.getString("stateCode"),
          rs.getString("stateName"),
          (Number) rs.getObject("enrollmentCount"),
          (Number) rs.getObject("traineeCount"),
          (Number) rs.getObject("completedCount"),
          (Number) rs.getObject("certifiedCount"),
          (Number) rs.getObject("joinedPlacementCount"),
          (Number) rs.getObject("employedCount"),
          rs.getBigDecimal("completionRatePct"),
          rs.getBigDecimal("certificationRatePct"),
          rs.getBigDecimal("placementRatePct"),
          rs.getBigDecimal("employmentRatePct")
      );

  private static final RowMapper<StatePerformanceProjection> STATE_ROW_MAPPER = (rs, rowNum) ->
      new StatePerformanceProjection(
          rs.getLong("stateId"),
          rs.getString("stateCode"),
          rs.getString("stateName"),
          (Number) rs.getObject("districtCount"),
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
          (Number) rs.getObject("trainingCenterCount")
      );

  private static final RowMapper<TrainingCenterSummaryProjection> TC_ROW_MAPPER = (rs, rowNum) ->
      new TrainingCenterSummaryProjection(
          rs.getLong("centerId"),
          rs.getString("centerCode"),
          rs.getString("centerName"),
          (Integer) rs.getObject("operationalCapacity"),
          rs.getString("pincode")
      );

  private final JdbcTemplate jdbcTemplate;

  public GeographicDashboardRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Q06: District outcome performance matrix ordered by employment rate ascending (priority intervention)
   * and enrollment count descending.
   */
  public List<DistrictPerformanceProjection> getFormalQ06DistrictOutcomes() {
    return getFormalQ06DistrictOutcomes(null);
  }

  /**
   * Retrieves all districts belonging to a specific state, including districts with zero enrollments.
   */
  public List<DistrictPerformanceProjection> getDistrictsByStateId(Long stateId) {
    return getDistrictsByStateId(stateId, null);
  }

  /**
   * Retrieves performance details for a specific district.
   */
  public DistrictPerformanceProjection getDistrictById(Long districtId) {
    return getDistrictById(districtId, null);
  }

  /**
   * Retrieves nationwide state performance summaries grouped by trainee state.
   */
  public List<StatePerformanceProjection> getStateSummaries() {
    return queryStateSummaries(null, null);
  }

  /**
   * Retrieves outcome summary for a single state.
   */
  public StatePerformanceProjection getStateSummaryById(Long stateId) {
    return queryStateSummaries(stateId, null).stream().findFirst().orElse(null);
  }

  /**
   * Q06: District outcome performance matrix with optional multi-dimensional filters.
   */
  public List<DistrictPerformanceProjection> getFormalQ06DistrictOutcomes(AnalyticsFilterCriteria criteria) {
    return queryDistricts(null, null, criteria, true);
  }

  /** Retrieves state districts, including districts with zero matching enrollments. */
  public List<DistrictPerformanceProjection> getDistrictsByStateId(Long stateId, AnalyticsFilterCriteria criteria) {
    return queryDistricts(stateId, criteria == null ? null : criteria.getDistrictId(), criteria, false);
  }

  public DistrictPerformanceProjection getDistrictById(Long districtId, AnalyticsFilterCriteria criteria) {
    return queryDistricts(null, districtId, criteria, false).stream().findFirst().orElse(null);
  }

  private List<DistrictPerformanceProjection> queryDistricts(Long stateId, Long districtId,
      AnalyticsFilterCriteria criteria, boolean onlyEnrolledDistricts) {
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String sql = """
        SELECT
          d.district_id AS districtId,
          d.district_code AS districtCode,
          d.district_name AS districtName,
          st.state_id AS stateId,
          st.state_code AS stateCode,
          st.state_name AS stateName,
          COALESCE(v.enrollment_count, 0) AS enrollmentCount,
          COALESCE(v.trainee_count, 0) AS traineeCount,
          COALESCE(v.completed_count, 0) AS completedCount,
          COALESCE(v.certified_count, 0) AS certifiedCount,
          COALESCE(v.joined_placement_count, 0) AS joinedPlacementCount,
          COALESCE(v.employed_count, 0) AS employedCount,
          v.completion_rate_pct AS completionRatePct,
          v.certification_rate_pct AS certificationRatePct,
          v.placement_rate_pct AS placementRatePct,
          v.employment_rate_pct AS employmentRatePct
        FROM districts d
        JOIN states st ON st.state_id = d.state_id
        LEFT JOIN (
          SELECT
            v.trainee_district_id AS district_id,
            v.trainee_state_id AS state_id,
            COUNT(*) AS enrollment_count,
            COUNT(DISTINCT v.trainee_id) AS trainee_count,
            SUM(v.is_completed_flag) AS completed_count,
            COUNT(DISTINCT IF(v.has_issued_certificate_flag = 1, v.trainee_id, NULL)) AS certified_count,
            SUM(v.has_joined_placement_flag) AS joined_placement_count,
            SUM(v.has_employment_flag) AS employed_count,
            ROUND(SUM(v.is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
            ROUND(COUNT(DISTINCT IF(v.has_issued_certificate_flag = 1, v.trainee_id, NULL))
              / NULLIF(COUNT(DISTINCT v.trainee_id), 0) * 100, 2) AS certification_rate_pct,
            ROUND(SUM(v.has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
            ROUND(SUM(v.has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
          FROM vw_enrollment_outcome_fact v
          WHERE 1=1
        """ + filter.getSqlFragment() + """
          GROUP BY v.trainee_district_id, v.trainee_state_id
        ) v ON v.district_id = d.district_id AND v.state_id = st.state_id
        WHERE st.deleted_at IS NULL AND d.deleted_at IS NULL
          AND (? IS NULL OR st.state_id = ?) AND (? IS NULL OR d.district_id = ?)
          AND (? = FALSE OR v.enrollment_count IS NOT NULL)
        ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct ASC, enrollmentCount DESC
        """;
    List<Object> params = new ArrayList<>(filter.getParameters());
    params.add(stateId);
    params.add(stateId);
    params.add(districtId);
    params.add(districtId);
    params.add(onlyEnrolledDistricts);
    return jdbcTemplate.query(sql, DISTRICT_ROW_MAPPER, params.toArray());
  }

  /**
   * Retrieves nationwide state performance summaries grouped by trainee state with optional filters.
   */
  public List<StatePerformanceProjection> getStateSummaries(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getStateSummaries();
    }
    return queryStateSummaries(criteria.getStateId(), criteria);
  }

  private List<StatePerformanceProjection> queryStateSummaries(Long stateId, AnalyticsFilterCriteria criteria) {
    Long districtId = criteria == null ? null : criteria.getDistrictId();
    AnalyticsQueryFilterBuilder.FilterPredicateResult filter =
        AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String sql = """
        SELECT
          st.state_id AS stateId,
          st.state_code AS stateCode,
          st.state_name AS stateName,
          COALESCE(d.district_count, 0) AS districtCount,
          COALESCE(v.enrollment_count, 0) AS enrollmentCount,
          COALESCE(v.trainee_count, 0) AS traineeCount,
          COALESCE(v.completed_count, 0) AS completedCount,
          COALESCE(v.certified_count, 0) AS certifiedCount,
          COALESCE(v.joined_placement_count, 0) AS joinedPlacementCount,
          COALESCE(v.employed_count, 0) AS employedCount,
          ROUND(v.completed_count / NULLIF(v.enrollment_count, 0) * 100, 2) AS completionRatePct,
          ROUND(v.certified_count / NULLIF(v.trainee_count, 0) * 100, 2) AS certificationRatePct,
          ROUND(v.joined_placement_count / NULLIF(v.enrollment_count, 0) * 100, 2) AS placementRatePct,
          ROUND(v.employed_count / NULLIF(v.enrollment_count, 0) * 100, 2) AS employmentRatePct,
          COALESCE(tc.center_count, 0) AS trainingCenterCount
        FROM states st
        LEFT JOIN (
          SELECT state_id, COUNT(*) AS district_count FROM districts
          WHERE deleted_at IS NULL AND (? IS NULL OR district_id = ?) GROUP BY state_id
        ) d ON d.state_id = st.state_id
        LEFT JOIN (
          SELECT
            v.trainee_state_id AS state_id,
            COUNT(*) AS enrollment_count,
            COUNT(DISTINCT v.trainee_id) AS trainee_count,
            SUM(v.is_completed_flag) AS completed_count,
            COUNT(DISTINCT IF(v.has_issued_certificate_flag = 1, v.trainee_id, NULL)) AS certified_count,
            SUM(v.has_joined_placement_flag) AS joined_placement_count,
            SUM(v.has_employment_flag) AS employed_count
          FROM vw_enrollment_outcome_fact v
          WHERE 1=1
        """ + filter.getSqlFragment() + """
          GROUP BY v.trainee_state_id
        ) v ON v.state_id = st.state_id
        LEFT JOIN (
          SELECT state_id, COUNT(*) AS center_count
          FROM training_centers
          WHERE deleted_at IS NULL AND (? IS NULL OR district_id = ?)
          GROUP BY state_id
        ) tc ON tc.state_id = st.state_id
        WHERE st.deleted_at IS NULL AND (? IS NULL OR st.state_id = ?)
        ORDER BY employmentRatePct IS NULL, employmentRatePct DESC, enrollmentCount DESC
        """;
    List<Object> params = new ArrayList<>();
    params.add(districtId);
    params.add(districtId);
    params.addAll(filter.getParameters());
    params.add(districtId);
    params.add(districtId);
    params.add(stateId);
    params.add(stateId);
    return jdbcTemplate.query(sql, STATE_ROW_MAPPER, params.toArray());
  }

  public StatePerformanceProjection getStateSummaryById(Long stateId, AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getStateSummaryById(stateId);
    }
    return queryStateSummaries(stateId, criteria).stream().findFirst().orElse(null);
  }

  public long countDistrictsWithoutTrainingCenters(AnalyticsFilterCriteria criteria) {
    Long state = criteria.getStateId();
    Long district = criteria.getDistrictId();
    Long count = jdbcTemplate.queryForObject("""
        SELECT COUNT(*) FROM districts d WHERE d.deleted_at IS NULL
          AND (? IS NULL OR d.state_id = ?) AND (? IS NULL OR d.district_id = ?)
          AND NOT EXISTS (SELECT 1 FROM training_centers tc WHERE tc.district_id = d.district_id AND tc.deleted_at IS NULL)
        """, Long.class, state, state, district, district);
    return count == null ? 0L : count;
  }

  /**
   * Retrieves operational training centers located in a specific district.
   */
  public List<TrainingCenterSummaryProjection> getTrainingCentersByDistrictId(Long districtId) {
    return jdbcTemplate.query(TRAINING_CENTERS_BY_DISTRICT_SQL, TC_ROW_MAPPER, districtId);
  }

  /**
   * Checks whether a state exists and is not soft-deleted.
   */
  public boolean existsStateById(Long stateId) {
    Integer count = jdbcTemplate.queryForObject(EXISTS_STATE_SQL, Integer.class, stateId);
    return count != null && count > 0;
  }

  /**
   * Checks whether a district exists and is not soft-deleted.
   */
  public boolean existsDistrictById(Long districtId) {
    Integer count = jdbcTemplate.queryForObject(EXISTS_DISTRICT_SQL, Integer.class, districtId);
    return count != null && count > 0;
  }

  /**
   * Counts active districts that have no operational training centers.
   */
  public Long countDistrictsWithoutTrainingCenters() {
    Long count = jdbcTemplate.queryForObject(COUNT_DISTRICTS_WITHOUT_TRAINING_CENTERS_SQL, Long.class);
    return count != null ? count : 0L;
  }

  /**
   * Counts active districts that have zero recorded training enrollments.
   */
  public Long countDistrictsWithZeroEnrollments() {
    Long count = jdbcTemplate.queryForObject(COUNT_DISTRICTS_WITH_ZERO_ENROLLMENTS_SQL, Long.class);
    return count != null ? count : 0L;
  }
}
