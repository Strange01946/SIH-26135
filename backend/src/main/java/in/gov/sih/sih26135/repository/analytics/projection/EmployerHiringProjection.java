package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection record representing formal Query Q07 results
 * from {@code vw_employer_hiring_summary} and {@code employers}.
 */
public record EmployerHiringProjection(
    Long employerId,
    String employerCode,
    String employerName,
    Long placementCount,
    Long traineeCount,
    Long joinedCount,
    BigDecimal joinRatePct,
    BigDecimal avgJoiningSalary
) {}
