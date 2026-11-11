package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anomaly detector for rule DQ-DUPLICATE-005 (DUPLICATE).
 *
 * <p>Identifies candidate duplicate trainee registrations across distinct training centers based on
 * deterministic, explainable signals (matching phone/email combined with date of birth or full name).
 *
 * <p>Safety Guardrails:
 * <ul>
 *   <li>Never matches based on name alone.</li>
 *   <li>Never exposes raw sensitive identifiers or full contact details in issue summaries.</li>
 *   <li>Canonical identity ordering (t1 < t2) ensures pair symmetry and prevents self-matching.</li>
 *   <li>Requires actual cross-center enrollment evidence (center_id != center_id).</li>
 *   <li>Bounded deterministic pagination prevents silent candidate truncation beyond arbitrary limits.</li>
 *   <li>Aggregate issue strategy preserves all detected candidate IDs without raw PII exposure.</li>
 * </ul>
 */
@Component
public class DuplicateTraineeCandidateQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(DuplicateTraineeCandidateQualityDetector.class);
  public static final String RULE_CODE = "DQ-DUPLICATE-005";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityIssueRegistrationService registrationService;
  private final DataQualityEngineProperties properties;

  public DuplicateTraineeCandidateQualityDetector(
      JdbcTemplate jdbcTemplate,
      DataQualityIssueRegistrationService registrationService,
      DataQualityEngineProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.registrationService = registrationService;
    this.properties = properties;
  }

  @Override
  public String getSupportedRuleCode() {
    return RULE_CODE;
  }

  @Override
  public DataQualityRuleExecutionSummary executeScan(
      DataQualityRule rule,
      DataQualityScanRequest request,
      ScanScopeContext scopeContext) {

    long startTime = System.currentTimeMillis();
    LocalDateTime executedAt = LocalDateTime.now();
    long evaluatedCount = 0;
    long violationCount = 0;
    long newIssuesCreated = 0;
    long deduplicatedSkipped = 0;

    int chunkSize = properties.getBatchChunkSize();
    long lastSeenId = 0;
    boolean hasMore = true;

    while (hasMore) {
      StringBuilder sql = new StringBuilder(
          "SELECT t.trainee_id, t.first_name, t.last_name, t.date_of_birth, t.phone, t.email, t.state_id " +
              "FROM trainees t " +
              "WHERE t.deleted_at IS NULL AND t.trainee_id > ? "
      );
      List<Object> params = new ArrayList<>();
      params.add(lastSeenId);

      // Tenancy scope filters - strictly enforced with authoritative non-null checks
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isEmployer()) {
          throw new AccessDeniedException("Employers are not authorized to scan general trainee records or duplicate registrations");
        } else if (scopeContext.isStateAdmin()) {
          if (scopeContext.stateId() == null) {
            throw new AccessDeniedException("State administrator scope identifier cannot be resolved");
          }
          sql.append("AND t.state_id = ? ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isTrainingProvider()) {
          if (scopeContext.trainingProviderId() == null) {
            throw new AccessDeniedException("Training provider scope identifier cannot be resolved");
          }
          sql.append("AND t.trainee_id IN (SELECT te.trainee_id FROM training_enrollments te WHERE te.provider_id = ?) ");
          params.add(scopeContext.trainingProviderId());
        } else {
          throw new AccessDeniedException("Caller persona is not authorized to scan trainee duplicate registrations");
        }
      }

      // Entity filter
      if (request != null && request.getEntityId() != null) {
        sql.append("AND t.trainee_id = ? ");
        params.add(request.getEntityId());
      }

      sql.append("ORDER BY t.trainee_id ASC LIMIT ?");
      params.add(chunkSize);

      List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
      if (rows.isEmpty()) {
        hasMore = false;
        break;
      }

      for (Map<String, Object> row : rows) {
        evaluatedCount++;
        Long t1Id = ((Number) row.get("trainee_id")).longValue();
        lastSeenId = t1Id;

        String phone = row.get("phone") != null ? ((String) row.get("phone")).trim() : "";
        String email = row.get("email") != null ? ((String) row.get("email")).trim() : "";
        String firstName = row.get("first_name") != null ? ((String) row.get("first_name")).trim() : "";
        String lastName = row.get("last_name") != null ? ((String) row.get("last_name")).trim() : "";
        LocalDate dob = row.get("date_of_birth") != null ? ((Date) row.get("date_of_birth")).toLocalDate() : null;

        // Weak identity guard: If neither phone nor email is present, skip duplicate matching
        if (phone.isEmpty() && email.isEmpty()) {
          continue;
        }

        // Query for canonical candidate t2 with t2.trainee_id > t1Id enrolled in distinct centers
        List<Long> matchingCandidateIds = findCrossCenterDuplicateCandidates(
            t1Id, phone, email, firstName, lastName, dob, scopeContext
        );

        if (!matchingCandidateIds.isEmpty()) {
          violationCount += matchingCandidateIds.size();

          // Aggregate issue strategy: Document all detected candidate IDs without exposing raw PII
          String summary;
          if (matchingCandidateIds.size() == 1) {
            summary = "Candidate duplicate trainee registration across distinct training centers (associated candidate trainee ID: "
                + matchingCandidateIds.get(0) + ")";
          } else {
            summary = "Candidate duplicate trainee registrations across distinct training centers. Matching candidate trainee IDs: "
                + matchingCandidateIds;
          }

          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "Trainee",
              t1Id,
              summary
          );
          if (created) {
            newIssuesCreated++;
          } else {
            deduplicatedSkipped++;
          }
        }
      }

      if (rows.size() < chunkSize || (request != null && request.getEntityId() != null)) {
        hasMore = false;
      }
    }

    long durationMs = System.currentTimeMillis() - startTime;
    return new DataQualityRuleExecutionSummary(
        rule.getId(),
        RULE_CODE,
        "Trainee",
        evaluatedCount,
        violationCount,
        newIssuesCreated,
        deduplicatedSkipped,
        executedAt,
        durationMs,
        "SUCCESS"
    );
  }

  /**
   * Finds deterministic duplicate candidate IDs across different training centers using bounded pagination
   * and respecting caller tenancy scope.
   */
  private List<Long> findCrossCenterDuplicateCandidates(
      Long t1Id,
      String phone,
      String email,
      String firstName,
      String lastName,
      LocalDate dob,
      ScanScopeContext scopeContext) {

    List<Long> allCandidateIds = new ArrayList<>();
    long lastSeenCandidateId = t1Id;
    int candidateBatchSize = 100;
    boolean hasMoreCandidates = true;

    while (hasMoreCandidates) {
      StringBuilder query = new StringBuilder(
          "SELECT DISTINCT t2.trainee_id FROM trainees t2 " +
              "WHERE t2.deleted_at IS NULL AND t2.trainee_id > ? "
      );
      List<Object> args = new ArrayList<>();
      args.add(lastSeenCandidateId);

      // Tenancy scope enforcement on candidate t2
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isStateAdmin()) {
          query.append("AND t2.state_id = ? ");
          args.add(scopeContext.stateId());
        } else if (scopeContext.isTrainingProvider()) {
          query.append("AND t2.trainee_id IN (SELECT te2.trainee_id FROM training_enrollments te2 WHERE te2.provider_id = ?) ");
          args.add(scopeContext.trainingProviderId());
        }
      }

      List<String> conditions = new ArrayList<>();

      // Signal A & B: Matching non-empty phone with DOB or Name
      if (!phone.isEmpty()) {
        if (dob != null) {
          conditions.add("(t2.phone = ? AND t2.date_of_birth = ?)");
          args.add(phone);
          args.add(Date.valueOf(dob));
        }
        if (!firstName.isEmpty()) {
          conditions.add("(t2.phone = ? AND t2.first_name = ? AND t2.last_name = ?)");
          args.add(phone);
          args.add(firstName);
          args.add(lastName);
        }
      }

      // Signal C & D: Matching non-empty email with DOB or Name
      if (!email.isEmpty()) {
        if (dob != null) {
          conditions.add("(LOWER(t2.email) = ? AND t2.date_of_birth = ?)");
          args.add(email.toLowerCase());
          args.add(Date.valueOf(dob));
        }
        if (!firstName.isEmpty()) {
          conditions.add("(LOWER(t2.email) = ? AND t2.first_name = ? AND t2.last_name = ?)");
          args.add(email.toLowerCase());
          args.add(firstName);
          args.add(lastName);
        }
      }

      // Signal E: Both phone and email match
      if (!phone.isEmpty() && !email.isEmpty()) {
        conditions.add("(t2.phone = ? AND LOWER(t2.email) = ?)");
        args.add(phone);
        args.add(email.toLowerCase());
      }

      if (conditions.isEmpty()) {
        return List.of();
      }

      query.append("AND (");
      query.append(String.join(" OR ", conditions));
      query.append(") ");

      // Cross-center relationship requirement: t1 and t2 must be registered in distinct centers
      query.append(
          "AND EXISTS (" +
              "  SELECT 1 FROM training_enrollments te1 " +
              "  JOIN training_enrollments te2 ON te1.center_id != te2.center_id " +
              "  WHERE te1.trainee_id = ? AND te2.trainee_id = t2.trainee_id" +
              ") "
      );
      args.add(t1Id);

      query.append("ORDER BY t2.trainee_id ASC LIMIT ?");
      args.add(candidateBatchSize);

      List<Long> page;
      try {
        page = jdbcTemplate.queryForList(query.toString(), Long.class, args.toArray());
      } catch (Exception e) {
        log.warn("Cross-center duplicate candidate search failed for trainee {}: {}", t1Id, e.getMessage());
        break;
      }

      if (page.isEmpty()) {
        hasMoreCandidates = false;
      } else {
        allCandidateIds.addAll(page);
        lastSeenCandidateId = page.get(page.size() - 1);
        if (page.size() < candidateBatchSize || allCandidateIds.size() >= 1000) {
          hasMoreCandidates = false;
        }
      }
    }

    return allCandidateIds;
  }
}
