package in.gov.sih.sih26135.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter establishing request correlation tracking across the entire HTTP lifecycle.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Runs at {@link Ordered#HIGHEST_PRECEDENCE} before Spring Security and DispatcherServlet.</li>
 *   <li>Canonical correlation header {@code X-Correlation-Id} with fallback to {@code X-Request-Id}.</li>
 *   <li>Strict header sanitization via {@code ^[A-Za-z0-9_-]{1,64}$} preventing log injection and response splitting.</li>
 *   <li>Safe UUID generation when headers are absent, malformed, empty, or oversized.</li>
 *   <li>Echoes sanitized correlation ID in both response headers ({@code X-Correlation-Id} and {@code X-Request-Id}).</li>
 *   <li>Populates {@link CorrelationContextHolder} and SLF4J MDC (keys: {@code correlationId}, {@code requestId}).</li>
 *   <li>Guaranteed thread-local and MDC cleanup in fail-safe {@code finally} block.</li>
 *   <li>Zero exposure of untrusted raw header input in logs or responses.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter implements Ordered {

  private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String CORRELATION_ID_ATTRIBUTE = "in.gov.sih.sih26135.correlationId";
  public static final String REQUEST_ID_ATTRIBUTE = "in.gov.sih.sih26135.requestId";

  public static final String MDC_CORRELATION_ID_KEY = "correlationId";
  public static final String MDC_REQUEST_ID_KEY = "requestId";

  private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  /**
   * Overridden to false to ensure error dispatches (e.g. forward to /error upon container-level exceptions)
   * re-establish the correlation context using the initial request attribute.
   */
  @Override
  protected boolean shouldNotFilterErrorDispatch() {
    return false;
  }

  /**
   * Remains true (default) because the SIH-26135 backend uses strictly synchronous Spring MVC REST controllers.
   * No asynchronous servlet processing (DeferredResult, CompletableFuture, SSE) is present or required.
   */
  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return true;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String previousCorrelationId = MDC.get(MDC_CORRELATION_ID_KEY);
    String previousRequestId = MDC.get(MDC_REQUEST_ID_KEY);
    String previousContextHolderId = CorrelationContextHolder.getCorrelationId();
    String previousContextRequestId = CorrelationContextHolder.getExplicitRequestId();

    ResolvedTracingIds resolved = resolveTracingIds(request);
    String correlationId = resolved.correlationId();
    String requestId = resolved.requestId();

    // Bind to request attributes, context holder, and SLF4J MDC
    request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
    request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
    CorrelationContextHolder.setCorrelationId(correlationId);
    CorrelationContextHolder.setRequestId(requestId);
    MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
    MDC.put(MDC_REQUEST_ID_KEY, requestId);

    // Set response headers upfront before any downstream filter or handler writes to response
    response.setHeader(CORRELATION_ID_HEADER, correlationId);
    response.setHeader(REQUEST_ID_HEADER, requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      // Restore pre-existing context if present (e.g. upstream tracing), or purge to prevent pooled thread leaks
      if (previousContextHolderId != null) {
        CorrelationContextHolder.setCorrelationId(previousContextHolderId);
      } else {
        CorrelationContextHolder.setCorrelationId(null);
      }

      if (previousContextRequestId != null) {
        CorrelationContextHolder.setRequestId(previousContextRequestId);
      } else {
        CorrelationContextHolder.setRequestId(null);
      }

      if (previousCorrelationId != null) {
        MDC.put(MDC_CORRELATION_ID_KEY, previousCorrelationId);
      } else {
        MDC.remove(MDC_CORRELATION_ID_KEY);
      }

      if (previousRequestId != null) {
        MDC.put(MDC_REQUEST_ID_KEY, previousRequestId);
      } else {
        MDC.remove(MDC_REQUEST_ID_KEY);
      }
    }
  }

  private record ResolvedTracingIds(String correlationId, String requestId) {}

  /**
   * Resolves and independently validates correlation and request identifiers from incoming
   * headers (or prior error-dispatch attributes), preserving single-header fallbacks and
   * generating safe server UUIDs when required.
   */
  private ResolvedTracingIds resolveTracingIds(HttpServletRequest request) {
    Object existingCorrAttr = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
    Object existingReqAttr = request.getAttribute(REQUEST_ID_ATTRIBUTE);
    if (existingCorrAttr instanceof String existingCorr && isValidCorrelationId(existingCorr)) {
      String resolvedReq = (existingReqAttr instanceof String existingReq && isValidCorrelationId(existingReq))
          ? existingReq
          : existingCorr;
      return new ResolvedTracingIds(existingCorr, resolvedReq);
    }

    String rawCorrelationId = request.getHeader(CORRELATION_ID_HEADER);
    String rawRequestId = request.getHeader(REQUEST_ID_HEADER);
    boolean validCorr = isValidCorrelationId(rawCorrelationId);
    boolean validReq = isValidCorrelationId(rawRequestId);

    if (validCorr && validReq) {
      return new ResolvedTracingIds(rawCorrelationId, rawRequestId);
    }
    if (validCorr && rawRequestId == null) {
      // Compatible fallback when only X-Correlation-Id is supplied
      return new ResolvedTracingIds(rawCorrelationId, rawCorrelationId);
    }
    if (rawCorrelationId == null && validReq) {
      // Compatible fallback when only X-Request-Id is supplied
      return new ResolvedTracingIds(rawRequestId, rawRequestId);
    }

    if (rawCorrelationId != null || rawRequestId != null) {
      log.debug("Invalid or untrusted correlation/request ID header received; generated safe server identifier.");
    }

    if (rawCorrelationId == null && rawRequestId == null) {
      String generated = UUID.randomUUID().toString();
      return new ResolvedTracingIds(generated, generated);
    }

    // One header supplied and invalid, or both invalid: independently replace invalid value(s)
    String effectiveCorr = validCorr ? rawCorrelationId : UUID.randomUUID().toString();
    String effectiveReq;
    if (validReq) {
      effectiveReq = rawRequestId;
    } else if (!validCorr) {
      effectiveReq = (rawRequestId == null) ? effectiveCorr : UUID.randomUUID().toString();
    } else {
      effectiveReq = UUID.randomUUID().toString();
    }
    return new ResolvedTracingIds(effectiveCorr, effectiveReq);
  }

  /**
   * Validates whether the given identifier matches safe alphanumeric, dash, and underscore format
   * within a strict length bound [1, 64].
   *
   * <p>Enforces strict fail-closed rejection: Any whitespace (leading, trailing, or embedded),
   * CRLF, special characters, or oversized values are strictly rejected without normalization.
   *
   * @param id the candidate identifier string
   * @return {@code true} if safe and valid; {@code false} otherwise
   */
  public static boolean isValidCorrelationId(String id) {
    if (id == null || id.isEmpty() || id.length() > 64) {
      return false;
    }
    return VALID_ID_PATTERN.matcher(id).matches();
  }
}
