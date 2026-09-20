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
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "courses")
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "course_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "course_code", length = 32, nullable = false, unique = true)
  private String courseCode;

  @Column(name = "course_name", length = 200, nullable = false)
  private String courseName;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sector_id", nullable = false)
  private Sector sector;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "industry_id")
  private Industry industry;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "qualification_level_id")
  private RefQualificationLevel qualificationLevel;

  @Column(name = "duration_hours")
  private Integer durationHours;

  @Column(name = "duration_days")
  private Integer durationDays;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delivery_mode_id", nullable = false)
  private RefDeliveryMode deliveryMode;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Course() {
  }

  public Course(String courseCode, String courseName, Sector sector, RefDeliveryMode deliveryMode, Long lifecycleStatusId) {
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.sector = sector;
    this.deliveryMode = deliveryMode;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCourseCode() {
    return courseCode;
  }

  public void setCourseCode(String courseCode) {
    this.courseCode = courseCode;
  }

  public String getCourseName() {
    return courseName;
  }

  public void setCourseName(String courseName) {
    this.courseName = courseName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Sector getSector() {
    return sector;
  }

  public void setSector(Sector sector) {
    this.sector = sector;
  }

  public Industry getIndustry() {
    return industry;
  }

  public void setIndustry(Industry industry) {
    this.industry = industry;
  }

  public RefQualificationLevel getQualificationLevel() {
    return qualificationLevel;
  }

  public void setQualificationLevel(RefQualificationLevel qualificationLevel) {
    this.qualificationLevel = qualificationLevel;
  }

  public Integer getDurationHours() {
    return durationHours;
  }

  public void setDurationHours(Integer durationHours) {
    this.durationHours = durationHours;
  }

  public Integer getDurationDays() {
    return durationDays;
  }

  public void setDurationDays(Integer durationDays) {
    this.durationDays = durationDays;
  }

  public RefDeliveryMode getDeliveryMode() {
    return deliveryMode;
  }

  public void setDeliveryMode(RefDeliveryMode deliveryMode) {
    this.deliveryMode = deliveryMode;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
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

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Course other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Course{" +
        "id=" + id +
        ", courseCode='" + courseCode + '\'' +
        ", courseName='" + courseName + '\'' +
        ", lifecycleStatusId=" + lifecycleStatusId +
        '}';
  }
}
