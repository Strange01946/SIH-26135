package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class CourseResponse {

  private Long id;
  private String courseCode;
  private String courseName;
  private String description;
  private Long sectorId;
  private String sectorCode;
  private String sectorName;
  private Long industryId;
  private String industryCode;
  private String industryName;
  private Long qualificationLevelId;
  private String qualificationLevelCode;
  private String qualificationLevelName;
  private Integer nsqfLevel;
  private Integer durationHours;
  private Integer durationDays;
  private Long deliveryModeId;
  private String deliveryModeCode;
  private String deliveryModeName;
  private Long lifecycleStatusId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public CourseResponse() {
  }

  public CourseResponse(
      Long id,
      String courseCode,
      String courseName,
      String description,
      Long sectorId,
      String sectorCode,
      String sectorName,
      Long industryId,
      String industryCode,
      String industryName,
      Long qualificationLevelId,
      String qualificationLevelCode,
      String qualificationLevelName,
      Integer nsqfLevel,
      Integer durationHours,
      Integer durationDays,
      Long deliveryModeId,
      String deliveryModeCode,
      String deliveryModeName,
      Long lifecycleStatusId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.description = description;
    this.sectorId = sectorId;
    this.sectorCode = sectorCode;
    this.sectorName = sectorName;
    this.industryId = industryId;
    this.industryCode = industryCode;
    this.industryName = industryName;
    this.qualificationLevelId = qualificationLevelId;
    this.qualificationLevelCode = qualificationLevelCode;
    this.qualificationLevelName = qualificationLevelName;
    this.nsqfLevel = nsqfLevel;
    this.durationHours = durationHours;
    this.durationDays = durationDays;
    this.deliveryModeId = deliveryModeId;
    this.deliveryModeCode = deliveryModeCode;
    this.deliveryModeName = deliveryModeName;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getSectorId() {
    return sectorId;
  }

  public void setSectorId(Long sectorId) {
    this.sectorId = sectorId;
  }

  public String getSectorCode() {
    return sectorCode;
  }

  public void setSectorCode(String sectorCode) {
    this.sectorCode = sectorCode;
  }

  public String getSectorName() {
    return sectorName;
  }

  public void setSectorName(String sectorName) {
    this.sectorName = sectorName;
  }

  public Long getIndustryId() {
    return industryId;
  }

  public void setIndustryId(Long industryId) {
    this.industryId = industryId;
  }

  public String getIndustryCode() {
    return industryCode;
  }

  public void setIndustryCode(String industryCode) {
    this.industryCode = industryCode;
  }

  public String getIndustryName() {
    return industryName;
  }

  public void setIndustryName(String industryName) {
    this.industryName = industryName;
  }

  public Long getQualificationLevelId() {
    return qualificationLevelId;
  }

  public void setQualificationLevelId(Long qualificationLevelId) {
    this.qualificationLevelId = qualificationLevelId;
  }

  public String getQualificationLevelCode() {
    return qualificationLevelCode;
  }

  public void setQualificationLevelCode(String qualificationLevelCode) {
    this.qualificationLevelCode = qualificationLevelCode;
  }

  public String getQualificationLevelName() {
    return qualificationLevelName;
  }

  public void setQualificationLevelName(String qualificationLevelName) {
    this.qualificationLevelName = qualificationLevelName;
  }

  public Integer getNsqfLevel() {
    return nsqfLevel;
  }

  public void setNsqfLevel(Integer nsqfLevel) {
    this.nsqfLevel = nsqfLevel;
  }

  public Integer getDurationHours() {
    return durationHours;
  }

  public void setDurationHours(Integer durationHours) {
    this.durationHours = durationHours;
  }

  public Integer getDurationDays() {
    return durationDays;
  }

  public void setDurationDays(Integer durationDays) {
    this.durationDays = durationDays;
  }

  public Long getDeliveryModeId() {
    return deliveryModeId;
  }

  public void setDeliveryModeId(Long deliveryModeId) {
    this.deliveryModeId = deliveryModeId;
  }

  public String getDeliveryModeCode() {
    return deliveryModeCode;
  }

  public void setDeliveryModeCode(String deliveryModeCode) {
    this.deliveryModeCode = deliveryModeCode;
  }

  public String getDeliveryModeName() {
    return deliveryModeName;
  }

  public void setDeliveryModeName(String deliveryModeName) {
    this.deliveryModeName = deliveryModeName;
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
