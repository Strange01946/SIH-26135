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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "employer_branches", uniqueConstraints = {
    @UniqueConstraint(name = "uk_employer_branches_code", columnNames = {"employer_id", "branch_code"})
})
public class EmployerBranch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employer_branch_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_id", nullable = false)
  private Employer employer;

  @Column(name = "branch_code", length = 32, nullable = false)
  private String branchCode;

  @Column(name = "branch_name", length = 200, nullable = false)
  private String branchName;

  @Column(name = "is_head_office", nullable = false)
  private Boolean isHeadOffice = false;

  @Column(name = "contact_person_name", length = 150)
  private String contactPersonName;

  @Column(name = "contact_email", length = 255)
  private String contactEmail;

  @Column(name = "contact_phone", length = 15)
  private String contactPhone;

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

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public EmployerBranch() {
  }

  public EmployerBranch(Employer employer, String branchCode, String branchName,
      Long stateId, Long districtId, Long lifecycleStatusId) {
    this.employer = employer;
    this.branchCode = branchCode;
    this.branchName = branchName;
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

  public Employer getEmployer() {
    return employer;
  }

  public void setEmployer(Employer employer) {
    this.employer = employer;
  }

  public String getBranchCode() {
    return branchCode;
  }

  public void setBranchCode(String branchCode) {
    this.branchCode = branchCode;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public Boolean getIsHeadOffice() {
    return isHeadOffice;
  }

  public void setIsHeadOffice(Boolean headOffice) {
    isHeadOffice = headOffice != null ? headOffice : false;
  }

  public String getContactPersonName() {
    return contactPersonName;
  }

  public void setContactPersonName(String contactPersonName) {
    this.contactPersonName = contactPersonName;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
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
    if (!(o instanceof EmployerBranch that)) {
      return false;
    }
    return Objects.equals(employer != null ? employer.getId() : null, that.employer != null ? that.employer.getId() : null)
        && Objects.equals(branchCode, that.branchCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employer != null ? employer.getId() : null, branchCode);
  }

  @Override
  public String toString() {
    return "EmployerBranch{" +
        "id=" + id +
        ", branchCode='" + branchCode + '\'' +
        ", branchName='" + branchName + '\'' +
        ", isHeadOffice=" + isHeadOffice +
        '}';
  }
}
