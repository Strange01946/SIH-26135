package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class UpdateTrainingBatchRequest {

  private LocalDate startDate;
  private LocalDate endDate;
  @Positive(message = "capacity must be greater than 0")
  private Integer capacity;
  private Long batchStatusId;

  public UpdateTrainingBatchRequest() {
  }

  public UpdateTrainingBatchRequest(LocalDate startDate, LocalDate endDate, Integer capacity, Long batchStatusId) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.capacity = capacity;
    this.batchStatusId = batchStatusId;
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
