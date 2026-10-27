package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateTrainingProviderRequest {

  @NotBlank(message = "providerCode is required")
  @Size(max = 32, message = "providerCode cannot exceed 32 characters")
  private String providerCode;
  @NotBlank(message = "providerName is required")
  @Size(max = 200, message = "providerName cannot exceed 200 characters")
  private String providerName;
  @NotBlank(message = "Registration number is required")
  @Size(max = 32, message = "Registration number cannot exceed 32 characters")
  private String registrationNumber;
  private Long organizationTypeId;
  private Long organizationId;
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
  private Long accreditationStatusId;
  @PositiveOrZero(message = "rating must be greater than or equal to 0")
  @Digits(integer = 1, fraction = 2, message = "rating numeric value out of bounds (<1 digits>.<2 digits> expected)")
  private BigDecimal rating;
  private Long lifecycleStatusId;

  public CreateTrainingProviderRequest() {
  }

  public CreateTrainingProviderRequest(
      String providerCode,
      String providerName,
      String registrationNumber,
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
    this.providerCode = providerCode;
    this.providerName = providerName;
    this.registrationNumber = registrationNumber;
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

  public String getProviderCode() {
    return providerCode;
  }

  public void setProviderCode(String providerCode) {
    this.providerCode = providerCode;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public void setRegistrationNumber(String registrationNumber) {
    this.registrationNumber = registrationNumber;
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
