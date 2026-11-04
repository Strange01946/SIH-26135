package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection for Course Outcome & Employability Index (formal query Q05).
 * Captures curriculum outcome metrics, qualification level metadata, and average starting wage.
 */
public record CourseEmployabilityProjection(
    Long courseId,
    String courseCode,
    String courseName,
    Long qualificationLevelId,
    String qualificationLevelCode,
    String qualificationLevelName,
    Integer nsqfLevel,
    Number enrollmentCount,
    Number traineeCount,
    Number completedCount,
    Number certifiedCount,
    Number joinedPlacementCount,
    Number employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct,
    BigDecimal avgStartingWage
) {}
