package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for triggering trainee skill-gap evaluation and remediation handoff
 * following employment separation, verification failure, or training assessment.
 */
public class RemediationTriggerRequest {

  @NotNull(message = "Trainee ID is required")
  private Long traineeId;

  @NotBlank(message = "Trigger source is required")
  @Size(max = 64, message = "Trigger source cannot exceed 64 characters")
  private String triggerSource;

  private Long referenceEntityId;

  private RecordSkillGapAssessmentWithGapsRequest assessmentRequest;

  @Size(max = 500, message = "Remarks cannot exceed 500 characters")
  private String remarks;

  public RemediationTriggerRequest() {
  }

  public RemediationTriggerRequest(Long traineeId, String triggerSource) {
    this.traineeId = traineeId;
    this.triggerSource = triggerSource;
  }

  public RemediationTriggerRequest(
      Long traineeId,
      String triggerSource,
      Long referenceEntityId,
      RecordSkillGapAssessmentWithGapsRequest assessmentRequest,
      String remarks) {
    this.traineeId = traineeId;
    this.triggerSource = triggerSource;
    this.referenceEntityId = referenceEntityId;
    this.assessmentRequest = assessmentRequest;
    this.remarks = remarks;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public String getTriggerSource() {
    return triggerSource;
  }

  public void setTriggerSource(String triggerSource) {
    this.triggerSource = triggerSource;
  }

  public Long getReferenceEntityId() {
    return referenceEntityId;
  }

  public void setReferenceEntityId(Long referenceEntityId) {
    this.referenceEntityId = referenceEntityId;
  }

  public RecordSkillGapAssessmentWithGapsRequest getAssessmentRequest() {
    return assessmentRequest;
  }

  public void setAssessmentRequest(RecordSkillGapAssessmentWithGapsRequest assessmentRequest) {
    this.assessmentRequest = assessmentRequest;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
