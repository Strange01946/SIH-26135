package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.dto.request.AuditLogFilterCriteria;
import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.dto.response.AuditTrailTimelineResponse;
import in.gov.sih.sih26135.entity.AuditLog;
import in.gov.sih.sih26135.entity.RefAuditAction;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AuditLogMapper;
import in.gov.sih.sih26135.repository.AuditLogQueryRepository;
import in.gov.sih.sih26135.repository.AuditLogRepository;
import in.gov.sih.sih26135.repository.RefAuditActionRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.AuditPayloadMasker;
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
      "TRAININGCENTER", "TRAININGBATCH", "IMPORTBATCH",
      "USER"
  );

  private static final Set<String> WORKFLOW_AUTOMATED_ACTIONS = Set.of(
      "STATUS_CHANGE"
  );

  private static final Set<String> SUPPORTED_MANUAL_ACTION_CODES = Set.of(
      "UPDATE",
      "EXPORT"
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

    org.springframework.security.core.Authentication auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    boolean isAuthenticatedApiCaller = auth != null && auth.isAuthenticated()
        && !"anonymousUser".equals(auth.getPrincipal());

    if (!isAuthenticatedApiCaller || scopeValidator == null) {
      throw new AccessDeniedException("Authenticated user identity is required to record audit log");
    }

    Long actorUserId = scopeValidator.resolveAuthenticatedUserId();
    if (actorUserId == null) {
      throw new AccessDeniedException("Authenticated user identity could not be resolved from server context");
    }

    // 2. Prevent manual API callers from fabricating automated workflow transitions
    String actionCode = auditAction.getActionCode() != null ? auditAction.getActionCode().toUpperCase().trim() : "";
    if (WORKFLOW_AUTOMATED_ACTIONS.contains(actionCode)) {
      throw new BadRequestException(
          "Automated workflow audit actions (" + actionCode + ") cannot be recorded manually via API",
          "UNAUTHORIZED_WORKFLOW_ACTION"
      );
    }

    // 3. Restrict manual administrative audit entries to explicitly supported action categories
    if (!SUPPORTED_MANUAL_ACTION_CODES.contains(actionCode)) {
      throw new BadRequestException(
          "Audit action " + actionCode + " is not supported for manual audit creation. Supported actions: " + SUPPORTED_MANUAL_ACTION_CODES,
          "UNSUPPORTED_MANUAL_ACTION"
      );
    }

    // 4. Validate that the target entity and scope are authorized
    String normalizedType = normalizeTimelineEntityType(request.getEntityType());
    if (request.getEntityId() != null) {
      validateEntityAccess(normalizedType, request.getEntityId());
    }

    // Derive occurrence timestamp strictly from trusted server clock (ignore client-supplied occurredAt)
    LocalDateTime occurredAt = LocalDateTime.now();

    // Derive correlation ID strictly from server context (ignore client-supplied correlationId)
    String correlationId = CorrelationContextHolder.getCorrelationId();

    // Sanitize change summary with AuditPayloadMasker (1000 char VARCHAR limit)
    String sanitizedSummary = AuditPayloadMasker.maskBounded(request.getChangeSummary(), 1000);

    AuditLog auditLog = new AuditLog(auditAction, request.getEntityType().trim(), occurredAt);
    auditLog.setActorUserId(actorUserId);
    auditLog.setEntityId(request.getEntityId());
    auditLog.setCorrelationId(correlationId);
    auditLog.setChangeSummary(sanitizedSummary);
    auditLog.setCreatedAt(LocalDateTime.now());

    AuditLog saved = auditLogRepository.save(auditLog);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public AuditLogResponse recordInternalAuditLog(Long actorUserId, Long auditActionId, String entityType, Long entityId, String summary) {
    if (auditActionId == null) {
      throw new BadRequestException("Audit action ID is required", "AUDIT_ACTION_REQUIRED");
    }
    RefAuditAction auditAction = refAuditActionRepository.findById(auditActionId)
        .orElseThrow(() -> new ResourceNotFoundException("RefAuditAction", "auditActionId"));

    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }

    Long verifiedActorUserId = null;
    if (actorUserId != null && userRepository != null && userRepository.existsById(actorUserId)) {
      verifiedActorUserId = actorUserId;
    }

    LocalDateTime occurredAt = LocalDateTime.now();
    String correlationId = CorrelationContextHolder.getCorrelationId();
    String sanitizedSummary = AuditPayloadMasker.maskBounded(summary, 1000);

    AuditLog auditLog = new AuditLog(auditAction, entityType.trim(), occurredAt);
    auditLog.setActorUserId(verifiedActorUserId);
    auditLog.setEntityId(entityId);
    auditLog.setCorrelationId(correlationId);
    auditLog.setChangeSummary(sanitizedSummary);
    auditLog.setCreatedAt(LocalDateTime.now());

    AuditLog saved = auditLogRepository.save(auditLog);
    return mapper.toResponse(saved);
  }

  private boolean isCallerNationwideAdmin() {
    if (scopeValidator == null) {
      return false;
    }
    return scopeValidator.isNationwideAdmin();
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

    // 2. Validate tenant scope - fail closed
    boolean isNationwideAdmin = isCallerNationwideAdmin();
    if (!isNationwideAdmin) {
      if (criteria.getEntityType() == null || criteria.getEntityId() == null) {
        throw new AccessDeniedException("Unscoped audit log queries are restricted to nationwide administrative callers");
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

    // 3. Determine caller's authorization to inspect entity - fail closed
    boolean isNationwideAdmin = isCallerNationwideAdmin();
    if (!isNationwideAdmin) {
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
      case "TRAININGCENTER" -> "TrainingCenter";
      case "TRAININGBATCH" -> "TrainingBatch";
      case "IMPORTBATCH" -> "ImportBatch";
      case "USER" -> "User";
      default -> entityType.trim();
    };
  }

  private void validateStateOrDistrictAdminAccess(Long stateId, Long districtId) {
    User caller = scopeValidator.resolveAuthenticatedUser()
        .orElseThrow(() -> new AccessDeniedException("Authoritative user record not found"));
    if (caller.getDistrictId() != null) {
      if (stateId == null || caller.getStateId() == null || !caller.getStateId().equals(stateId)
          || districtId == null || !caller.getDistrictId().equals(districtId)) {
        throw new AccessDeniedException("Entity is outside caller assigned district scope");
      }
      return;
    }
    scopeValidator.validateStateScope(stateId);
  }

  private void validateEntityAccess(String entityType, Long entityId) {
    if (jdbcTemplate == null || scopeValidator == null) {
      throw new AccessDeniedException("Security dependencies unavailable to validate entity scope");
    }
    String upper = entityType.trim().toUpperCase().replace("_", "");
    try {
      switch (upper) {
        case "PLACEMENT", "PLACEMENTRECORD" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT p.employer_id, p.provider_id, p.trainee_id, COALESCE(p.work_state_id, t.state_id) AS state_id, COALESCE(p.work_district_id, t.district_id) AS district_id "
                  + "FROM placement_records p LEFT JOIN trainees t ON p.trainee_id = t.trainee_id WHERE p.placement_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("PlacementRecord", "id");
          Long empId = toLong(rows.get(0).get("employer_id"));
          Long provId = toLong(rows.get(0).get("provider_id"));
          Long trId = toLong(rows.get(0).get("trainee_id"));
          Long stateId = toLong(rows.get(0).get("state_id"));
          Long districtId = toLong(rows.get(0).get("district_id"));
          if (scopeValidator.isEmployerCaller() && empId != null) {
            scopeValidator.validateDashboardEmployerScope(empId);
          } else if (scopeValidator.isTrainingProviderCaller() && provId != null) {
            scopeValidator.validateDashboardProviderScope(provId);
          } else if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateDashboardTraineeScope(trId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "TRAINING", "TRAININGENROLLMENT" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT te.provider_id, te.trainee_id, tc.state_id, tc.district_id "
                  + "FROM training_enrollments te LEFT JOIN training_batches tb ON te.batch_id = tb.batch_id "
                  + "LEFT JOIN training_centers tc ON tb.center_id = tc.center_id WHERE te.enrollment_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("TrainingEnrollment", "id");
          Long provId = toLong(rows.get(0).get("provider_id"));
          Long trId = toLong(rows.get(0).get("trainee_id"));
          Long stateId = toLong(rows.get(0).get("state_id"));
          Long districtId = toLong(rows.get(0).get("district_id"));
          if (scopeValidator.isTrainingProviderCaller() && provId != null) {
            scopeValidator.validateDashboardProviderScope(provId);
          } else if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateDashboardTraineeScope(trId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "VERIFICATION", "EMPLOYMENTVERIFICATION" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT e.employer_id, te.provider_id, ev.verifier_user_id, e.trainee_id, e.work_state_id, e.work_district_id "
                  + "FROM employment_verifications ev JOIN employment_records e ON ev.employment_record_id = e.employment_id "
                  + "LEFT JOIN training_enrollments te ON e.enrollment_id = te.enrollment_id WHERE ev.employment_verification_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("EmploymentVerification", "id");
          Long empId = toLong(rows.get(0).get("employer_id"));
          Long provId = toLong(rows.get(0).get("provider_id"));
          Long trId = toLong(rows.get(0).get("trainee_id"));
          Long stateId = toLong(rows.get(0).get("work_state_id"));
          Long districtId = toLong(rows.get(0).get("work_district_id"));
          if (scopeValidator.isEmployerCaller() && empId != null) {
            scopeValidator.validateDashboardEmployerScope(empId);
          } else if (scopeValidator.isTrainingProviderCaller() && provId != null) {
            scopeValidator.validateDashboardProviderScope(provId);
          } else if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateDashboardTraineeScope(trId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "FOLLOWUP", "FOLLOWUPTASK" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT ft.trainee_id, t.state_id, t.district_id FROM followup_tasks ft LEFT JOIN trainees t ON ft.trainee_id = t.trainee_id WHERE ft.followup_task_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("FollowupTask", "id");
          Long trId = toLong(rows.get(0).get("trainee_id"));
          Long stateId = toLong(rows.get(0).get("state_id"));
          Long districtId = toLong(rows.get(0).get("district_id"));
          if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateDashboardTraineeScope(trId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "SKILLGAP", "SKILLGAPRECOMMENDATION" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT sgr.trainee_id, t.state_id, t.district_id FROM skill_gap_recommendations sgr LEFT JOIN trainees t ON sgr.trainee_id = t.trainee_id WHERE sgr.skill_gap_recommendation_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("SkillGapRecommendation", "id");
          Long trId = toLong(rows.get(0).get("trainee_id"));
          Long stateId = toLong(rows.get(0).get("state_id"));
          Long districtId = toLong(rows.get(0).get("district_id"));
          if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateDashboardTraineeScope(trId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "EMPLOYMENT", "EMPLOYMENTRECORD" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT e.employer_id, te.provider_id, e.trainee_id, e.work_state_id, e.work_district_id "
                  + "FROM employment_records e LEFT JOIN training_enrollments te ON e.enrollment_id = te.enrollment_id WHERE e.employment_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("EmploymentRecord", "id");
          Long empId = toLong(rows.get(0).get("employer_id"));
          Long provId = toLong(rows.get(0).get("provider_id"));
          Long trId = toLong(rows.get(0).get("trainee_id"));
          Long stateId = toLong(rows.get(0).get("work_state_id"));
          Long districtId = toLong(rows.get(0).get("work_district_id"));
          if (scopeValidator.isEmployerCaller() && empId != null) {
            scopeValidator.validateDashboardEmployerScope(empId);
          } else if (scopeValidator.isTrainingProviderCaller() && provId != null) {
            scopeValidator.validateDashboardProviderScope(provId);
          } else if (scopeValidator.isTraineeCaller() && trId != null) {
            scopeValidator.validateDashboardTraineeScope(trId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "TRAININGCENTER" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT provider_id, state_id, district_id FROM training_centers WHERE center_id = ? AND deleted_at IS NULL", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("TrainingCenter", "id");
          Long provId = toLong(rows.get(0).get("provider_id"));
          Long stateId = toLong(rows.get(0).get("state_id"));
          Long districtId = toLong(rows.get(0).get("district_id"));
          if (scopeValidator.isTrainingProviderCaller() && provId != null) {
            scopeValidator.validateDashboardProviderScope(provId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "TRAININGBATCH" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT tb.provider_id, tc.state_id, tc.district_id FROM training_batches tb "
                  + "LEFT JOIN training_centers tc ON tb.center_id = tc.center_id WHERE tb.batch_id = ? AND tb.deleted_at IS NULL", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("TrainingBatch", "id");
          Long provId = toLong(rows.get(0).get("provider_id"));
          Long stateId = toLong(rows.get(0).get("state_id"));
          Long districtId = toLong(rows.get(0).get("district_id"));
          if (scopeValidator.isTrainingProviderCaller() && provId != null) {
            scopeValidator.validateDashboardProviderScope(provId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "IMPORTBATCH" -> {
          List<Map<String, Object>> rows = jdbcTemplate.queryForList(
              "SELECT u.training_provider_id, u.employer_id, COALESCE(u.state_id, tp.state_id, emp.state_id) AS state_id, "
                  + "COALESCE(u.district_id, tp.district_id, emp.district_id) AS district_id "
                  + "FROM import_batches ib LEFT JOIN users u ON ib.initiated_by_user_id = u.user_id "
                  + "LEFT JOIN training_providers tp ON u.training_provider_id = tp.provider_id "
                  + "LEFT JOIN employers emp ON u.employer_id = emp.employer_id WHERE ib.import_batch_id = ?", entityId);
          if (rows.isEmpty()) throw new ResourceNotFoundException("ImportBatch", "id");
          Long provId = toLong(rows.get(0).get("training_provider_id"));
          Long empId = toLong(rows.get(0).get("employer_id"));
          Long stateId = toLong(rows.get(0).get("state_id"));
          Long districtId = toLong(rows.get(0).get("district_id"));
          if (scopeValidator.isEmployerCaller() && empId != null) {
            scopeValidator.validateDashboardEmployerScope(empId);
          } else if (scopeValidator.isTrainingProviderCaller() && provId != null) {
            scopeValidator.validateDashboardProviderScope(provId);
          } else if (scopeValidator.isStateAdmin() && stateId != null) {
            validateStateOrDistrictAdminAccess(stateId, districtId);
          } else {
            scopeValidator.validateNationwideScope();
          }
        }
        case "USER" -> {
          Long authUserId = scopeValidator.resolveAuthenticatedUserId();
          if (authUserId == null || !authUserId.equals(entityId)) {
            scopeValidator.validateNationwideScope();
          }
        }
        default -> {
          scopeValidator.validateNationwideScope();
        }
      }
    } catch (ResourceNotFoundException | AccessDeniedException e) {
      throw e;
    } catch (Exception e) {
      throw new AccessDeniedException("Access validation failed for entity " + entityType + ":" + entityId, e);
    }
  }

  private static Long toLong(Object val) {
    return val instanceof Number n ? n.longValue() : null;
  }

  @Override
  public AuditLogResponse getAuditLogById(Long id) {
    if (id == null) {
      throw new BadRequestException("Audit log ID is required", "AUDIT_LOG_ID_REQUIRED");
    }
    AuditLog entity = auditLogRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id"));

    // Enforce consistent entity-level authorization for non-nationwide-admins
    if (!isCallerNationwideAdmin()) {
      if (entity.getEntityType() == null || entity.getEntityId() == null) {
        throw new AccessDeniedException("Access denied to unscoped audit log entry");
      }
      validateEntityAccess(entity.getEntityType(), entity.getEntityId());
    }

    return mapper.toResponse(entity);
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByActorUserId(Long actorUserId) {
    if (actorUserId == null) {
      throw new BadRequestException("Actor user ID is required", "ACTOR_USER_ID_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Actor audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByActorUserId(actorUserId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByActionId(Long auditActionId) {
    if (auditActionId == null) {
      throw new BadRequestException("Audit action ID is required", "AUDIT_ACTION_ID_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByAuditActionId(auditActionId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries by entity type are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByEntityType(entityType.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required", "ENTITY_ID_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      validateEntityAccess(entityType, entityId);
    }
    return auditLogRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId) {
    if (correlationId == null || correlationId.isBlank()) {
      throw new BadRequestException("Correlation ID is required", "CORRELATION_ID_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByCorrelationId(correlationId.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByOccurredAtBetween(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      throw new BadRequestException("Start and end timestamps are required", "TIMESTAMPS_REQUIRED");
    }
    if (start.isAfter(end)) {
      throw new BadRequestException("Start timestamp cannot be after end timestamp", "INVALID_DATE_RANGE");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByOccurredAtBetween(start, end).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findAll(pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByActorUserId(Long actorUserId, Pageable pageable) {
    if (actorUserId == null) {
      throw new BadRequestException("Actor user ID is required", "ACTOR_USER_ID_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Actor audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByActorUserId(actorUserId, pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByActionId(Long auditActionId, Pageable pageable) {
    if (auditActionId == null) {
      throw new BadRequestException("Audit action ID is required", "AUDIT_ACTION_ID_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByAuditActionId(auditActionId, pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByEntityType(String entityType, Pageable pageable) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries by entity type are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByEntityType(entityType.trim(), pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required", "ENTITY_ID_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      validateEntityAccess(entityType, entityId);
    }
    return auditLogRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId, pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId, Pageable pageable) {
    if (correlationId == null || correlationId.isBlank()) {
      throw new BadRequestException("Correlation ID is required", "CORRELATION_ID_REQUIRED");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByCorrelationId(correlationId.trim(), pageable)
        .map(mapper::toResponse);
  }

  @Override
  public Page<AuditLogResponse> getAuditLogsByOccurredAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable) {
    if (start == null || end == null) {
      throw new BadRequestException("Start and end timestamps are required", "TIMESTAMPS_REQUIRED");
    }
    if (start.isAfter(end)) {
      throw new BadRequestException("Start timestamp cannot be after end timestamp", "INVALID_DATE_RANGE");
    }
    if (!isCallerNationwideAdmin()) {
      throw new AccessDeniedException("Unscoped audit log queries are restricted to nationwide administrative callers");
    }
    return auditLogRepository.findByOccurredAtBetween(start, end, pageable)
        .map(mapper::toResponse);
  }
}
