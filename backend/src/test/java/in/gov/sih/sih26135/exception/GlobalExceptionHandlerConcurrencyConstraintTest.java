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
