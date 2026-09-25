package in.gov.sih.sih26135.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralized exception handler for REST APIs.
 * Ensures consistent, sanitized error responses without leaking sensitive internal details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
    log.warn("Application exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(
        ex.getStatus().value(),
        ex.getStatus().getReasonPhrase(),
        ex.getErrorCode(),
        ex.getMessage(),
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, ex.getStatus());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> validationErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .toList();

    log.warn("Validation error on {}: {}", request.getRequestURI(), validationErrors);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "VALIDATION_ERROR",
        "Request validation failed",
        request.getRequestURI(),
        validationErrors
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.warn("Malformed HTTP message on {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "MALFORMED_JSON",
        "Malformed or unreadable request body",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    log.warn("Method not supported on {}: {}", request.getRequestURI(), ex.getMethod());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.METHOD_NOT_ALLOWED.value(),
        HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
        "METHOD_NOT_ALLOWED",
        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
    log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getResourcePath());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        "ENDPOINT_NOT_FOUND",
        "No static resource or endpoint found for path: " + request.getRequestURI(),
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public void handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
    throw ex;
  }

  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public void handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
    throw ex;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception processing request to {}", request.getRequestURI(), ex);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred. Please try again later.",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
