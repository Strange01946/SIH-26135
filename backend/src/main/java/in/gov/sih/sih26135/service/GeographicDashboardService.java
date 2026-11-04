package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.DistrictPerformanceResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;

/**
 * Service interface for Geographic, State, and District Heatmap Dashboards.
 */
public interface GeographicDashboardService {

  /**
   * Retrieves the national geographic overview combining state outcome rollups,
   * Q06 district rankings, and regional disparity analytics.
   *
   * @return consolidated {@link GeographicOverviewResponse}
   */
  GeographicOverviewResponse getNationalGeographicOverview();

  /**
   * Retrieves state-specific outcome and geographic analytics for a valid state.
   *
   * @param stateId unique identifier of the state
   * @return state-specific {@link StateGeographicResponse}
   */
  StateGeographicResponse getStateGeographicAnalytics(Long stateId);

  /**
   * Retrieves district-specific outcome and geographic analytics for a valid district.
   *
   * @param districtId unique identifier of the district
   * @return district-specific {@link DistrictGeographicResponse}
   */
  DistrictGeographicResponse getDistrictGeographicAnalytics(Long districtId);

  /**
   * Retrieves formal query Q06 district outcome rankings leaderboard.
   *
   * @return district performance leaderboard
   */
  LeaderboardResponse<DistrictPerformanceResponse> getDistrictRankings();
}
