package in.gov.sih.sih26135.dto.response;

import java.util.List;

/**
 * Unified read-only summary capturing a candidate's complete journey across
 * consents, enrollments, placement, career progression, and skill gap remediation.
 */
public record CandidateLifecycleSummaryResponse(
    Long traineeId,
    List<TraineeConsentResponse> consents,
    List<TrainingEnrollmentResponse> enrollments,
    TraineePlacementEmploymentSummaryResponse placementEmploymentSummary,
    TraineeCareerTimelineResponse careerTimeline,
    TraineeSkillGapProfileResponse skillGapProfile
) {}
