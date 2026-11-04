package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EconomicOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringPerformanceResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;

/**
 * Service interface defining contracts for employer hiring performance,
 * wage progression, economic uplift, and employment mix analytics.
 */
public interface EconomicOutcomeDashboardService {

  /**
   * Retrieves the employer hiring performance leaderboard scorecard (formal Query Q07).
   *
   * @return generic leaderboard response containing ranked employer performance items
   */
  LeaderboardResponse<EmployerHiringPerformanceResponse> getEmployerLeaderboard();

  /**
   * Retrieves the consolidated economic outcome summary including wage progression (Q10),
   * salary bracket distribution, employment mix (Q08), engagement category breakdown (Q09),
   * and course-level wage progression (Q11).
   *
   * @return consolidated economic outcome summary response
   */
  EconomicOutcomeSummaryResponse getEconomicOutcomeSummary();
}
