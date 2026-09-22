package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TraineeAssessmentResponse {

  private Long id;
  private Long assessmentId;
  private String assessmentCode;
  private String assessmentName;
  private Long enrollmentId;
  private String enrollmentNumber;
  private Long traineeId;
  private String traineeRegistrationNumber;
  private String traineeFullName;
  private Integer attemptNumber;
  private Boolean appearedFlag;
  private LocalDate assessmentDate;
  private Long evaluatorUserId;
  private String evaluatorName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public TraineeAssessmentResponse() {
  }

  public TraineeAssessmentResponse(
      Long id,
      Long assessmentId,
      String assessmentCode,
      String assessmentName,
      Long enrollmentId,
      String enrollmentNumber,
      Long traineeId,
      String traineeRegistrationNumber,
      String traineeFullName,
      Integer attemptNumber,
      Boolean appearedFlag,
      LocalDate assessmentDate,
      Long evaluatorUserId,
      String evaluatorName,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.assessmentId = assessmentId;
    this.assessmentCode = assessmentCode;
    this.assessmentName = assessmentName;
    this.enrollmentId = enrollmentId;
    this.enrollmentNumber = enrollmentNumber;
    this.traineeId = traineeId;
    this.traineeRegistrationNumber = traineeRegistrationNumber;
    this.traineeFullName = traineeFullName;
    this.attemptNumber = attemptNumber;
    this.appearedFlag = appearedFlag;
    this.assessmentDate = assessmentDate;
    this.evaluatorUserId = evaluatorUserId;
    this.evaluatorName = evaluatorName;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(Long assessmentId) {
    this.assessmentId = assessmentId;
  }

  public String getAssessmentCode() {
    return assessmentCode;
  }

  public void setAssessmentCode(String assessmentCode) {
    this.assessmentCode = assessmentCode;
  }

  public String getAssessmentName() {
    return assessmentName;
  }

  public void setAssessmentName(String assessmentName) {
    this.assessmentName = assessmentName;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public String getEnrollmentNumber() {
    return enrollmentNumber;
  }

  public void setEnrollmentNumber(String enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public String getTraineeRegistrationNumber() {
    return traineeRegistrationNumber;
  }

  public void setTraineeRegistrationNumber(String traineeRegistrationNumber) {
    this.traineeRegistrationNumber = traineeRegistrationNumber;
  }

  public String getTraineeFullName() {
    return traineeFullName;
  }

  public void setTraineeFullName(String traineeFullName) {
    this.traineeFullName = traineeFullName;
  }

  public Integer getAttemptNumber() {
    return attemptNumber;
  }

  public void setAttemptNumber(Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
