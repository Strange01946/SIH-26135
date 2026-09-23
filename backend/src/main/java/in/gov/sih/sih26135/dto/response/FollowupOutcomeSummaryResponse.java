package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

public record FollowupOutcomeSummaryResponse(
    String followupTypeCode,
    Integer followupOffsetMonths,
    Long taskCount,
    Long traineeCount,
    BigDecimal successCount,
    BigDecimal noResponseCount,
    BigDecimal unreachableCount,
    BigDecimal successRatePct
) {}
