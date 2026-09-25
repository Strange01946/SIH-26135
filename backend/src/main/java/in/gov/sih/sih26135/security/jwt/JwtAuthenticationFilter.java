package in.gov.sih.sih26135.security.jwt;

import in.gov.sih.sih26135.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that processes HTTP requests carrying JWT bearer tokens in the Authorization header.
 *
 * <p>Validates incoming tokens strictly as ACCESS tokens using {@link JwtTokenProvider}.
 * Upon successful cryptographic verification and claim validation, populates Spring Security's
 * {@link SecurityContextHolder} with an authenticated {@link UsernamePasswordAuthenticationToken}.
 *
 * <p>Refresh tokens, expired tokens, tampered tokens, or malformed tokens are strictly rejected
 * from authenticating API requests.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    // Avoid repeatedly replacing an existing authenticated Authentication
    if (SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // Validate specifically as an ACCESS token.
      // Enforces signature, expiration, issuer, required claims, and token_type == "access".
      jwtTokenProvider.parseAndValidateToken(token, JwtTokenProvider.TOKEN_TYPE_ACCESS);

      Long userId = jwtTokenProvider.extractUserId(token);
      String username = jwtTokenProvider.extractUsername(token);
      List<String> authorityStrings = jwtTokenProvider.extractAuthorities(token);

      List<SimpleGrantedAuthority> authorities = authorityStrings.stream()
          .map(SimpleGrantedAuthority::new)
          .toList();

      UserPrincipal principal = new UserPrincipal(userId, username);

      UsernamePasswordAuthenticationToken authentication =
          UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (JwtValidationException e) {
      // Invalid, expired, malformed token, or refresh token presented as access token:
      // Clear security context so Spring Security's authorization filter rejects protected routes.
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}
