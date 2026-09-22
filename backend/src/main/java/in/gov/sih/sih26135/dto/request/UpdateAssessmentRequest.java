package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateAssessmentRequest {

  private String assessmentName;
  private Long assessmentTypeId;
  private LocalDate assessmentDate;
  private BigDecimal maximumScore;
  private BigDecimal passScore;
  private Long evaluatorUserId;
  private String evaluatorName;
  private Long lifecycleStatusId;

  public UpdateAssessmentRequest() {
  }

  public UpdateAssessmentRequest(
      String assessmentName,
      Long assessmentTypeId,
      LocalDate assessmentDate,
      BigDecimal maximumScore,
      BigDecimal passScore,
      Long evaluatorUserId,
      String evaluatorName,
      Long lifecycleStatusId) {
    this.assessmentName = assessmentName;
    this.assessmentTypeId = assessmentTypeId;
    this.assessmentDate = assessmentDate;
    this.maximumScore = maximumScore;
    this.passScore = passScore;
    this.evaluatorUserId = evaluatorUserId;
    this.evaluatorName = evaluatorName;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public String getAssessmentName() {
    return assessmentName;
  }

  public void setAssessmentName(String assessmentName) {
    this.assessmentName = assessmentName;
  }

  public Long getAssessmentTypeId() {
    return assessmentTypeId;
  }

  public void setAssessmentTypeId(Long assessmentTypeId) {
    this.assessmentTypeId = assessmentTypeId;
  }

  public LocalDate getAssessmentDate() {
    return assessmentDate;
  }

  public void setAssessmentDate(LocalDate assessmentDate) {
    this.assessmentDate = assessmentDate;
  }

  public BigDecimal getMaximumScore() {
    return maximumScore;
  }

  public void setMaximumScore(BigDecimal maximumScore) {
    this.maximumScore = maximumScore;
  }

  public BigDecimal getPassScore() {
    return passScore;
  }

  public void setPassScore(BigDecimal passScore) {
    this.passScore = passScore;
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

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
