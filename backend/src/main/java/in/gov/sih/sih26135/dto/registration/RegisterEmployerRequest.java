package in.gov.sih.sih26135.dto.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterEmployerRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be a valid email address")
  @Size(max = 255, message = "Email cannot exceed 255 characters")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
  private String password;

  @NotBlank(message = "Employer / Company name is required")
  @Size(max = 200, message = "Employer name cannot exceed 200 characters")
  private String employerName;

  @Size(max = 64)
  private String registrationNumber;

  @Pattern(regexp = "^$|^[A-Za-z0-9]{10,15}$", message = "GSTIN must contain 10 to 15 letters or digits")
  private String gstin;

  private Long organizationTypeId;

  private Long industryId;

  private Long sectorId;

  private Long companySizeId;

  @NotBlank(message = "Contact person name is required")
  @Size(max = 150, message = "Contact person name cannot exceed 150 characters")
  private String contactPersonName;

  @Email(message = "Contact email must be a valid email address")
  @Size(max = 255)
  private String contactEmail;

  @Size(max = 15)
  private String contactPhone;

  @NotNull(message = "State is required")
  private Long stateId;

  @NotNull(message = "District is required")
  private Long districtId;

  @Size(max = 200)
  private String addressLine1;

  @Size(max = 200)
  private String addressLine2;

  @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "Enter a valid six-digit PIN code")
  private String pincode;

  public RegisterEmployerRequest() {
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
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
}
