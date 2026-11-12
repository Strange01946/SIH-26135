package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.OperationalDashboardFilter;
import in.gov.sih.sih26135.dto.response.OperationalDataQualityScorecardResponse;
import in.gov.sih.sih26135.dto.response.OperationalDataQualityScorecardResponse.DomainCleanlinessMetric;
import in.gov.sih.sih26135.dto.response.OperationalDataQualityScorecardResponse.FailingRuleMetric;
import in.gov.sih.sih26135.dto.response.OperationalImportMetricsResponse;
import in.gov.sih.sih26135.dto.response.OperationalSystemHealthResponse;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.OperationalDashboardQueryRepository;
import in.gov.sih.sih26135.repository.OperationalDashboardQueryRepository.AuditMutationAggregateResult;
import in.gov.sih.sih26135.repository.OperationalDashboardQueryRepository.BatchAggregateResult;
import in.gov.sih.sih26135.repository.OperationalDashboardQueryRepository.EffectiveDashboardScope;
import in.gov.sih.sih26135.repository.OperationalDashboardQueryRepository.IncidentAggregateResult;
import in.gov.sih.sih26135.repository.OperationalDashboardQueryRepository.IssueSeverityAndStatusAggregate;
import in.gov.sih.sih26135.repository.OperationalDashboardQueryRepository.ResolutionDurationAggregate;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.OperationalDashboardService;
import in.gov.sih.sih26135.util.OperationalMetricsMath;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only implementation of {@link OperationalDashboardService}.
 */
@Service
@Transactional(readOnly = true)
public class OperationalDashboardServiceImpl implements OperationalDashboardService {

  private static final String CLEANLINESS_FORMULA =
      "(eligiblePopulation - distinctAffectedEntities) / eligiblePopulation * 100";

  private static final String SUPPRESSION_DISCLOSURE =
      "Counts reflect persisted operational events; Phase 28.6 deduplication and anti-flood cooldown suppression may consolidate repeated rapid triggers.";

  private final OperationalDashboardQueryRepository queryRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final JdbcTemplate jdbcTemplate;

  public OperationalDashboardServiceImpl(
      OperationalDashboardQueryRepository queryRepository,
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate) {
    this.queryRepository = Objects.requireNonNull(queryRepository, "OperationalDashboardQueryRepository is required");
    this.scopeValidator = Objects.requireNonNull(scopeValidator, "WorkflowSecurityScopeValidator is required");
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate is required");
  }

