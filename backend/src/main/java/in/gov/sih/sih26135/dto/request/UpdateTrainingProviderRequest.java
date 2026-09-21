package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;

public class UpdateTrainingProviderRequest {

  private String providerName;
  private Long organizationTypeId;
  private Long organizationId;
  private String contactPersonName;
  private String contactEmail;
  private String contactPhone;
  private String addressLine1;
  private String addressLine2;
  private String pincode;
  private Long locationId;
  private Long stateId;
  private Long districtId;
  private Long accreditationStatusId;
  private BigDecimal rating;
  private Long lifecycleStatusId;

  public UpdateTrainingProviderRequest() {
  }

  public UpdateTrainingProviderRequest(
      String providerName,
      Long organizationTypeId,
      Long organizationId,
      String contactPersonName,
      String contactEmail,
      String contactPhone,
      String addressLine1,
      String addressLine2,
      String pincode,
      Long locationId,
      Long stateId,
      Long districtId,
      Long accreditationStatusId,
      BigDecimal rating,
      Long lifecycleStatusId) {
    this.providerName = providerName;
    this.organizationTypeId = organizationTypeId;
    this.organizationId = organizationId;
    this.contactPersonName = contactPersonName;
    this.contactEmail = contactEmail;
    this.contactPhone = contactPhone;
    this.addressLine1 = addressLine1;
    this.addressLine2 = addressLine2;
    this.pincode = pincode;
    this.locationId = locationId;
    this.stateId = stateId;
    this.districtId = districtId;
    this.accreditationStatusId = accreditationStatusId;
    this.rating = rating;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
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

  public Long getAccreditationStatusId() {
    return accreditationStatusId;
  }

  public void setAccreditationStatusId(Long accreditationStatusId) {
    this.accreditationStatusId = accreditationStatusId;
  }

  public BigDecimal getRating() {
    return rating;
  }

  public void setRating(BigDecimal rating) {
    this.rating = rating;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
