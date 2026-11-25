package in.gov.sih.sih26135.service;

import static in.gov.sih.sih26135.service.LatestCareerUpdate.CONSENT_CODE;
import static in.gov.sih.sih26135.service.LatestCareerUpdate.SURVEY_CODE;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.trainee.TraineeSurvey;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheEvictionService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TraineeSurveyService {
  private static final String OPEN_ASSIGNMENTS = """
      SELECT t.followup_task_id, s.survey_id, s.survey_name, s.survey_template_version_id,
             LEAST(COALESCE(s.scheduled_end_date, c.scheduled_end_date),
                   COALESCE(c.scheduled_end_date, s.scheduled_end_date)) AS closes_on
      FROM followup_tasks t
      JOIN followup_campaigns c ON c.followup_campaign_id = t.followup_campaign_id
      JOIN surveys s ON s.survey_id = COALESCE(t.survey_id, c.survey_id)
      JOIN ref_followup_status ts ON ts.followup_status_id = t.followup_status_id
      JOIN ref_lifecycle_status cs ON cs.lifecycle_status_id = c.lifecycle_status_id
      JOIN ref_survey_instance_status ss ON ss.survey_instance_status_id = s.survey_instance_status_id
      WHERE t.trainee_id = ? AND ts.is_open_flag = 1 AND t.completed_at IS NULL
        AND c.deleted_at IS NULL AND cs.status_code = 'ACTIVE'
        AND s.deleted_at IS NULL AND ss.status_code = 'OPEN'
        AND t.scheduled_date <= CURRENT_DATE
        AND (t.next_followup_date IS NULL OR t.next_followup_date <= CURRENT_DATE)
        AND c.scheduled_start_date <= CURRENT_DATE AND s.scheduled_start_date <= CURRENT_DATE
        AND (c.scheduled_end_date IS NULL OR c.scheduled_end_date >= CURRENT_DATE)
        AND (s.scheduled_end_date IS NULL OR s.scheduled_end_date >= CURRENT_DATE)
        AND NOT EXISTS (
          SELECT 1 FROM survey_responses r
          JOIN ref_survey_response_status rs ON rs.survey_response_status_id = r.survey_response_status_id
          WHERE r.followup_task_id = t.followup_task_id AND rs.is_submitted_flag = 1
        )
      """;
  private static final String AVAILABLE_CAMPAIGNS = """
      SELECT c.followup_campaign_id,c.campaign_name,s.survey_id,s.survey_name,
        LEAST(COALESCE(c.scheduled_end_date,s.scheduled_end_date),
              COALESCE(s.scheduled_end_date,c.scheduled_end_date)) AS closes_on,
        (SELECT en.enrollment_id FROM training_enrollments en
         WHERE en.trainee_id=? AND en.deleted_at IS NULL
           AND (c.program_id IS NULL OR c.program_id=en.program_id)
           AND (c.course_id IS NULL OR c.course_id=en.course_id)
           AND (s.program_id IS NULL OR s.program_id=en.program_id)
           AND (s.course_id IS NULL OR s.course_id=en.course_id)
           AND (s.batch_id IS NULL OR s.batch_id=en.batch_id)
         ORDER BY en.actual_completion_date DESC,en.enrollment_id DESC LIMIT 1) AS enrollment_id,
        COALESCE(c.program_id,c.course_id,s.program_id,s.course_id,s.batch_id) IS NOT NULL AS needs_enrollment
      FROM followup_campaigns c JOIN surveys s ON s.survey_id=c.survey_id
      JOIN ref_lifecycle_status cs ON cs.lifecycle_status_id=c.lifecycle_status_id
      JOIN ref_survey_instance_status ss ON ss.survey_instance_status_id=s.survey_instance_status_id
      WHERE c.deleted_at IS NULL AND cs.status_code='ACTIVE' AND s.deleted_at IS NULL AND ss.status_code='OPEN'
        AND c.scheduled_start_date<=CURRENT_DATE AND s.scheduled_start_date<=CURRENT_DATE
        AND (c.scheduled_end_date IS NULL OR c.scheduled_end_date>=CURRENT_DATE)
        AND (s.scheduled_end_date IS NULL OR s.scheduled_end_date>=CURRENT_DATE)
        AND EXISTS (SELECT 1 FROM survey_questions q WHERE q.survey_template_version_id=s.survey_template_version_id)
        AND NOT EXISTS (SELECT 1 FROM followup_tasks t WHERE t.followup_campaign_id=c.followup_campaign_id AND t.trainee_id=?)
      """;

  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final TraineeConsentService consent;
  private final FollowupEngagementWorkflowService followups;
  private final SurveyResponseService responses;
  private final SurveyResponseAnswerService answers;
  private final FollowupTaskService tasks;
  private final AnalyticsCacheEvictionService analytics;

  public TraineeSurveyService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope,
      TraineeConsentService consent, FollowupEngagementWorkflowService followups,
      SurveyResponseService responses, SurveyResponseAnswerService answers,
      FollowupTaskService tasks, AnalyticsCacheEvictionService analytics) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.consent = consent;
    this.followups = followups;
    this.responses = responses;
    this.answers = answers;
    this.tasks = tasks;
    this.analytics = analytics;
  }

  public TraineeSurvey.Overview getOverview() {
    long traineeId = currentTraineeId();
    boolean hasConsent = consent.hasActiveConsent(traineeId, CONSENT_CODE);
    var assignments = hasConsent
        ? jdbc.query(OPEN_ASSIGNMENTS + " ORDER BY t.scheduled_date, t.followup_task_id",
            (rs, row) -> assignment(rs).summary(), traineeId)
        : List.<TraineeSurvey.Assignment>of();
    return new TraineeSurvey.Overview(hasConsent, assignments, recentSubmissions(traineeId),
        hasConsent && !careerSurveys().isEmpty());
  }

  private List<TraineeSurvey.Submission> recentSubmissions(long traineeId) {
    return jdbc.query("""
        SELECT r.survey_response_id, s.survey_name, r.submitted_at
        FROM survey_responses r
        JOIN surveys s ON s.survey_id = r.survey_id
        JOIN ref_survey_response_status rs ON rs.survey_response_status_id = r.survey_response_status_id
        WHERE r.trainee_id = ? AND rs.is_submitted_flag = 1
        ORDER BY r.submitted_at DESC, r.survey_response_id DESC LIMIT 20
        """, (rs, row) -> new TraineeSurvey.Submission(rs.getLong(1), rs.getString(2),
            rs.getObject(3, LocalDateTime.class)), traineeId);
  }

  public TraineeSurvey.CareerUpdateQuestionnaire getCareerUpdate() {
    long traineeId = currentTraineeId();
    consent.validateConsent(traineeId, CONSENT_CODE);
    var survey = careerSurvey();
    var latest = LatestCareerUpdate.find(jdbc, traineeId);
    List<TraineeSurvey.Answer> previous = latest == null ? List.of() : jdbc.query("""
        SELECT a.survey_question_id, a.answer_text, a.numeric_value, a.date_value, a.selected_option_id
        FROM survey_response_answers a JOIN survey_questions q USING (survey_question_id)
        WHERE a.survey_response_id=? AND q.survey_template_version_id=?
        """, (rs, row) -> {
      Long optionId = rs.getObject("selected_option_id", Long.class);
      return new TraineeSurvey.Answer(rs.getLong("survey_question_id"), rs.getString("answer_text"),
          rs.getBigDecimal("numeric_value"), null, rs.getObject("date_value", LocalDate.class),
          optionId == null ? null : List.of(optionId));
    }, latest.submissionId(), survey.versionId());
    return new TraineeSurvey.CareerUpdateQuestionnaire(survey.name(), survey.versionId(), questions(survey.versionId()), previous);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TraineeSurvey.Submission submitCareerUpdate(TraineeSurvey.SubmitRequest request) {
    long traineeId = currentTraineeId();
    lockTrainee(traineeId);
    consent.validateConsent(traineeId, CONSENT_CODE);
    var survey = careerSurvey();
    var questions = validateSubmission(survey.versionId(), request);
    var draft = draft(survey.id(), survey.versionId(), traineeId);
    var enrollment = jdbc.queryForList("""
        SELECT enrollment_id FROM training_enrollments WHERE trainee_id=? AND deleted_at IS NULL
        ORDER BY actual_completion_date DESC,enrollment_id DESC LIMIT 1
        """, Long.class, traineeId);
    if (!enrollment.isEmpty()) draft.setEnrollmentId(enrollment.get(0));
    long responseId = responses.createSurveyResponse(draft).getId();
    var submitted = finish(responseId, survey.name(), request, questions);
    analytics.evictOnFollowupOrSurveySubmitted(this);
    return submitted;
  }

  public TraineeSurvey.SubmissionDetail getOwnSubmission(long id) {
    return submission(currentTraineeId(), id);
  }

  public TraineeSurvey.StaffOverview getStaffOverview(long traineeId) {
    scope.validateTraineeScope(traineeId);
    boolean granted = consent.hasActiveConsent(traineeId, CONSENT_CODE);
    var assignments = jdbc.query("""
        SELECT t.followup_task_id,COALESCE(s.survey_name,c.campaign_name),t.scheduled_date,st.status_name
        FROM followup_tasks t JOIN followup_campaigns c ON c.followup_campaign_id=t.followup_campaign_id
        LEFT JOIN surveys s ON s.survey_id=COALESCE(t.survey_id,c.survey_id)
        JOIN ref_followup_status st ON st.followup_status_id=t.followup_status_id
        WHERE t.trainee_id=? ORDER BY t.scheduled_date DESC,t.followup_task_id DESC LIMIT 20
        """, (rs, row) -> new TraineeSurvey.StaffAssignment(rs.getLong(1), rs.getString(2),
            rs.getObject(3, LocalDate.class), rs.getString(4)), traineeId);
    return new TraineeSurvey.StaffOverview(granted, granted ? campaigns(traineeId, null).stream()
        .map(AssignableCampaign::summary).toList() : List.of(), assignments,
        granted ? recentSubmissions(traineeId) : List.of());
  }

  public TraineeSurvey.SubmissionDetail getStaffSubmission(long traineeId, long submissionId) {
    scope.validateTraineeScope(traineeId);
    consent.validateConsent(traineeId, CONSENT_CODE);
    return submission(traineeId, submissionId);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TraineeSurvey.Assignment schedule(long traineeId, TraineeSurvey.ScheduleRequest request) {
    scope.validateTraineeScope(traineeId);
    lockTrainee(traineeId);
    consent.validateConsent(traineeId, CONSENT_CODE);
    AssignableCampaign campaign = campaigns(traineeId, request.campaignId()).stream().findFirst()
        .orElseThrow(() -> new ConflictException("This campaign is unavailable or already assigned.", "SURVEY_CAMPAIGN_UNAVAILABLE"));
    LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
    LocalDate closesOn = campaign.summary().closesOn();
    if (request.scheduledOn().isBefore(today) || (closesOn != null && request.scheduledOn().isAfter(closesOn))) {
      throw new BadRequestException("Choose a date within the open campaign window.", "SURVEY_SCHEDULE_OUTSIDE_WINDOW");
    }
    Long pending = jdbc.queryForObject("SELECT followup_status_id FROM ref_followup_status WHERE status_code='PENDING'", Long.class);
    var input = new CreateFollowupTaskRequest(campaign.summary().id(), traineeId, request.scheduledOn(), pending);
    input.setSurveyId(campaign.surveyId());
    input.setEnrollmentId(campaign.enrollmentId());
    input.setAssignedUserId(scope.resolveAuthenticatedUserId());
    var task = tasks.createFollowupTask(input);
    analytics.evictOnFollowupOrSurveySubmitted(this);
    return new TraineeSurvey.Assignment(task.getId(), campaign.summary().survey(), closesOn);
  }

  private List<AssignableCampaign> campaigns(long traineeId, Long campaignId) {
    return jdbc.query("SELECT * FROM (" + AVAILABLE_CAMPAIGNS + """
        ) available WHERE (enrollment_id IS NOT NULL OR NOT needs_enrollment)
          AND (? IS NULL OR followup_campaign_id=?) ORDER BY campaign_name,followup_campaign_id LIMIT 100
        """, (rs, row) -> new AssignableCampaign(new TraineeSurvey.Campaign(rs.getLong(1), rs.getString(2),
            rs.getString(4), rs.getObject(5, LocalDate.class)), rs.getLong(3), rs.getObject(6, Long.class)),
        traineeId, traineeId, campaignId, campaignId);
  }

  private record AssignableCampaign(TraineeSurvey.Campaign summary, long surveyId, Long enrollmentId) {}

  private TraineeSurvey.SubmissionDetail submission(long traineeId, long id) {
    var submission = jdbc.query("""
        SELECT r.survey_response_id,s.survey_name,r.submitted_at
        FROM survey_responses r JOIN surveys s ON s.survey_id=r.survey_id
        JOIN ref_survey_response_status rs ON rs.survey_response_status_id=r.survey_response_status_id
        WHERE r.survey_response_id=? AND r.trainee_id=? AND rs.is_submitted_flag=1
        """, (rs, row) -> new TraineeSurvey.Submission(rs.getLong(1), rs.getString(2),
            rs.getObject(3, LocalDateTime.class)), id, traineeId).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Survey submission not found"));
    var recorded = jdbc.query("""
        SELECT q.question_text,COALESCE(o.option_label,m.option_label,a.answer_text,
          CASE a.boolean_value WHEN 1 THEN 'Yes' WHEN 0 THEN 'No' END,
          CAST(a.numeric_value AS CHAR),CAST(a.date_value AS CHAR))
        FROM survey_response_answers a JOIN survey_questions q ON q.survey_question_id=a.survey_question_id
        LEFT JOIN survey_question_options o ON o.survey_question_option_id=a.selected_option_id
        LEFT JOIN survey_response_answer_options ao ON ao.survey_response_answer_id=a.survey_response_answer_id
        LEFT JOIN survey_question_options m ON m.survey_question_option_id=ao.survey_question_option_id
        WHERE a.survey_response_id=? ORDER BY q.display_order,m.display_order
        """, (rs, row) -> new TraineeSurvey.RecordedAnswer(rs.getString(1), rs.getString(2)), id);
    return new TraineeSurvey.SubmissionDetail(submission, recorded);
  }

  private OpenSurvey careerSurvey() {
    return careerSurveys().stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("The employment update questionnaire is unavailable"));
  }

  private List<OpenSurvey> careerSurveys() {
    return jdbc.query("""
        SELECT s.survey_id,s.survey_name,s.survey_template_version_id
        FROM surveys s JOIN ref_survey_instance_status st ON st.survey_instance_status_id=s.survey_instance_status_id
        WHERE s.survey_code=? AND s.deleted_at IS NULL AND st.status_code='OPEN'
          AND s.scheduled_start_date<=CURRENT_DATE AND (s.scheduled_end_date IS NULL OR s.scheduled_end_date>=CURRENT_DATE)
        """, (rs, row) -> new OpenSurvey(rs.getLong(1), rs.getString(2), rs.getLong(3)), SURVEY_CODE);
  }

  private record OpenSurvey(long id, String name, long versionId) {}

  public TraineeSurvey.Questionnaire getQuestionnaire(long taskId) {
    long traineeId = currentTraineeId();
    Assignment assignment = getOpenAssignment(traineeId, taskId);
    consent.validateConsent(traineeId, CONSENT_CODE);
    return new TraineeSurvey.Questionnaire(assignment.summary(), assignment.versionId(),
        questions(assignment.versionId()));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TraineeSurvey.Submission submit(long taskId, TraineeSurvey.SubmitRequest request) {
    long traineeId = currentTraineeId();
    // Serialize self-service submissions for the same trainee and protect the assigned task transition.
    lockTrainee(traineeId);
    boolean ownsTask = !jdbc.queryForList("""
        SELECT followup_task_id FROM followup_tasks
        WHERE followup_task_id = ? AND trainee_id = ? FOR UPDATE
        """, Long.class, taskId, traineeId).isEmpty();
    if (!ownsTask) {
      throw new ResourceNotFoundException("Follow-up survey assignment not found");
    }
    Assignment assignment = jdbc.query(OPEN_ASSIGNMENTS + " AND t.followup_task_id = ?",
        (rs, row) -> assignment(rs), traineeId, taskId).stream().findFirst()
        .orElseThrow(() -> new ConflictException("This survey assignment is no longer open.",
            "SURVEY_ASSIGNMENT_CLOSED"));
    consent.validateConsent(traineeId, CONSENT_CODE);
    List<TraineeSurvey.Question> questions = validateSubmission(assignment.versionId(), request);
    Long completed = jdbc.queryForObject(
        "SELECT followup_status_id FROM ref_followup_status WHERE status_code = 'COMPLETED'", Long.class);
    Long completedOutcome = jdbc.queryForObject(
        "SELECT followup_outcome_id FROM ref_followup_outcome WHERE outcome_code = 'COMPLETED'", Long.class);

    var draft = draft(assignment.surveyId(), assignment.versionId(), traineeId);
    var recorded = followups.recordFollowupSurveyResponse(
        new RecordFollowupSurveyResponseRequest(taskId, draft, completed, completedOutcome));
    return finish(recorded.surveyResponse().getId(), assignment.summary().name(), request, questions);
  }

  private void lockTrainee(long traineeId) {
    if (jdbc.queryForList("SELECT trainee_id FROM trainees WHERE trainee_id=? AND deleted_at IS NULL FOR UPDATE",
        Long.class, traineeId).isEmpty()) throw new ResourceNotFoundException("Trainee", "id");
  }

  private List<TraineeSurvey.Question> validateSubmission(long versionId, TraineeSurvey.SubmitRequest request) {
    if (request.versionId() != versionId) {
      throw new ConflictException("The questionnaire changed. Reopen it before submitting.",
          "SURVEY_VERSION_CHANGED");
    }
    List<TraineeSurvey.Question> questions = questions(versionId);
    if (questions.isEmpty()) {
      throw new ConflictException("This questionnaire is not ready for submissions.", "SURVEY_NO_QUESTIONS");
    }
    validateAnswers(questions, request.answers());
    return questions;
  }

  private CreateSurveyResponseRequest draft(long surveyId, long versionId, long traineeId) {
    int attempt = jdbc.queryForObject("""
        SELECT COALESCE(MAX(attempt_number), 0) + 1 FROM survey_responses WHERE survey_id = ? AND trainee_id = ?
        """, Integer.class, surveyId, traineeId);
    Long started = jdbc.queryForObject(
        "SELECT survey_response_status_id FROM ref_survey_response_status WHERE status_code = 'STARTED'", Long.class);
    return new CreateSurveyResponseRequest(surveyId, versionId, traineeId, attempt, started, LocalDateTime.now());
  }

  private TraineeSurvey.Submission finish(long responseId, String name, TraineeSurvey.SubmitRequest request,
      List<TraineeSurvey.Question> questions) {
    Set<Long> multiChoiceQuestions = questions.stream().filter(q -> q.type().equals("MULTI_CHOICE"))
        .map(TraineeSurvey.Question::id).collect(Collectors.toSet());
    request.answers().forEach(answer ->
        saveAnswer(responseId, answer, multiChoiceQuestions.contains(answer.questionId())));
    var finish = new UpdateSurveyResponseRequest();
    finish.setSurveyResponseStatusId(jdbc.queryForObject(
        "SELECT survey_response_status_id FROM ref_survey_response_status WHERE status_code = 'SUBMITTED'", Long.class));
    var result = responses.updateSurveyResponse(responseId, finish);
    return new TraineeSurvey.Submission(responseId, name, result.getSubmittedAt());
  }

  private long currentTraineeId() {
    return scope.resolveAuthenticatedTrainee().filter(trainee -> trainee.getDeletedAt() == null).map(Trainee::getId)
        .orElseThrow(() -> new ResourceNotFoundException("No trainee profile is linked to this account"));
  }

  private Assignment getOpenAssignment(long traineeId, long taskId) {
    return jdbc.query(OPEN_ASSIGNMENTS + " AND t.followup_task_id = ?",
        (rs, row) -> assignment(rs), traineeId, taskId).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Open follow-up survey assignment not found"));
  }

  private static Assignment assignment(ResultSet rs) throws SQLException {
    return new Assignment(rs.getLong("survey_id"), rs.getLong("survey_template_version_id"),
        new TraineeSurvey.Assignment(rs.getLong("followup_task_id"),
            rs.getString("survey_name"), rs.getObject("closes_on", LocalDate.class)));
  }

  private record Assignment(long surveyId, long versionId, TraineeSurvey.Assignment summary) {}

  private List<TraineeSurvey.Question> questions(long versionId) {
    Map<Long, List<TraineeSurvey.Option>> options = new HashMap<>();
    jdbc.query("""
        SELECT q.survey_question_id, o.survey_question_option_id, o.option_label
        FROM survey_questions q JOIN survey_question_options o ON q.survey_question_id = o.survey_question_id
        WHERE q.survey_template_version_id = ? ORDER BY o.display_order
        """, rs -> {
          options.computeIfAbsent(rs.getLong(1), id -> new ArrayList<>())
              .add(new TraineeSurvey.Option(rs.getLong(2), rs.getString(3)));
        }, versionId);
    return jdbc.query("""
        SELECT q.survey_question_id, q.question_text, t.type_code, q.is_required
        FROM survey_questions q JOIN ref_question_type t ON q.question_type_id = t.question_type_id
        WHERE q.survey_template_version_id = ? ORDER BY q.display_order
        """, (rs, row) -> new TraineeSurvey.Question(rs.getLong(1), rs.getString(2), rs.getString(3),
            rs.getBoolean(4), options.getOrDefault(rs.getLong(1), List.of())), versionId);
  }

  private static void validateAnswers(List<TraineeSurvey.Question> questions, List<TraineeSurvey.Answer> answers) {
    Map<Long, TraineeSurvey.Answer> byQuestion = new HashMap<>();
    for (var answer : answers) {
      if (byQuestion.putIfAbsent(answer.questionId(), answer) != null) {
        throw new BadRequestException("Each question may be answered once.", "SURVEY_DUPLICATE_ANSWER");
      }
    }
    for (var question : questions) {
      var answer = byQuestion.remove(question.id());
      if (answer == null && !question.required()) {
        continue;
      }
      if (answer == null || !validAnswer(question, answer)) {
        throw new BadRequestException("A question is missing a required answer or has an invalid value.",
            "SURVEY_INVALID_ANSWER");
      }
    }
    if (!byQuestion.isEmpty()) {
      throw new BadRequestException("An answer does not belong to this questionnaire.", "SURVEY_UNKNOWN_QUESTION");
    }
  }

  private static boolean validAnswer(TraineeSurvey.Question question, TraineeSurvey.Answer answer) {
    var selections = answer.optionIds() == null ? List.<Long>of() : answer.optionIds();
    long providedValues = java.util.stream.Stream.of(answer.text(), answer.number(), answer.yes(), answer.date())
        .filter(java.util.Objects::nonNull).count() + (selections.isEmpty() ? 0 : 1);
    if (providedValues != 1) {
      return false;
    }
    return switch (question.type()) {
      case "TEXT" -> answer.text() != null && !answer.text().isBlank();
      case "BOOLEAN" -> answer.yes() != null;
      case "DATE" -> answer.date() != null;
      case "NUMBER", "SCALE" -> answer.number() != null && answer.number().stripTrailingZeros().scale() <= 0;
      case "DECIMAL" -> answer.number() != null;
      case "SINGLE_CHOICE", "MULTI_CHOICE" -> !selections.isEmpty()
          && (question.type().equals("MULTI_CHOICE") || selections.size() == 1)
          && new HashSet<>(selections).size() == selections.size()
          && selections.stream().allMatch(id -> question.options().stream().anyMatch(o -> id == o.id()));
      default -> false;
    };
  }

  private void saveAnswer(long responseId, TraineeSurvey.Answer answer, boolean multiChoice) {
    var selections = answer.optionIds();
    if (multiChoice) {
      jdbc.update("INSERT INTO survey_response_answers (survey_response_id, survey_question_id) VALUES (?, ?)",
          responseId, answer.questionId());
      jdbc.batchUpdate("""
          INSERT INTO survey_response_answer_options (survey_response_answer_id, survey_question_option_id)
          SELECT survey_response_answer_id, ? FROM survey_response_answers
          WHERE survey_response_id = ? AND survey_question_id = ?
          """, selections.stream().map(id -> new Object[]{id, responseId, answer.questionId()}).toList());
      return;
    }
    var payload = new CreateSurveyResponseAnswerRequest(responseId, answer.questionId());
    if (selections != null && !selections.isEmpty()) {
      payload.setSelectedOptionId(selections.get(0));
    }
    payload.setAnswerText(answer.text());
    payload.setNumericValue(answer.number());
    payload.setBooleanValue(answer.yes());
    payload.setDateValue(answer.date());
    answers.createSurveyResponseAnswer(payload);
  }
}
