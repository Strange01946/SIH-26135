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
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "training_centers")
public class TrainingCenter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "center_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", nullable = false)
  private TrainingProvider trainingProvider;

  @Column(name = "center_code", length = 32, nullable = false, unique = true)
  private String centerCode;

  @Column(name = "center_name", length = 200, nullable = false)
  private String centerName;

  @Column(name = "address_line1", length = 200)
  private String addressLine1;

  @Column(name = "address_line2", length = 200)
  private String addressLine2;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "pincode", length = 6, columnDefinition = "CHAR(6)")
  private String pincode;

  @Column(name = "location_id")
  private Long locationId;

  @Column(name = "state_id", nullable = false)
  private Long stateId;

  @Column(name = "district_id", nullable = false)
  private Long districtId;

  @Column(name = "operational_capacity")
  private Integer operationalCapacity;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public TrainingCenter() {
  }

  public TrainingCenter(TrainingProvider trainingProvider, String centerCode, String centerName,
      Long stateId, Long districtId, Long lifecycleStatusId) {
    this.trainingProvider = trainingProvider;
    this.centerCode = centerCode;
    this.centerName = centerName;
    this.stateId = stateId;
    this.districtId = districtId;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public TrainingProvider getTrainingProvider() {
    return trainingProvider;
  }

  public void setTrainingProvider(TrainingProvider trainingProvider) {
    this.trainingProvider = trainingProvider;
  }

  public String getCenterCode() {
    return centerCode;
  }

  public void setCenterCode(String centerCode) {
    this.centerCode = centerCode;
  }

  public String getCenterName() {
    return centerName;
  }

  public void setCenterName(String centerName) {
    this.centerName = centerName;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public void setAddressLine1(String addressLine1) {
    this.addressLine1 = addressLine1;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public void setAddressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
  }

  public String getPincode() {
    return pincode;
  }

  public void setPincode(String pincode) {
    this.pincode = pincode;
  }

  public Long getLocationId() {
    return locationId;
  }

  public void setLocationId(Long locationId) {
    this.locationId = locationId;
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

  public Integer getOperationalCapacity() {
    return operationalCapacity;
  }

  public void setOperationalCapacity(Integer operationalCapacity) {
    this.operationalCapacity = operationalCapacity;
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
    if (this == o) {
      return true;
    }
    if (!(o instanceof TrainingCenter other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "TrainingCenter{" +
        "id=" + id +
        ", centerCode='" + centerCode + '\'' +
        ", centerName='" + centerName + '\'' +
        ", stateId=" + stateId +
        ", districtId=" + districtId +
        '}';
  }
}
