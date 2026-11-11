package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.entity.RefDataQualityDetectionSource;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.event.listener.AuditTrailEventListener;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.RefDataQualityDetectionSourceRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Service managing concurrency-safe issue registration and deduplication.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Distributed transactional deduplication via MySQL named locks and key synchronization.</li>
 *   <li>The distributed lock remains held until the issue-creation transaction has committed or rolled back.</li>
 *   <li>Never releases the distributed lock before commit visibility is guaranteed.</li>
 *   <li>Deduplication key: (ruleId + entityType + entityId + is_open_flag=1).</li>
 *   <li>Fails closed when GET_LOCK times out, fails, or returns null.</li>
 *   <li>Fails closed when duplicate-existence check encounters database query failure.</li>
 *   <li>Preservation of historical RESOLVED / DISMISSED issues without silent reopening.</li>
 *   <li>Sanitization and PII redaction of issue summaries (max 500 characters).</li>
 * </ul>
 */
@Service
public class DataQualityIssueRegistrationService {

  private static final Logger log = LoggerFactory.getLogger(DataQualityIssueRegistrationService.class);

  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;
  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository;
  private final RefDataQualityDetectionSourceRepository refDataQualityDetectionSourceRepository;
  private final TransactionTemplate transactionTemplate;
  private final DataQualityEngineProperties properties;

  public DataQualityIssueRegistrationService(
      DataSource dataSource,
      JdbcTemplate jdbcTemplate,
      DataQualityIssueRepository dataQualityIssueRepository,
      RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository,
      RefDataQualityDetectionSourceRepository refDataQualityDetectionSourceRepository,
      PlatformTransactionManager transactionManager,
      DataQualityEngineProperties properties) {
    this.dataSource = dataSource;
    this.jdbcTemplate = jdbcTemplate;
    this.dataQualityIssueRepository = dataQualityIssueRepository;
    this.refDataQualityIssueStatusRepository = refDataQualityIssueStatusRepository;
    this.refDataQualityDetectionSourceRepository = refDataQualityDetectionSourceRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.properties = properties;
  }

  /**
   * Evaluates a finding and registers a new DataQualityIssue if no active OPEN issue exists for this rule and entity.
   *
   * @param rule data quality rule that detected the anomaly
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param rawSummary issue explanation
   * @return {@code true} if a new issue was created, {@code false} if skipped due to existing active issue or lock timeout
   */
  public boolean registerIssueIfAbsent(DataQualityRule rule, String entityType, Long entityId, String rawSummary) {
    if (rule == null || entityType == null || entityId == null) {
      return false;
    }

    String lockKey = ("dq_lock:" + rule.getId() + ":" + entityType.trim() + ":" + entityId).intern();
    int timeout = properties != null ? properties.getLockTimeoutSeconds() : 10;

    // JVM-level lock prevents thread contention and connection thrashing in the same process
    synchronized (lockKey) {
      // Distributed lock on dedicated connection: holds named lock across entire transaction and commit
      try (Connection lockConn = dataSource.getConnection()) {
        boolean lockAcquired = acquireNamedLock(lockConn, lockKey, timeout);
        if (!lockAcquired) {
          log.warn("Failed to acquire distributed named lock for {}: lock acquisition failed or timed out", lockKey);
          return false;
        }

        try {
          // Execute issue creation in a new transaction.
          // The distributed named lock on lockConn remains held until transactionTemplate.execute
          // returns (which guarantees that the transaction has COMMITTED to the database).
          Boolean created = transactionTemplate.execute(status -> {
            // 1. Check if active open issue exists - fails closed if database query fails
            boolean activeOpenIssueExists = checkActiveOpenIssueExists(rule.getId(), entityType.trim(), entityId);
            if (activeOpenIssueExists) {
              log.debug("Active open issue already exists for rule {} on {}:{} - deduplicated",
                  rule.getRuleCode(), entityType, entityId);
              return false;
            }

            // 2. Resolve default OPEN status and RULE source
            RefDataQualityIssueStatus openStatus = refDataQualityIssueStatusRepository.findByStatusCode("OPEN")
                .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode: OPEN"));

            RefDataQualityDetectionSource ruleSource = refDataQualityDetectionSourceRepository.findBySourceCode("RULE")
                .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityDetectionSource", "sourceCode: RULE"));

            // 3. Sanitize and redact PII from issue summary
            String sanitizedSummary = sanitizeIssueSummary(rawSummary);

            LocalDateTime now = LocalDateTime.now();
            DataQualityIssue issue = new DataQualityIssue(
                rule.getDataQualityCategory(),
                rule.getDataQualitySeverity(),
                openStatus,
                ruleSource,
                entityType.trim(),
                sanitizedSummary,
                now
            );
            issue.setDataQualityRule(rule);
            issue.setEntityId(entityId);
            issue.setCreatedAt(now);
            issue.setUpdatedAt(now);

            dataQualityIssueRepository.saveAndFlush(issue);
            log.debug("Created new DataQualityIssue for rule {} on {}:{}", rule.getRuleCode(), entityType, entityId);
            return true;
          });

          return Boolean.TRUE.equals(created);

        } finally {
          // Distributed lock is released ONLY AFTER transaction commit visibility is guaranteed
          releaseNamedLock(lockConn, lockKey);
        }

      } catch (SQLException e) {
        log.error("Database connection failure while managing distributed named lock for {}: {}", lockKey, e.getMessage(), e);
        throw new RuntimeException("Database error during distributed named lock management: " + e.getMessage(), e);
      }
    }
  }

