package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.DistrictPerformanceResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.RegionalDisparityResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.dto.response.StatePerformanceResponse;
import in.gov.sih.sih26135.dto.response.TrainingCenterSummaryResponse;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.analytics.GeographicDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.DistrictPerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.projection.StatePerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.projection.TrainingCenterSummaryProjection;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.GeographicDashboardService;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link GeographicDashboardService}.
 * Executes formal analytical SQL query Q06, state rollups, and district analytics
 * using {@link GeographicDashboardRepository} and transforms results into standardized dashboard DTOs.
 */
@Service
@Transactional(readOnly = true)
public class GeographicDashboardServiceImpl implements GeographicDashboardService {

  private static final String RANKING_METRIC_Q06 = "employmentRatePct (asc - priority intervention)";
  private static final String RANKING_METRIC_STATE_DISTRICTS = "employmentRatePct (desc)";

  private static final Map<String, String> DATA_AVAILABILITY_NOTES = Map.of(
      "ruralUrbanClassification",
      "Not available: source schema lacks rural/urban flags on training centers, locations, or trainees.",
      "aspirationalDistrictIndicator",
      "Not available: source schema lacks official aspirational district classification.",
      "populationDensity",
      "Not available: source schema lacks district population and geographic area denominators."
  );

  private final GeographicDashboardRepository geographicDashboardRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService privacyService;
  private in.gov.sih.sih26135.config.cache.AnalyticsCacheDelegate cacheDelegate;

  @org.springframework.beans.factory.annotation.Autowired
  public GeographicDashboardServiceImpl(
      GeographicDashboardRepository geographicDashboardRepository,
      WorkflowSecurityScopeValidator scopeValidator,
      in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService privacyService) {
    this.geographicDashboardRepository = java.util.Objects.requireNonNull(geographicDashboardRepository, "GeographicDashboardRepository is required");
    this.scopeValidator = java.util.Objects.requireNonNull(scopeValidator, "WorkflowSecurityScopeValidator is required");
    this.privacyService = privacyService != null ? privacyService : new in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService();
  }

  public GeographicDashboardServiceImpl(
      GeographicDashboardRepository geographicDashboardRepository,
      WorkflowSecurityScopeValidator scopeValidator) {
    this(geographicDashboardRepository, scopeValidator, new in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService());
  }

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  public void setCacheDelegate(in.gov.sih.sih26135.config.cache.AnalyticsCacheDelegate cacheDelegate) {
    this.cacheDelegate = cacheDelegate;
  }

  @Override
  public GeographicOverviewResponse getNationalGeographicOverview() {
    scopeValidator.validateNationwideScope();
    if (cacheDelegate != null) {
      return cacheDelegate.getNationalGeographicOverview(null, this::computeUnfilteredNationalOverview);
    }
    return computeUnfilteredNationalOverview();
  }

