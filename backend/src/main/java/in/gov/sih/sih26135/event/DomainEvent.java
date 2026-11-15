package in.gov.sih.sih26135.event;

import java.time.LocalDateTime;

/**
 * Common foundational contract for strongly typed domain events across the SIH-26135 platform.
 *
 * <p>Domain events represent state transitions, audit mutations, quality anomalies, or operational
 * triggers that occur within business workflow boundaries. All implementations must adhere to the
 * following invariants:
 * <ul>
 *   <li><b>Immutability:</b> All fields must be immutable and thread-safe.</li>
 *   <li><b>Context Detachment:</b> Events must not hold references to active or managed JPA entities;
 *       only detached scalar identifiers and sanitized value representations may be transported.</li>
 *   <li><b>Traceability:</b> Every event carries a distinct {@code eventId}, occurrence timestamp,
 *       and distributed trace {@code correlationId}.</li>
 *   <li><b>Tenancy Scoping:</b> Events must transport relevant tenant context (such as state, district,
 *       training provider, or employer identifiers) to support multi-tenant isolation.</li>
 * </ul>
 */
public interface DomainEvent {

  /**
   * Unique identifier of this event instance (UUID).
   *
   * @return stable event identifier
   */
  String getEventId();

  /**
   * Specific event type or transition code (e.g., "ENROLLED", "VERIFIED", "STATUS_CHANGE").
   *
   * @return canonical event type code
   */
  String getEventType();

  /**
   * Primary affected domain entity category (e.g., "Trainee", "Placement", "Verification").
   *
   * @return target entity type name
   */
  String getEntityType();

  /**
   * Primary key identifier of the target domain entity.
   *
   * @return target entity identifier
   */
  Long getEntityId();

  /**
   * Primary key identifier of the authenticated actor user who initiated the event, or null if system-driven.
   *
   * @return actor user ID or null
   */
  Long getActorUserId();

  /**
   * Distributed trace correlation identifier propagating across HTTP and asynchronous task boundaries.
   *
   * @return correlation ID
   */
  String getCorrelationId();

  /**
   * Timestamp when the domain event occurred.
   *
   * @return occurrence timestamp
   */
  LocalDateTime getOccurredAt();
}
