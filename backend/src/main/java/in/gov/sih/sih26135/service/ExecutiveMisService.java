package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.repository.analytics.util.AnalyticsQueryFilterBuilder;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import in.gov.sih.sih26135.util.OperationalMetricsMath;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExecutiveMisService {
  public enum Availability { RELEASED, SUPPRESSED, NO_DATA, NOT_APPLICABLE }
  public record Identifier(long id, String name) {}
  public record SchemeOption(long id, String name, String administration) {}
  public record StateOption(long id, String code, String name) {}
  public record DistrictOption(long id, long stateId, String name) {}
  public record GenderOption(String code, String name) {}
  public record FilterOptions(List<String> financialYears, List<SchemeOption> schemes, List<Identifier> sectors,
                              List<StateOption> states, List<DistrictOption> districts, List<GenderOption> genders,
                              boolean socialCategorySupported, long suppressionThreshold) {}
  public record Measure(BigDecimal value, Availability availability) {}
  public record Kpi(String key, Measure current, BigDecimal changePct) {}
  public record Stage(String key, Measure candidates) {}
  public record ProviderQuality(long providerId, Measure scorePct) {}
  public record KpiComparison(List<Kpi> kpis, String previousFinancialYear) {}

  private record Cohort(long trainees, long enrollments, long certifiedTrainees, long placedEnrollments, long centers, long assessed,
                        long placedTrainees, long retainedAfterPlacement,
                        long eligibleForRetention, long retained) {}

  private final JdbcTemplate jdbc;

  public ExecutiveMisService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public FilterOptions filterOptions(AnalyticsFilterCriteria criteria) {
    Long stateId = criteria.getStateId();
    Long districtId = criteria.getDistrictId();
    var years = new java.util.TreeSet<Integer>(java.util.Comparator.reverseOrder());
    years.add(fiscalStart(LocalDate.now()));
    years.add(fiscalStart(LocalDate.now()) - 1);
    years.addAll(jdbc.queryForList("""
        SELECT DISTINCT YEAR(DATE_SUB(enrollment_date, INTERVAL 3 MONTH))
        FROM vw_enrollment_outcome_fact WHERE enrollment_date >= '2000-04-01'
          AND (? IS NULL OR trainee_state_id = ?) AND (? IS NULL OR trainee_district_id = ?)
          AND enrollment_date < '2100-04-01'
        """, Integer.class, stateId, stateId, districtId, districtId));
    return new FilterOptions(years.stream().map(ExecutiveMisService::yearName).toList(), jdbc.query("""
        SELECT s.scheme_id, s.scheme_name, CASE ot.type_code
                 WHEN 'CENTRAL_MINISTRY' THEN 'CENTRAL'
                 WHEN 'STATE_DEPARTMENT' THEN 'STATE'
                 ELSE 'OTHER' END
        FROM schemes s JOIN departments d ON d.department_id = s.department_id
        JOIN organizations o ON o.organization_id = d.organization_id
        JOIN ref_organization_type ot ON ot.organization_type_id = o.organization_type_id
        WHERE s.deleted_at IS NULL ORDER BY s.scheme_name
        """, (rs, row) -> new SchemeOption(rs.getLong(1), rs.getString(2), rs.getString(3))),
        jdbc.query("SELECT sector_id, sector_name FROM sectors ORDER BY sector_name",
            (rs, row) -> new Identifier(rs.getLong(1), rs.getString(2))),
        jdbc.query("SELECT s.state_id, s.state_code, s.state_name " + ActiveGeography.STATES + " AND (? IS NULL OR s.state_id = ?) ORDER BY s.state_name",
            (rs, row) -> new StateOption(rs.getLong(1), rs.getString(2), rs.getString(3)), stateId, stateId),
        jdbc.query("SELECT d.district_id, d.state_id, d.district_name " + ActiveGeography.DISTRICTS + " AND (? IS NULL OR d.state_id = ?) AND (? IS NULL OR d.district_id = ?) ORDER BY d.district_name",
            (rs, row) -> new DistrictOption(rs.getLong(1), rs.getLong(2), rs.getString(3)), stateId, stateId, districtId, districtId),
        jdbc.query("""
            SELECT gender_code, gender_name FROM ref_gender
            WHERE gender_code IN ('MALE', 'FEMALE', 'TRANSGENDER', 'OTHER', 'PREFER_NOT_TO_SAY') ORDER BY sort_order
            """, (rs, row) -> new GenderOption(rs.getString(1), rs.getString(2))),
        false, 0);
  }

  public KpiComparison compareKpis(AnalyticsFilterCriteria criteria) {
    Cohort cohort = cohort(criteria);
    String previous = null;
    List<Measure> baseline = List.of();
    LocalDate start = criteria.resolveEffectiveStartDate();
    if (criteria.getFiscalYear() != null && start != null && start.getYear() > 2000) {
      previous = yearName(start.getYear() - 1);
      var previousCriteria = new AnalyticsFilterCriteria();
      previousCriteria.setFiscalYear(previous);
      previousCriteria.setSchemeId(criteria.getSchemeId());
      previousCriteria.setProgramId(criteria.getProgramId());
      previousCriteria.setSectorId(criteria.getSectorId());
      previousCriteria.setCourseId(criteria.getCourseId());
      previousCriteria.setStateId(criteria.getStateId());
      previousCriteria.setDistrictId(criteria.getDistrictId());
      previousCriteria.setGender(criteria.getGender());
      baseline = measures(cohort(previousCriteria));
    }
    List<Measure> current = measures(cohort);
    List<String> keys = List.of("enrolled", "certified", "placed", "retention6m", "activeCenters");
    List<Kpi> kpis = new ArrayList<>();
    for (int i = 0; i < keys.size(); i++) {
      Measure value = current.get(i);
      Measure before = baseline.isEmpty() ? null : baseline.get(i);
      kpis.add(new Kpi(keys.get(i), value, percentChange(value, before)));
    }
    return new KpiComparison(kpis, previous);
  }

  public List<Stage> pipeline(AnalyticsFilterCriteria criteria) {
    return pipeline(criteria, cohort(criteria));
  }

  private static String yearName(int start) {
    return String.format("FY%04d-%02d", start, (start + 1) % 100);
  }

  private static int fiscalStart(LocalDate date) {
    return date.minusMonths(3).getYear();
  }

  private Cohort cohort(AnalyticsFilterCriteria criteria) {
    var filter = AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    return jdbc.queryForObject("""
        WITH cohort AS (
          SELECT v.enrollment_id, v.trainee_id, v.center_id, v.has_issued_certificate_flag AS certified,
                 v.has_joined_placement_flag AS placed,
                 COALESCE(v.actual_completion_date, IF(v.is_completed_flag = 1, b.end_date, NULL)) AS completed_on,
                 EXISTS (SELECT 1 FROM trainee_assessments ta JOIN assessments a USING (assessment_id)
                         LEFT JOIN batch_assessment_publications pub USING (assessment_id)
                         WHERE ta.enrollment_id = v.enrollment_id AND ta.trainee_id = v.trainee_id
                           AND ta.appeared_flag = 1 AND a.deleted_at IS NULL
                           AND (pub.completed_at IS NOT NULL OR (pub.assessment_id IS NULL
                             AND COALESCE(ta.assessment_date, a.assessment_date) <= CURDATE()))) AS assessed
          FROM vw_enrollment_outcome_fact v LEFT JOIN training_batches b ON b.batch_id = v.batch_id WHERE 1 = 1
        """ + filter.getSqlFragment() + """
        ), retention AS (
          SELECT e.enrollment_id, MAX(r.retained_6m_flag) AS retained
          FROM cohort v JOIN vw_employment_fact e ON e.enrollment_id = v.enrollment_id AND e.trainee_id = v.trainee_id
          JOIN vw_employment_retention_fact r ON r.employment_id = e.employment_id
          WHERE e.is_wage_employment = 1 AND e.start_date >= v.completed_on AND r.retained_6m_flag IS NOT NULL
          GROUP BY e.enrollment_id
        )
        SELECT COUNT(DISTINCT v.trainee_id), COUNT(*),
          COUNT(DISTINCT IF(v.certified, v.trainee_id, NULL)),
          COALESCE(SUM(v.placed), 0),
          COUNT(DISTINCT IF(st.status_code = 'ACTIVE' AND c.deleted_at IS NULL, c.center_id, NULL)),
          COUNT(DISTINCT IF(v.assessed OR v.certified, v.trainee_id, NULL)),
          COUNT(DISTINCT IF(v.placed, v.trainee_id, NULL)),
          COUNT(DISTINCT IF(v.placed AND r.retained = 1, v.trainee_id, NULL)),
          COUNT(DISTINCT IF(r.retained IS NOT NULL, v.trainee_id, NULL)),
          COUNT(DISTINCT IF(r.retained = 1, v.trainee_id, NULL))
        FROM cohort v
        LEFT JOIN training_centers c ON c.center_id = v.center_id
        LEFT JOIN ref_lifecycle_status st ON st.lifecycle_status_id = c.lifecycle_status_id
        LEFT JOIN retention r ON r.enrollment_id = v.enrollment_id
        """, (rs, row) -> new Cohort(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
            rs.getLong(5), rs.getLong(6), rs.getLong(7), rs.getLong(8), rs.getLong(9), rs.getLong(10)),
        filter.getParameters().toArray());
  }

  private List<Measure> measures(Cohort c) {
    Measure retention = released(AnalyticsCalculationUtils.calculatePercentage(c.retained(), c.eligibleForRetention()));
    return List.of(count(c.enrollments()), count(c.certifiedTrainees()), count(c.placedEnrollments()), retention, count(c.centers()));
  }

  private List<Stage> pipeline(AnalyticsFilterCriteria criteria, Cohort c) {
    Long registrations = criteria.hasOrganizationalFilter() ? null : registrationCohort(criteria);
    List<String> keys = List.of("registered", "enrolled", "assessed", "certified", "placed", "retained");
    Long[] values = {registrations, c.trainees(), c.assessed(), c.certifiedTrainees(),
        c.placedTrainees(), c.retainedAfterPlacement()};
    List<Stage> stages = new ArrayList<>();
    for (int i = 0; i < keys.size(); i++) {
      Measure value = values[i] == null ? unavailable(Availability.NOT_APPLICABLE)
          : count(values[i]);
      stages.add(new Stage(keys.get(i), value));
    }
    return stages;
  }

  private long registrationCohort(AnalyticsFilterCriteria criteria) {
    var enrollment = AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    var scope = new StringBuilder();
    var params = new ArrayList<>();
    if (criteria.getStateId() != null) {
      scope.append(" AND t.state_id = ?");
      params.add(criteria.getStateId());
    }
    if (criteria.getDistrictId() != null) {
      scope.append(" AND t.district_id = ?");
      params.add(criteria.getDistrictId());
    }
    if (criteria.resolveGenderCode() != null) {
      scope.append(" AND t.gender_id = (SELECT gender_id FROM ref_gender WHERE gender_code = ?)");
      params.add(criteria.resolveGenderCode());
    }
    if (criteria.hasTemporalFilter()) {
      var period = new StringBuilder(" AND ((1 = 1");
      if (criteria.resolveEffectiveStartDate() != null) {
        period.append(" AND t.created_at >= ?");
        params.add(criteria.resolveEffectiveStartDate());
      }
      if (criteria.resolveEffectiveEndDate() != null) {
        period.append(" AND t.created_at < ?");
        params.add(criteria.resolveEffectiveEndDate().plusDays(1));
      }
      scope.append(period).append(") OR EXISTS (SELECT 1 FROM vw_enrollment_outcome_fact v WHERE v.trainee_id = t.trainee_id ")
          .append(enrollment.getSqlFragment()).append("))");
      params.addAll(enrollment.getParameters());
    }
    return jdbc.queryForObject("SELECT COUNT(*) FROM trainees t WHERE t.deleted_at IS NULL" + scope, Long.class, params.toArray());
  }

  public List<ProviderQuality> providerQuality(AnalyticsFilterCriteria criteria) {
    var filter = AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    return jdbc.query("""
        WITH evaluated AS (
          SELECT v.provider_id, EXISTS (
            SELECT 1 FROM data_quality_issues i
            JOIN ref_data_quality_issue_status s USING (data_quality_issue_status_id)
            WHERE UPPER(i.entity_type) IN ('BATCHENROLLMENT', 'BATCH_ENROLLMENT', 'TRAININGENROLLMENT', 'TRAINING_ENROLLMENT')
              AND i.entity_id = v.enrollment_id AND s.is_open_flag = 1
          ) AS affected
          FROM vw_enrollment_outcome_fact v WHERE 1 = 1
        """ + filter.getSqlFragment() + """
        )
        SELECT provider_id, COUNT(*), SUM(affected)
        FROM evaluated GROUP BY provider_id ORDER BY provider_id
        """, (rs, row) -> new ProviderQuality(rs.getLong(1),
        released(OperationalMetricsMath.dataCleanlinessIndex(rs.getLong(2), rs.getLong(3)))),
        filter.getParameters().toArray());
  }

  private static Measure count(long value) {
    return released(BigDecimal.valueOf(value));
  }

  private static Measure released(BigDecimal value) {
    return value != null ? new Measure(value, Availability.RELEASED) : unavailable(Availability.NO_DATA);
  }

  private static Measure unavailable(Availability state) {
    return new Measure(null, state);
  }

  private static BigDecimal percentChange(Measure now, Measure before) {
    if (before == null || now.value() == null || before.value() == null || before.value().signum() == 0) {
      return null;
    }
    return now.value().subtract(before.value()).multiply(BigDecimal.valueOf(100))
        .divide(before.value(), 2, RoundingMode.HALF_UP);
  }
}
