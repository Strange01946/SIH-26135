package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class CreateTraineeAssessmentRequest {

  private Long assessmentId;
  private Long enrollmentId;
  private Long traineeId;
  private Integer attemptNumber = 1;
  private Boolean appearedFlag = true;
  private LocalDate assessmentDate;
  private Long evaluatorUserId;
  private String evaluatorName;

  public CreateTraineeAssessmentRequest() {
  }

  public CreateTraineeAssessmentRequest(
      Long assessmentId,
      Long enrollmentId,
      Long traineeId,
      LocalDate assessmentDate) {
    this.assessmentId = assessmentId;
    this.enrollmentId = enrollmentId;
    this.traineeId = traineeId;
    this.assessmentDate = assessmentDate;
  }

  public CreateTraineeAssessmentRequest(
      Long assessmentId,
      Long enrollmentId,
      Long traineeId,
      Integer attemptNumber,
      Boolean appearedFlag,
      LocalDate assessmentDate,
      Long evaluatorUserId,
      String evaluatorName) {
    this.assessmentId = assessmentId;
    this.enrollmentId = enrollmentId;
    this.traineeId = traineeId;
    this.attemptNumber = attemptNumber != null ? attemptNumber : 1;
    this.appearedFlag = appearedFlag != null ? appearedFlag : true;
    this.assessmentDate = assessmentDate;
    this.evaluatorUserId = evaluatorUserId;
    this.evaluatorName = evaluatorName;
  }

  public Long getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(Long assessmentId) {
    this.assessmentId = assessmentId;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Integer getAttemptNumber() {
    return attemptNumber;
  }

  public void setAttemptNumber(Integer attemptNumber) {
    this.attemptNumber = attemptNumber != null ? attemptNumber : 1;
  }

  public Boolean getAppearedFlag() {
    return appearedFlag;
  }

  public void setAppearedFlag(Boolean appearedFlag) {
    this.appearedFlag = appearedFlag != null ? appearedFlag : true;
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
