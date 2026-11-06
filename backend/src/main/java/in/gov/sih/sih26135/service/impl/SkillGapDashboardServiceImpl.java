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
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final WorkflowSecurityScopeValidator scopeValidator;

  public SkillGapDashboardServiceImpl(
      SkillGapDashboardRepository repository,
      WorkflowSecurityScopeValidator scopeValidator) {
    this.repository = java.util.Objects.requireNonNull(repository, "SkillGapDashboardRepository is required");
    this.scopeValidator = java.util.Objects.requireNonNull(scopeValidator, "WorkflowSecurityScopeValidator is required");
  }

  @Override
  public SkillGapDashboardResponse getSkillGapDashboard() {
    scopeValidator.validateNationwideScope();
    List<TopSkillGapProjection> topGapsProjections = repository.getFormalQ13TopSkillGaps();
    List<SkillGapSeverityMixProjection> severityProjections = repository.getFormalQ14SeverityAndUptakeMix();
    SkillGapSummaryProjection summaryProjection = repository.getFormalQ14OverallSummary();

    Long totalCurrentGaps;
    Long totalAffectedTrainees;
    Long totalRecommendations;
    Long totalAcceptedRecommendations;

    if (summaryProjection != null) {
      totalCurrentGaps = summaryProjection.totalCurrentGaps() != null ? summaryProjection.totalCurrentGaps() : 0L;
      totalAffectedTrainees = summaryProjection.totalAffectedTrainees() != null ? summaryProjection.totalAffectedTrainees() : 0L;
      totalRecommendations = summaryProjection.totalRecommendations() != null ? summaryProjection.totalRecommendations() : 0L;
      totalAcceptedRecommendations = summaryProjection.totalAcceptedRecommendations() != null ? summaryProjection.totalAcceptedRecommendations() : 0L;
    } else {
      totalAffectedTrainees = null; // Missing aggregate projection does not establish nationwide distinct trainees
      if (severityProjections != null && !severityProjections.isEmpty()) {
        // Severity categories form a mutually exclusive partition for gap counts and recommendation counts.
        // However, nationwide distinct trainees CANNOT be summed across severity levels due to potential trainee overlap.
        long currentGaps = 0L;
        long recs = 0L;
        long accepted = 0L;
        for (SkillGapSeverityMixProjection s : severityProjections) {
          if (s.currentGapCount() != null) {
            currentGaps += s.currentGapCount();
          }
          if (s.recommendationCount() != null) {
            recs += s.recommendationCount();
          }
          if (s.acceptedRecommendationCount() != null) {
            accepted += s.acceptedRecommendationCount();
          }
        }
        totalCurrentGaps = currentGaps;
        totalRecommendations = recs;
        totalAcceptedRecommendations = accepted;
      } else {
        totalCurrentGaps = 0L;
        totalRecommendations = 0L;
        totalAcceptedRecommendations = 0L;
      }
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
            p.avgGapLevelDelta()
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
