package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateAssessmentResultRequest {

  private BigDecimal maximumScore;
  private BigDecimal obtainedScore;
  private BigDecimal scorePercentage;
  private Long assessmentOutcomeId;
  private LocalDateTime resultDeclaredAt;
  private String remarks;

  public UpdateAssessmentResultRequest() {
  }

  public UpdateAssessmentResultRequest(
      BigDecimal maximumScore,
      BigDecimal obtainedScore,
      BigDecimal scorePercentage,
      Long assessmentOutcomeId,
      LocalDateTime resultDeclaredAt,
      String remarks) {
    this.maximumScore = maximumScore;
    this.obtainedScore = obtainedScore;
    this.scorePercentage = scorePercentage;
    this.assessmentOutcomeId = assessmentOutcomeId;
    this.resultDeclaredAt = resultDeclaredAt;
    this.remarks = remarks;
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
}
