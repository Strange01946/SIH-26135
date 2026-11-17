package in.gov.sih.sih26135.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Spring Security {@link org.springframework.security.core.Authentication} token representing
 * an authenticated internal service execution context.
 *
 * <p>Never carries user passwords or administrator credentials. Identifies internal
 * system operations while preserving audit accountability and tenancy boundaries.
 */
public class InternalAuthenticationToken extends AbstractAuthenticationToken {

  private static final long serialVersionUID = 1L;

  private final InternalServicePrincipal principal;

  public InternalAuthenticationToken(
      InternalServicePrincipal principal,
      Collection<? extends GrantedAuthority> authorities) {
    super(authorities != null ? authorities : Collections.emptyList());
    this.principal = Objects.requireNonNull(principal, "principal cannot be null");
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null; // Internal service tokens never carry passwords or credentials
  }

  @Override
  public InternalServicePrincipal getPrincipal() {
    return principal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    InternalAuthenticationToken that = (InternalAuthenticationToken) o;
    return Objects.equals(principal, that.principal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), principal);
  }
}
