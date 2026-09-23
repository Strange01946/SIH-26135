package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record AuditActionResponse(
    Long id,
    String actionCode,
    String actionName,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
