package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SurveyResponseAnswerResponse {

  private Long id;
  private Long surveyResponseId;
  private Long surveyQuestionId;
  private String questionCode;
  private String questionText;
  private Long questionTypeId;
  private Long selectedOptionId;
  private String answerText;
  private BigDecimal numericValue;
  private Boolean booleanValue;
  private LocalDate dateValue;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public SurveyResponseAnswerResponse() {
  }

  public SurveyResponseAnswerResponse(
      Long id,
      Long surveyResponseId,
      Long surveyQuestionId,
      String questionCode,
      String questionText,
      Long questionTypeId,
      Long selectedOptionId,
      String answerText,
      BigDecimal numericValue,
      Boolean booleanValue,
      LocalDate dateValue,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.surveyResponseId = surveyResponseId;
    this.surveyQuestionId = surveyQuestionId;
    this.questionCode = questionCode;
    this.questionText = questionText;
    this.questionTypeId = questionTypeId;
    this.selectedOptionId = selectedOptionId;
    this.answerText = answerText;
    this.numericValue = numericValue;
    this.booleanValue = booleanValue;
    this.dateValue = dateValue;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
