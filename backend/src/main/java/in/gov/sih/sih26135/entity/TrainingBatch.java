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
@Table(name = "training_batches")
public class TrainingBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "batch_code", length = 32, nullable = false, unique = true)
  private String batchCode;

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
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "capacity", nullable = false)
  private Integer capacity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_status_id", nullable = false)
  private RefBatchStatus batchStatus;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public TrainingBatch() {
  }

  public TrainingBatch(String batchCode, Course course, TrainingProvider trainingProvider,
      TrainingCenter trainingCenter, Program program, LocalDate startDate, Integer capacity,
      RefBatchStatus batchStatus) {
    this.batchCode = batchCode;
    this.course = course;
    this.trainingProvider = trainingProvider;
    this.trainingCenter = trainingCenter;
    this.program = program;
    this.startDate = startDate;
    this.capacity = capacity;
    this.batchStatus = batchStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getBatchCode() {
    return batchCode;
  }

  public void setBatchCode(String batchCode) {
    this.batchCode = batchCode;
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

  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public RefBatchStatus getBatchStatus() {
    return batchStatus;
  }

  public void setBatchStatus(RefBatchStatus batchStatus) {
    this.batchStatus = batchStatus;
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
    if (!(o instanceof TrainingBatch other)) {
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
    return "TrainingBatch{" +
        "id=" + id +
        ", batchCode='" + batchCode + '\'' +
        ", startDate=" + startDate +
        ", capacity=" + capacity +
        '}';
  }
}
