package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Institutional & Curriculum Performance Analytics Dashboards.
 * Exposes training provider rankings (Query Q04) and course outcome & employability scorecards (Query Q05).
 */
public interface InstitutionalAnalyticsService {

  /**
   * Retrieves the training provider performance leaderboard (formal query Q04).
   * Ranks providers by employment rate descending, tie-broken by enrollment count descending.
   *
   * @return strongly typed {@link LeaderboardResponse} of {@link ProviderPerformanceResponse}
   */
  LeaderboardResponse<ProviderPerformanceResponse> getProviderLeaderboard();

  /**
   * Retrieves the course outcome and employability leaderboard (formal query Q05).
   * Ranks courses by employment conversion rate descending, tie-broken by enrollment volume descending.
   *
   * @return strongly typed {@link LeaderboardResponse} of {@link CourseEmployabilityResponse}
   */
  LeaderboardResponse<CourseEmployabilityResponse> getCourseLeaderboard();

  /**
   * Groups course outcome and employability performance by qualification level.
   *
   * @return map of qualification level name to ranked list of course performance items
   */
  Map<String, List<CourseEmployabilityResponse>> getCourseLeaderboardByQualificationLevel();
}
