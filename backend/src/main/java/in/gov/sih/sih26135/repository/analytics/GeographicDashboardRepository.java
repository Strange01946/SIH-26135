package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.repository.analytics.projection.DistrictPerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.projection.StatePerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.projection.TrainingCenterSummaryProjection;
import java.math.BigDecimal;
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

  private static final String Q06_SQL = """
      SELECT
        d.district_id AS districtId,
        d.district_code AS districtCode,
        d.district_name AS districtName,
        st.state_id AS stateId,
        st.state_code AS stateCode,
        st.state_name AS stateName,
        v.enrollment_count AS enrollmentCount,
        v.trainee_count AS traineeCount,
        v.completed_count AS completedCount,
        v.certified_count AS certifiedCount,
        v.joined_placement_count AS joinedPlacementCount,
        v.employed_count AS employedCount,
        v.completion_rate_pct AS completionRatePct,
        v.certification_rate_pct AS certificationRatePct,
        v.placement_rate_pct AS placementRatePct,
        v.employment_rate_pct AS employmentRatePct
      FROM vw_district_outcome_summary v
      JOIN districts d ON d.district_id = v.district_id
      JOIN states st ON st.state_id = d.state_id
      ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct ASC, v.enrollment_count DESC
      """;

  private static final String DISTRICTS_BY_STATE_SQL = """
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
      LEFT JOIN vw_district_outcome_summary v ON v.district_id = d.district_id
      WHERE st.state_id = ? AND d.deleted_at IS NULL
      ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct ASC, enrollmentCount DESC
      """;

  private static final String DISTRICT_BY_ID_SQL = """
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
      LEFT JOIN vw_district_outcome_summary v ON v.district_id = d.district_id
      WHERE d.district_id = ? AND d.deleted_at IS NULL
      """;

  private static final String STATE_SUMMARIES_SQL = """
      SELECT
        st.state_id AS stateId,
        st.state_code AS stateCode,
        st.state_name AS stateName,
        COUNT(DISTINCT d.district_id) AS districtCount,
        COALESCE(SUM(v.enrollment_count), 0) AS enrollmentCount,
        COALESCE(SUM(v.trainee_count), 0) AS traineeCount,
        COALESCE(SUM(v.completed_count), 0) AS completedCount,
        COALESCE(SUM(v.certified_count), 0) AS certifiedCount,
        COALESCE(SUM(v.joined_placement_count), 0) AS joinedPlacementCount,
        COALESCE(SUM(v.employed_count), 0) AS employedCount,
        ROUND(SUM(v.completed_count) / NULLIF(SUM(v.enrollment_count), 0) * 100, 2) AS completionRatePct,
        ROUND(SUM(v.certified_count) / NULLIF(SUM(v.enrollment_count), 0) * 100, 2) AS certificationRatePct,
        ROUND(SUM(v.joined_placement_count) / NULLIF(SUM(v.enrollment_count), 0) * 100, 2) AS placementRatePct,
        ROUND(SUM(v.employed_count) / NULLIF(SUM(v.enrollment_count), 0) * 100, 2) AS employmentRatePct,
        COALESCE(tc.center_count, 0) AS trainingCenterCount
      FROM states st
      LEFT JOIN districts d ON d.state_id = st.state_id AND d.deleted_at IS NULL
      LEFT JOIN vw_district_outcome_summary v ON v.district_id = d.district_id
      LEFT JOIN (
        SELECT state_id, COUNT(*) AS center_count
        FROM training_centers
        WHERE deleted_at IS NULL
        GROUP BY state_id
      ) tc ON tc.state_id = st.state_id
      WHERE st.deleted_at IS NULL
      GROUP BY st.state_id, st.state_code, st.state_name, tc.center_count
      ORDER BY employmentRatePct IS NULL, employmentRatePct DESC, enrollmentCount DESC
      """;

  private static final String STATE_SUMMARY_BY_ID_SQL = """
      SELECT
        st.state_id AS stateId,
        st.state_code AS stateCode,
        st.state_name AS stateName,
        COUNT(DISTINCT d.district_id) AS districtCount,
        COALESCE(SUM(v.enrollment_count), 0) AS enrollmentCount,
        COALESCE(SUM(v.trainee_count), 0) AS traineeCount,
        COALESCE(SUM(v.completed_count), 0) AS completedCount,
        COALESCE(SUM(v.certified_count), 0) AS certifiedCount,
        COALESCE(SUM(v.joined_placement_count), 0) AS joinedPlacementCount,
        COALESCE(SUM(v.employed_count), 0) AS employedCount,
        ROUND(SUM(v.completed_count) / NULLIF(SUM(v.enrollment_count), 0) * 100, 2) AS completionRatePct,
        ROUND(SUM(v.certified_count) / NULLIF(SUM(v.enrollment_count), 0) * 100, 2) AS certificationRatePct,
        ROUND(SUM(v.joined_placement_count) / NULLIF(SUM(v.enrollment_count), 0) * 100, 2) AS placementRatePct,
        ROUND(SUM(v.employed_count) / NULLIF(SUM(v.enrollment_count), 0) * 100, 2) AS employmentRatePct,
        COALESCE(tc.center_count, 0) AS trainingCenterCount
      FROM states st
      LEFT JOIN districts d ON d.state_id = st.state_id AND d.deleted_at IS NULL
      LEFT JOIN vw_district_outcome_summary v ON v.district_id = d.district_id
      LEFT JOIN (
        SELECT state_id, COUNT(*) AS center_count
        FROM training_centers
        WHERE deleted_at IS NULL
        GROUP BY state_id
      ) tc ON tc.state_id = st.state_id
      WHERE st.state_id = ? AND st.deleted_at IS NULL
      GROUP BY st.state_id, st.state_code, st.state_name, tc.center_count
      """;

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
    return jdbcTemplate.query(Q06_SQL, DISTRICT_ROW_MAPPER);
  }

  /**
   * Retrieves all districts belonging to a specific state, including districts with zero enrollments.
   */
  public List<DistrictPerformanceProjection> getDistrictsByStateId(Long stateId) {
    return jdbcTemplate.query(DISTRICTS_BY_STATE_SQL, DISTRICT_ROW_MAPPER, stateId);
  }

  /**
   * Retrieves performance details for a specific district.
   */
  public DistrictPerformanceProjection getDistrictById(Long districtId) {
    List<DistrictPerformanceProjection> results = jdbcTemplate.query(
        DISTRICT_BY_ID_SQL, DISTRICT_ROW_MAPPER, districtId);
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Retrieves nationwide state performance summaries rolled up from district outcomes.
   */
  public List<StatePerformanceProjection> getStateSummaries() {
    return jdbcTemplate.query(STATE_SUMMARIES_SQL, STATE_ROW_MAPPER);
  }

  /**
   * Retrieves outcome summary for a single state.
   */
  public StatePerformanceProjection getStateSummaryById(Long stateId) {
    List<StatePerformanceProjection> results = jdbcTemplate.query(
        STATE_SUMMARY_BY_ID_SQL, STATE_ROW_MAPPER, stateId);
    return results.isEmpty() ? null : results.get(0);
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
