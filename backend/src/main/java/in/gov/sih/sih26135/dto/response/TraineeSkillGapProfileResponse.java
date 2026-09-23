package in.gov.sih.sih26135.dto.response;

import java.util.List;

public record TraineeSkillGapProfileResponse(
    Long traineeId,
    List<SkillGapAssessmentResponse> assessments,
    List<SkillGapResponse> currentSkillGaps,
    List<SkillGapRecommendationResponse> recommendations
) {}
