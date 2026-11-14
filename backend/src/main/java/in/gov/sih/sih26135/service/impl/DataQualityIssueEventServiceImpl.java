package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueEventRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.DataQualityIssueEvent;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityIssueEventMapper;
import in.gov.sih.sih26135.repository.DataQualityIssueEventRepository;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.AuditPayloadMasker;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.DataQualityIssueEventService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataQualityIssueEventServiceImpl implements DataQualityIssueEventService {

  private static final Logger log = LoggerFactory.getLogger(DataQualityIssueEventServiceImpl.class);

  private final DataQualityIssueEventRepository dataQualityIssueEventRepository;
  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository;
  private final UserRepository userRepository;
  private final DataQualityIssueEventMapper mapper;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final JdbcTemplate jdbcTemplate;

  public DataQualityIssueEventServiceImpl(
      DataQualityIssueEventRepository dataQualityIssueEventRepository,
      DataQualityIssueRepository dataQualityIssueRepository,
      RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository,
      UserRepository userRepository,
      DataQualityIssueEventMapper mapper) {
    this(
        dataQualityIssueEventRepository,
        dataQualityIssueRepository,
        refDataQualityIssueStatusRepository,
        userRepository,
        mapper,
        null,
        null);
  }

  @Autowired
  public DataQualityIssueEventServiceImpl(
      DataQualityIssueEventRepository dataQualityIssueEventRepository,
      DataQualityIssueRepository dataQualityIssueRepository,
      RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository,
      UserRepository userRepository,
      DataQualityIssueEventMapper mapper,
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate) {
    this.dataQualityIssueEventRepository = dataQualityIssueEventRepository;
    this.dataQualityIssueRepository = dataQualityIssueRepository;
    this.refDataQualityIssueStatusRepository = refDataQualityIssueStatusRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
    this.scopeValidator = scopeValidator;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public DataQualityIssueEventResponse recordIssueEvent(CreateDataQualityIssueEventRequest request) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, true);

    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getDataQualityIssueId() == null) {
      throw new BadRequestException("Data quality issue ID is required", "DATA_QUALITY_ISSUE_ID_REQUIRED");
    }
    if (request.getDataQualityIssueStatusId() == null) {
      throw new BadRequestException("Data quality issue status ID is required", "STATUS_ID_REQUIRED");
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(request.getDataQualityIssueId())
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);

    RefDataQualityIssueStatus status = refDataQualityIssueStatusRepository.findById(request.getDataQualityIssueStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "id"));

    if (request.getChangedByUserId() != null && !userRepository.existsById(request.getChangedByUserId())) {
      throw new ResourceNotFoundException("User", "id");
    }

    if (request.getRemarks() != null && request.getRemarks().length() > 500) {
      throw new BadRequestException("Remarks must not exceed 500 characters", "REMARKS_TOO_LONG");
    }

    LocalDateTime changedAt = request.getChangedAt() != null ? request.getChangedAt() : LocalDateTime.now();

    DataQualityIssueEvent event = new DataQualityIssueEvent(issue, status, changedAt);
    event.setChangedByUserId(callerScope.caller() != null ? callerScope.caller().getId() : request.getChangedByUserId());
    event.setRemarks(AuditPayloadMasker.maskBounded(request.getRemarks(), 500));
    event.setCreatedAt(LocalDateTime.now());

    DataQualityIssueEvent saved = dataQualityIssueEventRepository.save(event);
    log.info("Recorded data quality issue event id={} for issue id={}, status id={}",
        saved.getId(), issue.getId(), status.getId());
    return mapper.toResponse(saved);
  }

  @Override
  public DataQualityIssueEventResponse getIssueEventById(Long id) {
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (id == null) {
      throw new BadRequestException("Event ID is required");
    }
    DataQualityIssueEvent event = dataQualityIssueEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssueEvent", "id"));
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, event.getDataQualityIssue(), jdbcTemplate);
    return mapper.toResponse(event);
  }

  private static void validatePaginationBounds(int page, int size) {
    if (page < 0) {
      throw new BadRequestException("Page index must not be negative", "INVALID_PAGE");
    }
    if (size < 1 || size > 100) {
      throw new BadRequestException("Page size must be between 1 and 100", "INVALID_SIZE");
    }
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByIssueId(Long issueId) {
    return getIssueEventsByIssueId(issueId, 0, 100);
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByIssueId(Long issueId, int page, int size) {
    validatePaginationBounds(page, size);
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (issueId == null) {
      throw new BadRequestException("Issue ID is required");
    }
    DataQualityIssue issue = dataQualityIssueRepository.findById(issueId)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));
    DataQualityIssueScopeSupport.validateCallerScopeForIssue(callerScope, issue, jdbcTemplate);
    return findScopedIssueEvents(callerScope, "ev.data_quality_issue_id = ?", page, size, issueId);
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByStatusId(Long statusId) {
    return getIssueEventsByStatusId(statusId, 0, 100);
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByStatusId(Long statusId, int page, int size) {
    validatePaginationBounds(page, size);
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return findScopedIssueEvents(callerScope, "ev.data_quality_issue_status_id = ?", page, size, statusId);
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByChangedByUserId(Long changedByUserId) {
    return getIssueEventsByChangedByUserId(changedByUserId, 0, 100);
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByChangedByUserId(Long changedByUserId, int page, int size) {
    validatePaginationBounds(page, size);
    DataQualityIssueScopeSupport.CallerScope callerScope =
        DataQualityIssueScopeSupport.resolveAndValidateCaller(scopeValidator, jdbcTemplate, false);
    if (changedByUserId == null) {
      throw new BadRequestException("Changed-by user ID is required");
    }
    return findScopedIssueEvents(callerScope, "ev.changed_by_user_id = ?", page, size, changedByUserId);
  }

  private List<DataQualityIssueEventResponse> findScopedIssueEvents(
      DataQualityIssueScopeSupport.CallerScope scope,
      String wherePredicate,
      int page,
      int size,
      Object... whereParams) {
    if (jdbcTemplate == null) {
      return List.of();
    }
    int safePage = Math.max(0, page);
    int safeSize = (size >= 1 && size <= 100) ? size : 100;
    long rawOffset = (long) safePage * (long) safeSize;
    if (rawOffset > Integer.MAX_VALUE) {
      return List.of();
    }
    StringBuilder sql = new StringBuilder(
        "SELECT ev.data_quality_issue_event_id FROM data_quality_issue_events ev "
            + "JOIN data_quality_issues dqi ON ev.data_quality_issue_id = dqi.data_quality_issue_id "
            + "WHERE ").append(wherePredicate);
    List<Object> params = new ArrayList<>();
    if (whereParams != null) {
      for (Object p : whereParams) {
        params.add(p);
      }
    }
    DataQualityIssueScopeSupport.appendIssueTenantWhereClause(scope, sql, null, params);
    sql.append(" ORDER BY ev.data_quality_issue_event_id DESC LIMIT ? OFFSET ?");
    params.add(safeSize);
    params.add((int) rawOffset);

    List<Long> ids = jdbcTemplate.queryForList(sql.toString(), Long.class, params.toArray());
    if (ids.isEmpty()) {
      return List.of();
    }
    Map<Long, DataQualityIssueEvent> byId = new LinkedHashMap<>();
    for (DataQualityIssueEvent event : dataQualityIssueEventRepository.findAllById(ids)) {
      byId.put(event.getId(), event);
    }
    List<DataQualityIssueEventResponse> ordered = new ArrayList<>(ids.size());
    for (Long id : ids) {
      DataQualityIssueEvent event = byId.get(id);
      if (event != null) {
        ordered.add(mapper.toResponse(event));
      }
    }
    return ordered;
  }
}
