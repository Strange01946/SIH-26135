package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ReopenDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.StartInvestigationRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.DataQualityIssueEvent;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidDataQualityTransitionException;
import in.gov.sih.sih26135.exception.OperationalErrorCodes;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityIssueEventMapper;
import in.gov.sih.sih26135.mapper.DataQualityIssueMapper;
import in.gov.sih.sih26135.repository.DataQualityIssueEventRepository;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.DataQualityWorkflowServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Phase 28.4 - Data Quality Workflow Service & State Machine Unit Tests")
class DataQualityWorkflowServiceTest {

  @Mock
  private DataQualityIssueRepository dataQualityIssueRepository;

  @Mock
  private DataQualityIssueEventRepository dataQualityIssueEventRepository;

  @Mock
  private RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TraineeRepository traineeRepository;

  @Mock
  private WorkflowSecurityScopeValidator scopeValidator;

  @Mock
  private DataQualityIssueMapper issueMapper;

  @Mock
  private DataQualityIssueEventMapper eventMapper;

  @Mock
  private JdbcTemplate jdbcTemplate;

  private DataQualityWorkflowServiceImpl workflowService;

  private RefDataQualityIssueStatus openStatus;
  private RefDataQualityIssueStatus assignedStatus;
  private RefDataQualityIssueStatus inProgressStatus;
  private RefDataQualityIssueStatus resolvedStatus;
  private RefDataQualityIssueStatus dismissedStatus;
  private RefDataQualityIssueStatus reopenedStatus;

  private User adminCaller;
  private User assigneeUser;

