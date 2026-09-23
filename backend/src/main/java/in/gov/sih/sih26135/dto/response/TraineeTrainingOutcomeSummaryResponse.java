package in.gov.sih.sih26135.dto.response;

import java.util.List;

public record TraineeTrainingOutcomeSummaryResponse(
    TrainingEnrollmentResponse enrollment,
    List<TraineeAssessmentResponse> assessments,
    List<AssessmentResultResponse> results,
    List<CertificationResponse> certifications
) {}
