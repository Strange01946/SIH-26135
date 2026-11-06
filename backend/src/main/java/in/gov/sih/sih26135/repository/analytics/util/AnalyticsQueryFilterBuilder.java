package in.gov.sih.sih26135.repository.analytics.util;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for constructing parameterized SQL WHERE clauses from validated {@link AnalyticsFilterCriteria}.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Parameterized binding via JDBC {@code ?} markers to prevent SQL injection.</li>
 *   <li>Safe column resolution matching authoritative schema views (e.g. {@code vw_enrollment_outcome_fact}).</li>
 *   <li>Pre-aggregation filtering ensuring mathematically sound rate and percentage calculations.</li>
 * </ul>
 */
public final class AnalyticsQueryFilterBuilder {

  private AnalyticsQueryFilterBuilder() {
    // Utility class
  }

  /**
   * Result containing the WHERE/AND clause SQL fragment and the associated positional parameter values.
   */
  public static final class FilterPredicateResult {
    private final String sqlFragment;
    private final List<Object> parameters;

    public FilterPredicateResult(String sqlFragment, List<Object> parameters) {
      this.sqlFragment = sqlFragment;
      this.parameters = parameters;
    }

    public String getSqlFragment() {
      return sqlFragment;
    }

    public List<Object> getParameters() {
      return parameters;
    }

    public boolean hasPredicates() {
      return !parameters.isEmpty();
    }
  }

  /**
   * Builds parameterized SQL predicates applicable to {@code vw_enrollment_outcome_fact}
   * or tables sharing its grain and column names.
   *
   * @param criteria validated filter criteria (may be null)
   * @param tableAlias optional table alias (e.g. "v" or "e"), or empty/null
   * @return {@link FilterPredicateResult} containing the SQL fragment and bound parameters
   */
  public static FilterPredicateResult buildEnrollmentFactPredicates(AnalyticsFilterCriteria criteria, String tableAlias) {
    if (criteria == null || criteria.isEmpty()) {
      return new FilterPredicateResult("", List.of());
    }

    String prefix = (tableAlias != null && !tableAlias.isBlank()) ? (tableAlias + ".") : "";
    StringBuilder sb = new StringBuilder();
    List<Object> params = new ArrayList<>();

    // 1. Temporal predicates
    LocalDate effectiveStart = criteria.resolveEffectiveStartDate();
    if (effectiveStart != null) {
      sb.append(" AND ").append(prefix).append("enrollment_date >= ?");
      params.add(effectiveStart);
    }
    LocalDate effectiveEnd = criteria.resolveEffectiveEndDate();
    if (effectiveEnd != null) {
      sb.append(" AND ").append(prefix).append("enrollment_date <= ?");
      params.add(effectiveEnd);
    }

    // 2. Organizational predicates
    if (criteria.getSchemeId() != null) {
      sb.append(" AND ").append(prefix).append("scheme_id = ?");
      params.add(criteria.getSchemeId());
    }
    if (criteria.getProgramId() != null) {
      sb.append(" AND ").append(prefix).append("program_id = ?");
      params.add(criteria.getProgramId());
    }
    if (criteria.getCourseId() != null) {
      sb.append(" AND ").append(prefix).append("course_id = ?");
      params.add(criteria.getCourseId());
    }
    if (criteria.getSectorId() != null) {
      sb.append(" AND ").append(prefix).append("course_id IN (SELECT co.course_id FROM courses co WHERE co.sector_id = ? AND co.deleted_at IS NULL)");
      params.add(criteria.getSectorId());
    }

    // 3. Spatial predicates
    if (criteria.getStateId() != null) {
      sb.append(" AND ").append(prefix).append("trainee_state_id = ?");
      params.add(criteria.getStateId());
    }
    if (criteria.getDistrictId() != null) {
      sb.append(" AND ").append(prefix).append("trainee_district_id = ?");
      params.add(criteria.getDistrictId());
    }

    // 4. Demographic predicates
    Long genderId = criteria.resolveGenderId();
    if (genderId != null) {
      sb.append(" AND ").append(prefix).append("trainee_id IN (SELECT tr.trainee_id FROM trainees tr WHERE tr.gender_id = ? AND tr.deleted_at IS NULL)");
      params.add(genderId);
    }

    return new FilterPredicateResult(sb.toString(), params);
  }
}
