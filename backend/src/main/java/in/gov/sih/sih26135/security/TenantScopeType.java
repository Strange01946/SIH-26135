package in.gov.sih.sih26135.security;

/**
 * Enumeration of tenant scope boundaries supported for internal and workflow execution contexts.
 */
public enum TenantScopeType {
  /**
   * Unrestricted nationwide scope (e.g. system CLI smoke tests, platform administrative jobs).
   */
  NATIONWIDE,

  /**
   * Scoped to a specific training provider tenant.
   */
  PROVIDER,

  /**
   * Scoped to a specific employer tenant.
   */
  EMPLOYER,

  /**
   * Scoped to a specific state jurisdiction.
   */
  STATE,

  /**
   * Scoped to a specific district jurisdiction.
   */
  DISTRICT
}
