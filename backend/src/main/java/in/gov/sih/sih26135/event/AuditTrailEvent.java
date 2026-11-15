package in.gov.sih.sih26135.event;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
) implements DomainEvent {

  private static final Map<IdentityKey, String> EVENT_IDS = new ConcurrentHashMap<>();
  private static final ReferenceQueue<AuditTrailEvent> REF_QUEUE = new ReferenceQueue<>();

  public AuditTrailEvent {
    if (actionCode == null || actionCode.isBlank()) {
      actionCode = "STATUS_CHANGE";
    }
    if (occurredAt == null) {
      occurredAt = LocalDateTime.now();
    }
  }

  @Override
  public String getEventId() {
    cleanQueue();
    return EVENT_IDS.computeIfAbsent(new IdentityKey(this), k -> UUID.randomUUID().toString());
  }

  private static void cleanQueue() {
    Reference<? extends AuditTrailEvent> ref;
    while ((ref = REF_QUEUE.poll()) != null) {
      EVENT_IDS.remove(ref);
    }
  }

  @Override
  public String getEventType() {
    return actionCode();
  }

  @Override
  public String getEntityType() {
    return entityType();
  }

  @Override
  public Long getEntityId() {
    return entityId();
  }

  @Override
  public Long getActorUserId() {
    return actorUserId();
  }

  @Override
  public String getCorrelationId() {
    return correlationId();
  }

  @Override
  public LocalDateTime getOccurredAt() {
    return occurredAt();
  }

  /**
   * Reference-equality weak key ensuring stable, distinct event IDs per instance without memory leaks.
   */
  private static final class IdentityKey extends WeakReference<AuditTrailEvent> {
    private final int hash;

    IdentityKey(AuditTrailEvent referent) {
      super(referent, REF_QUEUE);
      this.hash = System.identityHashCode(referent);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj instanceof IdentityKey other) {
        AuditTrailEvent myRef = get();
        AuditTrailEvent otherRef = other.get();
        return myRef != null && myRef == otherRef;
      }
      return false;
    }
  }
}
