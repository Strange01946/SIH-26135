package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.CenterScorecardResponse;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.dto.response.ProviderDataQualityIssueResponse;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.DataQualityIssueEvent;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidDataQualityTransitionException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.DataQualityIssueEventRepository;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.ProviderDataQualityQueryRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.security.AuditPayloadMasker;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.DataQualityEngineService;
import in.gov.sih.sih26135.service.ProviderDataQualityService;
import in.gov.sih.sih26135.service.dq.ScanScopeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
@Service
@Transactional
public class ProviderDataQualityServiceImpl implements ProviderDataQualityService {

  private static final Logger log = LoggerFactory.getLogger(ProviderDataQualityServiceImpl.class);

  private final ProviderDataQualityQueryRepository queryRepository;
  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final DataQualityIssueEventRepository dataQualityIssueEventRepository;
  private final RefDataQualityIssueStatusRepository statusRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final JdbcTemplate jdbcTemplate;

  @Autowired(required = false)
  private DataQualityEngineService dataQualityEngineService;

  public ProviderDataQualityServiceImpl(
      ProviderDataQualityQueryRepository queryRepository,
      DataQualityIssueRepository dataQualityIssueRepository,
      DataQualityIssueEventRepository dataQualityIssueEventRepository,
      RefDataQualityIssueStatusRepository statusRepository,
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate) {
    this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository is required");
    this.dataQualityIssueRepository = Objects.requireNonNull(dataQualityIssueRepository, "dataQualityIssueRepository is required");
    this.dataQualityIssueEventRepository = Objects.requireNonNull(dataQualityIssueEventRepository, "dataQualityIssueEventRepository is required");
    this.statusRepository = Objects.requireNonNull(statusRepository, "statusRepository is required");
    this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator is required");
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
    if (page < 0) {
      throw new BadRequestException("Page index cannot be negative", "INVALID_PAGE");
    }
    if (size < 1 || size > 100) {
      throw new BadRequestException("Page size must be between 1 and 100", "INVALID_SIZE");
    }

    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    if (!queryRepository.isIssueOwnedByProvider(issueId, providerId)) {
      boolean exists = dataQualityIssueRepository.existsById(issueId);
      if (exists) {
        throw new AccessDeniedException("Data quality issue does not belong to caller's provider scope");
      }
      throw new ResourceNotFoundException("DataQualityIssue", "id=" + issueId);
    }

    int offset = page * size;
    return queryRepository.findIssueEventsPaged(issueId, size, offset);
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
        ? issue.getDataQualityIssueStatus().getStatusCode().toUpperCase(Locale.ROOT)
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
        ? issue.getDataQualityIssueStatus().getStatusCode().toUpperCase(Locale.ROOT)
        : "OPEN";

    if ("RESOLVED".equals(currentStatus) || "DISMISSED".equals(currentStatus)) {
      throw new InvalidDataQualityTransitionException(currentStatus, "RESOLVED");
    }

    // MANDATORY RESOLUTION INTEGRITY:
    // Verify that the underlying record actually satisfies data quality rules before marking RESOLVED!
    verifyUnderlyingAnomalyCorrected(issue);

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
        ? issue.getDataQualityIssueStatus().getStatusCode().toUpperCase(Locale.ROOT)
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
  public List<DataQualityRuleExecutionSummary> triggerReEvaluation(Long issueId) {
    User caller = resolveAndValidateProviderCaller();
    Long providerId = caller.getTrainingProviderId();

    DataQualityScanRequest scanRequest = new DataQualityScanRequest();

    if (issueId != null && issueId > 0) {
      validateIssueId(issueId);
      if (!queryRepository.isIssueOwnedByProvider(issueId, providerId)) {
        boolean exists = dataQualityIssueRepository.existsById(issueId);
        if (exists) {
          throw new AccessDeniedException("Data quality issue does not belong to caller's provider scope");
        }
        throw new ResourceNotFoundException("DataQualityIssue", "id=" + issueId);
      }
      DataQualityIssue issue = dataQualityIssueRepository.findById(issueId)
          .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id=" + issueId));

      String ruleCode = issue.getDataQualityRule() != null ? issue.getDataQualityRule().getRuleCode() : null;
      String entityType = issue.getEntityType();

      if (entityType != null) {
        String normalized = entityType.trim().toUpperCase(Locale.ROOT).replace("_", "");
        if (!Set.of("TRAINEE", "PLACEMENTRECORD", "EMPLOYMENTRECORD", "ATTENDANCERECORD").contains(normalized)) {
          throw new BadRequestException("Data quality re-evaluation is not supported for entity type " + entityType + " or rule " + ruleCode, "UNSUPPORTED_RE_EVALUATION");
        }
      }

      Set<String> supportedRules = Set.of(
          "DQ-TRAINEE-001",
          "DQ-ATTENDANCE-004",
          "DQ-PLACEMENT-002",
          "DQ-EMPLOYMENT-003",
          "DQ-DUPLICATE-005"
      );
      if (ruleCode != null && !supportedRules.contains(ruleCode)) {
        throw new BadRequestException("Data quality re-evaluation is not supported for rule " + ruleCode, "UNSUPPORTED_RE_EVALUATION");
      }

      scanRequest.setRuleCode(ruleCode);
      scanRequest.setTargetEntityType(entityType);
      scanRequest.setEntityId(issue.getEntityId());
    }

    if (dataQualityEngineService == null) {
      throw new BadRequestException("Data quality engine service is unavailable", "SERVICE_UNAVAILABLE");
    }

    // Explicitly construct provider-scoped context
    ScanScopeContext providerScope = ScanScopeContext.providerScoped(caller.getId(), providerId);

    List<DataQualityRuleExecutionSummary> summaries;
    try {
      summaries = dataQualityEngineService.executeScopedScan(scanRequest, providerScope);
    } catch (AccessDeniedException ade) {
      throw ade;
    } catch (BadRequestException bre) {
      throw bre;
    } catch (Exception e) {
      log.error("Provider re-evaluation failed for provider {}: {}", providerId, e.getMessage(), e);
      throw new BadRequestException("Data quality re-evaluation failed: " + e.getMessage(), "SCAN_FAILED");
    }

    if (summaries != null) {
      for (DataQualityRuleExecutionSummary s : summaries) {
        if ("FAILED".equalsIgnoreCase(s.status())) {
          log.warn("Rule {} execution failed during provider scan", s.ruleCode());
          throw new BadRequestException("Data quality re-evaluation failed for rule: " + s.ruleCode(), "SCAN_FAILED");
        }
        if (issueId != null && "SKIPPED".equalsIgnoreCase(s.status())) {
          log.warn("Rule {} skipped during provider scan", s.ruleCode());
          throw new BadRequestException("Data quality re-evaluation is not supported for rule: " + s.ruleCode(), "UNSUPPORTED_RE_EVALUATION");
        }
      }
    }

    return summaries;
  }

  private void verifyUnderlyingAnomalyCorrected(DataQualityIssue issue) {
    Long entityId = issue.getEntityId();
    if (entityId == null || entityId <= 0) {
      throw new BadRequestException("Issue target entityId is invalid", "REMEDIATION_VALIDATION_FAILED");
    }

    String entityType = issue.getEntityType() != null ? issue.getEntityType().trim() : "";
    String normalizedEntityType = entityType.toUpperCase(Locale.ROOT).replace("_", "");

    String ruleCode = issue.getDataQualityRule() != null ? issue.getDataQualityRule().getRuleCode() : null;
    if (ruleCode == null || ruleCode.isBlank()) {
      throw new BadRequestException("Data quality issue rule code is missing and cannot be validated", "UNSUPPORTED_RULE_VALIDATION");
    }
    String normalizedRuleCode = ruleCode.trim().toUpperCase(Locale.ROOT);

    switch (normalizedRuleCode) {
      case "DQ-DUPLICATE-005": {
        // Cross-provider duplicate candidate registrations cannot be directly resolved by a single provider
        throw new AccessDeniedException(
            "Cross-provider duplicate candidate registration issues require administrative supervisor review and cannot be directly resolved by a single training provider");
      }

      case "DQ-TRAINEE-001": {
        // Missing Trainee Contact Information (phone or email)
        if (!"TRAINEE".equals(normalizedEntityType)) {
          throw new BadRequestException(
              "Target entity type '" + entityType + "' is invalid for rule " + ruleCode,
              "UNSUPPORTED_RULE_VALIDATION");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT trainee_id, phone, email FROM trainees WHERE trainee_id = ? AND deleted_at IS NULL",
            entityId);
        if (rows.isEmpty()) {
          throw new ResourceNotFoundException("Trainee", "id=" + entityId);
        }
        Map<String, Object> traineeRow = rows.get(0);
        String phone = (String) traineeRow.get("phone");
        String email = (String) traineeRow.get("email");
        boolean hasPhone = phone != null && !phone.trim().isEmpty();
        boolean hasEmail = email != null && !email.trim().isEmpty();
        if (!hasPhone && !hasEmail) {
          throw new BadRequestException(
              "Underlying Trainee #" + entityId + " still lacks contact details: both phone and email are missing",
              "REMEDIATION_VALIDATION_FAILED");
        }
        return;
      }

      case "DQ-ATTENDANCE-004": {
        // Attendance Outside Batch Operational Boundaries
        if (!"ATTENDANCERECORD".equals(normalizedEntityType)) {
          throw new BadRequestException(
              "Target entity type '" + entityType + "' is invalid for rule " + ruleCode,
              "UNSUPPORTED_RULE_VALIDATION");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT ar.attendance_id, ar.session_date, tb.start_date, tb.end_date " +
            "FROM attendance_records ar " +
            "JOIN training_batches tb ON ar.batch_id = tb.batch_id " +
            "WHERE ar.attendance_id = ?",
            entityId);
        if (rows.isEmpty()) {
          throw new ResourceNotFoundException("AttendanceRecord", "id=" + entityId);
        }
        Map<String, Object> attendanceRow = rows.get(0);
        Date sessionDate = (Date) attendanceRow.get("session_date");
        Date batchStartDate = (Date) attendanceRow.get("start_date");
        Date batchEndDate = (Date) attendanceRow.get("end_date");

        if (sessionDate == null || batchStartDate == null) {
          throw new BadRequestException(
              "Underlying AttendanceRecord #" + entityId + " has missing session date or batch start date",
              "REMEDIATION_VALIDATION_FAILED");
        }
        if (sessionDate.before(batchStartDate)) {
          throw new BadRequestException(
              "Underlying AttendanceRecord #" + entityId + " session date (" + sessionDate +
                  ") precedes batch start date (" + batchStartDate + ")",
              "REMEDIATION_VALIDATION_FAILED");
        }
        if (batchEndDate != null && sessionDate.after(batchEndDate)) {
          throw new BadRequestException(
              "Underlying AttendanceRecord #" + entityId + " session date (" + sessionDate +
                  ") occurs after batch end date (" + batchEndDate + ")",
              "REMEDIATION_VALIDATION_FAILED");
        }
        return;
      }

      case "DQ-PLACEMENT-002": {
        // Placement Temporal Incoherence
        if (!"PLACEMENTRECORD".equals(normalizedEntityType)) {
          throw new BadRequestException(
              "Target entity type '" + entityType + "' is invalid for rule " + ruleCode,
              "UNSUPPORTED_RULE_VALIDATION");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT pr.placement_id, pr.offer_date, pr.actual_joining_date, te.enrollment_date " +
            "FROM placement_records pr " +
            "JOIN training_enrollments te ON pr.enrollment_id = te.enrollment_id " +
            "WHERE pr.placement_id = ? AND pr.deleted_at IS NULL",
            entityId);
        if (rows.isEmpty()) {
          throw new ResourceNotFoundException("PlacementRecord", "id=" + entityId);
        }
        Map<String, Object> placementRow = rows.get(0);
        Date offerDate = (Date) placementRow.get("offer_date");
        Date actualJoiningDate = (Date) placementRow.get("actual_joining_date");
        Date enrollmentDate = (Date) placementRow.get("enrollment_date");

        if (offerDate != null && enrollmentDate != null && offerDate.before(enrollmentDate)) {
          throw new BadRequestException(
              "Underlying PlacementRecord #" + entityId + " offer date (" + offerDate + ") precedes enrollment date (" + enrollmentDate + ")",
              "REMEDIATION_VALIDATION_FAILED");
        }
        if (actualJoiningDate != null && offerDate != null && actualJoiningDate.before(offerDate)) {
          throw new BadRequestException(
              "Underlying PlacementRecord #" + entityId + " joining date (" + actualJoiningDate + ") precedes offer date (" + offerDate + ")",
              "REMEDIATION_VALIDATION_FAILED");
        }
        if (actualJoiningDate != null && enrollmentDate != null && actualJoiningDate.before(enrollmentDate)) {
          throw new BadRequestException(
              "Underlying PlacementRecord #" + entityId + " joining date (" + actualJoiningDate + ") precedes enrollment date (" + enrollmentDate + ")",
              "REMEDIATION_VALIDATION_FAILED");
        }
        return;
      }

      case "DQ-EMPLOYMENT-003": {
        // Invalid or Suspicious Salary
        if (!"EMPLOYMENTRECORD".equals(normalizedEntityType)) {
          throw new BadRequestException(
              "Target entity type '" + entityType + "' is invalid for rule " + ruleCode,
              "UNSUPPORTED_RULE_VALIDATION");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT er.employment_id, er.starting_salary, er.work_state_id, sf.frequency_code " +
            "FROM employment_records er " +
            "LEFT JOIN ref_salary_frequency sf ON er.salary_frequency_id = sf.salary_frequency_id " +
            "WHERE er.employment_id = ? AND er.deleted_at IS NULL",
            entityId);
        if (rows.isEmpty()) {
          throw new ResourceNotFoundException("EmploymentRecord", "id=" + entityId);
        }
        Map<String, Object> empRow = rows.get(0);
        Object salaryObj = empRow.get("starting_salary");
        if (salaryObj == null) {
          throw new BadRequestException(
              "Underlying EmploymentRecord #" + entityId + " starting salary is missing",
              "REMEDIATION_VALIDATION_FAILED");
        }
        BigDecimal salary = new BigDecimal(salaryObj.toString());
        if (salary.compareTo(BigDecimal.ZERO) <= 0) {
          throw new BadRequestException(
              "Underlying EmploymentRecord #" + entityId + " starting salary (" + salary + ") must be greater than zero",
              "REMEDIATION_VALIDATION_FAILED");
        }
        return;
      }

      case "DQ-CENTER-001": {
        // Training Center Profile and Capacity Validity
        if (!"TRAININGCENTER".equals(normalizedEntityType)) {
          throw new BadRequestException(
              "Target entity type '" + entityType + "' is invalid for rule " + ruleCode,
              "UNSUPPORTED_RULE_VALIDATION");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT center_id, center_name, state_id, district_id, operational_capacity FROM training_centers WHERE center_id = ? AND deleted_at IS NULL",
            entityId);
        if (rows.isEmpty()) {
          throw new ResourceNotFoundException("TrainingCenter", "id=" + entityId);
        }
        Map<String, Object> centerRow = rows.get(0);
        String centerName = (String) centerRow.get("center_name");
        Number capacity = (Number) centerRow.get("operational_capacity");

        if (centerName == null || centerName.trim().isEmpty()) {
          throw new BadRequestException(
              "Underlying TrainingCenter #" + entityId + " center name cannot be blank",
              "REMEDIATION_VALIDATION_FAILED");
        }
        if (capacity != null && capacity.intValue() < 0) {
          throw new BadRequestException(
              "Underlying TrainingCenter #" + entityId + " has invalid operational capacity: " + capacity,
              "REMEDIATION_VALIDATION_FAILED");
        }
        return;
      }

      case "DQ-ENROLLMENT-001": {
        // Enrollment Temporal Chronology
        if (!"TRAININGENROLLMENT".equals(normalizedEntityType)) {
          throw new BadRequestException(
              "Target entity type '" + entityType + "' is invalid for rule " + ruleCode,
              "UNSUPPORTED_RULE_VALIDATION");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT enrollment_id, enrollment_date, start_date, expected_completion_date, actual_completion_date " +
            "FROM training_enrollments WHERE enrollment_id = ? AND deleted_at IS NULL",
            entityId);
        if (rows.isEmpty()) {
          throw new ResourceNotFoundException("TrainingEnrollment", "id=" + entityId);
        }
        Map<String, Object> enrollmentRow = rows.get(0);
        Date enrollmentDate = (Date) enrollmentRow.get("enrollment_date");
        Date startDate = (Date) enrollmentRow.get("start_date");
        Date expectedCompletionDate = (Date) enrollmentRow.get("expected_completion_date");
        Date actualCompletionDate = (Date) enrollmentRow.get("actual_completion_date");

        if (actualCompletionDate != null && startDate != null && actualCompletionDate.before(startDate)) {
          throw new BadRequestException(
              "Underlying TrainingEnrollment #" + entityId + " still violates chronological rules: actualCompletionDate (" +
                  actualCompletionDate + ") precedes startDate (" + startDate + ")",
              "REMEDIATION_VALIDATION_FAILED");
        }
        if (expectedCompletionDate != null && startDate != null && expectedCompletionDate.before(startDate)) {
          throw new BadRequestException(
              "Underlying TrainingEnrollment #" + entityId + " still violates chronological rules: expectedCompletionDate (" +
                  expectedCompletionDate + ") precedes startDate (" + startDate + ")",
              "REMEDIATION_VALIDATION_FAILED");
        }
        if (startDate != null && enrollmentDate != null && startDate.before(enrollmentDate)) {
          throw new BadRequestException(
              "Underlying TrainingEnrollment #" + entityId + " still violates chronological rules: startDate (" +
                  startDate + ") precedes enrollmentDate (" + enrollmentDate + ")",
              "REMEDIATION_VALIDATION_FAILED");
        }
        return;
      }

      default: {
        // Fail closed for any unknown, future, custom, or unsupported rule code
        throw new BadRequestException(
            "Data quality issue with rule code '" + ruleCode + "' cannot be automatically verified for provider resolution",
            "UNSUPPORTED_RULE_VALIDATION");
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
