package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SkillGapFactResponse(
    Long skillGapId,
    String skillGapNumber,
    Long skillGapAssessmentId,
    Long traineeId,
    Long skillId,
    String skillCode,
    Long enrollmentId,
    Long courseId,
    Long jobRoleId,
    Long employmentId,
    Long placementId,
    Long observedSkillLevelId,
    Long requiredSkillLevelId,
    Integer gapLevelDelta,
    Long skillGapSeverityId,
    String severityCode,
    Long skillGapStatusId,
    String skillGapStatusCode,
    Long skillGapSourceId,
    String skillGapSourceCode,
    Boolean isCurrent,
    LocalDate identifiedOn,
    LocalDate resolvedOn,
    Long traineeStateId,
    Long traineeDistrictId,
    Long recommendationCount,
    BigDecimal acceptedRecommendationCount
) {}
