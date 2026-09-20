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
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "survey_responses", uniqueConstraints = {
    @UniqueConstraint(name = "uk_survey_responses_attempt", columnNames = {"survey_id", "trainee_id", "attempt_number"})
})
public class SurveyResponse {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "survey_response_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "survey_id", nullable = false)
  private Long surveyId;

  @Column(name = "survey_template_version_id", nullable = false)
  private Long surveyTemplateVersionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id")
  private TrainingEnrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "followup_task_id")
  private FollowupTask followupTask;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "attempt_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer attemptNumber = 1;

  @Column(name = "survey_response_status_id", nullable = false)
  private Long surveyResponseStatusId;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SurveyResponse() {
  }

  public SurveyResponse(Long surveyId, Long surveyTemplateVersionId, Trainee trainee,
      Integer attemptNumber, Long surveyResponseStatusId, LocalDateTime startedAt) {
    this.surveyId = surveyId;
    this.surveyTemplateVersionId = surveyTemplateVersionId;
    this.trainee = trainee;
    this.attemptNumber = attemptNumber != null ? attemptNumber : 1;
    this.surveyResponseStatusId = surveyResponseStatusId;
    this.startedAt = startedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSurveyId() {
    return surveyId;
  }

  public void setSurveyId(Long surveyId) {
    this.surveyId = surveyId;
  }

  public Long getSurveyTemplateVersionId() {
    return surveyTemplateVersionId;
  }

  public void setSurveyTemplateVersionId(Long surveyTemplateVersionId) {
    this.surveyTemplateVersionId = surveyTemplateVersionId;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public TrainingEnrollment getEnrollment() {
    return enrollment;
  }

  public void setEnrollment(TrainingEnrollment enrollment) {
    this.enrollment = enrollment;
  }

  public FollowupTask getFollowupTask() {
    return followupTask;
  }

  public void setFollowupTask(FollowupTask followupTask) {
    this.followupTask = followupTask;
  }

  public Integer getAttemptNumber() {
    return attemptNumber;
  }

  public void setAttemptNumber(Integer attemptNumber) {
    this.attemptNumber = attemptNumber != null ? attemptNumber : 1;
  }

  public Long getSurveyResponseStatusId() {
    return surveyResponseStatusId;
  }

  public void setSurveyResponseStatusId(Long surveyResponseStatusId) {
    this.surveyResponseStatusId = surveyResponseStatusId;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
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
    if (!(o instanceof SurveyResponse that)) {
      return false;
    }
    return Objects.equals(surveyId, that.surveyId)
        && Objects.equals(trainee != null ? trainee.getId() : null, that.trainee != null ? that.trainee.getId() : null)
        && Objects.equals(attemptNumber, that.attemptNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(surveyId, trainee != null ? trainee.getId() : null, attemptNumber);
  }

  @Override
  public String toString() {
    return "SurveyResponse{" +
        "id=" + id +
        ", surveyId=" + surveyId +
        ", attemptNumber=" + attemptNumber +
        ", startedAt=" + startedAt +
        '}';
  }
}
