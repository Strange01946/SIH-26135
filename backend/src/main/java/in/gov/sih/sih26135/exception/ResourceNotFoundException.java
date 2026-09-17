package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found (HTTP 404).
 */
public class ResourceNotFoundException extends BaseException {

  public ResourceNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
  }

  public ResourceNotFoundException(String resourceName, String fieldName) {
    super(String.format("%s not found with %s", resourceName, fieldName),
        HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
  }
}
