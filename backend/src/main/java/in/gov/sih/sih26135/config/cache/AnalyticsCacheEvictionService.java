package in.gov.sih.sih26135.config.cache;

import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service responsible for publishing targeted analytical cache invalidation events
 * following authoritative domain state transitions.
 */
@Service
public class AnalyticsCacheEvictionService {

  private final ApplicationEventPublisher eventPublisher;

  public AnalyticsCacheEvictionService(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = Objects.requireNonNull(eventPublisher, "ApplicationEventPublisher is required");
  }

  /**
   * Evicts caches affected by placement verification or employment transition.
   * Affects pipeline conversions, employment counts, and economic summaries.
   */
  public void evictOnPlacementTransition(Object source) {
    eventPublisher.publishEvent(new AnalyticsWorkflowInvalidationEvent(
        source,
        "PLACEMENT_TO_EMPLOYMENT",
        Set.of(
            AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW,
            AnalyticsCacheConfig.CACHE_STATE_AGGREGATES,
            AnalyticsCacheConfig.CACHE_DISTRICT_AGGREGATES,
            AnalyticsCacheConfig.CACHE_INSTITUTIONAL,
            AnalyticsCacheConfig.CACHE_ECONOMIC,
            AnalyticsCacheConfig.CACHE_RETENTION,
            AnalyticsCacheConfig.CACHE_SUMMARIES
        )
    ));
  }

  /**
   * Evicts caches affected by formal verification verdicts.
   */
  public void evictOnVerificationVerdict(Object source) {
    eventPublisher.publishEvent(new AnalyticsWorkflowInvalidationEvent(
        source,
        "VERIFICATION_VERDICT",
        Set.of(
            AnalyticsCacheConfig.CACHE_VERIFICATION,
            AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW,
            AnalyticsCacheConfig.CACHE_SUMMARIES
        )
    ));
  }

  /**
   * Evicts caches affected by training completion and certificate issuance.
   * If skill gaps were resolved during training completion, also evicts skill-gap cache.
   */
  public void evictOnCertificationIssued(Object source, boolean skillGapsResolved) {
    Set<String> caches = new java.util.HashSet<>(Set.of(
        AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW,
        AnalyticsCacheConfig.CACHE_STATE_AGGREGATES,
        AnalyticsCacheConfig.CACHE_DISTRICT_AGGREGATES,
        AnalyticsCacheConfig.CACHE_INSTITUTIONAL,
        AnalyticsCacheConfig.CACHE_SUMMARIES
    ));
    if (skillGapsResolved) {
      caches.add(AnalyticsCacheConfig.CACHE_SKILL_GAP);
    }
    eventPublisher.publishEvent(new AnalyticsWorkflowInvalidationEvent(
        source,
        skillGapsResolved ? "CERTIFICATION_ISSUED_WITH_SKILL_GAPS_RESOLVED" : "CERTIFICATION_ISSUED",
        caches
    ));
  }

  /**
   * Evicts caches affected by training completion and certificate issuance.
   */
  public void evictOnCertificationIssued(Object source) {
    evictOnCertificationIssued(source, false);
  }

  /**
   * Evicts caches affected by employment exit or unemployment transitions.
   */
  public void evictOnEmploymentExitOrTransition(Object source) {
    eventPublisher.publishEvent(new AnalyticsWorkflowInvalidationEvent(
        source,
        "EMPLOYMENT_LIFECYCLE_TRANSITION",
        Set.of(
            AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW,
            AnalyticsCacheConfig.CACHE_ECONOMIC,
            AnalyticsCacheConfig.CACHE_RETENTION,
            AnalyticsCacheConfig.CACHE_SUMMARIES
        )
    ));
  }

  /**
   * Evicts caches affected by follow-up task completion or survey submissions.
   */
  public void evictOnFollowupOrSurveySubmitted(Object source) {
    eventPublisher.publishEvent(new AnalyticsWorkflowInvalidationEvent(
        source,
        "FOLLOWUP_OR_SURVEY_SUBMISSION",
        Set.of(
            AnalyticsCacheConfig.CACHE_VERIFICATION,
            AnalyticsCacheConfig.CACHE_SUMMARIES
        )
    ));
  }

  /**
   * Evicts caches affected by follow-up engagement attempts that change task status or outcome.
   */
  public void evictOnEngagementAttempt(Object source) {
    eventPublisher.publishEvent(new AnalyticsWorkflowInvalidationEvent(
        source,
        "FOLLOWUP_ENGAGEMENT_ATTEMPT",
        Set.of(
            AnalyticsCacheConfig.CACHE_VERIFICATION,
            AnalyticsCacheConfig.CACHE_SUMMARIES
        )
    ));
  }

  /**
   * Evicts caches affected by skill gap assessments and remediation decisions.
   */
  public void evictOnSkillGapRemediation(Object source) {
    eventPublisher.publishEvent(new AnalyticsWorkflowInvalidationEvent(
        source,
        "SKILL_GAP_REMEDIATION",
        Set.of(
            AnalyticsCacheConfig.CACHE_SKILL_GAP,
            AnalyticsCacheConfig.CACHE_SUMMARIES
        )
    ));
  }

  /**
   * Evicts all analytical caches.
   */
  public void evictAllAnalyticsCaches(Object source) {
    eventPublisher.publishEvent(new AnalyticsWorkflowInvalidationEvent(
        source,
        "ADMIN_EVICT_ALL",
        Set.of(
            AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW,
            AnalyticsCacheConfig.CACHE_STATE_AGGREGATES,
            AnalyticsCacheConfig.CACHE_DISTRICT_AGGREGATES,
            AnalyticsCacheConfig.CACHE_INSTITUTIONAL,
            AnalyticsCacheConfig.CACHE_ECONOMIC,
            AnalyticsCacheConfig.CACHE_RETENTION,
            AnalyticsCacheConfig.CACHE_SKILL_GAP,
            AnalyticsCacheConfig.CACHE_VERIFICATION,
            AnalyticsCacheConfig.CACHE_SUMMARIES
        )
    ));
  }
}
