package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection record representing formal Query Q11 results
 * from {@code vw_salary_progression_fact} joined with {@code courses}.
 */
public record CourseWageProgressionProjection(
    Long courseId,
    String courseCode,
    String courseName,
    Long spellCount,
    BigDecimal avgFirstRecordedSalary,
    BigDecimal avgLatestRecordedSalary,
    BigDecimal avgSalaryChangePct
) {}
