package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.RefDataQualityCategory;
import in.gov.sih.sih26135.entity.RefDataQualityDetectionSource;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.entity.RefDataQualitySeverity;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.mapper.DataQualityIssueMapper;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.DataQualityRuleRepository;
import in.gov.sih.sih26135.repository.RefDataQualityCategoryRepository;
import in.gov.sih.sih26135.repository.RefDataQualityDetectionSourceRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.repository.RefDataQualitySeverityRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.DataQualityWorkflowService;
import in.gov.sih.sih26135.service.impl.DataQualityIssueServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Phase 28.4 - DataQualityIssueService Unit Security & Creation Lifecycle Tests")
class DataQualityIssueServiceSecurityTest {

  @Mock
  private DataQualityIssueRepository dataQualityIssueRepository;

  @Mock
  private DataQualityRuleRepository dataQualityRuleRepository;

  @Mock
  private RefDataQualityCategoryRepository refDataQualityCategoryRepository;

  @Mock
  private RefDataQualitySeverityRepository refDataQualitySeverityRepository;

  @Mock
  private RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository;

  @Mock
  private RefDataQualityDetectionSourceRepository refDataQualityDetectionSourceRepository;

  @Mock
  private DataQualityIssueMapper mapper;

  @Mock
  private WorkflowSecurityScopeValidator scopeValidator;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private DataQualityWorkflowService dataQualityWorkflowService;

  private DataQualityIssueServiceImpl issueService;

  private RefDataQualityIssueStatus openStatus;

