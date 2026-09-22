package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class UpdateTrainingEnrollmentRequest {

  private LocalDate startDate;
  private LocalDate expectedCompletionDate;
  private LocalDate actualCompletionDate;
  private Long enrollmentStatusId;
  private Long dropoutReasonId;
  private String dropoutRemarks;

  public UpdateTrainingEnrollmentRequest() {
  }

  public UpdateTrainingEnrollmentRequest(
      LocalDate startDate,
      LocalDate expectedCompletionDate,
      LocalDate actualCompletionDate,
      Long enrollmentStatusId,
      Long dropoutReasonId,
      String dropoutRemarks) {
    this.startDate = startDate;
    this.expectedCompletionDate = expectedCompletionDate;
    this.actualCompletionDate = actualCompletionDate;
    this.enrollmentStatusId = enrollmentStatusId;
    this.dropoutReasonId = dropoutReasonId;
    this.dropoutRemarks = dropoutRemarks;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getExpectedCompletionDate() {
    return expectedCompletionDate;
  }

  public void setExpectedCompletionDate(LocalDate expectedCompletionDate) {
    this.expectedCompletionDate = expectedCompletionDate;
  }

  public LocalDate getActualCompletionDate() {
    return actualCompletionDate;
  }

  public void setActualCompletionDate(LocalDate actualCompletionDate) {
    this.actualCompletionDate = actualCompletionDate;
  }

  public Long getEnrollmentStatusId() {
    return enrollmentStatusId;
  }

  public void setEnrollmentStatusId(Long enrollmentStatusId) {
    this.enrollmentStatusId = enrollmentStatusId;
  }

  public Long getDropoutReasonId() {
    return dropoutReasonId;
  }

  public void setDropoutReasonId(Long dropoutReasonId) {
    this.dropoutReasonId = dropoutReasonId;
  }

  public String getDropoutRemarks() {
    return dropoutRemarks;
  }

  public void setDropoutRemarks(String dropoutRemarks) {
    this.dropoutRemarks = dropoutRemarks;
  }
}
