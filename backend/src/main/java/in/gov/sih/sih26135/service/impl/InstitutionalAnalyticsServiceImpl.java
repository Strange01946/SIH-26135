package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.repository.analytics.InstitutionalAnalyticsRepository;
import in.gov.sih.sih26135.repository.analytics.projection.CourseEmployabilityProjection;
import in.gov.sih.sih26135.repository.analytics.projection.ProviderPerformanceProjection;
import in.gov.sih.sih26135.service.InstitutionalAnalyticsService;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link InstitutionalAnalyticsService}.
 * Executes formal queries Q04 and Q05 via {@link InstitutionalAnalyticsRepository},
 * applies deterministic ranking, computes derived curriculum metrics via {@link AnalyticsCalculationUtils},
 * and constructs standardized, immutable dashboard response models.
 */
@Service
@Transactional(readOnly = true)
public class InstitutionalAnalyticsServiceImpl implements InstitutionalAnalyticsService {

  private static final String RANKING_METRIC = "employment_rate_pct DESC, enrollment_count DESC";

  private final InstitutionalAnalyticsRepository institutionalAnalyticsRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;

  public InstitutionalAnalyticsServiceImpl(
      InstitutionalAnalyticsRepository institutionalAnalyticsRepository,
      WorkflowSecurityScopeValidator scopeValidator) {
    this.institutionalAnalyticsRepository = java.util.Objects.requireNonNull(institutionalAnalyticsRepository, "InstitutionalAnalyticsRepository is required");
    this.scopeValidator = java.util.Objects.requireNonNull(scopeValidator, "WorkflowSecurityScopeValidator is required");
  }

  @Override
  public LeaderboardResponse<ProviderPerformanceResponse> getProviderLeaderboard() {
    scopeValidator.validateNationwideScope();
    List<ProviderPerformanceProjection> projections =
        institutionalAnalyticsRepository.getFormalQ04ProviderLeaderboard();

    if (projections == null || projections.isEmpty()) {
      return LeaderboardResponse.of(RANKING_METRIC, Collections.emptyList());
    }

    List<ProviderPerformanceResponse> items = new ArrayList<>(projections.size());
    for (int i = 0; i < projections.size(); i++) {
      ProviderPerformanceProjection p = projections.get(i);
      items.add(new ProviderPerformanceResponse(
          p.providerId(),
          p.providerCode(),
          p.providerName(),
          toLong(p.enrollmentCount()),
          toLong(p.traineeCount()),
          toLong(p.completedCount()),
          toLong(p.certifiedCount()),
          toLong(p.joinedPlacementCount()),
          toLong(p.employedCount()),
          p.completionRatePct(),
          p.certificationRatePct(),
          p.placementRatePct(),
          p.employmentRatePct(),
          toLong(p.activeBatchCapacity()),
          i + 1
      ));
    }

    return LeaderboardResponse.of(RANKING_METRIC, items);
  }

  @Override
  public LeaderboardResponse<CourseEmployabilityResponse> getCourseLeaderboard() {
    scopeValidator.validateNationwideScope();
    List<CourseEmployabilityProjection> projections =
        institutionalAnalyticsRepository.getFormalQ05CourseEmployability();

    if (projections == null || projections.isEmpty()) {
      return LeaderboardResponse.of(RANKING_METRIC, Collections.emptyList());
    }

    List<CourseEmployabilityResponse> items = new ArrayList<>(projections.size());
    for (int i = 0; i < projections.size(); i++) {
      CourseEmployabilityProjection c = projections.get(i);
      Long rawEnrollmentCount = toNullableLong(c.enrollmentCount());
      Long rawCompletedCount = toNullableLong(c.completedCount());

      // Safe calculation of dropout rate:
      // 1. If enrollment is positive and completed count is available: max(0, enrollment - completed) / enrollment * 100
      // 2. If enrollment is positive and completed count is null but completionRatePct is available: 100.00 - completionRatePct
      // 3. Otherwise (zero/null enrollment, or both completedCount and completionRatePct unavailable): null
      BigDecimal dropoutRatePct = null;
      if (rawEnrollmentCount != null && rawEnrollmentCount > 0L) {
        if (rawCompletedCount != null) {
          long dropoutCount = Math.max(0L, rawEnrollmentCount - rawCompletedCount);
          dropoutRatePct = AnalyticsCalculationUtils.calculatePercentage(dropoutCount, rawEnrollmentCount);
        } else if (c.completionRatePct() != null) {
          dropoutRatePct = AnalyticsCalculationUtils.HUNDRED.subtract(c.completionRatePct())
              .setScale(AnalyticsCalculationUtils.DEFAULT_PERCENTAGE_SCALE, AnalyticsCalculationUtils.DEFAULT_ROUNDING_MODE);
        }
      }

      items.add(new CourseEmployabilityResponse(
          c.courseId(),
          c.courseCode(),
          c.courseName(),
          c.qualificationLevelId(),
          c.qualificationLevelCode(),
          c.qualificationLevelName(),
          c.nsqfLevel(),
          toLong(c.enrollmentCount()),
          toLong(c.traineeCount()),
          rawCompletedCount,
          toLong(c.certifiedCount()),
          toLong(c.joinedPlacementCount()),
          toLong(c.employedCount()),
          c.completionRatePct(),
          c.certificationRatePct(),
          c.placementRatePct(),
          c.employmentRatePct(),
          dropoutRatePct,
          c.avgStartingWage(),
          i + 1
      ));
    }

    return LeaderboardResponse.of(RANKING_METRIC, items);
  }

  @Override
  public Map<String, List<CourseEmployabilityResponse>> getCourseLeaderboardByQualificationLevel() {
    List<CourseEmployabilityResponse> courses = getCourseLeaderboard().items();
    if (courses.isEmpty()) {
      return Collections.emptyMap();
    }

    return courses.stream()
        .collect(Collectors.groupingBy(
            c -> c.qualificationLevelName() != null ? c.qualificationLevelName() : "Unspecified",
            LinkedHashMap::new,
            Collectors.toList()
        ));
  }

  private static Long toLong(Number number) {
    return number != null ? number.longValue() : 0L;
  }

  private static Long toNullableLong(Number number) {
    return number != null ? number.longValue() : null;
  }
}
