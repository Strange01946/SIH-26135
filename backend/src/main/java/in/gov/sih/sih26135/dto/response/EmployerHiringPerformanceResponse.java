package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing an employer hiring performance leaderboard scorecard
 * derived from formal analytical Query Q07.
 */
public record EmployerHiringPerformanceResponse(
    Long employerId,
    String employerCode,
    String employerName,
    Long placementCount,
    Long traineeCount,
    Long joinedCount,
    BigDecimal joinRatePct,
    BigDecimal avgJoiningSalary,
    Integer rank
) {}
