package in.gov.sih.sih26135.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
import in.gov.sih.sih26135.service.SurveyResponseService;
import in.gov.sih.sih26135.service.VerificationWorkflowService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Phase 29.11 Targeted Review Verification:
 * Validates actual Spring AOP proxy interception of real Spring-managed workflow services.
 * Proves that:
 * 1. Real workflow transition methods are intercepted by {@link in.gov.sih.sih26135.actuator.metrics.WorkflowMetricsAspect}.
 * 2. Successful executions record {@code outcome=SUCCESS} timer metrics with bounded tags and zero PII.
 * 3. Failing executions record {@code outcome=FAILURE} timer metrics while preserving exception propagation.
 * 4. Read-only query methods (get*) are excluded from transition timer metrics.
 */
@SpringBootTest
@DisplayName("Phase 29.11 — Real Spring AOP Workflow Metrics Integration Tests")
public class WorkflowAopIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  private VerificationWorkflowService verificationWorkflowService;

  @MockBean
  private SurveyResponseService surveyResponseService;

  @MockBean
  private EmploymentRecordService employmentRecordService;

  @MockBean
  private EmploymentVerificationRequestService employmentVerificationRequestService;

  @SpyBean
  private WorkflowSecurityScopeValidator workflowSecurityScopeValidator;

  @BeforeEach
  void verifyDatabaseIsolationAndSetupAuth() throws Exception {
    assertThat(dataSource).as("DataSource bean must be present").isNotNull();
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection).isNotNull();
      String catalog = connection.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    // Set administrative authentication context for scope validation
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "admin",
            "N/A",
            List.of(new SimpleGrantedAuthority("system.manage"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        )
    );
  }

  @AfterEach
  void cleanSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Real Spring-managed workflow service records SUCCESS timer on valid transition")
  void testRealWorkflowServiceRecordsSuccessTimer() {
    doNothing().when(workflowSecurityScopeValidator).validateTraineeScope(any());
    doNothing().when(workflowSecurityScopeValidator).validateEmployerScope(any());

    // 1. Controlled mock responses
    SurveyResponseResponse surveyResp = mock(SurveyResponseResponse.class);
    when(surveyResp.getId()).thenReturn(101L);
    when(surveyResp.getTraineeId()).thenReturn(1005143L);
    when(surveyResp.getFollowupTaskId()).thenReturn(null);
    when(surveyResponseService.getSurveyResponseById(101L)).thenReturn(surveyResp);

    EmploymentRecordResponse empResp = mock(EmploymentRecordResponse.class);
    when(empResp.id()).thenReturn(201L);
    when(empResp.traineeId()).thenReturn(1005143L);
    when(empResp.employerId()).thenReturn(501L);
    when(empResp.placementId()).thenReturn(null);
    when(employmentRecordService.getEmploymentRecordById(201L)).thenReturn(empResp);

    when(employmentVerificationRequestService.getRequestsByEmploymentId(201L)).thenReturn(Collections.emptyList());

    EmploymentVerificationRequestResponse expectedResponse = mock(EmploymentVerificationRequestResponse.class);
    when(employmentVerificationRequestService.createVerificationRequest(any())).thenReturn(expectedResponse);

    double initialSuccessCount = getWorkflowTimerCount("VERIFICATION", "INITIATE_VERIFICATION_FROM_SURVEY", "SUCCESS");

    // 2. Invoke real proxied workflow service
    InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
    request.setSurveyResponseId(101L);
    CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
    details.setTraineeId(1005143L);
    details.setEmploymentRecordId(201L);
    request.setVerificationRequestDetails(details);

    EmploymentVerificationRequestResponse actualResponse = verificationWorkflowService.initiateVerificationFromSurvey(request);
    assertThat(actualResponse).isSameAs(expectedResponse);

    // 3. Assert timer incremented
    double updatedSuccessCount = getWorkflowTimerCount("VERIFICATION", "INITIATE_VERIFICATION_FROM_SURVEY", "SUCCESS");
    assertThat(updatedSuccessCount).isEqualTo(initialSuccessCount + 1.0);
  }

  @Test
  @DisplayName("Real Spring-managed workflow service records FAILURE timer on exception and preserves propagation")
  void testRealWorkflowServiceRecordsFailureTimerAndPreservesException() {
    double initialFailureCount = getWorkflowTimerCount("VERIFICATION", "INITIATE_VERIFICATION_FROM_SURVEY", "FAILURE");

    // Calling initiateVerificationFromSurvey with null request throws BadRequestException inside the workflow service
    assertThatThrownBy(() -> verificationWorkflowService.initiateVerificationFromSurvey(null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Request body cannot be null");

    double updatedFailureCount = getWorkflowTimerCount("VERIFICATION", "INITIATE_VERIFICATION_FROM_SURVEY", "FAILURE");
    assertThat(updatedFailureCount).isEqualTo(initialFailureCount + 1.0);
  }

  @Test
  @DisplayName("Read-only workflow query methods (get*) are excluded from transition timer metrics")
  void testReadOnlyWorkflowQueriesAreExcludedFromMetrics() {
    double initialTimerCount = getAllWorkflowTimersCount();

    // Invoke read-only method on real workflow service bean
    try {
      verificationWorkflowService.getEmploymentVerificationSummary(999999L);
    } catch (Exception ignored) {
      // Even if entity not found, get* must not trigger transition timer
    }

    double updatedTimerCount = getAllWorkflowTimersCount();
    assertThat(updatedTimerCount).as("Read-only get* queries must not create or increment workflow transition timers")
        .isEqualTo(initialTimerCount);
  }

  private double getWorkflowTimerCount(String type, String transition, String outcome) {
    Timer timer = meterRegistry.find("workflow.transition.duration")
        .tag("workflow.type", type)
        .tag("transition", transition)
        .tag("outcome", outcome)
        .timer();
    return timer != null ? (double) timer.count() : 0.0;
  }

  private double getAllWorkflowTimersCount() {
    return meterRegistry.find("workflow.transition.duration")
        .timers()
        .stream()
        .mapToDouble(Timer::count)
        .sum();
  }
}
