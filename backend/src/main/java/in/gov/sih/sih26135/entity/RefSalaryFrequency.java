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
@Table(name = "ref_salary_frequency")
public class RefSalaryFrequency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "salary_frequency_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "frequency_code", length = 32, nullable = false, unique = true)
  private String frequencyCode;

  @Column(name = "frequency_name", length = 100, nullable = false)
  private String frequencyName;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefSalaryFrequency() {
  }

  public RefSalaryFrequency(String frequencyCode, String frequencyName, Integer sortOrder) {
    this.frequencyCode = frequencyCode;
    this.frequencyName = frequencyName;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getFrequencyCode() {
    return frequencyCode;
  }

  public void setFrequencyCode(String frequencyCode) {
    this.frequencyCode = frequencyCode;
  }

  public String getFrequencyName() {
    return frequencyName;
  }

  public void setFrequencyName(String frequencyName) {
    this.frequencyName = frequencyName;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder != null ? sortOrder : 0;
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
    if (!(o instanceof RefSalaryFrequency that)) {
      return false;
    }
    return Objects.equals(frequencyCode, that.frequencyCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(frequencyCode);
  }

  @Override
  public String toString() {
    return "RefSalaryFrequency{" +
        "id=" + id +
        ", frequencyCode='" + frequencyCode + '\'' +
        ", frequencyName='" + frequencyName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
