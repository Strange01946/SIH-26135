package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

public record SkillGapSummaryResponse(
    Long skillId,
    String skillCode,
    Long skillGapSeverityId,
    String severityCode,
    Long gapCount,
    Long traineeCount,
    BigDecimal currentGapCount,
    BigDecimal avgGapLevelDelta
) {}
