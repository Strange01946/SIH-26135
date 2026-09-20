package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ref_communication_channel")
public class RefCommunicationChannel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "communication_channel_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "channel_code", length = 32, nullable = false, unique = true)
  private String channelCode;

  @Column(name = "channel_name", length = 100, nullable = false)
  private String channelName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "required_consent_type_id")
  private RefConsentType requiredConsentType;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefCommunicationChannel() {
  }

  public RefCommunicationChannel(String channelCode, String channelName, Integer sortOrder) {
    this.channelCode = channelCode;
    this.channelName = channelName;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getChannelCode() {
    return channelCode;
  }

  public void setChannelCode(String channelCode) {
    this.channelCode = channelCode;
  }

  public String getChannelName() {
    return channelName;
  }

  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  public RefConsentType getRequiredConsentType() {
    return requiredConsentType;
  }

  public void setRequiredConsentType(RefConsentType requiredConsentType) {
    this.requiredConsentType = requiredConsentType;
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
    if (!(o instanceof RefCommunicationChannel that)) {
      return false;
    }
    return Objects.equals(channelCode, that.channelCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channelCode);
  }

  @Override
  public String toString() {
    return "RefCommunicationChannel{" +
        "id=" + id +
        ", channelCode='" + channelCode + '\'' +
        ", channelName='" + channelName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
