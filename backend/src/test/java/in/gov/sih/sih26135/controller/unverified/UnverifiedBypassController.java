package in.gov.sih.sih26135.controller.unverified;

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
 * Unverified controller located in an unverified subpackage under controller:
 * in.gov.sih.sih26135.controller.unverified
 * Annotated with @RestController, attempting namespace prefix bypass.
 * Must fail closed to HTTP 500.
 */
@RestController
@RequestMapping("/api/test/bypass")
public class UnverifiedBypassController {

  @GetMapping("/attempt/{id}")
  public String attempt(@PathVariable("id") String id) {
    ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    Path.Node paramNode = Mockito.mock(Path.Node.class);
    Mockito.when(paramNode.getKind()).thenReturn(ElementKind.PARAMETER);
    Mockito.when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
    Mockito.when(path.toString()).thenReturn("id");
    Mockito.when(violation.getPropertyPath()).thenReturn(path);
    Mockito.when(violation.getMessage()).thenReturn("bypass attempt validation failure");
    Mockito.doReturn(UnverifiedBypassController.class).when(violation).getRootBeanClass();

    throw new ConstraintViolationException("Bypass failure", Set.of(violation));
  }
}
