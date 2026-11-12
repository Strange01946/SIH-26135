package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder.ContextSnapshot;
import in.gov.sih.sih26135.correlation.CorrelationIdFilter;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.entity.RefDataQualitySeverity;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.DataQualityEngineService;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import in.gov.sih.sih26135.service.SystemEventLoggingService;
import in.gov.sih.sih26135.service.SystemEventLoggingService.EventSeverity;
import in.gov.sih.sih26135.service.dq.DataQualityDetector;
import in.gov.sih.sih26135.service.dq.ScanScopeContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

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

  private static final Set<String> EMPLOYER_ALLOWED_RULES = Set.of(
      "DQ-PLACEMENT-002",
      "DQ-EMPLOYMENT-003"
  );

  private final DataQualityRuleCatalogService catalogService;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final Map<String, DataQualityDetector> detectorRegistry = new HashMap<>();

  @Autowired(required = false)
  private DataQualityEngineProperties engineProperties;

  @Autowired(required = false)
  private SystemEventLoggingService systemEventLoggingService;

  @Autowired
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
    ContextSnapshot previousSnapshot = CorrelationContextHolder.captureSnapshot();
    String previousMdcCorr = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
    String previousMdcReq = MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
    boolean generatedServerCorrelation = false;
    if (!CorrelationIdFilter.isValidCorrelationId(previousSnapshot.correlationId())) {
      String serverCorr = UUID.randomUUID().toString();
      CorrelationContextHolder.setCorrelationId(serverCorr);
      CorrelationContextHolder.setRequestId(serverCorr);
      MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, serverCorr);
      MDC.put(CorrelationIdFilter.MDC_REQUEST_ID_KEY, serverCorr);
      generatedServerCorrelation = true;
    }

    try {
      return executeScanInternal(request);
    } finally {
      if (generatedServerCorrelation) {
        CorrelationContextHolder.restore(previousSnapshot);
        if (previousMdcCorr != null) {
          MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, previousMdcCorr);
        } else {
          MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        }
        if (previousMdcReq != null) {
          MDC.put(CorrelationIdFilter.MDC_REQUEST_ID_KEY, previousMdcReq);
        } else {
          MDC.remove(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
        }
      }
    }
  }

  private List<DataQualityRuleExecutionSummary> executeScanInternal(DataQualityScanRequest request) {
    // 1. Authorize caller and determine tenancy boundary (fails closed on missing scope)
    ScanScopeContext scopeContext = resolveAndValidateScope(request);

    // 2. Validate request parameters and persona-to-scope compatibility
    validateScanRequest(request, scopeContext);

    // 3. Resolve target rules to evaluate
    List<DataQualityRule> rules = catalogService.resolveRulesForScan(request);
    if (rules.isEmpty()) {
      return List.of();
    }

    // 4. If employer caller, restrict rules to employer-accessible domains
    if (scopeContext.isEmployer() && !scopeContext.canScanNationwide()) {
      rules = rules.stream()
          .filter(r -> EMPLOYER_ALLOWED_RULES.contains(r.getRuleCode()))
          .toList();
      if (rules.isEmpty()) {
        return List.of();
      }
    }

    List<DataQualityRuleExecutionSummary> summaries = new ArrayList<>();
    long totalNewIssues = 0L;
    long maxCriticalRuleNewIssues = 0L;
    long maxHighRuleNewIssues = 0L;
    DataQualityRule topSpikingCriticalOrHighRule = null;
    long topSpikingCount = 0L;

    // 5. Execute detectors for each resolved rule
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

        long created = summary != null && summary.newIssuesCreated() != null ? summary.newIssuesCreated() : 0L;
        if (created > 0) {
          totalNewIssues += created;
          String ruleSeverity = resolveRuleSeverityCode(rule);
          if ("CRITICAL".equals(ruleSeverity) && created > maxCriticalRuleNewIssues) {
            maxCriticalRuleNewIssues = created;
          }
          if ("HIGH".equals(ruleSeverity) && created > maxHighRuleNewIssues) {
            maxHighRuleNewIssues = created;
          }
          if (("CRITICAL".equals(ruleSeverity) || "HIGH".equals(ruleSeverity)) && created > topSpikingCount) {
            topSpikingCount = created;
            topSpikingCriticalOrHighRule = rule;
          }
        }
      } catch (AccessDeniedException ade) {
        // Security boundary violations must fail closed immediately
        log.warn("Security boundary violation during execution of rule {}: {}", rule.getRuleCode(), ade.getMessage());
        throw ade;
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

    evaluateAndRecordDataQualitySpikeSafely(
        scopeContext,
        totalNewIssues,
        maxCriticalRuleNewIssues,
        maxHighRuleNewIssues,
        topSpikingCriticalOrHighRule);

    return summaries;
  }

  private String resolveRuleSeverityCode(DataQualityRule rule) {
    if (rule == null || rule.getDataQualitySeverity() == null) {
      return "";
    }
    RefDataQualitySeverity severity = rule.getDataQualitySeverity();
    if (Hibernate.isInitialized(severity)) {
      String code = severity.getSeverityCode();
      return code != null ? code.toUpperCase(Locale.ROOT) : "";
    }
    Long severityId = severity.getId();
    if (Long.valueOf(4L).equals(severityId)) {
      return "CRITICAL";
    }
    if (Long.valueOf(3L).equals(severityId)) {
      return "HIGH";
    }
    if (Long.valueOf(2L).equals(severityId)) {
      return "MEDIUM";
    }
    if (Long.valueOf(1L).equals(severityId)) {
      return "LOW";
    }
    return "";
  }

  private void evaluateAndRecordDataQualitySpikeSafely(
      ScanScopeContext scopeContext,
      long totalNewIssues,
      long maxCriticalRuleNewIssues,
      long maxHighRuleNewIssues,
      DataQualityRule topSpikingRule) {
    if (systemEventLoggingService == null || totalNewIssues <= 0) {
      return;
    }

    int criticalThreshold = engineProperties != null ? engineProperties.getSpikeCriticalSeverityThreshold() : 1;
    int highThreshold = engineProperties != null ? engineProperties.getSpikeHighSeverityThreshold() : 10;
    int newIssuesThreshold = engineProperties != null ? engineProperties.getSpikeNewIssuesThreshold() : 20;
    int cooldownSeconds = engineProperties != null ? engineProperties.getSpikeAlertCooldownSeconds() : 300;

    boolean criticalSpike = maxCriticalRuleNewIssues >= criticalThreshold;
    boolean highVolumeSpike = totalNewIssues >= newIssuesThreshold;
    boolean highRuleSpike = maxHighRuleNewIssues >= highThreshold;
    if (!criticalSpike && !highVolumeSpike && !highRuleSpike) {
      return;
    }

    try {
      String tenantScopeKey;
      String entityType;
      Long entityId;
      if (!scopeContext.canScanNationwide() && scopeContext.isTrainingProvider() && scopeContext.trainingProviderId() != null) {
        tenantScopeKey = "PROVIDER:" + scopeContext.trainingProviderId();
        entityType = "TrainingProvider";
        entityId = scopeContext.trainingProviderId();
      } else if (!scopeContext.canScanNationwide() && scopeContext.isEmployer() && scopeContext.employerId() != null) {
        tenantScopeKey = "EMPLOYER:" + scopeContext.employerId();
        entityType = "Employer";
        entityId = scopeContext.employerId();
      } else if (!scopeContext.canScanNationwide() && scopeContext.isStateAdmin() && scopeContext.stateId() != null) {
        tenantScopeKey = "STATE:" + scopeContext.stateId();
        entityType = "State";
        entityId = scopeContext.stateId();
      } else {
        tenantScopeKey = "NATIONWIDE";
        entityType = "DataQualityRule";
        entityId = topSpikingRule != null ? topSpikingRule.getId() : null;
      }

      EventSeverity severity = (criticalSpike || highVolumeSpike) ? EventSeverity.CRITICAL : EventSeverity.ERROR;
      String ruleTag = topSpikingRule != null ? topSpikingRule.getRuleCode() : "SCAN_AGGREGATE";
      String signalKey = (criticalSpike ? "CRIT:" : highVolumeSpike ? "VOL:" : "HIGH:") + ruleTag;
      String message = "Data-quality anomaly spike detected: scope=" + tenantScopeKey
          + ", newIssuesCreated=" + totalNewIssues
          + ", criticalNew=" + maxCriticalRuleNewIssues
          + ", highNew=" + maxHighRuleNewIssues
          + ", rule=" + ruleTag;

      systemEventLoggingService.logDataQualitySpike(
          severity,
          "DATA_QUALITY_CRITICAL_SPIKE",
          scopeContext.callerUserId(),
          tenantScopeKey,
          signalKey,
          entityType,
          entityId,
          cooldownSeconds,
          message);
    } catch (Exception ex) {
      log.warn("Safe fallback: failed recording data-quality spike event: {}", ex.getClass().getSimpleName());
    }
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
      ScanScopeContext context = new ScanScopeContext(
          callerUserId,
          false,
          isStateAdmin,
          stateId,
          isProvider,
          providerId,
          isEmployer,
          employerId
      );
      // Require authoritative, non-null tenant identifiers for every non-nationwide scan
      context.validateAuthoritativeScope();
      return context;
    }

    return ScanScopeContext.nationwide(callerUserId);
  }

  private void validateScanRequest(DataQualityScanRequest request, ScanScopeContext scopeContext) {
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
      if (!scopeContext.canAccessEntityType(request.getTargetEntityType())) {
        throw new AccessDeniedException(
            "Caller persona is not authorized to scan entity type: " + request.getTargetEntityType()
        );
      }
    }

    if (request.getRuleCode() != null && !request.getRuleCode().isBlank()) {
      String ruleCode = request.getRuleCode().trim();
      if (scopeContext.isEmployer() && !scopeContext.canScanNationwide()) {
        if (!EMPLOYER_ALLOWED_RULES.contains(ruleCode)) {
          throw new AccessDeniedException(
              "Employers are not authorized to execute rule: " + ruleCode
          );
        }
      }
    }
  }
}
