package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

public class UpdateTrainingCenterRequest {

  @Size(max = 200, message = "centerName cannot exceed 200 characters")
  private String centerName;
  @Size(max = 200, message = "addressLine1 cannot exceed 200 characters")
  private String addressLine1;
  @Size(max = 200, message = "addressLine2 cannot exceed 200 characters")
  private String addressLine2;
  @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be 6 digits and cannot start with 0")
  private String pincode;
  private Long locationId;
  private Long stateId;
  private Long districtId;
  private Integer operationalCapacity;
  private Long lifecycleStatusId;

  public UpdateTrainingCenterRequest() {
  }

  public UpdateTrainingCenterRequest(
      String centerName,
      String addressLine1,
      String addressLine2,
      String pincode,
      Long locationId,
      Long stateId,
      Long districtId,
      Integer operationalCapacity,
      Long lifecycleStatusId) {
    this.centerName = centerName;
    this.addressLine1 = addressLine1;
    this.addressLine2 = addressLine2;
    this.pincode = pincode;
    this.locationId = locationId;
    this.stateId = stateId;
    this.districtId = districtId;
    this.operationalCapacity = operationalCapacity;
    this.lifecycleStatusId = lifecycleStatusId;
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
}
