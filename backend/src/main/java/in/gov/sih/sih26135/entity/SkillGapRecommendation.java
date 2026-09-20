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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "skill_gap_recommendations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sgr_number", columnNames = {
        "skill_gap_id", "recommendation_number"
    }),
    @UniqueConstraint(name = "uk_sgr_gap_action_course", columnNames = {
        "skill_gap_id", "skill_gap_action_type_id", "recommended_course_id"
    })
})
public class SkillGapRecommendation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_gap_recommendation_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_id", nullable = false)
  private SkillGap skillGap;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "recommendation_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer recommendationNumber = 1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_action_type_id", nullable = false)
  private RefSkillGapActionType skillGapActionType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recommended_course_id")
  private Course recommendedCourse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recommended_skill_id")
  private Skill recommendedSkill;

  @Column(name = "is_accepted_flag", nullable = false)
  private Boolean isAcceptedFlag = false;

  @Column(name = "accepted_on")
  private LocalDate acceptedOn;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "created_by_user_id")
  private Long createdByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SkillGapRecommendation() {
  }

  public SkillGapRecommendation(SkillGap skillGap, Integer recommendationNumber,
      RefSkillGapActionType skillGapActionType) {
    this.skillGap = skillGap;
    this.recommendationNumber = recommendationNumber;
    this.skillGapActionType = skillGapActionType;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public SkillGap getSkillGap() {
    return skillGap;
  }

  public void setSkillGap(SkillGap skillGap) {
    this.skillGap = skillGap;
  }

  public Integer getRecommendationNumber() {
    return recommendationNumber;
  }

  public void setRecommendationNumber(Integer recommendationNumber) {
    this.recommendationNumber = recommendationNumber;
  }

  public RefSkillGapActionType getSkillGapActionType() {
    return skillGapActionType;
  }

  public void setSkillGapActionType(RefSkillGapActionType skillGapActionType) {
    this.skillGapActionType = skillGapActionType;
  }

  public Course getRecommendedCourse() {
    return recommendedCourse;
  }

  public void setRecommendedCourse(Course recommendedCourse) {
    this.recommendedCourse = recommendedCourse;
  }

  public Skill getRecommendedSkill() {
    return recommendedSkill;
  }

  public void setRecommendedSkill(Skill recommendedSkill) {
    this.recommendedSkill = recommendedSkill;
  }

  public Boolean getIsAcceptedFlag() {
    return isAcceptedFlag;
  }

  public void setIsAcceptedFlag(Boolean isAcceptedFlag) {
    this.isAcceptedFlag = isAcceptedFlag;
  }

  public LocalDate getAcceptedOn() {
    return acceptedOn;
  }

  public void setAcceptedOn(LocalDate acceptedOn) {
    this.acceptedOn = acceptedOn;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public Long getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(Long createdByUserId) {
    this.createdByUserId = createdByUserId;
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
    if (!(o instanceof SkillGapRecommendation that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "SkillGapRecommendation{" +
        "id=" + id +
        ", recommendationNumber=" + recommendationNumber +
        ", isAcceptedFlag=" + isAcceptedFlag +
        ", acceptedOn=" + acceptedOn +
        '}';
  }
}
