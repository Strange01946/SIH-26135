package in.gov.sih.sih26135.dto.request;
import in.gov.sih.sih26135.validation.ValidDateOfBirth;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class CreateTraineeRequest {

  private Long userId;
  @NotBlank(message = "Registration number is required")
  @Size(max = 32, message = "Registration number cannot exceed 32 characters")
  private String registrationNumber;
  @NotBlank(message = "firstName is required")
  @Size(max = 80, message = "firstName cannot exceed 80 characters")
  private String firstName;
  @Size(max = 80, message = "middleName cannot exceed 80 characters")
  private String middleName;
  @Size(max = 80, message = "lastName cannot exceed 80 characters")
  private String lastName;
  @ValidDateOfBirth
  private LocalDate dateOfBirth;
  private Long genderId;
  @Email(message = "Email must be a valid email address")
  @Size(max = 255, message = "Email cannot exceed 255 characters")
  private String email;
  @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
  private String phone;
  @Size(max = 200, message = "addressLine1 cannot exceed 200 characters")
  private String addressLine1;
  @Size(max = 200, message = "addressLine2 cannot exceed 200 characters")
  private String addressLine2;
  @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be 6 digits and cannot start with 0")
  private String pincode;
  private Long locationId;
  private Long stateId;
  private Long districtId;
  private Long blockId;
  private Long educationLevelId;
  private Long currentEmploymentStatusId;
  private Long profileStatusId;

  public CreateTraineeRequest() {
  }

  public CreateTraineeRequest(
      Long userId,
      String registrationNumber,
      String firstName,
      String middleName,
      String lastName,
      LocalDate dateOfBirth,
      Long genderId,
      String email,
      String phone,
      String addressLine1,
      String addressLine2,
      String pincode,
      Long locationId,
      Long stateId,
      Long districtId,
      Long blockId,
      Long educationLevelId,
      Long currentEmploymentStatusId,
      Long profileStatusId) {
    this.userId = userId;
    this.registrationNumber = registrationNumber;
    this.firstName = firstName;
    this.middleName = middleName;
    this.lastName = lastName;
    this.dateOfBirth = dateOfBirth;
    this.genderId = genderId;
    this.email = email;
    this.phone = phone;
    this.addressLine1 = addressLine1;
    this.addressLine2 = addressLine2;
    this.pincode = pincode;
    this.locationId = locationId;
    this.stateId = stateId;
    this.districtId = districtId;
    this.blockId = blockId;
    this.educationLevelId = educationLevelId;
    this.currentEmploymentStatusId = currentEmploymentStatusId;
    this.profileStatusId = profileStatusId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public void setRegistrationNumber(String registrationNumber) {
    this.registrationNumber = registrationNumber;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public void setMiddleName(String middleName) {
    this.middleName = middleName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public Long getGenderId() {
    return genderId;
  }

  public void setGenderId(Long genderId) {
    this.genderId = genderId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
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

  public Long getBlockId() {
    return blockId;
  }

  public void setBlockId(Long blockId) {
    this.blockId = blockId;
  }

  public Long getEducationLevelId() {
    return educationLevelId;
  }

  public void setEducationLevelId(Long educationLevelId) {
    this.educationLevelId = educationLevelId;
  }

  public Long getCurrentEmploymentStatusId() {
    return currentEmploymentStatusId;
  }

  public void setCurrentEmploymentStatusId(Long currentEmploymentStatusId) {
    this.currentEmploymentStatusId = currentEmploymentStatusId;
  }

  public Long getProfileStatusId() {
    return profileStatusId;
  }

  public void setProfileStatusId(Long profileStatusId) {
    this.profileStatusId = profileStatusId;
  }
}
