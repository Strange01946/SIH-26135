package in.gov.sih.sih26135.service.dq;

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