  @Override
  public OperationalDataQualityScorecardResponse getDataQualityScorecard(OperationalDashboardFilter filter) {
    LocalDateTime evaluatedAt = LocalDateTime.now();
    OperationalDashboardFilter effectiveFilter = filter != null
        ? filter
        : OperationalDashboardFilter.parseAndValidate(null, null, null, null, null, evaluatedAt);

    EffectiveDashboardScope scope = resolveAndValidateScope(effectiveFilter);

    List<DomainCleanlinessMetric> domainBreakdown = queryRepository.aggregateDomainCleanliness(scope);

    long eligiblePopulation = 0L;
    long distinctAffectedEntities = 0L;
    for (DomainCleanlinessMetric domain : domainBreakdown) {
      eligiblePopulation = OperationalMetricsMath.safeAdd(eligiblePopulation, domain.eligibleCount());
      distinctAffectedEntities = OperationalMetricsMath.safeAdd(distinctAffectedEntities, domain.distinctAffectedCount());
    }
    BigDecimal dqiPct = OperationalMetricsMath.dataCleanlinessIndex(eligiblePopulation, distinctAffectedEntities);
    long cleanEntitiesEstimate = eligiblePopulation > 0L
        ? Math.max(0L, Math.subtractExact(eligiblePopulation, Math.min(eligiblePopulation, distinctAffectedEntities)))
        : 0L;

    IssueSeverityAndStatusAggregate statusAndSeverity = queryRepository.aggregateActiveIssuesByStatusAndSeverity(
        scope,
        effectiveFilter.windowStart(),
        effectiveFilter.windowEndExclusive(),
        effectiveFilter.timeWindowApplied()
    );

    long totalActiveIssues = statusAndSeverity.byStatus().values().stream()
        .reduce(0L, OperationalMetricsMath::safeAdd);

    List<FailingRuleMetric> topFailingRules = queryRepository.findTopFailingRules(
        scope,
        effectiveFilter.windowStart(),
        effectiveFilter.windowEndExclusive(),
        effectiveFilter.timeWindowApplied()
    );

    ResolutionDurationAggregate resolution = queryRepository.aggregateResolutionDuration(
        scope,
        effectiveFilter.windowStart(),
        effectiveFilter.windowEndExclusive(),
        effectiveFilter.timeWindowApplied()
    );

    BigDecimal avgResolutionHours = OperationalMetricsMath.averageHoursFromSeconds(
        resolution.totalSeconds(), resolution.resolvedCount());

    String periodMetricsScope = effectiveFilter.timeWindowApplied()
        ? "WINDOWED_ISSUE_ACTIVITY"
        : "ALL_CURRENT_ISSUE_ACTIVITY";
    String rankingBasis = effectiveFilter.timeWindowApplied()
        ? "ACTIVE_UNRESOLVED_DETECTED_IN_WINDOW"
        : "ACTIVE_UNRESOLVED_ISSUES";

    return new OperationalDataQualityScorecardResponse(
        dqiPct,
        CLEANLINESS_FORMULA,
        "CURRENT_COHORT_ACTIVE_ISSUES",
        eligiblePopulation,
        distinctAffectedEntities,
        cleanEntitiesEstimate,
        totalActiveIssues,
        statusAndSeverity.byStatus(),
        statusAndSeverity.bySeverity(),
        domainBreakdown,
        periodMetricsScope,
        rankingBasis,
        topFailingRules,
        statusAndSeverity.unruledActiveCount(),
        avgResolutionHours,
        "HOURS",
        resolution.resolvedCount(),
        resolution.reopenedCount(),
        effectiveFilter.timeWindowApplied() ? effectiveFilter.windowStart() : null,
        effectiveFilter.timeWindowApplied() ? effectiveFilter.windowEndExclusive() : null,
        effectiveFilter.timeWindowApplied(),
        evaluatedAt
    );
  }

  @Override
  public OperationalImportMetricsResponse getImportMetrics(OperationalDashboardFilter filter) {
    LocalDateTime evaluatedAt = LocalDateTime.now();
    OperationalDashboardFilter effectiveFilter = filter != null
        ? filter
        : OperationalDashboardFilter.parseAndValidate(null, null, null, null, null, evaluatedAt);

    EffectiveDashboardScope scope = resolveAndValidateScope(effectiveFilter);

    BatchAggregateResult agg = queryRepository.aggregateImportMetrics(
        scope, effectiveFilter.windowStart(), effectiveFilter.windowEndExclusive());

    Map<String, Long> batchesByStatus = agg.batchesByStatus();
    long pending = batchesByStatus.getOrDefault("PENDING", 0L);
    long running = batchesByStatus.getOrDefault("RUNNING", 0L);
    long completed = batchesByStatus.getOrDefault("COMPLETED", 0L);
    long failed = batchesByStatus.getOrDefault("FAILED", 0L);
    long cancelled = batchesByStatus.getOrDefault("CANCELLED", 0L);
    long activeBatches = OperationalMetricsMath.safeAdd(pending, running);
    long terminalBatches = OperationalMetricsMath.safeAdd(
        OperationalMetricsMath.safeAdd(completed, failed), cancelled);
    long totalBatches = batchesByStatus.values().stream()
        .reduce(0L, OperationalMetricsMath::safeAdd);

    Map<String, Long> recordsByStatus = agg.recordsByStatus();
    long successRecords = recordsByStatus.getOrDefault("SUCCESS", 0L);
    long failedRecords = recordsByStatus.getOrDefault("FAILED", 0L);
    long duplicateRecords = recordsByStatus.getOrDefault("DUPLICATE", 0L);
    long skippedRecords = recordsByStatus.getOrDefault("SKIPPED", 0L);
    long totalProcessedRecords = recordsByStatus.values().stream()
        .reduce(0L, OperationalMetricsMath::safeAdd);

    BigDecimal windowSeconds = effectiveFilter.windowDurationExactSeconds();
    BigDecimal windowHours = OperationalMetricsMath.hoursFromSeconds(windowSeconds);

    return new OperationalImportMetricsResponse(
        totalBatches,
        activeBatches,
        terminalBatches,
        pending,
        running,
        completed,
        failed,
        cancelled,
        batchesByStatus,
        totalProcessedRecords,
        successRecords,
        failedRecords,
        duplicateRecords,
        skippedRecords,
        OperationalMetricsMath.percentage(successRecords, totalProcessedRecords),
        OperationalMetricsMath.percentage(failedRecords, totalProcessedRecords),
        OperationalMetricsMath.percentage(duplicateRecords, totalProcessedRecords),
        OperationalMetricsMath.percentage(skippedRecords, totalProcessedRecords),
        agg.completedBatchesInWindow(),
        agg.successfulRowsProcessedInWindow(),
        agg.timedCompletedBatches(),
        agg.timedCompletedRecordCount(),
        OperationalMetricsMath.ratePerHour(agg.completedBatchesInWindow(), windowSeconds),
        OperationalMetricsMath.ratePerHour(agg.successfulRowsProcessedInWindow(), windowSeconds),
        OperationalMetricsMath.ratePerHour(agg.timedCompletedRecordCount(), agg.totalExecutionSeconds()),
        "ROWS_PER_HOUR",
        effectiveFilter.windowStart(),
        effectiveFilter.windowEndExclusive(),
        windowHours,
        evaluatedAt
    );
  }

