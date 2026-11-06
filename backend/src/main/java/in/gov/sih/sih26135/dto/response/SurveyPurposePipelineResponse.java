package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing survey pipeline breakdown by purpose and wave (formal Query Q20).
 *
 * @param purposeCode survey purpose code (EMPLOYMENT_OUTCOME, RETENTION_3M, etc.)
 * @param purposeName human-readable survey purpose name
 * @param followupOffsetMonths offset in months from placement/completion
 * @param responseRowCount total survey invitations / response opportunities
 * @param traineeCount distinct trainees surveyed
 * @param submittedCount count of submitted survey responses
 * @param pendingCount count of pending / unsubmitted responses
 * @param submissionRatePct percentage of survey opportunities submitted
 * @param sharePct percentage of all survey response rows represented by this purpose
 */
public record SurveyPurposePipelineResponse(
    String purposeCode,
    String purposeName,
    Integer followupOffsetMonths,
    Long responseRowCount,
    Long traineeCount,
    Long submittedCount,
    Long pendingCount,
    BigDecimal submissionRatePct,
    BigDecimal sharePct
) {}
