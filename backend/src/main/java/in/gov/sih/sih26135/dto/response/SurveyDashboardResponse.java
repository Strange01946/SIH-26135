package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated dashboard response DTO for Post-Training Survey Response Pipeline and Feedback (Query Q20).
 *
 * @param totalResponseRows total survey opportunities / invitations
 * @param totalTrainees distinct trainees surveyed
 * @param submittedCount count of submitted survey responses
 * @param pendingCount count of pending / unsubmitted responses
 * @param overallSubmissionRatePct percentage of opportunities submitted
 * @param purposePipeline purpose and wave pipeline breakdown (Q20)
 * @param satisfactionPerformance satisfaction metrics status and aggregates
 */
public record SurveyDashboardResponse(
    Long totalResponseRows,
    Long totalTrainees,
    Long submittedCount,
    Long pendingCount,
    BigDecimal overallSubmissionRatePct,
    List<SurveyPurposePipelineResponse> purposePipeline,
    SurveySatisfactionResponse satisfactionPerformance
) {}
