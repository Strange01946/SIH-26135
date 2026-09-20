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
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_log_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "actor_user_id")
  private Long actorUserId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "audit_action_id", nullable = false)
  private RefAuditAction auditAction;

  @Column(name = "entity_type", length = 64, nullable = false)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Column(name = "correlation_id", length = 64)
  private String correlationId;

  @Column(name = "change_summary", length = 1000)
  private String changeSummary;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public AuditLog() {
  }

  public AuditLog(RefAuditAction auditAction, String entityType, LocalDateTime occurredAt) {
    this.auditAction = auditAction;
    this.entityType = entityType;
    this.occurredAt = occurredAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(Long actorUserId) {
    this.actorUserId = actorUserId;
  }

  public RefAuditAction getAuditAction() {
    return auditAction;
  }

  public void setAuditAction(RefAuditAction auditAction) {
    this.auditAction = auditAction;
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

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(LocalDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getChangeSummary() {
    return changeSummary;
  }

  public void setChangeSummary(String changeSummary) {
    this.changeSummary = changeSummary;
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
    if (!(o instanceof AuditLog that)) {
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
    return "AuditLog{" +
        "id=" + id +
        ", actorUserId=" + actorUserId +
        ", entityType='" + entityType + '\'' +
        ", entityId=" + entityId +
        ", occurredAt=" + occurredAt +
        '}';
  }
}
