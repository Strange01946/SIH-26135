package in.gov.sih.sih26135.dto.response;

/**
 * Response payload representing the completed trainee enrollment orchestration stage.
 */
public record TraineeEnrollmentOrchestrationResponse(
    TrainingEnrollmentResponse enrollment,
    String consentStatus,
    String stageStatus,
    String correlationId
) {}
