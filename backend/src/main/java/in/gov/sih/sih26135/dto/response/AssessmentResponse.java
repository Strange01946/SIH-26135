package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AssessmentResponse {

  private Long id;
  private String assessmentCode;
  private String assessmentName;
  private Long assessmentTypeId;
  private String assessmentTypeCode;
  private String assessmentTypeName;
  private Long courseId;
  private String courseCode;
  private String courseName;
  private Long batchId;
  private String batchCode;
  private Long programId;
  private String programCode;
  private String programName;
  private LocalDate assessmentDate;
  private BigDecimal maximumScore;
  private BigDecimal passScore;
  private Long evaluatorUserId;
  private String evaluatorName;
  private Long lifecycleStatusId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public AssessmentResponse() {
  }

  public AssessmentResponse(
      Long id,
      String assessmentCode,
      String assessmentName,
      Long assessmentTypeId,
      String assessmentTypeCode,
      String assessmentTypeName,
      Long courseId,
      String courseCode,
      String courseName,
      Long batchId,
      String batchCode,
      Long programId,
      String programCode,
      String programName,
      LocalDate assessmentDate,
      BigDecimal maximumScore,
      BigDecimal passScore,
      Long evaluatorUserId,
      String evaluatorName,
      Long lifecycleStatusId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.assessmentCode = assessmentCode;
    this.assessmentName = assessmentName;
    this.assessmentTypeId = assessmentTypeId;
    this.assessmentTypeCode = assessmentTypeCode;
    this.assessmentTypeName = assessmentTypeName;
    this.courseId = courseId;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.batchId = batchId;
    this.batchCode = batchCode;
    this.programId = programId;
    this.programCode = programCode;
    this.programName = programName;
    this.assessmentDate = assessmentDate;
    this.maximumScore = maximumScore;
    this.passScore = passScore;
    this.evaluatorUserId = evaluatorUserId;
    this.evaluatorName = evaluatorName;
    this.lifecycleStatusId = lifecycleStatusId;
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

  public String getAssessmentTypeCode() {
    return assessmentTypeCode;
  }

  public void setAssessmentTypeCode(String assessmentTypeCode) {
    this.assessmentTypeCode = assessmentTypeCode;
  }

  public String getAssessmentTypeName() {
    return assessmentTypeName;
  }

  public void setAssessmentTypeName(String assessmentTypeName) {
    this.assessmentTypeName = assessmentTypeName;
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
