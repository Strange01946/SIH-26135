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
@Table(name = "ref_placement_source")
public class RefPlacementSource {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "placement_source_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "source_code", length = 32, nullable = false, unique = true)
  private String sourceCode;

  @Column(name = "source_name", length = 150, nullable = false)
  private String sourceName;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefPlacementSource() {
  }

  public RefPlacementSource(String sourceCode, String sourceName, Integer sortOrder) {
    this.sourceCode = sourceCode;
    this.sourceName = sourceName;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  public void setSourceCode(String sourceCode) {
    this.sourceCode = sourceCode;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
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
    if (!(o instanceof RefPlacementSource that)) {
      return false;
    }
    return Objects.equals(sourceCode, that.sourceCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceCode);
  }

  @Override
  public String toString() {
    return "RefPlacementSource{" +
        "id=" + id +
        ", sourceCode='" + sourceCode + '\'' +
        ", sourceName='" + sourceName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
