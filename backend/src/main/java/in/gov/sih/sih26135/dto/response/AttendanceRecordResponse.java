package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AttendanceRecordResponse {

  private Long id;
  private Long enrollmentId;
  private String enrollmentNumber;
  private Long traineeId;
  private Long batchId;
  private String batchCode;
  private LocalDate sessionDate;
  private Integer sessionSequence;
  private LocalTime sessionStartTime;
  private LocalTime sessionEndTime;
  private Long attendanceStatusId;
  private String attendanceStatusCode;
  private String attendanceStatusName;
  private Boolean countsAsPresent;
  private LocalDateTime markedAt;
  private Long markedByUserId;
  private String remarks;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public AttendanceRecordResponse() {
  }

  public AttendanceRecordResponse(
      Long id,
      Long enrollmentId,
      String enrollmentNumber,
      Long traineeId,
      Long batchId,
      String batchCode,
      LocalDate sessionDate,
      Integer sessionSequence,
      LocalTime sessionStartTime,
      LocalTime sessionEndTime,
      Long attendanceStatusId,
      String attendanceStatusCode,
      String attendanceStatusName,
      Boolean countsAsPresent,
      LocalDateTime markedAt,
      Long markedByUserId,
      String remarks,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.enrollmentId = enrollmentId;
    this.enrollmentNumber = enrollmentNumber;
    this.traineeId = traineeId;
    this.batchId = batchId;
    this.batchCode = batchCode;
    this.sessionDate = sessionDate;
    this.sessionSequence = sessionSequence;
    this.sessionStartTime = sessionStartTime;
    this.sessionEndTime = sessionEndTime;
    this.attendanceStatusId = attendanceStatusId;
    this.attendanceStatusCode = attendanceStatusCode;
    this.attendanceStatusName = attendanceStatusName;
    this.countsAsPresent = countsAsPresent;
    this.markedAt = markedAt;
    this.markedByUserId = markedByUserId;
    this.remarks = remarks;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public String getEnrollmentNumber() {
    return enrollmentNumber;
  }

  public void setEnrollmentNumber(String enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
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

  public String getBatchCode() {
    return batchCode;
  }

  public void setBatchCode(String batchCode) {
    this.batchCode = batchCode;
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
    this.sessionSequence = sessionSequence;
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

  public String getAttendanceStatusCode() {
    return attendanceStatusCode;
  }

  public void setAttendanceStatusCode(String attendanceStatusCode) {
    this.attendanceStatusCode = attendanceStatusCode;
  }

  public String getAttendanceStatusName() {
    return attendanceStatusName;
  }

  public void setAttendanceStatusName(String attendanceStatusName) {
    this.attendanceStatusName = attendanceStatusName;
  }

  public Boolean getCountsAsPresent() {
    return countsAsPresent;
  }

  public void setCountsAsPresent(Boolean countsAsPresent) {
    this.countsAsPresent = countsAsPresent;
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
}
