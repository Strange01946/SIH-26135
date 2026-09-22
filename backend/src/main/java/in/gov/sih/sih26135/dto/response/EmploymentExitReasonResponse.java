package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmploymentExitReasonResponse(
    Long id,
    String reasonCode,
    String reasonName,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
