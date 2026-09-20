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

@Entity
@Table(name = "system_event_logs")
public class SystemEventLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "system_event_log_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "system_event_category_id", nullable = false)
  private RefSystemEventCategory systemEventCategory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "system_event_severity_id", nullable = false)
  private RefSystemEventSeverity systemEventSeverity;

  @Column(name = "event_code", length = 64, nullable = false)
  private String eventCode;

  @Column(name = "actor_user_id")
  private Long actorUserId;

  @Column(name = "entity_type", length = 64)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "source_component", length = 64)
  private String sourceComponent;

  @Column(name = "event_summary", length = 500, nullable = false)
  private String eventSummary;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public SystemEventLog() {
  }

  public SystemEventLog(RefSystemEventCategory systemEventCategory,
      RefSystemEventSeverity systemEventSeverity, String eventCode, String eventSummary,
      LocalDateTime occurredAt) {
    this.systemEventCategory = systemEventCategory;
    this.systemEventSeverity = systemEventSeverity;
    this.eventCode = eventCode;
    this.eventSummary = eventSummary;
    this.occurredAt = occurredAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public RefSystemEventCategory getSystemEventCategory() {
    return systemEventCategory;
  }

  public void setSystemEventCategory(RefSystemEventCategory systemEventCategory) {
    this.systemEventCategory = systemEventCategory;
  }

  public RefSystemEventSeverity getSystemEventSeverity() {
    return systemEventSeverity;
  }

  public void setSystemEventSeverity(RefSystemEventSeverity systemEventSeverity) {
    this.systemEventSeverity = systemEventSeverity;
  }

  public String getEventCode() {
    return eventCode;
  }

  public void setEventCode(String eventCode) {
    this.eventCode = eventCode;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(Long actorUserId) {
    this.actorUserId = actorUserId;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public String getSourceComponent() {
    return sourceComponent;
  }

  public void setSourceComponent(String sourceComponent) {
    this.sourceComponent = sourceComponent;
  }

  public String getEventSummary() {
    return eventSummary;
  }

  public void setEventSummary(String eventSummary) {
    this.eventSummary = eventSummary;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(LocalDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SystemEventLog that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "SystemEventLog{" +
        "id=" + id +
        ", eventCode='" + eventCode + '\'' +
        ", actorUserId=" + actorUserId +
        ", occurredAt=" + occurredAt +
        '}';
  }
}
