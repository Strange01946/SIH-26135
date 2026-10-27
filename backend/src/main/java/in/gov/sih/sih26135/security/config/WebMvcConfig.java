package in.gov.sih.sih26135.security.config;

import in.gov.sih.sih26135.security.MethodSecurityPreValidationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration registering security and request processing interceptors.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final MethodSecurityPreValidationInterceptor methodSecurityPreValidationInterceptor;

  public WebMvcConfig(MethodSecurityPreValidationInterceptor methodSecurityPreValidationInterceptor) {
    this.methodSecurityPreValidationInterceptor = methodSecurityPreValidationInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(methodSecurityPreValidationInterceptor);
  }
}
