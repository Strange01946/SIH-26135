package in.gov.sih.sih26135.security;

import java.io.Serializable;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Authoritative security principal representing an internal, non-interactive system service
 * execution identity (e.g. CLI runners, scheduled jobs, event listeners).
 *
 * <p>Enforces:
 * <ul>
 *   <li>Explicit service identification and human-readable audit name.</li>
 *   <li>Explicit tenancy scope type (NATIONWIDE, PROVIDER, EMPLOYER, STATE, DISTRICT).</li>
 *   <li>Strict immutability and non-impersonation of regular human administrators.</li>
 *   <li>Zero exposure of sensitive credentials or wildcard admin overrides.</li>
 * </ul>
 */
public class InternalServicePrincipal implements Principal, Serializable {

  private static final long serialVersionUID = 1L;

  private final String serviceId;
  private final String serviceName;
  private final TenantScopeType tenantScopeType;
  private final Long tenantId;
  private final LocalDateTime createdAt;

  public InternalServicePrincipal(String serviceId, String serviceName) {
    this(serviceId, serviceName, null, TenantScopeType.NATIONWIDE);
  }

  public InternalServicePrincipal(
      String serviceId,
      String serviceName,
      Long tenantId,
      TenantScopeType tenantScopeType) {
    this.serviceId = Objects.requireNonNull(serviceId, "serviceId cannot be null").trim();
    this.serviceName = serviceName != null && !serviceName.isBlank() ? serviceName.trim() : serviceId;
    this.tenantScopeType = tenantScopeType != null ? tenantScopeType : TenantScopeType.NATIONWIDE;
    this.tenantId = tenantId;
    this.createdAt = LocalDateTime.now();

    if (this.tenantScopeType != TenantScopeType.NATIONWIDE && this.tenantId == null) {
      throw new IllegalArgumentException("tenantId is required for tenant-scoped internal principal: " + tenantScopeType);
    }
  }

  @Override
  public String getName() {
    return "internal:" + serviceId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public TenantScopeType getTenantScopeType() {
    return tenantScopeType;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public boolean isNationwide() {
    return tenantScopeType == TenantScopeType.NATIONWIDE;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InternalServicePrincipal that = (InternalServicePrincipal) o;
    return Objects.equals(serviceId, that.serviceId)
        && tenantScopeType == that.tenantScopeType
        && Objects.equals(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceId, tenantScopeType, tenantId);
  }

  @Override
  public String toString() {
    return "InternalServicePrincipal{"
        + "serviceId='" + serviceId + '\''
        + ", serviceName='" + serviceName + '\''
        + ", tenantScopeType=" + tenantScopeType
        + ", tenantId=" + tenantId
        + ", createdAt=" + createdAt
        + '}';
  }
}
