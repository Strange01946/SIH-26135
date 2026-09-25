package in.gov.sih.sih26135.security.config;

import in.gov.sih.sih26135.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration establishing the stateless HTTP request processing pipeline
 * and the JWT authentication boundary.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Stateless session management (no HTTP sessions created or used).</li>
 *   <li>Bearer token extraction and authentication via {@link JwtAuthenticationFilter}.</li>
 *   <li>Public access permitted strictly for POST /api/v1/auth/login, POST /api/v1/auth/refresh, /actuator/health, /error.</li>
 *   <li>Authentication required for GET /api/v1/auth/me and all business REST API endpoints.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Stateless bearer-token API: disable session-bound CSRF protection
        .csrf(AbstractHttpConfigurer::disable)
        // Disable interactive login mechanisms not appropriate for REST APIs
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        // Stateless session policy: no HttpSession will be created or used
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Request authorization boundary: public vs authenticated
        .authorizeHttpRequests(authorize -> authorize
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
