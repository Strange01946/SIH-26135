package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class UpdateEmploymentVerificationRequest {

  private Long recordVerificationStatusId;
  private Long employmentVerificationMethodId;
  private Long employmentInfoSourceId;
  private Long employmentVerificationRejectionReasonId;
  private Long verifiedByUserId;
  private Long employerRespondentUserId;
  private Boolean traineeAttestedFlag;
  private Boolean employerConfirmedFlag;
  private LocalDateTime verifiedAt;
  private String notes;
  private String rejectionNotes;
  private Boolean isCurrent;

  public UpdateEmploymentVerificationRequest() {}

  public Long getRecordVerificationStatusId() {
    return recordVerificationStatusId;
  }

  public void setRecordVerificationStatusId(Long recordVerificationStatusId) {
    this.recordVerificationStatusId = recordVerificationStatusId;
  }

  public Long getEmploymentVerificationMethodId() {
    return employmentVerificationMethodId;
  }

  public void setEmploymentVerificationMethodId(Long employmentVerificationMethodId) {
    this.employmentVerificationMethodId = employmentVerificationMethodId;
  }

  public Long getEmploymentInfoSourceId() {
    return employmentInfoSourceId;
  }

  public void setEmploymentInfoSourceId(Long employmentInfoSourceId) {
    this.employmentInfoSourceId = employmentInfoSourceId;
  }

  public Long getEmploymentVerificationRejectionReasonId() {
    return employmentVerificationRejectionReasonId;
  }

  public void setEmploymentVerificationRejectionReasonId(Long employmentVerificationRejectionReasonId) {
    this.employmentVerificationRejectionReasonId = employmentVerificationRejectionReasonId;
  }

  public Long getVerifiedByUserId() {
    return verifiedByUserId;
  }

  public void setVerifiedByUserId(Long verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
  }

  public Long getEmployerRespondentUserId() {
    return employerRespondentUserId;
  }

  public void setEmployerRespondentUserId(Long employerRespondentUserId) {
    this.employerRespondentUserId = employerRespondentUserId;
  }

  public Boolean getTraineeAttestedFlag() {
    return traineeAttestedFlag;
  }

  public void setTraineeAttestedFlag(Boolean traineeAttestedFlag) {
    this.traineeAttestedFlag = traineeAttestedFlag;
  }

  public Boolean getEmployerConfirmedFlag() {
    return employerConfirmedFlag;
  }

  public void setEmployerConfirmedFlag(Boolean employerConfirmedFlag) {
    this.employerConfirmedFlag = employerConfirmedFlag;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getRejectionNotes() {
    return rejectionNotes;
  }

  public void setRejectionNotes(String rejectionNotes) {
    this.rejectionNotes = rejectionNotes;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean current) {
    isCurrent = current;
  }
}
