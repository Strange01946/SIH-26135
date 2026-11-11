package in.gov.sih.sih26135.service.dq;

import org.springframework.security.access.AccessDeniedException;

/**
 * Encapsulates the resolved tenancy boundary and administrative authority of the caller executing a scan.
 */
public record ScanScopeContext(
    Long callerUserId,
    boolean isNationwideAdmin,
    boolean isStateAdmin,
    Long stateId,
    boolean isTrainingProvider,
    Long trainingProviderId,
    boolean isEmployer,
    Long employerId
) {

  /**
   * Returns true if caller holds nationwide administrative scan authority.
   */
  public boolean canScanNationwide() {
    return isNationwideAdmin;
  }

  /**
   * Validates that non-nationwide callers possess authoritative, non-null tenancy identifiers.
   * Fails closed with AccessDeniedException if the required identifier is missing.
   */
  public void validateAuthoritativeScope() {
    if (isNationwideAdmin) {
      return;
    }
    if (isStateAdmin) {
      if (stateId == null) {
        throw new AccessDeniedException("State administrator tenancy scope cannot be resolved: stateId is null");
      }
      return;
    }
    if (isTrainingProvider) {
      if (trainingProviderId == null) {
        throw new AccessDeniedException("Training provider tenancy scope cannot be resolved: trainingProviderId is null");
      }
      return;
    }
    if (isEmployer) {
      if (employerId == null) {
        throw new AccessDeniedException("Employer tenancy scope cannot be resolved: employerId is null");
      }
      return;
    }
    throw new AccessDeniedException("Caller lacks administrative or scoped operational authority to execute data quality scans");
  }

  /**
   * Checks whether the caller persona can safely execute scans against the specified entity type.
   */
  public boolean canAccessEntityType(String entityType) {
    if (isNationwideAdmin) {
      return true;
    }
    if (entityType == null) {
      return false;
    }
    String normalized = entityType.trim().toUpperCase().replace("_", "");
    if (isEmployer) {
      // Employers are restricted to placement and employment records
      return "PLACEMENTRECORD".equals(normalized) || "EMPLOYMENTRECORD".equals(normalized);
    }
    if (isStateAdmin || isTrainingProvider) {
      return "TRAINEE".equals(normalized) ||
             "PLACEMENTRECORD".equals(normalized) ||
             "EMPLOYMENTRECORD".equals(normalized) ||
             "ATTENDANCERECORD".equals(normalized);
    }
    return false;
  }

  /**
   * Factory method for creating an unrestricted nationwide administrative scope.
   */
  public static ScanScopeContext nationwide(Long userId) {
    return new ScanScopeContext(userId, true, false, null, false, null, false, null);
  }

  /**
   * Factory method for creating a state-scoped administrative boundary.
   */
  public static ScanScopeContext stateScoped(Long userId, Long stateId) {
    return new ScanScopeContext(userId, false, true, stateId, false, null, false, null);
  }

  /**
   * Factory method for creating a training-provider-scoped boundary.
   */
  public static ScanScopeContext providerScoped(Long userId, Long providerId) {
    return new ScanScopeContext(userId, false, false, null, true, providerId, false, null);
  }

  /**
   * Factory method for creating an employer-scoped boundary.
   */
  public static ScanScopeContext employerScoped(Long userId, Long employerId) {
    return new ScanScopeContext(userId, false, false, null, false, null, true, employerId);
  }
}
