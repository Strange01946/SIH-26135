package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillGapDashboardResponse;
import in.gov.sih.sih26135.dto.response.SkillGapSeverityBreakdownResponse;
import in.gov.sih.sih26135.dto.response.TopSkillGapResponse;
import in.gov.sih.sih26135.repository.analytics.SkillGapDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.SkillGapSeverityMixProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SkillGapSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.TopSkillGapProjection;
import in.gov.sih.sih26135.service.SkillGapDashboardService;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link SkillGapDashboardService}.
 * Coordinates repository executions of formal queries Q13 and Q14,
 * computing severity shares, recommendation acceptance rates, and overall uptake
 * with mathematical precision via {@link AnalyticsCalculationUtils}.
 */
@Service
@Transactional(readOnly = true)
public class SkillGapDashboardServiceImpl implements SkillGapDashboardService {

  private final SkillGapDashboardRepository repository;

  public SkillGapDashboardServiceImpl(SkillGapDashboardRepository repository) {
    this.repository = repository;
  }

  @Override
  public SkillGapDashboardResponse getSkillGapDashboard() {
    List<TopSkillGapProjection> topGapsProjections = repository.getFormalQ13TopSkillGaps();
    List<SkillGapSeverityMixProjection> severityProjections = repository.getFormalQ14SeverityAndUptakeMix();
    SkillGapSummaryProjection summaryProjection = repository.getFormalQ14OverallSummary();

    Long totalCurrentGaps = summaryProjection != null && summaryProjection.totalCurrentGaps() != null
        ? summaryProjection.totalCurrentGaps() : 0L;
    Long totalAffectedTrainees = summaryProjection != null && summaryProjection.totalAffectedTrainees() != null
        ? summaryProjection.totalAffectedTrainees() : 0L;
    Long totalRecommendations = summaryProjection != null && summaryProjection.totalRecommendations() != null
        ? summaryProjection.totalRecommendations() : 0L;
    Long totalAcceptedRecommendations = summaryProjection != null && summaryProjection.totalAcceptedRecommendations() != null
        ? summaryProjection.totalAcceptedRecommendations() : 0L;

    // Fallback calculation if summary projection is unavailable but severity mix items are present
    if (summaryProjection == null && severityProjections != null && !severityProjections.isEmpty()) {
      long currentGaps = 0L;
      long trainees = 0L;
      long recs = 0L;
      long accepted = 0L;
      for (SkillGapSeverityMixProjection s : severityProjections) {
        if (s.currentGapCount() != null) {
          currentGaps += s.currentGapCount();
        }
        if (s.traineeCount() != null) {
          trainees += s.traineeCount();
        }
        if (s.recommendationCount() != null) {
          recs += s.recommendationCount();
        }
        if (s.acceptedRecommendationCount() != null) {
          accepted += s.acceptedRecommendationCount();
        }
      }
      totalCurrentGaps = currentGaps;
      totalAffectedTrainees = trainees;
      totalRecommendations = recs;
      totalAcceptedRecommendations = accepted;
    }

    BigDecimal overallAcceptanceRatePct = AnalyticsCalculationUtils.calculatePercentage(
        totalAcceptedRecommendations, totalRecommendations
    );

    List<TopSkillGapResponse> topGaps = new ArrayList<>();
    if (topGapsProjections != null) {
      for (TopSkillGapProjection p : topGapsProjections) {
        topGaps.add(new TopSkillGapResponse(
            p.skillId(),
            p.skillCode(),
            p.skillName(),
            p.skillGapSeverityId(),
            p.severityCode(),
            p.gapCount(),
            p.traineeCount(),
            p.currentGapCount(),
            p.avgGapLevelDelta(),
            null, // sectorId
            null, // sectorName
            null, // jobRoleId
            null  // jobRoleName
        ));
      }
    }

    List<SkillGapSeverityBreakdownResponse> severityBreakdown = new ArrayList<>();
    if (severityProjections != null) {
      for (SkillGapSeverityMixProjection s : severityProjections) {
        BigDecimal sharePct = AnalyticsCalculationUtils.calculatePercentage(
            s.currentGapCount(), totalCurrentGaps
        );
        BigDecimal acceptanceRatePct = AnalyticsCalculationUtils.calculatePercentage(
            s.acceptedRecommendationCount(), s.recommendationCount()
        );
        severityBreakdown.add(new SkillGapSeverityBreakdownResponse(
            s.severityCode(),
            s.severityName(),
            s.currentGapCount(),
            s.traineeCount(),
            s.recommendationCount(),
            s.acceptedRecommendationCount(),
            sharePct,
            acceptanceRatePct
        ));
      }
    }

    return new SkillGapDashboardResponse(
        totalCurrentGaps,
        totalAffectedTrainees,
        totalRecommendations,
        totalAcceptedRecommendations,
        overallAcceptanceRatePct,
        topGaps,
        severityBreakdown
    );
  }
}
