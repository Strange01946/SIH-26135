package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing exit reasons and voluntary vs involuntary classification (formal Query Q17).
 */
public record AttritionReasonBreakdownResponse(
    Long employmentExitReasonId,
    String exitReasonCode,
    String exitReasonName,
    Long separationNatureId,
    String separationNatureCode,
    String separationNatureName,
    Boolean isVoluntaryFlag,
    Boolean isInvoluntaryFlag,
    Long exitCount,
    Long traineeCount,
    BigDecimal sharePct
) {}
