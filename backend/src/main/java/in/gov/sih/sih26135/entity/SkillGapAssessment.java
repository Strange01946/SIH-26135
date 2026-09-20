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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "skill_gap_assessments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_skill_gap_assessments_number", columnNames = {"assessment_number"})
})
public class SkillGapAssessment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_gap_assessment_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "assessment_number", length = 32, nullable = false, unique = true)
  private String assessmentNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id")
  private TrainingEnrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id")
  private TrainingBatch batch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_role_id")
  private JobRole jobRole;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_posting_id")
  private JobPosting jobPosting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_id")
  private EmploymentRecord employmentRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_id")
  private PlacementRecord placementRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_assessment_id")
  private TraineeAssessment traineeAssessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_result_id")
  private AssessmentResult assessmentResult;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "certification_id")
  private Certification certification;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_response_id")
  private SurveyResponse surveyResponse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "followup_task_id")
  private FollowupTask followupTask;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_id")
  private EmploymentVerification employmentVerification;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_source_id", nullable = false)
  private RefSkillGapSource skillGapSource;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_assessment_status_id", nullable = false)
  private RefSkillGapAssessmentStatus skillGapAssessmentStatus;

  @Column(name = "assessed_on", nullable = false)
  private LocalDate assessedOn;

  @Column(name = "assessed_by_user_id")
  private Long assessedByUserId;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SkillGapAssessment() {
  }

  public SkillGapAssessment(String assessmentNumber, Trainee trainee,
      RefSkillGapSource skillGapSource, RefSkillGapAssessmentStatus skillGapAssessmentStatus,
      LocalDate assessedOn) {
    this.assessmentNumber = assessmentNumber;
    this.trainee = trainee;
    this.skillGapSource = skillGapSource;
    this.skillGapAssessmentStatus = skillGapAssessmentStatus;
    this.assessedOn = assessedOn;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAssessmentNumber() {
    return assessmentNumber;
  }

  public void setAssessmentNumber(String assessmentNumber) {
    this.assessmentNumber = assessmentNumber;
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

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
  }

  public TrainingBatch getBatch() {
    return batch;
  }

  public void setBatch(TrainingBatch batch) {
    this.batch = batch;
  }

  public JobRole getJobRole() {
    return jobRole;
  }

  public void setJobRole(JobRole jobRole) {
    this.jobRole = jobRole;
  }

  public JobPosting getJobPosting() {
    return jobPosting;
  }

  public void setJobPosting(JobPosting jobPosting) {
    this.jobPosting = jobPosting;
  }

  public EmploymentRecord getEmploymentRecord() {
    return employmentRecord;
  }

  public void setEmploymentRecord(EmploymentRecord employmentRecord) {
    this.employmentRecord = employmentRecord;
  }

  public PlacementRecord getPlacementRecord() {
    return placementRecord;
  }

  public void setPlacementRecord(PlacementRecord placementRecord) {
    this.placementRecord = placementRecord;
  }

  public TraineeAssessment getTraineeAssessment() {
    return traineeAssessment;
  }

  public void setTraineeAssessment(TraineeAssessment traineeAssessment) {
    this.traineeAssessment = traineeAssessment;
  }

  public AssessmentResult getAssessmentResult() {
    return assessmentResult;
  }

  public void setAssessmentResult(AssessmentResult assessmentResult) {
    this.assessmentResult = assessmentResult;
  }

  public Certification getCertification() {
    return certification;
  }

  public void setCertification(Certification certification) {
    this.certification = certification;
  }

  public SurveyResponse getSurveyResponse() {
    return surveyResponse;
  }

  public void setSurveyResponse(SurveyResponse surveyResponse) {
    this.surveyResponse = surveyResponse;
  }

  public FollowupTask getFollowupTask() {
    return followupTask;
  }

  public void setFollowupTask(FollowupTask followupTask) {
    this.followupTask = followupTask;
  }

  public EmploymentVerification getEmploymentVerification() {
    return employmentVerification;
  }

  public void setEmploymentVerification(EmploymentVerification employmentVerification) {
    this.employmentVerification = employmentVerification;
  }

  public RefSkillGapSource getSkillGapSource() {
    return skillGapSource;
  }

  public void setSkillGapSource(RefSkillGapSource skillGapSource) {
    this.skillGapSource = skillGapSource;
  }

  public RefSkillGapAssessmentStatus getSkillGapAssessmentStatus() {
    return skillGapAssessmentStatus;
  }

  public void setSkillGapAssessmentStatus(
      RefSkillGapAssessmentStatus skillGapAssessmentStatus) {
    this.skillGapAssessmentStatus = skillGapAssessmentStatus;
  }

  public LocalDate getAssessedOn() {
    return assessedOn;
  }

  public void setAssessedOn(LocalDate assessedOn) {
    this.assessedOn = assessedOn;
  }

  public Long getAssessedByUserId() {
    return assessedByUserId;
  }

  public void setAssessedByUserId(Long assessedByUserId) {
    this.assessedByUserId = assessedByUserId;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
    if (!(o instanceof SkillGapAssessment that)) {
      return false;
    }
    return Objects.equals(assessmentNumber, that.assessmentNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assessmentNumber);
  }

  @Override
  public String toString() {
    return "SkillGapAssessment{" +
        "id=" + id +
        ", assessmentNumber='" + assessmentNumber + '\'' +
        ", assessedOn=" + assessedOn +
        '}';
  }
}
