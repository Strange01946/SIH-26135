package in.gov.sih.sih26135.dto.assessor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Score entry payload for an individual candidate in the scoring grid.
 */
public class CandidateScoreInput {

  @NotNull(message = "Trainee ID is required")
  @Positive(message = "Trainee ID must be positive")
  private Long traineeId;

  @NotNull(message = "Enrollment ID is required")
  @Positive(message = "Enrollment ID must be positive")
  private Long enrollmentId;

  private Long traineeAssessmentId;

  private Boolean appeared = true;

  private BigDecimal theoryScore;

  private BigDecimal practicalScore;

  private BigDecimal vivaScore;

  private String remarks;

  public CandidateScoreInput() {
  }

  public CandidateScoreInput(
      Long traineeId,
      Long enrollmentId,
      Long traineeAssessmentId,
      Boolean appeared,
      BigDecimal theoryScore,
      BigDecimal practicalScore,
      BigDecimal vivaScore,
      String remarks) {
    this.traineeId = traineeId;
    this.enrollmentId = enrollmentId;
    this.traineeAssessmentId = traineeAssessmentId;
    this.appeared = appeared != null ? appeared : true;
    this.theoryScore = theoryScore;
    this.practicalScore = practicalScore;
    this.vivaScore = vivaScore;
    this.remarks = remarks;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getTraineeAssessmentId() {
    return traineeAssessmentId;
  }

  public void setTraineeAssessmentId(Long traineeAssessmentId) {
    this.traineeAssessmentId = traineeAssessmentId;
  }

  public Boolean getAppeared() {
    return appeared;
  }

  public void setAppeared(Boolean appeared) {
    this.appeared = appeared;
  }

  public BigDecimal getTheoryScore() {
    return theoryScore;
  }

  public void setTheoryScore(BigDecimal theoryScore) {
    this.theoryScore = theoryScore;
  }

  public BigDecimal getPracticalScore() {
    return practicalScore;
  }

  public void setPracticalScore(BigDecimal practicalScore) {
    this.practicalScore = practicalScore;
  }

  public BigDecimal getVivaScore() {
    return vivaScore;
  }

  public void setVivaScore(BigDecimal vivaScore) {
    this.vivaScore = vivaScore;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
