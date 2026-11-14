package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.entity.RefDataQualityCategory;
import in.gov.sih.sih26135.entity.RefDataQualityDetectionSource;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.entity.RefDataQualitySeverity;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityIssueMapper;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.DataQualityRuleRepository;
import in.gov.sih.sih26135.repository.RefDataQualityCategoryRepository;
import in.gov.sih.sih26135.repository.RefDataQualityDetectionSourceRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.repository.RefDataQualitySeverityRepository;
import in.gov.sih.sih26135.security.AuditPayloadMasker;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.DataQualityIssueService;
import in.gov.sih.sih26135.service.DataQualityWorkflowService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataQualityIssueServiceImpl implements DataQualityIssueService {

  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final DataQualityRuleRepository dataQualityRuleRepository;
  private final RefDataQualityCategoryRepository refDataQualityCategoryRepository;
  private final RefDataQualitySeverityRepository refDataQualitySeverityRepository;
  private final RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository;
  private final RefDataQualityDetectionSourceRepository refDataQualityDetectionSourceRepository;
  private final DataQualityIssueMapper mapper;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final JdbcTemplate jdbcTemplate;
  private final DataQualityWorkflowService dataQualityWorkflowService;

  public DataQualityIssueServiceImpl(
      DataQualityIssueRepository dataQualityIssueRepository,
      DataQualityRuleRepository dataQualityRuleRepository,
      RefDataQualityCategoryRepository refDataQualityCategoryRepository,
      RefDataQualitySeverityRepository refDataQualitySeverityRepository,
      RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository,
      RefDataQualityDetectionSourceRepository refDataQualityDetectionSourceRepository,
      DataQualityIssueMapper mapper,
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate,
      DataQualityWorkflowService dataQualityWorkflowService) {
    this.dataQualityIssueRepository = Objects.requireNonNull(dataQualityIssueRepository, "dataQualityIssueRepository is required");
    this.dataQualityRuleRepository = Objects.requireNonNull(dataQualityRuleRepository, "dataQualityRuleRepository is required");
    this.refDataQualityCategoryRepository = Objects.requireNonNull(refDataQualityCategoryRepository, "refDataQualityCategoryRepository is required");
    this.refDataQualitySeverityRepository = Objects.requireNonNull(refDataQualitySeverityRepository, "refDataQualitySeverityRepository is required");
    this.refDataQualityIssueStatusRepository = Objects.requireNonNull(refDataQualityIssueStatusRepository, "refDataQualityIssueStatusRepository is required");
    this.refDataQualityDetectionSourceRepository = Objects.requireNonNull(refDataQualityDetectionSourceRepository, "refDataQualityDetectionSourceRepository is required");
    this.mapper = Objects.requireNonNull(mapper, "mapper is required");
    this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator is required");
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
    this.dataQualityWorkflowService = Objects.requireNonNull(dataQualityWorkflowService, "dataQualityWorkflowService is required");
  }

  @Override
  @Transactional
  public DataQualityIssueResponse reportIssue(CreateDataQualityIssueRequest request) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, true);

    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getEntityType() == null || request.getEntityType().isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }
    if (request.getIssueSummary() == null || request.getIssueSummary().isBlank()) {
      throw new BadRequestException("Issue summary is required", "ISSUE_SUMMARY_REQUIRED");
    }

    String entityType = request.getEntityType().trim();
    Long entityId = request.getEntityId();
    if (!callerScope.nationwideAdmin()) {
      if (entityId == null || entityId <= 0) {
        throw new AccessDeniedException("Non-nationwide callers must reference an authorized entityId");
      }
      DataQualityIssueScopeSupport.validateCallerScopeForEntity(callerScope, entityType, entityId, jdbcTemplate);
    }

    // Prevent lifecycle bypass during issue creation: new manually reported issues MUST start OPEN with no assignee
    if (request.getAssignedUserId() != null) {
      throw new BadRequestException(
          "Direct assignment during issue creation is prohibited. Use the /assign workflow endpoint.",
          "ASSIGNMENT_BYPASS_PROHIBITED"
      );
    }

    RefDataQualityIssueStatus openStatus = refDataQualityIssueStatusRepository.findByStatusCode("OPEN")
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode"));
    if (request.getDataQualityIssueStatusId() != null) {
      RefDataQualityIssueStatus requestedStatus = refDataQualityIssueStatusRepository.findById(request.getDataQualityIssueStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "dataQualityIssueStatusId"));
      if (!"OPEN".equalsIgnoreCase(requestedStatus.getStatusCode())) {
        throw new BadRequestException(
            "Newly reported issues must start in OPEN status. Lifecycle transitions must be performed via workflow endpoints.",
            "LIFECYCLE_BYPASS_PROHIBITED"
        );
      }
    }

    if (request.getDataQualityCategoryId() == null) {
      throw new BadRequestException("Data quality category ID is required", "CATEGORY_ID_REQUIRED");
    }
    RefDataQualityCategory category = refDataQualityCategoryRepository.findById(request.getDataQualityCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityCategory", "dataQualityCategoryId"));

    if (request.getDataQualitySeverityId() == null) {
      throw new BadRequestException("Data quality severity ID is required", "SEVERITY_ID_REQUIRED");
    }
    RefDataQualitySeverity severity = refDataQualitySeverityRepository.findById(request.getDataQualitySeverityId())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualitySeverity", "dataQualitySeverityId"));

    if (request.getDataQualityDetectionSourceId() == null) {
      throw new BadRequestException("Data quality detection source ID is required", "SOURCE_ID_REQUIRED");
    }
    RefDataQualityDetectionSource source = refDataQualityDetectionSourceRepository.findById(request.getDataQualityDetectionSourceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityDetectionSource", "dataQualityDetectionSourceId"));

    DataQualityRule rule = null;
    if (request.getDataQualityRuleId() != null) {
      rule = dataQualityRuleRepository.findById(request.getDataQualityRuleId())
          .orElseThrow(() -> new ResourceNotFoundException("DataQualityRule", "dataQualityRuleId"));
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime detectedAt = request.getDetectedAt() != null ? request.getDetectedAt() : now;

    DataQualityIssue issue = new DataQualityIssue(
        category,
        severity,
        openStatus,
        source,
        entityType,
        AuditPayloadMasker.maskBounded(request.getIssueSummary().trim(), 500),
        detectedAt
    );
    issue.setDataQualityRule(rule);
    issue.setEntityId(entityId);
    issue.setAssignedUserId(null);
    issue.setCreatedAt(now);
    issue.setUpdatedAt(now);

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public DataQualityIssueResponse updateIssue(Long id, UpdateDataQualityIssueRequest request) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, true);

    if (id == null || id <= 0) {
      throw new BadRequestException("Issue ID is required", "ID_REQUIRED");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));

    // Enforce tenant isolation BEFORE checking terminal status or mutating any fields
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);

    // A terminal issue must not be modified through generic update
    if (issue.getDataQualityIssueStatus() != null && Boolean.TRUE.equals(issue.getDataQualityIssueStatus().getIsTerminalFlag())) {
      throw new ConflictException("Cannot update an issue that is already in terminal state", "ISSUE_IS_TERMINAL");
    }

    // Protect lifecycle history: direct alteration of status, assignment or resolution metadata is strictly prohibited
    if (request.getDataQualityIssueStatusId() != null) {
      throw new BadRequestException(
          "Direct alteration of status is not permitted via generic update. Use dedicated lifecycle endpoints.",
          "LIFECYCLE_BYPASS_PROHIBITED"
      );
    }
    if (request.getAssignedUserId() != null) {
      throw new BadRequestException(
          "Direct alteration of assignment is not permitted via generic update. Use dedicated /assign endpoint.",
          "ASSIGNMENT_BYPASS_PROHIBITED"
      );
    }
    if (request.getResolvedAt() != null || request.getResolvedByUserId() != null || request.getResolutionNotes() != null) {
      throw new BadRequestException(
          "Direct alteration of resolution metadata is not permitted via generic update. Use dedicated /resolve endpoint.",
          "RESOLUTION_BYPASS_PROHIBITED"
      );
    }

    if (request.getIssueSummary() != null && !request.getIssueSummary().isBlank()) {
      issue.setIssueSummary(AuditPayloadMasker.maskBounded(request.getIssueSummary().trim(), 500));
    }

    if (request.getDataQualityCategoryId() != null) {
      RefDataQualityCategory category = refDataQualityCategoryRepository.findById(request.getDataQualityCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityCategory", "dataQualityCategoryId"));
      issue.setDataQualityCategory(category);
    }

    if (request.getDataQualitySeverityId() != null) {
      RefDataQualitySeverity severity = refDataQualitySeverityRepository.findById(request.getDataQualitySeverityId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualitySeverity", "dataQualitySeverityId"));
      issue.setDataQualitySeverity(severity);
    }

    if (request.getDataQualityDetectionSourceId() != null) {
      RefDataQualityDetectionSource source = refDataQualityDetectionSourceRepository.findById(request.getDataQualityDetectionSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityDetectionSource", "dataQualityDetectionSourceId"));
      issue.setDataQualityDetectionSource(source);
    }

    issue.setUpdatedAt(LocalDateTime.now());
    DataQualityIssue saved = dataQualityIssueRepository.save(issue);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public DataQualityIssueResponse assignIssue(Long id, Long assignedUserId) {
    if (dataQualityWorkflowService == null) {
      throw new AccessDeniedException("Workflow service unavailable to assign data quality issue");
    }
    return dataQualityWorkflowService.assignIssue(id, new AssignDataQualityIssueRequest(assignedUserId));
  }

  @Override
  @Transactional
  public DataQualityIssueResponse resolveIssue(Long id, ResolveDataQualityIssueRequest request) {
    if (dataQualityWorkflowService == null) {
      throw new AccessDeniedException("Workflow service unavailable to resolve data quality issue");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (Boolean.TRUE.equals(request.getIsDismissed())) {
      return dataQualityWorkflowService.dismissIssue(id, new DismissDataQualityIssueRequest(request.getResolutionNotes()));
    }
    return dataQualityWorkflowService.resolveIssue(id, request);
  }

  @Override
  public DataQualityIssueResponse getIssueById(Long id) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (id == null || id <= 0) {
      throw new BadRequestException("Issue ID is required");
    }
    DataQualityIssue entity = dataQualityIssueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, entity, jdbcTemplate);
    return mapper.toResponse(entity);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByRuleId(Long ruleId) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (ruleId == null) {
      throw new BadRequestException("Rule ID is required");
    }
    List<DataQualityIssue> issues = dataQualityIssueRepository.findByDataQualityRuleId(ruleId);
    return mapInScope(callerScope, issues);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByCategoryId(Long categoryId) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (categoryId == null) {
      throw new BadRequestException("Category ID is required");
    }
    List<DataQualityIssue> issues = dataQualityIssueRepository.findByDataQualityCategoryId(categoryId);
    return mapInScope(callerScope, issues);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesBySeverityId(Long severityId) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    List<DataQualityIssue> issues = dataQualityIssueRepository.findByDataQualitySeverityId(severityId);
    return mapInScope(callerScope, issues);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByStatusId(Long statusId) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    List<DataQualityIssue> issues = dataQualityIssueRepository.findByDataQualityIssueStatusId(statusId);
    return mapInScope(callerScope, issues);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByDetectionSourceId(Long sourceId) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (sourceId == null) {
      throw new BadRequestException("Detection source ID is required");
    }
    List<DataQualityIssue> issues = dataQualityIssueRepository.findByDataQualityDetectionSourceId(sourceId);
    return mapInScope(callerScope, issues);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByEntityType(String entityType) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    DataQualityIssueScopeSupport.validateCanQueryEntityType(callerScope, entityType);
    List<DataQualityIssue> issues = dataQualityIssueRepository.findByEntityType(entityType.trim());
    return mapInScope(callerScope, issues);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByEntity(String entityType, Long entityId) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null || entityId <= 0) {
      throw new BadRequestException("Entity ID is required");
    }
    DataQualityIssueScopeSupport.validateCallerScopeForEntity(callerScope, entityType, entityId, jdbcTemplate);
    return dataQualityIssueRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByAssignedUserId(Long assignedUserId) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (assignedUserId == null) {
      throw new BadRequestException("Assigned user ID is required");
    }
    List<DataQualityIssue> issues = dataQualityIssueRepository.findByAssignedUserId(assignedUserId);
    return mapInScope(callerScope, issues);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByResolvedByUserId(Long resolvedByUserId) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (resolvedByUserId == null) {
      throw new BadRequestException("Resolved by user ID is required");
    }
    List<DataQualityIssue> issues = dataQualityIssueRepository.findByResolvedByUserId(resolvedByUserId);
    return mapInScope(callerScope, issues);
  }

  private List<DataQualityIssueResponse> mapInScope(
      DataQualityIssueScopeSupport.CallerScope callerScope,
      List<DataQualityIssue> issues) {
    return DataQualityIssueScopeSupport.filterIssuesInScope(callerScope, issues, jdbcTemplate).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
