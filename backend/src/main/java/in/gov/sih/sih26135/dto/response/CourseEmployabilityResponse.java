package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response record for Course Outcome & Employability Leaderboard (formal query Q05).
 * Follows Phase 27.2 conventions: Long for count metrics, BigDecimal (scale 2) for percentage and monetary metrics.
 */
public record CourseEmployabilityResponse(
    Long courseId,
    String courseCode,
    String courseName,
    Long qualificationLevelId,
    String qualificationLevelCode,
    String qualificationLevelName,
    Integer nsqfLevel,
    Long enrollmentCount,
    Long traineeCount,
    Long completedCount,
    Long certifiedCount,
    Long joinedPlacementCount,
    Long employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct,
    BigDecimal dropoutRatePct,
    BigDecimal avgStartingWage,
    Integer rank
) {}
