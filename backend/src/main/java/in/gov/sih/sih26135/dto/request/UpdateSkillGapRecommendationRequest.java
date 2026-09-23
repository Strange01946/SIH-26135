package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class UpdateSkillGapRecommendationRequest {

  private Long skillGapActionTypeId;
  private Long recommendedCourseId;
  private Long recommendedSkillId;
  private Boolean isAcceptedFlag;
  private LocalDate acceptedOn;
  private String remarks;
  private Long createdByUserId;

  public UpdateSkillGapRecommendationRequest() {}

  public Long getSkillGapActionTypeId() {
    return skillGapActionTypeId;
  }

  public void setSkillGapActionTypeId(Long skillGapActionTypeId) {
    this.skillGapActionTypeId = skillGapActionTypeId;
  }

  public Long getRecommendedCourseId() {
    return recommendedCourseId;
  }

  public void setRecommendedCourseId(Long recommendedCourseId) {
    this.recommendedCourseId = recommendedCourseId;
  }

  public Long getRecommendedSkillId() {
    return recommendedSkillId;
  }

  public void setRecommendedSkillId(Long recommendedSkillId) {
    this.recommendedSkillId = recommendedSkillId;
  }

  public Boolean getIsAcceptedFlag() {
    return isAcceptedFlag;
  }

  public void setIsAcceptedFlag(Boolean acceptedFlag) {
    isAcceptedFlag = acceptedFlag;
  }

  public LocalDate getAcceptedOn() {
    return acceptedOn;
  }

  public void setAcceptedOn(LocalDate acceptedOn) {
    this.acceptedOn = acceptedOn;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public Long getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(Long createdByUserId) {
    this.createdByUserId = createdByUserId;
  }
}
