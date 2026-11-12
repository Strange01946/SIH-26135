package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.correlation.CorrelationIdFilter;
import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.request.OperationalEventFilterCriteria;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import in.gov.sih.sih26135.entity.SystemEventLog;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SystemEventLogMapper;
import in.gov.sih.sih26135.repository.RefSystemEventCategoryRepository;
import in.gov.sih.sih26135.repository.RefSystemEventSeverityRepository;
import in.gov.sih.sih26135.repository.SystemEventLogQueryRepository;
import in.gov.sih.sih26135.repository.SystemEventLogQueryRepository.CallerTenantScope;
import in.gov.sih.sih26135.repository.SystemEventLogRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.SystemEventLogService;
import in.gov.sih.sih26135.service.event.SystemEventSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SystemEventLogServiceImpl implements SystemEventLogService {

  private static final Logger log = LoggerFactory.getLogger(SystemEventLogServiceImpl.class);

  private static final Set<String> VALID_CATEGORIES = Set.of(
      "SECURITY", "INTEGRATION", "BATCH_JOB", "CONFIGURATION", "ACCESS"
  );
  private static final Set<String> VALID_SEVERITIES = Set.of(
      "INFO", "WARNING", "ERROR", "CRITICAL"
  );
  private static final int LEGACY_LIST_BOUND = 100;

  private final SystemEventLogRepository systemEventLogRepository;
  private final RefSystemEventCategoryRepository refSystemEventCategoryRepository;
  private final RefSystemEventSeverityRepository refSystemEventSeverityRepository;
  private final UserRepository userRepository;
  private final SystemEventLogMapper mapper;
  private final SystemEventLogQueryRepository queryRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;

  public SystemEventLogServiceImpl(
      SystemEventLogRepository systemEventLogRepository,
      RefSystemEventCategoryRepository refSystemEventCategoryRepository,
      RefSystemEventSeverityRepository refSystemEventSeverityRepository,
      UserRepository userRepository,
      SystemEventLogMapper mapper) {
    this(
        systemEventLogRepository,
        refSystemEventCategoryRepository,
        refSystemEventSeverityRepository,
        userRepository,
        mapper,
        null,
        null);
  }

  @Autowired
  public SystemEventLogServiceImpl(
      SystemEventLogRepository systemEventLogRepository,
      RefSystemEventCategoryRepository refSystemEventCategoryRepository,
      RefSystemEventSeverityRepository refSystemEventSeverityRepository,
      UserRepository userRepository,
      SystemEventLogMapper mapper,
      @Autowired(required = false) SystemEventLogQueryRepository queryRepository,
      @Autowired(required = false) WorkflowSecurityScopeValidator scopeValidator) {
    this.systemEventLogRepository = systemEventLogRepository;
    this.refSystemEventCategoryRepository = refSystemEventCategoryRepository;
    this.refSystemEventSeverityRepository = refSystemEventSeverityRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
    this.queryRepository = queryRepository;
    this.scopeValidator = scopeValidator;
  }

  @Override
  @Transactional
  public SystemEventLogResponse recordSystemEvent(CreateSystemEventLogRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getSystemEventCategoryId() == null) {
      throw new BadRequestException("System event category ID is required", "CATEGORY_ID_REQUIRED");
    }
    if (request.getSystemEventSeverityId() == null) {
      throw new BadRequestException("System event severity ID is required", "SEVERITY_ID_REQUIRED");
    }
    if (request.getEventCode() == null || request.getEventCode().isBlank()) {
      throw new BadRequestException("Event code is required", "EVENT_CODE_REQUIRED");
    }
    if (request.getEventCode().trim().length() > 64) {
      throw new BadRequestException("Event code must not exceed 64 characters", "EVENT_CODE_TOO_LONG");
    }
    if (request.getEventSummary() == null || request.getEventSummary().isBlank()) {
      throw new BadRequestException("Event summary is required", "EVENT_SUMMARY_REQUIRED");
    }
    if (request.getEventSummary().trim().length() > 500) {
      throw new BadRequestException("Event summary must not exceed 500 characters", "EVENT_SUMMARY_TOO_LONG");
    }
    if (request.getEntityType() != null && request.getEntityType().trim().length() > 64) {
      throw new BadRequestException("Entity type must not exceed 64 characters", "ENTITY_TYPE_TOO_LONG");
    }
    if (request.getSourceComponent() != null && request.getSourceComponent().trim().length() > 64) {
      throw new BadRequestException("Source component must not exceed 64 characters", "SOURCE_COMPONENT_TOO_LONG");
    }

    RefSystemEventCategory category = refSystemEventCategoryRepository.findById(request.getSystemEventCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventCategory", "id"));

    RefSystemEventSeverity severity = refSystemEventSeverityRepository.findById(request.getSystemEventSeverityId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventSeverity", "id"));

    if (request.getActorUserId() != null && !userRepository.existsById(request.getActorUserId())) {
      throw new ResourceNotFoundException("User", "id");
    }

    LocalDateTime occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now();

    String corrId = resolveValidContextCorrelationId();
    String reqId = resolveValidContextRequestId(corrId);
    String formattedSummary = SystemEventSanitizer.formatWithCorrelation(
        request.getEventSummary(), request.getEventCode(), corrId, reqId);
    String safeEventCode = SystemEventSanitizer.sanitizeEventCode(request.getEventCode(), "SYSTEM_EVENT");
    String safeEntityType = SystemEventSanitizer.sanitizeIdentifier(request.getEntityType());
    String safeSourceComponent = SystemEventSanitizer.sanitizeIdentifier(request.getSourceComponent());

    SystemEventLog logEntry = new SystemEventLog(
        category,
        severity,
        safeEventCode,
        formattedSummary,
        occurredAt
    );
    logEntry.setActorUserId(request.getActorUserId());
    logEntry.setEntityType(safeEntityType);
    logEntry.setEntityId(request.getEntityId());
    logEntry.setSourceComponent(safeSourceComponent);

    SystemEventLog saved = systemEventLogRepository.save(logEntry);
    log.info("Recorded system event log id={}, eventCode={}", saved.getId(), saved.getEventCode());
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SystemEventLogResponse recordSystemEventFromApi(CreateSystemEventLogRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    CallerTenantScope scope = resolveAndValidateScope(true);
    if (request.getSystemEventCategoryId() == null) {
      throw new BadRequestException("System event category ID is required", "CATEGORY_ID_REQUIRED");
    }
    if (request.getSystemEventSeverityId() == null) {
      throw new BadRequestException("System event severity ID is required", "SEVERITY_ID_REQUIRED");
    }
    if (request.getEventCode() == null || request.getEventCode().isBlank()) {
      throw new BadRequestException("Event code is required", "EVENT_CODE_REQUIRED");
    }
    if (request.getEventCode().trim().length() > 64) {
      throw new BadRequestException("Event code must not exceed 64 characters", "EVENT_CODE_TOO_LONG");
    }
    if (request.getEventSummary() == null || request.getEventSummary().isBlank()) {
      throw new BadRequestException("Event summary is required", "EVENT_SUMMARY_REQUIRED");
    }
    if (request.getEventSummary().trim().length() > 500) {
      throw new BadRequestException("Event summary must not exceed 500 characters", "EVENT_SUMMARY_TOO_LONG");
    }
    if (!scope.nationwideAdmin()) {
      validateApiWriteEntityScope(scope, request.getEntityType(), request.getEntityId());
    }

    RefSystemEventCategory category = refSystemEventCategoryRepository.findById(request.getSystemEventCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventCategory", "id"));
    RefSystemEventSeverity severity = refSystemEventSeverityRepository.findById(request.getSystemEventSeverityId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventSeverity", "id"));

    // Never trust client-supplied actorUserId or occurredAt over API
    LocalDateTime serverNow = LocalDateTime.now();
    String corrId = resolveValidContextCorrelationId();
    String reqId = resolveValidContextRequestId(corrId);
    String safeCode = SystemEventSanitizer.sanitizeEventCode(request.getEventCode(), "SYSTEM_EVENT");
    String formattedSummary = SystemEventSanitizer.formatWithCorrelation(
        request.getEventSummary(), safeCode, corrId, reqId);

    SystemEventLog entry = new SystemEventLog(category, severity, safeCode, formattedSummary, serverNow);
    entry.setActorUserId(scope.userId());
    entry.setEntityType(SystemEventSanitizer.sanitizeIdentifier(request.getEntityType()));
    entry.setEntityId(request.getEntityId());
    entry.setSourceComponent(SystemEventSanitizer.sanitizeIdentifier(
        request.getSourceComponent() != null ? request.getSourceComponent() : "API"));
    entry.setCreatedAt(serverNow);

    SystemEventLog saved = systemEventLogRepository.save(entry);
    log.info("Recorded API system event log id={}, eventCode={}, actorUserId={}",
        saved.getId(), saved.getEventCode(), saved.getActorUserId());
    return mapper.toResponse(saved);
  }

  @Override
  public Page<SystemEventLogResponse> searchEvents(
      OperationalEventFilterCriteria criteria,
      Long categoryId,
      Long severityId) {
    OperationalEventFilterCriteria effectiveCriteria = criteria != null ? criteria : new OperationalEventFilterCriteria();
    if (!effectiveCriteria.hasValidDateRange()) {
      throw new BadRequestException("Start date cannot be after end date", "INVALID_DATE_RANGE");
    }
    validateFilterIdentifiers(effectiveCriteria);
    CallerTenantScope scope = resolveAndValidateScope(false);
    Pageable pageable = PaginationUtils.buildPageable(
        effectiveCriteria.getPage(),
        effectiveCriteria.getSize(),
        effectiveCriteria.getSort(),
        effectiveCriteria.getDirection(),
        PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS,
        "occurredAt",
        Sort.Direction.DESC
    );
    return requireQueryRepository().searchSystemEvents(effectiveCriteria, categoryId, severityId, scope, pageable);
  }

  @Override
  public SystemEventLogResponse getEventById(Long id) {
    if (id == null) {
      throw new BadRequestException("Event ID is required");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      SystemEventLog event = systemEventLogRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("SystemEventLog", "id"));
      return mapper.toResponse(event);
    }
    return queryRepository.findScopedEventById(id, scope)
        .orElseThrow(() -> {
          if (systemEventLogRepository.existsById(id)) {
            return new AccessDeniedException("Access denied to system event outside caller tenant scope");
          }
          return new ResourceNotFoundException("SystemEventLog", "id");
        });
  }

  @Override
  public List<SystemEventLogResponse> getEventsByCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new BadRequestException("Category ID is required");
    }
    return getEventsByCategoryId(categoryId, defaultBoundedPageable()).getContent();
  }

  @Override
  public List<SystemEventLogResponse> getEventsBySeverityId(Long severityId) {
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    return getEventsBySeverityId(severityId, defaultBoundedPageable()).getContent();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByEventCode(String eventCode) {
    if (eventCode == null || eventCode.isBlank()) {
      throw new BadRequestException("Event code is required");
    }
    return getEventsByEventCode(eventCode, defaultBoundedPageable()).getContent();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByActorUserId(Long actorUserId) {
    if (actorUserId == null) {
      throw new BadRequestException("Actor user ID is required");
    }
    return getEventsByActorUserId(actorUserId, defaultBoundedPageable()).getContent();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    return getEventsByEntityType(entityType, defaultBoundedPageable()).getContent();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByEntity(String entityType, Long entityId) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    return getEventsByEntity(entityType, entityId, defaultBoundedPageable()).getContent();
  }

  @Override
  public List<SystemEventLogResponse> getEventsBySourceComponent(String sourceComponent) {
    if (sourceComponent == null || sourceComponent.isBlank()) {
      throw new BadRequestException("Source component is required");
    }
    return getEventsBySourceComponent(sourceComponent, defaultBoundedPageable()).getContent();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByOccurredAtBetween(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      throw new BadRequestException("Start and end dates are required");
    }
    if (start.isAfter(end)) {
      throw new BadRequestException("Start date cannot be after end date");
    }
    return getEventsByOccurredAtBetween(start, end, defaultBoundedPageable()).getContent();
  }

  @Override
  public Page<SystemEventLogResponse> getAllEvents(Pageable pageable) {
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findAll(pageable).map(mapper::toResponse);
    }
    return queryRepository.searchSystemEvents(new OperationalEventFilterCriteria(), null, null, scope, pageable);
  }

  @Override
  public Page<SystemEventLogResponse> getEventsByCategoryId(Long categoryId, Pageable pageable) {
    if (categoryId == null) {
      throw new BadRequestException("Category ID is required");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findBySystemEventCategoryId(categoryId, pageable).map(mapper::toResponse);
    }
    return queryRepository.searchSystemEvents(new OperationalEventFilterCriteria(), categoryId, null, scope, pageable);
  }

  @Override
  public Page<SystemEventLogResponse> getEventsBySeverityId(Long severityId, Pageable pageable) {
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findBySystemEventSeverityId(severityId, pageable).map(mapper::toResponse);
    }
    return queryRepository.searchSystemEvents(new OperationalEventFilterCriteria(), null, severityId, scope, pageable);
  }

  @Override
  public Page<SystemEventLogResponse> getEventsByEventCode(String eventCode, Pageable pageable) {
    if (eventCode == null || eventCode.isBlank()) {
      throw new BadRequestException("Event code is required");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findByEventCode(eventCode.trim(), pageable).map(mapper::toResponse);
    }
    OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria();
    criteria.setEventCode(eventCode.trim());
    return queryRepository.searchSystemEvents(criteria, null, null, scope, pageable);
  }

  @Override
  public Page<SystemEventLogResponse> getEventsByActorUserId(Long actorUserId, Pageable pageable) {
    if (actorUserId == null) {
      throw new BadRequestException("Actor user ID is required");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findByActorUserId(actorUserId, pageable).map(mapper::toResponse);
    }
    OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria();
    criteria.setActorUserId(actorUserId);
    return queryRepository.searchSystemEvents(criteria, null, null, scope, pageable);
  }

  @Override
  public Page<SystemEventLogResponse> getEventsByEntityType(String entityType, Pageable pageable) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findByEntityType(entityType.trim(), pageable).map(mapper::toResponse);
    }
    OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria();
    criteria.setEntityType(entityType.trim());
    return queryRepository.searchSystemEvents(criteria, null, null, scope, pageable);
  }

  @Override
  public Page<SystemEventLogResponse> getEventsByEntity(String entityType, Long entityId, Pageable pageable) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId, pageable).map(mapper::toResponse);
    }
    OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria();
    criteria.setEntityType(entityType.trim());
    criteria.setEntityId(entityId);
    return queryRepository.searchSystemEvents(criteria, null, null, scope, pageable);
  }

  @Override
  public Page<SystemEventLogResponse> getEventsBySourceComponent(String sourceComponent, Pageable pageable) {
    if (sourceComponent == null || sourceComponent.isBlank()) {
      throw new BadRequestException("Source component is required");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findBySourceComponent(sourceComponent.trim(), pageable).map(mapper::toResponse);
    }
    OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria();
    criteria.setSourceComponent(sourceComponent.trim());
    return queryRepository.searchSystemEvents(criteria, null, null, scope, pageable);
  }

  @Override
  public Page<SystemEventLogResponse> getEventsByOccurredAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable) {
    if (start == null || end == null) {
      throw new BadRequestException("Start and end dates are required");
    }
    if (start.isAfter(end)) {
      throw new BadRequestException("Start date cannot be after end date");
    }
    CallerTenantScope scope = resolveAndValidateScope(false);
    if (queryRepository == null) {
      return systemEventLogRepository.findByOccurredAtBetween(start, end, pageable).map(mapper::toResponse);
    }
    OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria();
    criteria.setStart(start);
    criteria.setEnd(end);
    return queryRepository.searchSystemEvents(criteria, null, null, scope, pageable);
  }

  private SystemEventLogQueryRepository requireQueryRepository() {
    if (queryRepository == null) {
      throw new AccessDeniedException("Diagnostic query infrastructure unavailable");
    }
    return queryRepository;
  }

  private Pageable defaultBoundedPageable() {
    return PageRequest.of(0, LEGACY_LIST_BOUND, Sort.by(Sort.Direction.DESC, "occurredAt"));
  }

  private CallerTenantScope resolveAndValidateScope(boolean requireWriteAuthority) {
    if (scopeValidator == null) {
      throw new AccessDeniedException("Security validator unavailable to authorize system event access");
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("Authentication is required to access system event logs");
    }

    Set<String> authorities = auth.getAuthorities() != null
        ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
        : Set.of();

    if (requireWriteAuthority) {
      if (!authorities.contains("system.manage") && !authorities.contains("system.event.manage")) {
        throw new AccessDeniedException("Caller lacks required management authority for system event creation");
      }
    } else {
      if (!authorities.contains("audit.read")
          && !authorities.contains("system.manage")
          && !authorities.contains("system.event.read")
          && !authorities.contains("system.event.manage")) {
        throw new AccessDeniedException("Caller lacks required diagnostic read authority");
      }
    }

    if (scopeValidator.isTraineeCaller() || authorities.contains("ROLE_trainee")) {
      throw new AccessDeniedException("Trainees are not authorized to access administrative system events");
    }

    Long userId = scopeValidator.resolveAuthenticatedUserId();
    User user = scopeValidator.resolveAuthenticatedUser()
        .orElseThrow(() -> new AccessDeniedException("Authoritative user record not found"));
    if (user.getDeletedAt() != null) {
      throw new AccessDeniedException("Deleted user cannot access system event diagnostics");
    }

    if (scopeValidator.isNationwideAdmin()) {
      return new CallerTenantScope(true, userId, null, null, null, null);
    }

    boolean roleProvider = authorities.contains("ROLE_training_provider");
    boolean roleEmployer = authorities.contains("ROLE_employer");
    boolean roleStateAdmin = authorities.contains("ROLE_state_admin") || authorities.contains("ROLE_government_admin");
    boolean roleOfficer = authorities.contains("ROLE_district_officer") || authorities.contains("ROLE_government_officer");

    int personaCount = (roleProvider ? 1 : 0) + (roleEmployer ? 1 : 0) + ((roleStateAdmin || roleOfficer) ? 1 : 0);
    if (personaCount > 1) {
      throw new AccessDeniedException("Caller holds conflicting persona roles");
    }

    boolean hasState = user.getStateId() != null;
    boolean hasDistrict = user.getDistrictId() != null;
    boolean hasProvider = user.getTrainingProviderId() != null;
    boolean hasEmployer = user.getEmployerId() != null;

    if ((roleProvider && !hasProvider) || (roleEmployer && !hasEmployer) || ((roleStateAdmin || roleOfficer) && !hasState)) {
      throw new AccessDeniedException("Caller lacks required authoritative tenant scope");
    }
    int rootCount = (hasState ? 1 : 0) + (hasProvider ? 1 : 0) + (hasEmployer ? 1 : 0);
    if (rootCount != 1) {
      throw new AccessDeniedException("Caller lacks an unambiguous tenant boundary for diagnostic queries");
    }
    if ((hasProvider || hasEmployer) && hasDistrict) {
      throw new AccessDeniedException("Caller has conflicting organizational and district tenancy");
    }
    if (hasState && (roleProvider || roleEmployer)) {
      throw new AccessDeniedException("Caller has conflicting role and state tenancy");
    }

    return new CallerTenantScope(
        false,
        userId,
        user.getStateId(),
        user.getDistrictId(),
        user.getTrainingProviderId(),
        user.getEmployerId());
  }

  private void validateFilterIdentifiers(OperationalEventFilterCriteria criteria) {
    if (criteria.getCategoryCode() != null && !criteria.getCategoryCode().isBlank()) {
      String code = criteria.getCategoryCode().trim().toUpperCase(Locale.ROOT);
      if (!VALID_CATEGORIES.contains(code)) {
        throw new BadRequestException("Unsupported system event categoryCode: " + code, "INVALID_EVENT_CATEGORY");
      }
    }
    if (criteria.getSeverityCode() != null && !criteria.getSeverityCode().isBlank()) {
      String code = criteria.getSeverityCode().trim().toUpperCase(Locale.ROOT);
      if (!VALID_SEVERITIES.contains(code)) {
        throw new BadRequestException("Unsupported system event severityCode: " + code, "INVALID_EVENT_SEVERITY");
      }
    }
    if (criteria.getCorrelationId() != null && !criteria.getCorrelationId().isBlank()) {
      if (!CorrelationIdFilter.isValidCorrelationId(criteria.getCorrelationId().trim())) {
        throw new BadRequestException("Invalid correlationId filter format", "INVALID_CORRELATION_ID");
      }
    }
    if (criteria.getRequestId() != null && !criteria.getRequestId().isBlank()) {
      if (!CorrelationIdFilter.isValidCorrelationId(criteria.getRequestId().trim())) {
        throw new BadRequestException("Invalid requestId filter format", "INVALID_REQUEST_ID");
      }
    }
  }

  private void validateApiWriteEntityScope(CallerTenantScope scope, String entityType, Long entityId) {
    if (entityType == null && entityId == null) {
      return;
    }
    if (entityType != null) {
      String normalized = entityType.trim().toUpperCase(Locale.ROOT).replace("_", "");
      if ("TRAININGPROVIDER".equals(normalized) && scope.trainingProviderId() != null
          && Objects.equals(entityId, scope.trainingProviderId())) {
        return;
      }
      if ("EMPLOYER".equals(normalized) && scope.employerId() != null
          && Objects.equals(entityId, scope.employerId())) {
        return;
      }
      if ("STATE".equals(normalized) && scope.stateId() != null && scope.districtId() == null
          && Objects.equals(entityId, scope.stateId())) {
        return;
      }
      if ("USER".equals(normalized) && Objects.equals(entityId, scope.userId())) {
        return;
      }
    }
    throw new AccessDeniedException("Caller cannot manually record system events targeting cross-tenant or system entities");
  }

  private String resolveValidContextCorrelationId() {
    String corrId = CorrelationContextHolder.getCorrelationId();
    if (!CorrelationIdFilter.isValidCorrelationId(corrId)) {
      String mdc = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
      return CorrelationIdFilter.isValidCorrelationId(mdc) ? mdc : null;
    }
    return corrId;
  }

  private String resolveValidContextRequestId(String fallbackCorrelationId) {
    String reqId = CorrelationContextHolder.getRequestId();
    if (!CorrelationIdFilter.isValidCorrelationId(reqId)) {
      String mdc = MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
      if (CorrelationIdFilter.isValidCorrelationId(mdc)) {
        return mdc;
      }
      return fallbackCorrelationId;
    }
    return reqId;
  }
}
