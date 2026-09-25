package in.gov.sih.sih26135.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration bean definition for PasswordEncoder.
 *
 * <p>Configures a production-grade Argon2id password encoder compliant with RFC 9106.
 * Parameters: salt length 16 bytes, hash length 32 bytes, parallelism 1, memory 16384 KiB (16 MiB), iterations 2.
 */
@Configuration
public class PasswordEncoderConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }
}
