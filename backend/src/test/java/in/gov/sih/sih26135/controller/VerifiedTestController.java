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
 * Test controller in the verified application controller package:
 * in.gov.sih.sih26135.controller
 * Annotated with @RestController and class name ends with "Controller".
 */
@RestController
@RequestMapping("/api/test/verified")
public class VerifiedTestController {

  @GetMapping("/param-check/{id}")
  public String checkParam(@PathVariable("id") String id) {
    ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    Path.Node paramNode = Mockito.mock(Path.Node.class);
    Mockito.when(paramNode.getKind()).thenReturn(ElementKind.PARAMETER);
    Mockito.when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
    Mockito.when(path.toString()).thenReturn("id");
    Mockito.when(violation.getPropertyPath()).thenReturn(path);
    Mockito.when(violation.getMessage()).thenReturn("must be a positive number");
    Mockito.doReturn(in.gov.sih.sih26135.controller.TraineeController.class).when(violation).getRootBeanClass();

    throw new ConstraintViolationException("Parameter validation failed", Set.of(violation));
  }

  public static class ValidatedDto {
    @jakarta.validation.constraints.NotBlank(message = "code must not be blank")
    private String code;

    public ValidatedDto() {}

    public ValidatedDto(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }
  }

  @org.springframework.web.bind.annotation.PostMapping("/dto-check")
  public String checkDto(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ValidatedDto dto) {
    return "ok";
  }
}
