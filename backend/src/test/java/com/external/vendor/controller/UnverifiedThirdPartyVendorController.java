package com.external.vendor.controller;

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
 * Unverified third-party vendor controller in external package:
 * com.external.vendor.controller
 * Annotated with @RestController, but must fail closed to HTTP 500 without leaking vendor secret.
 */
@RestController
@RequestMapping("/api/test/thirdparty")
public class UnverifiedThirdPartyVendorController {

  public static final String SENSITIVE_VENDOR_SECRET = "secret-vendor-token-ABC999-restricted";

  @GetMapping("/vendor-action/{id}")
  public String vendorAction(@PathVariable("id") String id) {
    ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    Path.Node paramNode = Mockito.mock(Path.Node.class);
    Mockito.when(paramNode.getKind()).thenReturn(ElementKind.PARAMETER);
    Mockito.when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
    Mockito.when(path.toString()).thenReturn("id");
    Mockito.when(violation.getPropertyPath()).thenReturn(path);
    Mockito.when(violation.getMessage()).thenReturn("Validation failure exposing: " + SENSITIVE_VENDOR_SECRET);
    Mockito.doReturn(UnverifiedThirdPartyVendorController.class).when(violation).getRootBeanClass();

    throw new ConstraintViolationException("Vendor internal constraint failed", Set.of(violation));
  }
}
