package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ref_qualification_level")
public class RefQualificationLevel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "qualification_level_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "level_code", length = 32, nullable = false, unique = true)
  private String levelCode;

  @Column(name = "level_name", length = 100, nullable = false)
  private String levelName;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "nsqf_level")
  private Integer nsqfLevel;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefQualificationLevel() {
  }

  public RefQualificationLevel(String levelCode, String levelName, Integer nsqfLevel, Integer sortOrder) {
    this.levelCode = levelCode;
    this.levelName = levelName;
    this.nsqfLevel = nsqfLevel;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(String levelCode) {
    this.levelCode = levelCode;
  }

  public String getLevelName() {
    return levelName;
  }

  public void setLevelName(String levelName) {
    this.levelName = levelName;
  }

  public Integer getNsqfLevel() {
    return nsqfLevel;
  }

  public void setNsqfLevel(Integer nsqfLevel) {
    this.nsqfLevel = nsqfLevel;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
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
    if (!(o instanceof RefQualificationLevel other)) {
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
    return "RefQualificationLevel{" +
        "id=" + id +
        ", levelCode='" + levelCode + '\'' +
        ", levelName='" + levelName + '\'' +
        ", nsqfLevel=" + nsqfLevel +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
