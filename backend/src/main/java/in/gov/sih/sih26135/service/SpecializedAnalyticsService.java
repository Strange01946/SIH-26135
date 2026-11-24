package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.repository.analytics.util.AnalyticsQueryFilterBuilder;
import in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SpecializedAnalyticsService {
  public record Bucket(String label, Long count) {}
  public record WageDistributions(List<Bucket> currentWages, List<Bucket> beforeTraining,
                                  List<Bucket> afterTraining, List<Bucket> pairedChange) {}
  public record ExitArea(String key, String name, Long exits) {}
  public record ExitAreas(List<ExitArea> sectors, List<ExitArea> districts) {}
  public record DropoutSignal(String key, String district, String provider, Long attendanceDips,
                              Long assessmentLags, Long candidatesWithSignal) {}
  public record IncidentDay(LocalDate day, long errors, long critical) {}

  private record Counts(String key, String label, long events, long candidates) {}
  private record WageCount(String series, int bracket, long count) {}
  private static final List<String> SALARY_LABELS = List.of("Under ₹10,000", "₹10,000–<₹15,000",
      "₹15,000–<₹25,000", "₹25,000 and above");
  private static final List<String> CHANGE_LABELS = List.of("Decline", "No change", "Increase <₹5,000",
      "Increase ₹5,000–<₹10,000", "Increase ₹10,000 and above");
  private static final String ENROLLMENTS = """
      WITH enrolled AS (
        SELECT v.enrollment_id, v.trainee_id, v.enrollment_date, v.actual_completion_date, v.batch_id,
               v.provider_id, v.trainee_district_id, v.enrollment_status_code
        FROM vw_enrollment_outcome_fact v WHERE v.enrollment_date <= CURDATE()
      """;
  private final JdbcTemplate jdbc;
  private final AnalyticsPrivacySuppressionService privacy;

  public SpecializedAnalyticsService(JdbcTemplate jdbc, AnalyticsPrivacySuppressionService privacy) {
    this.jdbc = jdbc;
    this.privacy = privacy;
  }

  public WageDistributions wages(AnalyticsFilterCriteria criteria) {
    var filter = AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    var counts = jdbc.query(ENROLLMENTS + filter.getSqlFragment() + """
        ), monthly_pay AS (
          SELECT e.employment_id, e.trainee_id, e.enrollment_id, e.start_date, e.end_date,
                 e.is_current, sh.salary_amount, sh.effective_from, sh.effective_to, sh.salary_history_id
          FROM employment_records e JOIN salary_history sh ON sh.employment_id = e.employment_id
          JOIN ref_salary_frequency sf ON sf.salary_frequency_id = sh.salary_frequency_id
          JOIN ref_engagement_type et ON et.engagement_type_id = e.engagement_type_id AND et.is_wage_employment = 1
          WHERE e.deleted_at IS NULL AND sf.frequency_code = 'MONTHLY' AND sh.currency_code = 'INR'
            AND e.start_date <= CURDATE() AND sh.effective_from BETWEEN e.start_date AND COALESCE(e.end_date, CURDATE())
            AND sh.effective_from <= CURDATE()
        ), current_candidates AS (
          SELECT p.salary_amount,
                 ROW_NUMBER() OVER (PARTITION BY p.trainee_id ORDER BY p.effective_from DESC, p.salary_history_id DESC) rn
          FROM monthly_pay p JOIN enrolled e ON e.enrollment_id = p.enrollment_id
          WHERE p.is_current = 1 AND (p.effective_to IS NULL OR p.effective_to >= CURDATE())
        ), observations AS (
          SELECT e.enrollment_id, e.trainee_id, e.actual_completion_date,
            (SELECT p.salary_amount FROM monthly_pay p
             WHERE p.trainee_id = e.trainee_id AND p.effective_from < e.enrollment_date
             ORDER BY p.effective_from DESC, p.salary_history_id DESC LIMIT 1) AS before_pay,
            (SELECT p.salary_amount FROM monthly_pay p
             WHERE p.enrollment_id = e.enrollment_id AND p.start_date >= e.actual_completion_date
               AND p.effective_from >= e.actual_completion_date
             ORDER BY p.effective_from ASC, p.salary_history_id ASC LIMIT 1) AS after_pay
          FROM enrolled e WHERE e.actual_completion_date IS NOT NULL
        ), paired AS (
          SELECT before_pay, after_pay,
                 ROW_NUMBER() OVER (PARTITION BY trainee_id ORDER BY actual_completion_date DESC, enrollment_id DESC) rn
          FROM observations WHERE before_pay IS NOT NULL AND after_pay IS NOT NULL
        ), levels AS (
          SELECT 'current' series, salary_amount AS amount FROM current_candidates WHERE rn = 1
          UNION ALL SELECT 'before', before_pay FROM paired WHERE rn = 1
          UNION ALL SELECT 'after', after_pay FROM paired WHERE rn = 1
        )
        SELECT series, CASE WHEN amount < 10000 THEN 0 WHEN amount < 15000 THEN 1
                            WHEN amount < 25000 THEN 2 ELSE 3 END bracket, COUNT(*) cohort_size
        FROM levels GROUP BY series, bracket
        UNION ALL
        SELECT 'change', CASE WHEN after_pay < before_pay THEN 0 WHEN after_pay = before_pay THEN 1
          WHEN after_pay - before_pay < 5000 THEN 2 WHEN after_pay - before_pay < 10000 THEN 3 ELSE 4 END bracket, COUNT(*)
        FROM paired WHERE rn = 1 GROUP BY bracket
        """, (rs, row) -> new WageCount(rs.getString(1), rs.getInt(2), rs.getLong(3)), filter.getParameters().toArray());
    return new WageDistributions(buckets(counts, "current", SALARY_LABELS), buckets(counts, "before", SALARY_LABELS),
        buckets(counts, "after", SALARY_LABELS), buckets(counts, "change", CHANGE_LABELS));
  }

  private List<Bucket> buckets(List<WageCount> counts, String series, List<String> labels) {
    Map<Integer, Long> values = counts.stream().filter(row -> row.series().equals(series))
        .collect(Collectors.toMap(WageCount::bracket, WageCount::count));
    var groups = new ArrayList<Counts>();
    for (int i = 0; i < labels.size(); i++) {
      long value = values.getOrDefault(i, 0L);
      groups.add(new Counts(Integer.toString(i), labels.get(i), value, value));
    }
    var withheld = withheldGroups(groups);
    return groups.stream().map(group -> new Bucket(group.label(), withheld.contains(group.key()) ? null : group.events())).toList();
  }

  public ExitAreas exitAreas(AnalyticsFilterCriteria criteria) {
    var filter = AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    String eligibility = filter.hasPredicates() ? """
        AND EXISTS (SELECT 1 FROM vw_enrollment_outcome_fact v
                    WHERE v.enrollment_id = er.enrollment_id
        """ + filter.getSqlFragment() + ")" : "";
    var exits = jdbc.query("""
        WITH exits AS (
          SELECT x.trainee_id, x.trainee_district_id, c.sector_id
          FROM vw_employment_exit_fact x JOIN employment_records er ON er.employment_id = x.employment_id
          LEFT JOIN training_enrollments en ON en.enrollment_id = er.enrollment_id AND en.deleted_at IS NULL
          LEFT JOIN courses c ON c.course_id = en.course_id AND c.deleted_at IS NULL
          WHERE er.deleted_at IS NULL AND x.separation_date <= CURDATE()
        """ + eligibility + """
        )
        SELECT 'sector' dimension, COALESCE(CAST(s.sector_id AS CHAR), 'unknown') area_key,
               COALESCE(s.sector_name, 'Sector not recorded') label,
               COUNT(*) exit_count, COUNT(DISTINCT x.trainee_id) people
        FROM exits x LEFT JOIN sectors s ON s.sector_id = x.sector_id GROUP BY s.sector_id, s.sector_name
        UNION ALL
        SELECT 'district', COALESCE(CAST(d.district_id AS CHAR), 'unknown'),
               COALESCE(CONCAT(d.district_name, ', ', st.state_name), 'District not recorded'),
               COUNT(*), COUNT(DISTINCT x.trainee_id)
        FROM exits x LEFT JOIN districts d ON d.district_id = x.trainee_district_id
        LEFT JOIN states st ON st.state_id = d.state_id GROUP BY d.district_id, d.district_name, st.state_name
        """, (rs, row) -> Map.entry(rs.getString(1), new Counts(rs.getString(2), rs.getString(3),
            rs.getLong(4), rs.getLong(5))), filter.getParameters().toArray());
    return new ExitAreas(exitAreas(exits, "sector"), exitAreas(exits, "district"));
  }

  private List<ExitArea> exitAreas(List<Map.Entry<String, Counts>> exits, String dimension) {
    var groups = exits.stream().filter(row -> row.getKey().equals(dimension)).map(Map.Entry::getValue).toList();
    var withheld = withheldGroups(groups);
    return groups.stream().sorted(Comparator.comparing(Counts::label))
        .map(row -> new ExitArea(row.key(), row.label(), withheld.contains(row.key()) ? null : row.events())).toList();
  }

  private List<String> withheldGroups(List<Counts> groups) {
    var withheld = new ArrayList<>(groups.stream().filter(row -> privacy.isSparse(row.candidates()))
        .map(Counts::key).toList());
    if (withheld.size() == 1) {
      groups.stream().filter(row -> row.candidates() >= AnalyticsPrivacySuppressionService.MIN_ANONYMITY_THRESHOLD)
          .min(Comparator.comparingLong(Counts::events).thenComparing(Counts::key))
          .ifPresent(row -> withheld.add(row.key()));
    }
    return withheld;
  }

  public List<DropoutSignal> dropoutSignals(AnalyticsFilterCriteria criteria) {
    var filter = AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    return jdbc.query(ENROLLMENTS + filter.getSqlFragment() + """
          AND v.enrollment_status_code IN ('ENROLLED', 'IN_PROGRESS')
        ), attendance AS (
          SELECT a.enrollment_id,
                 SUM(a.session_date >= CURDATE() - INTERVAL 13 DAY) recent_count,
                 SUM(a.session_date < CURDATE() - INTERVAL 13 DAY) earlier_count,
                 AVG(IF(a.session_date >= CURDATE() - INTERVAL 13 DAY, st.counts_as_present, NULL)) recent_rate,
                 AVG(IF(a.session_date < CURDATE() - INTERVAL 13 DAY, st.counts_as_present, NULL)) earlier_rate
          FROM attendance_records a JOIN enrolled e ON e.enrollment_id = a.enrollment_id
          JOIN ref_attendance_status st USING (attendance_status_id)
          WHERE a.session_date BETWEEN CURDATE() - INTERVAL 27 DAY AND CURDATE()
            AND a.session_date >= e.enrollment_date
          GROUP BY a.enrollment_id
        ), signals AS (
          SELECT e.trainee_id, e.provider_id, e.trainee_district_id,
                 IFNULL(a.recent_count >= 3 AND a.earlier_count >= 3 AND a.recent_rate < 0.75
                    AND a.earlier_rate - a.recent_rate >= 0.15, 0) attendance_dip,
                 EXISTS (SELECT 1 FROM assessments exam WHERE exam.batch_id = e.batch_id
                   AND exam.deleted_at IS NULL AND exam.assessment_date BETWEEN e.enrollment_date AND CURDATE() - INTERVAL 7 DAY
                   AND NOT EXISTS (SELECT 1 FROM trainee_assessments attempt
                     WHERE attempt.assessment_id = exam.assessment_id AND attempt.enrollment_id = e.enrollment_id
                       AND attempt.trainee_id = e.trainee_id AND attempt.appeared_flag = 1
                       AND attempt.assessment_date <= CURDATE())) assessment_lag
          FROM enrolled e LEFT JOIN attendance a ON a.enrollment_id = e.enrollment_id
          JOIN training_batches b ON b.batch_id = e.batch_id AND b.deleted_at IS NULL
          JOIN ref_batch_status bs ON bs.batch_status_id = b.batch_status_id AND bs.status_code IN ('OPEN', 'ONGOING')
        )
        SELECT CONCAT(p.provider_id, '-', COALESCE(d.district_id, 0)) team,
               COALESCE(d.district_name, 'District not recorded'), p.provider_name,
               COUNT(DISTINCT IF(s.attendance_dip = 1, s.trainee_id, NULL)),
               COUNT(DISTINCT IF(s.assessment_lag = 1, s.trainee_id, NULL)),
               COUNT(DISTINCT IF(s.attendance_dip = 1 OR s.assessment_lag = 1, s.trainee_id, NULL)),
               COUNT(DISTINCT s.trainee_id)
        FROM signals s JOIN training_providers p USING (provider_id)
        LEFT JOIN districts d ON d.district_id = s.trainee_district_id
        GROUP BY p.provider_id, p.provider_name, d.district_id, d.district_name
        ORDER BY d.district_name, p.provider_name
        """, (rs, row) -> {
      long attendance = rs.getLong(4);
      long assessment = rs.getLong(5);
      long any = rs.getLong(6);
      boolean withhold = privacy.isSparse(attendance) || privacy.isSparse(assessment)
          || privacy.isSparse(any - attendance) || privacy.isSparse(any - assessment)
          || privacy.isSparse(any) || privacy.isSparse(rs.getLong(7) - any);
      return new DropoutSignal(rs.getString(1), rs.getString(2), rs.getString(3), withhold ? null : attendance,
          withhold ? null : assessment, withhold ? null : any);
    }, filter.getParameters().toArray());
  }

  public List<IncidentDay> incidents(int days) {
    LocalDate today = LocalDate.now();
    LocalDate first = today.minusDays(days - 1L);
    var recorded = jdbc.query("""
        SELECT DATE(e.occurred_at), SUM(s.severity_code = 'ERROR'), SUM(s.severity_code = 'CRITICAL')
        FROM system_event_logs e JOIN ref_system_event_severity s USING (system_event_severity_id)
        WHERE e.occurred_at >= ? AND e.occurred_at < ? AND s.severity_code IN ('ERROR', 'CRITICAL')
        GROUP BY DATE(e.occurred_at)
        """, (rs, row) -> new IncidentDay(rs.getObject(1, LocalDate.class), rs.getLong(2), rs.getLong(3)),
        first.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
        .collect(Collectors.toMap(IncidentDay::day, row -> row));
    return first.datesUntil(today.plusDays(1)).map(day -> recorded.getOrDefault(day, new IncidentDay(day, 0, 0))).toList();
  }
}
