package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing aggregate separation totals, voluntary vs involuntary mix,
 * voluntary-to-involuntary ratio, and notice compliance metrics (formal Query Q18).
 *
 * <p>Notice compliance rate evaluates safely to {@code null} because notice periods are not
 * tracked in the underlying schema.
 */
public record SeparationAnalysisResponse(
    Long totalExitCount,
    Long voluntaryExitCount,
    Long involuntaryExitCount,
    Long otherExitCount,
    BigDecimal voluntaryExitSharePct,
    BigDecimal involuntaryExitSharePct,
    BigDecimal otherExitSharePct,
    BigDecimal voluntaryToInvoluntaryRatio,
    BigDecimal noticeComplianceRatePct
) {}
