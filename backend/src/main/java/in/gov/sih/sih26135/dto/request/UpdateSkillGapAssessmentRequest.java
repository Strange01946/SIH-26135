package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class UpdateSkillGapAssessmentRequest {

  private Long enrollmentId;
  private Long courseId;
  private Long batchId;
  private Long jobRoleId;
  private Long jobPostingId;
  private Long employmentRecordId;
  private Long placementRecordId;
  private Long traineeAssessmentId;
  private Long assessmentResultId;
  private Long certificationId;
  private Long surveyResponseId;
  private Long followupTaskId;
  private Long employmentVerificationId;
  private Long skillGapSourceId;
  private Long skillGapAssessmentStatusId;
  private LocalDate assessedOn;
  private Long assessedByUserId;
  private String notes;

  public UpdateSkillGapAssessmentRequest() {}

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public Long getJobRoleId() {
    return jobRoleId;
  }

  public void setJobRoleId(Long jobRoleId) {
    this.jobRoleId = jobRoleId;
  }

  public Long getJobPostingId() {
    return jobPostingId;
  }

  public void setJobPostingId(Long jobPostingId) {
    this.jobPostingId = jobPostingId;
  }

  public Long getEmploymentRecordId() {
    return employmentRecordId;
  }

  public void setEmploymentRecordId(Long employmentRecordId) {
    this.employmentRecordId = employmentRecordId;
  }

  public Long getPlacementRecordId() {
    return placementRecordId;
  }

  public void setPlacementRecordId(Long placementRecordId) {
    this.placementRecordId = placementRecordId;
  }

  public Long getTraineeAssessmentId() {
    return traineeAssessmentId;
  }

  public void setTraineeAssessmentId(Long traineeAssessmentId) {
    this.traineeAssessmentId = traineeAssessmentId;
  }

  public Long getAssessmentResultId() {
    return assessmentResultId;
  }

  public void setAssessmentResultId(Long assessmentResultId) {
    this.assessmentResultId = assessmentResultId;
  }

  public Long getCertificationId() {
    return certificationId;
  }

  public void setCertificationId(Long certificationId) {
    this.certificationId = certificationId;
  }

  public Long getSurveyResponseId() {
    return surveyResponseId;
  }

  public void setSurveyResponseId(Long surveyResponseId) {
    this.surveyResponseId = surveyResponseId;
  }

  public Long getFollowupTaskId() {
    return followupTaskId;
  }

  public void setFollowupTaskId(Long followupTaskId) {
    this.followupTaskId = followupTaskId;
  }

  public Long getEmploymentVerificationId() {
    return employmentVerificationId;
  }

  public void setEmploymentVerificationId(Long employmentVerificationId) {
    this.employmentVerificationId = employmentVerificationId;
  }

  public Long getSkillGapSourceId() {
    return skillGapSourceId;
  }

  public void setSkillGapSourceId(Long skillGapSourceId) {
    this.skillGapSourceId = skillGapSourceId;
  }

  public Long getSkillGapAssessmentStatusId() {
    return skillGapAssessmentStatusId;
  }

  public void setSkillGapAssessmentStatusId(Long skillGapAssessmentStatusId) {
    this.skillGapAssessmentStatusId = skillGapAssessmentStatusId;
  }

  public LocalDate getAssessedOn() {
    return assessedOn;
  }

  public void setAssessedOn(LocalDate assessedOn) {
    this.assessedOn = assessedOn;
  }

  public Long getAssessedByUserId() {
    return assessedByUserId;
  }

  public void setAssessedByUserId(Long assessedByUserId) {
    this.assessedByUserId = assessedByUserId;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
