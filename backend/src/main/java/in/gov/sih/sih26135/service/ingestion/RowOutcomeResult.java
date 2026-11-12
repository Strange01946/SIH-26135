package in.gov.sih.sih26135.service.ingestion;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-chunk outcome result and bounded intra-chunk uniqueness tracker cleared after every chunk.
 */
public final class RowOutcomeResult {

  public enum OutcomeType {
    SUCCESS,
    FAILED,
    DUPLICATE,
    SKIPPED
  }

  private final OutcomeType outcomeType;
  private final String entityType;
  private final Long entityId;
  private final String errorCode;
  private final String errorMessage;

  private RowOutcomeResult(
      OutcomeType outcomeType,
      String entityType,
      Long entityId,
      String errorCode,
      String errorMessage) {
    this.outcomeType = outcomeType;
    this.entityType = entityType;
    this.entityId = entityId;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  public static RowOutcomeResult success(String entityType, Long entityId) {
    return new RowOutcomeResult(OutcomeType.SUCCESS, entityType, entityId, null, null);
  }

  public static RowOutcomeResult failed(String entityType, String errorCode, String errorMessage) {
    return new RowOutcomeResult(
        OutcomeType.FAILED,
        entityType,
        null,
        ImportErrorSanitizer.sanitizeErrorCode(errorCode, "VALIDATION_ERROR"),
        ImportErrorSanitizer.sanitizeErrorMessage(errorMessage, "Row validation failed"));
  }

  public static RowOutcomeResult duplicate(String entityType, String errorCode, String errorMessage) {
    return new RowOutcomeResult(
        OutcomeType.DUPLICATE,
        entityType,
        null,
        ImportErrorSanitizer.sanitizeErrorCode(errorCode, "DUPLICATE_RECORD"),
        ImportErrorSanitizer.sanitizeErrorMessage(errorMessage, "Duplicate record detected"));
  }

  public static RowOutcomeResult skipped(String entityType, String errorCode, String errorMessage) {
    return new RowOutcomeResult(
        OutcomeType.SKIPPED,
        entityType,
        null,
        ImportErrorSanitizer.sanitizeErrorCode(errorCode, "EMPTY_ROW"),
        ImportErrorSanitizer.sanitizeErrorMessage(errorMessage, "Row skipped"));
  }

  public OutcomeType outcomeType() {
    return outcomeType;
  }

  public String entityType() {
    return entityType;
  }

  public Long entityId() {
    return entityId;
  }

  public String errorCode() {
    return errorCode;
  }

  public String errorMessage() {
    return errorMessage;
  }

  /**
   * Bounded intra-chunk unique-key tracker preventing intra-batch duplicate collisions prior to flush.
   */
  public static final class ChunkSeenKeys {
    public final Set<String> traineeRegNumbers = new HashSet<>();
    public final Set<Long> traineeUserIds = new HashSet<>();
    public final Set<String> traineeIdentitySignals = new HashSet<>();
    public final Set<String> placementNumbers = new HashSet<>();
    public final Set<Long> placementJobAppIds = new HashSet<>();
    public final Set<String> placementEnrollmentPostingKeys = new HashSet<>();
  }
}
