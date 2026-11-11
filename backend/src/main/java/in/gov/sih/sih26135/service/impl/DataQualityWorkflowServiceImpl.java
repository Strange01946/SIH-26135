package in.gov.sih.sih26135.service.impl;

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
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityIssueEventMapper;
import in.gov.sih.sih26135.mapper.DataQualityIssueMapper;
import in.gov.sih.sih26135.repository.DataQualityIssueEventRepository;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.DataQualityWorkflowService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of DataQualityWorkflowService enforcing auditable, atomic
 * lifecycle transitions, row-level concurrency locks, and strict tenant scope guardrails.
 */
@Service
@Transactional
public class DataQualityWorkflowServiceImpl implements DataQualityWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(DataQualityWorkflowServiceImpl.class);

  public static final String STATUS_OPEN = "OPEN";
  public static final String STATUS_ASSIGNED = "ASSIGNED";
  public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
  public static final String STATUS_RESOLVED = "RESOLVED";
  public static final String STATUS_DISMISSED = "DISMISSED";
  public static final String STATUS_REOPENED = "REOPENED";

  private static final Set<String> NATIONWIDE_ADMIN_ROLE_CODES = Set.of(
      "super_admin",
      "government_admin"
  );

  private static final Set<String> RESTRICTED_ASSIGNEE_ROLE_CODES = Set.of(
      "training_provider",
      "employer",
      "trainee",
      "district_officer",
      "government_officer",
      "state_admin"
  );

  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final DataQualityIssueEventRepository dataQualityIssueEventRepository;
  private final RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository;
  private final UserRepository userRepository;
  private final TraineeRepository traineeRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final DataQualityIssueMapper issueMapper;
  private final DataQualityIssueEventMapper eventMapper;
  private final JdbcTemplate jdbcTemplate;

  public DataQualityWorkflowServiceImpl(
      DataQualityIssueRepository dataQualityIssueRepository,
      DataQualityIssueEventRepository dataQualityIssueEventRepository,
      RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository,
      UserRepository userRepository,
      TraineeRepository traineeRepository,
      WorkflowSecurityScopeValidator scopeValidator,
      DataQualityIssueMapper issueMapper,
      DataQualityIssueEventMapper eventMapper,
      JdbcTemplate jdbcTemplate) {
    this.dataQualityIssueRepository = Objects.requireNonNull(dataQualityIssueRepository, "dataQualityIssueRepository is required");
    this.dataQualityIssueEventRepository = Objects.requireNonNull(dataQualityIssueEventRepository, "dataQualityIssueEventRepository is required");
    this.refDataQualityIssueStatusRepository = Objects.requireNonNull(refDataQualityIssueStatusRepository, "refDataQualityIssueStatusRepository is required");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository is required");
    this.traineeRepository = Objects.requireNonNull(traineeRepository, "traineeRepository is required");
    this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator is required");
    this.issueMapper = Objects.requireNonNull(issueMapper, "issueMapper is required");
    this.eventMapper = Objects.requireNonNull(eventMapper, "eventMapper is required");
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
  }

  @Override
  public DataQualityIssueResponse assignIssue(Long issueId, AssignDataQualityIssueRequest request) {
    validateIssueId(issueId);
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    Long assignedUserId = request.getAssignedUserId();
    if (assignedUserId == null || assignedUserId <= 0) {
      throw new BadRequestException("assignedUserId must be a positive integer", "INVALID_ASSIGNEE");
    }

    DataQualityIssueScopeSupport.CallerScope callerScope = resolveAndValidateManagerScope();
    User caller = callerScope.caller();
    lockIssueRow(issueId);
    DataQualityIssue issue = findIssue(issueId);
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);

    // Validate Assignee against the issue entity and tenant ownership
    validateAssigneeEligibility(assignedUserId, caller, issue);

    // Validate State Transition: allowed from OPEN, ASSIGNED, or REOPENED
    String currentStatusCode = getStatusCode(issue);
    if (STATUS_IN_PROGRESS.equalsIgnoreCase(currentStatusCode)) {
      throw new InvalidDataQualityTransitionException(currentStatusCode, STATUS_ASSIGNED);
    }
    if (STATUS_RESOLVED.equalsIgnoreCase(currentStatusCode) || STATUS_DISMISSED.equalsIgnoreCase(currentStatusCode)) {
      throw new InvalidDataQualityTransitionException(currentStatusCode, STATUS_ASSIGNED);
    }

    RefDataQualityIssueStatus assignedStatus = getRefStatus(STATUS_ASSIGNED);
    issue.setDataQualityIssueStatus(assignedStatus);
    issue.setAssignedUserId(assignedUserId);
    issue.setUpdatedAt(LocalDateTime.now());

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);

    // Append event atomically
    LocalDateTime now = LocalDateTime.now();
    String remarks = request.getRemarks();
    if (remarks == null || remarks.isBlank()) {
      remarks = "Assigned to user " + assignedUserId;
    } else {
      remarks = remarks.trim();
    }
    createAndAppendEvent(saved, assignedStatus, caller.getId(), now, remarks);

    log.info("Data quality issue {} successfully assigned to user {} by user {}", issueId, assignedUserId, caller.getId());
    return issueMapper.toResponse(saved);
  }

  @Override
  public DataQualityIssueResponse startInvestigation(Long issueId, StartInvestigationRequest request) {
    validateIssueId(issueId);
    DataQualityIssueScopeSupport.CallerScope callerScope = resolveAndValidateManagerScope();
    User caller = callerScope.caller();
    lockIssueRow(issueId);
    DataQualityIssue issue = findIssue(issueId);
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);

    // Validate State Transition: allowed from ASSIGNED or REOPENED
    String currentStatusCode = getStatusCode(issue);
    if (!STATUS_ASSIGNED.equalsIgnoreCase(currentStatusCode) && !STATUS_REOPENED.equalsIgnoreCase(currentStatusCode)) {
      throw new InvalidDataQualityTransitionException(currentStatusCode, STATUS_IN_PROGRESS);
    }

    // Require the authenticated actor to be the assigned investigator or an authorized administrative supervisor
    Long assignedUserId = issue.getAssignedUserId();
    if (assignedUserId == null || (!assignedUserId.equals(caller.getId()) && !isAdministrativeSupervisor(caller))) {
      throw new AccessDeniedException("Only the assigned investigator or an authorized administrative supervisor can start investigation");
    }

    RefDataQualityIssueStatus inProgressStatus = getRefStatus(STATUS_IN_PROGRESS);
    issue.setDataQualityIssueStatus(inProgressStatus);
    issue.setUpdatedAt(LocalDateTime.now());

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);

    // Append event atomically
    LocalDateTime now = LocalDateTime.now();
    String remarks = (request != null && request.getRemarks() != null && !request.getRemarks().isBlank())
        ? request.getRemarks().trim()
        : "Investigation started";
    createAndAppendEvent(saved, inProgressStatus, caller.getId(), now, remarks);

    log.info("Data quality issue {} transitioned to IN_PROGRESS by user {}", issueId, caller.getId());
    return issueMapper.toResponse(saved);
  }

  @Override
  public DataQualityIssueResponse resolveIssue(Long issueId, ResolveDataQualityIssueRequest request) {
    validateIssueId(issueId);
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getResolutionNotes() == null || request.getResolutionNotes().isBlank()) {
      throw new BadRequestException("resolutionNotes cannot be blank", "VALIDATION_FAILED");
    }
    if (request.getResolutionNotes().length() > 500) {
      throw new BadRequestException("resolutionNotes cannot exceed 500 characters", "VALIDATION_FAILED");
    }

    DataQualityIssueScopeSupport.CallerScope callerScope = resolveAndValidateManagerScope();
    User caller = callerScope.caller();
    lockIssueRow(issueId);
    DataQualityIssue issue = findIssue(issueId);
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);

    // Validate State Transition: allowed ONLY from IN_PROGRESS
    String currentStatusCode = getStatusCode(issue);
    if (!STATUS_IN_PROGRESS.equalsIgnoreCase(currentStatusCode)) {
      throw new InvalidDataQualityTransitionException(currentStatusCode, STATUS_RESOLVED);
    }

    // Require the authenticated actor to be the assigned investigator or an authorized administrative supervisor
    Long assignedUserId = issue.getAssignedUserId();
    if (assignedUserId != null && !assignedUserId.equals(caller.getId()) && !isAdministrativeSupervisor(caller)) {
      throw new AccessDeniedException("Only the assigned investigator or an authorized administrative supervisor can resolve this issue");
    }

    // Server-derived attribution
    LocalDateTime resolvedAt = LocalDateTime.now();
    Long resolvedByUserId = caller.getId();

    // Enforce temporal invariant: resolved_at >= detected_at
    if (issue.getDetectedAt() != null && resolvedAt.isBefore(issue.getDetectedAt())) {
      throw new BadRequestException("Resolved timestamp cannot be before detected timestamp", "RESOLVED_DATE_BEFORE_DETECTED");
    }

    RefDataQualityIssueStatus resolvedStatus = getRefStatus(STATUS_RESOLVED);
    issue.setDataQualityIssueStatus(resolvedStatus);
    issue.setResolvedAt(resolvedAt);
    issue.setResolvedByUserId(resolvedByUserId);
    issue.setResolutionNotes(request.getResolutionNotes().trim());
    issue.setUpdatedAt(resolvedAt);

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);

    // Append event atomically
    createAndAppendEvent(saved, resolvedStatus, resolvedByUserId, resolvedAt, request.getResolutionNotes().trim());

    log.info("Data quality issue {} resolved successfully by user {}", issueId, resolvedByUserId);
    return issueMapper.toResponse(saved);
  }

  @Override
  public DataQualityIssueResponse dismissIssue(Long issueId, DismissDataQualityIssueRequest request) {
    validateIssueId(issueId);
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getRemarks() == null || request.getRemarks().isBlank()) {
      throw new BadRequestException("remarks cannot be blank", "VALIDATION_FAILED");
    }
    if (request.getRemarks().length() > 500) {
      throw new BadRequestException("remarks cannot exceed 500 characters", "VALIDATION_FAILED");
    }

    DataQualityIssueScopeSupport.CallerScope callerScope = resolveAndValidateManagerScope();
    User caller = callerScope.caller();
    lockIssueRow(issueId);
    DataQualityIssue issue = findIssue(issueId);
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);

    // Validate State Transition: allowed ONLY from OPEN or IN_PROGRESS
    // Explicitly reject ASSIGNED -> DISMISSED, REOPENED -> DISMISSED, RESOLVED -> DISMISSED, DISMISSED -> DISMISSED
    String currentStatusCode = getStatusCode(issue);
    if (!STATUS_OPEN.equalsIgnoreCase(currentStatusCode) && !STATUS_IN_PROGRESS.equalsIgnoreCase(currentStatusCode)) {
      throw new InvalidDataQualityTransitionException(currentStatusCode, STATUS_DISMISSED);
    }

    // If dismissing an in-progress issue, require the actor to be the assigned investigator or an administrative supervisor
    if (STATUS_IN_PROGRESS.equalsIgnoreCase(currentStatusCode)) {
      Long assignedUserId = issue.getAssignedUserId();
      if (assignedUserId != null && !assignedUserId.equals(caller.getId()) && !isAdministrativeSupervisor(caller)) {
        throw new AccessDeniedException("Only the assigned investigator or an authorized administrative supervisor can dismiss an in-progress issue");
      }
    }

    LocalDateTime now = LocalDateTime.now();
    RefDataQualityIssueStatus dismissedStatus = getRefStatus(STATUS_DISMISSED);
    issue.setDataQualityIssueStatus(dismissedStatus);
    issue.setResolvedAt(now);
    issue.setResolvedByUserId(caller.getId());
    issue.setResolutionNotes(request.getRemarks().trim());
    issue.setUpdatedAt(now);

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);

    // Append event atomically
    createAndAppendEvent(saved, dismissedStatus, caller.getId(), now, request.getRemarks().trim());

    log.info("Data quality issue {} dismissed by user {}", issueId, caller.getId());
    return issueMapper.toResponse(saved);
  }

  @Override
  public DataQualityIssueResponse reopenIssue(Long issueId, ReopenDataQualityIssueRequest request) {
    validateIssueId(issueId);
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getReopeningReason() == null || request.getReopeningReason().isBlank()) {
      throw new BadRequestException("reopeningReason cannot be blank", "VALIDATION_FAILED");
    }
    if (request.getReopeningReason().length() > 500) {
      throw new BadRequestException("reopeningReason cannot exceed 500 characters", "VALIDATION_FAILED");
    }

    DataQualityIssueScopeSupport.CallerScope callerScope = resolveAndValidateManagerScope();
    User caller = callerScope.caller();
    lockIssueRow(issueId);
    DataQualityIssue issue = findIssue(issueId);
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);

    // Validate State Transition: allowed ONLY from RESOLVED
    String currentStatusCode = getStatusCode(issue);
    if (!STATUS_RESOLVED.equalsIgnoreCase(currentStatusCode)) {
      throw new InvalidDataQualityTransitionException(currentStatusCode, STATUS_REOPENED);
    }

    LocalDateTime now = LocalDateTime.now();
    RefDataQualityIssueStatus reopenedStatus = getRefStatus(STATUS_REOPENED);
    issue.setDataQualityIssueStatus(reopenedStatus);
    // Clear current resolution attribution from issue table; historical event log preserves past resolution
    issue.setResolvedAt(null);
    issue.setResolvedByUserId(null);
    issue.setResolutionNotes(null);
    issue.setUpdatedAt(now);

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);

    // Append event atomically
    createAndAppendEvent(saved, reopenedStatus, caller.getId(), now, request.getReopeningReason().trim());

    log.info("Data quality issue {} reopened by user {}", issueId, caller.getId());
    return issueMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataQualityIssueEventResponse> getIssueEvents(Long issueId) {
    validateIssueId(issueId);
    DataQualityIssueScopeSupport.CallerScope callerScope = resolveAndValidateReaderScope();
    DataQualityIssue issue = findIssue(issueId);
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);

    List<DataQualityIssueEvent> events = dataQualityIssueEventRepository.findByDataQualityIssueId(issueId);
    return events.stream()
        .sorted(Comparator.comparing(DataQualityIssueEvent::getChangedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DataQualityIssueEvent::getId, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(eventMapper::toResponse)
        .toList();
  }

  private void validateIssueId(Long issueId) {
    if (issueId == null || issueId <= 0) {
      throw new BadRequestException("Issue ID must be a positive integer", "INVALID_ISSUE_ID");
    }
  }

  private DataQualityIssue findIssue(Long issueId) {
    return dataQualityIssueRepository.findById(issueId)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));
  }

  private RefDataQualityIssueStatus getRefStatus(String statusCode) {
    return refDataQualityIssueStatusRepository.findByStatusCode(statusCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode=" + statusCode));
  }

  private String getStatusCode(DataQualityIssue issue) {
    if (issue.getDataQualityIssueStatus() == null || issue.getDataQualityIssueStatus().getStatusCode() == null) {
      return STATUS_OPEN;
    }
    return issue.getDataQualityIssueStatus().getStatusCode().toUpperCase();
  }

  private void lockIssueRow(Long issueId) {
    try {
      jdbcTemplate.queryForObject(
          "SELECT data_quality_issue_id FROM data_quality_issues WHERE data_quality_issue_id = ? FOR UPDATE",
          Long.class, issueId);
    } catch (EmptyResultDataAccessException e) {
      throw new ResourceNotFoundException("DataQualityIssue", "id");
    }
  }

  private DataQualityIssueScopeSupport.CallerScope resolveAndValidateManagerScope() {
    return DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, true);
  }

  private DataQualityIssueScopeSupport.CallerScope resolveAndValidateReaderScope() {
    return DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
  }

  private boolean isAdministrativeSupervisor(User caller) {
    if (scopeValidator.isNationwideAdmin()) {
      return true;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    Set<String> authorities = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
    if (authorities.contains("ROLE_super_admin") || authorities.contains("ROLE_government_admin")) {
      return true;
    }
    if (authorities.contains("ROLE_state_admin") && caller.getStateId() != null) {
      return true;
    }
    return false;
  }

  private void validateAssigneeEligibility(Long assignedUserId, User caller, DataQualityIssue issue) {
    if (assignedUserId == null || assignedUserId <= 0) {
      throw new BadRequestException("assignedUserId must be a positive integer", "INVALID_ASSIGNEE");
    }

    User assignee = userRepository.findById(assignedUserId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "assignedUserId"));

    if (assignee.getDeletedAt() != null || (assignee.getUserStatusId() != null && assignee.getUserStatusId() != 1)) {
      throw new BadRequestException("Assignee user is deleted or inactive", "INVALID_ASSIGNEE");
    }

    if (isUserTrainee(assignee)) {
      throw new BadRequestException("Trainees cannot be assigned data quality issues", "INVALID_ASSIGNEE");
    }

    Set<String> assigneeRoles = loadActiveRoleCodes(assignee.getId());
    if (assigneeRoles.contains("trainee")) {
      throw new BadRequestException("Trainees cannot be assigned data quality issues", "INVALID_ASSIGNEE");
    }

    boolean isNationwideAdminAssignee = validateAssigneeTenantAndRoleConsistency(assignee, assigneeRoles);
    validateAssigneeWorkflowPermission(assignee);
    validateAssigneeCallerScope(assignee, caller, isNationwideAdminAssignee);
    validateAssigneeAgainstIssueEntity(assignee, issue, isNationwideAdminAssignee);
  }

  private void validateAssigneeWorkflowPermission(User assignee) {
    Integer managePermCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_roles ur " +
            "JOIN roles r ON ur.role_id = r.role_id " +
            "JOIN role_permissions rp ON r.role_id = rp.role_id " +
            "JOIN permissions p ON rp.permission_id = p.permission_id " +
            "WHERE ur.user_id = ? AND r.deleted_at IS NULL " +
            "AND (r.lifecycle_status_id IS NULL OR r.lifecycle_status_id = 1) " +
            "AND p.permission_code = 'system.manage'",
        Integer.class, assignee.getId());
    if (managePermCount == null || managePermCount == 0) {
      throw new BadRequestException(
          "Assignee does not have workflow management authority (system.manage)",
          "INVALID_ASSIGNEE");
    }
  }

  private Set<String> loadActiveRoleCodes(Long userId) {
    List<String> rawRoles = jdbcTemplate.queryForList(
        "SELECT r.role_code FROM user_roles ur " +
            "JOIN roles r ON ur.role_id = r.role_id " +
            "WHERE ur.user_id = ? AND r.deleted_at IS NULL " +
            "AND (r.lifecycle_status_id IS NULL OR r.lifecycle_status_id = 1)",
        String.class, userId);
    if (rawRoles == null || rawRoles.isEmpty()) {
      return Set.of();
    }
    return rawRoles.stream()
        .filter(Objects::nonNull)
        .map(code -> code.trim().toLowerCase(Locale.ROOT))
        .map(code -> code.startsWith("role_") ? code.substring(5) : code)
        .filter(code -> !code.isEmpty())
        .collect(Collectors.toSet());
  }

  private boolean validateAssigneeTenantAndRoleConsistency(User assignee, Set<String> assigneeRoles) {
    Long aStateId = assignee.getStateId();
    Long aDistrictId = assignee.getDistrictId();
    Long aProviderId = assignee.getTrainingProviderId();
    Long aEmployerId = assignee.getEmployerId();

    boolean hasNationwideRole = assigneeRoles.stream().anyMatch(NATIONWIDE_ADMIN_ROLE_CODES::contains);
    boolean hasRestrictedRole = assigneeRoles.stream().anyMatch(RESTRICTED_ASSIGNEE_ROLE_CODES::contains);
    boolean isNationwideAdminAssignee = aStateId == null && aDistrictId == null && aProviderId == null
        && aEmployerId == null && hasNationwideRole && !hasRestrictedRole;

    if (aStateId == null && aDistrictId == null && aProviderId == null && aEmployerId == null
        && !isNationwideAdminAssignee) {
      throw new BadRequestException("Assignee has missing or ambiguous tenant scope", "ASSIGNEE_SCOPE_MISMATCH");
    }
    if (aProviderId != null && aEmployerId != null) {
      throw new BadRequestException("Assignee has conflicting tenant scope", "ASSIGNEE_SCOPE_MISMATCH");
    }

    if (assigneeRoles.contains("district_officer") && aDistrictId == null) {
      throw new BadRequestException("District officer assignee must have district scope", "ASSIGNEE_SCOPE_MISMATCH");
    }
    if (assigneeRoles.contains("training_provider") && aProviderId == null) {
      throw new BadRequestException("Training provider assignee must have training provider scope", "ASSIGNEE_SCOPE_MISMATCH");
    }
    if (assigneeRoles.contains("employer") && aEmployerId == null) {
      throw new BadRequestException("Employer assignee must have employer scope", "ASSIGNEE_SCOPE_MISMATCH");
    }
    if ((assigneeRoles.contains("state_admin") || assigneeRoles.contains("government_officer")) && aStateId == null) {
      throw new BadRequestException("Government officer assignee must have state scope", "ASSIGNEE_SCOPE_MISMATCH");
    }
    return isNationwideAdminAssignee;
  }

  private void validateAssigneeCallerScope(User assignee, User caller, boolean isNationwideAdminAssignee) {
    if (scopeValidator.isNationwideAdmin()) {
      return;
    }
    if (isNationwideAdminAssignee) {
      throw new BadRequestException(
          "Non-nationwide caller cannot assign issues to a nationwide administrator",
          "ASSIGNEE_SCOPE_MISMATCH");
    }

    Long aStateId = assignee.getStateId();
    Long aDistrictId = assignee.getDistrictId();
    Long aProviderId = assignee.getTrainingProviderId();
    Long aEmployerId = assignee.getEmployerId();

    if (caller.getDistrictId() != null) {
      if (aDistrictId != null && !aDistrictId.equals(caller.getDistrictId())) {
        throw new BadRequestException("Assignee does not belong to caller district scope", "ASSIGNEE_SCOPE_MISMATCH");
      }
      if (aDistrictId == null && aProviderId == null && aEmployerId == null) {
        throw new BadRequestException("District caller cannot assign issue outside district scope", "ASSIGNEE_SCOPE_MISMATCH");
      }
    }

    if (caller.getStateId() != null) {
      if (aStateId != null && !aStateId.equals(caller.getStateId())) {
        throw new BadRequestException("Assignee does not belong to caller state scope", "ASSIGNEE_SCOPE_MISMATCH");
      }
      if (aStateId == null && aDistrictId != null) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM districts WHERE district_id = ? AND state_id = ? AND deleted_at IS NULL",
            Integer.class, aDistrictId, caller.getStateId());
        if (count == null || count == 0) {
          throw new BadRequestException("Assignee district does not belong to caller state scope", "ASSIGNEE_SCOPE_MISMATCH");
        }
      }
      if (aProviderId != null) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM training_providers WHERE provider_id = ? AND state_id = ?",
            Integer.class, aProviderId, caller.getStateId());
        if (count == null || count == 0) {
          throw new BadRequestException("Assignee training provider is not located in caller state", "ASSIGNEE_SCOPE_MISMATCH");
        }
      }
      if (aEmployerId != null) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM employers WHERE employer_id = ? AND state_id = ?",
            Integer.class, aEmployerId, caller.getStateId());
        if (count == null || count == 0) {
          throw new BadRequestException("Assignee employer is not located in caller state", "ASSIGNEE_SCOPE_MISMATCH");
        }
      }
    } else if (caller.getTrainingProviderId() != null) {
      if (aProviderId == null || !aProviderId.equals(caller.getTrainingProviderId())) {
        throw new BadRequestException("Assignee does not belong to caller training provider scope", "ASSIGNEE_SCOPE_MISMATCH");
      }
    } else if (caller.getEmployerId() != null) {
      if (aEmployerId == null || !aEmployerId.equals(caller.getEmployerId())) {
        throw new BadRequestException("Assignee does not belong to caller employer scope", "ASSIGNEE_SCOPE_MISMATCH");
      }
    }
  }

  private void validateAssigneeAgainstIssueEntity(
      User assignee, DataQualityIssue issue, boolean isNationwideAdminAssignee) {
    String entityType = issue.getEntityType();
    Long entityId = issue.getEntityId();
    if (entityType == null || entityId == null) {
      throw new BadRequestException(
          "Issue entity type and identifier are required to validate assignee eligibility",
          "INVALID_ISSUE");
    }

    String normalizedType;
    try {
      normalizedType = DataQualityIssueScopeSupport.normalizeEntityType(entityType);
    } catch (BadRequestException e) {
      throw new BadRequestException("Unsupported entity type for assignee validation: " + entityType, "INVALID_ENTITY_TYPE");
    }

    if (isNationwideAdminAssignee) {
      return;
    }

    Long aStateId = assignee.getStateId();
    Long aDistrictId = assignee.getDistrictId();
    Long aProviderId = assignee.getTrainingProviderId();
    Long aEmployerId = assignee.getEmployerId();

    boolean supportsEmployer = "PlacementRecord".equals(normalizedType) || "EmploymentRecord".equals(normalizedType);
    if (!supportsEmployer && aEmployerId != null && aStateId == null && aDistrictId == null && aProviderId == null) {
      String label = switch (normalizedType) {
        case "Trainee" -> "trainee";
        case "AttendanceRecord" -> "attendance";
        case "TrainingEnrollment" -> "enrollment";
        default -> normalizedType.toLowerCase(Locale.ROOT);
      };
      throw new BadRequestException(
          "Employers cannot be assigned " + label + " data quality issues",
          "ASSIGNEE_SCOPE_MISMATCH");
    }

    if ((aStateId != null || aDistrictId != null)
        && !DataQualityIssueScopeSupport.matchesEntityStateAndDistrict(
            normalizedType, entityId, aStateId, aDistrictId, jdbcTemplate)) {
      if ("Trainee".equals(normalizedType) && aDistrictId == null) {
        throw new BadRequestException(
            "Assignee state scope does not match issue trainee state",
            "ASSIGNEE_SCOPE_MISMATCH");
      }
      throw new BadRequestException(
          "Assignee state or district scope does not match issue " + entityScopeLabel(normalizedType),
          "ASSIGNEE_SCOPE_MISMATCH");
    }

    if (aProviderId != null
        && !DataQualityIssueScopeSupport.matchesEntityProvider(normalizedType, entityId, aProviderId, jdbcTemplate)) {
      String providerTarget = "Trainee".equals(normalizedType)
          ? "issue trainee enrollments"
          : "issue " + entityScopeLabel(normalizedType);
      throw new BadRequestException(
          "Assignee training provider does not match " + providerTarget,
          "ASSIGNEE_SCOPE_MISMATCH");
    }

    if (aEmployerId != null && supportsEmployer
        && !DataQualityIssueScopeSupport.matchesEntityEmployer(normalizedType, entityId, aEmployerId, jdbcTemplate)) {
      throw new BadRequestException(
          "Assignee employer does not match issue " + entityScopeLabel(normalizedType),
          "ASSIGNEE_SCOPE_MISMATCH");
    }
  }

  private String entityScopeLabel(String normalizedType) {
    return switch (normalizedType) {
      case "Trainee" -> "trainee state or district";
      case "AttendanceRecord" -> "attendance record";
      case "PlacementRecord" -> "placement record";
      case "EmploymentRecord" -> "employment record";
      case "TrainingEnrollment" -> "enrollment record";
      default -> normalizedType;
    };
  }

  private boolean isUserTrainee(User user) {
    if (traineeRepository.findByUserId(user.getId()).isPresent()) {
      return true;
    }
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON ur.role_id = r.role_id " +
            "WHERE ur.user_id = ? AND r.deleted_at IS NULL " +
            "AND (r.lifecycle_status_id IS NULL OR r.lifecycle_status_id = 1) " +
            "AND (r.role_code = 'trainee' OR r.role_code = 'ROLE_trainee')",
        Integer.class, user.getId());
    return count != null && count > 0;
  }

  private void createAndAppendEvent(
      DataQualityIssue issue,
      RefDataQualityIssueStatus status,
      Long changedByUserId,
      LocalDateTime changedAt,
      String remarks) {
    DataQualityIssueEvent event = new DataQualityIssueEvent();
    event.setDataQualityIssue(issue);
    event.setDataQualityIssueStatus(status);
    event.setChangedByUserId(changedByUserId);
    event.setChangedAt(changedAt);
    event.setRemarks(remarks);
    event.setCreatedAt(changedAt);
    dataQualityIssueEventRepository.save(event);
  }
}
