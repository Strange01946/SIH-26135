package in.gov.sih.sih26135.config.cache;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.AttritionDashboardResponse;
import in.gov.sih.sih26135.dto.response.AttritionReasonSummaryResponse;
import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.CourseOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.DistrictOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EconomicOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringPerformanceResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringSummaryResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.FollowupDashboardResponse;
import in.gov.sih.sih26135.dto.response.FollowupOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.LongitudinalRetentionResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.ProgramOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.ProviderOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;
import in.gov.sih.sih26135.dto.response.SkillGapDashboardResponse;
import in.gov.sih.sih26135.dto.response.SkillGapSummaryResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.dto.response.SurveyDashboardResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentReasonSummaryResponse;
import in.gov.sih.sih26135.dto.response.VerificationDashboardResponse;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * High-performance analytical caching delegate.
 *
 * <p>All methods in this delegate are cached using Spring Cache and Caffeine with tiered TTLs.
 * Authorization checks and tenancy validations are strictly performed in the invoking
 * service boundaries before delegating to this service, ensuring that cached results are
 * never returned to unauthorized callers and cache hits cannot bypass security.
 */
@Service
public class AnalyticsCacheDelegate {

  // =========================================================================
  // 1. National Overview Aggregates (Tier 1: 15 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW, keyGenerator = "analyticsKeyGenerator")
  public DashboardKpiStripResponse getDashboardKpiStrip(AnalyticsFilterCriteria criteria, java.util.function.Supplier<DashboardKpiStripResponse> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW, keyGenerator = "analyticsKeyGenerator")
  public PipelineFunnelResponse getTrainingPipelineFunnel(AnalyticsFilterCriteria criteria, java.util.function.Supplier<PipelineFunnelResponse> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW, keyGenerator = "analyticsKeyGenerator")
  public LeaderboardResponse<SchemeProgramPerformanceResponse> getSchemeProgramPerformanceMatrix(AnalyticsFilterCriteria criteria, java.util.function.Supplier<LeaderboardResponse<SchemeProgramPerformanceResponse>> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW, keyGenerator = "analyticsKeyGenerator")
  public ExecutiveDashboardResponse getExecutiveDashboard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<ExecutiveDashboardResponse> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW, keyGenerator = "analyticsKeyGenerator")
  public GeographicOverviewResponse getNationalGeographicOverview(AnalyticsFilterCriteria criteria, java.util.function.Supplier<GeographicOverviewResponse> computer) {
    return computer.get();
  }

  // =========================================================================
  // 2. State-Level Aggregates (Tier 2: 10 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_STATE_AGGREGATES, keyGenerator = "analyticsKeyGenerator")
  public StateGeographicResponse getStateGeographicAnalytics(Long stateId, AnalyticsFilterCriteria criteria, java.util.function.Supplier<StateGeographicResponse> computer) {
    return computer.get();
  }

  // =========================================================================
  // 3. District-Level Aggregates (Tier 3: 5 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_DISTRICT_AGGREGATES, keyGenerator = "analyticsKeyGenerator")
  public DistrictGeographicResponse getDistrictGeographicAnalytics(Long districtId, AnalyticsFilterCriteria criteria, java.util.function.Supplier<DistrictGeographicResponse> computer) {
    return computer.get();
  }

  // =========================================================================
  // 4. Institutional Analytics (Tier 2: 10 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_INSTITUTIONAL, keyGenerator = "analyticsKeyGenerator")
  public LeaderboardResponse<ProviderPerformanceResponse> getProviderLeaderboard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<LeaderboardResponse<ProviderPerformanceResponse>> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_INSTITUTIONAL, keyGenerator = "analyticsKeyGenerator")
  public LeaderboardResponse<CourseEmployabilityResponse> getCourseEmployabilityScorecard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<LeaderboardResponse<CourseEmployabilityResponse>> computer) {
    return computer.get();
  }

  // =========================================================================
  // 5. Economic Outcomes (Tier 2: 10 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_ECONOMIC, keyGenerator = "analyticsKeyGenerator")
  public LeaderboardResponse<EmployerHiringPerformanceResponse> getEmployerLeaderboard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<LeaderboardResponse<EmployerHiringPerformanceResponse>> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_ECONOMIC, keyGenerator = "analyticsKeyGenerator")
  public EconomicOutcomeSummaryResponse getEconomicOutcomeSummary(AnalyticsFilterCriteria criteria, java.util.function.Supplier<EconomicOutcomeSummaryResponse> computer) {
    return computer.get();
  }

  // =========================================================================
  // 6. Retention and Attrition (Tier 2: 10 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_RETENTION, keyGenerator = "analyticsKeyGenerator")
  public LongitudinalRetentionResponse getLongitudinalRetention(AnalyticsFilterCriteria criteria, java.util.function.Supplier<LongitudinalRetentionResponse> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_RETENTION, keyGenerator = "analyticsKeyGenerator")
  public AttritionDashboardResponse getAttritionDashboard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<AttritionDashboardResponse> computer) {
    return computer.get();
  }

  // =========================================================================
  // 7. Skill Gap Analytics (Tier 2: 10 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_SKILL_GAP, keyGenerator = "analyticsKeyGenerator")
  public SkillGapDashboardResponse getSkillGapDashboard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<SkillGapDashboardResponse> computer) {
    return computer.get();
  }

  // =========================================================================
  // 8. Verification & Survey Integrity (Tier 3: 5 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_VERIFICATION, keyGenerator = "analyticsKeyGenerator")
  public VerificationDashboardResponse getVerificationDashboard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<VerificationDashboardResponse> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_VERIFICATION, keyGenerator = "analyticsKeyGenerator")
  public FollowupDashboardResponse getFollowupDashboard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<FollowupDashboardResponse> computer) {
    return computer.get();
  }

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_VERIFICATION, keyGenerator = "analyticsKeyGenerator")
  public SurveyDashboardResponse getSurveyDashboard(AnalyticsFilterCriteria criteria, java.util.function.Supplier<SurveyDashboardResponse> computer) {
    return computer.get();
  }

  // =========================================================================
  // 9. Analytical Summaries & Leaderboards (Tier 3: 5 min TTL)
  // =========================================================================

  @Cacheable(cacheNames = AnalyticsCacheConfig.CACHE_SUMMARIES, keyGenerator = "analyticsKeyGenerator")
  public <T> List<T> getSummaryList(String summaryKey, java.util.function.Supplier<List<T>> computer) {
    return computer.get();
  }
}
