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
@Table(name = "ref_skill_gap_severity", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ref_skill_gap_severity_code", columnNames = {"severity_code"}),
    @UniqueConstraint(name = "uk_ref_skill_gap_severity_rank", columnNames = {"severity_rank"})
})
public class RefSkillGapSeverity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_gap_severity_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "severity_code", length = 32, nullable = false, unique = true)
  private String severityCode;

  @Column(name = "severity_name", length = 100, nullable = false)
  private String severityName;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "severity_rank", nullable = false, unique = true)
  private Integer severityRank;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefSkillGapSeverity() {
  }

  public RefSkillGapSeverity(String severityCode, String severityName, Integer severityRank,
      Integer sortOrder) {
    this.severityCode = severityCode;
    this.severityName = severityName;
    this.severityRank = severityRank;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSeverityCode() {
    return severityCode;
  }

  public void setSeverityCode(String severityCode) {
    this.severityCode = severityCode;
  }

  public String getSeverityName() {
    return severityName;
  }

  public void setSeverityName(String severityName) {
    this.severityName = severityName;
  }

  public Integer getSeverityRank() {
    return severityRank;
  }

  public void setSeverityRank(Integer severityRank) {
    this.severityRank = severityRank;
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
    if (!(o instanceof RefSkillGapSeverity that)) {
      return false;
    }
    return Objects.equals(severityCode, that.severityCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(severityCode);
  }

  @Override
  public String toString() {
    return "RefSkillGapSeverity{" +
        "id=" + id +
        ", severityCode='" + severityCode + '\'' +
        ", severityName='" + severityName + '\'' +
        ", severityRank=" + severityRank +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
