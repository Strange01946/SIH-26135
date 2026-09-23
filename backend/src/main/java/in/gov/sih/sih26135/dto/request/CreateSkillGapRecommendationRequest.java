package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class CreateSkillGapRecommendationRequest {

  private Long skillGapId;
  private Integer recommendationNumber;
  private Long skillGapActionTypeId;
  private Long recommendedCourseId;
  private Long recommendedSkillId;
  private Boolean isAcceptedFlag = false;
  private LocalDate acceptedOn;
  private String remarks;
  private Long createdByUserId;

  public CreateSkillGapRecommendationRequest() {}

  public CreateSkillGapRecommendationRequest(
      Long skillGapId,
      Long skillGapActionTypeId) {
    this.skillGapId = skillGapId;
    this.skillGapActionTypeId = skillGapActionTypeId;
  }

  public Long getSkillGapId() {
    return skillGapId;
  }

  public void setSkillGapId(Long skillGapId) {
    this.skillGapId = skillGapId;
  }

  public Integer getRecommendationNumber() {
    return recommendationNumber;
  }

  public void setRecommendationNumber(Integer recommendationNumber) {
    this.recommendationNumber = recommendationNumber;
  }

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
