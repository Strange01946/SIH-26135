package in.gov.sih.sih26135.integration;

import com.fasterxml.jackson.databind.JsonNode;
import in.gov.sih.sih26135.service.CareerFollowupService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TraineeSurveyIntegrationTest extends BaseJourneyIntegrationTest {
  private static final String ROOT = "/api/v1/trainees/me/followup-surveys";
  private Long campaignId;
  private Long taskId;
  private Long versionId;
  private final Set<String> newAutomaticCampaigns = new HashSet<>();

  @Autowired
  private CareerFollowupService careerFollowups;

  @Test
  void assignedTraineeSubmitsValidatedAnswersOnceAndOtherTraineesReceiveNoAssignment() {
    String suffix = UUID.randomUUID().toString().substring(0, 10);
    Candidate owner = candidate("survey-owner-" + suffix);
    Candidate other = candidate("survey-other-" + suffix);
    Long templateId = insertAndGetId("""
        INSERT INTO survey_templates (template_code, template_name, survey_purpose_id, lifecycle_status_id)
        VALUES (?, 'Employment follow-up', 1, 1)
        """, "TEST-TPL-" + suffix);
    trackedSurveyTemplateIds.add(templateId);
    versionId = insertAndGetId("""
        INSERT INTO survey_template_versions
          (survey_template_id, version_number, version_label, effective_from, lifecycle_status_id)
        VALUES (?, 1, 'v1', CURRENT_DATE, 1)
        """, templateId);
    trackedSurveyTemplateVersionIds.add(versionId);
    Long surveyId = insertAndGetId("""
        INSERT INTO surveys (survey_code, survey_name, survey_template_version_id, survey_purpose_id,
          scheduled_start_date, survey_instance_status_id)
        VALUES (?, 'Six-month follow-up', ?, 3, CURRENT_DATE, 3)
        """, "TEST-SRV-" + suffix, versionId);
    trackedSurveyIds.add(surveyId);
    Long booleanQuestion = insertAndGetId("""
        INSERT INTO survey_questions
          (survey_template_version_id, question_code, question_text, question_type_id, display_order, is_required)
        VALUES (?, 'EMPLOYED', 'Are you employed?', 1, 1, 1)
        """, versionId);
    Long multiQuestion = insertAndGetId("""
        INSERT INTO survey_questions
          (survey_template_version_id, question_code, question_text, question_type_id, display_order, is_required)
        VALUES (?, 'BARRIERS', 'What barriers did you face?', 3, 2, 0)
        """, versionId);
    Long option = insertAndGetId("""
        INSERT INTO survey_question_options (survey_question_id, option_code, option_label, display_order)
        VALUES (?, 'LOCATION', 'Work location', 1)
        """, multiQuestion);
    campaignId = insertAndGetId("""
        INSERT INTO followup_campaigns
          (campaign_code, campaign_name, followup_type_id, survey_id, scheduled_start_date, lifecycle_status_id)
        VALUES (?, 'Six-month campaign', 2, ?, CURRENT_DATE, 1)
        """, "TEST-CAM-" + suffix, surveyId);
    taskId = insertAndGetId("""
        INSERT INTO followup_tasks (followup_campaign_id, trainee_id, scheduled_date, followup_status_id)
        VALUES (?, ?, CURRENT_DATE, 1)
        """, campaignId, owner.traineeId());

    assertThat(request(HttpMethod.GET, ROOT + "/" + taskId, other.headers(), null).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(request(HttpMethod.GET, ROOT, owner.headers(), null).getBody().at("/data/followupConsentGranted").asBoolean())
        .isFalse();
    var body = Map.of("versionId", versionId, "answers", List.of(
        Map.of("questionId", booleanQuestion, "yes", false),
        Map.of("questionId", multiQuestion, "optionIds", List.of(option))));
    String responsePath = ROOT + "/" + taskId + "/responses";
    assertThat(request(HttpMethod.POST, responsePath, owner.headers(), body).getStatusCode().is4xxClientError())
        .isTrue();

    Long consentId = insertAndGetId("""
        INSERT INTO trainee_consents (trainee_id, consent_type_id, consent_status_id, purpose, policy_version, granted_at)
        VALUES (?, 2, 1, 'Employment follow-up', '1.0', CURRENT_TIMESTAMP)
        """, owner.traineeId());
    trackedConsentIds.add(consentId);
    var overview = request(HttpMethod.GET, ROOT, owner.headers(), null);
    assertThat(overview.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(overview.getBody().at("/data/assignments/0/taskId").asLong()).isEqualTo(taskId);
    var questionnaire = request(HttpMethod.GET, ROOT + "/" + taskId, owner.headers(), null);
    assertThat(questionnaire.getBody().at("/data/questions/1/options/0/id").asLong()).isEqualTo(option);
    assertThat(request(HttpMethod.POST, responsePath, owner.headers(), Map.of("versionId", versionId, "answers", List.of()))
        .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var response = request(HttpMethod.POST, responsePath, owner.headers(), body);
    assertThat(response.getStatusCode()).withFailMessage("Survey submission returned %s", response.getBody())
        .isEqualTo(HttpStatus.CREATED);
    Long responseId = response.getBody().at("/data/id").asLong();
    assertThat(request(HttpMethod.POST, responsePath, owner.headers(), body).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(request(HttpMethod.GET, ROOT, owner.headers(), null).getBody().at("/data/recentSubmissions/0/id").asLong())
        .isEqualTo(responseId);
    assertThat(jdbcTemplate.queryForObject("""
        SELECT COUNT(*) FROM survey_response_answers a JOIN survey_response_answer_options o
          ON a.survey_response_answer_id = o.survey_response_answer_id
        WHERE a.survey_response_id = ? AND o.survey_question_option_id = ?
        """, Integer.class, responseId, option)).isEqualTo(1);
    assertThat(request(HttpMethod.GET, "/api/v1/trainees/me/employment-history", owner.headers(), null).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void automaticFollowupsUseEarlierEligibleMilestoneAndRecurAfterSubmission() {
    String suffix = UUID.randomUUID().toString().substring(0, 10);
    var today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    var trainingFirst = candidate("training-first-" + suffix);
    var followupFirst = candidate("followup-first-" + suffix);
    var trainingDay = today.minusMonths(7);
    var trainingDue = trainingDay.plusMonths(6);
    Long batch = insertAndGetId("""
        INSERT INTO training_batches (batch_code,course_id,provider_id,center_id,program_id,start_date,end_date,capacity,batch_status_id)
        SELECT ?,course_id,provider_id,center_id,program_id,?,?,5,3
        FROM training_batches WHERE deleted_at IS NULL AND center_id IS NOT NULL ORDER BY batch_id DESC LIMIT 1
        """, "FUP-BAT-" + suffix, trainingDay, today);
    trackedBatchIds.add(batch);
    Long firstEnrollment = enrollInBatch(trainingFirst.traineeId(), batch, "FUP-A-" + suffix, trainingDay);
    Long secondEnrollment = enrollInBatch(followupFirst.traineeId(), batch, "FUP-B-" + suffix, trainingDay);
    recordAttendance(trainingFirst.traineeId(), batch, firstEnrollment, trainingDay, 1);
    recordAttendance(trainingFirst.traineeId(), batch, firstEnrollment, today, 2);
    recordAttendance(followupFirst.traineeId(), batch, secondEnrollment, today.minusMonths(1), 1);

    assertThat(careerFollowups.assignIfDue(trainingFirst.traineeId(), today)).isFalse();
    grantFollowupConsent(trainingFirst.traineeId());
    grantFollowupConsent(followupFirst.traineeId());

    campaignId = insertAndGetId("""
        INSERT INTO followup_campaigns (campaign_code,campaign_name,followup_type_id,survey_id,scheduled_start_date,lifecycle_status_id)
        SELECT ?, 'Manual test wave', 7,survey_id,?,1 FROM surveys WHERE survey_code='SIH-CAREER-UPDATE'
        """, "FUP-MANUAL-" + suffix, today.minusMonths(8));
    var earlyFollowup = today.minusMonths(6).minusDays(1);
    jdbcTemplate.update("""
        INSERT INTO followup_tasks (followup_campaign_id,trainee_id,scheduled_date,completed_at,followup_status_id)
        VALUES (?,?,?,?,3),(?,?,?,?,3)
        """, campaignId, trainingFirst.traineeId(), today.minusMonths(3), today.minusMonths(3).atStartOfDay(),
        campaignId, followupFirst.traineeId(), earlyFollowup, earlyFollowup.atStartOfDay());
    taskId = insertAndGetId("""
        INSERT INTO followup_tasks (followup_campaign_id,trainee_id,scheduled_date,followup_status_id)
        SELECT followup_campaign_id,?,?,1 FROM followup_campaigns WHERE campaign_code='SIH-CAREER-FOLLOWUP'
        """, trainingFirst.traineeId(), today);
    assertThat(careerFollowups.assignIfDue(trainingFirst.traineeId(), today)).isFalse();
    jdbcTemplate.update("UPDATE followup_tasks SET followup_status_id=5 WHERE followup_task_id=?", taskId);

    var followupDue = earlyFollowup.plusMonths(6);
    rememberAutomaticCampaign(trainingDue);
    rememberAutomaticCampaign(followupDue);
    assertThat(careerFollowups.assignIfDue(trainingFirst.traineeId(), today)).isTrue();
    assertThat(careerFollowups.assignIfDue(followupFirst.traineeId(), today)).isTrue();
    assertThat(automaticDates(trainingFirst.traineeId())).containsExactly(trainingDue);
    assertThat(automaticDates(followupFirst.traineeId())).containsExactly(followupDue);
    assertThat(careerFollowups.assignIfDue(trainingFirst.traineeId(), today)).isFalse();

    var overview = request(HttpMethod.GET, ROOT, trainingFirst.headers(), null);
    assertThat(overview.getBody().at("/data/assignments/0/name").asText()).contains("Automatic six-month");
    long assignmentId = overview.getBody().at("/data/assignments/0/taskId").asLong();
    var questionnaire = request(HttpMethod.GET, ROOT + "/" + assignmentId, trainingFirst.headers(), null);
    var form = questionnaire.getBody().path("data");
    var question = form.path("questions").get(0);
    var response = request(HttpMethod.POST, ROOT + "/" + assignmentId + "/responses", trainingFirst.headers(),
        Map.of("versionId", form.path("versionId").asLong(), "answers", List.of(
            Map.of("questionId", question.path("id").asLong(), "optionIds", List.of(question.path("options").get(0).path("id").asLong())))));
    assertThat(response.getStatusCode()).withFailMessage("Automatic survey returned %s", response.getBody())
        .isEqualTo(HttpStatus.CREATED);
    assertThat(automaticDates(trainingFirst.traineeId())).containsExactly(trainingDue);
    assertThat(careerFollowups.assignIfDue(trainingFirst.traineeId(), today)).isFalse();
    var nextDue = today.plusMonths(6);
    rememberAutomaticCampaign(nextDue);
    assertThat(careerFollowups.assignIfDue(trainingFirst.traineeId(), nextDue.minusDays(1))).isFalse();
    assertThat(careerFollowups.assignIfDue(trainingFirst.traineeId(), nextDue)).isTrue();
    assertThat(automaticDates(trainingFirst.traineeId())).containsExactly(trainingDue, nextDue);
  }

  private Long enrollInBatch(long traineeId, long batch, String number, LocalDate date) {
    Long id = insertAndGetId("""
        INSERT INTO training_enrollments (enrollment_number,trainee_id,course_id,provider_id,center_id,batch_id,program_id,
          enrollment_status_id,enrollment_date,start_date)
        SELECT ?,?,course_id,provider_id,center_id,batch_id,program_id,1,?,?
        FROM training_batches WHERE batch_id=?
        """, number, traineeId, date, date, batch);
    trackedEnrollmentIds.add(id);
    return id;
  }

  private void recordAttendance(long traineeId, long batch, long enrollment, LocalDate day, int status) {
    trackedAttendanceIds.add(insertAndGetId("""
        INSERT INTO attendance_records (enrollment_id,trainee_id,batch_id,session_date,attendance_status_id)
        VALUES (?,?,?,?,?)
        """, enrollment, traineeId, batch, day, status));
  }

  private void grantFollowupConsent(long traineeId) {
    trackedConsentIds.add(insertAndGetId("""
        INSERT INTO trainee_consents (trainee_id,consent_type_id,consent_status_id,purpose,policy_version,granted_at)
        VALUES (?,2,1,'Employment follow-up','1.0',CURRENT_TIMESTAMP)
        """, traineeId));
  }

  private void rememberAutomaticCampaign(LocalDate date) {
    String code = "SIH-CAREER-AUTO-" + date;
    if (jdbcTemplate.queryForList("SELECT followup_campaign_id FROM followup_campaigns WHERE campaign_code=?", Long.class, code).isEmpty()) {
      newAutomaticCampaigns.add(code);
    }
  }

  private List<LocalDate> automaticDates(long traineeId) {
    return jdbcTemplate.query("""
        SELECT t.scheduled_date FROM followup_tasks t JOIN followup_campaigns c USING (followup_campaign_id)
        WHERE t.trainee_id=? AND c.campaign_code LIKE 'SIH-CAREER-AUTO-%' ORDER BY t.scheduled_date
        """, (rs, row) -> rs.getObject(1, LocalDate.class), traineeId);
  }

  @AfterEach
  void removeSurveyAssignmentsAndQuestions() {
    for (long traineeId : trackedTraineeIds) {
      jdbcTemplate.update("""
          DELETE a FROM survey_response_answers a JOIN survey_responses r USING (survey_response_id)
          WHERE r.trainee_id=? AND r.followup_task_id IS NOT NULL
          """, traineeId);
      jdbcTemplate.update("DELETE FROM survey_responses WHERE trainee_id=? AND followup_task_id IS NOT NULL", traineeId);
      jdbcTemplate.update("DELETE FROM followup_tasks WHERE trainee_id=?", traineeId);
    }
    for (String code : newAutomaticCampaigns) {
      jdbcTemplate.update("""
          DELETE c FROM followup_campaigns c LEFT JOIN followup_tasks t USING (followup_campaign_id)
          WHERE c.campaign_code=? AND t.followup_task_id IS NULL
          """, code);
    }
    if (campaignId != null) {
      jdbcTemplate.update("DELETE FROM followup_campaigns WHERE followup_campaign_id = ?", campaignId);
    }
    if (versionId != null) {
      jdbcTemplate.update("DELETE FROM survey_questions WHERE survey_template_version_id = ?", versionId);
    }
  }

  private Candidate candidate(String username) {
    Long userId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id)
        VALUES (?, ?, 'fixture-only-unused-hash', 'argon2id', 2)
        """, username, username + "@example.com");
    trackedUserIds.add(userId);
    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at) SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'trainee'
        """, userId);
    Long traineeId = insertAndGetId("""
        INSERT INTO trainees (user_id, registration_number, first_name, gender_id, state_id, district_id,
          current_employment_status_id, profile_status_id)
        VALUES (?, ?, 'Test trainee', 1, 101, 1001, 1, 1)
        """, userId, username);
    trackedTraineeIds.add(traineeId);
    return new Candidate(traineeId, createAuthHeaders(generateBearerToken(userId, username, List.of("ROLE_trainee"))));
  }

  private ResponseEntity<JsonNode> request(HttpMethod method, String path, HttpHeaders headers, Object body) {
    return restTemplate.exchange(getBaseUrl(path), method, new HttpEntity<>(body, headers), JsonNode.class);
  }

  private record Candidate(Long traineeId, HttpHeaders headers) {}
}
