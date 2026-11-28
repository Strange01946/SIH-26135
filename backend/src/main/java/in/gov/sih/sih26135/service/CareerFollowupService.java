package in.gov.sih.sih26135.service;

import static in.gov.sih.sih26135.service.LatestCareerUpdate.CONSENT_CODE;
import static in.gov.sih.sih26135.service.LatestCareerUpdate.SURVEY_CODE;

import in.gov.sih.sih26135.config.cache.AnalyticsCacheEvictionService;
import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerFollowupService {
  static final String MANUAL_CAMPAIGN = "SIH-CAREER-FOLLOWUP";
  private static final String AUTOMATIC_PREFIX = "SIH-CAREER-AUTO-";
  private static final String MANUAL_PREFIX = "SIH-CAREER-MANUAL-";
  private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

  private static final String ATTENDED_SESSIONS = """
      FROM attendance_records a
      JOIN ref_attendance_status ast ON ast.attendance_status_id=a.attendance_status_id
      JOIN training_enrollments e ON e.enrollment_id=a.enrollment_id AND e.trainee_id=a.trainee_id
        AND e.batch_id=a.batch_id AND e.deleted_at IS NULL
      WHERE ast.counts_as_present=1
      """;

  private static final String CAREER_TASKS = """
      FROM followup_tasks t
      JOIN followup_campaigns c ON c.followup_campaign_id=t.followup_campaign_id
      JOIN ref_followup_type ft ON ft.followup_type_id=c.followup_type_id
      JOIN ref_followup_status ts ON ts.followup_status_id=t.followup_status_id
      LEFT JOIN surveys s ON s.survey_id=COALESCE(t.survey_id,c.survey_id)
      LEFT JOIN ref_survey_purpose sp ON sp.survey_purpose_id=s.survey_purpose_id
      WHERE (sp.purpose_code='EMPLOYMENT_OUTCOME' OR sp.purpose_code LIKE 'RETENTION_%'
        OR ft.followup_offset_months IS NOT NULL)
      """;

  private static final String DUE_FOLLOWUPS = """
      WITH tracked_dates AS (
        SELECT tr.trainee_id,
          (SELECT DATE_ADD(MAX(a.session_date), INTERVAL 6 MONTH)
      """ + ATTENDED_SESSIONS + """
            AND a.trainee_id=tr.trainee_id AND a.session_date<=:today) AS training_due,
          (SELECT DATE(MAX(t.completed_at))
      """ + CAREER_TASKS + """
            AND t.trainee_id=tr.trainee_id AND ts.status_code='COMPLETED'
            AND t.completed_at < DATE_ADD(:today, INTERVAL 1 DAY)) AS last_followup
        FROM trainees tr WHERE tr.deleted_at IS NULL AND tr.trainee_id>:afterId AND tr.trainee_id<=:throughId
      ),
      milestones AS (
        SELECT trainee_id, CASE
          WHEN last_followup IS NULL THEN training_due
          WHEN training_due IS NULL OR last_followup>=training_due
            THEN DATE_ADD(last_followup, INTERVAL 6 MONTH)
          ELSE LEAST(training_due, DATE_ADD(last_followup, INTERVAL 6 MONTH))
        END AS due_on
        FROM tracked_dates
      )
      SELECT m.trainee_id,m.due_on
      FROM milestones m
      WHERE m.due_on<=:today
        AND NOT EXISTS (
          SELECT 1 FROM followup_tasks t JOIN followup_campaigns c USING (followup_campaign_id)
          WHERE t.trainee_id=m.trainee_id AND c.campaign_code=CONCAT(:autoPrefix,m.due_on)
        )
        AND NOT EXISTS (
          SELECT 1
      """ + CAREER_TASKS + """
            AND t.trainee_id=m.trainee_id AND ts.is_open_flag=1 AND t.completed_at IS NULL
            AND t.scheduled_date<=:today AND (t.next_followup_date IS NULL OR t.next_followup_date<=:today)
            AND c.deleted_at IS NULL AND s.deleted_at IS NULL
            AND c.lifecycle_status_id IN (SELECT lifecycle_status_id FROM ref_lifecycle_status WHERE status_code='ACTIVE')
            AND s.survey_instance_status_id IN (SELECT survey_instance_status_id FROM ref_survey_instance_status WHERE status_code='OPEN')
            AND c.scheduled_start_date<=:today AND s.scheduled_start_date<=:today
            AND (c.scheduled_end_date IS NULL OR c.scheduled_end_date>=:today)
            AND (s.scheduled_end_date IS NULL OR s.scheduled_end_date>=:today)
        )
      ORDER BY m.trainee_id LIMIT :limit
      """;

  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;
  private final TraineeConsentService consent;
  private final FollowupTaskService tasks;
  private final AnalyticsCacheEvictionService analytics;

  public CareerFollowupService(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc,
      TraineeConsentService consent, FollowupTaskService tasks, AnalyticsCacheEvictionService analytics) {
    this.jdbc = jdbc;
    this.namedJdbc = namedJdbc;
    this.consent = consent;
    this.tasks = tasks;
    this.analytics = analytics;
  }

  static LocalDate today() {
    return LocalDate.now(INDIA);
  }

  List<Long> dueTrainees(LocalDate today, long afterId, int limit) {
    return dueFollowups(today, afterId, Long.MAX_VALUE, limit).stream().map(Milestone::traineeId).toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public boolean assignIfDue(long traineeId, LocalDate today) {
    if (jdbc.queryForList("SELECT trainee_id FROM trainees WHERE trainee_id=? AND deleted_at IS NULL FOR UPDATE",
        Long.class, traineeId).isEmpty() || !consent.hasActiveConsent(traineeId, CONSENT_CODE)) {
      return false;
    }
    var due = dueFollowups(today, traineeId - 1, traineeId, 1);
    if (due.isEmpty()) {
      return false;
    }

    LocalDate dueOn = due.get(0).dueOn();
    var survey = jdbc.queryForList("""
        SELECT s.survey_id FROM surveys s JOIN ref_survey_instance_status ss USING (survey_instance_status_id)
        WHERE s.survey_code=? AND s.deleted_at IS NULL AND ss.status_code='OPEN'
          AND s.scheduled_start_date<=? AND (s.scheduled_end_date IS NULL OR s.scheduled_end_date>=?)
          AND s.program_id IS NULL AND s.course_id IS NULL AND s.batch_id IS NULL
          AND EXISTS (SELECT 1 FROM survey_questions q WHERE q.survey_template_version_id=s.survey_template_version_id)
        """, Long.class, SURVEY_CODE, today, today);
    if (survey.isEmpty()) {
      return false;
    }
    long surveyId = survey.get(0);
    Long campaignId = campaignOccurrence(AUTOMATIC_PREFIX + dueOn, "Automatic six-month employment follow-up",
        "MONTH_6", surveyId, dueOn, null, null);
    var enrollment = jdbc.queryForList("SELECT e.enrollment_id " + ATTENDED_SESSIONS + """
        AND a.trainee_id=? AND a.session_date<=?
        ORDER BY a.session_date DESC,a.attendance_id DESC LIMIT 1
        """, Long.class, traineeId, today);
    Long pending = jdbc.queryForObject("SELECT followup_status_id FROM ref_followup_status WHERE status_code='PENDING'",
        Long.class);
    var task = new CreateFollowupTaskRequest(campaignId, traineeId, dueOn, pending);
    task.setEnrollmentId(enrollment.isEmpty() ? null : enrollment.get(0));
    task.setSurveyId(surveyId);
    tasks.createFollowupTask(task);
    analytics.evictOnFollowupOrSurveySubmitted(this);
    return true;
  }

  long manualOccurrence(long surveyId, LocalDate date, LocalDate closesOn, Long userId) {
    return campaignOccurrence(MANUAL_PREFIX + date, "Employment outcome follow-up", "AD_HOC", surveyId, date, closesOn, userId);
  }

  private Long campaignOccurrence(String code, String name, String type, long surveyId, LocalDate date, LocalDate closesOn,
      Long userId) {
    // The database allows one assignment per trainee in each campaign. Each date is a separate wave.
    jdbc.update("""
        INSERT INTO followup_campaigns(campaign_code,campaign_name,followup_type_id,survey_id,scheduled_start_date,
          scheduled_end_date,lifecycle_status_id,created_by_user_id)
        SELECT ?,?,ft.followup_type_id,?,?,?,cs.lifecycle_status_id,?
        FROM ref_followup_type ft CROSS JOIN ref_lifecycle_status cs
        WHERE ft.type_code=? AND cs.status_code='ACTIVE'
        ON DUPLICATE KEY UPDATE followup_campaign_id=followup_campaigns.followup_campaign_id
        """, code, name, surveyId, date, closesOn, userId, type);
    return jdbc.queryForObject("SELECT followup_campaign_id FROM followup_campaigns WHERE campaign_code=? AND deleted_at IS NULL",
        Long.class, code);
  }

  private List<Milestone> dueFollowups(LocalDate today, long afterId, long throughId, int limit) {
    return namedJdbc.query(DUE_FOLLOWUPS, Map.of("today", today, "afterId", afterId, "throughId", throughId,
        "limit", limit, "autoPrefix", AUTOMATIC_PREFIX),
        (rs, row) -> new Milestone(rs.getLong(1), rs.getObject(2, LocalDate.class)));
  }

  private record Milestone(long traineeId, LocalDate dueOn) {}
}