  private GeographicOverviewResponse computeUnfilteredNationalOverview() {
    List<StatePerformanceProjection> stateProjections = geographicDashboardRepository.getStateSummaries();
    List<DistrictPerformanceProjection> q06Projections = geographicDashboardRepository.getFormalQ06DistrictOutcomes();

    List<StatePerformanceResponse> stateSummaries = new ArrayList<>();
    for (int i = 0; i < stateProjections.size(); i++) {
      stateSummaries.add(toStateResponse(stateProjections.get(i), i + 1));
    }

    List<DistrictPerformanceResponse> districtResponses = new ArrayList<>();
    for (int i = 0; i < q06Projections.size(); i++) {
      districtResponses.add(toDistrictResponse(q06Projections.get(i), i + 1));
    }

    long totalStates = stateSummaries.size();
    long totalDistricts = stateSummaries.stream().mapToLong(StatePerformanceResponse::districtCount).sum();
    long totalEnrollments = stateSummaries.stream().mapToLong(StatePerformanceResponse::enrollmentCount).sum();
    long totalCompleted = stateSummaries.stream().mapToLong(StatePerformanceResponse::completedCount).sum();
    long totalCertified = stateSummaries.stream().mapToLong(StatePerformanceResponse::certifiedCount).sum();
    long totalJoinedPlacement = stateSummaries.stream().mapToLong(StatePerformanceResponse::joinedPlacementCount).sum();
    long totalEmployed = stateSummaries.stream().mapToLong(StatePerformanceResponse::employedCount).sum();

    BigDecimal overallCompletionRatePct = AnalyticsCalculationUtils.calculatePercentage(totalCompleted, totalEnrollments);
    BigDecimal overallCertificationRatePct = AnalyticsCalculationUtils.calculatePercentage(totalCertified, totalEnrollments);
    BigDecimal overallPlacementRatePct = AnalyticsCalculationUtils.calculatePercentage(totalJoinedPlacement, totalEnrollments);
    BigDecimal overallEmploymentRatePct = AnalyticsCalculationUtils.calculatePercentage(totalEmployed, totalEnrollments);

    LeaderboardResponse<DistrictPerformanceResponse> districtRankings =
        LeaderboardResponse.of(RANKING_METRIC_Q06, districtResponses);

    RegionalDisparityResponse regionalDisparity = buildRegionalDisparity(
        districtResponses,
        geographicDashboardRepository.countDistrictsWithZeroEnrollments(),
        geographicDashboardRepository.countDistrictsWithoutTrainingCenters()
    );

    return privacyService.applyPrivacyGuardrail(new GeographicOverviewResponse(
        totalStates,
        totalDistricts,
        totalEnrollments,
        totalCompleted,
        totalCertified,
        totalJoinedPlacement,
        totalEmployed,
        overallCompletionRatePct,
        overallCertificationRatePct,
        overallPlacementRatePct,
        overallEmploymentRatePct,
        stateSummaries,
        districtRankings,
        regionalDisparity
    ));
  }

  @Override
  public StateGeographicResponse getStateGeographicAnalytics(Long stateId) {
    scopeValidator.validateStateScope(stateId);
    if (!geographicDashboardRepository.existsStateById(stateId)) {
      throw new ResourceNotFoundException("State not found with ID: " + stateId);
    }
    if (cacheDelegate != null) {
      return cacheDelegate.getStateGeographicAnalytics(stateId, null, () -> computeUnfilteredStateGeography(stateId));
    }
    return computeUnfilteredStateGeography(stateId);
  }

  private StateGeographicResponse computeUnfilteredStateGeography(Long stateId) {
    StatePerformanceProjection stateProj = geographicDashboardRepository.getStateSummaryById(stateId);
    List<DistrictPerformanceProjection> districtProjections =
        geographicDashboardRepository.getDistrictsByStateId(stateId);

    // Sort within-state districts by employment rate descending for performance evaluation
    List<DistrictPerformanceResponse> stateDistricts = new ArrayList<>();
    List<DistrictPerformanceProjection> sortedDistricts = districtProjections.stream()
        .sorted(Comparator.comparing(
            DistrictPerformanceProjection::employmentRatePct,
            Comparator.nullsLast(Comparator.reverseOrder())
        ).thenComparing(
            p -> toLong(p.enrollmentCount()),
            Comparator.reverseOrder()
        )).toList();

    for (int i = 0; i < sortedDistricts.size(); i++) {
      stateDistricts.add(toDistrictResponse(sortedDistricts.get(i), i + 1));
    }

    StatePerformanceResponse stateSummary = stateProj != null
        ? toStateResponse(stateProj, null)
        : null;

    Long totalTrainingCenters = stateSummary != null ? stateSummary.trainingCenterCount() : 0L;

    long districtsWithZeroEnrollment = stateDistricts.stream()
        .filter(d -> d.enrollmentCount() == 0L)
        .count();

    // In-state disparity
    RegionalDisparityResponse stateDisparity = buildRegionalDisparity(
        stateDistricts,
        districtsWithZeroEnrollment,
        0L // Center absence captured per district
    );

    LeaderboardResponse<DistrictPerformanceResponse> districtRankings =
        LeaderboardResponse.of(RANKING_METRIC_STATE_DISTRICTS, stateDistricts);

    return privacyService.applyPrivacyGuardrail(new StateGeographicResponse(
        stateId,
        stateSummary != null ? stateSummary.stateCode() : null,
        stateSummary != null ? stateSummary.stateName() : null,
        stateSummary,
        stateDistricts,
        districtRankings,
        totalTrainingCenters,
        stateDisparity
    ));
  }

