package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared fail-closed security and tenant-scope enforcement helper for data quality issue
 * services ({@link DataQualityIssueServiceImpl} and {@link DataQualityWorkflowServiceImpl}).
 */
final class DataQualityIssueScopeSupport {

  private static final int BATCH_SIZE = 500;

  private DataQualityIssueScopeSupport() {
  }

  record CallerScope(
      User caller,
      boolean nationwideAdmin,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      Long employerId,
      Set<String> authorities) {

    boolean isStateScoped() {
      return !nationwideAdmin && stateId != null;
    }

    boolean isProviderScoped() {
      return !nationwideAdmin && trainingProviderId != null;
    }

    boolean isEmployerScoped() {
      return !nationwideAdmin && employerId != null;
    }
  }

  static CallerScope resolveAndValidateCaller(
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate,
      boolean requireManageAuthority) {
    if (scopeValidator == null || jdbcTemplate == null) {
      throw new AccessDeniedException("Security dependencies unavailable to authorize data quality operation");
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("Authenticated user identity is required to access data quality issues");
    }

    Set<String> authorities = auth.getAuthorities() != null
        ? auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet())
        : Set.of();

    if (requireManageAuthority) {
      if (!authorities.contains("system.manage")) {
        throw new AccessDeniedException("Caller lacks required management authority 'system.manage'");
      }
    } else {
      if (!authorities.contains("audit.read") && !authorities.contains("system.manage")) {
        throw new AccessDeniedException("Caller lacks required read or manage authority");
      }
    }

    Long callerUserId = scopeValidator.resolveAuthenticatedUserId();
    if (callerUserId == null) {
      throw new AccessDeniedException("Authenticated user identity is required to access data quality issues");
    }

    if (scopeValidator.isTraineeCaller()) {
      String action = requireManageAuthority ? "manage" : "view";
      throw new AccessDeniedException("Trainees are not authorized to " + action + " data quality issues");
    }

    User caller = scopeValidator.resolveAuthenticatedUser()
        .orElseThrow(() -> new AccessDeniedException("Authoritative user record not found"));

    if (caller.getDeletedAt() != null || (caller.getUserStatusId() != null && caller.getUserStatusId() != 1)) {
      throw new AccessDeniedException("Caller user account is deleted or inactive");
    }

    if (scopeValidator.isNationwideAdmin()) {
      return new CallerScope(caller, true, null, null, null, null, authorities);
    }

