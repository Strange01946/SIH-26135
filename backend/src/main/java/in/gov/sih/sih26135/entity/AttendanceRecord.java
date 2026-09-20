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
import java.time.LocalTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attendance_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private TrainingEnrollment trainingEnrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private TrainingBatch trainingBatch;

  @Column(name = "session_date", nullable = false)
  private LocalDate sessionDate;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "session_sequence", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sessionSequence = 1;

  @Column(name = "session_start_time")
  private LocalTime sessionStartTime;

  @Column(name = "session_end_time")
  private LocalTime sessionEndTime;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attendance_status_id", nullable = false)
  private RefAttendanceStatus attendanceStatus;

  @Column(name = "marked_at", nullable = false)
  private LocalDateTime markedAt;

  @Column(name = "marked_by_user_id")
  private Long markedByUserId;

  @Column(name = "remarks", length = 255)
  private String remarks;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public AttendanceRecord() {
  }

  public AttendanceRecord(TrainingEnrollment trainingEnrollment, Trainee trainee, TrainingBatch trainingBatch,
      LocalDate sessionDate, Integer sessionSequence, RefAttendanceStatus attendanceStatus, LocalDateTime markedAt) {
    this.trainingEnrollment = trainingEnrollment;
    this.trainee = trainee;
    this.trainingBatch = trainingBatch;
    this.sessionDate = sessionDate;
    this.sessionSequence = sessionSequence != null ? sessionSequence : 1;
    this.attendanceStatus = attendanceStatus;
    this.markedAt = markedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public TrainingBatch getTrainingBatch() {
    return trainingBatch;
  }

  public void setTrainingBatch(TrainingBatch trainingBatch) {
    this.trainingBatch = trainingBatch;
  }

  public LocalDate getSessionDate() {
    return sessionDate;
  }

  public void setSessionDate(LocalDate sessionDate) {
    this.sessionDate = sessionDate;
  }

  public Integer getSessionSequence() {
    return sessionSequence;
  }

  public void setSessionSequence(Integer sessionSequence) {
    this.sessionSequence = sessionSequence != null ? sessionSequence : 1;
  }

  public LocalTime getSessionStartTime() {
    return sessionStartTime;
  }

  public void setSessionStartTime(LocalTime sessionStartTime) {
    this.sessionStartTime = sessionStartTime;
  }

  public LocalTime getSessionEndTime() {
    return sessionEndTime;
  }

  public void setSessionEndTime(LocalTime sessionEndTime) {
    this.sessionEndTime = sessionEndTime;
  }

  public RefAttendanceStatus getAttendanceStatus() {
    return attendanceStatus;
  }

  public void setAttendanceStatus(RefAttendanceStatus attendanceStatus) {
    this.attendanceStatus = attendanceStatus;
  }

  public LocalDateTime getMarkedAt() {
    return markedAt;
  }

  public void setMarkedAt(LocalDateTime markedAt) {
    this.markedAt = markedAt;
  }

  public Long getMarkedByUserId() {
    return markedByUserId;
  }

  public void setMarkedByUserId(Long markedByUserId) {
    this.markedByUserId = markedByUserId;
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
    if (!(o instanceof AttendanceRecord other)) {
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
    return "AttendanceRecord{" +
        "id=" + id +
        ", sessionDate=" + sessionDate +
        ", sessionSequence=" + sessionSequence +
        '}';
  }
}
