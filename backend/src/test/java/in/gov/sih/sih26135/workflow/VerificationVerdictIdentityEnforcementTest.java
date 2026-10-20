package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidCredentialsException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.EmploymentVerificationAttemptService;
import in.gov.sih.sih26135.service.EmploymentVerificationEvidenceService;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
import in.gov.sih.sih26135.service.EmploymentVerificationService;
import in.gov.sih.sih26135.service.FollowupTaskService;
import in.gov.sih.sih26135.service.SurveyResponseService;
import in.gov.sih.sih26135.service.VerificationWorkflowService;
import in.gov.sih.sih26135.service.impl.VerificationWorkflowServiceImpl;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VerificationVerdictIdentityEnforcementTest {

  @Mock private EmploymentRecordService employmentRecordService;
  @Mock private FollowupTaskService followupTaskService;
  @Mock private SurveyResponseService surveyResponseService;
  @Mock private EmploymentVerificationRequestService employmentVerificationRequestService;
  @Mock private EmploymentVerificationAttemptService employmentVerificationAttemptService;
  @Mock private EmploymentVerificationService employmentVerificationService;
  @Mock private EmploymentVerificationEvidenceService employmentVerificationEvidenceService;
  @Mock private UserRepository userRepository;
  @Mock private RefUserStatusRepository refUserStatusRepository;

  private VerificationWorkflowServiceImpl workflowService;

  private final Long AUTHENTICATED_USER_ID = 42L;
  private final String AUTHENTICATED_USERNAME = "authorized_verifier";

  @BeforeEach
  void setUp() {
    workflowService = new VerificationWorkflowServiceImpl(
        employmentRecordService,
        followupTaskService,
        surveyResponseService,
        employmentVerificationRequestService,
        employmentVerificationAttemptService,
        employmentVerificationService,
        employmentVerificationEvidenceService,
        userRepository,
        refUserStatusRepository
    );
    workflowService.setScopeValidator(mock(WorkflowSecurityScopeValidator.class));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAsUserPrincipal(Long userId, String username) {
    UserPrincipal principal = new UserPrincipal(userId, username);
    UsernamePasswordAuthenticationToken auth =
        UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("verification.manage"))
        );
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private User createMockActiveUser(Long userId, String username) {
    User user = new User();
    ReflectionTestUtils.setField(user, "id", userId);
    user.setUsername(username);
    user.setEmail(username + "@example.gov.in");
    user.setUserStatusId(1L);
    return user;
  }

  private RefUserStatus createActiveUserStatus() {
    RefUserStatus status = new RefUserStatus();
    ReflectionTestUtils.setField(status, "id", 1L);
    status.setStatusCode("ACTIVE");
    status.setStatusName("Active");
    return status;
  }

  private EmploymentRecordResponse buildMockEmploymentRecord(Long empId, Long traineeId) {
    return new EmploymentRecordResponse(
        empId, "EMP-" + empId, traineeId, "REG-" + traineeId, "Aditya", "Kumar",
        200L, "ENR-200", 10L, "PLC-001", 600L, "Tech Corp",
        700L, "Branch A", 800L, "Software Engineer", 1L, "REGULAR_WAGE",
        1L, "ACTIVE", "Active spell", true,
        LocalDate.of(2026, 1, 1), null, true,
        new BigDecimal("50000.00"), 1L, "MONTHLY", "INR",
        null, 1L, 1L, 1L, "DIRECT", "Direct",
        1L, "VERIFIED", LocalDateTime.now(), 1L,
        null, null, null, null,
        LocalDateTime.now(), LocalDateTime.now(), null
    );
  }

  private EmploymentVerificationResponse buildMockVerificationResponse(
      Long verificationId, Long empId, Long verifierUserId) {
    return new EmploymentVerificationResponse(
        verificationId, "EV-2026-001", null, null, empId, "EMP-" + empId,
        100L, "REG-100", "Aditya", "Kumar", 10L, "PLC-001",
        600L, "Tech Corp", 1, false, true, 1,
        2L, "VERIFIED", "Verified", 1L, "DIRECT", "Direct",
        1L, "DIRECT", "Direct", null, null, null,
        verifierUserId, null, true, true,
        LocalDateTime.now().minusDays(1), LocalDateTime.now(), LocalDateTime.now(),
        "Verified via official phone call", null,
        LocalDateTime.now().minusDays(1), LocalDateTime.now()
    );
  }

  @Nested
  @DisplayName("1. Identity Attribution and Caller Independence Tests")
  class IdentityAttributionTests {

    @Test
    @DisplayName("Persisted verifier ID strictly matches authenticated UserPrincipal when client supplies no user ID")
    void shouldDeriveVerifierIdFromAuthenticatedUserPrincipalWhenClientOmitsUserId() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User activeUser = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(activeUser));
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(createActiveUserStatus()));

      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentRecordId(10L);
      verReq.setVerificationNumber("EV-2026-001");
      verReq.setRecordVerificationStatusId(2L);
      verReq.setEmploymentVerificationMethodId(1L);
      verReq.setVerifiedByUserId(null); // omitted by client

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(verReq);
      request.setEmploymentRecordStatusId(2L);

      EmploymentVerificationResponse mockVerification =
          buildMockVerificationResponse(1001L, 10L, AUTHENTICATED_USER_ID);
      when(employmentVerificationService.createEmploymentVerification(any())).thenReturn(mockVerification);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(buildMockEmploymentRecord(10L, 100L));
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(buildMockEmploymentRecord(10L, 100L));
      when(employmentRecordService.updateEmploymentRecord(anyLong(), any())).thenReturn(buildMockEmploymentRecord(10L, 100L));

      RecordVerificationVerdictResponse response = workflowService.recordVerificationVerdictAndSynchronize(request);

      assertThat(response).isNotNull();
      ArgumentCaptor<CreateEmploymentVerificationRequest> verCaptor =
          ArgumentCaptor.forClass(CreateEmploymentVerificationRequest.class);
      verify(employmentVerificationService).createEmploymentVerification(verCaptor.capture());
      assertThat(verCaptor.getValue().getVerifiedByUserId()).isEqualTo(AUTHENTICATED_USER_ID);

      ArgumentCaptor<UpdateEmploymentRecordRequest> empCaptor =
          ArgumentCaptor.forClass(UpdateEmploymentRecordRequest.class);
      verify(employmentRecordService).updateEmploymentRecord(anyLong(), empCaptor.capture());
      assertThat(empCaptor.getValue().getVerifiedByUserId()).isEqualTo(AUTHENTICATED_USER_ID);
    }

    @Test
    @DisplayName("Caller cannot impersonate another verifier: client-supplied ID is strictly overridden by authenticated identity")
    void shouldOverrideClientSuppliedVerifierIdWithAuthenticatedPrincipal() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User activeUser = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(activeUser));
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(createActiveUserStatus()));

      Long spoofedUserId = 9999L;
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentRecordId(10L);
      verReq.setVerificationNumber("EV-2026-001");
      verReq.setRecordVerificationStatusId(2L);
      verReq.setEmploymentVerificationMethodId(1L);
      verReq.setVerifiedByUserId(spoofedUserId); // caller attempts to attribute verdict to user 9999

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(verReq);

      EmploymentVerificationResponse mockVerification =
          buildMockVerificationResponse(1001L, 10L, AUTHENTICATED_USER_ID);
      when(employmentVerificationService.createEmploymentVerification(any())).thenReturn(mockVerification);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(buildMockEmploymentRecord(10L, 100L));

      RecordVerificationVerdictResponse response = workflowService.recordVerificationVerdictAndSynchronize(request);

      assertThat(response).isNotNull();
      ArgumentCaptor<CreateEmploymentVerificationRequest> verCaptor =
          ArgumentCaptor.forClass(CreateEmploymentVerificationRequest.class);
      verify(employmentVerificationService).createEmploymentVerification(verCaptor.capture());
      // The spoofed ID must be completely erased and overridden with the authenticated ID
      assertThat(verCaptor.getValue().getVerifiedByUserId()).isEqualTo(AUTHENTICATED_USER_ID);
      assertThat(verCaptor.getValue().getVerifiedByUserId()).isNotEqualTo(spoofedUserId);
    }

    @Test
    @DisplayName("Derives user ID from database lookup when principal is UserDetails")
    void shouldResolveUserIdFromRepositoryWhenPrincipalIsUserDetails() {
      UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
          .username("admin_verifier")
          .password("irrelevant")
          .authorities("verification.manage")
          .build();
      UsernamePasswordAuthenticationToken auth =
          UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(auth);

      User activeUser = createMockActiveUser(77L, "admin_verifier");
      when(userRepository.findByUsernameOrEmail("admin_verifier", "admin_verifier")).thenReturn(Optional.of(activeUser));
      when(userRepository.findById(77L)).thenReturn(Optional.of(activeUser));
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(createActiveUserStatus()));

      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentRecordId(10L);
      verReq.setVerificationNumber("EV-2026-002");
      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(verReq);

      EmploymentVerificationResponse mockVerification = buildMockVerificationResponse(1002L, 10L, 77L);
      when(employmentVerificationService.createEmploymentVerification(any())).thenReturn(mockVerification);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(buildMockEmploymentRecord(10L, 100L));

      workflowService.recordVerificationVerdictAndSynchronize(request);

      ArgumentCaptor<CreateEmploymentVerificationRequest> captor =
          ArgumentCaptor.forClass(CreateEmploymentVerificationRequest.class);
      verify(employmentVerificationService).createEmploymentVerification(captor.capture());
      assertThat(captor.getValue().getVerifiedByUserId()).isEqualTo(77L);
    }
  }

  @Nested
  @DisplayName("2. Authentication and Authorization Boundary Tests")
  class AuthenticationBoundaryTests {

    @Test
    @DisplayName("Rejects verdict recording when security context has no authentication")
    void shouldRejectWhenNoAuthenticationInSecurityContext() {
      SecurityContextHolder.clearContext();

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("User is not authenticated");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentRecordService, never()).updateEmploymentRecord(anyLong(), any());
    }

    @Test
    @DisplayName("Rejects verdict recording when authentication is anonymous")
    void shouldRejectWhenAuthenticationIsAnonymous() {
      AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
          "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
      SecurityContextHolder.getContext().setAuthentication(anonymousToken);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("User is not authenticated");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("Fails closed when principal cannot be mapped to database user")
    void shouldFailClosedWhenPrincipalCannotBeMappedToDatabaseUser() {
      authenticateAsUserPrincipal(999L, "ghost_user");
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }
  }

  @Nested
  @DisplayName("3. Account Inactivity, Lockout, and Soft-Delete Failure Tests")
  class AccountStateSecurityTests {

    @Test
    @DisplayName("Rejects verdict recording when authenticated user account is soft-deleted")
    void shouldRejectWhenAuthenticatedUserIsSoftDeleted() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User deletedUser = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      ReflectionTestUtils.setField(deletedUser, "deletedAt", LocalDateTime.now().minusDays(1));
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(deletedUser));

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("deleted");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("Rejects verdict recording when authenticated user account is not active")
    void shouldRejectWhenAuthenticatedUserStatusIsNotActive() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User inactiveUser = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(inactiveUser));

      RefUserStatus suspendedStatus = new RefUserStatus();
      suspendedStatus.setStatusCode("SUSPENDED");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(suspendedStatus));

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("not active");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("Rejects verdict recording when authenticated user account is locked")
    void shouldRejectWhenAuthenticatedUserIsLocked() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User lockedUser = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      lockedUser.setLockedUntil(LocalDateTime.now().plusHours(2));
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(lockedUser));
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(createActiveUserStatus()));

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("locked");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("Rejects verdict recording before persistence when RefUserStatus record does not exist")
    void shouldRejectWhenRefUserStatusRecordNotFound() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User activeUser = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(activeUser));
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.empty());

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("User account status record not found");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentRecordService, never()).updateEmploymentRecord(anyLong(), any());
    }

    @Test
    @DisplayName("Rejects verdict recording before persistence when user status ID reference is null")
    void shouldRejectWhenUserStatusIdIsNull() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User userWithNullStatus = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      userWithNullStatus.setUserStatusId(null);
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(userWithNullStatus));

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("User account status reference is missing");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentRecordService, never()).updateEmploymentRecord(anyLong(), any());
    }

    @Test
    @DisplayName("Rejects verdict recording when status record has null status code")
    void shouldRejectWhenUserStatusCodeIsNull() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User activeUser = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(activeUser));

      RefUserStatus statusWithNullCode = new RefUserStatus();
      statusWithNullCode.setStatusCode(null);
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(statusWithNullCode));

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("User account is not active");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentRecordService, never()).updateEmploymentRecord(anyLong(), any());
    }
  }

  @Nested
  @DisplayName("4. Validation and Failure Non-Persistence Tests")
  class ValidationAndPersistenceSafetyTests {

    @Test
    @DisplayName("Rejects null request body before any authentication or persistence interaction")
    void shouldRejectNullRequestBody() {
      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(null))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Request body cannot be null");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("Rejects missing verification request before persistence")
    void shouldRejectMissingVerificationRequest() {
      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(null);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Verification request is required");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("Links evidence requests to created verification")
    void shouldLinkEvidenceRequestsToVerificationOutcome() {
      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      User activeUser = createMockActiveUser(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(activeUser));
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(createActiveUserStatus()));

      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentRecordId(10L);
      verReq.setVerificationNumber("EV-2026-003");

      CreateEmploymentVerificationEvidenceRequest evidenceReq = new CreateEmploymentVerificationEvidenceRequest();
      evidenceReq.setEmploymentVerificationEvidenceTypeId(1L);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(verReq);
      request.setEvidenceRequests(List.of(evidenceReq));

      EmploymentVerificationResponse mockVerification = buildMockVerificationResponse(500L, 10L, AUTHENTICATED_USER_ID);
      when(employmentVerificationService.createEmploymentVerification(any())).thenReturn(mockVerification);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(buildMockEmploymentRecord(10L, 100L));
      when(employmentVerificationEvidenceService.createVerificationEvidence(any()))
          .thenReturn(new EmploymentVerificationEvidenceResponse(
              1L, 500L, "EV-2026-003", null, 1L, "DOC", "Doc",
              "REF-001", "sha256hash", "doc.pdf", LocalDateTime.now(),
              AUTHENTICATED_USER_ID, "Notes", LocalDateTime.now(), LocalDateTime.now()));

      RecordVerificationVerdictResponse response = workflowService.recordVerificationVerdictAndSynchronize(request);

      assertThat(response.evidenceList()).hasSize(1);
      ArgumentCaptor<CreateEmploymentVerificationEvidenceRequest> evCaptor =
          ArgumentCaptor.forClass(CreateEmploymentVerificationEvidenceRequest.class);
      verify(employmentVerificationEvidenceService).createVerificationEvidence(evCaptor.capture());
      assertThat(evCaptor.getValue().getEmploymentVerificationId()).isEqualTo(500L);
    }
  }

  @Nested
  @DisplayName("5. Missing Required Repository Fail-Closed Security Tests")
  class MissingRepositoryFailClosedTests {

    @Test
    @DisplayName("Fails closed with IllegalStateException when UserRepository is null")
    void shouldFailClosedWhenUserRepositoryIsNull() {
      VerificationWorkflowServiceImpl serviceWithNullUserRepo = new VerificationWorkflowServiceImpl(
          employmentRecordService,
          followupTaskService,
          surveyResponseService,
          employmentVerificationRequestService,
          employmentVerificationAttemptService,
          employmentVerificationService,
          employmentVerificationEvidenceService,
          null,
          refUserStatusRepository
      );
      serviceWithNullUserRepo.setScopeValidator(mock(WorkflowSecurityScopeValidator.class));

      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> serviceWithNullUserRepo.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("UserRepository is required for verifier validation");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentRecordService, never()).updateEmploymentRecord(anyLong(), any());
    }

    @Test
    @DisplayName("Fails closed with IllegalStateException when RefUserStatusRepository is null")
    void shouldFailClosedWhenRefUserStatusRepositoryIsNull() {
      VerificationWorkflowServiceImpl serviceWithNullStatusRepo = new VerificationWorkflowServiceImpl(
          employmentRecordService,
          followupTaskService,
          surveyResponseService,
          employmentVerificationRequestService,
          employmentVerificationAttemptService,
          employmentVerificationService,
          employmentVerificationEvidenceService,
          userRepository,
          null
      );
      serviceWithNullStatusRepo.setScopeValidator(mock(WorkflowSecurityScopeValidator.class));

      authenticateAsUserPrincipal(AUTHENTICATED_USER_ID, AUTHENTICATED_USERNAME);
      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> serviceWithNullStatusRepo.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("RefUserStatusRepository is required for verifier validation");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentRecordService, never()).updateEmploymentRecord(anyLong(), any());
    }
  }

  @Nested
  @DisplayName("6. Spring Context and Constructor Injection Tests")
  class SpringConstructorInjectionTests {

    @Test
    @DisplayName("Production constructor explicitly annotated with @Autowired and backward-compatible constructor is deprecated")
    void productionConstructorHasAutowiredAnnotation() throws NoSuchMethodException {
      Constructor<VerificationWorkflowServiceImpl> productionConstructor =
          VerificationWorkflowServiceImpl.class.getConstructor(
              EmploymentRecordService.class,
              FollowupTaskService.class,
              SurveyResponseService.class,
              EmploymentVerificationRequestService.class,
              EmploymentVerificationAttemptService.class,
              EmploymentVerificationService.class,
              EmploymentVerificationEvidenceService.class,
              UserRepository.class,
              RefUserStatusRepository.class
          );

      assertThat(productionConstructor).isNotNull();
      assertThat(productionConstructor.isAnnotationPresent(Autowired.class)).isTrue();

      Constructor<VerificationWorkflowServiceImpl> legacyConstructor =
          VerificationWorkflowServiceImpl.class.getConstructor(
              EmploymentRecordService.class,
              FollowupTaskService.class,
              SurveyResponseService.class,
              EmploymentVerificationRequestService.class,
              EmploymentVerificationAttemptService.class,
              EmploymentVerificationService.class,
              EmploymentVerificationEvidenceService.class
          );

      assertThat(legacyConstructor).isNotNull();
      assertThat(legacyConstructor.isAnnotationPresent(Autowired.class)).isFalse();
      assertThat(legacyConstructor.isAnnotationPresent(Deprecated.class)).isTrue();
    }

    @Test
    @DisplayName("Spring IoC container selects @Autowired constructor and constructs bean with non-null repositories")
    void springContextConstructsVerificationWorkflowServiceImplUsingProductionConstructor() {
      try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
        context.registerBean(EmploymentRecordService.class, () -> employmentRecordService);
        context.registerBean(FollowupTaskService.class, () -> followupTaskService);
        context.registerBean(SurveyResponseService.class, () -> surveyResponseService);
        context.registerBean(EmploymentVerificationRequestService.class, () -> employmentVerificationRequestService);
        context.registerBean(EmploymentVerificationAttemptService.class, () -> employmentVerificationAttemptService);
        context.registerBean(EmploymentVerificationService.class, () -> employmentVerificationService);
        context.registerBean(EmploymentVerificationEvidenceService.class, () -> employmentVerificationEvidenceService);
        context.registerBean(UserRepository.class, () -> userRepository);
        context.registerBean(RefUserStatusRepository.class, () -> refUserStatusRepository);
        context.registerBean(WorkflowSecurityScopeValidator.class, () -> mock(WorkflowSecurityScopeValidator.class));
        context.register(VerificationWorkflowServiceImpl.class);
        context.refresh();

        VerificationWorkflowService bean = context.getBean(VerificationWorkflowService.class);
        assertThat(bean).isNotNull();
        assertThat(ReflectionTestUtils.getField(bean, "userRepository")).isSameAs(userRepository);
        assertThat(ReflectionTestUtils.getField(bean, "refUserStatusRepository")).isSameAs(refUserStatusRepository);
      }
    }
  }
}
