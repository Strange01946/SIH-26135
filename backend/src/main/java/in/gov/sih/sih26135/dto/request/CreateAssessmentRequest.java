package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateAssessmentRequest {

  private String assessmentCode;
  private String assessmentName;
  private Long assessmentTypeId;
  private Long courseId;
  private Long batchId;
  private Long programId;
  private LocalDate assessmentDate;
  private BigDecimal maximumScore;
  private BigDecimal passScore;
  private Long evaluatorUserId;
  private String evaluatorName;
  private Long lifecycleStatusId;

  public CreateAssessmentRequest() {
  }

  public CreateAssessmentRequest(
      String assessmentCode,
      String assessmentName,
      Long assessmentTypeId,
      Long batchId,
      LocalDate assessmentDate,
      BigDecimal maximumScore,
      Long lifecycleStatusId) {
    this.assessmentCode = assessmentCode;
    this.assessmentName = assessmentName;
    this.assessmentTypeId = assessmentTypeId;
    this.batchId = batchId;
    this.assessmentDate = assessmentDate;
    this.maximumScore = maximumScore;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public CreateAssessmentRequest(
      String assessmentCode,
      String assessmentName,
      Long assessmentTypeId,
      Long courseId,
      Long batchId,
      Long programId,
      LocalDate assessmentDate,
      BigDecimal maximumScore,
      BigDecimal passScore,
      Long evaluatorUserId,
      String evaluatorName,
      Long lifecycleStatusId) {
    this.assessmentCode = assessmentCode;
    this.assessmentName = assessmentName;
    this.assessmentTypeId = assessmentTypeId;
    this.courseId = courseId;
    this.batchId = batchId;
    this.programId = programId;
    this.assessmentDate = assessmentDate;
    this.maximumScore = maximumScore;
    this.passScore = passScore;
    this.evaluatorUserId = evaluatorUserId;
    this.evaluatorName = evaluatorName;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public String getAssessmentCode() {
    return assessmentCode;
  }

  public void setAssessmentCode(String assessmentCode) {
    this.assessmentCode = assessmentCode;
  }

  public String getAssessmentName() {
    return assessmentName;
  }

  public void setAssessmentName(String assessmentName) {
    this.assessmentName = assessmentName;
  }

  public Long getAssessmentTypeId() {
    return assessmentTypeId;
  }

  public void setAssessmentTypeId(Long assessmentTypeId) {
    this.assessmentTypeId = assessmentTypeId;
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

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public LocalDate getAssessmentDate() {
    return assessmentDate;
  }

  public void setAssessmentDate(LocalDate assessmentDate) {
    this.assessmentDate = assessmentDate;
  }

  public BigDecimal getMaximumScore() {
    return maximumScore;
  }

  public void setMaximumScore(BigDecimal maximumScore) {
    this.maximumScore = maximumScore;
  }

  public BigDecimal getPassScore() {
    return passScore;
  }

  public void setPassScore(BigDecimal passScore) {
    this.passScore = passScore;
  }

  public Long getEvaluatorUserId() {
    return evaluatorUserId;
  }

  public void setEvaluatorUserId(Long evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
  }

  public String getEvaluatorName() {
    return evaluatorName;
  }

  public void setEvaluatorName(String evaluatorName) {
    this.evaluatorName = evaluatorName;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
