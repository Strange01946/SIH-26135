package in.gov.sih.sih26135.communication.template;

import in.gov.sih.sih26135.exception.BadRequestException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates notification embedded URLs against trusted government and application domains,
 * preventing open-redirect, phishing, and link-injection attacks.
 */
@Component
public class TrustedUrlValidator {

  private static final Set<String> ALLOWED_SCHEMES = Set.of("https", "http");

  private static final List<String> TRUSTED_HOST_SUFFIXES = List.of(
      "skillindia.gov.in",
      "sih.gov.in",
      "sih26135.gov.in",
      "localhost",
      "127.0.0.1"
  );

  /**
   * Validates whether a certificate, survey, or verification link is a trusted application URL.
   *
   * @param rawUrl url string to validate
   * @param paramName parameter key for exception messaging
   * @throws BadRequestException if url is missing, malformed, non-https, or untrusted
   */
  public void validateTrustedUrl(String rawUrl, String paramName) {
    if (rawUrl == null || rawUrl.isBlank()) {
      throw new BadRequestException("Parameter '" + paramName + "' must not be blank", "INVALID_URL");
    }

    String trimmed = rawUrl.trim();

    // Prevent javascript: or data: pseudo-protocols
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
      throw new BadRequestException("Dangerous URL scheme detected in parameter '" + paramName + "'", "UNTRUSTED_URL_SCHEME");
    }

    URI uri;
    try {
      uri = URI.create(trimmed);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("Malformed URL in parameter '" + paramName + "': " + ex.getMessage(), "MALFORMED_URL");
    }

    String scheme = uri.getScheme();
    if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
      throw new BadRequestException("URL scheme must be HTTP or HTTPS for parameter '" + paramName + "'", "INVALID_URL_SCHEME");
    }

    // Require HTTPS unless connecting to local loopback
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new BadRequestException("URL host is missing in parameter '" + paramName + "'", "MISSING_URL_HOST");
    }

    String hostLower = host.toLowerCase(Locale.ROOT);
    boolean isLocal = hostLower.equals("localhost") || hostLower.equals("127.0.0.1");

    if (!isLocal && !"https".equalsIgnoreCase(scheme)) {
      throw new BadRequestException("Public application URLs must use HTTPS for parameter '" + paramName + "'", "HTTPS_REQUIRED");
    }

    // Disallow user-info (e.g. https://user:pass@host)
    if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
      throw new BadRequestException("URL must not contain embedded user credentials in parameter '" + paramName + "'", "CREDENTIALS_IN_URL");
    }

    // Verify host against trusted domains
    boolean isTrustedHost = false;
    for (String suffix : TRUSTED_HOST_SUFFIXES) {
      if (hostLower.equals(suffix) || hostLower.endsWith("." + suffix)) {
        isTrustedHost = true;
        break;
      }
    }

    if (!isTrustedHost) {
      throw new BadRequestException(
          "URL host '" + host + "' is not on the trusted application domain allowlist for parameter '" + paramName + "'",
          "UNTRUSTED_URL_HOST"
      );
    }
  }
}
