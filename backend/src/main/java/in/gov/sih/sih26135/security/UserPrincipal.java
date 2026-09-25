package in.gov.sih.sih26135.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * Minimal security principal representing an authenticated user identity extracted from a verified JWT.
 *
 * <p>Exposes only non-sensitive identity metadata (user ID and username).
 * Sensitive fields (passwords, hashes, PII, secrets) are strictly excluded.
 */
public record UserPrincipal(
    Long id,
    String username
) implements Principal, Serializable {

  @Override
  public String getName() {
    return username;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }
}
