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
import in.gov.sih.sih26135.entity.Trainee;
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

import java.time.LocalDateTime;
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
  }

  private DataQualityIssue createTestIssue(Long id, RefDataQualityIssueStatus status) {
    DataQualityIssue issue = new DataQualityIssue();
    issue.setId(id);
    issue.setDataQualityIssueStatus(status);
    issue.setEntityType("Trainee");
    issue.setEntityId(100L);
    issue.setIssueSummary("Test anomaly detected");
    issue.setDetectedAt(LocalDateTime.now().minusDays(1));
    return issue;
  }

  private void mockAdminAuthentication() {
    when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(adminCaller.getId());
    when(scopeValidator.resolveAuthenticatedUser()).thenReturn(Optional.of(adminCaller));
    when(scopeValidator.isNationwideAdmin()).thenReturn(true);
    when(scopeValidator.isTraineeCaller()).thenReturn(false);
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

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L, "Reassigned to secondary reviewer");
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

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L, "Assigning reopened issue");
      workflowService.assignIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(assignedStatus);
      assertThat(issue.getAssignedUserId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Should reject assignment when issue is IN_PROGRESS with 409 Conflict")
    void shouldRejectAssignmentWhenInProgress() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));
      when(traineeRepository.findByUserId(20L)).thenReturn(Optional.empty());

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L, "Invalid reassign");
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from IN_PROGRESS to ASSIGNED")
          .satisfies(ex -> assertThat(((InvalidDataQualityTransitionException) ex).getErrorCode())
              .isEqualTo(OperationalErrorCodes.INVALID_DQ_TRANSITION));
    }

    @Test
    @DisplayName("Should reject assignment when issue is in terminal state (RESOLVED / DISMISSED) with 409 Conflict")
    void shouldRejectAssignmentWhenTerminal() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, resolvedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(userRepository.findById(20L)).thenReturn(Optional.of(assigneeUser));
      when(traineeRepository.findByUserId(20L)).thenReturn(Optional.empty());

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L, "Invalid assign");
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from RESOLVED to ASSIGNED");
    }

    @Test
    @DisplayName("Should reject assignment when assignee is a Trainee with 400 Bad Request")
    void shouldRejectAssignmentToTrainee() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(traineeRepository.findByUserId(20L)).thenReturn(Optional.of(new Trainee()));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L, "Assigning to trainee");
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Trainees cannot be assigned data quality issues");
    }

    @Test
    @DisplayName("Should reject assignment when assignee user does not exist with 404 Not Found")
    void shouldRejectAssignmentWhenUserNotFound() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(traineeRepository.findByUserId(999L)).thenReturn(Optional.empty());
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(999L, "Assigning to ghost");
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should reject assignment when assignee user is deleted with 400 Bad Request")
    void shouldRejectAssignmentWhenAssigneeIsDeleted() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, openStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(traineeRepository.findByUserId(20L)).thenReturn(Optional.empty());

      User deletedUser = new User();
      deletedUser.setId(20L);
      deletedUser.setDeletedAt(LocalDateTime.now().minusDays(2));
      when(userRepository.findById(20L)).thenReturn(Optional.of(deletedUser));

      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(20L, "Assigning to deleted user");
      assertThatThrownBy(() -> workflowService.assignIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Assignee user is deleted or inactive");
    }
  }

  @Nested
  @DisplayName("2. Investigation Transition (ASSIGNED -> IN_PROGRESS)")
  class InvestigationTransitions {

    @Test
    @DisplayName("Should successfully start investigation from ASSIGNED state")
    void shouldStartInvestigationSuccessfully() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, assignedStatus);
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
    @DisplayName("Should successfully resolve an IN_PROGRESS issue with server attribution and event")
    void shouldResolveInProgressIssueSuccessfully() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("RESOLVED")).thenReturn(Optional.of(resolvedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Discrepancy remediated in enrollment register");
      workflowService.resolveIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(resolvedStatus);
      assertThat(issue.getResolvedByUserId()).isEqualTo(adminCaller.getId());
      assertThat(issue.getResolvedAt()).isNotNull();
      assertThat(issue.getResolutionNotes()).isEqualTo("Discrepancy remediated in enrollment register");

      ArgumentCaptor<DataQualityIssueEvent> eventCaptor = ArgumentCaptor.forClass(DataQualityIssueEvent.class);
      verify(dataQualityIssueEventRepository).save(eventCaptor.capture());
      assertThat(eventCaptor.getValue().getDataQualityIssueStatus()).isEqualTo(resolvedStatus);
      assertThat(eventCaptor.getValue().getRemarks()).isEqualTo("Discrepancy remediated in enrollment register");
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

    @Test
    @DisplayName("Should reject resolution when detected_at is in the future relative to server time")
    void shouldEnforceTemporalInvariant() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      issue.setDetectedAt(LocalDateTime.now().plusHours(2)); // future detection date
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Future detected issue");
      assertThatThrownBy(() -> workflowService.resolveIssue(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Resolved timestamp cannot be before detected timestamp");
    }
  }

  @Nested
  @DisplayName("4. Dismissal Transition (OPEN / IN_PROGRESS -> DISMISSED)")
  class DismissalTransitions {

    @Test
    @DisplayName("Should successfully dismiss an OPEN issue with remarks")
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
    @DisplayName("Should successfully dismiss an IN_PROGRESS issue with remarks")
    void shouldDismissInProgressIssueSuccessfully() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, inProgressStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("DISMISSED")).thenReturn(Optional.of(dismissedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Investigation concluded as non-actionable");
      workflowService.dismissIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(dismissedStatus);
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
      issue.setResolvedAt(LocalDateTime.now().minusHours(5));
      issue.setResolvedByUserId(99L);
      issue.setResolutionNotes("Old resolution note");

      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));
      when(refDataQualityIssueStatusRepository.findByStatusCode("REOPENED")).thenReturn(Optional.of(reopenedStatus));
      when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> inv.getArgument(0));

      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest("Anomaly recurred after external feed synchronization");
      workflowService.reopenIssue(1L, request);

      assertThat(issue.getDataQualityIssueStatus()).isEqualTo(reopenedStatus);
      assertThat(issue.getResolvedAt()).isNull();
      assertThat(issue.getResolvedByUserId()).isNull();
      assertThat(issue.getResolutionNotes()).isNull();

      ArgumentCaptor<DataQualityIssueEvent> eventCaptor = ArgumentCaptor.forClass(DataQualityIssueEvent.class);
      verify(dataQualityIssueEventRepository).save(eventCaptor.capture());
      assertThat(eventCaptor.getValue().getDataQualityIssueStatus()).isEqualTo(reopenedStatus);
      assertThat(eventCaptor.getValue().getRemarks()).isEqualTo("Anomaly recurred after external feed synchronization");
    }

    @Test
    @DisplayName("Should reject reopening a DISMISSED issue with 409 Conflict")
    void shouldRejectReopeningDismissedIssue() {
      mockAdminAuthentication();
      DataQualityIssue issue = createTestIssue(1L, dismissedStatus);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest("Cannot reopen dismissed issue");
      assertThatThrownBy(() -> workflowService.reopenIssue(1L, request))
          .isInstanceOf(InvalidDataQualityTransitionException.class)
          .hasMessageContaining("Cannot transition data quality issue from DISMISSED to REOPENED");
    }

    @Test
    @DisplayName("Should reject reopening an OPEN or ASSIGNED issue with 409 Conflict")
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
  }

  @Nested
  @DisplayName("6. Security, Tenancy & Authorization Guardrails")
  class SecurityAndTenancyGuardrails {

    @Test
    @DisplayName("Should reject unauthenticated callers with AccessDeniedException")
    void shouldRejectUnauthenticatedCaller() {
      when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(null);

      assertThatThrownBy(() -> workflowService.startInvestigation(1L, new StartInvestigationRequest()))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Authenticated user identity is required");
    }

    @Test
    @DisplayName("Should reject Trainee callers from managing data quality issues")
    void shouldRejectTraineeCaller() {
      when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(50L);
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

      when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(11L);
      when(scopeValidator.isTraineeCaller()).thenReturn(false);
      when(scopeValidator.resolveAuthenticatedUser()).thenReturn(Optional.of(stateAdmin));
      when(scopeValidator.isNationwideAdmin()).thenReturn(false);

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

      when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(12L);
      when(scopeValidator.isTraineeCaller()).thenReturn(false);
      when(scopeValidator.resolveAuthenticatedUser()).thenReturn(Optional.of(employerUser));
      when(scopeValidator.isNationwideAdmin()).thenReturn(false);

      DataQualityIssue issue = createTestIssue(1L, openStatus);
      issue.setEntityType("Trainee");
      issue.setEntityId(100L);
      when(dataQualityIssueRepository.findById(1L)).thenReturn(Optional.of(issue));

      assertThatThrownBy(() -> workflowService.dismissIssue(1L, new DismissDataQualityIssueRequest("Employer dismissing trainee")))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Employers are not authorized to manage trainee data quality issues");
    }
  }

  @Nested
  @DisplayName("7. Event Audit History Retrieval (GET /{id}/events)")
  class EventAuditHistory {

    @Test
    @DisplayName("Should return sorted historical events for an authorized issue")
    void shouldReturnOrderedEvents() {
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

      List<DataQualityIssueEventResponse> responses = workflowService.getIssueEvents(1L);
      assertThat(responses).hasSize(2);
      // verify sorted by changedAt ascending
      assertThat(responses.get(0).id()).isEqualTo(101L);
      assertThat(responses.get(1).id()).isEqualTo(102L);
    }
  }
}
