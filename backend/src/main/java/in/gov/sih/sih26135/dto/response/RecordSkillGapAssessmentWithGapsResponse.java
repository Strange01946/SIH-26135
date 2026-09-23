package in.gov.sih.sih26135.dto.response;

import java.util.List;

public record RecordSkillGapAssessmentWithGapsResponse(
    SkillGapAssessmentResponse assessment,
    List<SkillGapResponse> skillGaps,
    List<SkillGapRecommendationResponse> recommendations
) {}
