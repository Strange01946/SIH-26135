package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateSurveyResponseAnswerRequest {

  private Long surveyResponseId;
  private Long surveyQuestionId;
  private Long selectedOptionId;
  private String answerText;
  private BigDecimal numericValue;
  private Boolean booleanValue;
  private LocalDate dateValue;

  public CreateSurveyResponseAnswerRequest() {
  }

  public CreateSurveyResponseAnswerRequest(Long surveyResponseId, Long surveyQuestionId) {
    this.surveyResponseId = surveyResponseId;
    this.surveyQuestionId = surveyQuestionId;
  }

  public Long getSurveyResponseId() {
    return surveyResponseId;
  }

  public void setSurveyResponseId(Long surveyResponseId) {
    this.surveyResponseId = surveyResponseId;
  }

  public Long getSurveyQuestionId() {
    return surveyQuestionId;
  }

  public void setSurveyQuestionId(Long surveyQuestionId) {
    this.surveyQuestionId = surveyQuestionId;
  }

  public Long getSelectedOptionId() {
    return selectedOptionId;
  }

  public void setSelectedOptionId(Long selectedOptionId) {
    this.selectedOptionId = selectedOptionId;
  }

  public String getAnswerText() {
    return answerText;
  }

  public void setAnswerText(String answerText) {
    this.answerText = answerText;
  }

  public BigDecimal getNumericValue() {
    return numericValue;
  }

  public void setNumericValue(BigDecimal numericValue) {
    this.numericValue = numericValue;
  }

  public Boolean getBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(Boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public LocalDate getDateValue() {
    return dateValue;
  }

  public void setDateValue(LocalDate dateValue) {
    this.dateValue = dateValue;
  }
}
