package in.gov.sih.sih26135.dto.assessor;

import java.util.List;

/**
 * Composite response containing assessment metadata and candidate roster.
 */
public record AssessorRosterResponse(
    AssessorAssessmentSummaryResponse assessment,
    List<AssessorCandidateRosterItemResponse> candidates
) {}
