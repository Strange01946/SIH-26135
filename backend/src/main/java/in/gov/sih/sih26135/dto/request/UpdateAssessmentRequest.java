package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateAssessmentRequest {

  @Size(max = 200, message = "assessmentName cannot exceed 200 characters")
  private String assessmentName;
  private Long assessmentTypeId;
  private LocalDate assessmentDate;
  @PositiveOrZero(message = "maximumScore must be greater than or equal to 0")
  @Digits(integer = 6, fraction = 2, message = "maximumScore numeric value out of bounds (<6 digits>.<2 digits> expected)")
  private BigDecimal maximumScore;
  @PositiveOrZero(message = "passScore must be greater than or equal to 0")
  @Digits(integer = 6, fraction = 2, message = "passScore numeric value out of bounds (<6 digits>.<2 digits> expected)")
  private BigDecimal passScore;
  private Long evaluatorUserId;
  @Size(max = 150, message = "evaluatorName cannot exceed 150 characters")
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
