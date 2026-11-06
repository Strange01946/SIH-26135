package in.gov.sih.sih26135.service.privacy;

import in.gov.sih.sih26135.dto.response.AttritionDashboardResponse;
import in.gov.sih.sih26135.dto.response.AttritionReasonBreakdownResponse;
import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.CourseWageProgressionResponse;
import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.DistrictPerformanceResponse;
import in.gov.sih.sih26135.dto.response.EconomicOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringPerformanceResponse;
import in.gov.sih.sih26135.dto.response.EmploymentCategoryBreakdownResponse;
import in.gov.sih.sih26135.dto.response.EmploymentMixResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.FollowupDashboardResponse;
import in.gov.sih.sih26135.dto.response.FollowupWavePerformanceResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.LongitudinalRetentionResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.dto.response.RegionalDisparityResponse;
import in.gov.sih.sih26135.dto.response.RetentionCohortResponse;
import in.gov.sih.sih26135.dto.response.RetentionMilestoneResponse;
import in.gov.sih.sih26135.dto.response.SalaryDistributionResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;
import in.gov.sih.sih26135.dto.response.SeparationAnalysisResponse;
import in.gov.sih.sih26135.dto.response.SkillGapDashboardResponse;
import in.gov.sih.sih26135.dto.response.SkillGapSeverityBreakdownResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.dto.response.StatePerformanceResponse;
import in.gov.sih.sih26135.dto.response.SurveyDashboardResponse;
import in.gov.sih.sih26135.dto.response.SurveyPurposePipelineResponse;
import in.gov.sih.sih26135.dto.response.TopSkillGapResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentReasonBreakdownResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentSummaryResponse;
import in.gov.sih.sih26135.dto.response.VerificationDashboardResponse;
import in.gov.sih.sih26135.dto.response.VerificationMethodEfficacyResponse;
import in.gov.sih.sih26135.dto.response.VerificationStatusBreakdownResponse;
import in.gov.sih.sih26135.dto.response.WageProgressionResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Statistical Disclosure Control (SDC) and k-anonymity privacy suppression service.
 *
 * <p>Enforces the minimum anonymity threshold of {@code k = 5} distinct individuals across
 * all analytical dashboard and summary responses.
 *
 * <p>Key Guarantees:
 * <ul>
 *   <li>Distinct Individuals: Denominators and thresholds are evaluated based on distinct
 *       trainees / individuals, never raw transaction or fact row counts.</li>
 *   <li>Independent Cell-Level k=5: Sensitive person-level outcome counts (completed, certified,
 *       joined, employed, etc.) and corresponding rates are independently suppressed when sparse
 *       (1..4), even when parent cohort meets k >= 5.</li>
 *   <li>Safe Zeros Preserved: Legitimate zero counts (0) and zero rates (0.00%) are preserved
 *       where non-reconstructive.</li>
 *   <li>Secondary Suppression: In additive breakdowns, distributions, or geographic lists
 *       where the sum is known or published, if exactly 1 cell is suppressed via primary
 *       suppression, the smallest non-suppressed sibling cell with {@code count >= 5} is also
 *       suppressed to prevent deductive complement reconstruction.</li>
 *   <li>Sensitive Measure Protection: Starting wages, joining salaries, and salary progressions
 *       are suppressed to {@code null} if fewer than 5 distinct individuals contribute valid
 *       salary observations, or if the denominator cannot be safely established.</li>
 * </ul>
 */
@Service
public class AnalyticsPrivacySuppressionService {

  public static final long MIN_ANONYMITY_THRESHOLD = 5L;

  /**
   * Evaluates whether a person-level count falls below the k-anonymity threshold (1 to 4 distinct persons).
   * Safe zero (0) returns false as non-reconstructive zero.
   * Null returns false.
   */
  public boolean isSparse(Long count) {
    return count != null && count > 0 && count < MIN_ANONYMITY_THRESHOLD;
  }

  public boolean isSparse(Number count) {
    return count != null && count.longValue() > 0 && count.longValue() < MIN_ANONYMITY_THRESHOLD;
  }

  /**
   * Evaluates count threshold: returns null if 1 <= count < 5; preserves 0 and >= 5.
   */
  public Long suppressCount(Long count) {
    if (isSparse(count)) {
      return null;
    }
    return count;
  }

  /**
   * Suppresses rate/percentage if associated distinct count is suppressed (sparse 1..4).
   */
  public BigDecimal suppressRate(BigDecimal rate, Long distinctPersonCount) {
    if (isSparse(distinctPersonCount)) {
      return null;
    }
    return rate;
  }

  /**
   * Suppresses sensitive salary if salary-contributing population < 5 or cannot be established.
   */
  public BigDecimal suppressSalary(BigDecimal salary, Long distinctSalaryCount) {
    if (salary == null) {
      return null;
    }
    if (distinctSalaryCount == null || isSparse(distinctSalaryCount)) {
      return null;
    }
    return salary;
  }

  // =========================================================================
  // 1. Geographic Dashboards & Rollups
  // =========================================================================

  public DistrictPerformanceResponse applyPrivacyGuardrail(DistrictPerformanceResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new DistrictPerformanceResponse(
          response.districtId(),
          response.districtCode(),
          response.districtName(),
          response.stateId(),
          response.stateCode(),
          response.stateName(),
          null, // enrollmentCount
          null, // traineeCount
          null, // completedCount
          null, // certifiedCount
          null, // joinedPlacementCount
          null, // employedCount
          null, // completionRatePct
          null, // certificationRatePct
          null, // placementRatePct
          null, // employmentRatePct
          response.rank()
      );
    }

    Long enrollmentCount = isSparse(response.enrollmentCount()) ? null : response.enrollmentCount();
    Long traineeCount = response.traineeCount();

    boolean sComp = isSparse(response.completedCount());
    Long completedCount = sComp ? null : response.completedCount();
    BigDecimal completionRatePct = sComp ? null : response.completionRatePct();

    boolean sCert = isSparse(response.certifiedCount());
    Long certifiedCount = sCert ? null : response.certifiedCount();
    BigDecimal certificationRatePct = sCert ? null : response.certificationRatePct();

    boolean sJoin = isSparse(response.joinedPlacementCount());
    Long joinedPlacementCount = sJoin ? null : response.joinedPlacementCount();
    BigDecimal placementRatePct = sJoin ? null : response.placementRatePct();

    boolean sEmp = isSparse(response.employedCount());
    Long employedCount = sEmp ? null : response.employedCount();
    BigDecimal employmentRatePct = sEmp ? null : response.employmentRatePct();

