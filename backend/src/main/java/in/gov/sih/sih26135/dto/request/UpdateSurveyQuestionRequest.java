package in.gov.sih.sih26135.dto.request;

public class UpdateSurveyQuestionRequest {

  private String questionText;
  private Long questionTypeId;
  private Integer displayOrder;
  private Boolean isRequired;

  public UpdateSurveyQuestionRequest() {
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
