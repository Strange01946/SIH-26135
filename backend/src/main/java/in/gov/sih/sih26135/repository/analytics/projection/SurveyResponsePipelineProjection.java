package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Projection for formal Query Q20 post-training survey response pipeline by purpose and wave.
 *
 * @param surveyPurposeCode survey purpose code (EMPLOYMENT_OUTCOME, RETENTION_3M, etc.)
 * @param purposeName human-readable survey purpose name
 * @param followupOffsetMonths offset in months from placement/completion
 * @param responseRowCount total survey invitations / response records
 * @param traineeCount distinct trainees surveyed
 * @param submittedCount count of submitted survey responses
 * @param submissionRatePct percentage of responses submitted
 */
public record SurveyResponsePipelineProjection(
    String surveyPurposeCode,
    String purposeName,
    Integer followupOffsetMonths,
    Long responseRowCount,
    Long traineeCount,
    Long submittedCount,
    BigDecimal submissionRatePct
) {}