  @Override
  public OperationalSystemHealthResponse getSystemHealth(OperationalDashboardFilter filter) {
    LocalDateTime evaluatedAt = LocalDateTime.now();
    OperationalDashboardFilter effectiveFilter = filter != null
        ? filter
        : OperationalDashboardFilter.parseAndValidate(null, null, null, null, null, evaluatedAt);

    EffectiveDashboardScope scope = resolveAndValidateScope(effectiveFilter);

    IncidentAggregateResult incidentAgg = queryRepository.aggregateSystemIncidents(
        scope, effectiveFilter.windowStart(), effectiveFilter.windowEndExclusive());

    AuditMutationAggregateResult auditAgg = queryRepository.aggregateAuditMutations(
        scope, effectiveFilter.windowStart(), effectiveFilter.windowEndExclusive());

    long criticalCount = incidentAgg.bySeverity().getOrDefault("CRITICAL", 0L);
    long errorCount = incidentAgg.bySeverity().getOrDefault("ERROR", 0L);
    long warningCount = incidentAgg.bySeverity().getOrDefault("WARNING", 0L);
    long infoCount = incidentAgg.bySeverity().getOrDefault("INFO", 0L);

    BigDecimal windowSeconds = effectiveFilter.windowDurationExactSeconds();
    BigDecimal windowHours = OperationalMetricsMath.hoursFromSeconds(windowSeconds);

    return new OperationalSystemHealthResponse(
        incidentAgg.totalIncidents(),
        criticalCount,
        errorCount,
        warningCount,
        infoCount,
        incidentAgg.bySeverity(),
        incidentAgg.byCategory(),
        incidentAgg.correlatedIncidents(),
        OperationalMetricsMath.ratePerHour(criticalCount, windowSeconds),
        OperationalMetricsMath.ratePerHour(errorCount, windowSeconds),
        "EVENTS_PER_HOUR",
        auditAgg.totalMutationEvents(),
        auditAgg.distinctEntitiesModified(),
        auditAgg.mutationsByAction(),
        auditAgg.nonMutationCount(),
        SUPPRESSION_DISCLOSURE,
        effectiveFilter.windowStart(),
        effectiveFilter.windowEndExclusive(),
        windowHours,
        evaluatedAt
    );
  }

  // =========================================================================
  // Fail-Closed Authorization & Tenant Scope Resolution
  // =========================================================================

