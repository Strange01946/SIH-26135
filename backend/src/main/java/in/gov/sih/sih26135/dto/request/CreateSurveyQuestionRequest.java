package in.gov.sih.sih26135.dto.request;

public class CreateSurveyQuestionRequest {

  private Long surveyTemplateVersionId;
  private String questionCode;
  private String questionText;
  private Long questionTypeId;
  private Integer displayOrder;
  private Boolean isRequired;

  public CreateSurveyQuestionRequest() {
  }

  public CreateSurveyQuestionRequest(
      Long surveyTemplateVersionId,
      String questionCode,
      String questionText,
      Long questionTypeId,
      Integer displayOrder,
      Boolean isRequired) {
    this.surveyTemplateVersionId = surveyTemplateVersionId;
    this.questionCode = questionCode;
    this.questionText = questionText;
    this.questionTypeId = questionTypeId;
    this.displayOrder = displayOrder;
    this.isRequired = isRequired;
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
}
