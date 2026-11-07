package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AuditLogFilterCriteria;
import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.dto.response.AuditTrailTimelineResponse;
import in.gov.sih.sih26135.entity.AuditLog;
import in.gov.sih.sih26135.entity.RefAuditAction;
import in.gov.sih.sih26135.event.listener.AuditTrailEventListener;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AuditLogMapper;
import in.gov.sih.sih26135.repository.AuditLogQueryRepository;
import in.gov.sih.sih26135.repository.AuditLogRepository;
import in.gov.sih.sih26135.repository.RefAuditActionRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

  private static final Set<String> ALLOWED_TIMELINE_ENTITY_TYPES = Set.of(
      "PLACEMENT", "PLACEMENTRECORD",
      "TRAINING", "TRAININGENROLLMENT", "ASSESSMENTRESULT",
      "VERIFICATION", "EMPLOYMENTVERIFICATION",
      "FOLLOWUP", "FOLLOWUPTASK", "SURVEYRESPONSE",
      "SKILLGAP", "SKILLGAPRECOMMENDATION",
      "EMPLOYMENT", "EMPLOYMENTRECORD",
      "USER"
  );

  private final AuditLogRepository auditLogRepository;
  private final RefAuditActionRepository refAuditActionRepository;
  private final UserRepository userRepository;
  private final AuditLogMapper mapper;
  private final AuditLogQueryRepository auditLogQueryRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final JdbcTemplate jdbcTemplate;

  public AuditLogServiceImpl(
      AuditLogRepository auditLogRepository,
      RefAuditActionRepository refAuditActionRepository,
      UserRepository userRepository,
      AuditLogMapper mapper,
      AuditLogQueryRepository auditLogQueryRepository,
      @Autowired(required = false) WorkflowSecurityScopeValidator scopeValidator,
      @Autowired(required = false) JdbcTemplate jdbcTemplate) {
    this.auditLogRepository = auditLogRepository;
    this.refAuditActionRepository = refAuditActionRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
    this.auditLogQueryRepository = auditLogQueryRepository;
    this.scopeValidator = scopeValidator;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public AuditLogResponse recordAuditLog(CreateAuditLogRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getAuditActionId() == null) {
      throw new BadRequestException("Audit action ID is required", "AUDIT_ACTION_REQUIRED");
    }
    RefAuditAction auditAction = refAuditActionRepository.findById(request.getAuditActionId())
        .orElseThrow(() -> new ResourceNotFoundException("RefAuditAction", "auditActionId"));

    if (request.getEntityType() == null || request.getEntityType().isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }

    Long actorUserId = request.getActorUserId();
    if (scopeValidator != null) {
      Long authenticatedUserId = scopeValidator.resolveAuthenticatedUserId();
      boolean isAdmin = scopeValidator.isAdministrativeCaller() || scopeValidator.isNationwideAdmin();
      // If not nationwide admin, prevent client forgery of actor user ID
      if (!isAdmin && authenticatedUserId != null) {
        actorUserId = authenticatedUserId;
      }
    }

    if (actorUserId != null) {
      if (!userRepository.existsById(actorUserId)) {
        throw new ResourceNotFoundException("User", "actorUserId");
      }
    }

    LocalDateTime occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now();
    String sanitizedSummary = AuditTrailEventListener.sanitizeChangeSummary(request.getChangeSummary());

    AuditLog auditLog = new AuditLog(auditAction, request.getEntityType().trim(), occurredAt);
    auditLog.setActorUserId(actorUserId);
    auditLog.setEntityId(request.getEntityId());
    auditLog.setCorrelationId(request.getCorrelationId());
    auditLog.setChangeSummary(sanitizedSummary);
    auditLog.setCreatedAt(LocalDateTime.now());

    AuditLog saved = auditLogRepository.save(auditLog);
    return mapper.toResponse(saved);
  }

  private boolean isCallerSystemAdmin() {
    org.springframework.security.core.Authentication auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getAuthorities() != null) {
      Set<String> auths = auth.getAuthorities().stream()
          .map(org.springframework.security.core.GrantedAuthority::getAuthority)
          .collect(java.util.stream.Collectors.toSet());

      boolean isRestrictedPersona = auths.contains("ROLE_training_provider")
          || auths.contains("ROLE_employer")
          || auths.contains("ROLE_trainee")
          || auths.contains("ROLE_district_officer")
          || auths.contains("ROLE_government_officer");

      if (!isRestrictedPersona) {
        boolean hasAdminAuth = auths.contains("audit.read")
            || auths.contains("system.manage")
            || auths.stream().anyMatch(a -> a.equalsIgnoreCase("ROLE_super_admin")
                || a.equalsIgnoreCase("ROLE_government_admin")
                || a.equalsIgnoreCase("ROLE_admin"));
        if (hasAdminAuth) {
          return true;
        }
      }
    }
    if (scopeValidator != null) {
      return scopeValidator.isAdministrativeCaller() || scopeValidator.isNationwideAdmin();
    }
    return false;
  }

  @Override
  public Page<AuditLogResponse> searchAuditLogs(AuditLogFilterCriteria criteria) {
    if (criteria == null) {
      criteria = new AuditLogFilterCriteria();
    }

    // 1. Validate temporal range
    if (!criteria.hasValidDateRange() || (criteria.getStart() != null && criteria.getEnd() != null && criteria.getStart().isAfter(criteria.getEnd()))) {
      throw new BadRequestException("Start timestamp cannot be after end timestamp", "INVALID_DATE_RANGE");
    }

    // 2. Validate tenant scope
    boolean isAdmin = isCallerSystemAdmin();
    if (!isAdmin) {
      if (criteria.getEntityType() == null || criteria.getEntityId() == null) {
        throw new AccessDeniedException("Unscoped audit log queries are restricted to administrative callers");
      }
      validateEntityAccess(criteria.getEntityType(), criteria.getEntityId());
    }

    // 3. Build pageable
    int page = Math.max(0, criteria.getPage());
    int size = (criteria.getSize() >= 1 && criteria.getSize() <= 100) ? criteria.getSize() : 20;
    String sortProp = criteria.getSort() != null && !criteria.getSort().isBlank() ? criteria.getSort() : "occurredAt";
    Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.getDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

    return auditLogQueryRepository.searchAuditLogs(criteria, pageable);
  }

  @Override
  public AuditTrailTimelineResponse getEntityTimeline(String entityType, Long entityId) {
    // 1. Validate entity type against allowlist
    String normalizedType = normalizeTimelineEntityType(entityType);

    // 2. Validate entity ID
    if (entityId == null || entityId <= 0) {
      throw new BadRequestException("Entity ID must be positive", "INVALID_ENTITY_ID");
    }

    // 3. Determine caller's authorization to inspect entity
    boolean isAdmin = isCallerSystemAdmin();
    if (!isAdmin) {
      validateEntityAccess(normalizedType, entityId);
    }

    // 4. Retrieve and reconstruct chronological timeline
    return auditLogQueryRepository.getEntityTimeline(normalizedType, entityId);
  }

  private String normalizeTimelineEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }
    String upper = entityType.trim().toUpperCase().replace("_", "");
    if (!ALLOWED_TIMELINE_ENTITY_TYPES.contains(upper)) {
      throw new BadRequestException(
          "Unsupported entity type: " + entityType + ". Supported types are: Placement, TrainingEnrollment, EmploymentVerification, FollowupTask, SkillGapRecommendation, User, EmploymentRecord",
          "UNSUPPORTED_ENTITY_TYPE"
      );
    }
    return switch (upper) {
      case "PLACEMENT", "PLACEMENTRECORD" -> "Placement";
      case "TRAINING", "TRAININGENROLLMENT" -> "TrainingEnrollment";
      case "ASSESSMENTRESULT" -> "AssessmentResult";
      case "VERIFICATION", "EMPLOYMENTVERIFICATION" -> "EmploymentVerification";
      case "FOLLOWUP", "FOLLOWUPTASK" -> "FollowupTask";
      case "SURVEYRESPONSE" -> "SurveyResponse";
      case "SKILLGAP", "SKILLGAPRECOMMENDATION" -> "SkillGapRecommendation";
      case "EMPLOYMENT", "EMPLOYMENTRECORD" -> "EmploymentRecord";
      case "USER" -> "User";
      default -> entityType.trim();
    };
  }

  private void validateEntityAccess(String entityType, Long entityId) {
    if (jdbcTemplate == null || scopeValidator == null) {
      return;
    }
    String upper = entityType.trim().toUpperCase().replace("_", "");
    try {
      switch (upper) {
        case "PLACEMENT", "PLACEMENTRECORD" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT employer_id, trainee_id FROM placement_records WHERE placement_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("PlacementRecord", "id");
          Long empId = rows.get(0).get("employer_id") != null ? ((Number) rows.get(0).get("employer_id")).longValue() : null;
          Long trId = rows.get(0).get("trainee_id") != null ? ((Number) rows.get(0).get("trainee_id")).longValue() : null;
          if (scopeValidator.isEmployerCaller() && empId != null) {
            scopeValidator.validateEmployerScope(empId);
          } else if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateTraineeScope(trId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "TRAINING", "TRAININGENROLLMENT" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT provider_id, trainee_id FROM training_enrollments WHERE enrollment_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("TrainingEnrollment", "id");
          Long provId = rows.get(0).get("provider_id") != null ? ((Number) rows.get(0).get("provider_id")).longValue() : null;
          Long trId = rows.get(0).get("trainee_id") != null ? ((Number) rows.get(0).get("trainee_id")).longValue() : null;
          if (scopeValidator.isTrainingProviderCaller() && provId != null) {
            scopeValidator.validateProviderScope(provId);
          } else if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateTraineeScope(trId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "VERIFICATION", "EMPLOYMENTVERIFICATION" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT e.employer_id, ev.verifier_user_id FROM employment_verifications ev JOIN employment_records e ON ev.employment_record_id = e.employment_record_id WHERE ev.employment_verification_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("EmploymentVerification", "id");
          Long empId = rows.get(0).get("employer_id") != null ? ((Number) rows.get(0).get("employer_id")).longValue() : null;
          if (scopeValidator.isEmployerCaller() && empId != null) {
            scopeValidator.validateEmployerScope(empId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "FOLLOWUP", "FOLLOWUPTASK" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT trainee_id FROM followup_tasks WHERE followup_task_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("FollowupTask", "id");
          Long trId = rows.get(0).get("trainee_id") != null ? ((Number) rows.get(0).get("trainee_id")).longValue() : null;
          if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateTraineeScope(trId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "SKILLGAP", "SKILLGAPRECOMMENDATION" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT trainee_id FROM skill_gap_recommendations WHERE skill_gap_recommendation_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("SkillGapRecommendation", "id");
          Long trId = rows.get(0).get("trainee_id") != null ? ((Number) rows.get(0).get("trainee_id")).longValue() : null;
          if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateTraineeScope(trId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "EMPLOYMENT", "EMPLOYMENTRECORD" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT employer_id, trainee_id FROM employment_records WHERE employment_record_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("EmploymentRecord", "id");
          Long empId = rows.get(0).get("employer_id") != null ? ((Number) rows.get(0).get("employer_id")).longValue() : null;
          Long trId = rows.get(0).get("trainee_id") != null ? ((Number) rows.get(0).get("trainee_id")).longValue() : null;
          if (scopeValidator.isEmployerCaller() && empId != null) {
            scopeValidator.validateEmployerScope(empId);
          } else if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateTraineeScope(trId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "USER" -> {
          Long authUserId = scopeValidator.resolveAuthenticatedUserId();
          if (authUserId == null || !authUserId.equals(entityId)) {
            throw new AccessDeniedException("Callers may only inspect their own user audit history");
          }
        }
        default -> {
          // If entity type is unknown or unscoped, enforce administrative authority
          scopeValidator.validateNationwideScope();
        }
      }
    } catch (ResourceNotFoundException | AccessDeniedException e) {
      throw e;
    } catch (Exception e) {
      throw new AccessDeniedException("Access validation failed for entity " + entityType + ":" + entityId, e);
    }
  }

  @Override
  public AuditLogResponse getAuditLogById(Long id) {
    if (id == null) {
      throw new BadRequestException("Audit log ID is required");
    }
    AuditLog entity = auditLogRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByActorUserId(Long actorUserId) {
    if (actorUserId == null) {
      throw new BadRequestException("Actor user ID is required");
    }
    return auditLogRepository.findByActorUserId(actorUserId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByActionId(Long auditActionId) {
    if (auditActionId == null) {
      throw new BadRequestException("Audit action ID is required");
    }
    return auditLogRepository.findByAuditActionId(auditActionId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    return auditLogRepository.findByEntityType(entityType.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    return auditLogRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId) {
    if (correlationId == null || correlationId.isBlank()) {
      throw new BadRequestException("Correlation ID is required");
    }
    return auditLogRepository.findByCorrelationId(correlationId.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByOccurredAtBetween(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      throw new BadRequestException("Start and end timestamps are required");
    }
    if (start.isAfter(end)) {
      throw new BadRequestException("Start timestamp cannot be after end timestamp", "INVALID_DATE_RANGE");
    }
    return auditLogRepository.findByOccurredAtBetween(start, end).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
    return auditLogRepository.findAll(pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByActorUserId(Long actorUserId, Pageable pageable) {
    if (actorUserId == null) {
      throw new BadRequestException("Actor user ID is required");
    }
    return auditLogRepository.findByActorUserId(actorUserId, pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByActionId(Long auditActionId, Pageable pageable) {
    if (auditActionId == null) {
      throw new BadRequestException("Audit action ID is required");
    }
    return auditLogRepository.findByAuditActionId(auditActionId, pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByEntityType(String entityType, Pageable pageable) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    return auditLogRepository.findByEntityType(entityType.trim(), pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    return auditLogRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId, pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId, Pageable pageable) {
    if (correlationId == null || correlationId.isBlank()) {
      throw new BadRequestException("Correlation ID is required");
    }
    return auditLogRepository.findByCorrelationId(correlationId.trim(), pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByOccurredAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable) {
    if (start == null || end == null) {
      throw new BadRequestException("Start and end timestamps are required");
    }
    if (start.isAfter(end)) {
      throw new BadRequestException("Start timestamp cannot be after end timestamp", "INVALID_DATE_RANGE");
    }
    return auditLogRepository.findByOccurredAtBetween(start, end, pageable)
        .map(mapper::toResponse);
  }
}
