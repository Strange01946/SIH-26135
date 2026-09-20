package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vw_skill_gap_fact")
@Immutable
public class SkillGapFact {

  @Id
  @Column(name = "skill_gap_id", nullable = false)
  private Long skillGapId;

  @Column(name = "skill_gap_number", length = 32, nullable = false)
  private String skillGapNumber;

  @Column(name = "skill_gap_assessment_id", nullable = false)
  private Long skillGapAssessmentId;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "skill_id", nullable = false)
  private Long skillId;

  @Column(name = "skill_code", length = 32, nullable = false)
  private String skillCode;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "course_id")
  private Long courseId;

  @Column(name = "job_role_id")
  private Long jobRoleId;

  @Column(name = "employment_id")
  private Long employmentId;

  @Column(name = "placement_id")
  private Long placementId;

  @Column(name = "observed_skill_level_id", nullable = false)
  private Long observedSkillLevelId;

  @Column(name = "required_skill_level_id", nullable = false)
  private Long requiredSkillLevelId;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "gap_level_delta", nullable = false, columnDefinition = "TINYINT")
  private Integer gapLevelDelta;

  @Column(name = "skill_gap_severity_id", nullable = false)
  private Long skillGapSeverityId;

  @Column(name = "severity_code", length = 32, nullable = false)
  private String severityCode;

  @Column(name = "skill_gap_status_id", nullable = false)
  private Long skillGapStatusId;

  @Column(name = "skill_gap_status_code", length = 32, nullable = false)
  private String skillGapStatusCode;

  @Column(name = "skill_gap_source_id", nullable = false)
  private Long skillGapSourceId;

  @Column(name = "skill_gap_source_code", length = 32, nullable = false)
  private String skillGapSourceCode;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent;

  @Column(name = "identified_on", nullable = false)
  private LocalDate identifiedOn;

  @Column(name = "resolved_on")
  private LocalDate resolvedOn;

  @Column(name = "trainee_state_id", nullable = false)
  private Long traineeStateId;

  @Column(name = "trainee_district_id", nullable = false)
  private Long traineeDistrictId;

  @Column(name = "recommendation_count")
  private Long recommendationCount;

  @Column(name = "accepted_recommendation_count")
  private BigDecimal acceptedRecommendationCount;

  public SkillGapFact() {
  }

  public Long getSkillGapId() {
    return skillGapId;
  }

  public void setSkillGapId(Long skillGapId) {
    this.skillGapId = skillGapId;
  }

  public String getSkillGapNumber() {
    return skillGapNumber;
  }

  public void setSkillGapNumber(String skillGapNumber) {
    this.skillGapNumber = skillGapNumber;
  }

  public Long getSkillGapAssessmentId() {
    return skillGapAssessmentId;
  }

  public void setSkillGapAssessmentId(Long skillGapAssessmentId) {
    this.skillGapAssessmentId = skillGapAssessmentId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  public String getSkillCode() {
    return skillCode;
  }

  public void setSkillCode(String skillCode) {
    this.skillCode = skillCode;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getJobRoleId() {
    return jobRoleId;
  }

  public void setJobRoleId(Long jobRoleId) {
    this.jobRoleId = jobRoleId;
  }

  public Long getEmploymentId() {
    return employmentId;
  }

  public void setEmploymentId(Long employmentId) {
    this.employmentId = employmentId;
  }

  public Long getPlacementId() {
    return placementId;
  }

  public void setPlacementId(Long placementId) {
    this.placementId = placementId;
  }

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

  public Integer getGapLevelDelta() {
    return gapLevelDelta;
  }

  public void setGapLevelDelta(Integer gapLevelDelta) {
    this.gapLevelDelta = gapLevelDelta;
  }

  public Long getSkillGapSeverityId() {
    return skillGapSeverityId;
  }

  public void setSkillGapSeverityId(Long skillGapSeverityId) {
    this.skillGapSeverityId = skillGapSeverityId;
  }

  public String getSeverityCode() {
    return severityCode;
  }

  public void setSeverityCode(String severityCode) {
    this.severityCode = severityCode;
  }

  public Long getSkillGapStatusId() {
    return skillGapStatusId;
  }

  public void setSkillGapStatusId(Long skillGapStatusId) {
    this.skillGapStatusId = skillGapStatusId;
  }

  public String getSkillGapStatusCode() {
    return skillGapStatusCode;
  }

  public void setSkillGapStatusCode(String skillGapStatusCode) {
    this.skillGapStatusCode = skillGapStatusCode;
  }

  public Long getSkillGapSourceId() {
    return skillGapSourceId;
  }

  public void setSkillGapSourceId(Long skillGapSourceId) {
    this.skillGapSourceId = skillGapSourceId;
  }

  public String getSkillGapSourceCode() {
    return skillGapSourceCode;
  }

  public void setSkillGapSourceCode(String skillGapSourceCode) {
    this.skillGapSourceCode = skillGapSourceCode;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
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

  public Long getTraineeStateId() {
    return traineeStateId;
  }

  public void setTraineeStateId(Long traineeStateId) {
    this.traineeStateId = traineeStateId;
  }

  public Long getTraineeDistrictId() {
    return traineeDistrictId;
  }

  public void setTraineeDistrictId(Long traineeDistrictId) {
    this.traineeDistrictId = traineeDistrictId;
  }

  public Long getRecommendationCount() {
    return recommendationCount;
  }

  public void setRecommendationCount(Long recommendationCount) {
    this.recommendationCount = recommendationCount;
  }

  public BigDecimal getAcceptedRecommendationCount() {
    return acceptedRecommendationCount;
  }

  public void setAcceptedRecommendationCount(BigDecimal acceptedRecommendationCount) {
    this.acceptedRecommendationCount = acceptedRecommendationCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SkillGapFact that)) {
      return false;
    }
    return Objects.equals(skillGapId, that.skillGapId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(skillGapId);
  }
}
