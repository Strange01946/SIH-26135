package in.gov.sih.sih26135.dto.assessor;

import java.util.List;

/**
 * Result of saving draft or finalizing candidate scores.
 */
public record SaveAssessmentScoresResponse(
    Long assessmentId,
    boolean isFinalized,
    int totalProcessed,
    int passedCount,
    int failedCount,
    int absentCount,
    int certificatesIssued,
    List<AssessorCandidateRosterItemResponse> results
) {}
