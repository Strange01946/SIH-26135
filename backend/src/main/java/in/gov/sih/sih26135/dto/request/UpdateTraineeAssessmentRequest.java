package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class UpdateTraineeAssessmentRequest {

  private Boolean appearedFlag;
  private LocalDate assessmentDate;
  private Long evaluatorUserId;
  private String evaluatorName;

  public UpdateTraineeAssessmentRequest() {
  }

  public UpdateTraineeAssessmentRequest(
      Boolean appearedFlag,
      LocalDate assessmentDate,
      Long evaluatorUserId,
      String evaluatorName) {
    this.appearedFlag = appearedFlag;
    this.assessmentDate = assessmentDate;
    this.evaluatorUserId = evaluatorUserId;
    this.evaluatorName = evaluatorName;
  }

  public Boolean getAppearedFlag() {
    return appearedFlag;
  }

  public void setAppearedFlag(Boolean appearedFlag) {
    this.appearedFlag = appearedFlag;
  }

  public LocalDate getAssessmentDate() {
    return assessmentDate;
  }

  public void setAssessmentDate(LocalDate assessmentDate) {
    this.assessmentDate = assessmentDate;
  }

  public Long getEvaluatorUserId() {
    return evaluatorUserId;
  }

  public void setEvaluatorUserId(Long evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
  }

  public String getEvaluatorName() {
    return evaluatorName;
  }

  public void setEvaluatorName(String evaluatorName) {
    this.evaluatorName = evaluatorName;
  }
}
