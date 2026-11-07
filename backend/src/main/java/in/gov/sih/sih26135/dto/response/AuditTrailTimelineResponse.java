package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Standardized response DTO representing a chronological audit trail timeline for an entity.
 */
public record AuditTrailTimelineResponse(
    String entityType,
    Long entityId,
    long totalEntries,
    List<AuditTrailEntry> timelineEntries
) {

  public AuditTrailTimelineResponse {
    timelineEntries = timelineEntries != null ? Collections.unmodifiableList(timelineEntries) : List.of();
  }

  public static AuditTrailTimelineResponse of(String entityType, Long entityId, List<AuditTrailEntry> entries) {
    long count = entries != null ? entries.size() : 0L;
    return new AuditTrailTimelineResponse(entityType, entityId, count, entries);
  }

  /**
   * Represents an individual chronological entry within an entity's audit timeline.
   */
  public record AuditTrailEntry(
      Long auditLogId,
      Long actorUserId,
      String actionCode,
      String actionName,
      LocalDateTime occurredAt,
      String correlationId,
      String changeSummary
  ) {}
}
