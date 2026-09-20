package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "survey_questions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_survey_questions_version_code", columnNames = {"survey_template_version_id", "question_code"}),
    @UniqueConstraint(name = "uk_survey_questions_version_order", columnNames = {"survey_template_version_id", "display_order"})
})
public class SurveyQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "survey_question_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "survey_template_version_id", nullable = false)
  private Long surveyTemplateVersionId;

  @Column(name = "question_code", length = 32, nullable = false)
  private String questionCode;

  @Column(name = "question_text", length = 500, nullable = false)
  private String questionText;

  @Column(name = "question_type_id", nullable = false)
  private Long questionTypeId;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "display_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer displayOrder;

  @Column(name = "is_required", nullable = false)
  private Boolean isRequired = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SurveyQuestion() {
  }

  public SurveyQuestion(Long surveyTemplateVersionId, String questionCode, String questionText,
      Long questionTypeId, Integer displayOrder, Boolean isRequired) {
    this.surveyTemplateVersionId = surveyTemplateVersionId;
    this.questionCode = questionCode;
    this.questionText = questionText;
    this.questionTypeId = questionTypeId;
    this.displayOrder = displayOrder;
    this.isRequired = isRequired != null ? isRequired : true;
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
    isRequired = required != null ? required : true;
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
    if (!(o instanceof SurveyQuestion that)) {
      return false;
    }
    return Objects.equals(surveyTemplateVersionId, that.surveyTemplateVersionId)
        && Objects.equals(questionCode, that.questionCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(surveyTemplateVersionId, questionCode);
  }

  @Override
  public String toString() {
    return "SurveyQuestion{" +
        "id=" + id +
        ", questionCode='" + questionCode + '\'' +
        ", displayOrder=" + displayOrder +
        ", isRequired=" + isRequired +
        '}';
  }
}
