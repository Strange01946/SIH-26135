package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class CreateTrainingEnrollmentRequest {

  private String enrollmentNumber;
  private Long traineeId;
  private Long programId;
  private Long courseId;
  private Long providerId;
  private Long centerId;
  private Long batchId;
  private LocalDate enrollmentDate;
  private LocalDate startDate;
  private LocalDate expectedCompletionDate;
  private LocalDate actualCompletionDate;
  private Long enrollmentStatusId;
  private Long dropoutReasonId;
  private String dropoutRemarks;

  public CreateTrainingEnrollmentRequest() {
  }

  public CreateTrainingEnrollmentRequest(
      String enrollmentNumber,
      Long traineeId,
      Long batchId,
      LocalDate enrollmentDate,
      Long enrollmentStatusId) {
    this.enrollmentNumber = enrollmentNumber;
    this.traineeId = traineeId;
    this.batchId = batchId;
    this.enrollmentDate = enrollmentDate;
    this.enrollmentStatusId = enrollmentStatusId;
  }

  public CreateTrainingEnrollmentRequest(
      String enrollmentNumber,
      Long traineeId,
      Long programId,
      Long courseId,
      Long providerId,
      Long centerId,
      Long batchId,
      LocalDate enrollmentDate,
      LocalDate startDate,
      LocalDate expectedCompletionDate,
      LocalDate actualCompletionDate,
      Long enrollmentStatusId,
      Long dropoutReasonId,
      String dropoutRemarks) {
    this.enrollmentNumber = enrollmentNumber;
    this.traineeId = traineeId;
    this.programId = programId;
    this.courseId = courseId;
    this.providerId = providerId;
    this.centerId = centerId;
    this.batchId = batchId;
    this.enrollmentDate = enrollmentDate;
    this.startDate = startDate;
    this.expectedCompletionDate = expectedCompletionDate;
    this.actualCompletionDate = actualCompletionDate;
    this.enrollmentStatusId = enrollmentStatusId;
    this.dropoutReasonId = dropoutReasonId;
    this.dropoutRemarks = dropoutRemarks;
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

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
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

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public LocalDate getEnrollmentDate() {
    return enrollmentDate;
  }

  public void setEnrollmentDate(LocalDate enrollmentDate) {
    this.enrollmentDate = enrollmentDate;
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
