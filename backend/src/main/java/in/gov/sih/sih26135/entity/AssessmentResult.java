package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "assessment_results")
public class AssessmentResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "assessment_result_id", nullable = false, updatable = false)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_assessment_id", nullable = false, unique = true)
  private TraineeAssessment traineeAssessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @Column(name = "maximum_score", precision = 8, scale = 2, nullable = false)
  private BigDecimal maximumScore;

  @Column(name = "obtained_score", precision = 8, scale = 2)
  private BigDecimal obtainedScore;

  @Column(name = "score_percentage", precision = 5, scale = 2)
  private BigDecimal scorePercentage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_outcome_id", nullable = false)
  private RefAssessmentOutcome assessmentOutcome;

  @Column(name = "result_declared_at")
  private LocalDateTime resultDeclaredAt;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public AssessmentResult() {
  }

  public AssessmentResult(TraineeAssessment traineeAssessment, Trainee trainee, BigDecimal maximumScore,
      BigDecimal obtainedScore, BigDecimal scorePercentage, RefAssessmentOutcome assessmentOutcome) {
    this.traineeAssessment = traineeAssessment;
    this.trainee = trainee;
    this.maximumScore = maximumScore;
    this.obtainedScore = obtainedScore;
    this.scorePercentage = scorePercentage;
    this.assessmentOutcome = assessmentOutcome;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public TraineeAssessment getTraineeAssessment() {
    return traineeAssessment;
  }

  public void setTraineeAssessment(TraineeAssessment traineeAssessment) {
    this.traineeAssessment = traineeAssessment;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
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

  public RefAssessmentOutcome getAssessmentOutcome() {
    return assessmentOutcome;
  }

  public void setAssessmentOutcome(RefAssessmentOutcome assessmentOutcome) {
    this.assessmentOutcome = assessmentOutcome;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AssessmentResult other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "AssessmentResult{" +
        "id=" + id +
        ", maximumScore=" + maximumScore +
        ", obtainedScore=" + obtainedScore +
        ", scorePercentage=" + scorePercentage +
        '}';
  }
}
