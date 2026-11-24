package in.gov.sih.sih26135.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public class UpdateCourseRequest {

  @Size(max = 200, message = "courseName cannot exceed 200 characters")
  private String courseName;
  @Size(max = 65535, message = "description cannot exceed 65535 characters")
  private String description;
  @Positive private Long sectorId;
  @Positive @Schema(types = {"integer", "null"}, format = "int64")
  private Long industryId;
  @Positive @Schema(types = {"integer", "null"}, format = "int64")
  private Long qualificationLevelId;
  @Positive(message = "durationHours must be greater than 0")
  @Schema(types = {"integer", "null"}, format = "int32")
  private Integer durationHours;
  @Positive(message = "durationDays must be greater than 0")
  @Schema(types = {"integer", "null"}, format = "int32")
  private Integer durationDays;
  @Positive private Long deliveryModeId;
  @Positive private Long lifecycleStatusId;
  private boolean industryIdProvided;
  private boolean qualificationLevelIdProvided;
  private boolean durationHoursProvided;
  private boolean durationDaysProvided;

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
    if (industryId != null) setIndustryId(industryId);
    if (qualificationLevelId != null) setQualificationLevelId(qualificationLevelId);
    if (durationHours != null) setDurationHours(durationHours);
    if (durationDays != null) setDurationDays(durationDays);
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
    this.industryIdProvided = true;
  }

  public boolean hasIndustryId() { return industryIdProvided; }

  public Long getQualificationLevelId() {
    return qualificationLevelId;
  }

  public void setQualificationLevelId(Long qualificationLevelId) {
    this.qualificationLevelId = qualificationLevelId;
    this.qualificationLevelIdProvided = true;
  }

  public boolean hasQualificationLevelId() { return qualificationLevelIdProvided; }

  public Integer getDurationHours() {
    return durationHours;
  }

  public void setDurationHours(Integer durationHours) {
    this.durationHours = durationHours;
    this.durationHoursProvided = true;
  }

  public boolean hasDurationHours() { return durationHoursProvided; }

  public Integer getDurationDays() {
    return durationDays;
  }

  public void setDurationDays(Integer durationDays) {
    this.durationDays = durationDays;
    this.durationDaysProvided = true;
  }

  public boolean hasDurationDays() { return durationDaysProvided; }

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
