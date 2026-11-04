package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection record representing salary bracket counts from {@code vw_salary_progression_fact}.
 */
public record SalaryBracketCountProjection(
    String bracketCode,
    Long count
) {}
