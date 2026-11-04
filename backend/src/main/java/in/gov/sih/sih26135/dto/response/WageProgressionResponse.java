package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing wage progression and salary delta KPIs
 * derived from formal analytical Query Q10.
 */
public record WageProgressionResponse(
    Long employmentSpellCount,
    Long firstSalaryCount,
    Long latestSalaryCount,
    BigDecimal avgFirstRecordedSalary,
    BigDecimal avgLatestRecordedSalary,
    BigDecimal avgSalaryChangeAmount,
    BigDecimal avgSalaryChangePct
) {}
