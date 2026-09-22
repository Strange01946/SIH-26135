package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class UpdateAttendanceRecordRequest {

  private LocalTime sessionStartTime;
  private LocalTime sessionEndTime;
  private Long attendanceStatusId;
  private LocalDateTime markedAt;
  private Long markedByUserId;
  private String remarks;

  public UpdateAttendanceRecordRequest() {
  }

  public UpdateAttendanceRecordRequest(
      LocalTime sessionStartTime,
      LocalTime sessionEndTime,
      Long attendanceStatusId,
      LocalDateTime markedAt,
      Long markedByUserId,
      String remarks) {
    this.sessionStartTime = sessionStartTime;
    this.sessionEndTime = sessionEndTime;
    this.attendanceStatusId = attendanceStatusId;
    this.markedAt = markedAt;
    this.markedByUserId = markedByUserId;
    this.remarks = remarks;
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