  @BeforeEach
  void setUp() {
    issueService = new DataQualityIssueServiceImpl(
        dataQualityIssueRepository,
        dataQualityRuleRepository,
        refDataQualityCategoryRepository,
        refDataQualitySeverityRepository,
        refDataQualityIssueStatusRepository,
        refDataQualityDetectionSourceRepository,
        mapper,
        scopeValidator,
        jdbcTemplate,
        dataQualityWorkflowService
    );

    openStatus = new RefDataQualityIssueStatus();
    openStatus.setId(1L);
    openStatus.setStatusCode("OPEN");
    openStatus.setIsTerminalFlag(false);

    when(refDataQualityCategoryRepository.findById(1L)).thenReturn(Optional.of(new RefDataQualityCategory()));
    when(refDataQualitySeverityRepository.findById(1L)).thenReturn(Optional.of(new RefDataQualitySeverity()));
    when(refDataQualityDetectionSourceRepository.findById(1L)).thenReturn(Optional.of(new RefDataQualityDetectionSource()));
    when(refDataQualityIssueStatusRepository.findByStatusCode("OPEN")).thenReturn(Optional.of(openStatus));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void mockUserAuth(User user, boolean nationwideAdmin, String... authorities) {
    when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(user.getId());
    when(scopeValidator.resolveAuthenticatedUser()).thenReturn(Optional.of(user));
    when(scopeValidator.isNationwideAdmin()).thenReturn(nationwideAdmin);
    when(scopeValidator.isTraineeCaller()).thenReturn(false);

    List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
        .map(SimpleGrantedAuthority::new)
        .toList();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, granted)
    );
  }

  @ParameterizedTest
  @CsvSource({
      "2, ASSIGNED",
      "3, IN_PROGRESS",
      "4, RESOLVED",
      "5, DISMISSED",
      "6, REOPENED"
  })
  @DisplayName("reportIssue rejects any non-OPEN initial status at service boundary")
  void shouldRejectNonOpenStatusOnCreate(Long statusId, String statusCode) {
    User admin = new User();
    admin.setId(1L);
    mockUserAuth(admin, true, "ROLE_super_admin", "system.manage");

    RefDataQualityIssueStatus nonOpen = new RefDataQualityIssueStatus();
    nonOpen.setId(statusId);
    nonOpen.setStatusCode(statusCode);
    when(refDataQualityIssueStatusRepository.findById(statusId)).thenReturn(Optional.of(nonOpen));

    CreateDataQualityIssueRequest req = new CreateDataQualityIssueRequest(1L, 1L, 1L, "Trainee", "Bypass attempt");
    req.setEntityId(100L);
    req.setDataQualityIssueStatusId(statusId);

    assertThatThrownBy(() -> issueService.reportIssue(req))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode()).isEqualTo("LIFECYCLE_BYPASS_PROHIBITED"));
  }

  @Test
  @DisplayName("reportIssue allows explicit OPEN status with null assignee")
  void shouldAllowExplicitOpenStatusOnCreate() {
    User stateAdmin = new User();
    stateAdmin.setId(10L);
    stateAdmin.setStateId(1L);
    mockUserAuth(stateAdmin, false, "ROLE_state_admin", "system.manage");

    when(refDataQualityIssueStatusRepository.findById(1L)).thenReturn(Optional.of(openStatus));
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(100L), eq(1L))).thenReturn(1);
    when(dataQualityIssueRepository.save(any(DataQualityIssue.class))).thenAnswer(inv -> {
      DataQualityIssue saved = inv.getArgument(0);
      saved.setId(55L);
      return saved;
    });
    when(mapper.toResponse(any(DataQualityIssue.class))).thenReturn(org.mockito.Mockito.mock(DataQualityIssueResponse.class));

    CreateDataQualityIssueRequest req = new CreateDataQualityIssueRequest(1L, 1L, 1L, "Trainee", "Valid OPEN issue");
    req.setEntityId(100L);
    req.setDataQualityIssueStatusId(1L);

    DataQualityIssueResponse resp = issueService.reportIssue(req);
    assertThat(resp).isNotNull();
  }

  @Test
  @DisplayName("reportIssue rejects client-selected assignedUserId")
  void shouldRejectAssignedUserIdOnCreate() {
    User admin = new User();
    admin.setId(1L);
    mockUserAuth(admin, true, "ROLE_super_admin", "system.manage");

    CreateDataQualityIssueRequest req = new CreateDataQualityIssueRequest(1L, 1L, 1L, "Trainee", "Pre-assigned attempt");
    req.setEntityId(100L);
    req.setAssignedUserId(25L);

    assertThatThrownBy(() -> issueService.reportIssue(req))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode()).isEqualTo("ASSIGNMENT_BYPASS_PROHIBITED"));
  }

  @Test
  @DisplayName("District-scoped officer enforces both stateId and districtId on issue access")
  void shouldEnforceDistrictScopeForDistrictOfficer() {
    User districtOfficer = new User();
    districtOfficer.setId(15L);
    districtOfficer.setStateId(1L);
    districtOfficer.setDistrictId(5L);
    mockUserAuth(districtOfficer, false, "ROLE_district_officer", "audit.read");

    DataQualityIssue issue = new DataQualityIssue();
    issue.setId(99L);
    issue.setEntityType("Trainee");
    issue.setEntityId(200L);
    issue.setDetectedAt(LocalDateTime.now());
    when(dataQualityIssueRepository.findById(99L)).thenReturn(Optional.of(issue));

    // Trainee belongs to same state (1) but different district -> 0 rows
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(200L), eq(1L), eq(5L))).thenReturn(0);

    assertThatThrownBy(() -> issueService.getIssueById(99L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("does not belong to caller state scope");
  }

  @Test
  @DisplayName("Ambiguous caller scope fails closed on reads, updates, and creation")
  void shouldFailClosedForAmbiguousCallerScope() {
    User ambiguousUser = new User();
    ambiguousUser.setId(20L);
    ambiguousUser.setStateId(1L);
    ambiguousUser.setTrainingProviderId(2L);
    mockUserAuth(ambiguousUser, false, "ROLE_state_admin", "audit.read", "system.manage");

    assertThatThrownBy(() -> issueService.getIssueById(1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> issueService.getIssuesByStatusId(1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> issueService.updateIssue(1L, new UpdateDataQualityIssueRequest()))
        .isInstanceOf(AccessDeniedException.class);

    CreateDataQualityIssueRequest req = new CreateDataQualityIssueRequest(1L, 1L, 1L, "Trainee", "Test");
    req.setEntityId(100L);
    assertThatThrownBy(() -> issueService.reportIssue(req))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Legacy assignIssue and resolveIssue delegate cleanly to DataQualityWorkflowService")
  void shouldDelegateLegacyMethodsToWorkflowService() {
    issueService.assignIssue(10L, 20L);
    verify(dataQualityWorkflowService).assignIssue(eq(10L), eq(new AssignDataQualityIssueRequest(20L)));

    ResolveDataQualityIssueRequest resolveReq = new ResolveDataQualityIssueRequest("Fixed");
    issueService.resolveIssue(10L, resolveReq);
    verify(dataQualityWorkflowService).resolveIssue(eq(10L), eq(resolveReq));
  }
}
