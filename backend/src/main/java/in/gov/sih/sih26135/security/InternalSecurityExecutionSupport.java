package in.gov.sih.sih26135.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Authoritative security component facilitating explicitly authorized internal system workflows
 * (CLI runners, event listeners, background schedulers) without impersonating administrative users
 * or weakening multi-tenant boundaries.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Internal service identities and their allowed scopes are strictly derived from trusted,
 *       explicitly authorized workflow registrations, rejecting arbitrary caller-supplied identities.</li>
 *   <li>NATIONWIDE scope is strictly restricted only to explicitly registered nationwide services.</li>
 *   <li>Guaranteed security context cleanup in a finally block to prevent thread-local leakage.</li>
 *   <li>Derivation of narrowly scoped, explicit authorities ({@code ROLE_INTERNAL_SYSTEM}, {@code internal.communication.dispatch}).</li>
 *   <li>Strictly zero assignment of wildcard administrative roles (e.g. {@code ROLE_super_admin}).</li>
 * </ul>
 */
@Component
public class InternalSecurityExecutionSupport {

  private static final Logger log = LoggerFactory.getLogger(InternalSecurityExecutionSupport.class);

  public static final String ROLE_INTERNAL_SYSTEM = "ROLE_INTERNAL_SYSTEM";
  public static final String AUTHORITY_INTERNAL_DISPATCH = "internal.communication.dispatch";
  public static final String AUTHORITY_INTERNAL_WORKFLOW = "internal.workflow.execution";

  private static final List<SimpleGrantedAuthority> DEFAULT_INTERNAL_AUTHORITIES = List.of(
      new SimpleGrantedAuthority(ROLE_INTERNAL_SYSTEM),
      new SimpleGrantedAuthority(AUTHORITY_INTERNAL_DISPATCH),
      new SimpleGrantedAuthority(AUTHORITY_INTERNAL_WORKFLOW)
  );

  /**
   * Immutable registration of an authorized internal system workflow and its permitted scopes.
   */
  public record InternalWorkflowRegistration(
      String serviceId,
      String defaultServiceName,
      Set<TenantScopeType> allowedScopeTypes,
      boolean allowNationwide
  ) {
    public InternalWorkflowRegistration {
      Objects.requireNonNull(serviceId, "serviceId cannot be null");
      Objects.requireNonNull(defaultServiceName, "defaultServiceName cannot be null");
      allowedScopeTypes = allowedScopeTypes == null ? Collections.emptySet() : Set.copyOf(allowedScopeTypes);
    }
  }

  /**
   * Registry of explicitly authorized internal system workflows.
   */
  private final Map<String, InternalWorkflowRegistration> authorizedWorkflows = new ConcurrentHashMap<>();

  public InternalSecurityExecutionSupport() {
    registerDefaultAuthorizedWorkflows();
  }

  /**
   * Pre-registers standard authoritative internal workflows.
   */
  private void registerDefaultAuthorizedWorkflows() {
    // 1. Live Communication Smoke Test Runner (CLI) - Nationwide permitted for platform diagnostics
    registerWorkflow(new InternalWorkflowRegistration(
        "CLI_SMOKE_TEST",
        "Live Communication Smoke Test CLI Runner",
        Set.of(TenantScopeType.NATIONWIDE),
        true
    ));

    // 2. Candidate Lifecycle Post-Commit Event Listener - Nationwide permitted for domain event dispatch
    registerWorkflow(new InternalWorkflowRegistration(
        "CANDIDATE_LIFECYCLE_EVENT_LISTENER",
        "Candidate Lifecycle Event Listener",
        Set.of(TenantScopeType.NATIONWIDE),
        true
    ));

    // 3. Live Test Readiness Verifier - Nationwide permitted for read-only validation
    registerWorkflow(new InternalWorkflowRegistration(
        "LIVE_TEST_READINESS_VERIFIER",
        "Live Test Trainee Readiness Verifier",
        Set.of(TenantScopeType.NATIONWIDE),
        true
    ));

    // 4. Test Trainee Setup alias (backward compatibility in tests)
    registerWorkflow(new InternalWorkflowRegistration(
        "LIVE_TEST_DATA_SETUP",
        "Live Test Trainee Setup Runner",
        Set.of(TenantScopeType.NATIONWIDE),
        true
    ));

    // 5. Tenant-Scoped Background Automations (Strictly forbidden from claiming NATIONWIDE scope)
    registerWorkflow(new InternalWorkflowRegistration(
        "PROVIDER_AUTOMATION",
        "Training Provider Workflow Job",
        Set.of(TenantScopeType.PROVIDER),
        false
    ));

    registerWorkflow(new InternalWorkflowRegistration(
        "PROVIDER_B_AUTOMATION",
        "Provider B Workflow Job",
        Set.of(TenantScopeType.PROVIDER),
        false
    ));

    registerWorkflow(new InternalWorkflowRegistration(
        "EMPLOYER_AUTOMATION",
        "Employer Verification Workflow Job",
        Set.of(TenantScopeType.EMPLOYER),
        false
    ));

    registerWorkflow(new InternalWorkflowRegistration(
        "DISTRICT_AUTOMATION",
        "District Operations Workflow Job",
        Set.of(TenantScopeType.DISTRICT),
        false
    ));

    registerWorkflow(new InternalWorkflowRegistration(
        "STATE_AUTOMATION",
        "State Administration Workflow Job",
        Set.of(TenantScopeType.STATE),
        false
    ));
  }

