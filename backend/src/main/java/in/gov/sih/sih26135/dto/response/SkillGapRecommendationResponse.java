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
    LocalDateTime updatedAt,
    String status
) {
  public SkillGapRecommendationResponse(Long id, Long skillGapId, String skillGapNumber, Integer recommendationNumber,
      Long actionTypeId, String actionTypeCode, String actionTypeName, Long courseId, String courseCode,
      String courseName, Long skillId, String skillCode, String skillName, Boolean accepted, LocalDate acceptedOn,
      String remarks, Long createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this(id, skillGapId, skillGapNumber, recommendationNumber, actionTypeId, actionTypeCode, actionTypeName,
        courseId, courseCode, courseName, skillId, skillCode, skillName, accepted, acceptedOn, remarks,
        createdBy, createdAt, updatedAt, Boolean.TRUE.equals(accepted) ? "ACCEPTED" : "PROPOSED");
  }
}
