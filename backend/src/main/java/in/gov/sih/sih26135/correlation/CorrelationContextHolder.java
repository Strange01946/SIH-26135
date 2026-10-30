package in.gov.sih.sih26135.correlation;

/**
 * Thread-local context holder for the current HTTP request's correlation ID.
 * Provides thread-bound access to the canonical correlation identifier across
 * domain services, controllers, exception handlers, security components, and response envelopes.
 */
public final class CorrelationContextHolder {

  private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();

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
   * Clears the correlation ID bound to the current thread.
   * Must always be invoked in a fail-safe finally block to prevent thread-local leakage.
   */
  public static void clear() {
    CURRENT_CORRELATION_ID.remove();
  }
}
