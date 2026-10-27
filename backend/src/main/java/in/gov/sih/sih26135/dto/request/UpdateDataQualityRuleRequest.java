package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

public class UpdateDataQualityRuleRequest {

  @Size(max = 200, message = "ruleName cannot exceed 200 characters")
  private String ruleName;
  @Size(max = 1000, message = "description cannot exceed 1000 characters")
  private String description;
  @Size(max = 64, message = "targetEntityType cannot exceed 64 characters")
  private String targetEntityType;
  private Long dataQualityCategoryId;
  private Long dataQualitySeverityId;
  private Long lifecycleStatusId;

  public UpdateDataQualityRuleRequest() {}

  public String getRuleName() {
    return ruleName;
  }

  public void setRuleName(String ruleName) {
    this.ruleName = ruleName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTargetEntityType() {
    return targetEntityType;
  }

  public void setTargetEntityType(String targetEntityType) {
    this.targetEntityType = targetEntityType;
  }

  public Long getDataQualityCategoryId() {
    return dataQualityCategoryId;
  }

  public void setDataQualityCategoryId(Long dataQualityCategoryId) {
    this.dataQualityCategoryId = dataQualityCategoryId;
  }

  public Long getDataQualitySeverityId() {
    return dataQualitySeverityId;
  }

  public void setDataQualitySeverityId(Long dataQualitySeverityId) {
    this.dataQualitySeverityId = dataQualitySeverityId;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
