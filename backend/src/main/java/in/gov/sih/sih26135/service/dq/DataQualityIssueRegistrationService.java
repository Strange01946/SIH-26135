package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.entity.RefDataQualityDetectionSource;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.event.listener.AuditTrailEventListener;
import in.gov.sih.sih26135.exception.DataQualityLockAcquisitionException;
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
import java.util.List;
import java.util.Map;

/**
 * Service managing concurrency-safe issue registration and deduplication.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Distributed transactional deduplication via MySQL named locks and key synchronization.</li>
 *   <li>The distributed lock remains held until the issue-creation transaction has committed or rolled back.</li>
 *   <li>Never releases the distributed lock before commit visibility is guaranteed.</li>
 *   <li>Deduplication key: (ruleId + entityType + entityId + is_open_flag=1).</li>
 *   <li>Fails closed when GET_LOCK times out, fails, or returns null (distinguished from deduplication).</li>
 *   <li>Fails closed when duplicate-existence check encounters database query failure.</li>
 *   <li>Preservation of newly discovered relationships on existing OPEN issues without duplicate row creation.</li>
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
   * Evaluates a finding under a distributed named lock:
   * <ul>
   *   <li>If no active OPEN issue exists: creates and commits a new DataQualityIssue (CREATED).</li>
   *   <li>If an active OPEN issue exists and new findings are present: updates the existing issue summary (UPDATED).</li>
   *   <li>If an active OPEN issue exists with identical findings: skips registration (DEDUPLICATED).</li>
   *   <li>If named lock acquisition fails or times out: returns LOCK_FAILED without claiming deduplication.</li>
   * </ul>
   *
   * @param rule data quality rule that detected the anomaly
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param rawSummary issue explanation
   * @return structured {@link RegistrationResult} indicating the exact outcome
   */
  public RegistrationResult registerOrUpdateIssue(DataQualityRule rule, String entityType, Long entityId, String rawSummary) {
    if (rule == null || entityType == null || entityId == null) {
      return RegistrationResult.databaseError("Rule, entityType, and entityId are required");
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
          return RegistrationResult.lockFailed("Distributed named lock acquisition timed out or failed for " + lockKey);
        }

        try {
          // Execute issue registration or update in a new transaction.
          // The distributed named lock on lockConn remains held until transactionTemplate.execute
          // returns (which guarantees that the transaction has COMMITTED to the database).
          return transactionTemplate.execute(status -> {
            // 1. Check if active open issue exists - fails closed if database query fails
            Long existingIssueId = null;
            String existingSummary = null;
            try {
              List<Map<String, Object>> existingRows = jdbcTemplate.queryForList(
                  "SELECT dqi.data_quality_issue_id, dqi.issue_summary FROM data_quality_issues dqi " +
                      "JOIN ref_data_quality_issue_status s ON dqi.data_quality_issue_status_id = s.data_quality_issue_status_id " +
                      "WHERE dqi.data_quality_rule_id = ? AND dqi.entity_type = ? AND dqi.entity_id = ? AND s.is_open_flag = 1 " +
                      "ORDER BY dqi.data_quality_issue_id ASC LIMIT 1",
                  rule.getId(),
                  entityType.trim(),
                  entityId
              );
              if (!existingRows.isEmpty()) {
                Map<String, Object> row = existingRows.get(0);
                existingIssueId = ((Number) row.get("data_quality_issue_id")).longValue();
                existingSummary = (String) row.get("issue_summary");
              }
            } catch (Exception e) {
              log.error("Database query failure while checking active open issue for {}:{}: {}",
                  entityType, entityId, e.getMessage(), e);
              throw e;
            }

            String sanitizedSummary = sanitizeIssueSummary(rawSummary);

            if (existingIssueId != null) {
              // Active OPEN issue already exists
              if (sanitizedSummary.equals(existingSummary)) {
                log.debug("Active open issue {} already contains identical summary for rule {} on {}:{} - deduplicated",
                    existingIssueId, rule.getRuleCode(), entityType, entityId);
                return RegistrationResult.deduplicated(existingIssueId);
              } else {
                // Update the existing OPEN issue with the new findings (prevents loss of new duplicate candidates)
                LocalDateTime now = LocalDateTime.now();
                DataQualityIssue existingIssue = dataQualityIssueRepository.findById(existingIssueId).orElse(null);
                if (existingIssue != null) {
                  existingIssue.setIssueSummary(sanitizedSummary);
                  existingIssue.setUpdatedAt(now);
                  dataQualityIssueRepository.saveAndFlush(existingIssue);
                } else {
                  jdbcTemplate.update(
                      "UPDATE data_quality_issues SET issue_summary = ?, updated_at = NOW() WHERE data_quality_issue_id = ?",
                      sanitizedSummary, existingIssueId
                  );
                }
                log.info("Updated existing active open issue {} with newly discovered findings for rule {} on {}:{}",
                    existingIssueId, rule.getRuleCode(), entityType, entityId);
                return RegistrationResult.updated(existingIssueId);
              }
            }

            // 2. Resolve default OPEN status and RULE source
            RefDataQualityIssueStatus openStatus = refDataQualityIssueStatusRepository.findByStatusCode("OPEN")
                .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode: OPEN"));

            RefDataQualityDetectionSource ruleSource = refDataQualityDetectionSourceRepository.findBySourceCode("RULE")
                .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityDetectionSource", "sourceCode: RULE"));

            // 3. Create new issue
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
            return RegistrationResult.created(issue.getId());
          });

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
   * Legacy compatibility method: delegates to {@link #registerOrUpdateIssue}.
   * Throws {@link DataQualityLockAcquisitionException} on lock failure rather than masking as deduplication.
   *
   * @param rule data quality rule that detected the anomaly
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param rawSummary issue explanation
   * @return {@code true} if a new issue was created, {@code false} if deduplicated or updated
   */
  public boolean registerIssueIfAbsent(DataQualityRule rule, String entityType, Long entityId, String rawSummary) {
    RegistrationResult result = registerOrUpdateIssue(rule, entityType, entityId, rawSummary);
    if (result.isLockFailed()) {
      throw new DataQualityLockAcquisitionException("Failed to acquire distributed registration lock for " + entityType + ":" + entityId + ": " + result.message());
    }
    if (result.isDatabaseError()) {
      throw new RuntimeException("Database error during issue registration for " + entityType + ":" + entityId + ": " + result.message());
    }
    return result.isCreated();
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
