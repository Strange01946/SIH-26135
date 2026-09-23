package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateEmploymentVerificationRequest {

  private String verificationNumber;
  private Long employmentVerificationRequestId;
  private Long employmentRecordId;
  private Long traineeId;
  private Long placementRecordId;
  private Long employerId;
  private Integer cycleNumber;
  private Boolean isReverification;
  private Boolean isCurrent;
  private Long recordVerificationStatusId;
  private Long employmentVerificationMethodId;
  private Long employmentInfoSourceId;
  private Long employmentVerificationRejectionReasonId;
  private Long verifiedByUserId;
  private Long employerRespondentUserId;
  private Boolean traineeAttestedFlag;
  private Boolean employerConfirmedFlag;
  private LocalDateTime requestedAt;
  private LocalDateTime verifiedAt;
  private LocalDateTime outcomeRecordedAt;
  private String notes;
  private String rejectionNotes;

  public CreateEmploymentVerificationRequest() {}

  public CreateEmploymentVerificationRequest(
      String verificationNumber,
      Long employmentRecordId,
      Long recordVerificationStatusId,
      Long employmentVerificationMethodId,
      LocalDateTime requestedAt) {
    this.verificationNumber = verificationNumber;
    this.employmentRecordId = employmentRecordId;
    this.recordVerificationStatusId = recordVerificationStatusId;
    this.employmentVerificationMethodId = employmentVerificationMethodId;
    this.requestedAt = requestedAt;
  }

  public String getVerificationNumber() {
    return verificationNumber;
  }

  public void setVerificationNumber(String verificationNumber) {
    this.verificationNumber = verificationNumber;
  }

  public Long getEmploymentVerificationRequestId() {
    return employmentVerificationRequestId;
  }

  public void setEmploymentVerificationRequestId(Long employmentVerificationRequestId) {
    this.employmentVerificationRequestId = employmentVerificationRequestId;
  }

  public Long getEmploymentRecordId() {
    return employmentRecordId;
  }

  public void setEmploymentRecordId(Long employmentRecordId) {
    this.employmentRecordId = employmentRecordId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getPlacementRecordId() {
    return placementRecordId;
  }

  public void setPlacementRecordId(Long placementRecordId) {
    this.placementRecordId = placementRecordId;
  }

  public Long getEmployerId() {
    return employerId;
  }

  public void setEmployerId(Long employerId) {
    this.employerId = employerId;
  }

  public Integer getCycleNumber() {
    return cycleNumber;
  }

  public void setCycleNumber(Integer cycleNumber) {
    this.cycleNumber = cycleNumber;
  }

  public Boolean getIsReverification() {
    return isReverification;
  }

  public void setIsReverification(Boolean reverification) {
    isReverification = reverification;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean current) {
    isCurrent = current;
  }

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

  public LocalDateTime getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(LocalDateTime requestedAt) {
    this.requestedAt = requestedAt;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public LocalDateTime getOutcomeRecordedAt() {
    return outcomeRecordedAt;
  }

  public void setOutcomeRecordedAt(LocalDateTime outcomeRecordedAt) {
    this.outcomeRecordedAt = outcomeRecordedAt;
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
}
