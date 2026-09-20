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
@Table(name = "skill_levels")
public class SkillLevel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_level_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "level_code", length = 32, nullable = false, unique = true)
  private String levelCode;

  @Column(name = "level_name", length = 100, nullable = false)
  private String levelName;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "level_rank", nullable = false, unique = true)
  private Integer levelRank;

  @Column(name = "description", length = 500)
  private String description;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SkillLevel() {
  }

  public SkillLevel(String levelCode, String levelName, Integer levelRank, Integer sortOrder) {
    this.levelCode = levelCode;
    this.levelName = levelName;
    this.levelRank = levelRank;
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

  public Integer getLevelRank() {
    return levelRank;
  }

  public void setLevelRank(Integer levelRank) {
    this.levelRank = levelRank;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
    if (!(o instanceof SkillLevel other)) {
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
    return "SkillLevel{" +
        "id=" + id +
        ", levelCode='" + levelCode + '\'' +
        ", levelName='" + levelName + '\'' +
        ", levelRank=" + levelRank +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
