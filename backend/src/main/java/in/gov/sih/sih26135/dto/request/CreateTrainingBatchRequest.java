package in.gov.sih.sih26135.dto.request;

import in.gov.sih.sih26135.validation.DateSequence;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Schema(description = "Request payload for creating a new training batch")
@DateSequence(startField = "startDate", endField = "endDate")
public class CreateTrainingBatchRequest {

  @Schema(description = "Unique batch identifier code", example = "BATCH-2026-IT01", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "batchCode is required")
  @Size(max = 32, message = "batchCode cannot exceed 32 characters")
  private String batchCode;

  @Schema(description = "Associated course catalog ID", example = "101")
  private Long courseId;

  @Schema(description = "Training provider organization ID", example = "201")
  private Long providerId;

  @Schema(description = "Training center location ID", example = "301")
  private Long centerId;

  @Schema(description = "Government scheme or program ID", example = "1")
  private Long programId;

  @Schema(description = "Scheduled batch start date (ISO-8601)", example = "2026-03-01")
  private LocalDate startDate;

  @Schema(description = "Scheduled batch completion date (ISO-8601)", example = "2026-06-01")
  private LocalDate endDate;

  @Schema(description = "Maximum trainee seating capacity", example = "30")
  @Positive(message = "capacity must be greater than 0")
  private Integer capacity;

  @Schema(description = "Initial batch status ID", example = "1")
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
