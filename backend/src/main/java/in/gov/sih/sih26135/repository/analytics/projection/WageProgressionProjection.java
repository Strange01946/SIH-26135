package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection record representing formal Query Q10 results
 * from {@code vw_salary_progression_fact}.
 */
public record WageProgressionProjection(
    Long employmentWithSalaryRowCount,
    Long firstSalaryCount,
    Long latestSalaryCount,
    BigDecimal avgFirstRecordedSalary,
    BigDecimal avgLatestRecordedSalary,
    BigDecimal avgSalaryChangeAmount,
    BigDecimal avgSalaryChangePct
) {}
