package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.trainee.TraineeSurvey;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
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
  private static final String FOLLOWUP_CONSENT = "EMPLOYMENT_FOLLOW_UP";
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

  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final TraineeConsentService consent;
  private final FollowupEngagementWorkflowService followups;
  private final SurveyResponseService responses;
  private final SurveyResponseAnswerService answers;

  public TraineeSurveyService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope,
      TraineeConsentService consent, FollowupEngagementWorkflowService followups,
      SurveyResponseService responses, SurveyResponseAnswerService answers) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.consent = consent;
    this.followups = followups;
    this.responses = responses;
    this.answers = answers;
  }

  public TraineeSurvey.Overview getOverview() {
    long traineeId = currentTraineeId();
    boolean hasConsent = consent.hasActiveConsent(traineeId, FOLLOWUP_CONSENT);
    var assignments = hasConsent
        ? jdbc.query(OPEN_ASSIGNMENTS + " ORDER BY t.scheduled_date, t.followup_task_id",
            (rs, row) -> assignment(rs).summary(), traineeId)
        : List.<TraineeSurvey.Assignment>of();
    var history = jdbc.query("""
        SELECT r.survey_response_id, s.survey_name, r.submitted_at
        FROM survey_responses r
        JOIN surveys s ON s.survey_id = r.survey_id
        JOIN ref_survey_response_status rs ON rs.survey_response_status_id = r.survey_response_status_id
        WHERE r.trainee_id = ? AND rs.is_submitted_flag = 1
        ORDER BY r.submitted_at DESC, r.survey_response_id DESC LIMIT 20
        """, (rs, row) -> new TraineeSurvey.Submission(rs.getLong(1), rs.getString(2),
            rs.getObject(3, LocalDateTime.class)), traineeId);
    return new TraineeSurvey.Overview(hasConsent, assignments, history);
  }

  public TraineeSurvey.Questionnaire getQuestionnaire(long taskId) {
    long traineeId = currentTraineeId();
    Assignment assignment = getOpenAssignment(traineeId, taskId);
    consent.validateConsent(traineeId, FOLLOWUP_CONSENT);
    return new TraineeSurvey.Questionnaire(assignment.summary(), assignment.versionId(),
        questions(assignment.versionId()));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TraineeSurvey.Submission submit(long taskId, TraineeSurvey.SubmitRequest request) {
    long traineeId = currentTraineeId();
    // Serialize self-service submissions for the same trainee and protect the assigned task transition.
    jdbc.queryForObject("SELECT trainee_id FROM trainees WHERE trainee_id = ? FOR UPDATE", Long.class, traineeId);
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
    consent.validateConsent(traineeId, FOLLOWUP_CONSENT);
    if (request.versionId() != assignment.versionId()) {
      throw new ConflictException("The questionnaire changed. Reopen it before submitting.",
          "SURVEY_VERSION_CHANGED");
    }
    List<TraineeSurvey.Question> questions = questions(assignment.versionId());
    if (questions.isEmpty()) {
      throw new ConflictException("This questionnaire is not ready for submissions.", "SURVEY_NO_QUESTIONS");
    }
    validateAnswers(questions, request.answers());

    int attempt = jdbc.queryForObject("""
        SELECT COALESCE(MAX(attempt_number), 0) + 1 FROM survey_responses WHERE survey_id = ? AND trainee_id = ?
        """, Integer.class, assignment.surveyId(), traineeId);
    Long started = jdbc.queryForObject(
        "SELECT survey_response_status_id FROM ref_survey_response_status WHERE status_code = 'STARTED'", Long.class);
    Long submitted = jdbc.queryForObject(
        "SELECT survey_response_status_id FROM ref_survey_response_status WHERE status_code = 'SUBMITTED'", Long.class);
    Long completed = jdbc.queryForObject(
        "SELECT followup_status_id FROM ref_followup_status WHERE status_code = 'COMPLETED'", Long.class);
    Long completedOutcome = jdbc.queryForObject(
        "SELECT followup_outcome_id FROM ref_followup_outcome WHERE outcome_code = 'COMPLETED'", Long.class);

    var draft = new CreateSurveyResponseRequest(assignment.surveyId(), assignment.versionId(),
        traineeId, attempt, started, LocalDateTime.now());
    var recorded = followups.recordFollowupSurveyResponse(
        new RecordFollowupSurveyResponseRequest(taskId, draft, completed, completedOutcome));
    long responseId = recorded.surveyResponse().getId();
    Set<Long> multiChoiceQuestions = questions.stream().filter(q -> q.type().equals("MULTI_CHOICE"))
        .map(TraineeSurvey.Question::id).collect(Collectors.toSet());
    request.answers().forEach(answer ->
        saveAnswer(responseId, answer, multiChoiceQuestions.contains(answer.questionId())));
    var finish = new UpdateSurveyResponseRequest();
    finish.setSurveyResponseStatusId(submitted);
    var result = responses.updateSurveyResponse(responseId, finish);
    return new TraineeSurvey.Submission(responseId, assignment.summary().name(), result.getSubmittedAt());
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
