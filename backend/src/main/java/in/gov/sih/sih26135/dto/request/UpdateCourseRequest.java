package in.gov.sih.sih26135.dto.request;

public class UpdateCourseRequest {

  private String courseName;
  private String description;
  private Long sectorId;
  private Long industryId;
  private Long qualificationLevelId;
  private Integer durationHours;
  private Integer durationDays;
  private Long deliveryModeId;
  private Long lifecycleStatusId;

  public UpdateCourseRequest() {
  }

  public UpdateCourseRequest(
      String courseName,
      String description,
      Long sectorId,
      Long industryId,
      Long qualificationLevelId,
      Integer durationHours,
      Integer durationDays,
      Long deliveryModeId,
      Long lifecycleStatusId) {
    this.courseName = courseName;
    this.description = description;
    this.sectorId = sectorId;
    this.industryId = industryId;
    this.qualificationLevelId = qualificationLevelId;
    this.durationHours = durationHours;
    this.durationDays = durationDays;
    this.deliveryModeId = deliveryModeId;
    this.lifecycleStatusId = lifecycleStatusId;
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

  public Long getIndustryId() {
    return industryId;
  }

  public void setIndustryId(Long industryId) {
    this.industryId = industryId;
  }

  public Long getQualificationLevelId() {
    return qualificationLevelId;
  }

  public void setQualificationLevelId(Long qualificationLevelId) {
    this.qualificationLevelId = qualificationLevelId;
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

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
