package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class CreateJobApplicationRequest {

  private Long traineeId;
  private Long enrollmentId;
  private Long jobPostingId;
  private Long applicationStatusId;
  private LocalDate appliedDate;
  private Long referredByUserId;
  private Long nonSelectionReasonId;
  private String remarks;

  public CreateJobApplicationRequest() {
  }

  public CreateJobApplicationRequest(
      Long traineeId,
      Long jobPostingId,
      Long applicationStatusId,
      LocalDate appliedDate) {
    this.traineeId = traineeId;
    this.jobPostingId = jobPostingId;
    this.applicationStatusId = applicationStatusId;
    this.appliedDate = appliedDate;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getJobPostingId() {
    return jobPostingId;
  }

  public void setJobPostingId(Long jobPostingId) {
    this.jobPostingId = jobPostingId;
  }

  public Long getApplicationStatusId() {
    return applicationStatusId;
  }

  public void setApplicationStatusId(Long applicationStatusId) {
    this.applicationStatusId = applicationStatusId;
  }

  public LocalDate getAppliedDate() {
    return appliedDate;
  }

  public void setAppliedDate(LocalDate appliedDate) {
    this.appliedDate = appliedDate;
  }

  public Long getReferredByUserId() {
    return referredByUserId;
  }

  public void setReferredByUserId(Long referredByUserId) {
    this.referredByUserId = referredByUserId;
  }

  public Long getNonSelectionReasonId() {
    return nonSelectionReasonId;
  }

  public void setNonSelectionReasonId(Long nonSelectionReasonId) {
    this.nonSelectionReasonId = nonSelectionReasonId;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
