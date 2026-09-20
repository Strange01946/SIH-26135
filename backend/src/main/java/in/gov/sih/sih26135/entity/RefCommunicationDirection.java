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
@Table(name = "ref_communication_direction")
public class RefCommunicationDirection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "communication_direction_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "direction_code", length = 32, nullable = false, unique = true)
  private String directionCode;

  @Column(name = "direction_name", length = 100, nullable = false)
  private String directionName;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefCommunicationDirection() {
  }

  public RefCommunicationDirection(String directionCode, String directionName, Integer sortOrder) {
    this.directionCode = directionCode;
    this.directionName = directionName;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getDirectionCode() {
    return directionCode;
  }

  public void setDirectionCode(String directionCode) {
    this.directionCode = directionCode;
  }

  public String getDirectionName() {
    return directionName;
  }

  public void setDirectionName(String directionName) {
    this.directionName = directionName;
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
    if (!(o instanceof RefCommunicationDirection that)) {
      return false;
    }
    return Objects.equals(directionCode, that.directionCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(directionCode);
  }

  @Override
  public String toString() {
    return "RefCommunicationDirection{" +
        "id=" + id +
        ", directionCode='" + directionCode + '\'' +
        ", directionName='" + directionName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
