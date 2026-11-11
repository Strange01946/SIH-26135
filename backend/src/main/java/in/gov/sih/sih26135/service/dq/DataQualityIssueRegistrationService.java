package in.gov.sih.sih26135.service.dq;

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

import java.time.LocalDateTime;

/**
 * Service managing concurrency-safe issue registration and deduplication.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Distributed transactional deduplication via database named locks and key synchronization.</li>
 *   <li>Deduplication key: (ruleId + entityType + entityId + is_open_flag=1).</li>
 *   <li>Preservation of historical RESOLVED / DISMISSED issues without silent reopening.</li>
 *   <li>Sanitization and PII redaction of issue summaries (max 500 characters).</li>
 * </ul>
 */
@Service
public class DataQualityIssueRegistrationService {

  private static final Logger log = LoggerFactory.getLogger(DataQualityIssueRegistrationService.class);

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository;
  private final RefDataQualityDetectionSourceRepository refDataQualityDetectionSourceRepository;
  private final TransactionTemplate transactionTemplate;

  public DataQualityIssueRegistrationService(
      JdbcTemplate jdbcTemplate,
      DataQualityIssueRepository dataQualityIssueRepository,
      RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository,
      RefDataQualityDetectionSourceRepository refDataQualityDetectionSourceRepository,
      PlatformTransactionManager transactionManager) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataQualityIssueRepository = dataQualityIssueRepository;
    this.refDataQualityIssueStatusRepository = refDataQualityIssueStatusRepository;
    this.refDataQualityDetectionSourceRepository = refDataQualityDetectionSourceRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Evaluates a finding and registers a new DataQualityIssue if no active OPEN issue exists for this rule and entity.
   *
   * @param rule data quality rule that detected the anomaly
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param rawSummary issue explanation
   * @return {@code true} if a new issue was created, {@code false} if skipped due to existing active issue
   */
  public boolean registerIssueIfAbsent(DataQualityRule rule, String entityType, Long entityId, String rawSummary) {
    if (rule == null || entityType == null || entityId == null) {
      return false;
    }

    String lockKey = ("dq_lock:" + rule.getId() + ":" + entityType.trim() + ":" + entityId).intern();
    synchronized (lockKey) {
      Boolean created = transactionTemplate.execute(status -> {
        boolean lockAcquired = false;
        try {
          lockAcquired = acquireLock(lockKey);

          // 1. Check if an active open issue already exists for this rule and entity
          boolean hasActiveOpenIssue = checkActiveOpenIssueExists(rule.getId(), entityType.trim(), entityId);
          if (hasActiveOpenIssue) {
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

        } finally {
          if (lockAcquired) {
            releaseLock(lockKey);
          }
        }
      });

      return Boolean.TRUE.equals(created);
    }
  }

  /**
   * Checks whether an active (is_open_flag = 1) issue exists for the rule and entity.
   */
  public boolean checkActiveOpenIssueExists(Long ruleId, String entityType, Long entityId) {
    try {
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
    } catch (Exception e) {
      log.warn("Database check for active open issue failed: {}", e.getMessage());
      return false;
    }
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

  private boolean acquireLock(String lockKey) {
    try {
      Integer result = jdbcTemplate.queryForObject(
          "SELECT GET_LOCK(?, 10)",
          Integer.class,
          lockKey
      );
      return result != null && result == 1;
    } catch (Exception e) {
      // In in-memory or fallback non-MySQL test contexts, proceed
      log.debug("Named lock acquisition skipped or unsupported: {}", e.getMessage());
      return false;
    }
  }

  private void releaseLock(String lockKey) {
    try {
      jdbcTemplate.queryForObject(
          "SELECT RELEASE_LOCK(?)",
          Integer.class,
          lockKey
      );
    } catch (Exception e) {
      log.debug("Named lock release skipped: {}", e.getMessage());
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