    return new DistrictPerformanceResponse(
        response.districtId(),
        response.districtCode(),
        response.districtName(),
        response.stateId(),
        response.stateCode(),
        response.stateName(),
        enrollmentCount,
        traineeCount,
        completedCount,
        certifiedCount,
        joinedPlacementCount,
        employedCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct,
        response.rank()
    );
  }

  public List<DistrictPerformanceResponse> applyPrivacyGuardrailsToDistricts(
      List<DistrictPerformanceResponse> responses) {
    if (responses == null || responses.isEmpty()) {
      return responses != null ? responses : Collections.emptyList();
    }
    List<DistrictPerformanceResponse> step1 = new ArrayList<>(responses.size());
    for (DistrictPerformanceResponse r : responses) {
      step1.add(applyPrivacyGuardrail(r));
    }

    // Secondary suppression on traineeCount if exactly 1 district was primary-suppressed:
    int primarySuppressedTrainees = 0;
    for (DistrictPerformanceResponse r : responses) {
      if (isSparse(r.traineeCount())) {
        primarySuppressedTrainees++;
      }
    }

    if (primarySuppressedTrainees == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        DistrictPerformanceResponse d = step1.get(i);
        if (d.traineeCount() != null && d.traineeCount() >= MIN_ANONYMITY_THRESHOLD) {
          if (minVal == null || d.traineeCount() < minVal) {
            minVal = d.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        DistrictPerformanceResponse target = step1.get(minIdx);
        step1.set(minIdx, new DistrictPerformanceResponse(
            target.districtId(), target.districtCode(), target.districtName(),
            target.stateId(), target.stateCode(), target.stateName(),
            null, null, null, null, null, null,
            null, null, null, null,
            target.rank()
        ));
      }
    }

    // Secondary suppression on sensitive outcome: employedCount
    int primarySuppressedEmployed = 0;
    for (DistrictPerformanceResponse r : responses) {
      if (isSparse(r.employedCount())) {
        primarySuppressedEmployed++;
      }
    }
    if (primarySuppressedEmployed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        DistrictPerformanceResponse d = step1.get(i);
        if (d.employedCount() != null && d.employedCount() >= MIN_ANONYMITY_THRESHOLD) {
          if (minVal == null || d.employedCount() < minVal) {
            minVal = d.employedCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        DistrictPerformanceResponse target = step1.get(minIdx);
        step1.set(minIdx, new DistrictPerformanceResponse(
            target.districtId(), target.districtCode(), target.districtName(),
            target.stateId(), target.stateCode(), target.stateName(),
            target.enrollmentCount(), target.traineeCount(), target.completedCount(), target.certifiedCount(),
            target.joinedPlacementCount(), null,
            target.completionRatePct(), target.certificationRatePct(), target.placementRatePct(),
            null,
            target.rank()
        ));
      }
    }

    return step1;
  }

  public StatePerformanceResponse applyPrivacyGuardrail(StatePerformanceResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new StatePerformanceResponse(
          response.stateId(),
          response.stateCode(),
          response.stateName(),
          response.districtCount(),
          null, // enrollmentCount
          null, // traineeCount
          null, // completedCount
          null, // certifiedCount
          null, // joinedPlacementCount
          null, // employedCount
          null, // completionRatePct
          null, // certificationRatePct
          null, // placementRatePct
          null, // employmentRatePct
          response.trainingCenterCount(),
          response.rank()
      );
    }

    Long enrollmentCount = isSparse(response.enrollmentCount()) ? null : response.enrollmentCount();
    Long traineeCount = response.traineeCount();

    boolean sComp = isSparse(response.completedCount());
    Long completedCount = sComp ? null : response.completedCount();
    BigDecimal completionRatePct = sComp ? null : response.completionRatePct();

    boolean sCert = isSparse(response.certifiedCount());
    Long certifiedCount = sCert ? null : response.certifiedCount();
    BigDecimal certificationRatePct = sCert ? null : response.certificationRatePct();

    boolean sJoin = isSparse(response.joinedPlacementCount());
    Long joinedPlacementCount = sJoin ? null : response.joinedPlacementCount();
    BigDecimal placementRatePct = sJoin ? null : response.placementRatePct();

    boolean sEmp = isSparse(response.employedCount());
    Long employedCount = sEmp ? null : response.employedCount();
    BigDecimal employmentRatePct = sEmp ? null : response.employmentRatePct();

    return new StatePerformanceResponse(
        response.stateId(),
        response.stateCode(),
        response.stateName(),
        response.districtCount(),
        enrollmentCount,
        traineeCount,
        completedCount,
        certifiedCount,
        joinedPlacementCount,
        employedCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct,
        response.trainingCenterCount(),
        response.rank()
    );
  }

  public List<StatePerformanceResponse> applyPrivacyGuardrailsToStates(
      List<StatePerformanceResponse> responses) {
    if (responses == null || responses.isEmpty()) {
      return responses != null ? responses : Collections.emptyList();
    }
    List<StatePerformanceResponse> step1 = new ArrayList<>(responses.size());
    for (StatePerformanceResponse r : responses) {
      step1.add(applyPrivacyGuardrail(r));
    }

    // Secondary suppression on traineeCount if exactly 1 state was primary-suppressed:
    int primarySuppressedTrainees = 0;
    for (StatePerformanceResponse r : responses) {
      if (isSparse(r.traineeCount())) {
        primarySuppressedTrainees++;
      }
    }

    if (primarySuppressedTrainees == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        StatePerformanceResponse s = step1.get(i);
        if (s.traineeCount() != null && s.traineeCount() >= MIN_ANONYMITY_THRESHOLD) {
          if (minVal == null || s.traineeCount() < minVal) {
            minVal = s.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        StatePerformanceResponse target = step1.get(minIdx);
        step1.set(minIdx, new StatePerformanceResponse(
            target.stateId(), target.stateCode(), target.stateName(),
            target.districtCount(),
            null, null, null, null, null, null,
            null, null, null, null,
            target.trainingCenterCount(), target.rank()
        ));
      }
    }

    // Secondary suppression on sensitive outcome: employedCount
    int primarySuppressedEmployed = 0;
    for (StatePerformanceResponse r : responses) {
      if (isSparse(r.employedCount())) {
        primarySuppressedEmployed++;
      }
    }
    if (primarySuppressedEmployed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        StatePerformanceResponse s = step1.get(i);
        if (s.employedCount() != null && s.employedCount() >= MIN_ANONYMITY_THRESHOLD) {
          if (minVal == null || s.employedCount() < minVal) {
            minVal = s.employedCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        StatePerformanceResponse target = step1.get(minIdx);
        step1.set(minIdx, new StatePerformanceResponse(
            target.stateId(), target.stateCode(), target.stateName(),
            target.districtCount(),
            target.enrollmentCount(), target.traineeCount(), target.completedCount(), target.certifiedCount(),
            target.joinedPlacementCount(), null,
            target.completionRatePct(), target.certificationRatePct(), target.placementRatePct(),
            null,
            target.trainingCenterCount(), target.rank()
        ));
      }
    }

    return step1;
  }

  public GeographicOverviewResponse applyPrivacyGuardrail(GeographicOverviewResponse response) {
    if (response == null) {
      return null;
    }
    List<StatePerformanceResponse> safeStates = applyPrivacyGuardrailsToStates(response.stateSummaries());
    LeaderboardResponse<DistrictPerformanceResponse> safeDistricts = response.districtRankings() != null
        ? LeaderboardResponse.of(
            response.districtRankings().rankingMetric(),
            applyPrivacyGuardrailsToDistricts(response.districtRankings().items()))
        : null;

    if (isSparse(response.totalEnrollments())) {
      return new GeographicOverviewResponse(
          response.totalStates(),
          response.totalDistricts(),
          null, null, null, null, null, // total counts
          null, null, null, null, // rates
          safeStates,
          safeDistricts,
          response.regionalDisparity()
      );
    }

    // Withhold published totals if they would reconstruct a single suppressed state:
    boolean anyStateSuppressed = false;
    int unsuppressedStates = 0;
    if (safeStates != null) {
      for (StatePerformanceResponse s : safeStates) {
        if (s.traineeCount() == null) {
          anyStateSuppressed = true;
        } else {
          unsuppressedStates++;
        }
      }
    }
    boolean withholdTotals = anyStateSuppressed && unsuppressedStates < 2;

    Long totalEnrollments = withholdTotals ? null : response.totalEnrollments();
    Long totalCompleted = withholdTotals || isSparse(response.totalCompleted()) ? null : response.totalCompleted();
    Long totalCertified = withholdTotals || isSparse(response.totalCertified()) ? null : response.totalCertified();
    Long totalJoined = withholdTotals || isSparse(response.totalJoinedPlacement()) ? null : response.totalJoinedPlacement();
    Long totalEmployed = withholdTotals || isSparse(response.totalEmployed()) ? null : response.totalEmployed();

    BigDecimal overallCompRate = withholdTotals || isSparse(response.totalCompleted()) ? null : response.overallCompletionRatePct();
    BigDecimal overallCertRate = withholdTotals || isSparse(response.totalCertified()) ? null : response.overallCertificationRatePct();
    BigDecimal overallPlaceRate = withholdTotals || isSparse(response.totalJoinedPlacement()) ? null : response.overallPlacementRatePct();
    BigDecimal overallEmpRate = withholdTotals || isSparse(response.totalEmployed()) ? null : response.overallEmploymentRatePct();

    return new GeographicOverviewResponse(
        response.totalStates(),
        response.totalDistricts(),
        totalEnrollments,
        totalCompleted,
        totalCertified,
        totalJoined,
        totalEmployed,
        overallCompRate,
        overallCertRate,
        overallPlaceRate,
        overallEmpRate,
        safeStates,
        safeDistricts,
        response.regionalDisparity()
    );
  }

  public StateGeographicResponse applyPrivacyGuardrail(StateGeographicResponse response) {
    if (response == null) {
      return null;
    }
    StatePerformanceResponse safeState = applyPrivacyGuardrail(response.stateSummary());
    List<DistrictPerformanceResponse> safeDistricts = applyPrivacyGuardrailsToDistricts(response.districts());
    LeaderboardResponse<DistrictPerformanceResponse> safeRankings = response.districtRankings() != null
        ? LeaderboardResponse.of(
            response.districtRankings().rankingMetric(),
            applyPrivacyGuardrailsToDistricts(response.districtRankings().items()))
        : null;

    // If safeDistricts has a single district suppressed and no sibling exists to protect it:
    boolean anyDistrictSuppressed = false;
    int unsuppressedDistricts = 0;
    if (safeDistricts != null) {
      for (DistrictPerformanceResponse d : safeDistricts) {
        if (d.traineeCount() == null) {
          anyDistrictSuppressed = true;
        } else {
          unsuppressedDistricts++;
        }
      }
    }
    if (anyDistrictSuppressed && unsuppressedDistricts < 2 && safeState != null) {
      safeState = new StatePerformanceResponse(
          safeState.stateId(), safeState.stateCode(), safeState.stateName(),
          safeState.districtCount(),
          null, null, null, null, null, null,
          null, null, null, null,
          safeState.trainingCenterCount(), safeState.rank()
      );
    }

    return new StateGeographicResponse(
        response.stateId(),
        response.stateCode(),
        response.stateName(),
        safeState,
        safeDistricts,
        safeRankings,
        response.totalTrainingCenters(),
        response.stateDisparity()
    );
  }

  public DistrictGeographicResponse applyPrivacyGuardrail(DistrictGeographicResponse response) {
    if (response == null) {
      return null;
    }
    DistrictPerformanceResponse safePerf = applyPrivacyGuardrail(response.performance());
    Map<String, Object> safeIndicators = response.regionalIndicators();
    if (safePerf != null && safePerf.traineeCount() == null && safeIndicators != null) {
      safeIndicators = new LinkedHashMap<>(safeIndicators);
      safeIndicators.put("hasEnrollment", null);
    }
    return new DistrictGeographicResponse(
        response.districtId(),
        response.districtCode(),
        response.districtName(),
        response.stateId(),
        response.stateCode(),
        response.stateName(),
        safePerf,
        response.trainingCenterCount(),
        response.trainingCenters(),
        response.nationalRank(),
        response.stateRank(),
        safeIndicators
    );
  }

  // =========================================================================
  // 2. Institutional & Curriculum Analytics
  // =========================================================================

  public ProviderPerformanceResponse applyPrivacyGuardrail(ProviderPerformanceResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new ProviderPerformanceResponse(
          response.providerId(),
          response.providerCode(),
          response.providerName(),
          null, null, null, null, null, null, // counts
          null, null, null, null, // rates
          response.activeBatchCapacity(),
          response.rank()
      );
    }

    Long enrollmentCount = isSparse(response.enrollmentCount()) ? null : response.enrollmentCount();
    Long traineeCount = response.traineeCount();

    boolean sComp = isSparse(response.completedCount());
    Long completedCount = sComp ? null : response.completedCount();
    BigDecimal completionRatePct = sComp ? null : response.completionRatePct();

    boolean sCert = isSparse(response.certifiedCount());
    Long certifiedCount = sCert ? null : response.certifiedCount();
    BigDecimal certificationRatePct = sCert ? null : response.certificationRatePct();

    boolean sJoin = isSparse(response.joinedPlacementCount());
    Long joinedPlacementCount = sJoin ? null : response.joinedPlacementCount();
    BigDecimal placementRatePct = sJoin ? null : response.placementRatePct();

    boolean sEmp = isSparse(response.employedCount());
    Long employedCount = sEmp ? null : response.employedCount();
    BigDecimal employmentRatePct = sEmp ? null : response.employmentRatePct();

    return new ProviderPerformanceResponse(
        response.providerId(),
        response.providerCode(),
        response.providerName(),
        enrollmentCount,
        traineeCount,
        completedCount,
        certifiedCount,
        joinedPlacementCount,
        employedCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct,
        response.activeBatchCapacity(),
        response.rank()
    );
  }

  public List<ProviderPerformanceResponse> applyPrivacyGuardrailsToProviders(
      List<ProviderPerformanceResponse> responses) {
    if (responses == null || responses.isEmpty()) {
      return responses != null ? responses : Collections.emptyList();
    }
    List<ProviderPerformanceResponse> safeList = new ArrayList<>(responses.size());
    for (ProviderPerformanceResponse r : responses) {
      safeList.add(applyPrivacyGuardrail(r));
    }
    return safeList;
  }

  public CourseEmployabilityResponse applyPrivacyGuardrail(
      CourseEmployabilityResponse response, Long salaryContributorCount) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new CourseEmployabilityResponse(
          response.courseId(),
          response.courseCode(),
          response.courseName(),
          response.qualificationLevelId(),
          response.qualificationLevelCode(),
          response.qualificationLevelName(),
          response.nsqfLevel(),
          null, null, null, null, null, null,
          null, null, null, null, null,
          null, // avgStartingWage suppressed
          response.rank()
      );
    }

    Long enrollmentCount = isSparse(response.enrollmentCount()) ? null : response.enrollmentCount();
    Long traineeCount = response.traineeCount();

    boolean sComp = isSparse(response.completedCount());
    Long completedCount = sComp ? null : response.completedCount();
    BigDecimal completionRatePct = sComp ? null : response.completionRatePct();
    BigDecimal dropoutRatePct = sComp ? null : response.dropoutRatePct();

    boolean sCert = isSparse(response.certifiedCount());
    Long certifiedCount = sCert ? null : response.certifiedCount();
    BigDecimal certificationRatePct = sCert ? null : response.certificationRatePct();

    boolean sJoin = isSparse(response.joinedPlacementCount());
    Long joinedPlacementCount = sJoin ? null : response.joinedPlacementCount();
    BigDecimal placementRatePct = sJoin ? null : response.placementRatePct();

    boolean sEmp = isSparse(response.employedCount());
    Long employedCount = sEmp ? null : response.employedCount();
    BigDecimal employmentRatePct = sEmp ? null : response.employmentRatePct();

    // Sensitive salary protection: count distinct trainees contributing valid salary observations
    BigDecimal safeStartingWage = response.avgStartingWage();
    if (safeStartingWage != null) {
      if (salaryContributorCount != null) {
        if (isSparse(salaryContributorCount)) {
          safeStartingWage = null;
        }
      } else if (sEmp) {
        safeStartingWage = null;
      }
    }

    return new CourseEmployabilityResponse(
        response.courseId(),
        response.courseCode(),
        response.courseName(),
        response.qualificationLevelId(),
        response.qualificationLevelCode(),
        response.qualificationLevelName(),
        response.nsqfLevel(),
        enrollmentCount,
        traineeCount,
        completedCount,
        certifiedCount,
        joinedPlacementCount,
        employedCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct,
        dropoutRatePct,
        safeStartingWage,
        response.rank()
    );
  }

  public CourseEmployabilityResponse applyPrivacyGuardrail(CourseEmployabilityResponse response) {
    return applyPrivacyGuardrail(response, null);
  }

  public List<CourseEmployabilityResponse> applyPrivacyGuardrailsToCourses(
      List<CourseEmployabilityResponse> responses) {
    if (responses == null || responses.isEmpty()) {
      return responses != null ? responses : Collections.emptyList();
    }
    List<CourseEmployabilityResponse> safeList = new ArrayList<>(responses.size());
    for (CourseEmployabilityResponse r : responses) {
      safeList.add(applyPrivacyGuardrail(r));
    }
    return safeList;
  }

  // =========================================================================
  // 3. Economic Outcomes, Salary & Employment Mix
  // =========================================================================

  public EmployerHiringPerformanceResponse applyPrivacyGuardrail(
      EmployerHiringPerformanceResponse response, Long salaryContributorCount) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new EmployerHiringPerformanceResponse(
          response.employerId(),
          response.employerCode(),
          response.employerName(),
          null, null, null,
          null, null,
          response.rank()
      );
    }

    Long placementCount = isSparse(response.placementCount()) ? null : response.placementCount();
    Long traineeCount = response.traineeCount();

    boolean sJoin = isSparse(response.joinedCount());
    Long joinedCount = sJoin ? null : response.joinedCount();
    BigDecimal joinRatePct = sJoin ? null : response.joinRatePct();

    BigDecimal safeJoiningSalary = response.avgJoiningSalary();
    if (safeJoiningSalary != null) {
      if (salaryContributorCount != null) {
        if (isSparse(salaryContributorCount)) {
          safeJoiningSalary = null;
        }
      } else if (sJoin) {
        safeJoiningSalary = null;
      }
    }

    return new EmployerHiringPerformanceResponse(
        response.employerId(),
        response.employerCode(),
        response.employerName(),
        placementCount,
        traineeCount,
        joinedCount,
        joinRatePct,
        safeJoiningSalary,
        response.rank()
    );
  }

  public EmployerHiringPerformanceResponse applyPrivacyGuardrail(EmployerHiringPerformanceResponse response) {
    return applyPrivacyGuardrail(response, null);
  }

  public List<EmployerHiringPerformanceResponse> applyPrivacyGuardrailsToEmployers(
      List<EmployerHiringPerformanceResponse> responses) {
    if (responses == null || responses.isEmpty()) {
      return responses != null ? responses : Collections.emptyList();
    }
    List<EmployerHiringPerformanceResponse> safeList = new ArrayList<>(responses.size());
    for (EmployerHiringPerformanceResponse r : responses) {
      safeList.add(applyPrivacyGuardrail(r));
    }
    return safeList;
  }

  /**
   * Applies primary and secondary (complementary) suppression to salary distribution brackets.
   * If exactly 1 bracket is suppressed via primary suppression, the smallest non-suppressed bracket
   * is also suppressed to prevent complement reconstruction.
   */
  public List<SalaryDistributionResponse> applyPrivacyGuardrailToSalaryDistribution(
      List<SalaryDistributionResponse> distribution) {
    if (distribution == null || distribution.isEmpty()) {
      return distribution != null ? distribution : Collections.emptyList();
    }

    List<SalaryDistributionResponse> step1 = new ArrayList<>(distribution.size());
    int primarySuppressedCount = 0;
    long totalCount = 0L;

    for (SalaryDistributionResponse bracket : distribution) {
      if (bracket.count() != null) {
        totalCount += bracket.count();
      }
      if (isSparse(bracket.count())) {
        step1.add(new SalaryDistributionResponse(
            bracket.bracketCode(),
            bracket.bracketName(),
            null,
            null
        ));
        primarySuppressedCount++;
      } else {
        step1.add(bracket);
      }
    }

    // If total population is sparse (1..4), all brackets must be suppressed
    if (isSparse(totalCount)) {
      List<SalaryDistributionResponse> fullySuppressed = new ArrayList<>(distribution.size());
      for (SalaryDistributionResponse b : distribution) {
        fullySuppressed.add(new SalaryDistributionResponse(b.bracketCode(), b.bracketName(), null, null));
      }
      return fullySuppressed;
    }

    // Secondary suppression needed if exactly 1 bracket is suppressed
    if (primarySuppressedCount == 1) {
      int minIndex = -1;
      Long minCount = null;
      for (int i = 0; i < step1.size(); i++) {
        SalaryDistributionResponse b = step1.get(i);
        if (b.count() != null && b.count() > 0) {
          if (minCount == null || b.count() < minCount) {
            minCount = b.count();
            minIndex = i;
          }
        }
      }
      if (minIndex != -1) {
        SalaryDistributionResponse target = step1.get(minIndex);
        step1.set(minIndex, new SalaryDistributionResponse(
            target.bracketCode(),
            target.bracketName(),
            null,
            null
        ));
      }
    }

    return step1;
  }

  public WageProgressionResponse applyPrivacyGuardrail(WageProgressionResponse response) {
    if (response == null) {
      return null;
    }
    Long spellCount = response.employmentSpellCount();
    Long firstCount = response.firstSalaryCount();
    Long latestCount = response.latestSalaryCount();

    boolean suppressSpell = isSparse(spellCount);
    boolean suppressFirst = isSparse(firstCount);
    boolean suppressLatest = isSparse(latestCount);

    BigDecimal firstSal = (suppressFirst || (firstCount != null && firstCount == 0))
        ? null : response.avgFirstRecordedSalary();
    BigDecimal latestSal = (suppressLatest || (latestCount != null && latestCount == 0))
        ? null : response.avgLatestRecordedSalary();
    BigDecimal deltaAmt = (suppressFirst || suppressLatest) ? null : response.avgSalaryChangeAmount();
    BigDecimal deltaPct = (suppressFirst || suppressLatest) ? null : response.avgSalaryChangePct();

    return new WageProgressionResponse(
        suppressSpell ? null : spellCount,
        suppressFirst ? null : firstCount,
        suppressLatest ? null : latestCount,
        firstSal,
        latestSal,
        deltaAmt,
        deltaPct
    );
  }

  public CourseWageProgressionResponse applyPrivacyGuardrail(CourseWageProgressionResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.spellCount())) {
      return new CourseWageProgressionResponse(
          response.courseId(),
          response.courseCode(),
          response.courseName(),
          null, null, null, null
      );
    }
    return response;
  }

  public List<CourseWageProgressionResponse> applyPrivacyGuardrailsToCourseWageProgression(
      List<CourseWageProgressionResponse> responses) {
    if (responses == null || responses.isEmpty()) {
      return responses != null ? responses : Collections.emptyList();
    }
    List<CourseWageProgressionResponse> safeList = new ArrayList<>(responses.size());
    for (CourseWageProgressionResponse r : responses) {
      safeList.add(applyPrivacyGuardrail(r));
    }
    return safeList;
  }

  public EmploymentMixResponse applyPrivacyGuardrail(EmploymentMixResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new EmploymentMixResponse(
          null, null, null, null, null, null, null, null, null,
          null, null, null, null, null, null
      );
    }
    return response;
  }

  public List<EmploymentCategoryBreakdownResponse> applyPrivacyGuardrailsToCategoryBreakdown(
      List<EmploymentCategoryBreakdownResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<EmploymentCategoryBreakdownResponse> step1 = new ArrayList<>(items.size());
    int primarySuppressed = 0;
    for (EmploymentCategoryBreakdownResponse item : items) {
      if (isSparse(item.traineeCount())) {
        step1.add(new EmploymentCategoryBreakdownResponse(
            item.engagementTypeCode(), item.engagementTypeName(),
            item.isWageEmployment(), item.isSelfEmployment(), item.isApprenticeship(), item.isInternship(),
            null, null, null, null, null
        ));
        primarySuppressed++;
      } else {
        step1.add(item);
      }
    }

    if (primarySuppressed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        EmploymentCategoryBreakdownResponse it = step1.get(i);
        if (it.traineeCount() != null && it.traineeCount() > 0) {
          if (minVal == null || it.traineeCount() < minVal) {
            minVal = it.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        EmploymentCategoryBreakdownResponse t = step1.get(minIdx);
        step1.set(minIdx, new EmploymentCategoryBreakdownResponse(
            t.engagementTypeCode(), t.engagementTypeName(),
            t.isWageEmployment(), t.isSelfEmployment(), t.isApprenticeship(), t.isInternship(),
            null, null, null, null, null
        ));
      }
    }
    return step1;
  }

  public EconomicOutcomeSummaryResponse applyPrivacyGuardrail(EconomicOutcomeSummaryResponse response) {
    if (response == null) {
      return null;
    }
    return new EconomicOutcomeSummaryResponse(
        applyPrivacyGuardrail(response.wageProgression()),
        applyPrivacyGuardrailToSalaryDistribution(response.salaryDistribution()),
        applyPrivacyGuardrail(response.employmentMix()),
        applyPrivacyGuardrailsToCategoryBreakdown(response.categoryBreakdown()),
        applyPrivacyGuardrailsToCourseWageProgression(response.courseWageProgression())
    );
  }

  // =========================================================================
  // 4. Longitudinal Retention & Attrition
  // =========================================================================

  public RetentionMilestoneResponse applyPrivacyGuardrail(RetentionMilestoneResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.observableCount())) {
      return new RetentionMilestoneResponse(
          response.milestone(),
          response.milestoneMonths(),
          null, null, null
      );
    }
    return response;
  }

  public List<RetentionMilestoneResponse> applyPrivacyGuardrailsToRetentionMilestones(
      List<RetentionMilestoneResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<RetentionMilestoneResponse> safeList = new ArrayList<>(items.size());
    for (RetentionMilestoneResponse r : items) {
      safeList.add(applyPrivacyGuardrail(r));
    }
    return safeList;
  }

  public RetentionCohortResponse applyPrivacyGuardrail(RetentionCohortResponse response) {
    if (response == null) {
      return null;
    }
    boolean s6 = isSparse(response.observable6mCount());
    boolean s12 = isSparse(response.observable12mCount());
    boolean s24 = isSparse(response.observable24mCount());
    boolean s36 = isSparse(response.observable36mCount());
    boolean sSpells = isSparse(response.employmentSpellCount());

    return new RetentionCohortResponse(
        sSpells ? null : response.employmentSpellCount(),
        s6 ? null : response.observable6mCount(),
        s6 ? null : response.retained6mCount(),
        s6 ? null : response.retention6mPct(),
        s12 ? null : response.observable12mCount(),
        s12 ? null : response.retained12mCount(),
        s12 ? null : response.retention12mPct(),
        s24 ? null : response.observable24mCount(),
        s24 ? null : response.retained24mCount(),
        s24 ? null : response.retention24mPct(),
        s36 ? null : response.observable36mCount(),
        s36 ? null : response.retained36mCount(),
        s36 ? null : response.retention36mPct()
    );
  }

  public LongitudinalRetentionResponse applyPrivacyGuardrail(LongitudinalRetentionResponse response) {
    if (response == null) {
      return null;
    }
    Long totalSpells = isSparse(response.employmentSpellCount()) ? null : response.employmentSpellCount();
    return new LongitudinalRetentionResponse(
        totalSpells,
        applyPrivacyGuardrail(response.cohortSummary()),
        applyPrivacyGuardrailsToRetentionMilestones(response.milestones())
    );
  }

  public SeparationAnalysisResponse applyPrivacyGuardrail(SeparationAnalysisResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.totalExitCount())) {
      return new SeparationAnalysisResponse(null, null, null, null, null, null, null, null, null);
    }

    boolean sVol = isSparse(response.voluntaryExitCount());
    boolean sInvol = isSparse(response.involuntaryExitCount());
    boolean sOther = isSparse(response.otherExitCount());

    int suppressedCount = (sVol ? 1 : 0) + (sInvol ? 1 : 0) + (sOther ? 1 : 0);

    if (suppressedCount == 1) {
      Long minVal = null;
      int minCategory = -1; // 1: vol, 2: invol, 3: other

      if (!sVol && response.voluntaryExitCount() != null && response.voluntaryExitCount() > 0) {
        minVal = response.voluntaryExitCount();
        minCategory = 1;
      }
      if (!sInvol && response.involuntaryExitCount() != null && response.involuntaryExitCount() > 0) {
        if (minVal == null || response.involuntaryExitCount() < minVal) {
          minVal = response.involuntaryExitCount();
          minCategory = 2;
        }
      }
      if (!sOther && response.otherExitCount() != null && response.otherExitCount() > 0) {
        if (minVal == null || response.otherExitCount() < minVal) {
          minCategory = 3;
        }
      }

      if (minCategory == 1) sVol = true;
      else if (minCategory == 2) sInvol = true;
      else if (minCategory == 3) sOther = true;
    }

    Long volCount = sVol ? null : response.voluntaryExitCount();
    BigDecimal volShare = sVol ? null : response.voluntaryExitSharePct();
    Long involCount = sInvol ? null : response.involuntaryExitCount();
    BigDecimal involShare = sInvol ? null : response.involuntaryExitSharePct();
    Long otherCount = sOther ? null : response.otherExitCount();
    BigDecimal otherShare = sOther ? null : response.otherExitSharePct();
    BigDecimal ratio = (sVol || sInvol) ? null : response.voluntaryToInvoluntaryRatio();

    return new SeparationAnalysisResponse(
        response.totalExitCount(),
        volCount,
        involCount,
        otherCount,
        volShare,
        involShare,
        otherShare,
        ratio,
        response.noticeComplianceRatePct()
    );
  }

  public UnemploymentSummaryResponse applyPrivacyGuardrail(UnemploymentSummaryResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new UnemploymentSummaryResponse(
          null, null, null, null, null, null, null, null
      );
    }
    return response;
  }

  public List<AttritionReasonBreakdownResponse> applyPrivacyGuardrailsToAttritionReasons(
      List<AttritionReasonBreakdownResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<AttritionReasonBreakdownResponse> step1 = new ArrayList<>(items.size());
    int primarySuppressed = 0;
    for (AttritionReasonBreakdownResponse item : items) {
      if (isSparse(item.traineeCount())) {
        step1.add(new AttritionReasonBreakdownResponse(
            item.employmentExitReasonId(), item.exitReasonCode(), item.exitReasonName(),
            item.separationNatureId(), item.separationNatureCode(), item.separationNatureName(),
            item.isVoluntaryFlag(), item.isInvoluntaryFlag(),
            null, null, null
        ));
        primarySuppressed++;
      } else {
        step1.add(item);
      }
    }

    if (primarySuppressed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        AttritionReasonBreakdownResponse it = step1.get(i);
        if (it.traineeCount() != null && it.traineeCount() > 0) {
          if (minVal == null || it.traineeCount() < minVal) {
            minVal = it.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        AttritionReasonBreakdownResponse t = step1.get(minIdx);
        step1.set(minIdx, new AttritionReasonBreakdownResponse(
            t.employmentExitReasonId(), t.exitReasonCode(), t.exitReasonName(),
            t.separationNatureId(), t.separationNatureCode(), t.separationNatureName(),
            t.isVoluntaryFlag(), t.isInvoluntaryFlag(),
            null, null, null
        ));
      }
    }
    return step1;
  }

  public List<UnemploymentReasonBreakdownResponse> applyPrivacyGuardrailsToUnemploymentReasons(
      List<UnemploymentReasonBreakdownResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<UnemploymentReasonBreakdownResponse> step1 = new ArrayList<>(items.size());
    int primarySuppressed = 0;
    for (UnemploymentReasonBreakdownResponse item : items) {
      if (isSparse(item.traineeCount())) {
        step1.add(new UnemploymentReasonBreakdownResponse(
            item.unemploymentReasonId(), item.unemploymentReasonCode(), item.reasonName(),
            null, null, null, null, null, null, null
        ));
        primarySuppressed++;
      } else {
        step1.add(item);
      }
    }

    if (primarySuppressed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        UnemploymentReasonBreakdownResponse it = step1.get(i);
        if (it.traineeCount() != null && it.traineeCount() > 0) {
          if (minVal == null || it.traineeCount() < minVal) {
            minVal = it.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        UnemploymentReasonBreakdownResponse t = step1.get(minIdx);
        step1.set(minIdx, new UnemploymentReasonBreakdownResponse(
            t.unemploymentReasonId(), t.unemploymentReasonCode(), t.reasonName(),
            null, null, null, null, null, null, null
        ));
      }
    }
    return step1;
  }

  public AttritionDashboardResponse applyPrivacyGuardrail(AttritionDashboardResponse response) {
    if (response == null) {
      return null;
    }
    return new AttritionDashboardResponse(
        applyPrivacyGuardrail(response.unemploymentSummary()),
        applyPrivacyGuardrailsToUnemploymentReasons(response.unemploymentReasons()),
        applyPrivacyGuardrail(response.separationAnalysis()),
        applyPrivacyGuardrailsToAttritionReasons(response.exitReasons())
    );
  }

  // =========================================================================
  // 5. Executive Overview & Pipeline Funnel
  // =========================================================================

  public DashboardKpiStripResponse applyPrivacyGuardrail(DashboardKpiStripResponse response) {
    if (response == null) {
      return null;
    }
    Long traineeCount = response.traineeCount() != null ? response.traineeCount() : response.enrolledTraineeCount();
    if (isSparse(traineeCount)) {
      return new DashboardKpiStripResponse(
          null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
      );
    }

    Long enrollmentCount = isSparse(response.enrollmentCount()) ? null : response.enrollmentCount();
    Long enrolledTraineeCount = response.enrolledTraineeCount();

    boolean sComp = isSparse(response.completedEnrollmentCount());
    Long completedCount = sComp ? null : response.completedEnrollmentCount();
    BigDecimal completionRatePct = sComp ? null : response.completionRatePct();

    boolean sCert = isSparse(response.certifiedEnrollmentCount());
    Long certifiedCount = sCert ? null : response.certifiedEnrollmentCount();
    BigDecimal certificationRatePct = sCert ? null : response.certificationRatePct();

    Long placementCount = isSparse(response.placementCount()) ? null : response.placementCount();

    boolean sJoin = isSparse(response.joinedPlacementCount());
    Long joinedPlacementCount = sJoin ? null : response.joinedPlacementCount();
    BigDecimal placementRatePct = sJoin ? null : response.placementRatePct();

    boolean sEmp = isSparse(response.employedTraineeCount());
    Long employedTraineeCount = sEmp ? null : response.employedTraineeCount();
    BigDecimal employmentRatePct = sEmp ? null : response.employmentRatePct();

    Long spellCount = isSparse(response.employmentSpellCount()) ? null : response.employmentSpellCount();
    Long currentSpell = (sEmp || isSparse(response.currentEmploymentSpellCount())) ? null : response.currentEmploymentSpellCount();
    Long currentSelf = (sEmp || isSparse(response.currentSelfEmploymentCount())) ? null : response.currentSelfEmploymentCount();
    Long currentApp = (sEmp || isSparse(response.currentApprenticeshipCount())) ? null : response.currentApprenticeshipCount();

    return new DashboardKpiStripResponse(
        traineeCount,
        enrollmentCount,
        enrolledTraineeCount,
        completedCount,
        certifiedCount,
        placementCount,
        joinedPlacementCount,
        spellCount,
        employedTraineeCount,
        currentSpell,
        currentSelf,
        currentApp,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct
    );
  }

  public PipelineFunnelResponse applyPrivacyGuardrail(PipelineFunnelResponse response, Long distinctTraineeCount) {
    if (response == null) {
      return null;
    }
    if (distinctTraineeCount != null) {
      if (isSparse(distinctTraineeCount)) {
        return new PipelineFunnelResponse(null, null, null, null, null, null, null, null, null);
      }
    } else if (response.enrollmentCount() != null && isSparse(response.enrollmentCount())) {
      return new PipelineFunnelResponse(null, null, null, null, null, null, null, null, null);
    }

    Long enrollmentCount = isSparse(response.enrollmentCount()) ? null : response.enrollmentCount();

    boolean sComp = isSparse(response.completedCount());
    Long completedCount = sComp ? null : response.completedCount();
    BigDecimal completionRatePct = sComp ? null : response.completionRatePct();

    boolean sCert = isSparse(response.certifiedCount());
    Long certifiedCount = sCert ? null : response.certifiedCount();
    BigDecimal certificationRatePct = sCert ? null : response.certificationRatePct();

    boolean sJoin = isSparse(response.joinedPlacementCount());
    Long joinedPlacementCount = sJoin ? null : response.joinedPlacementCount();
    BigDecimal placementRatePct = sJoin ? null : response.placementRatePct();

    boolean sEmp = isSparse(response.employedCount());
    Long employedCount = sEmp ? null : response.employedCount();
    BigDecimal employmentRatePct = sEmp ? null : response.employmentRatePct();

    return new PipelineFunnelResponse(
        enrollmentCount,
        completedCount,
        certifiedCount,
        joinedPlacementCount,
        employedCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct
    );
  }

  public PipelineFunnelResponse applyPrivacyGuardrail(PipelineFunnelResponse response) {
    return applyPrivacyGuardrail(response, null);
  }

  public SchemeProgramPerformanceResponse applyPrivacyGuardrail(SchemeProgramPerformanceResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new SchemeProgramPerformanceResponse(
          response.programId(), response.programCode(), response.programName(),
          response.schemeId(), response.schemeCode(), response.schemeName(),
          null, null, null, null, null, null, null, null,
          null, null, null, null
      );
    }

    Long enrollmentCount = isSparse(response.enrollmentCount()) ? null : response.enrollmentCount();
    Long traineeCount = response.traineeCount();

    boolean sComp = isSparse(response.completedCount());
    Long completedCount = sComp ? null : response.completedCount();
    BigDecimal completionRatePct = sComp ? null : response.completionRatePct();

    boolean sCert = isSparse(response.certifiedCount());
    Long certifiedCount = sCert ? null : response.certifiedCount();
    BigDecimal certificationRatePct = sCert ? null : response.certificationRatePct();

    boolean sJoin = isSparse(response.joinedPlacementCount());
    Long joinedPlacementCount = sJoin ? null : response.joinedPlacementCount();
    BigDecimal placementRatePct = sJoin ? null : response.placementRatePct();

    boolean sEmp = isSparse(response.employedCount());
    Long employedCount = sEmp ? null : response.employedCount();
    BigDecimal employmentRatePct = sEmp ? null : response.employmentRatePct();

    Long selfEmp = isSparse(response.selfEmploymentSpellCount()) ? null : response.selfEmploymentSpellCount();
    Long app = isSparse(response.apprenticeshipSpellCount()) ? null : response.apprenticeshipSpellCount();

    return new SchemeProgramPerformanceResponse(
        response.programId(), response.programCode(), response.programName(),
        response.schemeId(), response.schemeCode(), response.schemeName(),
        enrollmentCount, traineeCount,
        completedCount, certifiedCount,
        joinedPlacementCount, employedCount,
        selfEmp, app,
        completionRatePct, certificationRatePct,
        placementRatePct, employmentRatePct
    );
  }

  public List<SchemeProgramPerformanceResponse> applyPrivacyGuardrailsToSchemePrograms(
      List<SchemeProgramPerformanceResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<SchemeProgramPerformanceResponse> safeList = new ArrayList<>(items.size());
    for (SchemeProgramPerformanceResponse r : items) {
      safeList.add(applyPrivacyGuardrail(r));
    }
    return safeList;
  }

  public ExecutiveDashboardResponse applyPrivacyGuardrail(ExecutiveDashboardResponse response) {
    if (response == null) {
      return null;
    }
    LeaderboardResponse<SchemeProgramPerformanceResponse> safeMatrix = response.schemeProgramMatrix() != null
        ? LeaderboardResponse.of(
            response.schemeProgramMatrix().rankingMetric(),
            applyPrivacyGuardrailsToSchemePrograms(response.schemeProgramMatrix().items()))
        : null;

    return new ExecutiveDashboardResponse(
        applyPrivacyGuardrail(response.kpiStrip()),
        applyPrivacyGuardrail(response.pipelineFunnel()),
        safeMatrix
    );
  }

  // =========================================================================
  // 6. Skill Gap Analytics
  // =========================================================================

  public TopSkillGapResponse applyPrivacyGuardrail(TopSkillGapResponse response) {
    if (response == null) {
      return null;
    }
    if (isSparse(response.traineeCount())) {
      return new TopSkillGapResponse(
          response.skillId(), response.skillCode(), response.skillName(),
          response.skillGapSeverityId(), response.severityCode(),
          null, null, null, null
      );
    }
    return response;
  }

  public List<TopSkillGapResponse> applyPrivacyGuardrailsToTopSkillGaps(List<TopSkillGapResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<TopSkillGapResponse> safeList = new ArrayList<>(items.size());
    for (TopSkillGapResponse r : items) {
      safeList.add(applyPrivacyGuardrail(r));
    }
    return safeList;
  }

  public List<SkillGapSeverityBreakdownResponse> applyPrivacyGuardrailsToSkillGapSeverity(
      List<SkillGapSeverityBreakdownResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<SkillGapSeverityBreakdownResponse> step1 = new ArrayList<>(items.size());
    int primarySuppressed = 0;
    for (SkillGapSeverityBreakdownResponse item : items) {
      if (isSparse(item.traineeCount())) {
        step1.add(new SkillGapSeverityBreakdownResponse(
            item.severityCode(), item.severityName(),
            null, null, null, null, null, null
        ));
        primarySuppressed++;
      } else {
        step1.add(item);
      }
    }

    if (primarySuppressed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        SkillGapSeverityBreakdownResponse it = step1.get(i);
        if (it.traineeCount() != null && it.traineeCount() > 0) {
          if (minVal == null || it.traineeCount() < minVal) {
            minVal = it.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        SkillGapSeverityBreakdownResponse t = step1.get(minIdx);
        step1.set(minIdx, new SkillGapSeverityBreakdownResponse(
            t.severityCode(), t.severityName(),
            null, null, null, null, null, null
        ));
      }
    }
    return step1;
  }

  public SkillGapDashboardResponse applyPrivacyGuardrail(SkillGapDashboardResponse response) {
    if (response == null) {
      return null;
    }
    List<TopSkillGapResponse> safeTopGaps = applyPrivacyGuardrailsToTopSkillGaps(response.topSkillGaps());
    List<SkillGapSeverityBreakdownResponse> safeSeverity = applyPrivacyGuardrailsToSkillGapSeverity(response.severityBreakdown());

    if (isSparse(response.totalAffectedTrainees())) {
      return new SkillGapDashboardResponse(
          null, null, null, null, null,
          safeTopGaps,
          safeSeverity
      );
    }

    boolean sAcc = isSparse(response.totalAcceptedRecommendations());
    Long totalAccepted = sAcc ? null : response.totalAcceptedRecommendations();
    BigDecimal acceptanceRate = sAcc ? null : response.overallAcceptanceRatePct();

    return new SkillGapDashboardResponse(
        response.totalCurrentGaps(),
        response.totalAffectedTrainees(),
        response.totalRecommendations(),
        totalAccepted,
        acceptanceRate,
        safeTopGaps,
        safeSeverity
    );
  }

  // =========================================================================
  // 7. Verification, Follow-up & Survey Dashboards
  // =========================================================================

  public List<VerificationStatusBreakdownResponse> applyPrivacyGuardrailsToVerificationStatuses(
      List<VerificationStatusBreakdownResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<VerificationStatusBreakdownResponse> step1 = new ArrayList<>(items.size());
    int primarySuppressed = 0;
    for (VerificationStatusBreakdownResponse item : items) {
      if (isSparse(item.traineeCount())) {
        step1.add(new VerificationStatusBreakdownResponse(
            item.statusCode(), item.statusName(), item.isVerified(),
            null, null, null, null
        ));
        primarySuppressed++;
      } else {
        step1.add(item);
      }
    }

    if (primarySuppressed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        VerificationStatusBreakdownResponse it = step1.get(i);
        if (it.traineeCount() != null && it.traineeCount() > 0) {
          if (minVal == null || it.traineeCount() < minVal) {
            minVal = it.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        VerificationStatusBreakdownResponse t = step1.get(minIdx);
        step1.set(minIdx, new VerificationStatusBreakdownResponse(
            t.statusCode(), t.statusName(), t.isVerified(),
            null, null, null, null
        ));
      }
    }
    return step1;
  }

  public List<VerificationMethodEfficacyResponse> applyPrivacyGuardrailsToVerificationMethods(
      List<VerificationMethodEfficacyResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<VerificationMethodEfficacyResponse> step1 = new ArrayList<>(items.size());
    int primarySuppressed = 0;
    for (VerificationMethodEfficacyResponse item : items) {
      if (isSparse(item.traineeCount())) {
        step1.add(new VerificationMethodEfficacyResponse(
            item.methodCode(), item.methodName(), item.isOfficial(), item.isTraineeSelfReported(),
            null, null, null, null, null, null, null
        ));
        primarySuppressed++;
      } else {
        step1.add(item);
      }
    }

    if (primarySuppressed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        VerificationMethodEfficacyResponse it = step1.get(i);
        if (it.traineeCount() != null && it.traineeCount() > 0) {
          if (minVal == null || it.traineeCount() < minVal) {
            minVal = it.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        VerificationMethodEfficacyResponse t = step1.get(minIdx);
        step1.set(minIdx, new VerificationMethodEfficacyResponse(
            t.methodCode(), t.methodName(), t.isOfficial(), t.isTraineeSelfReported(),
            null, null, null, null, null, null, null
        ));
      }
    }
    return step1;
  }

  public VerificationDashboardResponse applyPrivacyGuardrail(VerificationDashboardResponse response) {
    if (response == null) {
      return null;
    }
    List<VerificationStatusBreakdownResponse> safeStatuses =
        applyPrivacyGuardrailsToVerificationStatuses(response.statusBreakdown());
    List<VerificationMethodEfficacyResponse> safeMethods =
        applyPrivacyGuardrailsToVerificationMethods(response.methodEfficacyBreakdown());

    if (isSparse(response.totalTrainees())) {
      return new VerificationDashboardResponse(
          null, null, null, null, null, null, null, null, null,
          null, null, null, null, null,
          safeStatuses,
          safeMethods
      );
    }

    boolean sVer = isSparse(response.verifiedCount());
    boolean sOff = isSparse(response.officialVerifiedCount());
    boolean sSelf = isSparse(response.selfReportedCount());
    boolean sPend = isSparse(response.pendingCount());
    boolean sRej = isSparse(response.rejectedCount());
    boolean sUnv = isSparse(response.unverifiedCount());

    Long verifiedCount = sVer ? null : response.verifiedCount();
    Long officialVerifiedCount = sOff ? null : response.officialVerifiedCount();
    Long selfReportedCount = sSelf ? null : response.selfReportedCount();
    Long pendingCount = sPend ? null : response.pendingCount();
    Long rejectedCount = sRej ? null : response.rejectedCount();
    Long unverifiedCount = sUnv ? null : response.unverifiedCount();

    BigDecimal verificationSuccessRatePct = sVer ? null : response.verificationSuccessRatePct();
    BigDecimal officialVerificationRatePct = sOff ? null : response.officialVerificationRatePct();
    BigDecimal selfReportedRatePct = sSelf ? null : response.selfReportedRatePct();
    BigDecimal pendingRatePct = sPend ? null : response.pendingRatePct();
    BigDecimal verificationCompletionRatePct = (sVer || sPend) ? null : response.verificationCompletionRatePct();

    return new VerificationDashboardResponse(
        response.totalVerifications(),
        response.totalEmployments(),
        response.totalTrainees(),
        verifiedCount,
        officialVerifiedCount,
        selfReportedCount,
        pendingCount,
        rejectedCount,
        unverifiedCount,
        verificationCompletionRatePct,
        verificationSuccessRatePct,
        officialVerificationRatePct,
        selfReportedRatePct,
        pendingRatePct,
        safeStatuses,
        safeMethods
    );
  }

  public List<FollowupWavePerformanceResponse> applyPrivacyGuardrailsToFollowupWaves(
      List<FollowupWavePerformanceResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<FollowupWavePerformanceResponse> step1 = new ArrayList<>(items.size());
    int primarySuppressed = 0;
    for (FollowupWavePerformanceResponse item : items) {
      if (isSparse(item.traineeCount())) {
        step1.add(new FollowupWavePerformanceResponse(
            item.typeCode(), item.typeName(), item.offsetMonths(),
            null, null, null, null, null, null, null, null, null
        ));
        primarySuppressed++;
      } else {
        step1.add(item);
      }
    }

    if (primarySuppressed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        FollowupWavePerformanceResponse it = step1.get(i);
        if (it.traineeCount() != null && it.traineeCount() > 0) {
          if (minVal == null || it.traineeCount() < minVal) {
            minVal = it.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        FollowupWavePerformanceResponse t = step1.get(minIdx);
        step1.set(minIdx, new FollowupWavePerformanceResponse(
            t.typeCode(), t.typeName(), t.offsetMonths(),
            null, null, null, null, null, null, null, null, null
        ));
      }
    }
    return step1;
  }

  public FollowupDashboardResponse applyPrivacyGuardrail(FollowupDashboardResponse response) {
    if (response == null) {
      return null;
    }
    List<FollowupWavePerformanceResponse> safeWaves =
        applyPrivacyGuardrailsToFollowupWaves(response.wavePerformance());

    if (isSparse(response.totalTrainees())) {
      return new FollowupDashboardResponse(
          null, null, null, null, null, null, null, null, null, null,
          safeWaves,
          response.slaPerformance()
      );
    }

    boolean sSucc = isSparse(response.successfulContacts());
    boolean sNoResp = isSparse(response.noResponseCount());
    boolean sUnreach = isSparse(response.unreachableCount());
    boolean sComp = isSparse(response.completedTasks());
    boolean sPend = isSparse(response.pendingTasks());

    Long successfulContacts = sSucc ? null : response.successfulContacts();
    Long noResponseCount = sNoResp ? null : response.noResponseCount();
    Long unreachableCount = sUnreach ? null : response.unreachableCount();
    Long completedTasks = sComp ? null : response.completedTasks();
    Long pendingTasks = sPend ? null : response.pendingTasks();

    BigDecimal contactSuccessRatePct = sSucc ? null : response.contactSuccessRatePct();
    BigDecimal unreachableRatePct = sUnreach ? null : response.unreachableRatePct();
    BigDecimal taskCompletionRatePct = (sComp || sPend) ? null : response.taskCompletionRatePct();

    return new FollowupDashboardResponse(
        response.totalTasks(),
        response.totalTrainees(),
        completedTasks,
        pendingTasks,
        successfulContacts,
        noResponseCount,
        unreachableCount,
        contactSuccessRatePct,
        unreachableRatePct,
        taskCompletionRatePct,
        safeWaves,
        response.slaPerformance()
    );
  }

  public List<SurveyPurposePipelineResponse> applyPrivacyGuardrailsToSurveyPipelines(
      List<SurveyPurposePipelineResponse> items) {
    if (items == null || items.isEmpty()) {
      return items != null ? items : Collections.emptyList();
    }
    List<SurveyPurposePipelineResponse> step1 = new ArrayList<>(items.size());
    int primarySuppressed = 0;
    for (SurveyPurposePipelineResponse item : items) {
      if (isSparse(item.traineeCount())) {
        step1.add(new SurveyPurposePipelineResponse(
            item.purposeCode(), item.purposeName(), item.followupOffsetMonths(),
            null, null, null, null, null, null
        ));
        primarySuppressed++;
      } else {
        step1.add(item);
      }
    }

    if (primarySuppressed == 1) {
      int minIdx = -1;
      Long minVal = null;
      for (int i = 0; i < step1.size(); i++) {
        SurveyPurposePipelineResponse it = step1.get(i);
        if (it.traineeCount() != null && it.traineeCount() > 0) {
          if (minVal == null || it.traineeCount() < minVal) {
            minVal = it.traineeCount();
            minIdx = i;
          }
        }
      }
      if (minIdx != -1) {
        SurveyPurposePipelineResponse t = step1.get(minIdx);
        step1.set(minIdx, new SurveyPurposePipelineResponse(
            t.purposeCode(), t.purposeName(), t.followupOffsetMonths(),
            null, null, null, null, null, null
        ));
      }
    }
    return step1;
  }

  public SurveyDashboardResponse applyPrivacyGuardrail(SurveyDashboardResponse response) {
    if (response == null) {
      return null;
    }
    List<SurveyPurposePipelineResponse> safePipelines =
        applyPrivacyGuardrailsToSurveyPipelines(response.purposePipeline());

    if (isSparse(response.totalTrainees())) {
      return new SurveyDashboardResponse(
          null, null, null, null, null,
          safePipelines,
          response.satisfactionPerformance()
      );
    }

    boolean sSub = isSparse(response.submittedCount());
    boolean sPend = isSparse(response.pendingCount());

    Long submittedCount = sSub ? null : response.submittedCount();
    Long pendingCount = sPend ? null : response.pendingCount();
    BigDecimal overallSubmissionRatePct = sSub ? null : response.overallSubmissionRatePct();

    return new SurveyDashboardResponse(
        response.totalResponseRows(),
        response.totalTrainees(),
        submittedCount,
        pendingCount,
        overallSubmissionRatePct,
        safePipelines,
        response.satisfactionPerformance()
    );
  }
}
