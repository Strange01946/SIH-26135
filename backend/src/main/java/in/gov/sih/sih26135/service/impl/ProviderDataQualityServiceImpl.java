package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.CenterScorecardResponse;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.dto.response.ProviderDataQualityIssueResponse;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.DataQualityIssueEvent;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidDataQualityTransitionException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityIssueEventMapper;
import in.gov.sih.sih26135.repository.DataQualityIssueEventRepository;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.ProviderDataQualityQueryRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.security.AuditPayloadMasker;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.DataQualityEngineService;
import in.gov.sih.sih26135.service.ProviderDataQualityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ProviderDataQualityServiceImpl implements ProviderDataQualityService {

  private static final Logger log = LoggerFactory.getLogger(ProviderDataQualityServiceImpl.class);

  private final ProviderDataQualityQueryRepository queryRepository;
  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final DataQualityIssueEventRepository dataQualityIssueEventRepository;
  private final RefDataQualityIssueStatusRepository statusRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final DataQualityIssueEventMapper eventMapper;
  private final JdbcTemplate jdbcTemplate;

  @Autowired(required = false)
  private DataQualityEngineService dataQualityEngineService;

  public ProviderDataQualityServiceImpl(
      ProviderDataQualityQueryRepository queryRepository,
      DataQualityIssueRepository dataQualityIssueRepository,
      DataQualityIssueEventRepository dataQualityIssueEventRepository,
      RefDataQualityIssueStatusRepository statusRepository,
      WorkflowSecurityScopeValidator scopeValidator,
      DataQualityIssueEventMapper eventMapper,
      JdbcTemplate jdbcTemplate) {
    this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository is required");
    this.dataQualityIssueRepository = Objects.requireNonNull(dataQualityIssueRepository, "dataQualityIssueRepository is required");
    this.dataQualityIssueEventRepository = Objects.requireNonNull(dataQualityIssueEventRepository, "dataQualityIssueEventRepository is required");
    this.statusRepository = Objects.requireNonNull(statusRepository, "statusRepository is required");
    this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator is required");
    this.eventMapper = Objects.requireNonNull(eventMapper, "eventMapper is required");
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
  }

  private User resolveAndValidateProviderCaller() {
    if (scopeValidator.isTraineeCaller()) {
      throw new AccessDeniedException("Trainees are not authorized to access training provider data quality portal");
    }
    User caller = scopeValidator.resolveAuthenticatedUser()
        .orElseThrow(() -> new AccessDeniedException("Authenticated user identity is required"));

    if (caller.getDeletedAt() != null || (caller.getUserStatusId() != null && caller.getUserStatusId() != 2L)) {
      throw new AccessDeniedException("Caller user account is deleted or inactive");
    }

    if (caller.getTrainingProviderId() == null) {
      throw new AccessDeniedException("Caller lacks required training provider tenant scope");
    }
    return caller;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProviderDataQualityIssueResponse> getProviderIssues(
      String severity,
      String category,
      String status,
      String entityType,
      Long centerId,
      Boolean activeOnly,
      Pageable pageable) {

    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    if (centerId != null && centerId > 0) {
      validateCenterBelongsToProvider(centerId, providerId);
    }

    return queryRepository.findProviderIssues(
        providerId, severity, category, status, entityType, centerId, activeOnly, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public ProviderDataQualityIssueResponse getIssueById(Long issueId) {
    validateIssueId(issueId);
    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    return queryRepository.findProviderIssueById(providerId, issueId)
        .orElseGet(() -> {
          // Determine whether issue does not exist or exists under another provider
          boolean exists = dataQualityIssueRepository.existsById(issueId);
          if (exists) {
            throw new AccessDeniedException("Data quality issue does not belong to caller's provider scope");
          }
          throw new ResourceNotFoundException("DataQualityIssue", "id=" + issueId);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataQualityIssueEventResponse> getIssueEvents(Long issueId, int page, int size) {
    validateIssueId(issueId);
    int safePage = Math.max(0, page);
    int safeSize = (size >= 1 && size <= 100) ? size : 20;

    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    if (!queryRepository.isIssueOwnedByProvider(issueId, providerId)) {
      boolean exists = dataQualityIssueRepository.existsById(issueId);
      if (exists) {
        throw new AccessDeniedException("Data quality issue does not belong to caller's provider scope");
      }
      throw new ResourceNotFoundException("DataQualityIssue", "id=" + issueId);
    }

    int offset = safePage * safeSize;
    List<DataQualityIssueEvent> events = dataQualityIssueEventRepository.findByDataQualityIssueId(issueId);
    if (events == null || events.isEmpty()) {
      return List.of();
    }

    return events.stream()
        .sorted((a, b) -> a.getChangedAt().compareTo(b.getChangedAt()))
        .skip(offset)
        .limit(safeSize)
        .map(eventMapper::toResponse)
        .toList();
  }

  @Override
  public ProviderDataQualityIssueResponse startInvestigation(Long issueId) {
    validateIssueId(issueId);
    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    lockIssueRow(issueId);
    if (!queryRepository.isIssueOwnedByProvider(issueId, providerId)) {
      boolean exists = dataQualityIssueRepository.existsById(issueId);
      if (exists) {
        throw new AccessDeniedException("Data quality issue does not belong to caller's provider scope");
      }
      throw new ResourceNotFoundException("DataQualityIssue", "id=" + issueId);
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(issueId)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id=" + issueId));

    String currentStatus = issue.getDataQualityIssueStatus() != null
        ? issue.getDataQualityIssueStatus().getStatusCode().toUpperCase()
        : "OPEN";

    if (!"OPEN".equals(currentStatus) && !"ASSIGNED".equals(currentStatus) && !"REOPENED".equals(currentStatus)) {
      throw new InvalidDataQualityTransitionException(currentStatus, "IN_PROGRESS");
    }

    RefDataQualityIssueStatus inProgressStatus = statusRepository.findByStatusCode("IN_PROGRESS")
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode=IN_PROGRESS"));

    LocalDateTime now = LocalDateTime.now();
    issue.setDataQualityIssueStatus(inProgressStatus);
    issue.setAssignedUserId(caller.getId());
    issue.setUpdatedAt(now);
    dataQualityIssueRepository.saveAndFlush(issue);

    appendIssueEvent(issue, inProgressStatus, caller.getId(), now, "Provider started investigation");

    return queryRepository.findProviderIssueById(providerId, issueId)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id=" + issueId));
  }

  @Override
  public ProviderDataQualityIssueResponse resolveIssue(Long issueId, ResolveDataQualityIssueRequest request) {
    validateIssueId(issueId);
    if (request == null || request.getResolutionNotes() == null || request.getResolutionNotes().isBlank()) {
      throw new BadRequestException("resolutionNotes cannot be blank", "VALIDATION_FAILED");
    }
    if (request.getResolutionNotes().length() > 500) {
      throw new BadRequestException("resolutionNotes cannot exceed 500 characters", "VALIDATION_FAILED");
    }

    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    lockIssueRow(issueId);
    if (!queryRepository.isIssueOwnedByProvider(issueId, providerId)) {
      boolean exists = dataQualityIssueRepository.existsById(issueId);
      if (exists) {
        throw new AccessDeniedException("Data quality issue does not belong to caller's provider scope");
      }
      throw new ResourceNotFoundException("DataQualityIssue", "id=" + issueId);
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(issueId)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id=" + issueId));

    String currentStatus = issue.getDataQualityIssueStatus() != null
        ? issue.getDataQualityIssueStatus().getStatusCode().toUpperCase()
        : "OPEN";

    if ("RESOLVED".equals(currentStatus) || "DISMISSED".equals(currentStatus)) {
      throw new InvalidDataQualityTransitionException(currentStatus, "RESOLVED");
    }

    RefDataQualityIssueStatus resolvedStatus = statusRepository.findByStatusCode("RESOLVED")
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode=RESOLVED"));

    LocalDateTime now = LocalDateTime.now();
    String safeNotes = AuditPayloadMasker.maskBounded(request.getResolutionNotes().trim(), 500);

    issue.setDataQualityIssueStatus(resolvedStatus);
    issue.setResolvedAt(now);
    issue.setResolvedByUserId(caller.getId());
    issue.setResolutionNotes(safeNotes);
    issue.setUpdatedAt(now);
    dataQualityIssueRepository.saveAndFlush(issue);

    appendIssueEvent(issue, resolvedStatus, caller.getId(), now, safeNotes);

    log.info("Provider user {} successfully resolved data quality issue {}", caller.getId(), issueId);

    return queryRepository.findProviderIssueById(providerId, issueId)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id=" + issueId));
  }

  @Override
  public ProviderDataQualityIssueResponse dismissIssue(Long issueId, DismissDataQualityIssueRequest request) {
    validateIssueId(issueId);
    if (request == null || request.getRemarks() == null || request.getRemarks().isBlank()) {
      throw new BadRequestException("remarks cannot be blank", "VALIDATION_FAILED");
    }
    if (request.getRemarks().length() > 500) {
      throw new BadRequestException("remarks cannot exceed 500 characters", "VALIDATION_FAILED");
    }

    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    lockIssueRow(issueId);
    if (!queryRepository.isIssueOwnedByProvider(issueId, providerId)) {
      boolean exists = dataQualityIssueRepository.existsById(issueId);
      if (exists) {
        throw new AccessDeniedException("Data quality issue does not belong to caller's provider scope");
      }
      throw new ResourceNotFoundException("DataQualityIssue", "id=" + issueId);
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(issueId)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id=" + issueId));

    String currentStatus = issue.getDataQualityIssueStatus() != null
        ? issue.getDataQualityIssueStatus().getStatusCode().toUpperCase()
        : "OPEN";

    if ("RESOLVED".equals(currentStatus) || "DISMISSED".equals(currentStatus)) {
      throw new InvalidDataQualityTransitionException(currentStatus, "DISMISSED");
    }

    RefDataQualityIssueStatus dismissedStatus = statusRepository.findByStatusCode("DISMISSED")
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode=DISMISSED"));

    LocalDateTime now = LocalDateTime.now();
    String safeRemarks = AuditPayloadMasker.maskBounded(request.getRemarks().trim(), 500);

    issue.setDataQualityIssueStatus(dismissedStatus);
    issue.setResolvedAt(now);
    issue.setResolvedByUserId(caller.getId());
    issue.setResolutionNotes(safeRemarks);
    issue.setUpdatedAt(now);
    dataQualityIssueRepository.saveAndFlush(issue);

    appendIssueEvent(issue, dismissedStatus, caller.getId(), now, safeRemarks);

    log.info("Provider user {} successfully dismissed data quality issue {}", caller.getId(), issueId);

    return queryRepository.findProviderIssueById(providerId, issueId)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id=" + issueId));
  }

  @Override
  @Transactional(readOnly = true)
  public CenterScorecardResponse getCenterScorecard(Long centerId) {
    if (centerId == null || centerId <= 0) {
      throw new BadRequestException("centerId must be a positive integer", "INVALID_CENTER_ID");
    }

    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    validateCenterBelongsToProvider(centerId, providerId);

    CenterScorecardResponse scorecard = queryRepository.getCenterScorecard(providerId, centerId);
    if (scorecard == null) {
      throw new ResourceNotFoundException("TrainingCenter", "id=" + centerId);
    }
    return scorecard;
  }

  @Override
  public void triggerReEvaluation(Long issueId) {
    User caller = resolveAndValidateProviderCaller();
    if (issueId != null && issueId > 0) {
      validateIssueId(issueId);
      if (!queryRepository.isIssueOwnedByProvider(issueId, caller.getTrainingProviderId())) {
        throw new AccessDeniedException("Data quality issue does not belong to caller's provider scope");
      }
    }
    if (dataQualityEngineService != null) {
      try {
        dataQualityEngineService.executeScan(new DataQualityScanRequest());
      } catch (Exception e) {
        log.warn("Rule re-evaluation failed or was partially completed: {}", e.getMessage());
      }
    }
  }

  private void validateCenterBelongsToProvider(Long centerId, Long providerId) {
    if (!queryRepository.isCenterOwnedByProvider(centerId, providerId)) {
      Integer exists = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM training_centers WHERE center_id = ? AND deleted_at IS NULL",
          Integer.class, centerId);
      if (exists != null && exists > 0) {
        throw new AccessDeniedException("Training center " + centerId + " does not belong to authenticated provider scope");
      }
      throw new ResourceNotFoundException("TrainingCenter", "id=" + centerId);
    }
  }

  private void validateIssueId(Long issueId) {
    if (issueId == null || issueId <= 0) {
      throw new BadRequestException("Issue ID must be a positive integer", "INVALID_ISSUE_ID");
    }
  }

  private void lockIssueRow(Long issueId) {
    try {
      jdbcTemplate.queryForObject(
          "SELECT data_quality_issue_id FROM data_quality_issues WHERE data_quality_issue_id = ? FOR UPDATE",
          Long.class, issueId);
    } catch (Exception e) {
      throw new ResourceNotFoundException("DataQualityIssue", "id=" + issueId);
    }
  }

  private void appendIssueEvent(
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
    event.setRemarks(AuditPayloadMasker.maskBounded(remarks, 500));
    event.setCreatedAt(changedAt);
    dataQualityIssueEventRepository.save(event);
  }
}
