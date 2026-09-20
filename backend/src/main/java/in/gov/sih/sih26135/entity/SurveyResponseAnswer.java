package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "survey_response_answers", uniqueConstraints = {
    @UniqueConstraint(name = "uk_survey_response_answers_question", columnNames = {"survey_response_id", "survey_question_id"})
})
public class SurveyResponseAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "survey_response_answer_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_response_id", nullable = false)
  private SurveyResponse surveyResponse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_question_id", nullable = false)
  private SurveyQuestion surveyQuestion;

  @Column(name = "selected_option_id")
  private Long selectedOptionId;

  @Column(name = "answer_text", length = 1000)
  private String answerText;

  @Column(name = "numeric_value", precision = 12, scale = 2)
  private BigDecimal numericValue;

  @Column(name = "boolean_value")
  private Boolean booleanValue;

  @Column(name = "date_value")
  private LocalDate dateValue;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SurveyResponseAnswer() {
  }

  public SurveyResponseAnswer(SurveyResponse surveyResponse, SurveyQuestion surveyQuestion) {
    this.surveyResponse = surveyResponse;
    this.surveyQuestion = surveyQuestion;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public SurveyResponse getSurveyResponse() {
    return surveyResponse;
  }

  public void setSurveyResponse(SurveyResponse surveyResponse) {
    this.surveyResponse = surveyResponse;
  }

  public SurveyQuestion getSurveyQuestion() {
    return surveyQuestion;
  }

  public void setSurveyQuestion(SurveyQuestion surveyQuestion) {
    this.surveyQuestion = surveyQuestion;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SurveyResponseAnswer that)) {
      return false;
    }
    return Objects.equals(surveyResponse != null ? surveyResponse.getId() : null, that.surveyResponse != null ? that.surveyResponse.getId() : null)
        && Objects.equals(surveyQuestion != null ? surveyQuestion.getId() : null, that.surveyQuestion != null ? that.surveyQuestion.getId() : null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(surveyResponse != null ? surveyResponse.getId() : null,
        surveyQuestion != null ? surveyQuestion.getId() : null);
  }

  @Override
  public String toString() {
    return "SurveyResponseAnswer{" +
        "id=" + id +
        ", selectedOptionId=" + selectedOptionId +
        '}';
  }
}
