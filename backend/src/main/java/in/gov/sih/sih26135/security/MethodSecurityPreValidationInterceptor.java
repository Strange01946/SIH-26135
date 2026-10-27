package in.gov.sih.sih26135.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces method-level authorization before controller argument resolution and Bean Validation.
 * Uses Spring Security's native {@link PreAuthorizeAuthorizationManager} to ensure standard SpEL
 * expression evaluation, role hierarchy support, and security context integration.
 *
 * This ensures that unauthorized requests are rejected with 403 Forbidden prior to payload validation,
 * preventing data disclosure and upholding authorization boundary invariants.
 */
@Component
public class MethodSecurityPreValidationInterceptor implements HandlerInterceptor {

  private final ApplicationContext applicationContext;
  private final PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager;
  private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

  public MethodSecurityPreValidationInterceptor(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    this.preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();
    this.preAuthorizeAuthorizationManager.setApplicationContext(applicationContext);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    // Check if the method or bean class is annotated with @PreAuthorize
    PreAuthorize preAuth = handlerMethod.getMethodAnnotation(PreAuthorize.class);
    if (preAuth == null) {
      preAuth = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
    }

    if (preAuth == null || preAuth.value().isBlank()) {
      return true;
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || trustResolver.isAnonymous(auth)) {
      // Allow unauthenticated / anonymous requests through to Spring Security's authentication entry point (401 Unauthorized)
      return true;
    }

    MethodInvocation invocation = new HandlerMethodInvocation(handlerMethod, applicationContext);
    AuthorizationDecision decision = preAuthorizeAuthorizationManager.check(() -> auth, invocation);

    if (decision != null && !decision.isGranted()) {
      throw new AccessDeniedException("Access denied");
    }

    return true;
  }

  /**
   * Adapter bridging Spring MVC HandlerMethod to AOP Alliance MethodInvocation for Spring Security evaluation.
   */
  static class HandlerMethodInvocation implements MethodInvocation {
    private final HandlerMethod handlerMethod;
    private final ApplicationContext applicationContext;

    HandlerMethodInvocation(HandlerMethod handlerMethod, ApplicationContext applicationContext) {
      this.handlerMethod = handlerMethod;
      this.applicationContext = applicationContext;
    }

    @Override
    public Method getMethod() {
      return handlerMethod.getMethod();
    }

    @Override
    public Object[] getArguments() {
      return new Object[0];
    }

    @Override
    public Object getThis() {
      Object bean = handlerMethod.getBean();
      if (bean instanceof String beanName) {
        return applicationContext.getBean(beanName);
      }
      return bean;
    }

    @Override
    public AccessibleObject getStaticPart() {
      return handlerMethod.getMethod();
    }

    @Override
    public Object proceed() {
      throw new UnsupportedOperationException("proceed() is not supported in pre-validation authorization check");
    }
  }
}
