package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapDashboardResponse;

/**
 * Service interface for Skill Gap, Industry Demand Alignment, and Remediation Analytics.
 */
public interface SkillGapDashboardService {

  /**
   * Retrieves the consolidated skill gap dashboard, including top skill gap rankings
   * and severity mix with remediation recommendation uptake metrics.
   *
   * @return consolidated skill gap dashboard response
   */
  SkillGapDashboardResponse getSkillGapDashboard();
}
