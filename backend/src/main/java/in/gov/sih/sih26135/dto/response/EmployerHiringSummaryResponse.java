package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

public record EmployerHiringSummaryResponse(
    Long employerId,
    Long placementCount,
    Long traineeCount,
    BigDecimal joinedCount,
    BigDecimal joinRatePct,
    BigDecimal avgJoiningSalary
) {}
