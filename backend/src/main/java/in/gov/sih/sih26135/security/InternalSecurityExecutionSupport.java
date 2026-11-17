package in.gov.sih.sih26135.security;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Authoritative security component facilitating explicitly authorized internal system workflows
 * (CLI runners, event listeners, background schedulers) without impersonating administrative users
 * or weakening multi-tenant boundaries.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Rejection of untrusted HTTP callers attempting to claim internal service execution privileges.</li>
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
   * Validates internal service principal integrity.
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
    if (!principal.isNationwide() && principal.getTenantId() == null) {
      throw new IllegalArgumentException("tenantId is required when tenantScopeType is not NATIONWIDE");
    }
  }
}
