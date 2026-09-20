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
@Table(name = "ref_separation_nature", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ref_separation_nature_code", columnNames = {"nature_code"})
})
public class RefSeparationNature {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "separation_nature_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "nature_code", length = 32, nullable = false, unique = true)
  private String natureCode;

  @Column(name = "nature_name", length = 100, nullable = false)
  private String natureName;

  @Column(name = "is_voluntary_flag", nullable = false)
  private Boolean isVoluntaryFlag = false;

  @Column(name = "is_involuntary_flag", nullable = false)
  private Boolean isInvoluntaryFlag = false;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefSeparationNature() {
  }

  public RefSeparationNature(String natureCode, String natureName, Boolean isVoluntaryFlag,
      Boolean isInvoluntaryFlag, Integer sortOrder) {
    this.natureCode = natureCode;
    this.natureName = natureName;
    this.isVoluntaryFlag = isVoluntaryFlag;
    this.isInvoluntaryFlag = isInvoluntaryFlag;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getNatureCode() {
    return natureCode;
  }

  public void setNatureCode(String natureCode) {
    this.natureCode = natureCode;
  }

  public String getNatureName() {
    return natureName;
  }

  public void setNatureName(String natureName) {
    this.natureName = natureName;
  }

  public Boolean getIsVoluntaryFlag() {
    return isVoluntaryFlag;
  }

  public void setIsVoluntaryFlag(Boolean isVoluntaryFlag) {
    this.isVoluntaryFlag = isVoluntaryFlag;
  }

  public Boolean getIsInvoluntaryFlag() {
    return isInvoluntaryFlag;
  }

  public void setIsInvoluntaryFlag(Boolean isInvoluntaryFlag) {
    this.isInvoluntaryFlag = isInvoluntaryFlag;
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
    if (!(o instanceof RefSeparationNature that)) {
      return false;
    }
    return Objects.equals(natureCode, that.natureCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(natureCode);
  }

  @Override
  public String toString() {
    return "RefSeparationNature{" +
        "id=" + id +
        ", natureCode='" + natureCode + '\'' +
        ", natureName='" + natureName + '\'' +
        ", isVoluntaryFlag=" + isVoluntaryFlag +
        ", isInvoluntaryFlag=" + isInvoluntaryFlag +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
