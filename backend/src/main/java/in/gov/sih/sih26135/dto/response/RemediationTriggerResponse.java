package in.gov.sih.sih26135.dto.response;

/**
 * Response payload representing the outcome of a skill-gap remediation trigger.
 */
public record RemediationTriggerResponse(
    Long traineeId,
    String triggerSource,
    Long referenceEntityId,
    String remediationStatus,
    RecordSkillGapAssessmentWithGapsResponse assessmentResponse,
    String remarks,
    String correlationId
) {}
