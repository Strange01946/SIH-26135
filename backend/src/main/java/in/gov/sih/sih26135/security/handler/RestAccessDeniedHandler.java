package in.gov.sih.sih26135.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Standardized Spring Security {@link AccessDeniedHandler} that intercepts authorized requests
 * where the authenticated identity lacks sufficient permissions, returning a sanitized JSON {@link ApiResponse}
 * envelope with HTTP 403.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Consistent JSON response format across all forbidden access attempts.</li>
 *   <li>Generic, safe message ("Access denied") preventing disclosure of authorization boundaries.</li>
 *   <li>Strictly zero exposure of internal role hierarchies, stack traces, or framework details.</li>
 * </ul>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

  public static final String ERROR_CODE = "FORBIDDEN";
  public static final String DEFAULT_MESSAGE = "Access denied";

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException, ServletException {

    String path = request.getRequestURI();
    log.warn("Access denied for request to {}", path);

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    ApiResponse<Void> apiResponse = ApiResponse.error(ERROR_CODE, DEFAULT_MESSAGE, path);

    objectMapper.writeValue(response.getWriter(), apiResponse);
  }
}
