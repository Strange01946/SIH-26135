package in.gov.sih.sih26135.correlation;

/**
 * Thread-local context holder for the current HTTP request's correlation ID.
 * Provides thread-bound access to the canonical correlation identifier across
 * domain services, controllers, exception handlers, security components, and response envelopes.
 */
public final class CorrelationContextHolder {

  private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();
  private static final ThreadLocal<String> CURRENT_REQUEST_ID = new ThreadLocal<>();

  private CorrelationContextHolder() {
    // Utility class; prevent instantiation
  }

  /**
   * Retrieves the correlation ID bound to the current thread.
   *
   * @return the current correlation ID, or {@code null} if no context is active
   */
  public static String getCorrelationId() {
    return CURRENT_CORRELATION_ID.get();
  }

  /**
   * Binds a correlation ID to the current thread.
   *
   * @param correlationId the sanitized correlation identifier
   */
  public static void setCorrelationId(String correlationId) {
    if (correlationId == null || correlationId.isBlank()) {
      CURRENT_CORRELATION_ID.remove();
    } else {
      CURRENT_CORRELATION_ID.set(correlationId);
    }
  }

  /**
   * Retrieves the request ID bound to the current thread, or falls back to correlation ID if unset.
   *
   * @return the current request ID, or {@code null} if none active
   */
  public static String getRequestId() {
    String req = CURRENT_REQUEST_ID.get();
    return req != null ? req : CURRENT_CORRELATION_ID.get();
  }

  /**
   * Retrieves only the explicit request ID bound to the current thread without fallback.
   */
  public static String getExplicitRequestId() {
    return CURRENT_REQUEST_ID.get();
  }

  /**
   * Binds a request ID to the current thread.
   *
   * @param requestId the sanitized request identifier
   */
  public static void setRequestId(String requestId) {
    if (requestId == null || requestId.isBlank()) {
      CURRENT_REQUEST_ID.remove();
    } else {
      CURRENT_REQUEST_ID.set(requestId);
    }
  }

  /**
   * Clears both correlation and request IDs bound to the current thread.
   * Must always be invoked in a fail-safe finally block to prevent thread-local leakage.
   */
  public static void clear() {
    CURRENT_CORRELATION_ID.remove();
    CURRENT_REQUEST_ID.remove();
  }

  /**
   * Immutable snapshot of the current thread's correlation and request identifiers.
   */
  public record ContextSnapshot(String correlationId, String explicitRequestId) {}

  /**
   * Captures the current thread's correlation context for safe restoration after scoped execution.
   */
  public static ContextSnapshot captureSnapshot() {
    return new ContextSnapshot(CURRENT_CORRELATION_ID.get(), CURRENT_REQUEST_ID.get());
  }

  /**
   * Restores a previously captured correlation context snapshot (clearing if empty).
   */
  public static void restore(ContextSnapshot snapshot) {
    if (snapshot == null) {
      clear();
      return;
    }
    setCorrelationId(snapshot.correlationId());
    setRequestId(snapshot.explicitRequestId());
  }
}
