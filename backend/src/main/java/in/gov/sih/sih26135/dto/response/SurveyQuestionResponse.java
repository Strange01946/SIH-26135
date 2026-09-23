package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class SurveyQuestionResponse {

  private Long id;
  private Long surveyTemplateVersionId;
  private String questionCode;
  private String questionText;
  private Long questionTypeId;
  private Integer displayOrder;
  private Boolean isRequired;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public SurveyQuestionResponse() {
  }

  public SurveyQuestionResponse(
      Long id,
      Long surveyTemplateVersionId,
      String questionCode,
      String questionText,
      Long questionTypeId,
      Integer displayOrder,
      Boolean isRequired,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.surveyTemplateVersionId = surveyTemplateVersionId;
    this.questionCode = questionCode;
    this.questionText = questionText;
    this.questionTypeId = questionTypeId;
    this.displayOrder = displayOrder;
    this.isRequired = isRequired;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSurveyTemplateVersionId() {
    return surveyTemplateVersionId;
  }

  public void setSurveyTemplateVersionId(Long surveyTemplateVersionId) {
    this.surveyTemplateVersionId = surveyTemplateVersionId;
  }

  public String getQuestionCode() {
    return questionCode;
  }

  public void setQuestionCode(String questionCode) {
    this.questionCode = questionCode;
  }

  public String getQuestionText() {
    return questionText;
  }

  public void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  public Long getQuestionTypeId() {
    return questionTypeId;
  }

  public void setQuestionTypeId(Long questionTypeId) {
    this.questionTypeId = questionTypeId;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
  }

  public Boolean getIsRequired() {
    return isRequired;
  }

  public void setIsRequired(Boolean required) {
    isRequired = required;
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
}
