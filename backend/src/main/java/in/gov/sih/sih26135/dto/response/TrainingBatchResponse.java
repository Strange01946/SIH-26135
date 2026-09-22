package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TrainingBatchResponse {

  private Long id;
  private String batchCode;
  private Long courseId;
  private String courseCode;
  private String courseName;
  private Long providerId;
  private String providerCode;
  private String providerName;
  private Long centerId;
  private String centerCode;
  private String centerName;
  private Long programId;
  private String programCode;
  private String programName;
  private LocalDate startDate;
  private LocalDate endDate;
  private Integer capacity;
  private Long batchStatusId;
  private String batchStatusCode;
  private String batchStatusName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public TrainingBatchResponse() {
  }

  public TrainingBatchResponse(
      Long id,
      String batchCode,
      Long courseId,
      String courseCode,
      String courseName,
      Long providerId,
      String providerCode,
      String providerName,
      Long centerId,
      String centerCode,
      String centerName,
      Long programId,
      String programCode,
      String programName,
      LocalDate startDate,
      LocalDate endDate,
      Integer capacity,
      Long batchStatusId,
      String batchStatusCode,
      String batchStatusName,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.batchCode = batchCode;
    this.courseId = courseId;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.providerId = providerId;
    this.providerCode = providerCode;
    this.providerName = providerName;
    this.centerId = centerId;
    this.centerCode = centerCode;
    this.centerName = centerName;
    this.programId = programId;
    this.programCode = programCode;
    this.programName = programName;
    this.startDate = startDate;
    this.endDate = endDate;
    this.capacity = capacity;
    this.batchStatusId = batchStatusId;
    this.batchStatusCode = batchStatusCode;
    this.batchStatusName = batchStatusName;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
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

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public String getCourseCode() {
    return courseCode;
  }

  public void setCourseCode(String courseCode) {
    this.courseCode = courseCode;
  }

  public String getCourseName() {
    return courseName;
  }

  public void setCourseName(String courseName) {
    this.courseName = courseName;
  }

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
  }

  public String getProviderCode() {
    return providerCode;
  }

  public void setProviderCode(String providerCode) {
    this.providerCode = providerCode;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public Long getCenterId() {
    return centerId;
  }

  public void setCenterId(Long centerId) {
    this.centerId = centerId;
  }

  public String getCenterCode() {
    return centerCode;
  }

  public void setCenterCode(String centerCode) {
    this.centerCode = centerCode;
  }

  public String getCenterName() {
    return centerName;
  }

  public void setCenterName(String centerName) {
    this.centerName = centerName;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public String getProgramCode() {
    return programCode;
  }

  public void setProgramCode(String programCode) {
    this.programCode = programCode;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
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

  public String getBatchStatusCode() {
    return batchStatusCode;
  }

  public void setBatchStatusCode(String batchStatusCode) {
    this.batchStatusCode = batchStatusCode;
  }

  public String getBatchStatusName() {
    return batchStatusName;
  }

  public void setBatchStatusName(String batchStatusName) {
    this.batchStatusName = batchStatusName;
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
}
