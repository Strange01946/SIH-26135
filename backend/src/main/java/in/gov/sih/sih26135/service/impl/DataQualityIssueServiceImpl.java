package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest;
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
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.DataQualityIssueService;
import java.time.LocalDateTime;
import java.util.List;
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
  private final UserRepository userRepository;
  private final DataQualityIssueMapper mapper;

  public DataQualityIssueServiceImpl(
      DataQualityIssueRepository dataQualityIssueRepository,
      DataQualityRuleRepository dataQualityRuleRepository,
      RefDataQualityCategoryRepository refDataQualityCategoryRepository,
      RefDataQualitySeverityRepository refDataQualitySeverityRepository,
      RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository,
      RefDataQualityDetectionSourceRepository refDataQualityDetectionSourceRepository,
      UserRepository userRepository,
      DataQualityIssueMapper mapper) {
    this.dataQualityIssueRepository = dataQualityIssueRepository;
    this.dataQualityRuleRepository = dataQualityRuleRepository;
    this.refDataQualityCategoryRepository = refDataQualityCategoryRepository;
    this.refDataQualitySeverityRepository = refDataQualitySeverityRepository;
    this.refDataQualityIssueStatusRepository = refDataQualityIssueStatusRepository;
    this.refDataQualityDetectionSourceRepository = refDataQualityDetectionSourceRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public DataQualityIssueResponse reportIssue(CreateDataQualityIssueRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getEntityType() == null || request.getEntityType().isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }
    if (request.getIssueSummary() == null || request.getIssueSummary().isBlank()) {
      throw new BadRequestException("Issue summary is required", "ISSUE_SUMMARY_REQUIRED");
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

    RefDataQualityIssueStatus status;
    if (request.getDataQualityIssueStatusId() != null) {
      status = refDataQualityIssueStatusRepository.findById(request.getDataQualityIssueStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "dataQualityIssueStatusId"));
      if (Boolean.TRUE.equals(status.getIsTerminalFlag())) {
        throw new BadRequestException("Newly reported issues cannot start in terminal status", "INITIAL_STATUS_CANNOT_BE_TERMINAL");
      }
    } else {
      status = refDataQualityIssueStatusRepository.findByStatusCode("OPEN")
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode"));
    }

    if (request.getAssignedUserId() != null) {
      if (!userRepository.existsById(request.getAssignedUserId())) {
        throw new ResourceNotFoundException("User", "assignedUserId");
      }
    }

    LocalDateTime detectedAt = request.getDetectedAt() != null ? request.getDetectedAt() : LocalDateTime.now();

    DataQualityIssue issue = new DataQualityIssue(
        category,
        severity,
        status,
        source,
        request.getEntityType().trim(),
        request.getIssueSummary().trim(),
        detectedAt
    );
    issue.setDataQualityRule(rule);
    issue.setEntityId(request.getEntityId());
    issue.setAssignedUserId(request.getAssignedUserId());

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public DataQualityIssueResponse updateIssue(Long id, UpdateDataQualityIssueRequest request) {
    if (id == null) {
      throw new BadRequestException("Issue ID is required", "ID_REQUIRED");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));

    // A terminal issue must not be modified through generic update
    if (issue.getDataQualityIssueStatus() != null && Boolean.TRUE.equals(issue.getDataQualityIssueStatus().getIsTerminalFlag())) {
      throw new ConflictException("Cannot update an issue that is already in terminal state", "ISSUE_IS_TERMINAL");
    }

    if (request.getIssueSummary() != null && !request.getIssueSummary().isBlank()) {
      issue.setIssueSummary(request.getIssueSummary().trim());
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

    if (request.getAssignedUserId() != null) {
      if (!userRepository.existsById(request.getAssignedUserId())) {
        throw new ResourceNotFoundException("User", "assignedUserId");
      }
      issue.setAssignedUserId(request.getAssignedUserId());
    }

    // Status transition validation in generic update
    if (request.getDataQualityIssueStatusId() != null) {
      RefDataQualityIssueStatus newStatus = refDataQualityIssueStatusRepository.findById(request.getDataQualityIssueStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "dataQualityIssueStatusId"));
      if (Boolean.TRUE.equals(newStatus.getIsTerminalFlag())) {
        throw new BadRequestException(
            "Terminal status transitions must be performed via resolveIssue()",
            "USE_RESOLVE_ISSUE_FOR_TERMINAL_STATUS"
        );
      }
      issue.setDataQualityIssueStatus(newStatus);
    }

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public DataQualityIssueResponse assignIssue(Long id, Long assignedUserId) {
    if (id == null) {
      throw new BadRequestException("Issue ID is required", "ID_REQUIRED");
    }
    if (assignedUserId == null) {
      throw new BadRequestException("Assigned user ID is required", "ASSIGNED_USER_REQUIRED");
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));

    if (issue.getDataQualityIssueStatus() != null && Boolean.TRUE.equals(issue.getDataQualityIssueStatus().getIsTerminalFlag())) {
      throw new ConflictException("Cannot assign an issue that is already in terminal state", "ISSUE_IS_TERMINAL");
    }

    if (!userRepository.existsById(assignedUserId)) {
      throw new ResourceNotFoundException("User", "assignedUserId");
    }

    issue.setAssignedUserId(assignedUserId);

    if (issue.getDataQualityIssueStatus() != null && "OPEN".equalsIgnoreCase(issue.getDataQualityIssueStatus().getStatusCode())) {
      refDataQualityIssueStatusRepository.findByStatusCode("ASSIGNED")
          .ifPresent(issue::setDataQualityIssueStatus);
    }

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public DataQualityIssueResponse resolveIssue(Long id, ResolveDataQualityIssueRequest request) {
    if (id == null) {
      throw new BadRequestException("Issue ID is required", "ID_REQUIRED");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));

    if (issue.getDataQualityIssueStatus() != null && Boolean.TRUE.equals(issue.getDataQualityIssueStatus().getIsTerminalFlag())) {
      throw new ConflictException("Issue is already resolved or dismissed", "ISSUE_ALREADY_RESOLVED");
    }

    String targetStatusCode = Boolean.TRUE.equals(request.getIsDismissed()) ? "DISMISSED" : "RESOLVED";
    RefDataQualityIssueStatus terminalStatus = refDataQualityIssueStatusRepository.findByStatusCode(targetStatusCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode"));

    LocalDateTime resolvedAt = request.getResolvedAt() != null ? request.getResolvedAt() : LocalDateTime.now();
    if (resolvedAt.isBefore(issue.getDetectedAt())) {
      throw new BadRequestException("Resolved date cannot be before detected date", "RESOLVED_DATE_BEFORE_DETECTED");
    }

    if (request.getResolvedByUserId() != null) {
      if (!userRepository.existsById(request.getResolvedByUserId())) {
        throw new ResourceNotFoundException("User", "resolvedByUserId");
      }
    }

    issue.setDataQualityIssueStatus(terminalStatus);
    issue.setResolvedAt(resolvedAt);
    issue.setResolvedByUserId(request.getResolvedByUserId());
    issue.setResolutionNotes(request.getResolutionNotes());

    DataQualityIssue saved = dataQualityIssueRepository.save(issue);
    return mapper.toResponse(saved);
  }

  @Override
  public DataQualityIssueResponse getIssueById(Long id) {
    if (id == null) {
      throw new BadRequestException("Issue ID is required");
    }
    DataQualityIssue entity = dataQualityIssueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByRuleId(Long ruleId) {
    if (ruleId == null) {
      throw new BadRequestException("Rule ID is required");
    }
    return dataQualityIssueRepository.findByDataQualityRuleId(ruleId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new BadRequestException("Category ID is required");
    }
    return dataQualityIssueRepository.findByDataQualityCategoryId(categoryId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesBySeverityId(Long severityId) {
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    return dataQualityIssueRepository.findByDataQualitySeverityId(severityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return dataQualityIssueRepository.findByDataQualityIssueStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByDetectionSourceId(Long sourceId) {
    if (sourceId == null) {
      throw new BadRequestException("Detection source ID is required");
    }
    return dataQualityIssueRepository.findByDataQualityDetectionSourceId(sourceId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    return dataQualityIssueRepository.findByEntityType(entityType.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByEntity(String entityType, Long entityId) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    return dataQualityIssueRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByAssignedUserId(Long assignedUserId) {
    if (assignedUserId == null) {
      throw new BadRequestException("Assigned user ID is required");
    }
    return dataQualityIssueRepository.findByAssignedUserId(assignedUserId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueResponse> getIssuesByResolvedByUserId(Long resolvedByUserId) {
    if (resolvedByUserId == null) {
      throw new BadRequestException("Resolved by user ID is required");
    }
    return dataQualityIssueRepository.findByResolvedByUserId(resolvedByUserId).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
