package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reusable fail-closed tenancy scope resolver for training center and training batch services.
 * Delegates identity and nationwide verification to {@link WorkflowSecurityScopeValidator}.
 */
final class TrainingTenantScopeSupport {

  private TrainingTenantScopeSupport() {
  }

  record CallerScope(
      boolean nationwideUnrestricted,
      Long stateId,
      Long districtId,
      Long trainingProviderId) {

    boolean isProviderScoped() {
      return !nationwideUnrestricted && trainingProviderId != null;
    }

    boolean isStateScoped() {
      return !nationwideUnrestricted && stateId != null;
    }
  }

  static CallerScope resolveCallerScope(
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("Authentication is required for training resource access");
    }

    Set<String> authorities = auth.getAuthorities() != null
        ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
        : Set.of();

    if (scopeValidator.isTraineeCaller() || authorities.contains("ROLE_trainee")
        || scopeValidator.isEmployerCaller() || authorities.contains("ROLE_employer")) {
      throw new AccessDeniedException("Caller persona is not authorized to access training center or batch resources");
    }

    if (scopeValidator.isNationwideAdmin()) {
      return new CallerScope(true, null, null, null);
    }

    User caller = scopeValidator.resolveAuthenticatedUser()
        .orElseThrow(() -> new AccessDeniedException("Authoritative caller user record not found"));
    if (caller.getDeletedAt() != null || (caller.getUserStatusId() != null && caller.getUserStatusId() != 1)) {
      throw new AccessDeniedException("Caller user account is deleted or inactive");
    }

    boolean roleProvider = authorities.contains("ROLE_training_provider");
    boolean roleStateAdmin = authorities.contains("ROLE_state_admin");
    boolean roleOfficer = authorities.contains("ROLE_district_officer") || authorities.contains("ROLE_government_officer");
    boolean roleNational = authorities.contains("ROLE_super_admin") || authorities.contains("ROLE_government_admin");

    boolean hasState = caller.getStateId() != null;
    boolean hasDistrict = caller.getDistrictId() != null;
    boolean hasProvider = caller.getTrainingProviderId() != null;
    boolean hasEmployer = caller.getEmployerId() != null;

    if (hasEmployer) {
      throw new AccessDeniedException("Employer accounts cannot access training center or batch resources");
    }
    if ((roleProvider ? 1 : 0) + ((roleStateAdmin || roleOfficer) ? 1 : 0) > 1) {
      throw new AccessDeniedException("Caller holds conflicting persona roles");
    }
    if (roleProvider && (!hasProvider || hasState || hasDistrict)) {
      throw new AccessDeniedException("Training provider caller lacks valid unambiguous provider scope");
    }
    if ((roleStateAdmin || roleOfficer) && (!hasState || hasProvider)) {
      throw new AccessDeniedException("Administrative caller lacks valid unambiguous state scope");
    }
    if (roleOfficer && !hasDistrict) {
      throw new AccessDeniedException("District officer caller lacks required districtId scope");
    }
    if (roleNational && (hasState || hasDistrict || hasProvider)) {
      throw new AccessDeniedException("Scoped administrator cannot claim nationwide scope");
    }

    if (hasProvider) {
      if (hasState || hasDistrict) {
        throw new AccessDeniedException("Caller has conflicting provider and geographic tenancy");
      }
      return new CallerScope(false, null, null, caller.getTrainingProviderId());
    }
    if (hasState) {
      return new CallerScope(false, caller.getStateId(), caller.getDistrictId(), null);
    }

    // Legacy core administrative callers whose DB user holds super_admin / government_admin without tenant restrictions
    if (!roleProvider && !roleStateAdmin && !roleOfficer
        && (hasUnscopedDbAdminRole(caller.getId(), jdbcTemplate) || isBootstrapSystemAdmin(caller, authorities))) {
      return new CallerScope(true, null, null, null);
    }

    throw new AccessDeniedException("Caller lacks authorized tenant scope");
  }

  private static boolean isBootstrapSystemAdmin(User caller, Set<String> authorities) {
    return Long.valueOf(1L).equals(caller.getId())
        && "admin".equalsIgnoreCase(caller.getUsername())
        && caller.getStateId() == null
        && caller.getDistrictId() == null
        && caller.getTrainingProviderId() == null
        && caller.getEmployerId() == null
        && !authorities.contains("ROLE_verifier")
        && !authorities.contains("ROLE_analyst")
        && authorities.contains("center.manage")
        && authorities.contains("course.manage")
        && authorities.contains("provider.manage");
  }

  static void validateCenterOwnership(CallerScope scope, Long providerId, Long stateId, Long districtId) {
    if (scope == null || scope.nationwideUnrestricted()) {
      return;
    }
    if (scope.isProviderScoped()) {
      if (providerId == null || !scope.trainingProviderId().equals(providerId)) {
        throw new AccessDeniedException("Training center does not belong to caller training provider");
      }
      return;
    }
    if (scope.isStateScoped()) {
      if (stateId == null || !scope.stateId().equals(stateId)) {
        throw new AccessDeniedException("Training center is outside caller assigned state scope");
      }
      if (scope.districtId() != null && !scope.districtId().equals(districtId)) {
        throw new AccessDeniedException("Training center is outside caller assigned district scope");
      }
      return;
    }
    throw new AccessDeniedException("Caller lacks authorized scope for training center");
  }

  static void validateBatchOwnership(
      CallerScope scope, Long batchProviderId, Long centerProviderId, Long stateId, Long districtId) {
    if (scope == null || scope.nationwideUnrestricted()) {
      return;
    }
    if (scope.isProviderScoped()) {
      if (batchProviderId == null || !scope.trainingProviderId().equals(batchProviderId)
          || centerProviderId == null || !scope.trainingProviderId().equals(centerProviderId)) {
        throw new AccessDeniedException("Training batch or its center does not belong to caller training provider");
      }
      return;
    }
    if (scope.isStateScoped()) {
      if (stateId == null || !scope.stateId().equals(stateId)) {
        throw new AccessDeniedException("Training batch is outside caller assigned state scope");
      }
      if (scope.districtId() != null && !scope.districtId().equals(districtId)) {
        throw new AccessDeniedException("Training batch is outside caller assigned district scope");
      }
      return;
    }
    throw new AccessDeniedException("Caller lacks authorized scope for training batch");
  }

  private static boolean hasUnscopedDbAdminRole(Long userId, JdbcTemplate jdbcTemplate) {
    if (userId == null || jdbcTemplate == null) {
      return false;
    }
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON ur.role_id = r.role_id " +
            "WHERE ur.user_id = ? AND r.deleted_at IS NULL " +
            "AND (r.lifecycle_status_id IS NULL OR r.lifecycle_status_id = 1) " +
            "AND r.role_code IN ('super_admin', 'government_admin', 'ROLE_super_admin', 'ROLE_government_admin')",
        Integer.class, userId);
    return count != null && count > 0;
  }
}
