package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "sectors")
public class Sector {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "sector_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "sector_code", length = 32, nullable = false, unique = true)
  private String sectorCode;

  @Column(name = "sector_name", length = 150, nullable = false, unique = true)
  private String sectorName;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Sector() {
  }

  public Sector(String sectorCode, String sectorName, Long lifecycleStatusId) {
    this.sectorCode = sectorCode;
    this.sectorName = sectorName;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSectorCode() {
    return sectorCode;
  }

  public void setSectorCode(String sectorCode) {
    this.sectorCode = sectorCode;
  }

  public String getSectorName() {
    return sectorName;
  }

  public void setSectorName(String sectorName) {
    this.sectorName = sectorName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
    if (!(o instanceof Sector other)) {
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
    return "Sector{" +
        "id=" + id +
        ", sectorCode='" + sectorCode + '\'' +
        ", sectorName='" + sectorName + '\'' +
        ", lifecycleStatusId=" + lifecycleStatusId +
        '}';
  }
}
