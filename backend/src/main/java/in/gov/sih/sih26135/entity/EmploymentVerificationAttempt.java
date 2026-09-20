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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "employment_verification_attempts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_emp_verif_attempts_number", columnNames = {
        "employment_verification_request_id", "attempt_number"
    })
})
public class EmploymentVerificationAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_verification_attempt_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_request_id", nullable = false)
  private EmploymentVerificationRequest employmentVerificationRequest;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "attempt_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer attemptNumber = 1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_method_id", nullable = false)
  private RefEmploymentVerificationMethod employmentVerificationMethod;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_attempt_status_id", nullable = false)
  private RefEmploymentVerificationAttemptStatus employmentVerificationAttemptStatus;

  @Column(name = "attempted_by_user_id")
  private Long attemptedByUserId;

  @Column(name = "employer_respondent_user_id")
  private Long employerRespondentUserId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "communication_log_id")
  private CommunicationLog communicationLog;

  @Column(name = "attempted_at", nullable = false)
  private LocalDateTime attemptedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public EmploymentVerificationAttempt() {
  }

  public EmploymentVerificationAttempt(EmploymentVerificationRequest employmentVerificationRequest,
      Integer attemptNumber, RefEmploymentVerificationMethod employmentVerificationMethod,
      RefEmploymentVerificationAttemptStatus employmentVerificationAttemptStatus,
      LocalDateTime attemptedAt) {
    this.employmentVerificationRequest = employmentVerificationRequest;
    this.attemptNumber = attemptNumber;
    this.employmentVerificationMethod = employmentVerificationMethod;
    this.employmentVerificationAttemptStatus = employmentVerificationAttemptStatus;
    this.attemptedAt = attemptedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public EmploymentVerificationRequest getEmploymentVerificationRequest() {
    return employmentVerificationRequest;
  }

  public void setEmploymentVerificationRequest(
      EmploymentVerificationRequest employmentVerificationRequest) {
    this.employmentVerificationRequest = employmentVerificationRequest;
  }

  public Integer getAttemptNumber() {
    return attemptNumber;
  }

  public void setAttemptNumber(Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
  }

  public RefEmploymentVerificationMethod getEmploymentVerificationMethod() {
    return employmentVerificationMethod;
  }

  public void setEmploymentVerificationMethod(
      RefEmploymentVerificationMethod employmentVerificationMethod) {
    this.employmentVerificationMethod = employmentVerificationMethod;
  }

  public RefEmploymentVerificationAttemptStatus getEmploymentVerificationAttemptStatus() {
    return employmentVerificationAttemptStatus;
  }

  public void setEmploymentVerificationAttemptStatus(
      RefEmploymentVerificationAttemptStatus employmentVerificationAttemptStatus) {
    this.employmentVerificationAttemptStatus = employmentVerificationAttemptStatus;
  }

  public Long getAttemptedByUserId() {
    return attemptedByUserId;
  }

  public void setAttemptedByUserId(Long attemptedByUserId) {
    this.attemptedByUserId = attemptedByUserId;
  }

  public Long getEmployerRespondentUserId() {
    return employerRespondentUserId;
  }

  public void setEmployerRespondentUserId(Long employerRespondentUserId) {
    this.employerRespondentUserId = employerRespondentUserId;
  }

  public CommunicationLog getCommunicationLog() {
    return communicationLog;
  }

  public void setCommunicationLog(CommunicationLog communicationLog) {
    this.communicationLog = communicationLog;
  }

  public LocalDateTime getAttemptedAt() {
    return attemptedAt;
  }

  public void setAttemptedAt(LocalDateTime attemptedAt) {
    this.attemptedAt = attemptedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
    if (!(o instanceof EmploymentVerificationAttempt that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "EmploymentVerificationAttempt{" +
        "id=" + id +
        ", attemptNumber=" + attemptNumber +
        ", attemptedAt=" + attemptedAt +
        ", completedAt=" + completedAt +
        '}';
  }
}
