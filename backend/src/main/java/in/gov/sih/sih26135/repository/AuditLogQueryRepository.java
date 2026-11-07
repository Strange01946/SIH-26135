package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.dto.request.AuditLogFilterCriteria;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.dto.response.AuditTrailTimelineResponse;
import in.gov.sih.sih26135.event.listener.AuditTrailEventListener;
import in.gov.sih.sih26135.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Class-based query repository executing bounded, parameterized queries against
 * {@code audit_logs} using {@link JdbcTemplate}.
 *
 * <p>Preserves Spring Data JPA repository interface count (137) and repository {@code @Query}
 * annotation count (2) invariants.
 */
@Repository
public class AuditLogQueryRepository {

  private static final Map<String, String> SORT_FIELD_COLUMN_MAP = Map.of(
      "occurredat", "a.occurred_at",
      "id", "a.audit_log_id",
      "auditlogid", "a.audit_log_id",
      "entitytype", "a.entity_type",
      "actoruserid", "a.actor_user_id",
      "createdat", "a.created_at"
  );

  private final JdbcTemplate jdbcTemplate;

  public AuditLogQueryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Searches audit log records using multi-criteria parameterized SQL.
   */
  public Page<AuditLogResponse> searchAuditLogs(AuditLogFilterCriteria criteria, Pageable pageable) {
    StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
    List<Object> params = new ArrayList<>();

    if (criteria.getActorUserId() != null) {
      whereClause.append(" AND a.actor_user_id = ?");
      params.add(criteria.getActorUserId());
    }

    if (criteria.getAuditActionId() != null) {
      whereClause.append(" AND a.audit_action_id = ?");
      params.add(criteria.getAuditActionId());
    }

    if (criteria.getAuditActionCode() != null && !criteria.getAuditActionCode().isBlank()) {
      whereClause.append(" AND UPPER(r.action_code) = ?");
      params.add(criteria.getAuditActionCode().trim().toUpperCase());
    }

    if (criteria.getEntityType() != null && !criteria.getEntityType().isBlank()) {
      whereClause.append(" AND UPPER(a.entity_type) = ?");
      params.add(criteria.getEntityType().trim().toUpperCase());
    }

    if (criteria.getEntityId() != null) {
      whereClause.append(" AND a.entity_id = ?");
      params.add(criteria.getEntityId());
    }

    if (criteria.getStart() != null) {
      whereClause.append(" AND a.occurred_at >= ?");
      params.add(Timestamp.valueOf(criteria.getStart()));
    }

    if (criteria.getEnd() != null) {
      whereClause.append(" AND a.occurred_at <= ?");
      params.add(Timestamp.valueOf(criteria.getEnd()));
    }

    if (criteria.getCorrelationId() != null && !criteria.getCorrelationId().isBlank()) {
      whereClause.append(" AND a.correlation_id = ?");
      params.add(criteria.getCorrelationId().trim());
    }

    // Count query
    String countSql = "SELECT COUNT(*) FROM audit_logs a JOIN ref_audit_action r ON a.audit_action_id = r.audit_action_id"
        + whereClause;
    Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    long totalCount = total != null ? total : 0L;

    if (totalCount == 0) {
      return new PageImpl<>(Collections.emptyList(), pageable, 0L);
    }

    // Determine sort column and direction
    String sortColumn = resolveSortColumn(pageable);
    String direction = resolveSortDirection(pageable);

    String querySql = "SELECT a.audit_log_id, a.actor_user_id, a.audit_action_id, r.action_code, r.action_name, "
        + "a.entity_type, a.entity_id, a.occurred_at, a.correlation_id, a.change_summary, a.created_at "
        + "FROM audit_logs a JOIN ref_audit_action r ON a.audit_action_id = r.audit_action_id "
        + whereClause
        + " ORDER BY " + sortColumn + " " + direction + ", a.audit_log_id " + direction
        + " LIMIT ? OFFSET ?";

    List<Object> queryParams = new ArrayList<>(params);
    queryParams.add(pageable.getPageSize());
    queryParams.add(pageable.getOffset());

    List<AuditLogResponse> content = jdbcTemplate.query(querySql, new AuditLogRowMapper(), queryParams.toArray());
    return new PageImpl<>(content, pageable, totalCount);
  }

  /**
   * Reconstructs an entity's chronological audit trail timeline.
   */
  public AuditTrailTimelineResponse getEntityTimeline(String entityType, Long entityId) {
    String sql = "SELECT a.audit_log_id, a.actor_user_id, a.audit_action_id, r.action_code, r.action_name, "
        + "a.occurred_at, a.correlation_id, a.change_summary "
        + "FROM audit_logs a JOIN ref_audit_action r ON a.audit_action_id = r.audit_action_id "
        + "WHERE UPPER(a.entity_type) = ? AND a.entity_id = ? "
        + "ORDER BY a.occurred_at ASC, a.audit_log_id ASC "
        + "LIMIT 1000";

    List<AuditTrailTimelineResponse.AuditTrailEntry> entries = jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Timestamp occurredTs = rs.getTimestamp("occurred_at");
          LocalDateTime occurredAt = occurredTs != null ? occurredTs.toLocalDateTime() : null;
          Long actorUserId = rs.getObject("actor_user_id") != null ? rs.getLong("actor_user_id") : null;
          String sanitizedSummary = AuditTrailEventListener.sanitizeChangeSummary(rs.getString("change_summary"));

          return new AuditTrailTimelineResponse.AuditTrailEntry(
              rs.getLong("audit_log_id"),
              actorUserId,
              rs.getString("action_code"),
              rs.getString("action_name"),
              occurredAt,
              rs.getString("correlation_id"),
              sanitizedSummary
          );
        },
        entityType.trim().toUpperCase(),
        entityId
    );

    return AuditTrailTimelineResponse.of(entityType, entityId, entries);
  }

  private String resolveSortColumn(Pageable pageable) {
    if (pageable.getSort().isSorted()) {
      Sort.Order order = pageable.getSort().iterator().next();
      String property = order.getProperty().toLowerCase().replace("_", "");
      String column = SORT_FIELD_COLUMN_MAP.get(property);
      if (column != null) {
        return column;
      }
      throw new BadRequestException("Invalid sort field: " + order.getProperty(), "INVALID_SORT_FIELD");
    }
    return "a.occurred_at";
  }

  private String resolveSortDirection(Pageable pageable) {
    if (pageable.getSort().isSorted()) {
      Sort.Order order = pageable.getSort().iterator().next();
      return order.isAscending() ? "ASC" : "DESC";
    }
    return "DESC";
  }

  private static class AuditLogRowMapper implements RowMapper<AuditLogResponse> {
    @Override
    public AuditLogResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
      Timestamp occurredTs = rs.getTimestamp("occurred_at");
      LocalDateTime occurredAt = occurredTs != null ? occurredTs.toLocalDateTime() : null;

      Timestamp createdTs = rs.getTimestamp("created_at");
      LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;

      Long actorUserId = rs.getObject("actor_user_id") != null ? rs.getLong("actor_user_id") : null;
      Long entityId = rs.getObject("entity_id") != null ? rs.getLong("entity_id") : null;
      String sanitizedSummary = AuditTrailEventListener.sanitizeChangeSummary(rs.getString("change_summary"));

      return new AuditLogResponse(
          rs.getLong("audit_log_id"),
          actorUserId,
          rs.getLong("audit_action_id"),
          rs.getString("action_code"),
          rs.getString("action_name"),
          rs.getString("entity_type"),
          entityId,
          occurredAt,
          rs.getString("correlation_id"),
          sanitizedSummary,
          createdAt
      );
    }
  }
}
