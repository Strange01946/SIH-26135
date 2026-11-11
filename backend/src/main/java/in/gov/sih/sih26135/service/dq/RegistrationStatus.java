package in.gov.sih.sih26135.service.dq;

/**
 * Outcome status of a data quality issue registration or update evaluation.
 */
public enum RegistrationStatus {
  /** A new DataQualityIssue was successfully created and committed. */
  CREATED,

  /** An existing active OPEN DataQualityIssue was updated with newly discovered findings. */
  UPDATED,

  /** An existing active OPEN DataQualityIssue already contains identical findings; skipped. */
  DEDUPLICATED,

  /** Distributed named lock acquisition failed, timed out, or returned null. */
  LOCK_FAILED,

  /** Database query failure occurred during deduplication verification. */
  DATABASE_ERROR
}
