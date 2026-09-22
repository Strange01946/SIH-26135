package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record PlacementStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isOfferFlag,
    Boolean isJoinedFlag,
    Boolean isUnsuccessfulFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
