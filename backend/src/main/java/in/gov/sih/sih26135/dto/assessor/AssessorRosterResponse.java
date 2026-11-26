package in.gov.sih.sih26135.dto.assessor;

import java.util.List;

/**
 * Composite response containing assessment metadata and candidate roster.
 */
public record AssessorRosterResponse(
    AssessorAssessmentSummaryResponse assessment,
    List<AssessorCandidateRosterItemResponse> candidates,
    boolean managedBatch,
    boolean checkedIn
) {
  public AssessorRosterResponse(AssessorAssessmentSummaryResponse assessment, List<AssessorCandidateRosterItemResponse> candidates) {
    this(assessment, candidates, false, true);
  }
}
