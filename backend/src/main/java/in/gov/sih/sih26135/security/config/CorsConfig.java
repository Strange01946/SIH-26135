package in.gov.sih.sih26135.security.config;

import java.util.Collections;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring configuration providing the {@link CorsConfigurationSource} bean.
 *
 * <p>Registers environment-backed CORS settings across all application endpoints ("/**").
 * When no origins are configured, an empty origin list is registered, effectively enforcing
 * a default-deny policy for all cross-origin browser requests.
 */
@Configuration
public class CorsConfig {

  private final SecurityCorsProperties corsProperties;

  public CorsConfig(SecurityCorsProperties corsProperties) {
    this.corsProperties = corsProperties;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    List<String> origins = corsProperties.getAllowedOrigins();
    if (!origins.isEmpty()) {
      configuration.setAllowedOrigins(origins);
    } else {
      // Explicitly register empty list so no cross-origin requests are permitted by default
      configuration.setAllowedOrigins(Collections.emptyList());
    }

    configuration.setAllowedMethods(corsProperties.getAllowedMethods());
    configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
    configuration.setExposedHeaders(corsProperties.getExposedHeaders());
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    if (corsProperties.getMaxAge() != null) {
      configuration.setMaxAge(corsProperties.getMaxAge().getSeconds());
    }

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
