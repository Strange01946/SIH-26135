package in.gov.sih.sih26135.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Training batch details and lifecycle status response")
public class TrainingBatchResponse {

  @Schema(description = "Training batch primary key identifier", example = "501")
  private Long id;

  @Schema(description = "Unique batch identifier code", example = "BATCH-2026-IT01")
  private String batchCode;

  @Schema(description = "Course catalog ID", example = "101")
  private Long courseId;

  @Schema(description = "Course identifier code", example = "CRS-IT-01")
  private String courseCode;

  @Schema(description = "Course display name", example = "Certificate in Information Technology")
  private String courseName;

  @Schema(description = "Training provider ID", example = "201")
  private Long providerId;

  @Schema(description = "Training provider code", example = "TP-DEL-01")
  private String providerCode;

  @Schema(description = "Training provider organization name", example = "Apex Skills Academy")
  private String providerName;

  @Schema(description = "Training center ID", example = "301")
  private Long centerId;

  @Schema(description = "Training center code", example = "TC-DEL-NORTH")
  private String centerCode;

  @Schema(description = "Training center name", example = "Apex Skills North Delhi Center")
  private String centerName;

  @Schema(description = "Program or scheme ID", example = "1")
  private Long programId;

  @Schema(description = "Program code", example = "PMKVY-4.0")
  private String programCode;

  @Schema(description = "Program name", example = "Pradhan Mantri Kaushal Vikas Yojana 4.0")
  private String programName;

  @Schema(description = "Batch start date (ISO-8601)", example = "2026-03-01")
  private LocalDate startDate;

  @Schema(description = "Batch end date (ISO-8601)", example = "2026-06-01")
  private LocalDate endDate;

  @Schema(description = "Batch maximum capacity", example = "30")
  private Integer capacity;

  @Schema(description = "Batch status lookup ID", example = "1")
  private Long batchStatusId;

  @Schema(description = "Batch status code", example = "IN_PROGRESS")
  private String batchStatusCode;

  @Schema(description = "Batch status display name", example = "In Progress")
  private String batchStatusName;

  @Schema(description = "Creation timestamp", example = "2026-01-15T09:00:00")
  private LocalDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2026-02-01T14:20:00")
  private LocalDateTime updatedAt;

  @Schema(description = "Soft deletion timestamp if deleted", example = "null")
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
