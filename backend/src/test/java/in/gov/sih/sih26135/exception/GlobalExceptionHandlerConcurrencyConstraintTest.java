package in.gov.sih.sih26135.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerConcurrencyConstraintTest {

  private GlobalExceptionHandler exceptionHandler;

  @Mock private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  @Test
  @DisplayName("Translates uk_employment_records_current_per_type constraint violation into 409 ACTIVE_SPELL_ALREADY_EXISTS")
  void testCurrentEmploymentSpellConstraintViolation_translatesTo409ActiveSpell() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '10-1-1' for key 'uk_employment_records_current_per_type'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("ACTIVE_SPELL_ALREADY_EXISTS");
    assertThat(response.getBody().getMessage()).contains("active current spell");
  }

  @Test
  @DisplayName("Translates uk_emp_verif_requests_open constraint violation into 409 OPEN_REQUEST_EXISTS")
  void testOpenVerificationRequestConstraintViolation_translatesTo409OpenRequest() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '1-1' for key 'uk_emp_verif_requests_open'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("OPEN_REQUEST_EXISTS");
    assertThat(response.getBody().getMessage()).contains("open verification request already exists");
  }

  @Test
  @DisplayName("Translates uk_emp_verif_requests_cycle constraint violation into 409 DUPLICATE_CYCLE_NUMBER")
  void testVerificationRequestCycleConstraintViolation_translatesTo409DuplicateCycle() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '1-2' for key 'uk_emp_verif_requests_cycle'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_CYCLE_NUMBER");
    assertThat(response.getBody().getMessage()).contains("cycle number already exists");
  }

  @Test
  @DisplayName("Translates uk_employment_records_placement constraint violation into 409 DUPLICATE_PLACEMENT_EMPLOYMENT")
  void testPlacementEmploymentConstraintViolation_translatesTo409DuplicatePlacement() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '10' for key 'uk_employment_records_placement'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_PLACEMENT_EMPLOYMENT");
  }

  @Test
  @DisplayName("Translates uk_unemployment_events_current constraint violation into 409 ACTIVE_PERIOD_ALREADY_EXISTS")
  void testUnemploymentEventCurrentConstraintViolation_translatesTo409ActivePeriod() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '100-1' for key 'uk_unemployment_events_current'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("ACTIVE_PERIOD_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Translates uk_placement_records_number constraint violation into 409 PLACEMENT_NUMBER_ALREADY_EXISTS")
  void testPlacementNumberConstraintViolation_translatesTo409PlacementNumberExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry 'PLC-2026-001' for key 'uk_placement_records_number'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("PLACEMENT_NUMBER_ALREADY_EXISTS");
    assertThat(response.getBody().getMessage()).contains("Placement number already exists");
  }

  @Test
  @DisplayName("Translates uk_placement_records_application constraint violation into 409 DUPLICATE_PLACEMENT_APPLICATION")
  void testPlacementApplicationConstraintViolation_translatesTo409DuplicatePlacementApplication() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '50' for key 'uk_placement_records_application'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_PLACEMENT_APPLICATION");
  }

  @Test
  @DisplayName("Translates uk_placement_records_enrollment_posting constraint violation into 409 DUPLICATE_PLACEMENT_ENROLLMENT_POSTING")
  void testPlacementEnrollmentPostingConstraintViolation_translatesTo409DuplicatePlacementEnrollmentPosting() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '10-20' for key 'uk_placement_records_enrollment_posting'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_PLACEMENT_ENROLLMENT_POSTING");
  }

  @Test
  @DisplayName("Translates uk_certifications_certificate_number constraint violation into 409 CERTIFICATE_NUMBER_ALREADY_EXISTS")
  void testCertificateNumberConstraintViolation_translatesTo409CertificateNumberExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry 'CERT-2026-999' for key 'uk_certifications_certificate_number'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("CERTIFICATE_NUMBER_ALREADY_EXISTS");
    assertThat(response.getBody().getMessage()).contains("Certificate number already exists");
  }

  @Test
  @DisplayName("Translates uk_job_applications_trainee_posting constraint violation into 409 DUPLICATE_JOB_APPLICATION")
  void testJobApplicationTraineePostingConstraintViolation_translatesTo409DuplicateJobApplication() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '100-200' for key 'uk_job_applications_trainee_posting'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_JOB_APPLICATION");
    assertThat(response.getBody().getMessage()).contains("Trainee has already applied for this job posting");
  }

  @Test
  @DisplayName("Translates uk_assessment_results_attempt constraint violation into 409 ASSESSMENT_RESULT_ALREADY_EXISTS")
  void testAssessmentResultAttemptConstraintViolation_translatesTo409AssessmentResultAlreadyExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '15' for key 'uk_assessment_results_attempt'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("ASSESSMENT_RESULT_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Translates uk_assessments_code constraint violation into 409 ASSESSMENT_CODE_ALREADY_EXISTS")
  void testAssessmentCodeConstraintViolation_translatesTo409AssessmentCodeExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry 'ASM-001' for key 'uk_assessments_code'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("ASSESSMENT_CODE_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Translates uk_training_batches_code constraint violation into 409 BATCH_CODE_ALREADY_EXISTS")
  void testTrainingBatchCodeConstraintViolation_translatesTo409BatchCodeExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry 'BAT-001' for key 'uk_training_batches_code'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("BATCH_CODE_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Translates uk_trainee_assessments_attempt constraint violation into 409 TRAINEE_ASSESSMENT_ATTEMPT_ALREADY_EXISTS")
  void testTraineeAssessmentAttemptConstraintViolation_translatesTo409TraineeAssessmentAttemptExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '1-100-1' for key 'uk_trainee_assessments_attempt'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("TRAINEE_ASSESSMENT_ATTEMPT_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Translates uk_attendance_enrollment_session constraint violation into 409 ATTENDANCE_ALREADY_EXISTS")
  void testAttendanceSessionConstraintViolation_translatesTo409AttendanceAlreadyExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '10-2026-03-01-1' for key 'uk_attendance_enrollment_session'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("ATTENDANCE_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Translates uk_employment_exit_events_employment constraint violation into 409 EXIT_EVENT_ALREADY_EXISTS")
  void testEmploymentExitEventConstraintViolation_translatesTo409ExitEventExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '50' for key 'uk_employment_exit_events_employment'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("EXIT_EVENT_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Translates uk_unemployment_events_trainee_start constraint violation into 409 UNEMPLOYMENT_START_DATE_EXISTS")
  void testUnemploymentStartDateConstraintViolation_translatesTo409UnemploymentStartDateExists() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '100-2026-02-01' for key 'uk_unemployment_events_trainee_start'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UNEMPLOYMENT_START_DATE_EXISTS");
  }

  @Test
  @DisplayName("Translates uk_salary_history_employment_from constraint violation into 409 DUPLICATE_EMPLOYMENT_EFFECTIVE_FROM")
  void testSalaryHistoryEffectiveFromConstraintViolation_translatesTo409DuplicateEffectiveFrom() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '50-2026-02-01' for key 'uk_salary_history_employment_from'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_EMPLOYMENT_EFFECTIVE_FROM");
  }

  @Test
  @DisplayName("Translates uk_trainees_user_id constraint violation into 409 USER_ALREADY_LINKED")
  void testTraineeUserIdConstraintViolation_translatesTo409UserAlreadyLinked() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '77' for key 'uk_trainees_user_id'");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("USER_ALREADY_LINKED");
  }

  @Test
  @DisplayName("Unrelated or unknown database integrity violations return sanitized HTTP 500 without leaking SQL internals")
  void testUnknownIntegrityViolation_returnsSanitized500() {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Cannot add or update a child row: a foreign key constraint fails (`SIH26135`.`users`, CONSTRAINT `fk_secret_table`)");
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("could not execute statement", sqlEx);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("An unexpected database error occurred. Please try again later.");
    assertThat(response.getBody().getMessage()).doesNotContain("SIH26135");
    assertThat(response.getBody().getMessage()).doesNotContain("fk_secret_table");
  }
}