  @Override
  public DistrictGeographicResponse getDistrictGeographicAnalytics(Long districtId) {
    scopeValidator.validateDistrictScope(districtId);
    if (!geographicDashboardRepository.existsDistrictById(districtId)) {
      throw new ResourceNotFoundException("District not found with ID: " + districtId);
    }
    if (cacheDelegate != null) {
      return cacheDelegate.getDistrictGeographicAnalytics(districtId, null, () -> computeUnfilteredDistrictGeography(districtId));
    }
    return computeUnfilteredDistrictGeography(districtId);
  }

  private DistrictGeographicResponse computeUnfilteredDistrictGeography(Long districtId) {
    DistrictPerformanceProjection districtProj = geographicDashboardRepository.getDistrictById(districtId);
    List<TrainingCenterSummaryProjection> centerProjs =
        geographicDashboardRepository.getTrainingCentersByDistrictId(districtId);

    List<TrainingCenterSummaryResponse> trainingCenters = centerProjs.stream()
        .map(c -> new TrainingCenterSummaryResponse(
            c.centerId(),
            c.centerCode(),
            c.centerName(),
            c.operationalCapacity(),
            c.pincode()
        )).toList();

    // Determine national rank from formal Q06 ordering
    List<DistrictPerformanceProjection> q06All = geographicDashboardRepository.getFormalQ06DistrictOutcomes();
    Integer nationalRank = null;
    for (int i = 0; i < q06All.size(); i++) {
      if (q06All.get(i).districtId().equals(districtId)) {
        nationalRank = i + 1;
        break;
      }
    }

    // Determine state rank within parent state
    Integer stateRank = null;
    if (districtProj != null && districtProj.stateId() != null) {
      List<DistrictPerformanceProjection> stateDistricts =
          geographicDashboardRepository.getDistrictsByStateId(districtProj.stateId());
      List<DistrictPerformanceProjection> sortedInState = stateDistricts.stream()
          .sorted(Comparator.comparing(
              DistrictPerformanceProjection::employmentRatePct,
              Comparator.nullsLast(Comparator.reverseOrder())
          ).thenComparing(
              p -> toLong(p.enrollmentCount()),
              Comparator.reverseOrder()
          )).toList();

      for (int i = 0; i < sortedInState.size(); i++) {
        if (sortedInState.get(i).districtId().equals(districtId)) {
          stateRank = i + 1;
          break;
        }
      }
    }

    DistrictPerformanceResponse performance = districtProj != null
        ? toDistrictResponse(districtProj, nationalRank)
        : null;

    Map<String, Object> regionalIndicators = new LinkedHashMap<>();
    regionalIndicators.put("hasTrainingCenter", !trainingCenters.isEmpty());
    regionalIndicators.put("trainingCenterCount", (long) trainingCenters.size());
    regionalIndicators.put("hasEnrollment", performance != null && performance.enrollmentCount() > 0L);
    regionalIndicators.put("dataAvailabilityNotes", DATA_AVAILABILITY_NOTES);

    return privacyService.applyPrivacyGuardrail(new DistrictGeographicResponse(
        districtId,
        districtProj != null ? districtProj.districtCode() : null,
        districtProj != null ? districtProj.districtName() : null,
        districtProj != null ? districtProj.stateId() : null,
        districtProj != null ? districtProj.stateCode() : null,
        districtProj != null ? districtProj.stateName() : null,
        performance,
        (long) trainingCenters.size(),
        trainingCenters,
        nationalRank,
        stateRank,
        regionalIndicators
    ));
  }

