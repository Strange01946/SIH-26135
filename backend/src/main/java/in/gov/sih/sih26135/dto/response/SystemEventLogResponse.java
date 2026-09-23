package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SystemEventLogResponse(
    Long id,
    Long systemEventCategoryId,
    String systemEventCategoryCode,
    String systemEventCategoryName,
    Long systemEventSeverityId,
    String systemEventSeverityCode,
    String systemEventSeverityName,
    String eventCode,
    Long actorUserId,
    String entityType,
    Long entityId,
    String sourceComponent,
    String eventSummary,
    LocalDateTime occurredAt,
    LocalDateTime createdAt
) {}