  private EffectiveDashboardScope resolveAndValidateScope(OperationalDashboardFilter filter) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("Authentication is required to access operational dashboards");
    }

    Set<String> authorities = auth.getAuthorities() != null
        ? auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet())
        : Set.of();

    boolean hasPermission = authorities.contains("audit.read")
        || authorities.contains("system.manage")
        || authorities.contains("analytics.read")
        || authorities.contains("system.event.read")
        || authorities.contains("system.event.manage");
    if (!hasPermission) {
      throw new AccessDeniedException("Caller lacks required authority to access operational dashboards");
    }

    if (scopeValidator.isTraineeCaller() || authorities.contains("ROLE_trainee")) {
      throw new AccessDeniedException("Trainees are not authorized to access operational dashboards");
    }

    Long callerUserId = scopeValidator.resolveAuthenticatedUserId();
    if (callerUserId == null) {
      throw new AccessDeniedException("Authenticated caller identity could not be resolved");
    }
    User caller = scopeValidator.resolveAuthenticatedUser()
        .orElseThrow(() -> new AccessDeniedException("Authoritative user record not found"));
    if (caller.getDeletedAt() != null) {
      throw new AccessDeniedException("Deleted user account cannot access operational dashboards");
    }

    if (scopeValidator.isNationwideAdmin()) {
      return resolveNationwideFilteredScope(filter);
    }

    return resolveNonNationwideFilteredScope(caller, authorities, filter);
  }

  private EffectiveDashboardScope resolveNationwideFilteredScope(OperationalDashboardFilter filter) {
    Long reqState = filter.requestedStateId();
    Long reqDistrict = filter.requestedDistrictId();
    Long reqProvider = filter.requestedTrainingProviderId();

    if (reqDistrict != null) {
      Long districtParentState = scopeValidator.findDistrictStateId(reqDistrict);
      if (districtParentState == null) {
        throw new BadRequestException("Requested districtId does not exist", "INVALID_FILTER_PARAMETER");
      }
      if (reqState != null && !reqState.equals(districtParentState)) {
        throw new BadRequestException(
            "Requested districtId does not belong to requested stateId",
            "INVALID_FILTER_PARAMETER"
        );
      }
      if (reqState == null) {
        reqState = districtParentState;
      }
    }

    if (reqProvider != null) {
      ProviderGeo geo = lookupProviderGeo(reqProvider);
      if (geo == null) {
        throw new BadRequestException("Requested trainingProviderId does not exist", "INVALID_FILTER_PARAMETER");
      }
      if (reqState != null && !reqState.equals(geo.stateId())) {
        throw new BadRequestException(
            "Requested trainingProviderId does not belong to requested stateId",
            "INVALID_FILTER_PARAMETER"
        );
      }
      if (reqDistrict != null && !reqDistrict.equals(geo.districtId())) {
        throw new BadRequestException(
            "Requested trainingProviderId does not belong to requested districtId",
            "INVALID_FILTER_PARAMETER"
        );
      }
    }

    boolean unfiltered = reqState == null && reqDistrict == null && reqProvider == null;
    return new EffectiveDashboardScope(unfiltered, reqState, reqDistrict, reqProvider, null);
  }

  private EffectiveDashboardScope resolveNonNationwideFilteredScope(
      User caller,
      Set<String> authorities,
      OperationalDashboardFilter filter) {

    boolean roleProvider = authorities.contains("ROLE_training_provider");
    boolean roleEmployer = authorities.contains("ROLE_employer");
    boolean roleStateAdmin = authorities.contains("ROLE_state_admin")
        || authorities.contains("ROLE_government_admin");
    boolean roleOfficer = authorities.contains("ROLE_district_officer")
        || authorities.contains("ROLE_government_officer");

    int personaCount = (roleProvider ? 1 : 0) + (roleEmployer ? 1 : 0)
        + ((roleStateAdmin || roleOfficer) ? 1 : 0);
    if (personaCount == 0 || personaCount > 1) {
      throw new AccessDeniedException("Caller lacks a valid unambiguous operational persona role");
    }

    boolean hasState = caller.getStateId() != null;
    boolean hasDistrict = caller.getDistrictId() != null;
    boolean hasProvider = caller.getTrainingProviderId() != null;
    boolean hasEmployer = caller.getEmployerId() != null;

    if ((roleProvider && !hasProvider) || (roleEmployer && !hasEmployer)
        || ((roleStateAdmin || roleOfficer) && !hasState)) {
      throw new AccessDeniedException("Caller lacks required authoritative tenant scope");
    }
    int rootCount = (hasState ? 1 : 0) + (hasProvider ? 1 : 0) + (hasEmployer ? 1 : 0);
    if (rootCount != 1) {
      throw new AccessDeniedException("Caller has ambiguous or conflicting tenant scope");
    }
    if ((hasProvider || hasEmployer) && hasDistrict) {
      throw new AccessDeniedException("Caller has conflicting organizational and geographic tenancy");
    }
    if (hasState && (roleProvider || roleEmployer)) {
      throw new AccessDeniedException("Caller has conflicting role and state tenancy");
    }
    if (roleOfficer && !hasDistrict) {
      throw new AccessDeniedException("District officer caller lacks required districtId scope");
    }

    Long reqState = filter.requestedStateId();
    Long reqDistrict = filter.requestedDistrictId();
    Long reqProvider = filter.requestedTrainingProviderId();

    if (hasEmployer) {
      if (reqState != null || reqDistrict != null || reqProvider != null) {
        throw new AccessDeniedException("Employer callers cannot filter operational metrics by geography or provider");
      }
      return new EffectiveDashboardScope(false, null, null, null, caller.getEmployerId());
    }

    if (hasProvider) {
      if (reqState != null || reqDistrict != null) {
        throw new AccessDeniedException("Training provider callers cannot query geographic filters outside provider ownership");
      }
      if (reqProvider != null && !reqProvider.equals(caller.getTrainingProviderId())) {
        throw new AccessDeniedException("Training provider caller cannot access another training provider's metrics");
      }
      return new EffectiveDashboardScope(false, null, null, caller.getTrainingProviderId(), null);
    }

    // State admin or District officer
    Long callerState = caller.getStateId();
    Long callerDistrict = caller.getDistrictId();

    if (reqState != null && !reqState.equals(callerState)) {
      throw new AccessDeniedException("Caller cannot access operational metrics outside assigned state");
    }
    if (callerDistrict != null) {
      if (reqDistrict != null && !reqDistrict.equals(callerDistrict)) {
        throw new AccessDeniedException("Caller cannot access operational metrics outside assigned district");
      }
    } else if (reqDistrict != null) {
      Long parentState = scopeValidator.findDistrictStateId(reqDistrict);
      if (parentState == null) {
        throw new BadRequestException("Requested districtId does not exist", "INVALID_FILTER_PARAMETER");
      }
      if (!callerState.equals(parentState)) {
        throw new AccessDeniedException("Requested districtId belongs to another state");
      }
    }

    Long effectiveDistrict = callerDistrict != null ? callerDistrict : reqDistrict;

    if (reqProvider != null) {
      ProviderGeo geo = lookupProviderGeo(reqProvider);
      if (geo == null || !callerState.equals(geo.stateId())) {
        throw new AccessDeniedException("Requested trainingProviderId is outside caller state scope");
      }
      if (effectiveDistrict != null && !effectiveDistrict.equals(geo.districtId())) {
        throw new AccessDeniedException("Requested trainingProviderId is outside caller district scope");
      }
    }

    return new EffectiveDashboardScope(false, callerState, effectiveDistrict, reqProvider, null);
  }

  private record ProviderGeo(Long stateId, Long districtId) {}

  private ProviderGeo lookupProviderGeo(Long providerId) {
    List<ProviderGeo> rows = jdbcTemplate.query(
        "SELECT state_id, district_id FROM training_providers WHERE provider_id = ? AND deleted_at IS NULL",
        (rs, rowNum) -> new ProviderGeo(
            rs.getObject("state_id") != null ? rs.getLong("state_id") : null,
            rs.getObject("district_id") != null ? rs.getLong("district_id") : null),
        providerId
    );
    return rows.isEmpty() ? null : rows.get(0);
  }
}
