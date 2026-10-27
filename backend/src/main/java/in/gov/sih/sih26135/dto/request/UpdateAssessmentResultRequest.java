package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import in.gov.sih.sih26135.validation.NumberRange;

@NumberRange(minField = "obtainedScore", maxField = "maximumScore", targetField = "obtainedScore")
public class UpdateAssessmentResultRequest {

  @PositiveOrZero(message = "maximumScore must be greater than or equal to 0")
  @Digits(integer = 6, fraction = 2, message = "maximumScore numeric value out of bounds (<6 digits>.<2 digits> expected)")
  private BigDecimal maximumScore;
  @PositiveOrZero(message = "obtainedScore must be greater than or equal to 0")
  @Digits(integer = 6, fraction = 2, message = "obtainedScore numeric value out of bounds (<6 digits>.<2 digits> expected)")
  private BigDecimal obtainedScore;
  @PositiveOrZero(message = "scorePercentage must be greater than or equal to 0")
  @Digits(integer = 3, fraction = 2, message = "scorePercentage numeric value out of bounds (<3 digits>.<2 digits> expected)")
  private BigDecimal scorePercentage;
  private Long assessmentOutcomeId;
  private LocalDateTime resultDeclaredAt;
  @Size(max = 500, message = "remarks cannot exceed 500 characters")
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
