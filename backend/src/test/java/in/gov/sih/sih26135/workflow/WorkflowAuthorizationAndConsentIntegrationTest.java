package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.TraineeRecommendationDecisionRequest;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.OrganizationRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.PlacementRecordService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.EmploymentLifecycleWorkflowService;
import in.gov.sih.sih26135.service.FollowupEngagementWorkflowService;
import in.gov.sih.sih26135.service.PlacementEmploymentWorkflowService;
import in.gov.sih.sih26135.service.SkillGapRemediationWorkflowService;
import in.gov.sih.sih26135.service.TrainingOutcomeWorkflowService;
import in.gov.sih.sih26135.service.VerificationWorkflowService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 25.11 Integration Test Suite:
 * Authorization and Consent Verification at Workflow Boundaries.
 *
 * <p>Verifies:
 * <ul>
 *   <li>1. Unauthenticated requests to all workflow endpoints reject with HTTP 401 UNAUTHORIZED.</li>
 *   <li>2. Method-level RBAC enforcement blocks unauthorized roles/authorities with HTTP 403 FORBIDDEN.</li>
 *   <li>3. Trainee decision boundary fix: allows ROLE_trainee on recommendation accept/reject,
 *          while strictly denying trainees on administrative skill-gap workflow endpoints.</li>
 *   <li>4. Security error sanitization: 401 and 403 responses never leak stack traces or metadata.</li>
 *   <li>5. Database state invariance: rejected unauthorized workflow attempts leave domain tables unchanged.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 25.11 — Workflow Authorization and Consent Boundary Integration Tests")
class WorkflowAuthorizationAndConsentIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @SpyBean
  private WorkflowSecurityScopeValidator scopeValidator;

  @Autowired
  private PlacementEmploymentWorkflowService placementEmploymentWorkflowService;

  @Autowired
  private TrainingOutcomeWorkflowService trainingOutcomeWorkflowService;

  @Autowired
  private SkillGapRemediationWorkflowService skillGapRemediationWorkflowService;

  @Autowired
  private EmploymentLifecycleWorkflowService employmentLifecycleWorkflowService;

  @Autowired
  private FollowupEngagementWorkflowService followupEngagementWorkflowService;

  @Autowired
  private VerificationWorkflowService verificationWorkflowService;

  @MockBean
  private PlacementRecordService placementRecordService;

  @MockBean
  private TrainingEnrollmentService trainingEnrollmentService;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private TraineeRepository traineeRepository;

  @MockBean
  private EmployerRepository employerRepository;

  @MockBean
  private TrainingProviderRepository trainingProviderRepository;

  @MockBean
  private OrganizationRepository organizationRepository;

  private static final String PLACEMENT_WORKFLOW_URL = "/api/v1/workflows/placement-employment";
  private static final String EMPLOYMENT_WORKFLOW_URL = "/api/v1/workflows/employment-lifecycle";
  private static final String VERIFICATION_WORKFLOW_URL = "/api/v1/workflows/verification";
  private static final String TRAINING_OUTCOME_WORKFLOW_URL = "/api/v1/workflows/training-outcome";
  private static final String FOLLOWUP_WORKFLOW_URL = "/api/v1/workflows/followup-engagement";
  private static final String SKILL_GAP_WORKFLOW_URL = "/api/v1/workflows/skill-gap-remediation";

  private String adminToken;
  private String traineeToken;
  private String employerToken;
  private String verifierToken;
  private String employmentOfficerToken;
  private String surveyOfficerToken;
  private String assessmentOfficerToken;

  @BeforeEach
  void setUpAndVerifyCatalogIsolation() throws Exception {
    assertThat(dataSource).as("DataSource must be configured").isNotNull();
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L, "admin_user", List.of("ROLE_super_admin", "system.manage", "assessment.manage", "enrollment.manage", "placement.manage", "employment.manage", "verification.manage", "survey.manage"));

    traineeToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        100L, "trainee_user", List.of("ROLE_trainee", "trainee.read", "consent.manage"));

    employerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        200L, "employer_user", List.of("ROLE_employer", "placement.manage"));

    verifierToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        300L, "verifier_user", List.of("ROLE_verifier", "verification.manage"));

    employmentOfficerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        400L, "employment_officer", List.of("ROLE_government_officer", "employment.manage"));

    surveyOfficerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        500L, "survey_analyst", List.of("ROLE_analyst", "survey.manage"));

    assessmentOfficerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        600L, "assessor_user", List.of("ROLE_assessor", "assessment.manage"));

    User adminUser = new User();
    adminUser.setId(1L);
    lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

    User traineeUser = new User();
    traineeUser.setId(100L);
    lenient().when(userRepository.findById(100L)).thenReturn(Optional.of(traineeUser));

    Trainee trainee = new Trainee();
    trainee.setId(100L);
    trainee.setUser(traineeUser);
    lenient().when(traineeRepository.findByUserId(100L)).thenReturn(Optional.of(trainee));

    User employerUser = new User();
    employerUser.setId(200L);
    employerUser.setEmployerId(200L);
    employerUser.setOrganizationId(700L);
    lenient().when(userRepository.findById(200L)).thenReturn(Optional.of(employerUser));

    User assessorUser = new User();
    assessorUser.setId(600L);
    assessorUser.setTrainingProviderId(600L);
    assessorUser.setOrganizationId(700L);
    lenient().when(userRepository.findById(600L)).thenReturn(Optional.of(assessorUser));

    Employer employer200 = new Employer();
    employer200.setId(200L);
    employer200.setOrganizationId(700L);
    lenient().when(employerRepository.findById(200L)).thenReturn(Optional.of(employer200));

    TrainingProvider provider600 = new TrainingProvider();
    provider600.setId(600L);
    provider600.setOrganizationId(700L);
    lenient().when(trainingProviderRepository.findById(600L)).thenReturn(Optional.of(provider600));

    lenient().when(organizationRepository.existsById(anyLong())).thenReturn(true);
  }

  // =========================================================================
  // 1. Unauthenticated Boundary Tests (HTTP 401 UNAUTHORIZED)
  // =========================================================================
  @Nested
  @DisplayName("1. Unauthenticated Request Boundaries")
  class UnauthenticatedBoundaryTests {

    @Test
    @DisplayName("Placement-Employment workflow rejects unauthenticated requests with 401")
    void placementEmploymentWorkflow_rejectsUnauthenticated() throws Exception {
      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
          .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    @DisplayName("Employment-Lifecycle workflow rejects unauthenticated requests with 401")
    void employmentLifecycleWorkflow_rejectsUnauthenticated() throws Exception {
      mockMvc.perform(post(EMPLOYMENT_WORKFLOW_URL + "/record-exit-and-initiate-unemployment")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Verification workflow rejects unauthenticated requests with 401")
    void verificationWorkflow_rejectsUnauthenticated() throws Exception {
      mockMvc.perform(post(VERIFICATION_WORKFLOW_URL + "/initiate-from-survey")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Training-Outcome workflow rejects unauthenticated requests with 401")
    void trainingOutcomeWorkflow_rejectsUnauthenticated() throws Exception {
      mockMvc.perform(post(TRAINING_OUTCOME_WORKFLOW_URL + "/record-assessment-and-issue-certificate")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Followup-Engagement workflow rejects unauthenticated requests with 401")
    void followupEngagementWorkflow_rejectsUnauthenticated() throws Exception {
      mockMvc.perform(post(FOLLOWUP_WORKFLOW_URL + "/record-survey-response")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Skill-Gap Remediation workflow rejects unauthenticated requests with 401")
    void skillGapWorkflow_rejectsUnauthenticated() throws Exception {
      mockMvc.perform(post(SKILL_GAP_WORKFLOW_URL + "/recommendations/1/accept")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
  }

  // =========================================================================
  // 2. Cross-Domain RBAC Method Security Enforcement (HTTP 403 FORBIDDEN)
  // =========================================================================
  @Nested
  @DisplayName("2. Method-Level RBAC Security Enforcement")
  class RbacMethodSecurityEnforcementTests {

    @Test
    @DisplayName("Placement-Employment workflow requires placement.manage or employment.manage")
    void placementEmploymentWorkflow_rbacCheck() throws Exception {
      // Verifier lacks placement.manage and employment.manage -> 403 Forbidden
      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", verifierToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.message").value("Access denied"));

      // Employer has placement.manage -> Passes method security (400 validation error, not 403)
      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("PLACEMENT_ID_REQUIRED"));
    }

    @Test
    @DisplayName("Employment-Lifecycle workflow requires employment.manage")
    void employmentLifecycleWorkflow_rbacCheck() throws Exception {
      // Employer lacks employment.manage -> 403 Forbidden
      mockMvc.perform(post(EMPLOYMENT_WORKFLOW_URL + "/record-exit-and-initiate-unemployment")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));

      // Employment officer has employment.manage -> Passes method security (400 validation error, not 403)
      mockMvc.perform(post(EMPLOYMENT_WORKFLOW_URL + "/record-exit-and-initiate-unemployment")
              .header("Authorization", employmentOfficerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("EXIT_EVENT_REQUEST_REQUIRED"));
    }

    @Test
    @DisplayName("Verification workflow requires verification.manage")
    void verificationWorkflow_rbacCheck() throws Exception {
      // Employment officer lacks verification.manage -> 403 Forbidden
      mockMvc.perform(post(VERIFICATION_WORKFLOW_URL + "/initiate-from-survey")
              .header("Authorization", employmentOfficerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));

      // Verifier has verification.manage -> Passes method security (400 validation error, not 403)
      mockMvc.perform(post(VERIFICATION_WORKFLOW_URL + "/initiate-from-survey")
              .header("Authorization", verifierToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("SURVEY_RESPONSE_ID_REQUIRED"));
    }

    @Test
    @DisplayName("Training-Outcome workflow requires enrollment.manage or assessment.manage")
    void trainingOutcomeWorkflow_rbacCheck() throws Exception {
      // Verifier lacks enrollment.manage and assessment.manage -> 403 Forbidden
      mockMvc.perform(post(TRAINING_OUTCOME_WORKFLOW_URL + "/record-assessment-and-issue-certificate")
              .header("Authorization", verifierToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));

      // Assessor has assessment.manage -> Passes method security (400 validation error, not 403)
      mockMvc.perform(post(TRAINING_OUTCOME_WORKFLOW_URL + "/record-assessment-and-issue-certificate")
              .header("Authorization", assessmentOfficerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("ASSESSMENT_RESULT_REQUEST_REQUIRED"));
    }

    @Test
    @DisplayName("Followup-Engagement workflow requires survey.manage")
    void followupEngagementWorkflow_rbacCheck() throws Exception {
      // Trainee lacks survey.manage -> 403 Forbidden
      mockMvc.perform(post(FOLLOWUP_WORKFLOW_URL + "/record-survey-response")
              .header("Authorization", traineeToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));

      // Survey analyst has survey.manage -> Passes method security (400 validation error, not 403)
      mockMvc.perform(post(FOLLOWUP_WORKFLOW_URL + "/record-survey-response")
              .header("Authorization", surveyOfficerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("FOLLOWUP_TASK_ID_REQUIRED"));
    }
  }

  // =========================================================================
  // 3. Skill Gap Remediation Trainee Decision Boundary (Fix Verification)
  // =========================================================================
  @Nested
  @DisplayName("3. Skill-Gap Trainee Decision Boundary Fix Verification")
  class SkillGapTraineeDecisionBoundaryTests {

    @Test
    @DisplayName("Trainee user CAN reach acceptRecommendation without receiving 403 Forbidden")
    void traineeUser_canReachAcceptRecommendation() throws Exception {
      // Trainee token holds ROLE_trainee -> passes @PreAuthorize!
      // Reaches service layer, which returns 404 for non-existent recommendation (NOT 403 Forbidden)
      mockMvc.perform(post(SKILL_GAP_WORKFLOW_URL + "/recommendations/999999/accept")
              .header("Authorization", traineeToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Trainee user CAN reach rejectRecommendation without receiving 403 Forbidden")
    void traineeUser_canReachRejectRecommendation() throws Exception {
      // Trainee token holds ROLE_trainee -> passes @PreAuthorize!
      // When remarks are supplied, reaches service and fails on 404 for non-existent recommendation (NOT 403)
      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          100L, "Not suitable for my current career trajectory", null);

      mockMvc.perform(post(SKILL_GAP_WORKFLOW_URL + "/recommendations/999999/reject")
              .header("Authorization", traineeToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Non-trainee without admin authorities receives 403 Forbidden on acceptRecommendation")
    void nonTraineeWithoutAdmin_receives403OnAcceptRecommendation() throws Exception {
      // Employer lacks assessment.manage, enrollment.manage, and ROLE_trainee -> 403 Forbidden
      mockMvc.perform(post(SKILL_GAP_WORKFLOW_URL + "/recommendations/1/accept")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Non-trainee without admin authorities receives 403 Forbidden on rejectRecommendation")
    void nonTraineeWithoutAdmin_receives403OnRejectRecommendation() throws Exception {
      // Verifier lacks assessment.manage, enrollment.manage, and ROLE_trainee -> 403 Forbidden
      mockMvc.perform(post(SKILL_GAP_WORKFLOW_URL + "/recommendations/1/reject")
              .header("Authorization", verifierToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Trainee is strictly BLOCKED from administrative endpoints (record-assessment-with-gaps)")
    void trainee_blockedFromRecordAssessmentWithGaps() throws Exception {
      mockMvc.perform(post(SKILL_GAP_WORKFLOW_URL + "/record-assessment-with-gaps")
              .header("Authorization", traineeToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Trainee is strictly BLOCKED from administrative endpoints (enroll-remediation-course)")
    void trainee_blockedFromEnrollRemediationCourse() throws Exception {
      mockMvc.perform(post(SKILL_GAP_WORKFLOW_URL + "/enroll-remediation-course")
              .header("Authorization", traineeToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Trainee is strictly BLOCKED from administrative profile queries (GET /profile)")
    void trainee_blockedFromGetTraineeSkillGapProfile() throws Exception {
      mockMvc.perform(get(SKILL_GAP_WORKFLOW_URL + "/profile")
              .param("traineeId", "100")
              .header("Authorization", traineeToken))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
  }

  // =========================================================================
  // 4. Authoritative Organization, Provider, Employer, and Trainee Scope Enforcement
  // =========================================================================
  @Nested
  @DisplayName("4. Authoritative Organization, Provider, Employer, and Trainee Scope Enforcement")
  class ScopeBoundaryEnforcementTests {

    @Test
    @DisplayName("Administrative caller bypasses tenant scope checks and is permitted across boundaries")
    void adminCaller_bypassesScopeChecks_permittedAcrossBoundaries() throws Exception {
      // Admin token holds ROLE_super_admin -> passes @PreAuthorize!
      // In service, isAdministrativeCaller() is true -> scope validator does not reject
      // Reaches domain validation: returns 400 for missing payload details (NOT FORBIDDEN or SCOPE MISMATCH)
      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("PLACEMENT_ID_REQUIRED"));
    }

    @Test
    @DisplayName("Cross-employer operation is rejected with EMPLOYER_MISMATCH and zero data disclosure")
    void crossEmployerOperation_rejectedWithScopeMismatch() throws Exception {
      PlacementRecordResponse mockPlacement = new PlacementRecordResponse(
          10L, "PLC-001", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 999L, "OtherEmployer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(10L)).thenReturn(mockPlacement);

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(10L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("EMPLOYER_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Authenticated employer is not authorized to act on records of another employer"))
          .andExpect(jsonPath("$.data").doesNotExist())
          .andExpect(jsonPath("$.exception").doesNotExist())
          .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("Cross-provider operation is rejected with PROVIDER_MISMATCH and zero data disclosure")
    void crossProviderOperation_rejectedWithScopeMismatch() throws Exception {
      TrainingEnrollmentResponse mockEnrollment = new TrainingEnrollmentResponse();
      mockEnrollment.setId(20L);
      mockEnrollment.setTraineeId(100L);
      mockEnrollment.setProviderId(999L);
      when(trainingEnrollmentService.getById(20L)).thenReturn(mockEnrollment);

      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(20L);
      request.setCompletionEnrollmentStatusId(2L);
      CreateAssessmentResultRequest arReq = new CreateAssessmentResultRequest();
      arReq.setTraineeId(100L);
      arReq.setTraineeAssessmentId(1L);
      request.setAssessmentResultRequest(arReq);

      mockMvc.perform(post(TRAINING_OUTCOME_WORKFLOW_URL + "/record-assessment-and-issue-certificate")
              .header("Authorization", assessmentOfficerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("PROVIDER_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Authenticated training provider is not authorized to act on records of another training provider"))
          .andExpect(jsonPath("$.data").doesNotExist())
          .andExpect(jsonPath("$.exception").doesNotExist())
          .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("Cross-trainee operation is rejected with TRAINEE_MISMATCH and zero data disclosure")
    void crossTraineeOperation_rejectedWithScopeMismatch() throws Exception {
      String traineeWithPermToken = "Bearer " + jwtTokenProvider.generateAccessToken(
          100L, "trainee_user", List.of("ROLE_trainee", "placement.manage"));

      mockMvc.perform(get(PLACEMENT_WORKFLOW_URL + "/summary")
              .param("traineeId", "999")
              .header("Authorization", traineeWithPermToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("TRAINEE_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Authenticated trainee is not authorized to act on records of another trainee"))
          .andExpect(jsonPath("$.data").doesNotExist())
          .andExpect(jsonPath("$.exception").doesNotExist())
          .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("Cross-organization operation is rejected with ORGANIZATION_MISMATCH and zero data disclosure")
    void crossOrganizationOperation_rejectedWithScopeMismatch() throws Exception {
      PlacementRecordResponse mockPlacement = new PlacementRecordResponse(
          10L, "PLC-001", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 20L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(10L)).thenReturn(mockPlacement);

      Employer otherOrgEmployer = new Employer();
      otherOrgEmployer.setId(20L);
      otherOrgEmployer.setOrganizationId(800L);
      when(employerRepository.findById(20L)).thenReturn(Optional.of(otherOrgEmployer));

      // User 200 is configured with organizationId = 700L, attempting to access employer belonging to organization 800L
      User orgUser = new User();
      orgUser.setId(200L);
      orgUser.setEmployerId(20L);
      orgUser.setOrganizationId(700L);
      when(userRepository.findById(200L)).thenReturn(Optional.of(orgUser));

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(10L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("ORGANIZATION_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Authenticated organization is not authorized to act on records of another organization"))
          .andExpect(jsonPath("$.data").doesNotExist())
          .andExpect(jsonPath("$.exception").doesNotExist())
          .andExpect(jsonPath("$.trace").doesNotExist());
    }
  }

  // =========================================================================
  // 5. Information Leakage and Security Sanitization Verification
  // =========================================================================
  @Nested
  @DisplayName("5. Security Response Sanitization & Information Leakage Defense")
  class SecuritySanitizationTests {

    @Test
    @DisplayName("401 Unauthorized response does not leak internal exception or database traces")
    void unauthorizedResponse_isSanitized() throws Exception {
      mockMvc.perform(get(PLACEMENT_WORKFLOW_URL + "/summary")
              .param("traineeId", "1"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
          .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
          .andExpect(jsonPath("$.data").doesNotExist())
          .andExpect(jsonPath("$.exception").doesNotExist())
          .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("403 Forbidden response does not leak required permissions or internal roles")
    void forbiddenResponse_isSanitized() throws Exception {
      mockMvc.perform(get(PLACEMENT_WORKFLOW_URL + "/summary")
              .param("traineeId", "1")
              .header("Authorization", verifierToken))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.message").value("Access denied"))
          .andExpect(jsonPath("$.data").doesNotExist())
          .andExpect(jsonPath("$.exception").doesNotExist())
          .andExpect(jsonPath("$.trace").doesNotExist());
    }
  }

  // =========================================================================
  // 6. Real Spring Context Validator Wiring Verification
  // =========================================================================
  @Nested
  @DisplayName("6. Real Spring Context Mandatory Validator Wiring")
  class RealSpringContextWiringTests {

    @Test
    @DisplayName("All six workflow services are initialized with non-null WorkflowSecurityScopeValidator in Spring context")
    void allWorkflowServicesHaveNonNullValidatorWired() {
      assertThat(placementEmploymentWorkflowService).isNotNull();
      assertThat(trainingOutcomeWorkflowService).isNotNull();
      assertThat(skillGapRemediationWorkflowService).isNotNull();
      assertThat(employmentLifecycleWorkflowService).isNotNull();
      assertThat(followupEngagementWorkflowService).isNotNull();
      assertThat(verificationWorkflowService).isNotNull();

      assertThat(ReflectionTestUtils.getField(placementEmploymentWorkflowService, "scopeValidator"))
          .as("PlacementEmploymentWorkflowService must have non-null scopeValidator wired")
          .isNotNull();
      assertThat(ReflectionTestUtils.getField(trainingOutcomeWorkflowService, "scopeValidator"))
          .as("TrainingOutcomeWorkflowService must have non-null scopeValidator wired")
          .isNotNull();
      assertThat(ReflectionTestUtils.getField(skillGapRemediationWorkflowService, "scopeValidator"))
          .as("SkillGapRemediationWorkflowService must have non-null scopeValidator wired")
          .isNotNull();
      assertThat(ReflectionTestUtils.getField(employmentLifecycleWorkflowService, "scopeValidator"))
          .as("EmploymentLifecycleWorkflowService must have non-null scopeValidator wired")
          .isNotNull();
      assertThat(ReflectionTestUtils.getField(followupEngagementWorkflowService, "scopeValidator"))
          .as("FollowupEngagementWorkflowService must have non-null scopeValidator wired")
          .isNotNull();
      assertThat(ReflectionTestUtils.getField(verificationWorkflowService, "scopeValidator"))
          .as("VerificationWorkflowService must have non-null scopeValidator wired")
          .isNotNull();
    }
  }

  // =========================================================================
  // 7. Fail-Closed Scope and Admin-Role Variant Enforcement
  // =========================================================================
  @Nested
  @DisplayName("7. Fail-Closed Scope & Admin-Role Variant Regression Tests")
  class FailClosedScopeAndAdminRoleRegressionTests {

    @Test
    @DisplayName("Target employer not found in repository fails closed with EMPLOYER_NOT_FOUND")
    void targetEmployerNotFound_failsClosed() throws Exception {
      PlacementRecordResponse mockPlacement = new PlacementRecordResponse(
          15L, "PLC-015", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 200L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(15L)).thenReturn(mockPlacement);
      when(employerRepository.findById(200L)).thenReturn(Optional.empty());

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(15L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("EMPLOYER_NOT_FOUND"))
          .andExpect(jsonPath("$.message").value("Target employer record not found for scope validation"));
    }

    @Test
    @DisplayName("Target employer missing organization affiliation fails closed with ORGANIZATION_ASSIGNMENT_REQUIRED")
    void targetEmployerMissingOrgAffiliation_failsClosed() throws Exception {
      PlacementRecordResponse mockPlacement = new PlacementRecordResponse(
          16L, "PLC-016", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 200L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(16L)).thenReturn(mockPlacement);

      Employer employerWithoutOrg = new Employer();
      employerWithoutOrg.setId(200L);
      employerWithoutOrg.setOrganizationId(null);
      when(employerRepository.findById(200L)).thenReturn(Optional.of(employerWithoutOrg));

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(16L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("ORGANIZATION_ASSIGNMENT_REQUIRED"))
          .andExpect(jsonPath("$.message").value("Target employer is missing required organization affiliation"));
    }

    @Test
    @DisplayName("Caller employer missing organization affiliation fails closed with ORGANIZATION_ASSIGNMENT_REQUIRED")
    void callerEmployerMissingOrgAffiliation_failsClosed() throws Exception {
      PlacementRecordResponse mockPlacement = new PlacementRecordResponse(
          17L, "PLC-017", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 200L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(17L)).thenReturn(mockPlacement);

      // Caller user 200 has null organizationId and employer entity has null organizationId
      User callerWithoutOrg = new User();
      callerWithoutOrg.setId(200L);
      callerWithoutOrg.setEmployerId(200L);
      callerWithoutOrg.setOrganizationId(null);
      when(userRepository.findById(200L)).thenReturn(Optional.of(callerWithoutOrg));

      Employer callerEmployerWithoutOrg = new Employer();
      callerEmployerWithoutOrg.setId(200L);
      callerEmployerWithoutOrg.setOrganizationId(null);
      when(employerRepository.findById(200L)).thenReturn(Optional.of(callerEmployerWithoutOrg));

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(17L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("ORGANIZATION_ASSIGNMENT_REQUIRED"));
    }

    @Test
    @DisplayName("Unauthorized admin-role variant ROLE_government_officer does NOT bypass scope check")
    void roleGovernmentOfficer_doesNotBypassScopeCheck() throws Exception {
      String officerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
          888L, "gov_officer", List.of("ROLE_government_officer", "placement.manage"));

      User officerUser = new User();
      officerUser.setId(888L);
      officerUser.setEmployerId(null);
      when(userRepository.findById(888L)).thenReturn(Optional.of(officerUser));

      PlacementRecordResponse mockPlacement = new PlacementRecordResponse(
          18L, "PLC-018", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 200L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(18L)).thenReturn(mockPlacement);

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(18L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", officerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("EMPLOYER_ASSIGNMENT_REQUIRED"))
          .andExpect(jsonPath("$.message").value("Authenticated user is not assigned to an employer"));
    }

    @Test
    @DisplayName("Unauthorized admin-role variant ROLE_ADMIN does NOT bypass scope check")
    void roleAdmin_doesNotBypassScopeCheck() throws Exception {
      String roleAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
          889L, "legacy_admin", List.of("ROLE_ADMIN", "placement.manage"));

      User legacyAdminUser = new User();
      legacyAdminUser.setId(889L);
      legacyAdminUser.setEmployerId(null);
      when(userRepository.findById(889L)).thenReturn(Optional.of(legacyAdminUser));

      PlacementRecordResponse mockPlacement = new PlacementRecordResponse(
          19L, "PLC-019", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 200L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(19L)).thenReturn(mockPlacement);

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(19L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", roleAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("EMPLOYER_ASSIGNMENT_REQUIRED"))
          .andExpect(jsonPath("$.message").value("Authenticated user is not assigned to an employer"));
    }
  }

  // =========================================================================
  // 8. Database State Invariance on Authorization Rejection
  // =========================================================================
  @Nested
  @DisplayName("6. Persisted State Invariance on Security Rejection")
  class StateInvarianceTests {

    private final String[] SENSITIVE_TABLES = {
        "assessment_results",
        "certifications",
        "training_enrollments",
        "placement_records",
        "employment_records",
        "salary_history",
        "employment_exit_events",
        "trainee_unemployment_events",
        "employment_verification_requests",
        "employment_verifications",
        "employment_verification_evidence",
        "skill_gap_assessments",
        "skill_gaps",
        "skill_gap_recommendations",
        "followup_tasks",
        "survey_responses",
        "communication_logs"
    };

    @Test
    @DisplayName("Rejected unauthorized operations leave domain table row counts completely unchanged")
    void rejectedOperations_leaveStateUnchanged() throws Exception {
      Map<String, Long> countsBefore = captureRowCounts();

      // Attempt 1: Unauthorized mutation on Placement workflow (verifier -> placement)
      mockMvc.perform(post(PLACEMENT_WORKFLOW_URL + "/transition-placement-to-employment")
              .header("Authorization", verifierToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden());

      // Attempt 2: Unauthorized mutation on Employment workflow (employer -> employment-lifecycle)
      mockMvc.perform(post(EMPLOYMENT_WORKFLOW_URL + "/record-exit-and-initiate-unemployment")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden());

      // Attempt 3: Unauthorized mutation on Follow-up workflow (trainee -> followup)
      mockMvc.perform(post(FOLLOWUP_WORKFLOW_URL + "/record-survey-response")
              .header("Authorization", traineeToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden());

      // Attempt 4: Unauthorized mutation on Skill-gap workflow (employer -> skill-gap accept)
      mockMvc.perform(post(SKILL_GAP_WORKFLOW_URL + "/recommendations/1/accept")
              .header("Authorization", employerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden());

      // Attempt 5: Unauthenticated mutation
      mockMvc.perform(post(TRAINING_OUTCOME_WORKFLOW_URL + "/record-assessment-and-issue-certificate")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized());

      Map<String, Long> countsAfter = captureRowCounts();

      for (String table : SENSITIVE_TABLES) {
        assertThat(countsAfter.get(table))
            .as("Row count for table " + table + " must remain unchanged after rejected operations")
            .isEqualTo(countsBefore.get(table));
      }
    }

    private Map<String, Long> captureRowCounts() throws Exception {
      Map<String, Long> counts = new HashMap<>();
      try (Connection conn = dataSource.getConnection();
           Statement stmt = conn.createStatement()) {
        for (String table : SENSITIVE_TABLES) {
          try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (rs.next()) {
              counts.put(table, rs.getLong(1));
            }
          }
        }
      }
      return counts;
    }
  }
}
