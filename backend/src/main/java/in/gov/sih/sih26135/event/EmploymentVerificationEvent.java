package in.gov.sih.sih26135.event;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Strongly typed immutable domain event representing an employment verification lifecycle transition
 * (e.g., verification initiated, evidence uploaded, verification attestation confirmed, or milestone retention verified).
 */
public record EmploymentVerificationEvent(
    String eventId,
    String eventType,
    Long entityId,
    Long employmentRecordId,
    Long traineeId,
    Long actorUserId,
    String correlationId,
    LocalDateTime occurredAt,
    Long employerId,
    Long stateId,
    String verificationMethod,
    String verificationStatus,
    BigDecimal verifiedSalary,
    Integer retentionMonthPeriod,
    String metadataJson
) implements DomainEvent {

  public EmploymentVerificationEvent {
    if (eventId == null || eventId.isBlank()) {
      eventId = UUID.randomUUID().toString();
    }
    if (eventType == null || eventType.isBlank()) {
      eventType = "VERIFICATION_UPDATE";
    }
    if (occurredAt == null) {
      occurredAt = LocalDateTime.now();
    }
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = CorrelationContextHolder.getCorrelationId();
    }
  }

  /**
   * Convenience factory for creating an employment verification event.
   */
  public static EmploymentVerificationEvent of(
      String eventType,
      Long entityId,
      Long employmentRecordId,
      Long traineeId,
      Long actorUserId,
      Long employerId,
      String verificationMethod,
      String verificationStatus,
      BigDecimal verifiedSalary,
      Integer retentionMonthPeriod) {
    return new EmploymentVerificationEvent(
        UUID.randomUUID().toString(),
        eventType,
        entityId,
        employmentRecordId,
        traineeId,
        actorUserId,
        CorrelationContextHolder.getCorrelationId(),
        LocalDateTime.now(),
        employerId,
        null,
        verificationMethod,
        verificationStatus,
        verifiedSalary,
        retentionMonthPeriod,
        null
    );
  }

  @Override
  public String getEventId() {
    return eventId();
  }

  @Override
  public String getEventType() {
    return eventType();
  }

  @Override
  public String getEntityType() {
    return "EmploymentVerification";
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
}
