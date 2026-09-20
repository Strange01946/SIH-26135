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
@Table(name = "ref_delivery_mode")
public class RefDeliveryMode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "delivery_mode_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "mode_code", length = 32, nullable = false, unique = true)
  private String modeCode;

  @Column(name = "mode_name", length = 100, nullable = false)
  private String modeName;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefDeliveryMode() {
  }

  public RefDeliveryMode(String modeCode, String modeName, Integer sortOrder) {
    this.modeCode = modeCode;
    this.modeName = modeName;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getModeCode() {
    return modeCode;
  }

  public void setModeCode(String modeCode) {
    this.modeCode = modeCode;
  }

  public String getModeName() {
    return modeName;
  }

  public void setModeName(String modeName) {
    this.modeName = modeName;
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
    if (!(o instanceof RefDeliveryMode other)) {
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
    return "RefDeliveryMode{" +
        "id=" + id +
        ", modeCode='" + modeCode + '\'' +
        ", modeName='" + modeName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
