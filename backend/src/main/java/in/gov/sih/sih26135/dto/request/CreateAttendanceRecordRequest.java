package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CreateAttendanceRecordRequest {

  private Long enrollmentId;
  private Long traineeId;
  private Long batchId;
  private LocalDate sessionDate;
  private Integer sessionSequence = 1;
  private LocalTime sessionStartTime;
  private LocalTime sessionEndTime;
  private Long attendanceStatusId;
  private LocalDateTime markedAt;
  private Long markedByUserId;
  private String remarks;

  public CreateAttendanceRecordRequest() {
  }

  public CreateAttendanceRecordRequest(
      Long enrollmentId,
      LocalDate sessionDate,
      Long attendanceStatusId) {
    this.enrollmentId = enrollmentId;
    this.sessionDate = sessionDate;
    this.attendanceStatusId = attendanceStatusId;
  }

  public CreateAttendanceRecordRequest(
      Long enrollmentId,
      Long traineeId,
      Long batchId,
      LocalDate sessionDate,
      Integer sessionSequence,
      LocalTime sessionStartTime,
      LocalTime sessionEndTime,
      Long attendanceStatusId,
      LocalDateTime markedAt,
      Long markedByUserId,
      String remarks) {
    this.enrollmentId = enrollmentId;
    this.traineeId = traineeId;
    this.batchId = batchId;
    this.sessionDate = sessionDate;
    this.sessionSequence = sessionSequence != null ? sessionSequence : 1;
    this.sessionStartTime = sessionStartTime;
    this.sessionEndTime = sessionEndTime;
    this.attendanceStatusId = attendanceStatusId;
    this.markedAt = markedAt;
    this.markedByUserId = markedByUserId;
    this.remarks = remarks;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
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

  public Long getAttendanceStatusId() {
    return attendanceStatusId;
  }

  public void setAttendanceStatusId(Long attendanceStatusId) {
    this.attendanceStatusId = attendanceStatusId;
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
}
