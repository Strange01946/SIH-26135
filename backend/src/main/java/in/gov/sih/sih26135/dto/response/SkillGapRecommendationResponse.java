package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SkillGapRecommendationResponse(
    Long id,
    Long skillGapId,
    String skillGapNumber,
    Integer recommendationNumber,
    Long skillGapActionTypeId,
    String skillGapActionTypeCode,
    String skillGapActionTypeName,
    Long recommendedCourseId,
    String recommendedCourseCode,
    String recommendedCourseName,
    Long recommendedSkillId,
    String recommendedSkillCode,
    String recommendedSkillName,
    Boolean isAcceptedFlag,
    LocalDate acceptedOn,
    String remarks,
    Long createdByUserId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
