package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing course-level wage progression and salary change
 * derived from formal analytical Query Q11.
 */
public record CourseWageProgressionResponse(
    Long courseId,
    String courseCode,
    String courseName,
    Long spellCount,
    BigDecimal avgFirstRecordedSalary,
    BigDecimal avgLatestRecordedSalary,
    BigDecimal avgSalaryChangePct
) {}