  /**
   * Explicitly registers an authorized internal workflow registration.
   *
   * @param registration the workflow registration to authorize
   */
  public void registerWorkflow(InternalWorkflowRegistration registration) {
    Objects.requireNonNull(registration, "registration cannot be null");
    authorizedWorkflows.put(registration.serviceId(), registration);
    log.info("[InternalSecurity] Registered authorized internal workflow: serviceId={}, allowNationwide={}, scopes={}",
        registration.serviceId(), registration.allowNationwide(), registration.allowedScopeTypes());
  }

  /**
   * Creates an authorized nationwide internal service principal for a registered workflow.
   *
   * @param serviceId registered service ID
   * @return authorized principal
   */
  public InternalServicePrincipal createAuthorizedNationwidePrincipal(String serviceId) {
    InternalWorkflowRegistration reg = getVerifiedRegistration(serviceId);
    if (!reg.allowNationwide()) {
      throw new AccessDeniedException("Internal workflow '" + serviceId + "' is not authorized for NATIONWIDE scope");
    }
    return new InternalServicePrincipal(reg.serviceId(), reg.defaultServiceName(), null, TenantScopeType.NATIONWIDE);
  }

  /**
   * Creates an authorized tenant-scoped internal service principal for a registered workflow.
   *
   * @param serviceId registered service ID
   * @param scopeType tenant scope type
   * @param tenantId tenant ID
   * @return authorized principal
   */
  public InternalServicePrincipal createAuthorizedTenantPrincipal(String serviceId, TenantScopeType scopeType, Long tenantId) {
    InternalWorkflowRegistration reg = getVerifiedRegistration(serviceId);
    if (scopeType == null || scopeType == TenantScopeType.NATIONWIDE) {
      throw new IllegalArgumentException("Tenant scope type must be a specific tenant granularity (not null or NATIONWIDE)");
    }
    if (tenantId == null) {
      throw new IllegalArgumentException("tenantId is required for tenant-scoped internal principal");
    }
    if (!reg.allowedScopeTypes().contains(scopeType)) {
      throw new AccessDeniedException("Tenant scope type " + scopeType + " is not authorized for workflow '" + serviceId + "'");
    }
    return new InternalServicePrincipal(reg.serviceId(), reg.defaultServiceName(), tenantId, scopeType);
  }

  /**
   * Resolves and verifies an authorized registration for the given service ID.
   */
  private InternalWorkflowRegistration getVerifiedRegistration(String serviceId) {
    if (serviceId == null || serviceId.isBlank()) {
      throw new IllegalArgumentException("serviceId cannot be null or blank");
    }
    InternalWorkflowRegistration reg = authorizedWorkflows.get(serviceId);
    if (reg == null) {
      log.warn("Security violation: attempt to access unregistered internal serviceId={}", serviceId);
      throw new AccessDeniedException("Unregistered or unauthorized internal service identity: " + serviceId);
    }
    return reg;
  }

