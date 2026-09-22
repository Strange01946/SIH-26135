package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record JoiningStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isJoinedFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
