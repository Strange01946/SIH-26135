package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Request payload for configuring and initiating automated data quality scans.
 * Can target a single rule, an entity type, a specific entity, or rule categories.
 */
public class DataQualityScanRequest {

  @Positive(message = "ruleId must be positive")
  private Long ruleId;

  @Size(max = 64, message = "ruleCode cannot exceed 64 characters")
  private String ruleCode;

  @Size(max = 64, message = "targetEntityType cannot exceed 64 characters")
  private String targetEntityType;

  @Positive(message = "entityId must be positive")
  private Long entityId;

  @Positive(message = "categoryId must be positive")
  private Long categoryId;

  @Positive(message = "severityId must be positive")
  private Long severityId;

  public DataQualityScanRequest() {}

  public DataQualityScanRequest(
      Long ruleId,
      String ruleCode,
      String targetEntityType,
      Long entityId,
      Long categoryId,
      Long severityId) {
    this.ruleId = ruleId;
    this.ruleCode = ruleCode;
    this.targetEntityType = targetEntityType;
    this.entityId = entityId;
    this.categoryId = categoryId;
    this.severityId = severityId;
  }

  public Long getRuleId() {
    return ruleId;
  }

  public void setRuleId(Long ruleId) {
    this.ruleId = ruleId;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public void setRuleCode(String ruleCode) {
    this.ruleCode = ruleCode;
  }

  public String getTargetEntityType() {
    return targetEntityType;
  }

  public void setTargetEntityType(String targetEntityType) {
    this.targetEntityType = targetEntityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public Long getSeverityId() {
    return severityId;
  }

  public void setSeverityId(Long severityId) {
    this.severityId = severityId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataQualityScanRequest that = (DataQualityScanRequest) o;
    return Objects.equals(ruleId, that.ruleId) &&
        Objects.equals(ruleCode, that.ruleCode) &&
        Objects.equals(targetEntityType, that.targetEntityType) &&
        Objects.equals(entityId, that.entityId) &&
        Objects.equals(categoryId, that.categoryId) &&
        Objects.equals(severityId, that.severityId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleId, ruleCode, targetEntityType, entityId, categoryId, severityId);
  }

  @Override
  public String toString() {
    return "DataQualityScanRequest{" +
        "ruleId=" + ruleId +
        ", ruleCode='" + ruleCode + '\'' +
        ", targetEntityType='" + targetEntityType + '\'' +
        ", entityId=" + entityId +
        ", categoryId=" + categoryId +
        ", severityId=" + severityId +
        '}';
  }
}
