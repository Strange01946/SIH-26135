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

@Entity
@Table(name = "training_enrollments")
public class TrainingEnrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "enrollment_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "enrollment_number", length = 32, nullable = false, unique = true)
  private String enrollmentNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", nullable = false)
  private TrainingProvider trainingProvider;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "center_id", nullable = false)
  private TrainingCenter trainingCenter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private TrainingBatch trainingBatch;

  @Column(name = "enrollment_date", nullable = false)
  private LocalDate enrollmentDate;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "expected_completion_date")
  private LocalDate expectedCompletionDate;

  @Column(name = "actual_completion_date")
  private LocalDate actualCompletionDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_status_id", nullable = false)
  private RefEnrollmentStatus enrollmentStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dropout_reason_id")
  private RefTrainingDropoutReason dropoutReason;

  @Column(name = "dropout_remarks", length = 500)
  private String dropoutRemarks;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public TrainingEnrollment() {
  }

  public TrainingEnrollment(String enrollmentNumber, Trainee trainee, Program program, Course course,
      TrainingProvider trainingProvider, TrainingCenter trainingCenter, TrainingBatch trainingBatch,
      LocalDate enrollmentDate, RefEnrollmentStatus enrollmentStatus) {
    this.enrollmentNumber = enrollmentNumber;
    this.trainee = trainee;
    this.program = program;
    this.course = course;
    this.trainingProvider = trainingProvider;
    this.trainingCenter = trainingCenter;
    this.trainingBatch = trainingBatch;
    this.enrollmentDate = enrollmentDate;
    this.enrollmentStatus = enrollmentStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEnrollmentNumber() {
    return enrollmentNumber;
  }

  public void setEnrollmentNumber(String enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
  }

  public TrainingProvider getTrainingProvider() {
    return trainingProvider;
  }

  public void setTrainingProvider(TrainingProvider trainingProvider) {
    this.trainingProvider = trainingProvider;
  }

  public TrainingCenter getTrainingCenter() {
    return trainingCenter;
  }

  public void setTrainingCenter(TrainingCenter trainingCenter) {
    this.trainingCenter = trainingCenter;
  }

  public TrainingBatch getTrainingBatch() {
    return trainingBatch;
  }

  public void setTrainingBatch(TrainingBatch trainingBatch) {
    this.trainingBatch = trainingBatch;
  }

  public LocalDate getEnrollmentDate() {
    return enrollmentDate;
  }

  public void setEnrollmentDate(LocalDate enrollmentDate) {
    this.enrollmentDate = enrollmentDate;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getExpectedCompletionDate() {
    return expectedCompletionDate;
  }

  public void setExpectedCompletionDate(LocalDate expectedCompletionDate) {
    this.expectedCompletionDate = expectedCompletionDate;
  }

  public LocalDate getActualCompletionDate() {
    return actualCompletionDate;
  }

  public void setActualCompletionDate(LocalDate actualCompletionDate) {
    this.actualCompletionDate = actualCompletionDate;
  }

  public RefEnrollmentStatus getEnrollmentStatus() {
    return enrollmentStatus;
  }

  public void setEnrollmentStatus(RefEnrollmentStatus enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
  }

  public RefTrainingDropoutReason getDropoutReason() {
    return dropoutReason;
  }

  public void setDropoutReason(RefTrainingDropoutReason dropoutReason) {
    this.dropoutReason = dropoutReason;
  }

  public String getDropoutRemarks() {
    return dropoutRemarks;
  }

  public void setDropoutRemarks(String dropoutRemarks) {
    this.dropoutRemarks = dropoutRemarks;
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
    if (!(o instanceof TrainingEnrollment other)) {
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
    return "TrainingEnrollment{" +
        "id=" + id +
        ", enrollmentNumber='" + enrollmentNumber + '\'' +
        ", enrollmentDate=" + enrollmentDate +
        '}';
  }
}
