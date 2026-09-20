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
@Table(name = "program_training_providers")
public class ProgramTrainingProvider {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "program_training_provider_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", nullable = false)
  private TrainingProvider trainingProvider;

  @Column(name = "empanelled_from", nullable = false)
  private LocalDate empanelledFrom;

  @Column(name = "empanelled_to")
  private LocalDate empanelledTo;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public ProgramTrainingProvider() {
  }

  public ProgramTrainingProvider(Program program, TrainingProvider trainingProvider,
      LocalDate empanelledFrom, Long lifecycleStatusId) {
    this.program = program;
    this.trainingProvider = trainingProvider;
    this.empanelledFrom = empanelledFrom;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  public TrainingProvider getTrainingProvider() {
    return trainingProvider;
  }

  public void setTrainingProvider(TrainingProvider trainingProvider) {
    this.trainingProvider = trainingProvider;
  }

  public LocalDate getEmpanelledFrom() {
    return empanelledFrom;
  }

  public void setEmpanelledFrom(LocalDate empanelledFrom) {
    this.empanelledFrom = empanelledFrom;
  }

  public LocalDate getEmpanelledTo() {
    return empanelledTo;
  }

  public void setEmpanelledTo(LocalDate empanelledTo) {
    this.empanelledTo = empanelledTo;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProgramTrainingProvider other)) {
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
    return "ProgramTrainingProvider{" +
        "id=" + id +
        ", empanelledFrom=" + empanelledFrom +
        ", empanelledTo=" + empanelledTo +
        ", lifecycleStatusId=" + lifecycleStatusId +
        '}';
  }
}
