package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class EngagementTypeResponse {

  private Long id;
  private String typeCode;
  private String typeName;
  private Boolean isWageEmployment;
  private Boolean isSelfEmployment;
  private Boolean isApprenticeship;
  private Boolean isInternship;
  private Boolean requiresEmployer;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public EngagementTypeResponse() {
  }

  public EngagementTypeResponse(
      Long id,
      String typeCode,
      String typeName,
      Boolean isWageEmployment,
      Boolean isSelfEmployment,
      Boolean isApprenticeship,
      Boolean isInternship,
      Boolean requiresEmployer,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.typeCode = typeCode;
    this.typeName = typeName;
    this.isWageEmployment = isWageEmployment;
    this.isSelfEmployment = isSelfEmployment;
    this.isApprenticeship = isApprenticeship;
    this.isInternship = isInternship;
    this.requiresEmployer = requiresEmployer;
    this.sortOrder = sortOrder;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTypeCode() {
    return typeCode;
  }

  public void setTypeCode(String typeCode) {
    this.typeCode = typeCode;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public Boolean getIsWageEmployment() {
    return isWageEmployment;
  }

  public void setIsWageEmployment(Boolean isWageEmployment) {
    this.isWageEmployment = isWageEmployment;
  }

  public Boolean getIsSelfEmployment() {
    return isSelfEmployment;
  }

  public void setIsSelfEmployment(Boolean isSelfEmployment) {
    this.isSelfEmployment = isSelfEmployment;
  }

  public Boolean getIsApprenticeship() {
    return isApprenticeship;
  }

  public void setIsApprenticeship(Boolean isApprenticeship) {
    this.isApprenticeship = isApprenticeship;
  }

  public Boolean getIsInternship() {
    return isInternship;
  }

  public void setIsInternship(Boolean isInternship) {
    this.isInternship = isInternship;
  }

  public Boolean getRequiresEmployer() {
    return requiresEmployer;
  }

  public void setRequiresEmployer(Boolean requiresEmployer) {
    this.requiresEmployer = requiresEmployer;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
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
}
