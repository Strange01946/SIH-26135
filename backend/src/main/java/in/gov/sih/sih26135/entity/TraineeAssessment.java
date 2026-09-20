package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "trainee_assessments")
public class TraineeAssessment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "trainee_assessment_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", nullable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private TrainingEnrollment trainingEnrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "attempt_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer attemptNumber = 1;

  @Column(name = "appeared_flag", nullable = false)
  private Boolean appearedFlag = true;

  @Column(name = "assessment_date", nullable = false)
  private LocalDate assessmentDate;

  @Column(name = "evaluator_user_id")
  private Long evaluatorUserId;

  @Column(name = "evaluator_name", length = 150)
  private String evaluatorName;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public TraineeAssessment() {
  }

  public TraineeAssessment(Assessment assessment, TrainingEnrollment trainingEnrollment, Trainee trainee,
      Integer attemptNumber, Boolean appearedFlag, LocalDate assessmentDate) {
    this.assessment = assessment;
    this.trainingEnrollment = trainingEnrollment;
    this.trainee = trainee;
    this.attemptNumber = attemptNumber != null ? attemptNumber : 1;
    this.appearedFlag = appearedFlag != null ? appearedFlag : true;
    this.assessmentDate = assessmentDate;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Assessment getAssessment() {
    return assessment;
  }

  public void setAssessment(Assessment assessment) {
    this.assessment = assessment;
  }

  public TrainingEnrollment getTrainingEnrollment() {
    return trainingEnrollment;
  }

  public void setTrainingEnrollment(TrainingEnrollment trainingEnrollment) {
    this.trainingEnrollment = trainingEnrollment;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
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
    if (!(o instanceof TraineeAssessment other)) {
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
    return "TraineeAssessment{" +
        "id=" + id +
        ", attemptNumber=" + attemptNumber +
        ", appearedFlag=" + appearedFlag +
        ", assessmentDate=" + assessmentDate +
        '}';
  }
}
