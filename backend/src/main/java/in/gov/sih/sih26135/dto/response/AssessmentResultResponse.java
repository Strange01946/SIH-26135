package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AssessmentResultResponse {

  private Long id;
  private Long traineeAssessmentId;
  private Long traineeId;
  private String traineeRegistrationNumber;
  private String traineeFullName;
  private BigDecimal maximumScore;
  private BigDecimal obtainedScore;
  private BigDecimal scorePercentage;
  private Long assessmentOutcomeId;
  private String assessmentOutcomeCode;
  private String assessmentOutcomeName;
  private Boolean isPassFlag;
  private LocalDateTime resultDeclaredAt;
  private String remarks;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public AssessmentResultResponse() {
  }

  public AssessmentResultResponse(
      Long id,
      Long traineeAssessmentId,
      Long traineeId,
      String traineeRegistrationNumber,
      String traineeFullName,
      BigDecimal maximumScore,
      BigDecimal obtainedScore,
      BigDecimal scorePercentage,
      Long assessmentOutcomeId,
      String assessmentOutcomeCode,
      String assessmentOutcomeName,
      Boolean isPassFlag,
      LocalDateTime resultDeclaredAt,
      String remarks,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.traineeAssessmentId = traineeAssessmentId;
    this.traineeId = traineeId;
    this.traineeRegistrationNumber = traineeRegistrationNumber;
    this.traineeFullName = traineeFullName;
    this.maximumScore = maximumScore;
    this.obtainedScore = obtainedScore;
    this.scorePercentage = scorePercentage;
    this.assessmentOutcomeId = assessmentOutcomeId;
    this.assessmentOutcomeCode = assessmentOutcomeCode;
    this.assessmentOutcomeName = assessmentOutcomeName;
    this.isPassFlag = isPassFlag;
    this.resultDeclaredAt = resultDeclaredAt;
    this.remarks = remarks;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTraineeAssessmentId() {
    return traineeAssessmentId;
  }

  public void setTraineeAssessmentId(Long traineeAssessmentId) {
    this.traineeAssessmentId = traineeAssessmentId;
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

  public BigDecimal getMaximumScore() {
    return maximumScore;
  }

  public void setMaximumScore(BigDecimal maximumScore) {
    this.maximumScore = maximumScore;
  }

  public BigDecimal getObtainedScore() {
    return obtainedScore;
  }

  public void setObtainedScore(BigDecimal obtainedScore) {
    this.obtainedScore = obtainedScore;
  }

  public BigDecimal getScorePercentage() {
    return scorePercentage;
  }

  public void setScorePercentage(BigDecimal scorePercentage) {
    this.scorePercentage = scorePercentage;
  }

  public Long getAssessmentOutcomeId() {
    return assessmentOutcomeId;
  }

  public void setAssessmentOutcomeId(Long assessmentOutcomeId) {
    this.assessmentOutcomeId = assessmentOutcomeId;
  }

  public String getAssessmentOutcomeCode() {
    return assessmentOutcomeCode;
  }

  public void setAssessmentOutcomeCode(String assessmentOutcomeCode) {
    this.assessmentOutcomeCode = assessmentOutcomeCode;
  }

  public String getAssessmentOutcomeName() {
    return assessmentOutcomeName;
  }

  public void setAssessmentOutcomeName(String assessmentOutcomeName) {
    this.assessmentOutcomeName = assessmentOutcomeName;
  }

  public Boolean getIsPassFlag() {
    return isPassFlag;
  }

  public void setIsPassFlag(Boolean passFlag) {
    isPassFlag = passFlag;
  }

  public LocalDateTime getResultDeclaredAt() {
    return resultDeclaredAt;
  }

  public void setResultDeclaredAt(LocalDateTime resultDeclaredAt) {
    this.resultDeclaredAt = resultDeclaredAt;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
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
