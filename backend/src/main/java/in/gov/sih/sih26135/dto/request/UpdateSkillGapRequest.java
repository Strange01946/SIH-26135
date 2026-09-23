package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class UpdateSkillGapRequest {

  private Long observedSkillLevelId;
  private Long requiredSkillLevelId;
  private Long targetSkillLevelId;
  private Integer gapLevelDelta;
  private Long skillImportanceId;
  private Long skillGapSeverityId;
  private Long skillGapStatusId;
  private Long skillGapSourceId;
  private Boolean isCurrent;
  private LocalDate identifiedOn;
  private LocalDate resolvedOn;
  private String notes;

  public UpdateSkillGapRequest() {}

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

  public Long getSkillImportanceId() {
    return skillImportanceId;
  }

  public void setSkillImportanceId(Long skillImportanceId) {
    this.skillImportanceId = skillImportanceId;
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

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean current) {
    isCurrent = current;
  }

  public LocalDate getIdentifiedOn() {
    return identifiedOn;
  }

  public void setIdentifiedOn(LocalDate identifiedOn) {
    this.identifiedOn = identifiedOn;
  }

  public LocalDate getResolvedOn() {
    return resolvedOn;
  }

  public void setResolvedOn(LocalDate resolvedOn) {
    this.resolvedOn = resolvedOn;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