  @Override
  public LeaderboardResponse<DistrictPerformanceResponse> getDistrictRankings() {
    List<DistrictPerformanceProjection> q06Projections = geographicDashboardRepository.getFormalQ06DistrictOutcomes();
    List<DistrictPerformanceResponse> districtResponses = new ArrayList<>();
    for (int i = 0; i < q06Projections.size(); i++) {
      districtResponses.add(toDistrictResponse(q06Projections.get(i), i + 1));
    }
    return LeaderboardResponse.of(RANKING_METRIC_Q06, privacyService.applyPrivacyGuardrailsToDistricts(districtResponses));
  }

  @Override
  public GeographicOverviewResponse getNationalGeographicOverview(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getNationalGeographicOverview();
    }
    scopeValidator.validateNationwideScope();
    scopeValidator.validateAnalyticsFilterScope(criteria);
    if (cacheDelegate != null) {
      return cacheDelegate.getNationalGeographicOverview(criteria, () -> computeFilteredNationalOverview(criteria));
    }
    return computeFilteredNationalOverview(criteria);
  }

  private GeographicOverviewResponse computeFilteredNationalOverview(AnalyticsFilterCriteria criteria) {
    List<StatePerformanceProjection> stateProjections = geographicDashboardRepository.getStateSummaries(criteria);
    List<DistrictPerformanceProjection> q06Projections = geographicDashboardRepository.getFormalQ06DistrictOutcomes(criteria);

    List<StatePerformanceResponse> stateSummaries = new ArrayList<>();
    for (int i = 0; i < stateProjections.size(); i++) {
      stateSummaries.add(toStateResponse(stateProjections.get(i), i + 1));
    }

    List<DistrictPerformanceResponse> districtResponses = new ArrayList<>();
    for (int i = 0; i < q06Projections.size(); i++) {
      districtResponses.add(toDistrictResponse(q06Projections.get(i), i + 1));
    }

    long totalStates = stateSummaries.size();
    long totalDistricts = stateSummaries.stream().mapToLong(StatePerformanceResponse::districtCount).sum();
    long totalEnrollments = stateSummaries.stream().mapToLong(StatePerformanceResponse::enrollmentCount).sum();
    long totalCompleted = stateSummaries.stream().mapToLong(StatePerformanceResponse::completedCount).sum();
    long totalCertified = stateSummaries.stream().mapToLong(StatePerformanceResponse::certifiedCount).sum();
    long totalJoinedPlacement = stateSummaries.stream().mapToLong(StatePerformanceResponse::joinedPlacementCount).sum();
    long totalEmployed = stateSummaries.stream().mapToLong(StatePerformanceResponse::employedCount).sum();

    BigDecimal overallCompletionRatePct = AnalyticsCalculationUtils.calculatePercentage(totalCompleted, totalEnrollments);
    BigDecimal overallCertificationRatePct = AnalyticsCalculationUtils.calculatePercentage(totalCertified, totalEnrollments);
    BigDecimal overallPlacementRatePct = AnalyticsCalculationUtils.calculatePercentage(totalJoinedPlacement, totalEnrollments);
    BigDecimal overallEmploymentRatePct = AnalyticsCalculationUtils.calculatePercentage(totalEmployed, totalEnrollments);

    LeaderboardResponse<DistrictPerformanceResponse> districtRankings =
        LeaderboardResponse.of(RANKING_METRIC_Q06, districtResponses);

    RegionalDisparityResponse regionalDisparity = buildRegionalDisparity(
        districtResponses,
        geographicDashboardRepository.countDistrictsWithZeroEnrollments(),
        geographicDashboardRepository.countDistrictsWithoutTrainingCenters()
    );

    return privacyService.applyPrivacyGuardrail(new GeographicOverviewResponse(
        totalStates,
        totalDistricts,
        totalEnrollments,
        totalCompleted,
        totalCertified,
        totalJoinedPlacement,
        totalEmployed,
        overallCompletionRatePct,
        overallCertificationRatePct,
        overallPlacementRatePct,
        overallEmploymentRatePct,
        stateSummaries,
        districtRankings,
        regionalDisparity
    ));
  }

  @Override
  public StateGeographicResponse getStateGeographicAnalytics(Long stateId, AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getStateGeographicAnalytics(stateId);
    }
    scopeValidator.validateStateScope(stateId);
    scopeValidator.validateAnalyticsFilterScope(criteria);
    if (!geographicDashboardRepository.existsStateById(stateId)) {
      throw new ResourceNotFoundException("State not found with ID: " + stateId);
    }
    if (cacheDelegate != null) {
      return cacheDelegate.getStateGeographicAnalytics(stateId, criteria, () -> computeFilteredStateGeography(stateId, criteria));
    }
    return computeFilteredStateGeography(stateId, criteria);
  }

  private StateGeographicResponse computeFilteredStateGeography(Long stateId, AnalyticsFilterCriteria criteria) {
    StatePerformanceProjection stateProj = geographicDashboardRepository.getStateSummaryById(stateId, criteria);
    List<DistrictPerformanceProjection> districtProjections =
        geographicDashboardRepository.getDistrictsByStateId(stateId, criteria);

    List<DistrictPerformanceResponse> stateDistricts = new ArrayList<>();
    List<DistrictPerformanceProjection> sortedDistricts = districtProjections.stream()
        .sorted(Comparator.comparing(
            DistrictPerformanceProjection::employmentRatePct,
            Comparator.nullsLast(Comparator.reverseOrder())
        ).thenComparing(
            p -> toLong(p.enrollmentCount()),
            Comparator.reverseOrder()
        )).toList();

    for (int i = 0; i < sortedDistricts.size(); i++) {
      stateDistricts.add(toDistrictResponse(sortedDistricts.get(i), i + 1));
    }

    StatePerformanceResponse stateSummary = stateProj != null
        ? toStateResponse(stateProj, null)
        : null;

    Long totalTrainingCenters = stateSummary != null ? stateSummary.trainingCenterCount() : 0L;

    long districtsWithZeroEnrollment = stateDistricts.stream()
        .filter(d -> d.enrollmentCount() == 0L)
        .count();

    RegionalDisparityResponse stateDisparity = buildRegionalDisparity(
        stateDistricts,
        districtsWithZeroEnrollment,
        0L
    );

    LeaderboardResponse<DistrictPerformanceResponse> districtRankings =
        LeaderboardResponse.of(RANKING_METRIC_STATE_DISTRICTS, stateDistricts);

    return privacyService.applyPrivacyGuardrail(new StateGeographicResponse(
        stateId,
        stateSummary != null ? stateSummary.stateCode() : null,
        stateSummary != null ? stateSummary.stateName() : null,
        stateSummary,
        stateDistricts,
        districtRankings,
        totalTrainingCenters,
        stateDisparity
    ));
  }

  @Override
  public DistrictGeographicResponse getDistrictGeographicAnalytics(Long districtId, AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getDistrictGeographicAnalytics(districtId);
    }
    scopeValidator.validateDistrictScope(districtId);
    scopeValidator.validateAnalyticsFilterScope(criteria);
    if (!geographicDashboardRepository.existsDistrictById(districtId)) {
      throw new ResourceNotFoundException("District not found with ID: " + districtId);
    }
    if (cacheDelegate != null) {
      return cacheDelegate.getDistrictGeographicAnalytics(districtId, criteria, () -> computeFilteredDistrictGeography(districtId, criteria));
    }
    return computeFilteredDistrictGeography(districtId, criteria);
  }

  private DistrictGeographicResponse computeFilteredDistrictGeography(Long districtId, AnalyticsFilterCriteria criteria) {
    DistrictPerformanceProjection districtProj = geographicDashboardRepository.getDistrictById(districtId, criteria);
    List<TrainingCenterSummaryProjection> centerProjs =
        geographicDashboardRepository.getTrainingCentersByDistrictId(districtId);

    List<TrainingCenterSummaryResponse> trainingCenters = centerProjs.stream()
        .map(c -> new TrainingCenterSummaryResponse(
            c.centerId(),
            c.centerCode(),
            c.centerName(),
            c.operationalCapacity(),
            c.pincode()
        )).toList();

    List<DistrictPerformanceProjection> q06All = geographicDashboardRepository.getFormalQ06DistrictOutcomes(criteria);
    Integer nationalRank = null;
    for (int i = 0; i < q06All.size(); i++) {
      if (q06All.get(i).districtId().equals(districtId)) {
        nationalRank = i + 1;
        break;
      }
    }

    Integer stateRank = null;
    if (districtProj != null && districtProj.stateId() != null) {
      List<DistrictPerformanceProjection> stateDistricts =
          geographicDashboardRepository.getDistrictsByStateId(districtProj.stateId(), criteria);
      List<DistrictPerformanceProjection> sortedInState = stateDistricts.stream()
          .sorted(Comparator.comparing(
              DistrictPerformanceProjection::employmentRatePct,
              Comparator.nullsLast(Comparator.reverseOrder())
          ).thenComparing(
              p -> toLong(p.enrollmentCount()),
              Comparator.reverseOrder()
          )).toList();

      for (int i = 0; i < sortedInState.size(); i++) {
        if (sortedInState.get(i).districtId().equals(districtId)) {
          stateRank = i + 1;
          break;
        }
      }
    }

    DistrictPerformanceResponse performance = districtProj != null
        ? toDistrictResponse(districtProj, nationalRank)
        : null;

    Map<String, Object> regionalIndicators = new LinkedHashMap<>();
    regionalIndicators.put("hasTrainingCenter", !trainingCenters.isEmpty());
    regionalIndicators.put("trainingCenterCount", (long) trainingCenters.size());
    regionalIndicators.put("hasEnrollment", performance != null && performance.enrollmentCount() > 0L);
    regionalIndicators.put("dataAvailabilityNotes", DATA_AVAILABILITY_NOTES);

    return privacyService.applyPrivacyGuardrail(new DistrictGeographicResponse(
        districtId,
        districtProj != null ? districtProj.districtCode() : null,
        districtProj != null ? districtProj.districtName() : null,
        districtProj != null ? districtProj.stateId() : null,
        districtProj != null ? districtProj.stateCode() : null,
        districtProj != null ? districtProj.stateName() : null,
        performance,
        (long) trainingCenters.size(),
        trainingCenters,
        nationalRank,
        stateRank,
        regionalIndicators
    ));
  }

  @Override
  public LeaderboardResponse<DistrictPerformanceResponse> getDistrictRankings(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getDistrictRankings();
    }
    scopeValidator.validateNationwideScope();
    scopeValidator.validateAnalyticsFilterScope(criteria);
    List<DistrictPerformanceProjection> projections = geographicDashboardRepository.getFormalQ06DistrictOutcomes(criteria);
    List<DistrictPerformanceResponse> items = new ArrayList<>();
    for (int i = 0; i < projections.size(); i++) {
      items.add(toDistrictResponse(projections.get(i), i + 1));
    }
    return LeaderboardResponse.of(RANKING_METRIC_Q06, privacyService.applyPrivacyGuardrailsToDistricts(items));
  }

  private RegionalDisparityResponse buildRegionalDisparity(
      List<DistrictPerformanceResponse> districts,
      Long zeroEnrollmentCount,
      Long zeroCentersCount) {

    List<DistrictPerformanceResponse> withRates = districts.stream()
        .filter(d -> d.employmentRatePct() != null)
        .toList();

    List<DistrictPerformanceResponse> lowestPerforming = withRates.stream()
        .sorted(Comparator.comparing(DistrictPerformanceResponse::employmentRatePct)
            .thenComparing(DistrictPerformanceResponse::enrollmentCount, Comparator.reverseOrder()))
        .limit(5)
        .toList();

    List<DistrictPerformanceResponse> topPerforming = withRates.stream()
        .sorted(Comparator.comparing(DistrictPerformanceResponse::employmentRatePct, Comparator.reverseOrder())
            .thenComparing(DistrictPerformanceResponse::enrollmentCount, Comparator.reverseOrder()))
        .limit(5)
        .toList();

    BigDecimal employmentRateGap = null;
    if (!lowestPerforming.isEmpty() && !topPerforming.isEmpty()) {
      BigDecimal minRate = lowestPerforming.get(0).employmentRatePct();
      BigDecimal maxRate = topPerforming.get(0).employmentRatePct();
      if (minRate != null && maxRate != null) {
        employmentRateGap = maxRate.subtract(minRate);
      }
    }

    BigDecimal placementRateGap = null;
    List<DistrictPerformanceResponse> withPlacementRates = districts.stream()
        .filter(d -> d.placementRatePct() != null)
        .sorted(Comparator.comparing(DistrictPerformanceResponse::placementRatePct))
        .toList();

    if (withPlacementRates.size() >= 2) {
      BigDecimal minPl = withPlacementRates.get(0).placementRatePct();
      BigDecimal maxPl = withPlacementRates.get(withPlacementRates.size() - 1).placementRatePct();
      placementRateGap = maxPl.subtract(minPl);
    }

    return new RegionalDisparityResponse(
        lowestPerforming,
        topPerforming,
        employmentRateGap,
        placementRateGap,
        zeroEnrollmentCount != null ? zeroEnrollmentCount : 0L,
        zeroCentersCount != null ? zeroCentersCount : 0L,
        DATA_AVAILABILITY_NOTES
    );
  }

  private DistrictPerformanceResponse toDistrictResponse(DistrictPerformanceProjection p, Integer rank) {
    Long enrollmentCount = toLong(p.enrollmentCount());
    Long completedCount = toLong(p.completedCount());
    Long certifiedCount = toLong(p.certifiedCount());
    Long joinedPlacementCount = toLong(p.joinedPlacementCount());
    Long employedCount = toLong(p.employedCount());

    BigDecimal completionRatePct = p.completionRatePct() != null
        ? AnalyticsCalculationUtils.round(p.completionRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(completedCount, enrollmentCount);

    BigDecimal certificationRatePct = p.certificationRatePct() != null
        ? AnalyticsCalculationUtils.round(p.certificationRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(certifiedCount, enrollmentCount);

    BigDecimal placementRatePct = p.placementRatePct() != null
        ? AnalyticsCalculationUtils.round(p.placementRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(joinedPlacementCount, enrollmentCount);

    BigDecimal employmentRatePct = p.employmentRatePct() != null
        ? AnalyticsCalculationUtils.round(p.employmentRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(employedCount, enrollmentCount);

    return new DistrictPerformanceResponse(
        p.districtId(),
        p.districtCode(),
        p.districtName(),
        p.stateId(),
        p.stateCode(),
        p.stateName(),
        enrollmentCount,
        toLong(p.traineeCount()),
        completedCount,
        certifiedCount,
        joinedPlacementCount,
        employedCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct,
        rank
    );
  }

  private StatePerformanceResponse toStateResponse(StatePerformanceProjection p, Integer rank) {
    Long enrollmentCount = toLong(p.enrollmentCount());
    Long completedCount = toLong(p.completedCount());
    Long certifiedCount = toLong(p.certifiedCount());
    Long joinedPlacementCount = toLong(p.joinedPlacementCount());
    Long employedCount = toLong(p.employedCount());

    BigDecimal completionRatePct = p.completionRatePct() != null
        ? AnalyticsCalculationUtils.round(p.completionRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(completedCount, enrollmentCount);

    BigDecimal certificationRatePct = p.certificationRatePct() != null
        ? AnalyticsCalculationUtils.round(p.certificationRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(certifiedCount, enrollmentCount);

    BigDecimal placementRatePct = p.placementRatePct() != null
        ? AnalyticsCalculationUtils.round(p.placementRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(joinedPlacementCount, enrollmentCount);

    BigDecimal employmentRatePct = p.employmentRatePct() != null
        ? AnalyticsCalculationUtils.round(p.employmentRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(employedCount, enrollmentCount);

    return new StatePerformanceResponse(
        p.stateId(),
        p.stateCode(),
        p.stateName(),
        toLong(p.districtCount()),
        enrollmentCount,
        toLong(p.traineeCount()),
        completedCount,
        certifiedCount,
        joinedPlacementCount,
        employedCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct,
        toLong(p.trainingCenterCount()),
        rank
    );
  }

  private Long toLong(Number number) {
    if (number == null) {
      return 0L;
    }
    return number.longValue();
  }
}
