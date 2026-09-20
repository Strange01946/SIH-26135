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
@Table(name = "ref_data_quality_category", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ref_data_quality_category_code", columnNames = {"category_code"})
})
public class RefDataQualityCategory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_quality_category_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "category_code", length = 32, nullable = false, unique = true)
  private String categoryCode;

  @Column(name = "category_name", length = 100, nullable = false)
  private String categoryName;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefDataQualityCategory() {
  }

  public RefDataQualityCategory(String categoryCode, String categoryName, Integer sortOrder) {
    this.categoryCode = categoryCode;
    this.categoryName = categoryName;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
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
    if (!(o instanceof RefDataQualityCategory that)) {
      return false;
    }
    return Objects.equals(categoryCode, that.categoryCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(categoryCode);
  }

  @Override
  public String toString() {
    return "RefDataQualityCategory{" +
        "id=" + id +
        ", categoryCode='" + categoryCode + '\'' +
        ", categoryName='" + categoryName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
