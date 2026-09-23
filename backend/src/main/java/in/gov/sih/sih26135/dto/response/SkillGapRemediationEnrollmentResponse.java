package in.gov.sih.sih26135.dto.response;

public record SkillGapRemediationEnrollmentResponse(
    SkillGapRecommendationResponse recommendation,
    TrainingEnrollmentResponse createdEnrollment
) {}
