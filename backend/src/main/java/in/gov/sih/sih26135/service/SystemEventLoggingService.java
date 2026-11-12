package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import java.util.Optional;

/**
 * Structured, transaction-isolated operational incident logging service for Phase 28.6.
 */
public interface SystemEventLoggingService {

  enum EventCategory {
    SECURITY,
    INTEGRATION,
    BATCH_JOB,
    CONFIGURATION,
    ACCESS
  }

  enum EventSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
  }

  /**
   * Persists a structured operational event in an independent transaction with server-derived timestamps,
   * correlation context, sanitized summary, and recursive/fallback protection.
   */
  Optional<SystemEventLogResponse> logEvent(
      EventCategory category,
      EventSeverity severity,
      String eventCode,
      Long actorUserId,
      String entityType,
      Long entityId,
      String sourceComponent,
      String rawMessage);

  /**
   * Records a rate-limit violation with bounded per-(actor, code, route) cooldown throttling to avoid
   * database amplification under abusive request floods while preventing single-rejection duplicates.
   */
  Optional<SystemEventLogResponse> logRateLimitViolation(
      String eventCode,
      Long actorUserId,
      String sanitizedRoute,
      String sourceComponent,
      String rawMessage);

  /**
   * Records a tenant-isolated data-quality spike event when threshold criteria are met, with bounded
   * anti-flood cooldown suppression for the same unresolved tenant spike signal.
   */
  Optional<SystemEventLogResponse> logDataQualitySpike(
      EventSeverity severity,
      String eventCode,
      Long actorUserId,
      String tenantScopeKey,
      String spikeSignalKey,
      String entityType,
      Long entityId,
      int cooldownSeconds,
      String rawMessage);

  /**
   * Resets internal in-memory cooldown and deduplication windows (intended for automated tests).
   */
  void resetThrottleState();
}
