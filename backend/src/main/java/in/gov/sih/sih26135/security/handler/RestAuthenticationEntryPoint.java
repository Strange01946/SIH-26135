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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Standardized Spring Security {@link AuthenticationEntryPoint} that intercepts unauthenticated
 * HTTP requests to protected resources and returns a sanitized JSON {@link ApiResponse} envelope with HTTP 401.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Consistent JSON response format across all unauthenticated access attempts.</li>
 *   <li>Generic, safe message preventing user enumeration or leaking token/key material.</li>
 *   <li>Strictly zero exposure of stack traces, token signatures, or internal exceptions.</li>
 * </ul>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

  public static final String ERROR_CODE = "UNAUTHORIZED";
  public static final String DEFAULT_MESSAGE = "Authentication is required to access this resource";

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) throws IOException, ServletException {

    String path = request.getRequestURI();
    log.warn("Unauthenticated access attempt to {}", path);

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    ApiResponse<Void> apiResponse = ApiResponse.error(ERROR_CODE, DEFAULT_MESSAGE, path);

    objectMapper.writeValue(response.getWriter(), apiResponse);
  }
}
