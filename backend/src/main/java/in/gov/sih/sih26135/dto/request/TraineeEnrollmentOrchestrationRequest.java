package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request payload for orchestrating trainee enrollment with mandatory consent verification
 * and batch-provider consistency validation.
 */
public class TraineeEnrollmentOrchestrationRequest {

  @NotNull(message = "Trainee ID is required")
  private Long traineeId;

  @NotNull(message = "Training batch ID is required")
  private Long batchId;

  @NotNull(message = "Enrollment date is required")
  private LocalDate enrollmentDate;

  @Size(max = 32, message = "Enrollment number cannot exceed 32 characters")
  private String enrollmentNumber;

  @Size(max = 64, message = "Consent code cannot exceed 64 characters")
  private String consentCode;

  private Long consentTypeId;
  private Long trainingProviderId;
  private Long programId;
  private Long courseId;
  private Long centerId;
  private Long enrollmentStatusId;
  private LocalDate startDate;
  private LocalDate expectedCompletionDate;

  public TraineeEnrollmentOrchestrationRequest() {
  }

  public TraineeEnrollmentOrchestrationRequest(Long traineeId, Long batchId, LocalDate enrollmentDate) {
    this.traineeId = traineeId;
    this.batchId = batchId;
    this.enrollmentDate = enrollmentDate;
  }

  public TraineeEnrollmentOrchestrationRequest(
      Long traineeId,
      Long batchId,
      LocalDate enrollmentDate,
      String enrollmentNumber,
      String consentCode,
      Long trainingProviderId) {
    this.traineeId = traineeId;
    this.batchId = batchId;
    this.enrollmentDate = enrollmentDate;
    this.enrollmentNumber = enrollmentNumber;
    this.consentCode = consentCode;
    this.trainingProviderId = trainingProviderId;
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

  public LocalDate getEnrollmentDate() {
    return enrollmentDate;
  }

  public void setEnrollmentDate(LocalDate enrollmentDate) {
    this.enrollmentDate = enrollmentDate;
  }

  public String getEnrollmentNumber() {
    return enrollmentNumber;
  }

  public void setEnrollmentNumber(String enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
  }

  public String getConsentCode() {
    return consentCode;
  }

  public void setConsentCode(String consentCode) {
    this.consentCode = consentCode;
  }

  public Long getConsentTypeId() {
    return consentTypeId;
  }

  public void setConsentTypeId(Long consentTypeId) {
    this.consentTypeId = consentTypeId;
  }

  public Long getTrainingProviderId() {
    return trainingProviderId;
  }

  public void setTrainingProviderId(Long trainingProviderId) {
    this.trainingProviderId = trainingProviderId;
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

  public Long getCenterId() {
    return centerId;
  }

  public void setCenterId(Long centerId) {
    this.centerId = centerId;
  }

  public Long getEnrollmentStatusId() {
    return enrollmentStatusId;
  }

  public void setEnrollmentStatusId(Long enrollmentStatusId) {
    this.enrollmentStatusId = enrollmentStatusId;
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
}
