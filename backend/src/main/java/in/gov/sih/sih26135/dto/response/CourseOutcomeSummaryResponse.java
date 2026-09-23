package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

public record CourseOutcomeSummaryResponse(
    Long courseId,
    Long enrollmentCount,
    Long traineeCount,
    BigDecimal completedCount,
    BigDecimal certifiedCount,
    BigDecimal joinedPlacementCount,
    BigDecimal employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct
) {}
