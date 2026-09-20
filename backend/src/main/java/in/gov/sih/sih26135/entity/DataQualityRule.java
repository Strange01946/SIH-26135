package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "data_quality_rules", uniqueConstraints = {
    @UniqueConstraint(name = "uk_data_quality_rules_code", columnNames = {"rule_code"})
})
public class DataQualityRule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_quality_rule_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "rule_code", length = 64, nullable = false, unique = true)
  private String ruleCode;

  @Column(name = "rule_name", length = 200, nullable = false)
  private String ruleName;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "target_entity_type", length = 64, nullable = false)
  private String targetEntityType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_category_id", nullable = false)
  private RefDataQualityCategory dataQualityCategory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_severity_id", nullable = false)
  private RefDataQualitySeverity dataQualitySeverity;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public DataQualityRule() {
  }

  public DataQualityRule(String ruleCode, String ruleName, String targetEntityType,
      RefDataQualityCategory dataQualityCategory, RefDataQualitySeverity dataQualitySeverity,
      Long lifecycleStatusId) {
    this.ruleCode = ruleCode;
    this.ruleName = ruleName;
    this.targetEntityType = targetEntityType;
    this.dataQualityCategory = dataQualityCategory;
    this.dataQualitySeverity = dataQualitySeverity;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public void setRuleCode(String ruleCode) {
    this.ruleCode = ruleCode;
  }

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

  public RefDataQualityCategory getDataQualityCategory() {
    return dataQualityCategory;
  }

  public void setDataQualityCategory(RefDataQualityCategory dataQualityCategory) {
    this.dataQualityCategory = dataQualityCategory;
  }

  public RefDataQualitySeverity getDataQualitySeverity() {
    return dataQualitySeverity;
  }

  public void setDataQualitySeverity(RefDataQualitySeverity dataQualitySeverity) {
    this.dataQualitySeverity = dataQualitySeverity;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DataQualityRule that)) {
      return false;
    }
    return Objects.equals(ruleCode, that.ruleCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleCode);
  }

  @Override
  public String toString() {
    return "DataQualityRule{" +
        "id=" + id +
        ", ruleCode='" + ruleCode + '\'' +
        ", ruleName='" + ruleName + '\'' +
        ", targetEntityType='" + targetEntityType + '\'' +
        '}';
  }
}
