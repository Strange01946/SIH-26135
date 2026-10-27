package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class UpdateEmployerRequest {

  @Size(max = 200, message = "employerName cannot exceed 200 characters")
  private String employerName;
  @Size(max = 32, message = "Registration number cannot exceed 32 characters")
  private String registrationNumber;
  @Size(max = 15, message = "gstin cannot exceed 15 characters")
  private String gstin;
  private Long organizationTypeId;
  private Long organizationId;
  private Long industryId;
  private Long sectorId;
  private Long companySizeId;
  @Size(max = 150, message = "contactPersonName cannot exceed 150 characters")
  private String contactPersonName;
  @Email(message = "Email must be a valid email address")
  @Size(max = 255, message = "Email cannot exceed 255 characters")
  private String contactEmail;
  @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
  private String contactPhone;
  @Size(max = 200, message = "addressLine1 cannot exceed 200 characters")
  private String addressLine1;
  @Size(max = 200, message = "addressLine2 cannot exceed 200 characters")
  private String addressLine2;
  @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be 6 digits and cannot start with 0")
  private String pincode;
  private Long locationId;
  private Long stateId;
  private Long districtId;
  private Long recordVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;
  private Long lifecycleStatusId;

  public UpdateEmployerRequest() {
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

  public Long getIndustryId() {
    return industryId;
  }

  public void setIndustryId(Long industryId) {
    this.industryId = industryId;
  }

  public Long getSectorId() {
    return sectorId;
  }

  public void setSectorId(Long sectorId) {
    this.sectorId = sectorId;
  }

  public Long getCompanySizeId() {
    return companySizeId;
  }

  public void setCompanySizeId(Long companySizeId) {
    this.companySizeId = companySizeId;
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

  public Long getRecordVerificationStatusId() {
    return recordVerificationStatusId;
  }

  public void setRecordVerificationStatusId(Long recordVerificationStatusId) {
    this.recordVerificationStatusId = recordVerificationStatusId;
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
}