  /**
   * Checks whether an active (is_open_flag = 1) issue exists for the rule and entity.
   * Does NOT swallow database exceptions; fails closed to prevent duplicate issues on DB errors.
   */
  public boolean checkActiveOpenIssueExists(Long ruleId, String entityType, Long entityId) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM data_quality_issues dqi " +
            "JOIN ref_data_quality_issue_status s ON dqi.data_quality_issue_status_id = s.data_quality_issue_status_id " +
            "WHERE dqi.data_quality_rule_id = ? AND dqi.entity_type = ? AND dqi.entity_id = ? AND s.is_open_flag = 1",
        Integer.class,
        ruleId,
        entityType,
        entityId
    );
    return count != null && count > 0;
  }

  /**
   * Checks whether a historical resolved or dismissed issue exists for the rule and entity.
   */
  public boolean checkHistoricalClosedIssueExists(Long ruleId, String entityType, Long entityId) {
    try {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM data_quality_issues dqi " +
              "JOIN ref_data_quality_issue_status s ON dqi.data_quality_issue_status_id = s.data_quality_issue_status_id " +
              "WHERE dqi.data_quality_rule_id = ? AND dqi.entity_type = ? AND dqi.entity_id = ? AND s.is_terminal_flag = 1",
          Integer.class,
          ruleId,
          entityType,
          entityId
      );
      return count != null && count > 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Acquires a MySQL distributed named lock on the specified connection.
   */
  public static boolean acquireNamedLock(Connection conn, String lockKey, int timeoutSeconds) {
    try (PreparedStatement stmt = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
      stmt.setString(1, lockKey);
      stmt.setInt(2, timeoutSeconds);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          int result = rs.getInt(1);
          if (rs.wasNull()) {
            return false;
          }
          return result == 1;
        }
        return false;
      }
    } catch (SQLException e) {
      log.warn("GET_LOCK query failed on connection for {}: {}", lockKey, e.getMessage());
      return false;
    }
  }

  /**
   * Releases a MySQL distributed named lock on the specified connection.
   */
  public static boolean releaseNamedLock(Connection conn, String lockKey) {
    try (PreparedStatement stmt = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
      stmt.setString(1, lockKey);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          int result = rs.getInt(1);
          return !rs.wasNull() && result == 1;
        }
      }
      return false;
    } catch (SQLException e) {
      log.warn("RELEASE_LOCK query failed on connection for {}: {}", lockKey, e.getMessage());
      return false;
    }
  }

  /**
   * Sanitizes issue summary to prevent PII leakage and enforce schema length limits (max 500).
   */
  public static String sanitizeIssueSummary(String summary) {
    if (summary == null || summary.isBlank()) {
      return "Data quality anomaly detected";
    }
    String cleaned = summary.trim();
    // Redact sensitive patterns (Aadhaar 12-digits, PAN 10-chars) via shared audit sanitizer
    cleaned = AuditTrailEventListener.sanitizeChangeSummary(cleaned);
    if (cleaned.length() > 500) {
      cleaned = cleaned.substring(0, 497) + "...";
    }
    return cleaned;
  }
}
