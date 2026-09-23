package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SeparationNatureResponse(
    Long id,
    String natureCode,
    String natureName,
    Boolean isVoluntaryFlag,
    Boolean isInvoluntaryFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
