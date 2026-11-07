package in.gov.sih.sih26135.event;

import java.time.LocalDateTime;

/**
 * Strongly typed internal domain event representing a completed workflow transition
 * ready for asynchronous audit trail persistence.
 *
 * <p>Contains thread-detached, immutable snapshot metadata captured prior to crossing
 * asynchronous and transactional boundaries.
 *
 * @param actionCode    canonical audit action code (e.g., "STATUS_CHANGE", "CREATE", "UPDATE")
 * @param entityType    target domain entity type name (e.g., "Placement", "TrainingEnrollment")
 * @param entityId      primary key identifier of the affected entity
 * @param actorUserId   authenticated user ID who performed the transition, or null if system-driven
 * @param changeSummary sanitized transition summary description
 * @param occurredAt    timestamp when the domain event occurred
 * @param correlationId HTTP request or distributed trace correlation identifier
 */
public record AuditTrailEvent(
    String actionCode,
    String entityType,
    Long entityId,
    Long actorUserId,
    String changeSummary,
    LocalDateTime occurredAt,
    String correlationId
) {

  public AuditTrailEvent {
    if (actionCode == null || actionCode.isBlank()) {
      actionCode = "STATUS_CHANGE";
    }
    if (occurredAt == null) {
      occurredAt = LocalDateTime.now();
    }
  }
}
