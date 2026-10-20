package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "organizations")
public class Organization {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "organization_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "organization_code", length = 32, nullable = false, unique = true)
  private String organizationCode;

  @Column(name = "organization_name", length = 200, nullable = false)
  private String organizationName;

  @Column(name = "organization_type_id", nullable = false)
  private Long organizationTypeId;

  @Column(name = "parent_organization_id")
  private Long parentOrganizationId;

  @Column(name = "state_id")
  private Long stateId;

  @Column(name = "district_id")
  private Long districtId;

  @Column(name = "location_id")
  private Long locationId;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Organization() {
  }

  public Organization(Long id, String organizationCode, String organizationName, Long organizationTypeId, Long lifecycleStatusId) {
    this.id = id;
    this.organizationCode = organizationCode;
    this.organizationName = organizationName;
    this.organizationTypeId = organizationTypeId;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrganizationCode() {
    return organizationCode;
  }

  public void setOrganizationCode(String organizationCode) {
    this.organizationCode = organizationCode;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public Long getOrganizationTypeId() {
    return organizationTypeId;
  }

  public void setOrganizationTypeId(Long organizationTypeId) {
    this.organizationTypeId = organizationTypeId;
  }

  public Long getParentOrganizationId() {
    return parentOrganizationId;
  }

  public void setParentOrganizationId(Long parentOrganizationId) {
    this.parentOrganizationId = parentOrganizationId;
  }

  public Long getStateId() {
    return stateId;
  }

  public void setStateId(Long stateId) {
    this.stateId = stateId;
  }

  public Long getDistrictId() {
    return districtId;
  }

  public void setDistrictId(Long districtId) {
    this.districtId = districtId;
  }

  public Long getLocationId() {
    return locationId;
  }

  public void setLocationId(Long locationId) {
    this.locationId = locationId;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Organization that = (Organization) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
