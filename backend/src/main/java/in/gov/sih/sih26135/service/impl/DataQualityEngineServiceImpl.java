package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.DataQualityEngineService;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import in.gov.sih.sih26135.service.dq.DataQualityDetector;
import in.gov.sih.sih26135.service.dq.ScanScopeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Production implementation of DataQualityEngineService.
 *
 * <p>Orchestrates rule resolution, caller authorization, tenant scope validation,
 * detector execution, and metric reporting.
 */
@Service
public class DataQualityEngineServiceImpl implements DataQualityEngineService {

  private static final Logger log = LoggerFactory.getLogger(DataQualityEngineServiceImpl.class);

  private static final Set<String> ALLOWED_TARGET_ENTITY_TYPES = Set.of(
      "TRAINEE",
      "PLACEMENTRECORD",
      "EMPLOYMENTRECORD",
      "ATTENDANCERECORD"
  );

  private final DataQualityRuleCatalogService catalogService;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final Map<String, DataQualityDetector> detectorRegistry = new HashMap<>();

  public DataQualityEngineServiceImpl(
      DataQualityRuleCatalogService catalogService,
      WorkflowSecurityScopeValidator scopeValidator,
      List<DataQualityDetector> detectors) {
    this.catalogService = catalogService;
    this.scopeValidator = scopeValidator;
    if (detectors != null) {
      for (DataQualityDetector detector : detectors) {
        this.detectorRegistry.put(detector.getSupportedRuleCode(), detector);
      }
    }
  }

  @Override
  public List<DataQualityRuleExecutionSummary> executeScan(DataQualityScanRequest request) {
    // 1. Authorize caller and determine tenancy boundary
    ScanScopeContext scopeContext = resolveAndValidateScope(request);

    // 2. Validate request parameters
    validateScanRequest(request);

    // 3. Resolve target rules to evaluate
    List<DataQualityRule> rules = catalogService.resolveRulesForScan(request);
    if (rules.isEmpty()) {
      return List.of();
    }

    List<DataQualityRuleExecutionSummary> summaries = new ArrayList<>();

    // 4. Execute detectors for each resolved rule
    for (DataQualityRule rule : rules) {
      DataQualityDetector detector = detectorRegistry.get(rule.getRuleCode());
      if (detector == null) {
        log.warn("No executable detector registered for rule code: {}", rule.getRuleCode());
        summaries.add(new DataQualityRuleExecutionSummary(
            rule.getId(),
            rule.getRuleCode(),
            rule.getTargetEntityType(),
            0L,
            0L,
            0L,
            0L,
            LocalDateTime.now(),
            0L,
            "SKIPPED"
        ));
        continue;
      }

      try {
        DataQualityRuleExecutionSummary summary = detector.executeScan(rule, request, scopeContext);
        summaries.add(summary);
      } catch (Exception e) {
        log.error("Rule execution failed for {}: {}", rule.getRuleCode(), e.getMessage(), e);
        // Partial failure isolation: capture failure summary and continue evaluating other rules
        summaries.add(new DataQualityRuleExecutionSummary(
            rule.getId(),
            rule.getRuleCode(),
            rule.getTargetEntityType(),
            0L,
            0L,
            0L,
            0L,
            LocalDateTime.now(),
            0L,
            "FAILED"
        ));
      }
    }

    return summaries;
  }

  private ScanScopeContext resolveAndValidateScope(DataQualityScanRequest request) {
    if (scopeValidator == null) {
      throw new AccessDeniedException("Security validator unavailable to verify caller authorization");
    }

    Long callerUserId = scopeValidator.resolveAuthenticatedUserId();
    if (callerUserId == null) {
      throw new AccessDeniedException("Authenticated user identity is required to execute data quality scans");
    }

    boolean isNationwide = scopeValidator.isNationwideAdmin();
    Optional<User> userOpt = scopeValidator.resolveAuthenticatedUser();
    User user = userOpt.orElse(null);

    Long stateId = user != null ? user.getStateId() : null;
    Long providerId = user != null ? user.getTrainingProviderId() : null;
    Long employerId = user != null ? user.getEmployerId() : null;

    boolean isStateAdmin = scopeValidator.isStateAdmin();
    boolean isProvider = scopeValidator.isTrainingProviderCaller();
    boolean isEmployer = scopeValidator.isEmployerCaller();

    if (!isNationwide) {
      // Non-nationwide callers are strictly prohibited from unscoped platform-wide scanning
      if (!isStateAdmin && !isProvider && !isEmployer) {
        throw new AccessDeniedException("Caller lacks administrative or scoped operational authority to execute data quality scans");
      }
      return new ScanScopeContext(
          callerUserId,
          false,
          isStateAdmin,
          stateId,
          isProvider,
          providerId,
          isEmployer,
          employerId
      );
    }

    return ScanScopeContext.nationwide(callerUserId);
  }

  private void validateScanRequest(DataQualityScanRequest request) {
    if (request == null) {
      return;
    }

    if (request.getTargetEntityType() != null && !request.getTargetEntityType().isBlank()) {
      String normalized = request.getTargetEntityType().trim().toUpperCase().replace("_", "");
      if (!ALLOWED_TARGET_ENTITY_TYPES.contains(normalized)) {
        throw new BadRequestException(
            "Unsupported target entity type: " + request.getTargetEntityType() +
                ". Supported types: Trainee, PlacementRecord, EmploymentRecord, AttendanceRecord",
            "UNSUPPORTED_ENTITY_TYPE"
        );
      }
    }
  }
}
