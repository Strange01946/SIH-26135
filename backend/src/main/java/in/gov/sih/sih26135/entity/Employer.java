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
@Table(name = "employers")
public class Employer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employer_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "employer_code", length = 32, nullable = false, unique = true)
  private String employerCode;

  @Column(name = "employer_name", length = 200, nullable = false)
  private String employerName;

  @Column(name = "registration_number", length = 64, unique = true)
  private String registrationNumber;

  @Column(name = "gstin", length = 15, unique = true)
  private String gstin;

  @Column(name = "organization_type_id", nullable = false)
  private Long organizationTypeId;

  @Column(name = "organization_id")
  private Long organizationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "industry_id")
  private Industry industry;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sector_id")
  private Sector sector;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_size_id")
  private RefCompanySize companySize;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "record_verification_status_id", nullable = false)
  private RefRecordVerificationStatus recordVerificationStatus;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "verified_by_user_id")
  private Long verifiedByUserId;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Employer() {
  }

  public Employer(String employerCode, String employerName, Long organizationTypeId,
      Long stateId, Long districtId, RefRecordVerificationStatus recordVerificationStatus,
      Long lifecycleStatusId) {
    this.employerCode = employerCode;
    this.employerName = employerName;
    this.organizationTypeId = organizationTypeId;
    this.stateId = stateId;
    this.districtId = districtId;
    this.recordVerificationStatus = recordVerificationStatus;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmployerCode() {
    return employerCode;
  }

  public void setEmployerCode(String employerCode) {
    this.employerCode = employerCode;
  }

  public String getEmployerName() {
    return employerName;
  }

  public void setEmployerName(String employerName) {
    this.employerName = employerName;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public void setRegistrationNumber(String registrationNumber) {
    this.registrationNumber = registrationNumber;
  }

  public String getGstin() {
    return gstin;
  }

  public void setGstin(String gstin) {
    this.gstin = gstin;
  }

  public Long getOrganizationTypeId() {
    return organizationTypeId;
  }

  public void setOrganizationTypeId(Long organizationTypeId) {
    this.organizationTypeId = organizationTypeId;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public Industry getIndustry() {
    return industry;
  }

  public void setIndustry(Industry industry) {
    this.industry = industry;
  }

  public Sector getSector() {
    return sector;
  }

  public void setSector(Sector sector) {
    this.sector = sector;
  }

  public RefCompanySize getCompanySize() {
    return companySize;
  }

  public void setCompanySize(RefCompanySize companySize) {
    this.companySize = companySize;
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

  public RefRecordVerificationStatus getRecordVerificationStatus() {
    return recordVerificationStatus;
  }

  public void setRecordVerificationStatus(RefRecordVerificationStatus recordVerificationStatus) {
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public Long getVerifiedByUserId() {
    return verifiedByUserId;
  }

  public void setVerifiedByUserId(Long verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
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
    if (!(o instanceof Employer employer)) {
      return false;
    }
    return Objects.equals(employerCode, employer.employerCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employerCode);
  }

  @Override
  public String toString() {
    return "Employer{" +
        "id=" + id +
        ", employerCode='" + employerCode + '\'' +
        ", employerName='" + employerName + '\'' +
        ", registrationNumber='" + registrationNumber + '\'' +
        ", gstin='" + gstin + '\'' +
        ", stateId=" + stateId +
        ", districtId=" + districtId +
        '}';
  }
}