  /**
   * Constructs an authorized {@link InternalAuthenticationToken} for the given internal principal.
   *
   * @param principal verified internal service principal
   * @return authenticated internal token
   */
  public InternalAuthenticationToken createInternalToken(InternalServicePrincipal principal) {
    validateCallerCanEstablishInternalContext(principal);
    return new InternalAuthenticationToken(principal, DEFAULT_INTERNAL_AUTHORITIES);
  }

  /**
   * Executes a given action within an authorized internal system security context.
   *
   * @param principal verified internal service principal
   * @param action runnable to execute
   */
  public void runAsInternalService(InternalServicePrincipal principal, Runnable action) {
    Objects.requireNonNull(action, "action cannot be null");
    runAsInternalService(principal, () -> {
      action.run();
      return null;
    });
  }

  /**
   * Executes a given supplier action within an authorized internal system security context and returns the result.
   *
   * @param principal verified internal service principal
   * @param action supplier action to execute
   * @param <T> return type
   * @return action result
   */
  public <T> T runAsInternalService(InternalServicePrincipal principal, Supplier<T> action) {
    Objects.requireNonNull(principal, "principal cannot be null");
    Objects.requireNonNull(action, "action cannot be null");

    validateCallerCanEstablishInternalContext(principal);

    SecurityContext originalContext = SecurityContextHolder.getContext();
    Authentication originalAuth = originalContext != null ? originalContext.getAuthentication() : null;

    InternalAuthenticationToken internalToken = new InternalAuthenticationToken(principal, DEFAULT_INTERNAL_AUTHORITIES);

    try {
      SecurityContext newContext = SecurityContextHolder.createEmptyContext();
      newContext.setAuthentication(internalToken);
      SecurityContextHolder.setContext(newContext);

      log.debug("[InternalSecurity] Established internal service execution context: serviceId={}, nationwide={}",
          principal.getServiceId(), principal.isNationwide());

      return action.get();
    } finally {
      if (originalAuth != null) {
        SecurityContext restoredContext = SecurityContextHolder.createEmptyContext();
        restoredContext.setAuthentication(originalAuth);
        SecurityContextHolder.setContext(restoredContext);
      } else {
        SecurityContextHolder.clearContext();
      }
      log.debug("[InternalSecurity] Restored security context after internal service execution: serviceId={}",
          principal.getServiceId());
    }
  }

  /**
   * Validates that the requested internal service principal is explicitly authorized by registration
   * and that its claimed scope is permitted for that service.
   */
  private void validateCallerCanEstablishInternalContext(InternalServicePrincipal principal) {
    if (principal == null) {
      throw new IllegalArgumentException("InternalServicePrincipal cannot be null");
    }
    if (principal.getServiceId() == null || principal.getServiceId().isBlank()) {
      throw new IllegalArgumentException("serviceId cannot be null or blank");
    }
    if (principal.getServiceName() == null || principal.getServiceName().isBlank()) {
      throw new IllegalArgumentException("serviceName cannot be null or blank");
    }
    if (principal.getTenantScopeType() == null) {
      throw new IllegalArgumentException("tenantScopeType cannot be null");
    }

    // 1. Authoritative Registration Check: service identity must be registered
    InternalWorkflowRegistration reg = getVerifiedRegistration(principal.getServiceId());

    // 2. Nationwide Scope Authorization: service must be explicitly authorized for nationwide scope
    if (principal.isNationwide()) {
      if (!reg.allowNationwide()) {
        log.warn("Security violation: internal serviceId={} attempted unauthorized NATIONWIDE scope",
            principal.getServiceId());
        throw new AccessDeniedException("Internal service '" + principal.getServiceId()
            + "' is not authorized for NATIONWIDE scope");
      }
    } else {
      // 3. Tenant-Scoped Validation: scope type must be permitted and tenantId must be non-null
      if (!reg.allowedScopeTypes().contains(principal.getTenantScopeType())) {
        log.warn("Security violation: tenant scope {} is not permitted for internal serviceId={}",
            principal.getTenantScopeType(), principal.getServiceId());
        throw new AccessDeniedException("Tenant scope type " + principal.getTenantScopeType()
            + " is not permitted for internal service: " + principal.getServiceId());
      }
      if (principal.getTenantId() == null) {
        throw new IllegalArgumentException("tenantId is required when tenantScopeType is not NATIONWIDE");
      }
    }
  }
}

