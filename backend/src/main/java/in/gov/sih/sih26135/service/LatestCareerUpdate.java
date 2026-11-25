package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.trainee.TraineeSurvey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;

public final class LatestCareerUpdate {
  static final String SURVEY_CODE = "SIH-CAREER-UPDATE";
  static final String CONSENT_CODE = "EMPLOYMENT_FOLLOW_UP";

  private LatestCareerUpdate() {}

  public static TraineeSurvey.CareerUpdate find(JdbcTemplate jdbc, long traineeId) {
    return jdbc.query("""
        WITH latest AS (
          SELECT r.survey_response_id, r.submitted_at
          FROM survey_responses r
          JOIN surveys s USING (survey_id)
          JOIN ref_survey_response_status rs USING (survey_response_status_id)
          WHERE r.trainee_id=? AND s.survey_code=? AND s.deleted_at IS NULL
            AND rs.is_submitted_flag=1 AND r.submitted_at IS NOT NULL
          ORDER BY r.submitted_at DESC, r.survey_response_id DESC LIMIT 1
        )
        SELECT l.survey_response_id, l.submitted_at, es.employment_status_id, es.status_code, es.status_name,
          MAX(CASE WHEN q.question_code='ORGANIZATION' THEN a.answer_text END) AS organization,
          MAX(CASE WHEN q.question_code='WORK_ROLE' THEN a.answer_text END) AS work_role,
          MAX(CASE WHEN q.question_code='CHANGE_DATE' THEN a.date_value END) AS since_date,
          MAX(CASE WHEN q.question_code='MONTHLY_INCOME' THEN a.numeric_value END) AS monthly_income,
          MAX(CASE WHEN q.question_code='WORK_LOCATION' THEN a.answer_text END) AS work_location,
          MAX(CASE WHEN q.question_code='SKILL_USE' THEN a.answer_text END) AS skills,
          MAX(CASE WHEN q.question_code='BARRIERS' THEN a.answer_text END) AS barriers
        FROM latest l
        JOIN survey_response_answers a USING (survey_response_id)
        JOIN survey_questions q USING (survey_question_id)
        JOIN survey_response_answers work ON work.survey_response_id=l.survey_response_id
        JOIN survey_questions work_question ON work_question.survey_question_id=work.survey_question_id
          AND work_question.question_code='WORK_STATUS'
        JOIN survey_question_options status_option ON status_option.survey_question_option_id=work.selected_option_id
        JOIN ref_employment_status es ON es.status_code=status_option.option_code
        GROUP BY l.survey_response_id, l.submitted_at, es.employment_status_id, es.status_code, es.status_name
        """, (rs, row) -> new TraineeSurvey.CareerUpdate(rs.getLong("survey_response_id"),
            rs.getObject("submitted_at", LocalDateTime.class), rs.getLong("employment_status_id"),
            rs.getString("status_code"), rs.getString("status_name"), rs.getString("organization"),
            rs.getString("work_role"), rs.getObject("since_date", LocalDate.class), rs.getBigDecimal("monthly_income"),
            rs.getString("work_location"), rs.getString("skills"), rs.getString("barriers")), traineeId, SURVEY_CODE)
        .stream().findFirst().orElse(null);
  }
}
