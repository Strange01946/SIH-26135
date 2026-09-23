package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class UpdateSkillGapObservationRequest {

  private Long observedSkillLevelId;
  private Long requiredSkillLevelId;
  private Long targetSkillLevelId;
  private Integer gapLevelDelta;
  private Long skillGapSeverityId;
  private Long skillGapStatusId;
  private Long skillGapSourceId;
  private LocalDate observedOn;
  private Long observedByUserId;
  private String notes;

  public UpdateSkillGapObservationRequest() {}

  public Long getObservedSkillLevelId() {
    return observedSkillLevelId;
  }

  public void setObservedSkillLevelId(Long observedSkillLevelId) {
    this.observedSkillLevelId = observedSkillLevelId;
  }

  public Long getRequiredSkillLevelId() {
    return requiredSkillLevelId;
  }

  public void setRequiredSkillLevelId(Long requiredSkillLevelId) {
    this.requiredSkillLevelId = requiredSkillLevelId;
  }

  public Long getTargetSkillLevelId() {
    return targetSkillLevelId;
  }

  public void setTargetSkillLevelId(Long targetSkillLevelId) {
    this.targetSkillLevelId = targetSkillLevelId;
  }

  public Integer getGapLevelDelta() {
    return gapLevelDelta;
  }

  public void setGapLevelDelta(Integer gapLevelDelta) {
    this.gapLevelDelta = gapLevelDelta;
  }

  public Long getSkillGapSeverityId() {
    return skillGapSeverityId;
  }

  public void setSkillGapSeverityId(Long skillGapSeverityId) {
    this.skillGapSeverityId = skillGapSeverityId;
  }

  public Long getSkillGapStatusId() {
    return skillGapStatusId;
  }

  public void setSkillGapStatusId(Long skillGapStatusId) {
    this.skillGapStatusId = skillGapStatusId;
  }

  public Long getSkillGapSourceId() {
    return skillGapSourceId;
  }

  public void setSkillGapSourceId(Long skillGapSourceId) {
    this.skillGapSourceId = skillGapSourceId;
  }

  public LocalDate getObservedOn() {
    return observedOn;
  }

  public void setObservedOn(LocalDate observedOn) {
    this.observedOn = observedOn;
  }

  public Long getObservedByUserId() {
    return observedByUserId;
  }

  public void setObservedByUserId(Long observedByUserId) {
    this.observedByUserId = observedByUserId;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
