package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.dto.request.OperationalEventFilterCriteria;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.security.AuditPayloadMasker;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Class-based query repository executing bounded, tenant-scoped SQL searches against
 * {@code system_event_logs} using {@link JdbcTemplate}.
 *
 * <p>Preserves Spring Data JPA repository interface count (137) and {@code @Query} count (2).
 */
@Repository
public class SystemEventLogQueryRepository {

  public record CallerTenantScope(
      boolean nationwideAdmin,
      Long userId,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      Long employerId
  ) {}

  private static final Map<String, String> SORT_COLUMN_MAP = Map.ofEntries(
      Map.entry("id", "sel.system_event_log_id"),
      Map.entry("eventcode", "sel.event_code"),
      Map.entry("actoruserid", "sel.actor_user_id"),
      Map.entry("entitytype", "sel.entity_type"),
      Map.entry("entityid", "sel.entity_id"),
      Map.entry("sourcecomponent", "sel.source_component"),
      Map.entry("occurredat", "sel.occurred_at"),
      Map.entry("createdat", "sel.created_at")
  );

  private static final String BASE_SELECT =
      "SELECT sel.system_event_log_id, " +
          "sel.system_event_category_id, c.category_code, c.category_name, " +
          "sel.system_event_severity_id, s.severity_code, s.severity_name, " +
          "sel.event_code, sel.actor_user_id, sel.entity_type, sel.entity_id, " +
          "sel.source_component, sel.event_summary, sel.occurred_at, sel.created_at ";

  private static final String BASE_FROM =
      "FROM system_event_logs sel " +
          "JOIN ref_system_event_category c ON sel.system_event_category_id = c.system_event_category_id " +
          "JOIN ref_system_event_severity s ON sel.system_event_severity_id = s.system_event_severity_id " +
          "LEFT JOIN users au ON sel.actor_user_id = au.user_id " +
          "LEFT JOIN users eu ON UPPER(sel.entity_type) = 'USER' AND sel.entity_id = eu.user_id " +
          "LEFT JOIN import_batches ib ON UPPER(sel.entity_type) IN ('IMPORTBATCH', 'IMPORT_BATCH') AND sel.entity_id = ib.import_batch_id " +
          "LEFT JOIN users ibu ON ib.initiated_by_user_id = ibu.user_id " +
          "LEFT JOIN training_providers tp ON UPPER(sel.entity_type) IN ('TRAININGPROVIDER', 'TRAINING_PROVIDER') AND sel.entity_id = tp.provider_id " +
          "LEFT JOIN training_centers tc ON UPPER(sel.entity_type) IN ('TRAININGCENTER', 'TRAINING_CENTER') AND sel.entity_id = tc.center_id " +
          "LEFT JOIN employers emp ON UPPER(sel.entity_type) = 'EMPLOYER' AND sel.entity_id = emp.employer_id " +
          "LEFT JOIN training_providers autp ON au.training_provider_id = autp.provider_id " +
          "LEFT JOIN employers auemp ON au.employer_id = auemp.employer_id " +
          "LEFT JOIN training_providers eutp ON eu.training_provider_id = eutp.provider_id " +
          "LEFT JOIN employers euemp ON eu.employer_id = euemp.employer_id " +
          "LEFT JOIN training_providers ibutp ON ibu.training_provider_id = ibutp.provider_id " +
          "LEFT JOIN employers ibuemp ON ibu.employer_id = ibuemp.employer_id ";

  private final JdbcTemplate jdbcTemplate;

  public SystemEventLogQueryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Page<SystemEventLogResponse> searchSystemEvents(
      OperationalEventFilterCriteria criteria,
      Long categoryId,
      Long severityId,
      CallerTenantScope scope,
      Pageable pageable) {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> params = new ArrayList<>();

    appendFilterPredicates(where, params, criteria, categoryId, severityId);
    appendTenantPredicates(where, params, scope);

    String countSql = "SELECT COUNT(*) " + BASE_FROM + where;
    Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    long total = totalCount != null ? totalCount : 0L;
    if (total == 0L || pageable.getOffset() >= total) {
      return new PageImpl<>(java.util.Collections.emptyList(), pageable, total);
    }

    String orderBy = buildOrderByClause(pageable);
    String dataSql = BASE_SELECT + BASE_FROM + where + orderBy + " LIMIT ? OFFSET ?";
    List<Object> dataParams = new ArrayList<>(params);
    dataParams.add(pageable.getPageSize());
    dataParams.add(pageable.getOffset());

    List<SystemEventLogResponse> content = jdbcTemplate.query(dataSql, new SystemEventRowMapper(), dataParams.toArray());
    return new PageImpl<>(content, pageable, total);
  }

