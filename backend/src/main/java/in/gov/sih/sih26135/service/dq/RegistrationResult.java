package in.gov.sih.sih26135.service.dq;

/**
 * Concurrency and deduplication result representing the outcome of issue registration or update.
 */
public record RegistrationResult(
    RegistrationStatus status,
    Long issueId,
    String message
) {

  public boolean isCreated() {
    return status == RegistrationStatus.CREATED;
  }

  public boolean isUpdated() {
    return status == RegistrationStatus.UPDATED;
  }

  public boolean isDeduplicated() {
    return status == RegistrationStatus.DEDUPLICATED;
  }

  public boolean isLockFailed() {
    return status == RegistrationStatus.LOCK_FAILED;
  }

  public boolean isDatabaseError() {
    return status == RegistrationStatus.DATABASE_ERROR;
  }

  public boolean isFailure() {
    return status == RegistrationStatus.LOCK_FAILED || status == RegistrationStatus.DATABASE_ERROR;
  }

  public static RegistrationResult created(Long id) {
    return new RegistrationResult(RegistrationStatus.CREATED, id, "Issue created");
  }

  public static RegistrationResult updated(Long id) {
    return new RegistrationResult(RegistrationStatus.UPDATED, id, "Existing open issue updated with new findings");
  }

  public static RegistrationResult deduplicated(Long id) {
    return new RegistrationResult(RegistrationStatus.DEDUPLICATED, id, "Active open issue already exists with identical findings");
  }

  public static RegistrationResult lockFailed(String message) {
    return new RegistrationResult(RegistrationStatus.LOCK_FAILED, null, message);
  }

  public static RegistrationResult databaseError(String message) {
    return new RegistrationResult(RegistrationStatus.DATABASE_ERROR, null, message);
  }
}
