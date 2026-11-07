package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Filter criteria DTO for querying audit trail records.
 * Supports pagination, temporal bounds, actor scoping, and entity correlation.
 */
public class AuditLogFilterCriteria {

  @Positive(message = "actorUserId must be positive")
  private Long actorUserId;

  @Size(max = 64, message = "auditActionCode cannot exceed 64 characters")
  private String auditActionCode;

  @Size(max = 64, message = "entityType cannot exceed 64 characters")
  private String entityType;

  @Positive(message = "entityId must be positive")
  private Long entityId;

  private LocalDateTime start;

  private LocalDateTime end;

  @Size(max = 64, message = "correlationId cannot exceed 64 characters")
  private String correlationId;

  @Min(value = 0, message = "page index cannot be negative")
  private int page = 0;

  @Min(value = 1, message = "page size must be at least 1")
  @Max(value = 100, message = "page size cannot exceed 100")
  private int size = 20;

  @Size(max = 64, message = "sort field cannot exceed 64 characters")
  private String sort = "occurredAt";

  @Pattern(regexp = "(?i)^(ASC|DESC)$", message = "sort direction must be ASC or DESC")
  private String direction = "DESC";

  public AuditLogFilterCriteria() {}

  public AuditLogFilterCriteria(
      Long actorUserId,
      String auditActionCode,
      String entityType,
      Long entityId,
      LocalDateTime start,
      LocalDateTime end,
      String correlationId,
      int page,
      int size,
      String sort,
      String direction) {
    this.actorUserId = actorUserId;
    this.auditActionCode = auditActionCode;
    this.entityType = entityType;
    this.entityId = entityId;
    this.start = start;
    this.end = end;
    this.correlationId = correlationId;
    this.page = Math.max(0, page);
    this.size = (size >= 1 && size <= 100) ? size : 20;
    this.sort = (sort != null && !sort.isBlank()) ? sort : "occurredAt";
    this.direction = (direction != null && direction.equalsIgnoreCase("ASC")) ? "ASC" : "DESC";
  }

  public boolean hasValidDateRange() {
    if (start != null && end != null) {
      return !start.isAfter(end);
    }
    return true;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(Long actorUserId) {
    this.actorUserId = actorUserId;
  }

  public String getAuditActionCode() {
    return auditActionCode;
  }

  public void setAuditActionCode(String auditActionCode) {
    this.auditActionCode = auditActionCode;
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

  public LocalDateTime getStart() {
    return start;
  }

  public void setStart(LocalDateTime start) {
    this.start = start;
  }

  public LocalDateTime getEnd() {
    return end;
  }

  public void setEnd(LocalDateTime end) {
    this.end = end;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getSort() {
    return sort;
  }

  public void setSort(String sort) {
    this.sort = sort;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AuditLogFilterCriteria that = (AuditLogFilterCriteria) o;
    return page == that.page &&
        size == that.size &&
        Objects.equals(actorUserId, that.actorUserId) &&
        Objects.equals(auditActionCode, that.auditActionCode) &&
        Objects.equals(entityType, that.entityType) &&
        Objects.equals(entityId, that.entityId) &&
        Objects.equals(start, that.start) &&
        Objects.equals(end, that.end) &&
        Objects.equals(correlationId, that.correlationId) &&
        Objects.equals(sort, that.sort) &&
        Objects.equals(direction, that.direction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actorUserId, auditActionCode, entityType, entityId,
        start, end, correlationId, page, size, sort, direction);
  }

  @Override
  public String toString() {
    return "AuditLogFilterCriteria{" +
        "actorUserId=" + actorUserId +
        ", auditActionCode='" + auditActionCode + '\'' +
        ", entityType='" + entityType + '\'' +
        ", entityId=" + entityId +
        ", start=" + start +
        ", end=" + end +
        ", correlationId='" + correlationId + '\'' +
        ", page=" + page +
        ", size=" + size +
        ", sort='" + sort + '\'' +
        ", direction='" + direction + '\'' +
        '}';
  }
}
