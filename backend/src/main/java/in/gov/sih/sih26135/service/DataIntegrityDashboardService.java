package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.FollowupDashboardResponse;
import in.gov.sih.sih26135.dto.response.SurveyDashboardResponse;
import in.gov.sih.sih26135.dto.response.VerificationDashboardResponse;

/**
 * Service interface for Verification Integrity, Longitudinal Follow-up, and Survey Analytics.
 */
public interface DataIntegrityDashboardService {

  /**
   * Retrieves verification integrity and method efficacy analytics (formal Queries Q21 & Q22).
   *
   * @return consolidated verification dashboard response
   */
  VerificationDashboardResponse getVerificationDashboard();

  /**
   * Retrieves longitudinal follow-up outreach success and SLA status analytics (formal Query Q19).
   *
   * @return consolidated follow-up dashboard response
   */
  FollowupDashboardResponse getFollowupDashboard();

  /**
   * Retrieves post-training survey response pipeline and satisfaction analytics (formal Query Q20).
   *
   * @return consolidated survey dashboard response
   */
  SurveyDashboardResponse getSurveyDashboard();
}