    return resolveNonNationwideCallerScope(caller, authorities);
  }

  private static CallerScope resolveNonNationwideCallerScope(User caller, Set<String> authorities) {
    boolean roleProvider = authorities.contains("ROLE_training_provider");
    boolean roleEmployer = authorities.contains("ROLE_employer");
    boolean roleStateAdmin = authorities.contains("ROLE_state_admin");
    boolean roleOfficer = authorities.contains("ROLE_district_officer")
        || authorities.contains("ROLE_government_officer");
    boolean roleNational = authorities.contains("ROLE_super_admin")
        || authorities.contains("ROLE_government_admin");

    int personaRoleCount = (roleProvider ? 1 : 0)
        + (roleEmployer ? 1 : 0)
        + ((roleStateAdmin || roleOfficer) ? 1 : 0);
    if (personaRoleCount > 1) {
      throw new AccessDeniedException("Caller holds conflicting persona roles");
    }

    boolean hasStateId = caller.getStateId() != null;
    boolean hasDistrictId = caller.getDistrictId() != null;
    boolean hasProviderId = caller.getTrainingProviderId() != null;
    boolean hasEmployerId = caller.getEmployerId() != null;

    if (roleNational && (hasProviderId || hasEmployerId)) {
      throw new AccessDeniedException("Caller has conflicting administrative role and organizational tenancy");
    }
    if ((roleStateAdmin || roleOfficer) && (hasProviderId || hasEmployerId)) {
      throw new AccessDeniedException("Administrative caller has conflicting provider or employer tenancy scope");
    }
    if ((roleStateAdmin || roleOfficer) && !hasStateId) {
      throw new AccessDeniedException("Administrative caller lacks required state scope");
    }
    if (roleProvider && !hasProviderId) {
      throw new AccessDeniedException("Training provider caller lacks required trainingProviderId scope");
    }
    if (roleEmployer && !hasEmployerId) {
      throw new AccessDeniedException("Employer caller lacks required employerId scope");
    }

    int tenantRootCount = (hasStateId ? 1 : 0) + (hasProviderId ? 1 : 0) + (hasEmployerId ? 1 : 0);
    if (tenantRootCount == 0) {
      throw new AccessDeniedException("Caller lacks authorized tenant scope");
    }
    if (tenantRootCount > 1) {
      throw new AccessDeniedException("Caller has ambiguous or conflicting tenant scope");
    }

    if (hasStateId && (roleProvider || roleEmployer)) {
      throw new AccessDeniedException("Caller has conflicting persona role and state tenancy");
    }
    if ((hasProviderId || hasEmployerId) && hasDistrictId) {
      throw new AccessDeniedException("Caller has conflicting organizational and geographic tenancy");
    }

    return new CallerScope(
        caller,
        false,
        caller.getStateId(),
        caller.getDistrictId(),
        caller.getTrainingProviderId(),
        caller.getEmployerId(),
        authorities
    );
  }

  static void validateCallerScopeForIssue(
      CallerScope scope,
      DataQualityIssue issue,
      JdbcTemplate jdbcTemplate) {
    if (scope.nationwideAdmin()) {
      return;
    }
    if (issue == null || issue.getEntityType() == null || issue.getEntityType().isBlank()
        || issue.getEntityId() == null) {
      throw new AccessDeniedException("Caller lacks authority to manage unscoped data quality issue");
    }
    validateCallerScopeForEntity(scope, issue.getEntityType(), issue.getEntityId(), jdbcTemplate);
  }

  static void validateCanQueryEntityType(CallerScope scope, String entityType) {
    if (scope.nationwideAdmin()) {
      return;
    }
    String normalized = normalizeEntityType(entityType);
    if (normalized == null) {
      throw new AccessDeniedException("Caller lacks authority to query issue entity type: " + entityType);
    }
    if (scope.isEmployerScoped()
        && !"PlacementRecord".equals(normalized)
        && !"EmploymentRecord".equals(normalized)) {
      throw new AccessDeniedException(
          "Employers are not authorized to access data quality issues for entity type: " + entityType);
    }
  }

  static void validateCallerScopeForEntity(
      CallerScope scope,
      String entityType,
      Long entityId,
      JdbcTemplate jdbcTemplate) {
    if (scope.nationwideAdmin()) {
      return;
    }
    if (jdbcTemplate == null) {
      throw new AccessDeniedException("Database helper unavailable to validate entity tenant scope");
    }
    if (entityType == null || entityType.isBlank() || entityId == null || entityId <= 0) {
      throw new AccessDeniedException("Caller lacks authority to access unscoped data quality entity");
    }

    String normalized = normalizeEntityType(entityType);
    if (normalized == null) {
      throw new AccessDeniedException("Caller lacks authority to manage issue for entity type: " + entityType);
    }

    switch (normalized) {
      case "Trainee" -> validateTraineeScope(scope, entityId, jdbcTemplate);
      case "AttendanceRecord" -> validateAttendanceScope(scope, entityId, jdbcTemplate);
      case "PlacementRecord" -> validatePlacementScope(scope, entityId, jdbcTemplate);
      case "EmploymentRecord" -> validateEmploymentScope(scope, entityId, jdbcTemplate);
      case "BatchEnrollment" -> validateEnrollmentScope(scope, entityId, jdbcTemplate);
      default -> throw new AccessDeniedException("Caller lacks authority to manage issue for entity type: " + entityType);
    }
  }

  private static void validateTraineeScope(CallerScope scope, Long traineeId, JdbcTemplate jdbcTemplate) {
    if (scope.isEmployerScoped()) {
      throw new AccessDeniedException("Employers are not authorized to manage trainee data quality issues");
    }
    if (scope.isStateScoped()) {
      Integer count = scope.districtId() != null
          ? jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM trainees WHERE trainee_id = ? AND state_id = ? AND district_id = ? AND deleted_at IS NULL",
              Integer.class, traineeId, scope.stateId(), scope.districtId())
          : jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM trainees WHERE trainee_id = ? AND state_id = ? AND deleted_at IS NULL",
              Integer.class, traineeId, scope.stateId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue trainee does not belong to caller state scope");
      }
      return;
    }
    if (scope.isProviderScoped()) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM training_enrollments te " +
              "JOIN trainees t ON te.trainee_id = t.trainee_id " +
              "WHERE te.trainee_id = ? AND te.provider_id = ? AND te.deleted_at IS NULL AND t.deleted_at IS NULL",
          Integer.class, traineeId, scope.trainingProviderId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue trainee does not belong to caller training provider scope");
      }
      return;
    }
    throw new AccessDeniedException("Caller lacks authorized tenant scope for this trainee issue");
  }

  private static void validateAttendanceScope(CallerScope scope, Long attendanceId, JdbcTemplate jdbcTemplate) {
    if (scope.isEmployerScoped()) {
      throw new AccessDeniedException("Employers are not authorized to manage attendance data quality issues");
    }
    if (scope.isStateScoped()) {
      Integer count = scope.districtId() != null
          ? jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM attendance_records ar " +
                  "JOIN training_batches tb ON ar.batch_id = tb.batch_id " +
                  "JOIN training_centers tc ON tb.center_id = tc.center_id " +
                  "WHERE ar.attendance_id = ? AND tc.state_id = ? AND tc.district_id = ? AND tc.deleted_at IS NULL",
              Integer.class, attendanceId, scope.stateId(), scope.districtId())
          : jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM attendance_records ar " +
                  "JOIN training_batches tb ON ar.batch_id = tb.batch_id " +
                  "JOIN training_centers tc ON tb.center_id = tc.center_id " +
                  "WHERE ar.attendance_id = ? AND tc.state_id = ? AND tc.deleted_at IS NULL",
              Integer.class, attendanceId, scope.stateId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue attendance record does not belong to caller state scope");
      }
      return;
    }
    if (scope.isProviderScoped()) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM attendance_records ar " +
              "JOIN training_enrollments te ON ar.enrollment_id = te.enrollment_id " +
              "WHERE ar.attendance_id = ? AND te.provider_id = ? AND te.deleted_at IS NULL",
          Integer.class, attendanceId, scope.trainingProviderId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue attendance record does not belong to caller training provider scope");
      }
      return;
    }
    throw new AccessDeniedException("Caller lacks authorized tenant scope for this attendance issue");
  }

  private static void validatePlacementScope(CallerScope scope, Long placementId, JdbcTemplate jdbcTemplate) {
    if (scope.isStateScoped()) {
      Integer count = scope.districtId() != null
          ? jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM placement_records WHERE placement_id = ? AND work_state_id = ? AND work_district_id = ? AND deleted_at IS NULL",
              Integer.class, placementId, scope.stateId(), scope.districtId())
          : jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM placement_records WHERE placement_id = ? AND work_state_id = ? AND deleted_at IS NULL",
              Integer.class, placementId, scope.stateId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue placement record does not belong to caller state scope");
      }
      return;
    }
    if (scope.isProviderScoped()) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM placement_records WHERE placement_id = ? AND provider_id = ? AND deleted_at IS NULL",
          Integer.class, placementId, scope.trainingProviderId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue placement record does not belong to caller training provider scope");
      }
      return;
    }
    if (scope.isEmployerScoped()) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM placement_records WHERE placement_id = ? AND employer_id = ? AND deleted_at IS NULL",
          Integer.class, placementId, scope.employerId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue placement record does not belong to caller employer scope");
      }
      return;
    }
    throw new AccessDeniedException("Caller lacks authorized tenant scope for this placement issue");
  }

  private static void validateEmploymentScope(CallerScope scope, Long employmentId, JdbcTemplate jdbcTemplate) {
    if (scope.isStateScoped()) {
      Integer count = scope.districtId() != null
          ? jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM employment_records WHERE employment_id = ? AND work_state_id = ? AND work_district_id = ? AND deleted_at IS NULL",
              Integer.class, employmentId, scope.stateId(), scope.districtId())
          : jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM employment_records WHERE employment_id = ? AND work_state_id = ? AND deleted_at IS NULL",
              Integer.class, employmentId, scope.stateId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue employment record does not belong to caller state scope");
      }
      return;
    }
    if (scope.isProviderScoped()) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM employment_records er " +
              "JOIN training_enrollments te ON er.enrollment_id = te.enrollment_id " +
              "WHERE er.employment_id = ? AND te.provider_id = ? AND te.deleted_at IS NULL AND er.deleted_at IS NULL",
          Integer.class, employmentId, scope.trainingProviderId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue employment record does not belong to caller training provider scope");
      }
      return;
    }
    if (scope.isEmployerScoped()) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM employment_records WHERE employment_id = ? AND employer_id = ? AND deleted_at IS NULL",
          Integer.class, employmentId, scope.employerId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue employment record does not belong to caller employer scope");
      }
      return;
    }
    throw new AccessDeniedException("Caller lacks authorized tenant scope for this employment issue");
  }

  private static void validateEnrollmentScope(CallerScope scope, Long enrollmentId, JdbcTemplate jdbcTemplate) {
    if (scope.isEmployerScoped()) {
      throw new AccessDeniedException("Employers are not authorized to manage enrollment data quality issues");
    }
    if (scope.isStateScoped()) {
      Integer count = scope.districtId() != null
          ? jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM training_enrollments te " +
                  "JOIN training_batches tb ON te.batch_id = tb.batch_id " +
                  "JOIN training_centers tc ON tb.center_id = tc.center_id " +
                  "WHERE te.enrollment_id = ? AND tc.state_id = ? AND tc.district_id = ? AND tc.deleted_at IS NULL AND te.deleted_at IS NULL",
              Integer.class, enrollmentId, scope.stateId(), scope.districtId())
          : jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM training_enrollments te " +
                  "JOIN training_batches tb ON te.batch_id = tb.batch_id " +
                  "JOIN training_centers tc ON tb.center_id = tc.center_id " +
                  "WHERE te.enrollment_id = ? AND tc.state_id = ? AND tc.deleted_at IS NULL AND te.deleted_at IS NULL",
              Integer.class, enrollmentId, scope.stateId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue enrollment does not belong to caller state scope");
      }
      return;
    }
    if (scope.isProviderScoped()) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM training_enrollments WHERE enrollment_id = ? AND provider_id = ? AND deleted_at IS NULL",
          Integer.class, enrollmentId, scope.trainingProviderId());
      if (count == null || count == 0) {
        throw new AccessDeniedException("Issue enrollment does not belong to caller training provider scope");
      }
      return;
    }
    throw new AccessDeniedException("Caller lacks authorized tenant scope for this enrollment issue");
  }

  static List<DataQualityIssue> filterIssuesInScope(
      CallerScope scope,
      List<DataQualityIssue> issues,
      JdbcTemplate jdbcTemplate) {
    if (issues == null || issues.isEmpty()) {
      return List.of();
    }
    if (scope.nationwideAdmin()) {
      return issues;
    }
    if (jdbcTemplate == null) {
      throw new AccessDeniedException("Database helper unavailable to filter issues by tenant scope");
    }

    Map<String, Set<Long>> candidateIdsByType = new HashMap<>();
    for (DataQualityIssue issue : issues) {
      if (issue == null || issue.getEntityId() == null || issue.getEntityId() <= 0) {
        continue;
      }
      String normalizedType = normalizeEntityType(issue.getEntityType());
      if (normalizedType == null) {
        continue;
      }
      candidateIdsByType
          .computeIfAbsent(normalizedType, k -> new LinkedHashSet<>())
          .add(issue.getEntityId());
    }

    Map<String, Set<Long>> authorizedIdsByType = new HashMap<>();
    for (Map.Entry<String, Set<Long>> entry : candidateIdsByType.entrySet()) {
      authorizedIdsByType.put(
          entry.getKey(),
          findAuthorizedEntityIds(scope, entry.getKey(), entry.getValue(), jdbcTemplate)
      );
    }

    List<DataQualityIssue> filtered = new ArrayList<>();
    for (DataQualityIssue issue : issues) {
      if (issue == null || issue.getEntityId() == null) {
        continue;
      }
      String normalizedType = normalizeEntityType(issue.getEntityType());
      if (normalizedType != null) {
        Set<Long> allowedIds = authorizedIdsByType.get(normalizedType);
        if (allowedIds != null && allowedIds.contains(issue.getEntityId())) {
          filtered.add(issue);
        }
      }
    }
    return filtered;
  }

  private static Set<Long> findAuthorizedEntityIds(
      CallerScope scope,
      String normalizedType,
      Set<Long> candidateIds,
      JdbcTemplate jdbcTemplate) {
    if (candidateIds == null || candidateIds.isEmpty()) {
      return Set.of();
    }
    if (scope.isEmployerScoped()
        && !"PlacementRecord".equals(normalizedType)
        && !"EmploymentRecord".equals(normalizedType)) {
      return Set.of();
    }

    List<Long> idList = new ArrayList<>(candidateIds);
    Set<Long> authorized = new HashSet<>();

    for (int start = 0; start < idList.size(); start += BATCH_SIZE) {
      List<Long> chunk = idList.subList(start, Math.min(start + BATCH_SIZE, idList.size()));
      String inClause = String.join(", ", Collections.nCopies(chunk.size(), "?"));
      String sql;
      List<Object> params = new ArrayList<>();

      switch (normalizedType) {
        case "Trainee" -> {
          if (scope.isStateScoped()) {
            sql = scope.districtId() != null
                ? "SELECT trainee_id FROM trainees WHERE state_id = ? AND district_id = ? AND deleted_at IS NULL AND trainee_id IN (" + inClause + ")"
                : "SELECT trainee_id FROM trainees WHERE state_id = ? AND deleted_at IS NULL AND trainee_id IN (" + inClause + ")";
            params.add(scope.stateId());
            if (scope.districtId() != null) {
              params.add(scope.districtId());
            }
          } else if (scope.isProviderScoped()) {
            sql = "SELECT DISTINCT te.trainee_id FROM training_enrollments te " +
                "JOIN trainees t ON te.trainee_id = t.trainee_id " +
                "WHERE te.provider_id = ? AND te.deleted_at IS NULL AND t.deleted_at IS NULL AND te.trainee_id IN (" + inClause + ")";
            params.add(scope.trainingProviderId());
          } else {
            return Set.of();
          }
        }
        case "AttendanceRecord" -> {
          if (scope.isStateScoped()) {
            sql = scope.districtId() != null
                ? "SELECT DISTINCT ar.attendance_id FROM attendance_records ar " +
                    "JOIN training_batches tb ON ar.batch_id = tb.batch_id " +
                    "JOIN training_centers tc ON tb.center_id = tc.center_id " +
                    "WHERE tc.state_id = ? AND tc.district_id = ? AND tc.deleted_at IS NULL AND ar.attendance_id IN (" + inClause + ")"
                : "SELECT DISTINCT ar.attendance_id FROM attendance_records ar " +
                    "JOIN training_batches tb ON ar.batch_id = tb.batch_id " +
                    "JOIN training_centers tc ON tb.center_id = tc.center_id " +
                    "WHERE tc.state_id = ? AND tc.deleted_at IS NULL AND ar.attendance_id IN (" + inClause + ")";
            params.add(scope.stateId());
            if (scope.districtId() != null) {
              params.add(scope.districtId());
            }
          } else if (scope.isProviderScoped()) {
            sql = "SELECT DISTINCT ar.attendance_id FROM attendance_records ar " +
                "JOIN training_enrollments te ON ar.enrollment_id = te.enrollment_id " +
                "WHERE te.provider_id = ? AND te.deleted_at IS NULL AND ar.attendance_id IN (" + inClause + ")";
            params.add(scope.trainingProviderId());
          } else {
            return Set.of();
          }
        }
        case "PlacementRecord" -> {
          if (scope.isStateScoped()) {
            sql = scope.districtId() != null
                ? "SELECT placement_id FROM placement_records WHERE work_state_id = ? AND work_district_id = ? AND deleted_at IS NULL AND placement_id IN (" + inClause + ")"
                : "SELECT placement_id FROM placement_records WHERE work_state_id = ? AND deleted_at IS NULL AND placement_id IN (" + inClause + ")";
            params.add(scope.stateId());
            if (scope.districtId() != null) {
              params.add(scope.districtId());
            }
          } else if (scope.isProviderScoped()) {
            sql = "SELECT placement_id FROM placement_records WHERE provider_id = ? AND deleted_at IS NULL AND placement_id IN (" + inClause + ")";
            params.add(scope.trainingProviderId());
          } else if (scope.isEmployerScoped()) {
            sql = "SELECT placement_id FROM placement_records WHERE employer_id = ? AND deleted_at IS NULL AND placement_id IN (" + inClause + ")";
            params.add(scope.employerId());
          } else {
            return Set.of();
          }
        }
        case "EmploymentRecord" -> {
          if (scope.isStateScoped()) {
            sql = scope.districtId() != null
                ? "SELECT employment_id FROM employment_records WHERE work_state_id = ? AND work_district_id = ? AND deleted_at IS NULL AND employment_id IN (" + inClause + ")"
                : "SELECT employment_id FROM employment_records WHERE work_state_id = ? AND deleted_at IS NULL AND employment_id IN (" + inClause + ")";
            params.add(scope.stateId());
            if (scope.districtId() != null) {
              params.add(scope.districtId());
            }
          } else if (scope.isProviderScoped()) {
            sql = "SELECT DISTINCT er.employment_id FROM employment_records er " +
                "JOIN training_enrollments te ON er.enrollment_id = te.enrollment_id " +
                "WHERE te.provider_id = ? AND te.deleted_at IS NULL AND er.deleted_at IS NULL AND er.employment_id IN (" + inClause + ")";
            params.add(scope.trainingProviderId());
          } else if (scope.isEmployerScoped()) {
            sql = "SELECT employment_id FROM employment_records WHERE employer_id = ? AND deleted_at IS NULL AND employment_id IN (" + inClause + ")";
            params.add(scope.employerId());
          } else {
            return Set.of();
          }
        }
        case "BatchEnrollment" -> {
          if (scope.isStateScoped()) {
            sql = scope.districtId() != null
                ? "SELECT DISTINCT te.enrollment_id FROM training_enrollments te " +
                    "JOIN training_batches tb ON te.batch_id = tb.batch_id " +
                    "JOIN training_centers tc ON tb.center_id = tc.center_id " +
                    "WHERE tc.state_id = ? AND tc.district_id = ? AND tc.deleted_at IS NULL AND te.deleted_at IS NULL AND te.enrollment_id IN (" + inClause + ")"
                : "SELECT DISTINCT te.enrollment_id FROM training_enrollments te " +
                    "JOIN training_batches tb ON te.batch_id = tb.batch_id " +
                    "JOIN training_centers tc ON tb.center_id = tc.center_id " +
                    "WHERE tc.state_id = ? AND tc.deleted_at IS NULL AND te.deleted_at IS NULL AND te.enrollment_id IN (" + inClause + ")";
            params.add(scope.stateId());
            if (scope.districtId() != null) {
              params.add(scope.districtId());
            }
          } else if (scope.isProviderScoped()) {
            sql = "SELECT DISTINCT enrollment_id FROM training_enrollments WHERE provider_id = ? AND deleted_at IS NULL AND enrollment_id IN (" + inClause + ")";
            params.add(scope.trainingProviderId());
          } else {
            return Set.of();
          }
        }
        default -> {
          return Set.of();
        }
      }

      params.addAll(chunk);
      List<Long> matched = jdbcTemplate.queryForList(sql, Long.class, params.toArray());
      authorized.addAll(matched);
    }

    return authorized;
  }

  static String normalizeEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      return null;
    }
    String normalized = entityType.trim().toUpperCase().replace("_", "");
    return switch (normalized) {
      case "TRAINEE" -> "Trainee";
      case "ATTENDANCERECORD", "ATTENDANCE" -> "AttendanceRecord";
      case "PLACEMENTRECORD", "PLACEMENT" -> "PlacementRecord";
      case "EMPLOYMENTRECORD", "EMPLOYMENT" -> "EmploymentRecord";
      case "BATCHENROLLMENT", "TRAININGENROLLMENT", "ENROLLMENT" -> "BatchEnrollment";
      default -> null;
    };
  }
}
