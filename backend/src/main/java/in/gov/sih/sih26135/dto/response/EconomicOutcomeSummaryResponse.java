package in.gov.sih.sih26135.dto.response;

import java.util.List;

/**
 * Consolidated response DTO representing economic outcomes, wage progression,
 * salary distribution, and employment mix analytics for the SIH-26135 platform
 * (Queries Q08, Q09, Q10, Q11).
 */
public record EconomicOutcomeSummaryResponse(
    WageProgressionResponse wageProgression,
    List<SalaryDistributionResponse> salaryDistribution,
    EmploymentMixResponse employmentMix,
    List<EmploymentCategoryBreakdownResponse> categoryBreakdown,
    List<CourseWageProgressionResponse> courseWageProgression
) {

  /**
   * Compact constructor enforcing non-null immutable lists.
   */
  public EconomicOutcomeSummaryResponse {
    salaryDistribution = salaryDistribution != null ? List.copyOf(salaryDistribution) : List.of();
    categoryBreakdown = categoryBreakdown != null ? List.copyOf(categoryBreakdown) : List.of();
    courseWageProgression = courseWageProgression != null ? List.copyOf(courseWageProgression) : List.of();
  }
}
