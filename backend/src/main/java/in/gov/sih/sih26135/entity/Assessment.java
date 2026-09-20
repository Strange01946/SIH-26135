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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "assessments")
public class Assessment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "assessment_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "assessment_code", length = 32, nullable = false, unique = true)
  private String assessmentCode;

  @Column(name = "assessment_name", length = 200, nullable = false)
  private String assessmentName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_type_id", nullable = false)
  private RefAssessmentType assessmentType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private TrainingBatch trainingBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @Column(name = "assessment_date", nullable = false)
  private LocalDate assessmentDate;

  @Column(name = "maximum_score", precision = 8, scale = 2, nullable = false)
  private BigDecimal maximumScore;

  @Column(name = "pass_score", precision = 8, scale = 2)
  private BigDecimal passScore;

  @Column(name = "evaluator_user_id")
  private Long evaluatorUserId;

  @Column(name = "evaluator_name", length = 150)
  private String evaluatorName;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Assessment() {
  }

  public Assessment(String assessmentCode, String assessmentName, RefAssessmentType assessmentType, Course course,
      TrainingBatch trainingBatch, Program program, LocalDate assessmentDate, BigDecimal maximumScore, Long lifecycleStatusId) {
    this.assessmentCode = assessmentCode;
    this.assessmentName = assessmentName;
    this.assessmentType = assessmentType;
    this.course = course;
    this.trainingBatch = trainingBatch;
    this.program = program;
    this.assessmentDate = assessmentDate;
    this.maximumScore = maximumScore;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public RefAssessmentType getAssessmentType() {
    return assessmentType;
  }

  public void setAssessmentType(RefAssessmentType assessmentType) {
    this.assessmentType = assessmentType;
  }

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
  }

  public TrainingBatch getTrainingBatch() {
    return trainingBatch;
  }

  public void setTrainingBatch(TrainingBatch trainingBatch) {
    this.trainingBatch = trainingBatch;
  }

  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
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

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Assessment other)) {
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
    return "Assessment{" +
        "id=" + id +
        ", assessmentCode='" + assessmentCode + '\'' +
        ", assessmentName='" + assessmentName + '\'' +
        ", assessmentDate=" + assessmentDate +
        '}';
  }
}
