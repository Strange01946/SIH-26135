package in.gov.sih.sih26135.communication.service;

import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.communication.webhook.MockWebhookSignatureValidator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunicationCenterQueryService {
  private static final String LOGS_FROM = """
      FROM communication_logs l
      JOIN ref_communication_direction d USING (communication_direction_id)
      JOIN ref_communication_channel ch USING (communication_channel_id)
      JOIN ref_communication_purpose p USING (communication_purpose_id)
      JOIN ref_communication_status s USING (communication_status_id)
      JOIN trainees t ON t.trainee_id = l.trainee_id
      WHERE d.direction_code = 'OUTBOUND'
      """;
  private static final String LOGS_FILTER = """
      AND (:channel IS NULL OR ch.channel_code = :channel)
      AND (:status IS NULL
          OR (:status = 'OPTED_OUT' AND s.status_code = 'BLOCKED_NO_CONSENT'
              AND l.failure_reason = 'CONSENT_WITHDRAWN')
          OR s.status_code = :status)
      AND (:search IS NULL OR l.message_template_code LIKE :search ESCAPE '!'
           OR p.purpose_code LIKE :search ESCAPE '!'
           OR CAST(l.communication_log_id AS CHAR) = :id)
      """;
  private static final String LOG_COLUMNS = """
      SELECT l.communication_log_id, l.message_template_code, p.purpose_code, ch.channel_code,
          s.status_code, l.failure_reason, l.provider_message_id,
          CASE WHEN t.deleted_at IS NULL THEN
            CASE WHEN ch.channel_code = 'EMAIL' THEN t.email
                 WHEN ch.channel_code IN ('SMS', 'WHATSAPP', 'PHONE') THEN t.phone END
          END AS contact,
          l.created_at, l.sent_at, l.delivered_at
      """;
  private static final String JOBS_FROM = """
      FROM communication_dispatch_jobs j
      JOIN ref_communication_channel ch ON ch.communication_channel_id = j.channel_id
      """;
  private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9.!#$%&'+/=?^_{}|~-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
  private static final Pattern PHONE = Pattern.compile("\\+?[0-9 ().-]{7,24}");

  private final NamedParameterJdbcTemplate jdbc;

  public CommunicationCenterQueryService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  public PagedResponse<LogRow> logs(String channel, String status, String query, int page, int size) {
    String search = query == null || query.isBlank() ? null : query.trim();
    var parameters = new MapSqlParameterSource()
        .addValue("channel", normalize(channel))
        .addValue("status", normalize(status))
        .addValue("id", search)
        .addValue("search", search == null ? null : "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
    return list(LOG_COLUMNS, LOGS_FROM + LOGS_FILTER, "l.communication_log_id", parameters, page, size, this::mapLog);
  }

  public LogRow log(long id) {
    return jdbc.queryForObject(LOG_COLUMNS + LOGS_FROM + " AND l.communication_log_id = :id",
        new MapSqlParameterSource("id", id), this::mapLog);
  }

  @Transactional(readOnly = true)
  public JobQueue jobs(String status, int page, int size) {
    var states = jdbc.query("""
        SELECT job_status, COUNT(*) AS total
        FROM communication_dispatch_jobs GROUP BY job_status ORDER BY job_status
        """, (rs, row) -> new JobCount(rs.getString("job_status"), rs.getLong("total")));
    var pageData = list("""
        SELECT j.job_id, j.template_code, ch.channel_code, j.job_status, j.attempt_count, j.max_attempts,
            j.delivery_mode, j.scheduled_at, j.updated_at, j.communication_log_id
        """, JOBS_FROM + " WHERE (:status IS NULL OR j.job_status = :status)", "j.job_id",
        new MapSqlParameterSource("status", normalize(status)), page, size,
        (rs, row) -> new JobRow(rs.getLong("job_id"), rs.getString("template_code"), rs.getString("channel_code"),
            rs.getString("job_status"), rs.getInt("attempt_count"), rs.getInt("max_attempts"),
            rs.getString("delivery_mode"), rs.getObject("scheduled_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class), rs.getObject("communication_log_id", Long.class)));
    return new JobQueue(states, pageData);
  }

  private <T> PagedResponse<T> list(String columns, String from, String sort, MapSqlParameterSource params,
      int page, int size, RowMapper<T> mapper) {
    var request = PageRequest.of(page, size);
    Long total = jdbc.queryForObject("SELECT COUNT(*) " + from, params, Long.class);
    params.addValue("limit", size).addValue("offset", request.getOffset());
    List<T> rows = jdbc.query(columns + from + " ORDER BY " + sort + " DESC LIMIT :limit OFFSET :offset", params, mapper);
    return PagedResponse.of(new PageImpl<>(rows, request, total));
  }

  private LogRow mapLog(ResultSet rs, int row) throws SQLException {
    String status = rs.getString("status_code");
    String providerId = rs.getString("provider_message_id");
    String channel = rs.getString("channel_code");
    return new LogRow(rs.getLong("communication_log_id"), rs.getString("message_template_code"),
        rs.getString("purpose_code"), channel, status, mask(channel, rs.getString("contact")),
        "OPTED_OUT".equals(status) || ("BLOCKED_NO_CONSENT".equals(status)
            && "CONSENT_WITHDRAWN".equals(rs.getString("failure_reason"))),
        providerId != null && providerId.startsWith(MockWebhookSignatureValidator.DEMO_RECEIPT_PREFIX),
        rs.getObject("created_at", LocalDateTime.class), rs.getObject("sent_at", LocalDateTime.class),
        rs.getObject("delivered_at", LocalDateTime.class));
  }

  static String mask(String channel, String contact) {
    if (contact == null || contact.isBlank()) {
      return null;
    }
    String value = contact.trim();
    if ("EMAIL".equals(channel)) {
      var email = EMAIL.matcher(value);
      if (email.matches()) {
        String local = email.group(1);
        return (local.length() > 2 ? local.charAt(0) + "***" + local.charAt(local.length() - 1) : "***")
            + "@" + email.group(2);
      }
    } else if (PHONE.matcher(value).matches()) {
      String digits = value.replaceAll("[^0-9]", "");
      if (digits.length() >= 8) {
        String suffix = digits.substring(digits.length() - 3);
        return (value.startsWith("+91") && digits.length() == 12 ? "+91-" : "") + "XXXXX-XX" + suffix;
      }
    }
    return null;
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  public record LogRow(long id, String templateCode, String purpose, String channel, String status,
      String recipientMasked, boolean optedOut, boolean demoReceipt, LocalDateTime createdAt,
      LocalDateTime sentAt, LocalDateTime deliveredAt) {}

  public record JobRow(long id, String templateCode, String channel, String status, int attemptCount, int maxAttempts,
      String deliveryMode, LocalDateTime scheduledAt, LocalDateTime updatedAt, Long communicationLogId) {}

  public record JobCount(String status, long count) {}

  public record JobQueue(List<JobCount> counts, PagedResponse<JobRow> jobs) {}
}
