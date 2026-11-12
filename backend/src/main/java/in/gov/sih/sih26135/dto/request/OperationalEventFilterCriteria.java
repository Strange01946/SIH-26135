package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Filter criteria DTO for querying system operational event logs.
 * Supports pagination, temporal filtering, severity/category scoping, and correlation tracing.
 */
public class OperationalEventFilterCriteria {

  @Size(max = 64, message = "categoryCode cannot exceed 64 characters")
  private String categoryCode;

  @Size(max = 64, message = "severityCode cannot exceed 64 characters")
  private String severityCode;

  @Size(max = 64, message = "eventCode cannot exceed 64 characters")
  private String eventCode;

  @Positive(message = "actorUserId must be positive")
  private Long actorUserId;

  @Size(max = 64, message = "entityType cannot exceed 64 characters")
  private String entityType;

  @Positive(message = "entityId must be positive")
  private Long entityId;

  private LocalDateTime start;

  private LocalDateTime end;

  @Size(max = 64, message = "correlationId cannot exceed 64 characters")
  private String correlationId;

  @Size(max = 64, message = "requestId cannot exceed 64 characters")
  private String requestId;

  @Size(max = 64, message = "sourceComponent cannot exceed 64 characters")
  private String sourceComponent;

  @Min(value = 0, message = "page index cannot be negative")
  private int page = 0;

  @Min(value = 1, message = "page size must be at least 1")
  @Max(value = 100, message = "page size cannot exceed 100")
  private int size = 20;

  @Size(max = 64, message = "sort field cannot exceed 64 characters")
  private String sort = "occurredAt";

  @Pattern(regexp = "(?i)^(ASC|DESC)$", message = "sort direction must be ASC or DESC")
  private String direction = "DESC";

  public OperationalEventFilterCriteria() {}

  public OperationalEventFilterCriteria(
      String categoryCode,
      String severityCode,
      String eventCode,
      Long actorUserId,
      String entityType,
      Long entityId,
      LocalDateTime start,
      LocalDateTime end,
      String correlationId,
      int page,
      int size,
      String sort,
      String direction) {
    this.categoryCode = categoryCode;
    this.severityCode = severityCode;
    this.eventCode = eventCode;
    this.actorUserId = actorUserId;
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

  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public String getSeverityCode() {
    return severityCode;
  }

  public void setSeverityCode(String severityCode) {
    this.severityCode = severityCode;
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

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getSourceComponent() {
    return sourceComponent;
  }

  public void setSourceComponent(String sourceComponent) {
    this.sourceComponent = sourceComponent;
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
    OperationalEventFilterCriteria that = (OperationalEventFilterCriteria) o;
    return page == that.page &&
        size == that.size &&
        Objects.equals(categoryCode, that.categoryCode) &&
        Objects.equals(severityCode, that.severityCode) &&
        Objects.equals(eventCode, that.eventCode) &&
        Objects.equals(actorUserId, that.actorUserId) &&
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
    return Objects.hash(categoryCode, severityCode, eventCode, actorUserId,
        entityType, entityId, start, end, correlationId, page, size, sort, direction);
  }

  @Override
  public String toString() {
    return "OperationalEventFilterCriteria{" +
        "categoryCode='" + categoryCode + '\'' +
        ", severityCode='" + severityCode + '\'' +
        ", eventCode='" + eventCode + '\'' +
        ", actorUserId=" + actorUserId +
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
