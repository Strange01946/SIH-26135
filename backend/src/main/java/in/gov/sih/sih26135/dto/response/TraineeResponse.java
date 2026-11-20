package in.gov.sih.sih26135.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Trainee profile response payload")
public class TraineeResponse {

  @Schema(description = "Trainee primary key identifier", example = "1005143")
  private Long id;

  @Schema(description = "Associated user account ID", example = "1001")
  private Long userId;

  @Schema(description = "Unique alphanumeric registration number", example = "TRN-2026-0001")
  private String registrationNumber;

  @Schema(description = "First name", example = "Rajesh")
  private String firstName;

  @Schema(description = "Middle name", example = "Kumar")
  private String middleName;

  @Schema(description = "Last name", example = "Sharma")
  private String lastName;

  @Schema(description = "Date of birth (ISO-8601)", example = "2000-01-15")
  private LocalDate dateOfBirth;

  @Schema(description = "Gender lookup ID", example = "1")
  private Long genderId;

  @Schema(description = "Contact email", example = "trainee@example.org")
  private String email;

  @Schema(description = "Contact phone number", example = "9876543210")
  private String phone;

  @Schema(description = "Primary street address", example = "123 Main Street")
  private String addressLine1;

  @Schema(description = "Secondary address details", example = "Sector 4")
  private String addressLine2;

  @Schema(description = "Postal pincode", example = "110001")
  private String pincode;

  @Schema(description = "Location ID", example = "1")
  private Long locationId;

  @Schema(description = "State jurisdiction ID", example = "10")
  private Long stateId;

  @Schema(description = "District jurisdiction ID", example = "101")
  private Long districtId;

  @Schema(description = "Block ID", example = "1001")
  private Long blockId;

  @Schema(description = "Education level ID", example = "2")
  private Long educationLevelId;

  @Schema(description = "Employment status ID", example = "1")
  private Long currentEmploymentStatusId;

  @Schema(description = "Profile status ID", example = "1")
  private Long profileStatusId;

  @Schema(description = "Record creation timestamp", example = "2026-01-10T10:00:00")
  private LocalDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2026-02-15T12:30:00")
  private LocalDateTime updatedAt;

  @Schema(description = "Soft deletion timestamp if deleted", example = "null")
  private LocalDateTime deletedAt;

  public TraineeResponse() {
  }

  public TraineeResponse(
      Long id,
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
      Long profileStatusId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
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
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
}
