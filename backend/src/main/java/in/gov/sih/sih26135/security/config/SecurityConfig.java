package in.gov.sih.sih26135.security.config;

import in.gov.sih.sih26135.security.handler.RestAccessDeniedHandler;
import in.gov.sih.sih26135.security.handler.RestAuthenticationEntryPoint;
import in.gov.sih.sih26135.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration establishing the stateless HTTP request processing pipeline,
 * security exception handling, method-level authorization, production CORS, and HTTP security response headers.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Stateless session management (no HTTP sessions created or used).</li>
 *   <li>Standardized JSON error handling for 401 Unauthorized via {@link RestAuthenticationEntryPoint}.</li>
 *   <li>Standardized JSON error handling for 403 Forbidden via {@link RestAccessDeniedHandler}.</li>
 *   <li>Method-level RBAC authorization via {@link EnableMethodSecurity}.</li>
 *   <li>Bearer token extraction and authentication via {@link JwtAuthenticationFilter}.</li>
 *   <li>Explicit, environment-backed CORS validation via {@link CorsConfigurationSource}.</li>
 *   <li>Production HTTP security response headers (nosniff, frame-deny, CSP, Referrer-Policy, HSTS).</li>
 *   <li>Public access permitted strictly for POST /api/v1/auth/login, POST /api/v1/auth/refresh, /actuator/health, /error.</li>
 *   <li>Authentication required for GET /api/v1/auth/me and all business REST API endpoints.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
  private final RestAccessDeniedHandler restAccessDeniedHandler;
  private final CorsConfigurationSource corsConfigurationSource;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RestAuthenticationEntryPoint restAuthenticationEntryPoint,
      RestAccessDeniedHandler restAccessDeniedHandler,
      CorsConfigurationSource corsConfigurationSource) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    this.restAccessDeniedHandler = restAccessDeniedHandler;
    this.corsConfigurationSource = corsConfigurationSource;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Stateless bearer-token API: disable session-bound CSRF protection
        .csrf(AbstractHttpConfigurer::disable)
        // Explicit production CORS configuration backed by environment properties
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        // Disable interactive login mechanisms not appropriate for REST APIs
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        // Stateless session policy: no HttpSession will be created or used
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Production HTTP Security Response Headers
        .headers(headers -> headers
            .contentTypeOptions(Customizer.withDefaults())
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
            )
        )
        // Standardized security exception handling
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(restAuthenticationEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler)
        )
        // Request authorization boundary: public vs authenticated
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
            .requestMatchers("/api/v1/auth/me").authenticated()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/error").permitAll()
            .anyRequest().authenticated()
        )
        // Register JWT authentication filter before the standard username/password filter
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