  @BeforeEach
  void setUp() {
    workflowService = new DataQualityWorkflowServiceImpl(
        dataQualityIssueRepository,
        dataQualityIssueEventRepository,
        refDataQualityIssueStatusRepository,
        userRepository,
        traineeRepository,
        scopeValidator,
        issueMapper,
        eventMapper,
        jdbcTemplate
    );

    openStatus = new RefDataQualityIssueStatus();
    openStatus.setId(1L);
    openStatus.setStatusCode("OPEN");
    openStatus.setStatusName("Open");
    openStatus.setIsTerminalFlag(false);

    assignedStatus = new RefDataQualityIssueStatus();
    assignedStatus.setId(2L);
    assignedStatus.setStatusCode("ASSIGNED");
    assignedStatus.setStatusName("Assigned");
    assignedStatus.setIsTerminalFlag(false);

    inProgressStatus = new RefDataQualityIssueStatus();
    inProgressStatus.setId(3L);
    inProgressStatus.setStatusCode("IN_PROGRESS");
    inProgressStatus.setStatusName("In Progress");
    inProgressStatus.setIsTerminalFlag(false);

    resolvedStatus = new RefDataQualityIssueStatus();
    resolvedStatus.setId(4L);
    resolvedStatus.setStatusCode("RESOLVED");
    resolvedStatus.setStatusName("Resolved");
    resolvedStatus.setIsTerminalFlag(true);

    dismissedStatus = new RefDataQualityIssueStatus();
    dismissedStatus.setId(5L);
    dismissedStatus.setStatusCode("DISMISSED");
    dismissedStatus.setStatusName("Dismissed");
    dismissedStatus.setIsTerminalFlag(true);

    reopenedStatus = new RefDataQualityIssueStatus();
    reopenedStatus.setId(6L);
    reopenedStatus.setStatusCode("REOPENED");
    reopenedStatus.setStatusName("Reopened");
    reopenedStatus.setIsTerminalFlag(false);

    adminCaller = new User();
    adminCaller.setId(10L);
    adminCaller.setUsername("superadmin");

    assigneeUser = new User();
    assigneeUser.setId(20L);
    assigneeUser.setUsername("reviewer");
    assigneeUser.setStateId(1L);

    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(1);
    when(jdbcTemplate.queryForObject(contains("ROLE_trainee"), eq(Integer.class), anyLong())).thenReturn(0);
    when(jdbcTemplate.queryForObject(contains("FOR UPDATE"), eq(Long.class), anyLong())).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private DataQualityIssue createTestIssue(Long id, RefDataQualityIssueStatus status) {
    DataQualityIssue issue = new DataQualityIssue();
    issue.setId(id);
    issue.setDataQualityIssueStatus(status);
    issue.setEntityType("Trainee");
    issue.setEntityId(100L);
    issue.setIssueSummary("Test anomaly detected");
    issue.setDetectedAt(LocalDateTime.now().minusDays(1));
    issue.setAssignedUserId(20L);
    return issue;
  }

  private void mockAdminAuthentication() {
    when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(adminCaller.getId());
    when(scopeValidator.resolveAuthenticatedUser()).thenReturn(Optional.of(adminCaller));
    when(scopeValidator.isNationwideAdmin()).thenReturn(true);
    when(scopeValidator.isTraineeCaller()).thenReturn(false);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            adminCaller, null, List.of(
                new SimpleGrantedAuthority("ROLE_super_admin"),
                new SimpleGrantedAuthority("system.manage")
            )
        )
    );
  }

  private void mockUserAuthentication(User user, String... authorities) {
    when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(user.getId());
    when(scopeValidator.resolveAuthenticatedUser()).thenReturn(Optional.of(user));
    when(scopeValidator.isNationwideAdmin()).thenReturn(false);
    when(scopeValidator.isTraineeCaller()).thenReturn(false);

    List<SimpleGrantedAuthority> auths = Arrays.stream(authorities)
        .map(SimpleGrantedAuthority::new)
        .toList();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, auths)
    );
  }

  @Nested
  @DisplayName("1. Assignment Transition (OPEN -> ASSIGNED, ASSIGNED -> ASSIGNED, REOPENED -> ASSIGNED)")
  class AssignmentTransitions {

    @Test
    @DisplayName("Should successfully assign an OPEN issue to a valid user")
    void shouldAssignOpenIssueSuccessfully() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));
      when(traineeRepository.findByUserId(20L)).thenReturn(Optional.empty());
      when(refDataQualityIssueStatusRepository.findByStatusCode("ASSIGNED")).thenReturn(Optional.of(assignedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L, "Assigned for manual review");
      workflowService.assignIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(assignedStatus);
      assertThat(issue.getAssignedUserId()).isEqualTo(20L);

      ArgumentCaptor<DataQualityIssueEvent> eventCaptor = ArgumentCaptor.forClass(DataQualityIssueEvent.class);
      verify(dataQualityIssueEventRepository).save(eventCaptor.capture());
      DataQualityIssueEvent savedEvent = eventCaptor.getValue();
      assertThat(savedEvent.getDataQualityIssueStatus()).isEqualTo(assignedStatus);
      assertThat(savedEvent.getChangedByUserId()).isEqualTo(10L);
      assertThat(savedEvent.getRemarks()).isEqualTo("Assigned for manual review");
    }

    @Test
    @DisplayName("Should allow re-assignment when issue is already ASSIGNED")
    void shouldAllowReassignmentWhenAlreadyAssigned() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, assignedStatus);
      issue.setAssignedUserId(15L);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));
      when(traineeRepository.findByUserId(20L)).thenReturn(Optional.empty());
      when(refDataQualityIssueStatusRepository.findByStatusCode("ASSIGNED")).thenReturn(Optional.of(assignedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L);
      workflowService.assignIssue(1L, request);

      assertThat(issue.getAssignedUserId()).isEqualTo(20L);
      verify(dataQualityIssueEventRepository).save(any(DataQualityIssueEvent.class));
    }

    @Test
    @DisplayName("Should allow assignment when issue is REOPENED")
    void shouldAllowAssignmentWhenReopened() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, reopenedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));
      when(traineeRepository.findByUserId(20L)).thenReturn(Optional.empty());
      when(refDataQualityIssueStatusRepository.findByStatusCode("ASSIGNED")).thenReturn(Optional.of(assignedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L, "Reassigned after reopen");
      workflowService.assignIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(assignedStatus);
      assertThat(issue.getAssignedUserId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Should reject assigning an issue that is already IN_PROGRESS with 409 Conflict")
    void shouldRejectAssignmentWhenInProgress() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L);
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from IN_PROGRESS to ASSIGNED")
          .satisfies(ex -> assertThat(((InvalidDataQualityTransitionException) ex).getErrorCode())
              .isEqualTo(OperationalErrorCodes.INVALID_DQ_TRANSITION));

      verify(dataQualityIssueRepository, never()).save(any());
      verify(dataQualityIssueEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject assigning an issue that is already RESOLVED with 409 Conflict")
    void shouldRejectAssignmentWhenResolved() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, resolvedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L);
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from RESOLVED to ASSIGNED");
    }

    @Test
    @DisplayName("Should reject assigning to Trainee user with 400 Bad Request")
    void shouldRejectAssignmentToTrainee() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));
      when(traineeRepository.findByUserId(20L)).thenReturn(Optional.of(new in.gov.sih.sih26135.entity.Trainee()));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L);
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Trainees cannot be assigned data quality issues");
    }

    @Test
    @DisplayName("Should reject assigning to non-existent user with 404 Not Found")
    void shouldRejectAssignmentToNonExistentUser() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(999L);
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should reject assigning to deleted/inactive user with 400 Bad Request")
    void shouldRejectAssignmentToDeletedUser() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      assigneeUser.setDeletedAt(LocalDateTime.now());
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L);
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Assignee user is deleted or inactive");
    }

    @Test
    @DisplayName("Should reject nationwide admin assigning to unrelated assignee outside issue tenant scope")
    void shouldRejectNationwideAdminAssigningToUnrelatedAssignee() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);

      User unrelatedAssignee = new User();
      unrelatedAssignee.setId(25L);
      unrelatedAssignee.setStateId(2L); // State 2, but trainee is in State 1

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(25L)).thenReturn(Optional.of(unrelatedAssignee));
      when(traineeRepository.findByUserId(25L)).thenReturn(Optional.empty());
      when(jdbcTemplate.queryForObject(contains("trainees"), eq(Integer.class), eq(100L), eq(2L))).thenReturn(0);

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(25L);
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Assignee state scope does not match issue trainee state");
    }

    @Test
    @DisplayName("Should reject provider caller assigning to unscoped user (null tenancy fields)")
    void shouldRejectProviderAssigningToUnscopedUser() {
      User providerCaller = new User();
      providerCaller.setId(50L);
      providerCaller.setTrainingProviderId(10L);
      mockUserAuthentication(providerCaller, "ROLE_training_provider", "system.manage");

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);

      User unscopedAssignee = new User();
      unscopedAssignee.setId(60L);
      // All tenancy fields null (missing scope)
      unscopedAssignee.setStateId(null);
      unscopedAssignee.setTrainingProviderId(null);
      unscopedAssignee.setEmployerId(null);

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(60L)).thenReturn(Optional.of(unscopedAssignee));
      when(traineeRepository.findByUserId(60L)).thenReturn(Optional.empty());

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(60L);
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Assignee has missing or ambiguous tenant scope");
    }

    @Test
    @DisplayName("Should reject assignee with conflicting tenant scope (both provider and employer)")
    void shouldRejectAssigneeWithConflictingScope() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);

      User conflictingAssignee = new User();
      conflictingAssignee.setId(70L);
      conflictingAssignee.setTrainingProviderId(10L);
      conflictingAssignee.setEmployerId(20L); // Conflicting!

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(70L)).thenReturn(Optional.of(conflictingAssignee));
      when(traineeRepository.findByUserId(70L)).thenReturn(Optional.empty());

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(70L);
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Assignee has conflicting tenant scope");
    }
  }

  @Nested
  @DisplayName("2. Investigation Transition (ASSIGNED -> IN_PROGRESS)")
  class InvestigationTransitions {

    @Test
    @DisplayName("Should successfully start investigation from ASSIGNED state by assigned investigator")
    void shouldStartInvestigationSuccessfullyByAssignedInvestigator() {
      User assignedInvestigator = new User();
      assignedInvestigator.setId(20L);
      assignedInvestigator.setStateId(1L);
      mockUserAuthentication(assignedInvestigator, "ROLE_state_admin", "system.manage");

      DataQualityIssue issue = createTestIssue(1L, assignedStatus);
      issue.setAssignedUserId(20L);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("IN_PROGRESS")).thenReturn(Optional.of(inProgressStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      StartInvestigationRequest request = new StartInvestigationRequest("Initiating forensic audit");
      workflowService.startInvestigation(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(inProgressStatus);

      ArgumentCaptor<DataQualityIssueEvent> eventCaptor = ArgumentCaptor.forClass(DataQualityIssueEvent.class);
      verify(dataQualityIssueEventRepository).save(eventCaptor.capture());
      assertThat(eventCaptor.getValue().getDataQualityIssueStatus()).isEqualTo(inProgressStatus);
      assertThat(eventCaptor.getValue().getRemarks()).isEqualTo("Initiating forensic audit");
    }

    @Test
    @DisplayName("Should allow administrative supervisor to start investigation override")
    void shouldAllowSupervisorToStartInvestigation() {
      mockAdminAuthentication(); // AdminCaller (id=10)
      DataQualityIssue issue = createTestIssue(1L, assignedStatus);
      issue.setAssignedUserId(20L); // Assigned to user 20
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("IN_PROGRESS")).thenReturn(Optional.of(inProgressStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      StartInvestigationRequest request = new StartInvestigationRequest("Supervisor starting audit");
      workflowService.startInvestigation(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(inProgressStatus);
    }

    @Test
    @DisplayName("Should reject unauthorized investigator from starting investigation with 403 Forbidden")
    void shouldRejectUnauthorizedInvestigatorStartingInvestigation() {
      User otherUser = new User();
      otherUser.setId(30L);
      otherUser.setTrainingProviderId(10L);
      mockUserAuthentication(otherUser, "ROLE_training_provider", "system.manage");

      DataQualityIssue issue = createTestIssue(1L, assignedStatus);
      issue.setAssignedUserId(20L); // Assigned to user 20, caller is user 30
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      StartInvestigationRequest request = new StartInvestigationRequest("Unauthorized start");
      assertThatThrownBy(() -> workflowService.startInvestigation(1L, request))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Only the assigned investigator or an authorized administrative supervisor can start investigation");
    }

    @Test
    @DisplayName("Should reject starting investigation directly from OPEN without assignment with 409 Conflict")
    void shouldRejectStartingInvestigationDirectlyFromOpen() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      StartInvestigationRequest request = new StartInvestigationRequest("Direct start");
      assertThatThrownBy(() -> workflowService.startInvestigation(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from OPEN to IN_PROGRESS")
          .satisfies(ex -> assertThat(((InvalidDataQualityTransitionException) ex).getErrorCode())
              .isEqualTo(OperationalErrorCodes.INVALID_DQ_TRANSITION));
    }

    @Test
    @DisplayName("Should reject starting investigation when already IN_PROGRESS with 409 Conflict")
    void shouldRejectStartingInvestigationWhenAlreadyInProgress() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      StartInvestigationRequest request = new StartInvestigationRequest("Duplicate start");
      assertThatThrownBy(() -> workflowService.startInvestigation(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from IN_PROGRESS to IN_PROGRESS");
    }
  }

  @Nested
  @DisplayName("3. Resolution Transition (IN_PROGRESS -> RESOLVED)")
  class ResolutionTransitions {

    @Test
    @DisplayName("Should successfully resolve an IN_PROGRESS issue by assigned investigator")
    void shouldResolveInProgressIssueSuccessfullyByAssignedInvestigator() {
      User assignedInvestigator = new User();
      assignedInvestigator.setId(20L);
      assignedInvestigator.setStateId(1L);
      mockUserAuthentication(assignedInvestigator, "ROLE_state_admin", "system.manage");

      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      issue.setAssignedUserId(20L);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("RESOLVED")).thenReturn(Optional.of(resolvedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Discrepancy remediated in enrollment register");
      workflowService.resolveIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(resolvedStatus);
      assertThat(issue.getResolvedByUserId()).isEqualTo(20L);
      assertThat(issue.getResolvedAt()).isNotNull();
      assertThat(issue.getResolutionNotes()).isEqualTo("Discrepancy remediated in enrollment register");

      ArgumentCaptor<DataQualityIssueEvent> eventCaptor = ArgumentCaptor.forClass(DataQualityIssueEvent.class);
      verify(dataQualityIssueEventRepository).save(eventCaptor.capture());
      assertThat(eventCaptor.getValue().getDataQualityIssueStatus()).isEqualTo(resolvedStatus);
      assertThat(eventCaptor.getValue().getRemarks()).isEqualTo("Discrepancy remediated in enrollment register");
    }

    @Test
    @DisplayName("Should reject unauthorized user resolving issue with 403 Forbidden")
    void shouldRejectUnauthorizedUserResolvingIssue() {
      User otherUser = new User();
      otherUser.setId(30L);
      otherUser.setTrainingProviderId(10L);
      mockUserAuthentication(otherUser, "ROLE_training_provider", "system.manage");

      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      issue.setAssignedUserId(20L); // Assigned to user 20
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Unauthorized resolution attempt");
      assertThatThrownBy(() -> workflowService.resolveIssue(1L, request))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Only the assigned investigator or an authorized administrative supervisor can resolve this issue");
    }

    @Test
    @DisplayName("Should reject resolution directly from OPEN with 409 Conflict")
    void shouldRejectResolutionDirectlyFromOpen() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Premature resolution");
      assertThatThrownBy(() -> workflowService.resolveIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from OPEN to RESOLVED");
    }

    @Test
    @DisplayName("Should reject resolution directly from ASSIGNED without investigation with 409 Conflict")
    void shouldRejectResolutionDirectlyFromAssigned() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, assignedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Skipping investigation");
      assertThatThrownBy(() -> workflowService.resolveIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from ASSIGNED to RESOLVED");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null, empty or whitespace-only resolution notes with 400 Bad Request")
    void shouldRejectBlankResolutionNotes(String blankNotes) {
      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest(blankNotes);
      assertThatThrownBy(() -> workflowService.resolveIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("resolutionNotes cannot be blank");
    }

    @Test
    @DisplayName("Should reject resolution notes exceeding 500 characters with 400 Bad Request")
    void shouldRejectOversizedResolutionNotes() {
      String oversized = "A".repeat(501);
      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest(oversized);
      assertThatThrownBy(() -> workflowService.resolveIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("resolutionNotes cannot exceed 500 characters");
    }
  }

  @Nested
  @DisplayName("4. Dismissal Transition (OPEN -> DISMISSED, IN_PROGRESS -> DISMISSED)")
  class DismissTransitions {

    @Test
    @DisplayName("Should successfully dismiss an OPEN issue with remarks and event")
    void shouldDismissOpenIssueSuccessfully() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("DISMISSED")).thenReturn(Optional.of(dismissedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("False positive verified with source documentation");
      workflowService.dismissIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(dismissedStatus);
      assertThat(issue.getResolvedByUserId()).isEqualTo(adminCaller.getId());
      assertThat(issue.getResolutionNotes()).isEqualTo("False positive verified with source documentation");

      ArgumentCaptor<DataQualityIssueEvent> eventCaptor = ArgumentCaptor.forClass(DataQualityIssueEvent.class);
      verify(dataQualityIssueEventRepository).save(eventCaptor.capture());
      assertThat(eventCaptor.getValue().getDataQualityIssueStatus()).isEqualTo(dismissedStatus);
      assertThat(eventCaptor.getValue().getRemarks()).isEqualTo("False positive verified with source documentation");
    }

    @Test
    @DisplayName("Should successfully dismiss an IN_PROGRESS issue by assigned investigator")
    void shouldDismissInProgressIssueSuccessfullyByAssignedInvestigator() {
      User assignedInvestigator = new User();
      assignedInvestigator.setId(20L);
      assignedInvestigator.setStateId(1L);
      mockUserAuthentication(assignedInvestigator, "ROLE_state_admin", "system.manage");

      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      issue.setAssignedUserId(20L);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("DISMISSED")).thenReturn(Optional.of(dismissedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Investigation concluded as non-actionable");
      workflowService.dismissIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(dismissedStatus);
    }

    @Test
    @DisplayName("Should explicitly reject ASSIGNED -> DISMISSED with 409 Conflict")
    void shouldRejectDismissingAssignedIssueWith409() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, assignedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Cannot dismiss assigned directly");
      assertThatThrownBy(() -> workflowService.dismissIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from ASSIGNED to DISMISSED")
          .satisfies(ex -> assertThat(((InvalidDataQualityTransitionException) ex).getErrorCode())
              .isEqualTo(OperationalErrorCodes.INVALID_DQ_TRANSITION));
    }

    @Test
    @DisplayName("Should explicitly reject REOPENED -> DISMISSED with 409 Conflict")
    void shouldRejectDismissingReopenedIssueWith409() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, reopenedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Cannot dismiss reopened directly");
      assertThatThrownBy(() -> workflowService.dismissIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from REOPENED to DISMISSED")
          .satisfies(ex -> assertThat(((InvalidDataQualityTransitionException) ex).getErrorCode())
              .isEqualTo(OperationalErrorCodes.INVALID_DQ_TRANSITION));
    }

    @Test
    @DisplayName("Should reject dismissing an already RESOLVED issue with 409 Conflict")
    void shouldRejectDismissingResolvedIssue() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, resolvedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Cannot dismiss resolved issue");
      assertThatThrownBy(() -> workflowService.dismissIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from RESOLVED to DISMISSED");
    }

    @Test
    @DisplayName("Should reject dismissing an already DISMISSED issue with 409 Conflict")
    void shouldRejectDismissingAlreadyDismissedIssue() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, dismissedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Duplicate dismissal");
      assertThatThrownBy(() -> workflowService.dismissIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from DISMISSED to DISMISSED");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null, empty or whitespace-only dismissal remarks with 400 Bad Request")
    void shouldRejectBlankDismissalRemarks(String blankRemarks) {
      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest(blankRemarks);
      assertThatThrownBy(() -> workflowService.dismissIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("remarks cannot be blank");
    }
  }

  @Nested
  @DisplayName("5. Reopening Transition (RESOLVED -> REOPENED)")
  class ReopenTransitions {

    @Test
    @DisplayName("Should successfully reopen a RESOLVED issue, clear attribution, and append event")
    void shouldReopenResolvedIssueSuccessfully() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, resolvedStatus);
      issue.setResolvedByUserId(10L);
      issue.setResolvedAt(LocalDateTime.now().minusHours(5));
      issue.setResolutionNotes("Initial resolution");

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("REOPENED")).thenReturn(Optional.of(reopenedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest("Audit showed secondary enrollment anomaly persisted");
      workflowService.reopenIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(reopenedStatus);
      assertThat(issue.getResolvedByUserId()).isNull();
      assertThat(issue.getResolvedAt()).isNull();
      assertThat(issue.getResolutionNotes()).isNull();

      ArgumentCaptor<DataQualityIssueEvent> eventCaptor = ArgumentCaptor.forClass(DataQualityIssueEvent.class);
      verify(dataQualityIssueEventRepository).save(eventCaptor.capture());
      assertThat(eventCaptor.getValue().getDataQualityIssueStatus()).isEqualTo(reopenedStatus);
      assertThat(eventCaptor.getValue().getRemarks()).isEqualTo("Audit showed secondary enrollment anomaly persisted");
    }

    @Test
    @DisplayName("Should reject reopening an already DISMISSED issue with 409 Conflict")
    void shouldRejectReopeningDismissedIssue() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, dismissedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest("Cannot reopen dismissed");
      assertThatThrownBy(() -> workflowService.reopenIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from DISMISSED to REOPENED");
    }

    @Test
    @DisplayName("Should reject reopening an OPEN issue with 409 Conflict")
    void shouldRejectReopeningOpenIssue() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest("Cannot reopen open issue");
      assertThatThrownBy(() -> workflowService.reopenIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from OPEN to REOPENED");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should reject null, empty or whitespace-only reopening reason with 400 Bad Request")
    void shouldRejectBlankReopeningReason(String blankReason) {
      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest(blankReason);
      assertThatThrownBy(() -> workflowService.reopenIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("reopeningReason cannot be blank");
    }

    @Test
    @DisplayName("Should reject reopening reason exceeding 500 characters with 400 Bad Request")
    void shouldRejectOversizedReopeningReason() {
      String oversized = "R".repeat(501);
      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest(oversized);
      assertThatThrownBy(() -> workflowService.reopenIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("reopeningReason cannot exceed 500 characters");
    }
  }

  @Nested
  @DisplayName("6. Security Scope & Multi-Tenant Boundaries")
  class SecurityScopeAndTenantBoundaries {

    @Test
    @DisplayName("Should reject Trainee callers from managing data quality issues")
    void shouldRejectTraineeCaller() {
      User trainee = new User();
      trainee.setId(50L);
      mockUserAuthentication(trainee, "ROLE_trainee", "system.manage");
      when(scopeValidator.isTraineeCaller()).thenReturn(true);

      assertThatThrownBy(() -> workflowService.startInvestigation(1L, new StartInvestigationRequest()))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Trainees are not authorized");
    }

    @Test
    @DisplayName("Should reject State Admin when issue entity belongs to a different state")
    void shouldRejectStateAdminForOutOfScopeEntity() {
      User stateAdmin = new User();
      stateAdmin.setId(11L);
      stateAdmin.setStateId(5L); // State 5
      mockUserAuthentication(stateAdmin, "ROLE_state_admin", "system.manage");

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      // Mock database returning 0 count for trainee in state 5
      when(jdbcTemplate.queryForObject(contains("trainees"), eq(Integer.class), eq(100L), eq(5L))).thenReturn(0);

      assertThatThrownBy(() -> workflowService.dismissIssue(1L, new DismissDataQualityIssueRequest("Out of state dismissal")))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("does not belong to caller state scope");
    }

    @Test
    @DisplayName("Should reject Employer caller attempting to manage Trainee issues")
    void shouldRejectEmployerFromTraineeIssues() {
      User employerUser = new User();
      employerUser.setId(12L);
      employerUser.setEmployerId(88L); // Employer persona
      mockUserAuthentication(employerUser, "ROLE_employer", "system.manage");

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      assertThatThrownBy(() -> workflowService.dismissIssue(1L, new DismissDataQualityIssueRequest("Employer dismissing trainee")))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Employers are not authorized to manage trainee data quality issues");
    }

    @Test
    @DisplayName("Should reject caller with audit.read alone from lifecycle mutation methods")
    void shouldRejectCallerWithAuditReadAloneFromMutations() {
      User auditReader = new User();
      auditReader.setId(40L);
      auditReader.setStateId(1L);
      mockUserAuthentication(auditReader, "audit.read"); // No system.manage!

      assertThatThrownBy(() -> workflowService.assignIssue(1L, new AssignDataQualityIssueRequest(20L)))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Caller lacks required management authority 'system.manage'");

      assertThatThrownBy(() -> workflowService.startInvestigation(1L, new StartInvestigationRequest()))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Caller lacks required management authority 'system.manage'");

      assertThatThrownBy(() -> workflowService.resolveIssue(1L, new ResolveDataQualityIssueRequest("Notes")))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Caller lacks required management authority 'system.manage'");

      assertThatThrownBy(() -> workflowService.dismissIssue(1L, new DismissDataQualityIssueRequest("Dismiss")))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Caller lacks required management authority 'system.manage'");

      assertThatThrownBy(() -> workflowService.reopenIssue(1L, new ReopenDataQualityIssueRequest("Reopen")))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Caller lacks required management authority 'system.manage'");
    }

    @Test
    @DisplayName("Should allow caller with audit.read to view issue events")
    void shouldAllowCallerWithAuditReadToViewEvents() {
      User auditReader = new User();
      auditReader.setId(40L);
      auditReader.setStateId(1L);
      mockUserAuthentication(auditReader, "audit.read");

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(dataQualityIssueEventRepository.findByDataQualityIssueId(1L)).thenReturn(List.of());

      List<DataQualityIssueEventResponse> events = workflowService.getIssueEvents(1L);
      assertThat(events).isEmpty();
    }
  }

  @Nested
  @DisplayName("7. Event Audit History Retrieval (GET /{id}/events)")
  class EventAuditHistory {

    @Test
    @DisplayName("Should return sorted historical events for an authorized issue via repository fallback when jdbcTemplate is null")
    void shouldReturnOrderedEvents() {
      DataQualityWorkflowServiceImpl fallbackService = new DataQualityWorkflowServiceImpl(
          dataQualityIssueRepository,
          dataQualityIssueEventRepository,
          refDataQualityIssueStatusRepository,
          userRepository,
          traineeRepository,
          scopeValidator,
          issueMapper,
          eventMapper,
          null
      );
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      DataQualityIssueEvent event1 = new DataQualityIssueEvent();
      event1.setId(101L);
      event1.setChangedAt(LocalDateTime.now().minusHours(2));
      event1.setRemarks("Event 1");

      DataQualityIssueEvent event2 = new DataQualityIssueEvent();
      event2.setId(102L);
      event2.setChangedAt(LocalDateTime.now().minusHours(1));
      event2.setRemarks("Event 2");

      when(dataQualityIssueEventRepository.findByDataQualityIssueId(1L)).thenReturn(List.of(event2, event1));
      when(eventMapper.toResponse(any(DataQualityIssueEvent.class))).thenAnswer(inv -> {
        DataQualityIssueEvent e = inv.getArgument(0);
        return new DataQualityIssueEventResponse(
            e.getId(), 1L, 2L, "STATUS", "Status", 10L, e.getChangedAt(), e.getRemarks(), e.getCreatedAt()
        );
      });

      List<DataQualityIssueEventResponse> responses = fallbackService.getIssueEvents(1L);
      assertThat(responses).hasSize(2);
      // verify sorted by changedAt ascending
      assertThat(responses.get(0).id()).isEqualTo(101L);
      assertThat(responses.get(1).id()).isEqualTo(102L);
    }

    @Test
    @DisplayName("Should return empty list without executing repository fallback when JDBC query returns zero records")
    void shouldReturnEmptyListWithoutRepositoryFallbackWhenJdbcReturnsEmpty() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      // Simulate JDBC query returning zero records for an out-of-range page
      when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(Object[].class)))
          .thenReturn(List.of());

      List<DataQualityIssueEventResponse> responses = workflowService.getIssueEvents(1L, 10, 50);

      // Verify empty list is returned immediately
      assertThat(responses).isEmpty();

      // Verify unbounded findByDataQualityIssueId() is NEVER invoked
      verify(dataQualityIssueEventRepository, never()).findByDataQualityIssueId(anyLong());
      verify(dataQualityIssueEventRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("Should return bounded events via JDBC without executing unbounded findByDataQualityIssueId")
    void shouldReturnBoundedEventsViaJdbcWithoutRepositoryFallback() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      DataQualityIssueEvent event1 = new DataQualityIssueEvent();
      event1.setId(101L);
      event1.setChangedAt(LocalDateTime.now().minusHours(2));
      event1.setRemarks("Event 1");

      DataQualityIssueEvent event2 = new DataQualityIssueEvent();
      event2.setId(102L);
      event2.setChangedAt(LocalDateTime.now().minusHours(1));
      event2.setRemarks("Event 2");

      when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(Object[].class)))
          .thenReturn(List.of(101L, 102L));
      when(dataQualityIssueEventRepository.findAllById(List.of(101L, 102L)))
          .thenReturn(List.of(event1, event2));
      when(eventMapper.toResponse(any(DataQualityIssueEvent.class))).thenAnswer(inv -> {
        DataQualityIssueEvent e = inv.getArgument(0);
        return new DataQualityIssueEventResponse(
            e.getId(), 1L, 2L, "STATUS", "Status", 10L, e.getChangedAt(), e.getRemarks(), e.getCreatedAt()
        );
      });

      List<DataQualityIssueEventResponse> responses = workflowService.getIssueEvents(1L, 0, 50);
      assertThat(responses).hasSize(2);
      assertThat(responses.get(0).id()).isEqualTo(101L);
      assertThat(responses.get(1).id()).isEqualTo(102L);

      verify(dataQualityIssueEventRepository, never()).findByDataQualityIssueId(anyLong());
      verify(dataQualityIssueEventRepository).findAllById(List.of(101L, 102L));
    }

    @Test
    @DisplayName("Should validate pagination bounds on event audit retrieval")
    void shouldValidatePaginationBounds() {
      assertThatThrownBy(() -> workflowService.getIssueEvents(1L, -1, 50))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Page index must not be negative");

      assertThatThrownBy(() -> workflowService.getIssueEvents(1L, 0, 0))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Page size must be between 1 and 100");

      assertThatThrownBy(() -> workflowService.getIssueEvents(1L, 0, 101))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Page size must be between 1 and 100");
    }

    @Test
    @DisplayName("Should return bounded page of historical events via fallback when jdbcTemplate is null")
    void shouldReturnBoundedPageOfOrderedEvents() {
      DataQualityWorkflowServiceImpl fallbackService = new DataQualityWorkflowServiceImpl(
          dataQualityIssueRepository,
          dataQualityIssueEventRepository,
          refDataQualityIssueStatusRepository,
          userRepository,
          traineeRepository,
          scopeValidator,
          issueMapper,
          eventMapper,
          null
      );
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      DataQualityIssueEvent event1 = new DataQualityIssueEvent();
      event1.setId(101L);
      event1.setChangedAt(LocalDateTime.now().minusHours(2));
      event1.setRemarks("Event 1");

      DataQualityIssueEvent event2 = new DataQualityIssueEvent();
      event2.setId(102L);
      event2.setChangedAt(LocalDateTime.now().minusHours(1));
      event2.setRemarks("Event 2");

      when(dataQualityIssueEventRepository.findByDataQualityIssueId(1L)).thenReturn(List.of(event2, event1));
      when(eventMapper.toResponse(any(DataQualityIssueEvent.class))).thenAnswer(inv -> {
        DataQualityIssueEvent e = inv.getArgument(0);
        return new DataQualityIssueEventResponse(
            e.getId(), 1L, 2L, "STATUS", "Status", 10L, e.getChangedAt(), e.getRemarks(), e.getCreatedAt()
        );
      });

      List<DataQualityIssueEventResponse> page0 = fallbackService.getIssueEvents(1L, 0, 1);
      assertThat(page0).hasSize(1);
      assertThat(page0.get(0).id()).isEqualTo(101L);

      List<DataQualityIssueEventResponse> page1 = fallbackService.getIssueEvents(1L, 1, 1);
      assertThat(page1).hasSize(1);
      assertThat(page1.get(0).id()).isEqualTo(102L);
    }
  }

  @Nested
  @DisplayName("8. Assignee District & Role-Permission Eligibility Validation")
  class AssigneeDistrictAndPermissionTests {

    private DataQualityIssueResponse toMockIssueResponse(DataQualityIssue entity) {
      String statusCode = entity.getDataQualityIssueStatus() != null
          ? entity.getDataQualityIssueStatus().getStatusCode() : null;
      return new DataQualityIssueResponse(
          entity.getId(), 1L, "RULE", "Rule", 1L, "CAT", "Category",
          1L, "HIGH", "High", 2L, statusCode, statusCode,
          true, false, 1L, "AUTO", "Auto", entity.getEntityType(), entity.getEntityId(),
          entity.getIssueSummary(), entity.getDetectedAt(), entity.getAssignedUserId(),
          entity.getResolvedAt(), entity.getResolvedByUserId(), entity.getResolutionNotes(),
          entity.getCreatedAt(), entity.getUpdatedAt()
      );
    }

    @Test
    @DisplayName("State admin assigns District A issue to District A officer with system.manage -> accepted")
    void stateAdminAssignsDistrictIssueToMatchingDistrictOfficer() {
      User stateCaller = new User();
      stateCaller.setId(30L);
      stateCaller.setStateId(1L);
      mockUserAuthentication(stateCaller, "ROLE_state_admin", "system.manage");

      User districtAOfficer = new User();
      districtAOfficer.setId(31L);
      districtAOfficer.setStateId(1L);
      districtAOfficer.setDistrictId(10L);
      districtAOfficer.setUserStatusId(1L);

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(31L)).thenReturn(Optional.of(districtAOfficer));
      when(traineeRepository.findByUserId(31L)).thenReturn(Optional.empty());
      when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(31L)))
          .thenReturn(List.of("district_officer"));
      when(jdbcTemplate.queryForObject(contains("district_id = ?"), eq(Integer.class), eq(100L), eq(1L), eq(10L)))
          .thenReturn(1);
      when(refDataQualityIssueStatusRepository.findByStatusCode("ASSIGNED")).thenReturn(Optional.of(assignedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));
      when(issueMapper.toResponse(any(DataQualityIssue.class)))
          .thenAnswer(inv -> toMockIssueResponse(inv.getArgument(0)));

      DataQualityIssueResponse response = workflowService.assignIssue(1L, new AssignDataQualityIssueRequest(31L, "District A review"));
      assertThat(response.assignedUserId()).isEqualTo(31L);
      assertThat(response.dataQualityIssueStatusCode()).isEqualTo("ASSIGNED");
    }

    @Test
    @DisplayName("State admin assigns District A issue to District B officer in same state -> rejected")
    void stateAdminAssignsDistrictIssueToMismatchedDistrictOfficerRejected() {
      User stateCaller = new User();
      stateCaller.setId(30L);
      stateCaller.setStateId(1L);
      mockUserAuthentication(stateCaller, "ROLE_state_admin", "system.manage");

      User districtBOfficer = new User();
      districtBOfficer.setId(32L);
      districtBOfficer.setStateId(1L);
      districtBOfficer.setDistrictId(20L);
      districtBOfficer.setUserStatusId(1L);

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(32L)).thenReturn(Optional.of(districtBOfficer));
      when(traineeRepository.findByUserId(32L)).thenReturn(Optional.empty());
      when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(32L)))
          .thenReturn(List.of("district_officer"));
      when(jdbcTemplate.queryForObject(contains("district_id = ?"), eq(Integer.class), eq(100L), eq(1L), eq(20L)))
          .thenReturn(0);

      assertThatThrownBy(() -> workflowService.assignIssue(1L, new AssignDataQualityIssueRequest(32L)))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode")
          .isEqualTo("ASSIGNEE_SCOPE_MISMATCH");

      verify(dataQualityIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("State admin assigns issue to user without workflow-management authority (system.manage) -> rejected")
    void stateAdminAssignsIssueToUserWithoutSystemManageRejected() {
      User stateCaller = new User();
      stateCaller.setId(30L);
      stateCaller.setStateId(1L);
      mockUserAuthentication(stateCaller, "ROLE_state_admin", "system.manage");

      User analystUser = new User();
      analystUser.setId(33L);
      analystUser.setStateId(1L);
      analystUser.setDistrictId(10L);
      analystUser.setUserStatusId(1L);

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(33L)).thenReturn(Optional.of(analystUser));
      when(traineeRepository.findByUserId(33L)).thenReturn(Optional.empty());
      when(jdbcTemplate.queryForObject(contains("system.manage"), eq(Integer.class), eq(33L))).thenReturn(0);

      assertThatThrownBy(() -> workflowService.assignIssue(1L, new AssignDataQualityIssueRequest(33L)))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode")
          .isEqualTo("INVALID_ASSIGNEE");

      verify(dataQualityIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Valid provider and employer assignments continue to succeed")
    void validProviderAndEmployerAssignmentsSucceed() {
      User providerCaller = new User();
      providerCaller.setId(40L);
      providerCaller.setTrainingProviderId(300L);
      mockUserAuthentication(providerCaller, "ROLE_training_provider", "system.manage");

      User providerAssignee = new User();
      providerAssignee.setId(41L);
      providerAssignee.setTrainingProviderId(300L);
      providerAssignee.setUserStatusId(1L);

      DataQualityIssue traineeIssue = createTestIssue(1L, openStatus);
      traineeIssue.setEntityType("Trainee");
      traineeIssue.setEntityId(100L);

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(traineeIssue));
      when(userRepository.findById(41L)).thenReturn(Optional.of(providerAssignee));
      when(traineeRepository.findByUserId(41L)).thenReturn(Optional.empty());
      when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(41L)))
          .thenReturn(List.of("training_provider"));
      when(refDataQualityIssueStatusRepository.findByStatusCode("ASSIGNED")).thenReturn(Optional.of(assignedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));
      when(issueMapper.toResponse(any(DataQualityIssue.class)))
          .thenAnswer(inv -> toMockIssueResponse(inv.getArgument(0)));

      DataQualityIssueResponse prvResp = workflowService.assignIssue(1L, new AssignDataQualityIssueRequest(41L));
      assertThat(prvResp.assignedUserId()).isEqualTo(41L);

      User employerCaller = new User();
      employerCaller.setId(50L);
      employerCaller.setEmployerId(400L);
      mockUserAuthentication(employerCaller, "ROLE_employer", "system.manage");

      User employerAssignee = new User();
      employerAssignee.setId(51L);
      employerAssignee.setEmployerId(400L);
      employerAssignee.setUserStatusId(1L);

      DataQualityIssue placementIssue = createTestIssue(2L, openStatus);
      placementIssue.setEntityType("PlacementRecord");
      placementIssue.setEntityId(200L);

      when(dataQualityIssueRepository.findById(2L)).thenReturn(Optional.of(placementIssue));
      when(userRepository.findById(51L)).thenReturn(Optional.of(employerAssignee));
      when(traineeRepository.findByUserId(51L)).thenReturn(Optional.empty());
      when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(51L)))
          .thenReturn(List.of("employer"));

      DataQualityIssueResponse empResp = workflowService.assignIssue(2L, new AssignDataQualityIssueRequest(51L));
      assertThat(empResp.assignedUserId()).isEqualTo(51L);
    }

    @Test
    @DisplayName("Nationwide admin cannot bypass district eligibility while verified nationwide assignment is preserved")
    void nationwideAdminCannotBypassDistrictEligibilityWhileNationwideAssignmentPreserved() {
      mockAdminAuthentication();

      User districtBOfficer = new User();
      districtBOfficer.setId(32L);
      districtBOfficer.setStateId(1L);
      districtBOfficer.setDistrictId(20L);
      districtBOfficer.setUserStatusId(1L);

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(32L)).thenReturn(Optional.of(districtBOfficer));
      when(traineeRepository.findByUserId(32L)).thenReturn(Optional.empty());
      when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(32L)))
          .thenReturn(List.of("district_officer"));
      when(jdbcTemplate.queryForObject(contains("district_id = ?"), eq(Integer.class), eq(100L), eq(1L), eq(20L)))
          .thenReturn(0);

      assertThatThrownBy(() -> workflowService.assignIssue(1L, new AssignDataQualityIssueRequest(32L)))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode")
          .isEqualTo("ASSIGNEE_SCOPE_MISMATCH");

      User nationwideAssignee = new User();
      nationwideAssignee.setId(60L);
      nationwideAssignee.setUserStatusId(1L);
      when(userRepository.findById(60L)).thenReturn(Optional.of(nationwideAssignee));
      when(traineeRepository.findByUserId(60L)).thenReturn(Optional.empty());
      when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(60L)))
          .thenReturn(List.of("super_admin"));
      when(refDataQualityIssueStatusRepository.findByStatusCode("ASSIGNED")).thenReturn(Optional.of(assignedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));
      when(issueMapper.toResponse(any(DataQualityIssue.class)))
          .thenAnswer(inv -> toMockIssueResponse(inv.getArgument(0)));

      DataQualityIssueResponse nwResp = workflowService.assignIssue(1L, new AssignDataQualityIssueRequest(60L));
      assertThat(nwResp.assignedUserId()).isEqualTo(60L);
    }
  }
}
