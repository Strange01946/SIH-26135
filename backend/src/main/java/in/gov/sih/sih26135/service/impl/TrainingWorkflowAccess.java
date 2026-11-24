package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class TrainingWorkflowAccess {
  private final WorkflowSecurityScopeValidator security;
  private final JdbcTemplate jdbc;

  TrainingWorkflowAccess(WorkflowSecurityScopeValidator security, JdbcTemplate jdbc) {
    this.security = security;
    this.jdbc = jdbc;
  }

  TrainingTenantScopeSupport.CallerScope providerScope() {
    var scope = TrainingTenantScopeSupport.resolveCallerScope(security, jdbc);
    if (!scope.isProviderScoped() || !hasAuthority("ROLE_training_provider")) {
      throw new AccessDeniedException("Training provider access is required");
    }
    actor();
    return scope;
  }

  TrainingTenantScopeSupport.CallerScope reviewerScope() {
    var scope = TrainingTenantScopeSupport.resolveCallerScope(security, jdbc);
    boolean authorizedRole = scope.nationwideUnrestricted()
        ? hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_government_admin")
        : (hasAuthority("ROLE_district_officer") && scope.districtId() != null)
          || (hasAuthority("ROLE_state_admin") && scope.isStateScoped());
    if (!authorizedRole || scope.isProviderScoped()) {
      throw new AccessDeniedException("An authorized training reviewer is required");
    }
    actor();
    return scope;
  }

  User actor() {
    return security.resolveAuthenticatedUser()
        .filter(user -> user.getDeletedAt() == null && Long.valueOf(2L).equals(user.getUserStatusId()))
        .orElseThrow(() -> new AccessDeniedException("Active user account required"));
  }

  boolean eligibleProvider(long providerId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM training_providers p JOIN ref_accreditation_status a USING (accreditation_status_id)
          WHERE provider_id=? AND lifecycle_status_id=1 AND deleted_at IS NULL
            AND a.status_code NOT IN ('SUSPENDED','EXPIRED','REVOKED'))
        """, Boolean.class, providerId));
  }

  static boolean hasAuthority(String code) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getAuthorities().stream()
        .anyMatch(authority -> code.equals(authority.getAuthority()));
  }
}
