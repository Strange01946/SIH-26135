package in.gov.sih.sih26135.dto.response;

public record RecordAssessmentAndIssueCertificateResponse(
    AssessmentResultResponse assessmentResult,
    CertificationResponse certification,
    TrainingEnrollmentResponse updatedEnrollment
) {}
