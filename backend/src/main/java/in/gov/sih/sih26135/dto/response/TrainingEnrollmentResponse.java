package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TrainingEnrollmentResponse {

  private Long id;
  private String enrollmentNumber;
  private Long traineeId;
  private String traineeRegistrationNumber;
  private String traineeFullName;
  private Long programId;
  private String programCode;
  private String programName;
  private Long courseId;
  private String courseCode;
  private String courseName;
  private Long providerId;
  private String providerCode;
  private String providerName;
  private Long centerId;
  private String centerCode;
  private String centerName;
  private Long batchId;
  private String batchCode;
  private LocalDate enrollmentDate;
  private LocalDate startDate;
  private LocalDate expectedCompletionDate;
  private LocalDate actualCompletionDate;
  private Long enrollmentStatusId;
  private String enrollmentStatusCode;
  private String enrollmentStatusName;
  private Boolean isTerminal;
  private Boolean isCompletedFlag;
  private Long dropoutReasonId;
  private String dropoutReasonCode;
  private String dropoutReasonName;
  private String dropoutRemarks;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public TrainingEnrollmentResponse() {
  }

  public TrainingEnrollmentResponse(
      Long id,
      String enrollmentNumber,
      Long traineeId,
      String traineeRegistrationNumber,
      String traineeFullName,
      Long programId,
      String programCode,
      String programName,
      Long courseId,
      String courseCode,
      String courseName,
      Long providerId,
      String providerCode,
      String providerName,
      Long centerId,
      String centerCode,
      String centerName,
      Long batchId,
      String batchCode,
      LocalDate enrollmentDate,
      LocalDate startDate,
      LocalDate expectedCompletionDate,
      LocalDate actualCompletionDate,
      Long enrollmentStatusId,
      String enrollmentStatusCode,
      String enrollmentStatusName,
      Boolean isTerminal,
      Boolean isCompletedFlag,
      Long dropoutReasonId,
      String dropoutReasonCode,
      String dropoutReasonName,
      String dropoutRemarks,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.enrollmentNumber = enrollmentNumber;
    this.traineeId = traineeId;
    this.traineeRegistrationNumber = traineeRegistrationNumber;
    this.traineeFullName = traineeFullName;
    this.programId = programId;
    this.programCode = programCode;
    this.programName = programName;
    this.courseId = courseId;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.providerId = providerId;
    this.providerCode = providerCode;
    this.providerName = providerName;
    this.centerId = centerId;
    this.centerCode = centerCode;
    this.centerName = centerName;
    this.batchId = batchId;
    this.batchCode = batchCode;
    this.enrollmentDate = enrollmentDate;
    this.startDate = startDate;
    this.expectedCompletionDate = expectedCompletionDate;
    this.actualCompletionDate = actualCompletionDate;
    this.enrollmentStatusId = enrollmentStatusId;
    this.enrollmentStatusCode = enrollmentStatusCode;
    this.enrollmentStatusName = enrollmentStatusName;
    this.isTerminal = isTerminal;
    this.isCompletedFlag = isCompletedFlag;
    this.dropoutReasonId = dropoutReasonId;
    this.dropoutReasonCode = dropoutReasonCode;
    this.dropoutReasonName = dropoutReasonName;
    this.dropoutRemarks = dropoutRemarks;
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

  public String getTraineeRegistrationNumber() {
    return traineeRegistrationNumber;
  }

  public void setTraineeRegistrationNumber(String traineeRegistrationNumber) {
    this.traineeRegistrationNumber = traineeRegistrationNumber;
  }

  public String getTraineeFullName() {
    return traineeFullName;
  }

  public void setTraineeFullName(String traineeFullName) {
    this.traineeFullName = traineeFullName;
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

  public String getEnrollmentStatusCode() {
    return enrollmentStatusCode;
  }

  public void setEnrollmentStatusCode(String enrollmentStatusCode) {
    this.enrollmentStatusCode = enrollmentStatusCode;
  }

  public String getEnrollmentStatusName() {
    return enrollmentStatusName;
  }

  public void setEnrollmentStatusName(String enrollmentStatusName) {
    this.enrollmentStatusName = enrollmentStatusName;
  }

  public Boolean getIsTerminal() {
    return isTerminal;
  }

  public void setIsTerminal(Boolean terminal) {
    isTerminal = terminal;
  }

  public Boolean getIsCompletedFlag() {
    return isCompletedFlag;
  }

  public void setIsCompletedFlag(Boolean completedFlag) {
    isCompletedFlag = completedFlag;
  }

  public Long getDropoutReasonId() {
    return dropoutReasonId;
  }

  public void setDropoutReasonId(Long dropoutReasonId) {
    this.dropoutReasonId = dropoutReasonId;
  }

  public String getDropoutReasonCode() {
    return dropoutReasonCode;
  }

  public void setDropoutReasonCode(String dropoutReasonCode) {
    this.dropoutReasonCode = dropoutReasonCode;
  }

  public String getDropoutReasonName() {
    return dropoutReasonName;
  }

  public void setDropoutReasonName(String dropoutReasonName) {
    this.dropoutReasonName = dropoutReasonName;
  }

  public String getDropoutRemarks() {
    return dropoutRemarks;
  }

  public void setDropoutRemarks(String dropoutRemarks) {
    this.dropoutRemarks = dropoutRemarks;
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