  public Optional<SystemEventLogResponse> findScopedEventById(Long id, CallerTenantScope scope) {
    StringBuilder where = new StringBuilder(" WHERE sel.system_event_log_id = ?");
    List<Object> params = new ArrayList<>();
    params.add(id);
    appendTenantPredicates(where, params, scope);

    String sql = BASE_SELECT + BASE_FROM + where + " LIMIT 1";
    List<SystemEventLogResponse> rows = jdbcTemplate.query(sql, new SystemEventRowMapper(), params.toArray());
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  private void appendFilterPredicates(
      StringBuilder where,
      List<Object> params,
      OperationalEventFilterCriteria criteria,
      Long categoryId,
      Long severityId) {
    if (categoryId != null) {
      where.append(" AND sel.system_event_category_id = ?");
      params.add(categoryId);
    }
    if (criteria.getCategoryCode() != null && !criteria.getCategoryCode().isBlank()) {
      where.append(" AND UPPER(c.category_code) = ?");
      params.add(criteria.getCategoryCode().trim().toUpperCase(Locale.ROOT));
    }
    if (severityId != null) {
      where.append(" AND sel.system_event_severity_id = ?");
      params.add(severityId);
    }
    if (criteria.getSeverityCode() != null && !criteria.getSeverityCode().isBlank()) {
      where.append(" AND UPPER(s.severity_code) = ?");
      params.add(criteria.getSeverityCode().trim().toUpperCase(Locale.ROOT));
    }
    if (criteria.getEventCode() != null && !criteria.getEventCode().isBlank()) {
      where.append(" AND UPPER(sel.event_code) = ?");
      params.add(criteria.getEventCode().trim().toUpperCase(Locale.ROOT));
    }
    if (criteria.getActorUserId() != null) {
      where.append(" AND sel.actor_user_id = ?");
      params.add(criteria.getActorUserId());
    }
    if (criteria.getEntityType() != null && !criteria.getEntityType().isBlank()) {
      where.append(" AND UPPER(sel.entity_type) = ?");
      params.add(criteria.getEntityType().trim().toUpperCase(Locale.ROOT));
    }
    if (criteria.getEntityId() != null) {
      where.append(" AND sel.entity_id = ?");
      params.add(criteria.getEntityId());
    }
    if (criteria.getSourceComponent() != null && !criteria.getSourceComponent().isBlank()) {
      where.append(" AND UPPER(sel.source_component) = ?");
      params.add(criteria.getSourceComponent().trim().toUpperCase(Locale.ROOT));
    }
    if (criteria.getCorrelationId() != null && !criteria.getCorrelationId().isBlank()) {
      String escapedCorr = escapeLike(criteria.getCorrelationId().trim());
      where.append(" AND (sel.event_summary LIKE ? ESCAPE '!' OR sel.event_summary LIKE ? ESCAPE '!')");
      params.add("[cid=" + escapedCorr + "|%");
      params.add("[cid=" + escapedCorr + "]%");
    }
    if (criteria.getRequestId() != null && !criteria.getRequestId().isBlank()) {
      String targetReq = criteria.getRequestId().trim();
      // Match strictly against the leading canonical [cid=<corr>|rid=<req>] prefix (never body text)
      where.append(
          " AND sel.event_summary LIKE '[cid=%|rid=%]%'" +
              " AND SUBSTRING_INDEX(SUBSTRING_INDEX(sel.event_summary, ']', 1), '|rid=', -1) = ?");
      params.add(targetReq);
    }
    if (criteria.getStart() != null) {
      where.append(" AND sel.occurred_at >= ?");
      params.add(Timestamp.valueOf(criteria.getStart()));
    }
    if (criteria.getEnd() != null) {
      where.append(" AND sel.occurred_at <= ?");
      params.add(Timestamp.valueOf(criteria.getEnd()));
    }
  }

  private void appendTenantPredicates(StringBuilder where, List<Object> params, CallerTenantScope scope) {
    if (scope.nationwideAdmin()) {
      return;
    }
    if (scope.trainingProviderId() != null) {
      where.append(
          " AND COALESCE(" +
              "ibu.training_provider_id, " +
              "tc.provider_id, " +
              "CASE WHEN UPPER(sel.entity_type) IN ('TRAININGPROVIDER', 'TRAINING_PROVIDER') THEN sel.entity_id ELSE NULL END, " +
              "eu.training_provider_id, " +
              "au.training_provider_id" +
              ") = ?");
      params.add(scope.trainingProviderId());
      return;
    }
    if (scope.employerId() != null) {
      where.append(
          " AND COALESCE(" +
              "ibu.employer_id, " +
              "CASE WHEN UPPER(sel.entity_type) = 'EMPLOYER' THEN sel.entity_id ELSE NULL END, " +
              "eu.employer_id, " +
              "au.employer_id" +
              ") = ?");
      params.add(scope.employerId());
      return;
    }
    if (scope.stateId() != null) {
      where.append(
          " AND COALESCE(" +
              "tp.state_id, tc.state_id, emp.state_id, " +
              "CASE WHEN UPPER(sel.entity_type) = 'STATE' THEN sel.entity_id ELSE NULL END, " +
              "ibu.state_id, ibutp.state_id, ibuemp.state_id, " +
              "eu.state_id, eutp.state_id, euemp.state_id, " +
              "au.state_id, autp.state_id, auemp.state_id" +
              ") = ?");
      params.add(scope.stateId());
      if (scope.districtId() != null) {
        where.append(
            " AND COALESCE(" +
                "tp.district_id, tc.district_id, emp.district_id, " +
                "CASE WHEN UPPER(sel.entity_type) = 'DISTRICT' THEN sel.entity_id ELSE NULL END, " +
                "ibu.district_id, ibutp.district_id, ibuemp.district_id, " +
                "eu.district_id, eutp.district_id, euemp.district_id, " +
                "au.district_id, autp.district_id, auemp.district_id" +
                ") = ?");
        params.add(scope.districtId());
      }
      return;
    }
    // Fail closed if scope cannot be established
    where.append(" AND 1 = 0");
  }

  private String buildOrderByClause(Pageable pageable) {
    if (pageable == null || pageable.getSort().isUnsorted()) {
      return " ORDER BY sel.occurred_at DESC, sel.system_event_log_id DESC";
    }
    Sort.Order primary = pageable.getSort().iterator().next();
    String key = primary.getProperty().toLowerCase(Locale.ROOT).replace("_", "");
    String column = SORT_COLUMN_MAP.get(key);
    if (column == null) {
      throw new BadRequestException("Invalid sort field provided", "INVALID_SORT_FIELD");
    }
    String dir = primary.isAscending() ? "ASC" : "DESC";
    if ("sel.system_event_log_id".equals(column)) {
      return " ORDER BY " + column + " " + dir;
    }
    return " ORDER BY " + column + " " + dir + ", sel.system_event_log_id " + dir;
  }

  private static String escapeLike(String value) {
    return value
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_");
  }

  private static class SystemEventRowMapper implements RowMapper<SystemEventLogResponse> {
    @Override
    public SystemEventLogResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
      Long id = rs.getLong("system_event_log_id");
      Long catId = rs.getObject("system_event_category_id") != null
          ? rs.getLong("system_event_category_id") : null;
      String catCode = rs.getString("category_code");
      String catName = rs.getString("category_name");
      Long sevId = rs.getObject("system_event_severity_id") != null
          ? rs.getLong("system_event_severity_id") : null;
      String sevCode = rs.getString("severity_code");
      String sevName = rs.getString("severity_name");
      String eventCode = rs.getString("event_code");
      Long actorUserId = rs.getObject("actor_user_id") != null
          ? rs.getLong("actor_user_id") : null;
      String entityType = rs.getString("entity_type");
      Long entityId = rs.getObject("entity_id") != null
          ? rs.getLong("entity_id") : null;
      String sourceComponent = rs.getString("source_component");
      String eventSummary = AuditPayloadMasker.maskPreservingCorrelationBounded(rs.getString("event_summary"), 500);
      Timestamp occurredTs = rs.getTimestamp("occurred_at");
      Timestamp createdTs = rs.getTimestamp("created_at");
      LocalDateTime occurredAt = occurredTs != null ? occurredTs.toLocalDateTime() : null;
      LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;

      return new SystemEventLogResponse(
          id,
          catId,
          catCode,
          catName,
          sevId,
          sevCode,
          sevName,
          eventCode,
          actorUserId,
          entityType,
          entityId,
          sourceComponent,
          eventSummary,
          occurredAt,
          createdAt
      );
    }
  }
}
