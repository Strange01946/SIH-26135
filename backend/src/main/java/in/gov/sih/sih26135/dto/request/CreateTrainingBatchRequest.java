package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class CreateTrainingBatchRequest {

  private String batchCode;
  private Long courseId;
  private Long providerId;
  private Long centerId;
  private Long programId;
  private LocalDate startDate;
  private LocalDate endDate;
  private Integer capacity;
  private Long batchStatusId;

  public CreateTrainingBatchRequest() {
  }

  public CreateTrainingBatchRequest(
      String batchCode,
      Long courseId,
      Long providerId,
      Long centerId,
      Long programId,
      LocalDate startDate,
      LocalDate endDate,
      Integer capacity,
      Long batchStatusId) {
    this.batchCode = batchCode;
    this.courseId = courseId;
    this.providerId = providerId;
    this.centerId = centerId;
    this.programId = programId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.capacity = capacity;
    this.batchStatusId = batchStatusId;
  }

  public String getBatchCode() {
    return batchCode;
  }

  public void setBatchCode(String batchCode) {
    this.batchCode = batchCode;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
  }

  public Long getCenterId() {
    return centerId;
  }

  public void setCenterId(Long centerId) {
    this.centerId = centerId;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
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

  public Long getBatchStatusId() {
    return batchStatusId;
  }

  public void setBatchStatusId(Long batchStatusId) {
    this.batchStatusId = batchStatusId;
  }
}
