package in.gov.sih.sih26135.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.Collections;
import java.util.Set;
import org.mockito.Mockito;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test fixture demonstrating a package-spoofing attack against controller trust:
 * - Declares the EXACT whitelisted package: in.gov.sih.sih26135.controller
 * - Class simple name ends with the expected suffix "Controller"
 * - Annotated with @RestController
 * - Contains sensitive implementation details ("spoofed-controller-secret-token")
 * - NOT included in the authoritative TRUSTED_CONTROLLER_CLASSES allowlist.
 *
 * Verifies that GlobalExceptionHandler fails closed to HTTP 500 without leaking secrets.
 */
@RestController
@RequestMapping("/api/test/spoofed")
public class SpoofedWhitelistedPackageController {

  public static final String SENSITIVE_TOKEN = "spoofed-controller-secret-token-XYZ789";

  @GetMapping("/param-check/{id}")
  public String checkParam(@PathVariable("id") String id) {
    ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    Path.Node paramNode = Mockito.mock(Path.Node.class);
    Mockito.when(paramNode.getKind()).thenReturn(ElementKind.PARAMETER);
    Mockito.when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
    Mockito.when(path.toString()).thenReturn("spoofedParam");
    Mockito.when(violation.getPropertyPath()).thenReturn(path);
    Mockito.when(violation.getMessage()).thenReturn("Validation failed with secret " + SENSITIVE_TOKEN);
    Mockito.doReturn(SpoofedWhitelistedPackageController.class).when(violation).getRootBeanClass();

    throw new ConstraintViolationException("Spoofed validation failed", Set.of(violation));
  }
}
