package in.gov.sih.sih26135.exception;

import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralized exception handler for REST APIs.
 * Ensures consistent, sanitized error responses without leaking sensitive internal details.
 * <p>
 * Employs safe-by-construction design: client-facing messages and validation details
 * are derived strictly from code allowlists, verified domain messages, and safe templates.
 * Arbitrary exception messages and rejected user inputs are never exposed directly.
 * Regular expression scanning acts as an additional defense-in-depth boundary layer.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Defense-in-depth regex pattern for detecting potential sensitive data patterns.
   * Note: This is an additional validation safeguard, not a substitute for safe-by-construction design.
   */
  private static final Pattern SENSITIVE_DATA_PATTERN = Pattern.compile(
      "(?i)(?s)(?:"
          // 1. Sentinels & Test Markers
          + "SENTINEL[A-Za-z0-9_]*"

          // 2. Aadhaar: 12 contiguous digits or 4-4-4 formatted with space or hyphen
          + "|(?<!\\d)(?:\\d{12}|\\d{4}[\\s-]\\d{4}[\\s-]\\d{4})(?!\\d)"

          // 3. Indian PAN: 5 letters, 4 digits, 1 letter
          + "|(?<![A-Za-z0-9])[A-Za-z]{5}[0-9]{4}[A-Za-z](?![A-Za-z0-9])"

          // 4. JWT & Bearer Tokens
          + "|\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}(?:\\.[A-Za-z0-9_-]*)?\\b"
          + "|\\bBearer\\s+[A-Za-z0-9_.-]{10,}\\b"

          // 5. Passwords, Secrets, API Keys & Credentials
          + "|\\b(?:password|passwd|pwd|secret|api_?key|access_token|refresh_token|client_secret|auth_token|private_key)\\s*[:=]\\s*[^\\s,;]+"
          + "|\\b(?:password|passwd|pwd|secret|credential|token)\\s*[:=]?\\s*['\"][^'\"]+['\"]"

          // 6. Database Connection URLs & Credentials
          + "|jdbc:[^\\s,;]+"
          + "|(?:mysql|postgresql|oracle|sqlserver)://[^\\s,;]+"
          + "|\\b(?:user|username|password)=[^\\s,;]+"

          // 7. SQL Statements, Fragments & DB Integrity Details
          + "|\\b(?:SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE)\\b[^;]{1,250}?\\b(?:FROM|INTO|SET|TABLE|WHERE|VALUES)\\b"
          + "|\\b(?:Duplicate\\s+entry|foreign\\s+key\\s+constraint|Column\\s+'.*'\\s+cannot\\s+be\\s+null)\\b"
          + "|\\b(?:uk_[a-z0-9_]+|fk_[a-z0-9_]+|pk_[a-z0-9_]+)\\b"

          // 8. Hibernate / JPA Internals
          + "|\\borg\\.hibernate\\b"
          + "|\\bHHH\\d+\\b"
          + "|\\bcould\\s+not\\s+execute\\s+statement\\b"

          // 9. Filesystem Paths (Windows & Unix)
          + "|[A-Za-z]:\\\\[^\\s,;]+"
          + "|/(?:etc|var|usr|opt|home|app|bin|tmp)/[^\\s,;]+"

          // 10. Java Package & Class Names
          + "|\\b(?:in\\.gov\\.sih|org\\.springframework|com\\.fasterxml\\.jackson|com\\.mysql|org\\.apache)[a-zA-Z0-9_.]*"

          // 11. Stack Traces & Source Locations
          + "|\\bat\\s+[a-zA-Z0-9_$.]+\\([a-zA-Z0-9_$]+\\.java:\\d+\\)"
          + "|\\bCaused\\s+by:\\s+"
          + "|\\b[a-zA-Z0-9_$]+\\.java:\\d+\\b"

          // 12. IP Addresses & Internal Hostnames
          + "|\\blocalhost:\\d+\\b"
          + "|\\b127\\.0\\.0\\.1\\b"
          + "|\\b10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"
          + "|\\b172\\.(?:1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}\\b"
          + "|\\b192\\.168\\.\\d{1,3}\\.\\d{1,3}\\b"
          + "|\\b[a-zA-Z0-9-]+\\.internal\\b"
          + ")"
  );

  private boolean containsSensitiveData(String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    if (text.toUpperCase().contains("SENTINEL")) {
      return true;
    }
    return SENSITIVE_DATA_PATTERN.matcher(text).find();
  }

  private boolean isTrustedApiPrefix(String uri) {
    if (uri == null || uri.isBlank()) {
      return false;
    }
    return uri.startsWith("/api/v1/")
        || uri.startsWith("/api/auth/")
        || uri.startsWith("/api/test/")
        || uri.equals("/api/v1")
        || uri.equals("/api/auth")
        || uri.equals("/api/test");
  }

  private String sanitizeUri(HttpServletRequest request) {
    if (request == null) {
      return "/sanitized-path";
    }
    return sanitizeUri(request.getRequestURI(), request);
  }

  private String sanitizeUri(String rawUri) {
    return sanitizeUri(rawUri, null);
  }

  private String sanitizeUri(String rawUri, HttpServletRequest request) {
    // 1. If request is provided, resolve HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
    if (request != null) {
      Object patternAttr = request.getAttribute(
          org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      if (patternAttr instanceof String template && !template.isBlank()) {
        String cleanTemplate = template.trim();
        // Strip query parameters by default
        int queryIdx = cleanTemplate.indexOf('?');
        if (queryIdx >= 0) {
          cleanTemplate = cleanTemplate.substring(0, queryIdx).trim();
        }
        // Validate trusted API prefix and reject templates containing sensitive data
        if (isTrustedApiPrefix(cleanTemplate) && !containsSensitiveData(cleanTemplate)) {
          return cleanTemplate;
        }
      }
    }

    // 2. If no trusted route template is available, return "/sanitized-path" by default.
    return "/sanitized-path";
  }

  private static final Pattern SAFE_STANDARD_VALIDATION_PATTERN = Pattern.compile(
      "^("
          + "must not be (null|blank|empty)"
          + "|is required"
          + "|must be a well-formed email address"
          + "|must be a valid (email|URL|url|date|ISO date)"
          + "|must be a (positive|negative) number"
          + "|must be (greater than|less than)( or equal to)? -?[0-9]+(\\.[0-9]+)?"
          + "|must be between -?[0-9]+(\\.[0-9]+)? and -?[0-9]+(\\.[0-9]+)?"
          + "|must be (true|false)"
          + "|must be (a )?(past|future) date( or present)?"
          + "|must be in the (past|future)( or present)?"
          + "|(size|length) must be between [0-9]+ and [0-9]+"
          + "|cannot exceed [0-9]+ characters"
          + "|numeric value out of bounds( \\(<[0-9]+ digits>\\.<[0-9]+ digits> expected\\))?"
          + "|must be (greater than|less than)( or equal to)? (?:start date|minimum value)"
          + "|must be at least [0-9]+ years old and not in the future"
          + "|is invalid"
          + "|invalid format"
          + "|size is out of allowed range"
          + ")$",
      Pattern.CASE_INSENSITIVE
  );

  private static final int MAX_COLLECTION_DEPTH = 3;

  private boolean containsRejectedValue(String message, Object rejectedValue) {
    return containsRejectedValue(message, rejectedValue, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  private boolean containsRejectedValue(String message, Object rejectedValue, int depth, Set<Object> visited) {
    if (message == null || message.isBlank() || rejectedValue == null) {
      return false;
    }

    if (rejectedValue.getClass().isArray()) {
      if (depth >= MAX_COLLECTION_DEPTH) {
        // Fail closed: cannot guarantee unexamined branches nested beyond the depth limit are safe
        return true;
      }
      if (!visited.add(rejectedValue)) {
        return false; // Cycle detected: avoid infinite recursion on already examined collection
      }
      int len = java.lang.reflect.Array.getLength(rejectedValue);
      for (int i = 0; i < len; i++) {
        Object elem = java.lang.reflect.Array.get(rejectedValue, i);
        if (containsRejectedValue(message, elem, depth + 1, visited)) {
          return true;
        }
      }
      return false;
    }

    if (rejectedValue instanceof Iterable<?> iter) {
      if (depth >= MAX_COLLECTION_DEPTH) {
        // Fail closed: cannot guarantee unexamined branches nested beyond the depth limit are safe
        return true;
      }
      if (!visited.add(rejectedValue)) {
        return false; // Cycle detected: avoid infinite recursion on already examined collection
      }
      for (Object elem : iter) {
        if (containsRejectedValue(message, elem, depth + 1, visited)) {
          return true;
        }
      }
      return false;
    }

    String rejectedStr = String.valueOf(rejectedValue).trim();
    if (rejectedStr.isBlank()) {
      return false;
    }
    return message.toLowerCase().contains(rejectedStr.toLowerCase());
  }

  private boolean isSafeStandardValidationMessage(String propertyName, String message, Object rejectedValue) {
    if (message == null || message.isBlank()) {
      return false;
    }
    if (containsSensitiveData(message)) {
      return false;
    }
    if (containsRejectedValue(message, rejectedValue)) {
      return false;
    }
    String normalized = message.trim();
    if (propertyName != null && !propertyName.isBlank()) {
      String simpleName = propertyName.trim();
      int lastDot = simpleName.lastIndexOf('.');
      if (lastDot >= 0 && lastDot < simpleName.length() - 1) {
        simpleName = simpleName.substring(lastDot + 1);
      }
      String fullPrefix = propertyName.trim().toLowerCase() + " ";
      String simplePrefix = simpleName.toLowerCase() + " ";
      if (normalized.toLowerCase().startsWith(fullPrefix)) {
        normalized = normalized.substring(fullPrefix.length()).trim();
      } else if (normalized.toLowerCase().startsWith(simplePrefix)) {
        normalized = normalized.substring(simplePrefix.length()).trim();
      }
    }
    return SAFE_STANDARD_VALIDATION_PATTERN.matcher(normalized).matches();
  }

  private Object getSafeRejectedValue(FieldError fieldError) {
    if (fieldError == null) {
      return null;
    }
    try {
      return fieldError.getRejectedValue();
    } catch (Exception ignored) {
      return null;
    }
  }

  private Object getSafeInvalidValue(ConstraintViolation<?> violation) {
    if (violation == null) {
      return null;
    }
    try {
      return violation.getInvalidValue();
    } catch (Exception ignored) {
      return null;
    }
  }

  private String sanitizeFieldError(FieldError fieldError) {
    if (fieldError == null) {
      return "field: is invalid";
    }
    String field = fieldError.getField() != null ? fieldError.getField() : "field";
    if (containsSensitiveData(field)) {
      field = "field";
    }
    String defaultMessage = fieldError.getDefaultMessage();
    Object rejectedValue = getSafeRejectedValue(fieldError);
    if (!isSafeStandardValidationMessage(field, defaultMessage, rejectedValue)) {
      return field + ": " + getSafeValidationMessage(fieldError.getCode());
    }
    return field + ": " + defaultMessage;
  }

  private String sanitizeObjectError(ObjectError objectError) {
    if (objectError instanceof FieldError fieldError) {
      return sanitizeFieldError(fieldError);
    }
    if (objectError == null) {
      return "object: is invalid";
    }
    String name = objectError.getObjectName() != null ? objectError.getObjectName() : "object";
    if (containsSensitiveData(name)) {
      name = "object";
    }
    String defaultMessage = objectError.getDefaultMessage();
    if (!isSafeStandardValidationMessage(name, defaultMessage, null)) {
      return name + ": " + getSafeValidationMessage(objectError.getCode());
    }
    return name + ": " + defaultMessage;
  }

  private String sanitizeParameterError(String paramName, MessageSourceResolvable resolvable, Object rejectedValue) {
    if (paramName == null || containsSensitiveData(paramName)) {
      paramName = "parameter";
    }
    if (resolvable == null) {
      return paramName + ": is invalid";
    }
    String message = resolvable.getDefaultMessage();
    String code = null;
    if (resolvable.getCodes() != null && resolvable.getCodes().length > 0) {
      for (String c : resolvable.getCodes()) {
        int firstDot = c.indexOf('.');
        String prefix = firstDot >= 0 ? c.substring(0, firstDot) : c;
        int lastDot = c.lastIndexOf('.');
        String suffix = lastDot >= 0 ? c.substring(lastDot + 1) : c;
        if (isRecognizedConstraintCode(prefix)) {
          code = prefix;
          break;
        } else if (isRecognizedConstraintCode(suffix)) {
          code = suffix;
          break;
        } else if (code == null && !suffix.isBlank()) {
          code = suffix;
        }
      }
    }
    if (!isSafeStandardValidationMessage(paramName, message, rejectedValue)) {
      return paramName + ": " + getSafeValidationMessage(code);
    }
    return paramName + ": " + message;
  }

  private boolean isRecognizedConstraintCode(String code) {
    if (code == null || code.isBlank()) {
      return false;
    }
    return switch (code) {
      case "NotNull", "NotBlank", "NotEmpty", "Email", "Min", "DecimalMin",
           "Max", "DecimalMax", "Positive", "PositiveOrZero", "Negative",
           "NegativeOrZero", "Size", "Length", "Digits", "Pattern",
           "Past", "PastOrPresent", "Future", "FutureOrPresent",
           "AssertTrue", "AssertFalse", "DateSequence", "NumberRange", "ValidDateOfBirth" -> true;
      default -> false;
    };
  }

  private String getSafeValidationMessage(String code) {
    if (code == null) {
      return "is invalid";
    }
    return switch (code) {
      case "NotNull", "NotBlank", "NotEmpty" -> "must not be blank";
      case "Email" -> "must be a well-formed email address";
      case "Min", "DecimalMin" -> "must be greater than or equal to minimum value";
      case "Max", "DecimalMax" -> "must be less than or equal to maximum value";
      case "Positive" -> "must be greater than 0";
      case "PositiveOrZero" -> "must be greater than or equal to 0";
      case "Negative" -> "must be less than 0";
      case "NegativeOrZero" -> "must be less than or equal to 0";
      case "Size", "Length" -> "size is out of allowed range";
      case "Digits" -> "numeric value out of bounds";
      case "Pattern" -> "invalid format";
      case "Past", "PastOrPresent" -> "must be a date in the past or present";
      case "Future", "FutureOrPresent" -> "must be a date in the future or present";
      case "AssertTrue" -> "must be true";
      case "AssertFalse" -> "must be false";
      case "DateSequence" -> "must be greater than or equal to start date";
      case "NumberRange" -> "must be greater than or equal to minimum value";
      case "ValidDateOfBirth" -> "must be at least 15 years old and not in the future";
      default -> "is invalid";
    };
  }

  private String sanitizeConstraintViolation(ConstraintViolation<?> violation) {
    if (violation == null) {
      return "property: is invalid";
    }
    String property = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "property";
    if (containsSensitiveData(property)) {
      property = "property";
    }
    String message = violation.getMessage();
    Object invalidValue = getSafeInvalidValue(violation);
    if (!isSafeStandardValidationMessage(property, message, invalidValue)) {
      return property + ": " + getSafeConstraintMessage(violation);
    }
    return property + ": " + message;
  }

  private String getSafeConstraintMessage(ConstraintViolation<?> violation) {
    if (violation != null) {
      try {
        if (violation.getConstraintDescriptor() != null
            && violation.getConstraintDescriptor().getAnnotation() != null) {
          String annotationName = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
          return getSafeValidationMessage(annotationName);
        }
      } catch (Exception ignored) {
      }
    }
    return "is invalid";
  }

  private static final Pattern SAFE_RESOURCE_NOT_FOUND_PATTERN = Pattern.compile(
      "^[A-Za-z0-9_]+ not found(?: with [a-zA-Z0-9_: ]+)?$"
  );

  private static final Pattern SAFE_SORT_DIRECTION_PATTERN = Pattern.compile(
      "^Invalid sort direction '[A-Za-z0-9_ -]+'\\. Must be ASC or DESC$"
  );

  private static final Pattern SAFE_SORT_FIELD_PATTERN = Pattern.compile(
      "^Unsupported sort field '[A-Za-z0-9_ -]+'\\. Supported fields: \\[.*\\]$"
  );

  private static final Pattern SAFE_PAGE_SIZE_PATTERN = Pattern.compile(
      "^Page size must not exceed [0-9]+$"
  );

  private static final Pattern SAFE_ALREADY_EXISTS_PATTERN = Pattern.compile(
      "^[A-Za-z ]+ already exists(?:: [A-Za-z0-9_ -]+)?$"
  );

  private static final Pattern SAFE_ROW_EXISTS_PATTERN = Pattern.compile(
      "^Row number [0-9]+ already exists in import batch$"
  );

  private static final Set<String> APPROVED_DOMAIN_MESSAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      // 1. Entity Lifecycle & Deletion Constraints
      "Cannot create job application for soft-deleted trainee",
      "Cannot create training enrollment for soft-deleted trainee",
      "Cannot create training enrollment for soft-deleted training batch",
      "Cannot create unemployment event for soft-deleted trainee",
      "Cannot issue certification for soft-deleted trainee",
      "Cannot record exit event for soft-deleted trainee",
      "Cannot update a deleted assessment",
      "Cannot update a deleted certification",
      "Cannot update a deleted training batch",
      "Cannot update a deleted training enrollment",
      "Cannot update status of a deleted training batch",
      "Cannot verify a revoked or expired certification",
      "Trainee is soft-deleted",
      "User account has been deleted",
      "User account is locked",
      "User account is not active",
      "User account status record not found",
      "User account status reference is missing",

      // 2. Workflow & Survey Invariants
      "Cannot update an already submitted survey response; create a new attempt",
      "Cannot delete answer of an already submitted survey response",
      "Cannot delete a submitted survey response",
      "Cannot delete a completed or terminal verification attempt",
      "Cannot modify an attempt that is in a terminal status",
      "Cannot delete a running import batch",
      "Terminal import batch cannot be updated",
      "Cannot delete assessment with existing skill gaps",
      "Cannot delete assessment result because certifications exist",
      "Cannot delete trainee assessment because assessment results exist",
      "Cannot delete import batch with existing records",
      "Cannot delete verification attempt with associated evidence",
      "Cannot delete employment verification with associated evidence records",
      "Cannot delete followup task with associated communication logs",
      "Cannot delete followup task with associated survey responses",
      "Cannot update soft-deleted followup campaign",
      "Followup campaign is already deleted",
      "Cannot create followup task for a soft-deleted campaign",
      "Cannot update followup task for a soft-deleted campaign",
      "Cannot delete communication log that has already been dispatched or delivered",
      "Cannot delete data quality rule with existing issues",
      "Cannot delete job application referenced by a placement record",
      "Cannot delete skill gap with existing observations",
      "Cannot delete skill gap with existing recommendations",
      "Cannot delete survey question with associated response answers",
      "Cannot delete survey response with associated answers",
      "Cannot delete the current active verification for an employment record",
      "Cannot delete verification request with existing verification records",
      "Cannot add answers to an already submitted survey response",
      "Cannot add salary history to a deleted employment record",
      "Cannot apply to a deleted job posting",
      "Cannot assign an issue that is already in terminal state",
      "Cannot associate deleted enrollment with job application",
      "Cannot create placement record for a deleted enrollment",
      "Cannot create verification request for a deleted employment record",

      // 3. Authorization & Tenant Isolation Boundaries
      "Target employer is missing required organization affiliation",
      "Target employer record not found for scope validation",
      "Authenticated employer is not authorized to act on records of another employer",
      "Authenticated organization is not authorized to act on records of another organization",
      "Authenticated trainee is not authorized to act on records of another trainee",
      "Authenticated training provider is not authorized to act on records of another training provider",
      "Authenticated user is not assigned to an employer",
      "User is not authenticated",
      "Unable to resolve authenticated user identity",

      // 4. Core Domain Invariants & Validations
      "Invalid date range",
      "Enrollment already exists",
      "Invalid username/email or password",
      "Invalid token signature",
      "Trainee ID is required to query consents",
      "Only isCurrent=true is supported for current employment records filter",
      "Only isCurrent=true is supported for current employments filter",
      "Only isCurrent=true is supported for current employment retentions filter",
      "Only isCurrent=true is supported for current employment verifications filter",
      "Only isCurrent=true is supported for current skill gaps filter",
      "Only isCurrent=true is supported for current unemployments filter",
      "Username or email is required",
      "Identity verification failed. Please verify the provided identity details.",
      "Invalid PAN format",
      "Invalid Aadhaar number format",
      "Invalid or expired authentication token",
      "Trainee consent is required to perform this action",
      "Role already assigned to user",
      "The request is invalid",
      "Request validation failed",
      "Malformed or unreadable request body",
      "Required request body is missing",
      "The requested resource was not found",
      "A resource with the specified unique attributes already exists",

      // 5. Business Validation Rules
      "Start date cannot precede enrollment date",
      "Expected completion date cannot precede start date",
      "Actual completion date cannot precede start date",
      "Maximum score must be greater than 0",
      "Pass score cannot be negative",
      "Pass score cannot exceed maximum score",
      "Obtained score cannot be negative",
      "Obtained score cannot exceed maximum score",
      "Score percentage must be between 0 and 100",
      "Session sequence must be greater than or equal to 1",
      "Session end time cannot precede session start time",
      "Actual joining date cannot be before offer date",
      "Submitted timestamp cannot be before started timestamp",
      "Verified timestamp cannot be before requested timestamp",
      "Action type requires a recommended course",
      "At least one answer value must be provided",
      "At least one skill gap is required",
      "Rating must be between 0.00 and 5.00",
      "Invalid pincode format. Must be 6 digits starting with 1-9",
      "Certification requires a passing assessment outcome",
      "Course code already exists",
      "Batch code already exists",
      "Center code already exists",
      "Provider code already exists",
      "Registration number already exists",
      "Username already exists",
      "Email already exists",
      "Trainee registration number already exists",
      "Trainee already linked to this user ID",
      "Placement number already exists",
      "Job posting code already exists",
      "Campaign code already exists",
      "Assessment code already exists",
      "Certificate number already exists",
      "Employer code already exists",
      "GSTIN already exists",
      "Role code already exists",
      "Permission code already exists",
      "Job role code already exists",
      "Skill code already exists",
      "Skill name already exists",
      "Program code already exists",
      "Scheme code already exists",
      "Rule code already exists",
      "Batch code exists",
      "Verification attempt number already exists",
      "Verification evidence reference already exists",
      "Employer branch code already exists",
      "Skill gap observation number already exists",
      "Survey question already exists for this survey, version, and question code",
      "Survey question order already exists for this survey, version, and order number",
      "Employment number already exists",
      "Request number already exists",
      "Verification number already exists",
      "Skill gap number already exists",
      "Skill gap assessment number already exists",
      "Skill gap recommendation number already exists",
      "Training enrollment number already exists"
  )));

  private String getSafeMessageForErrorCode(String errorCode, HttpStatus status) {
    if (errorCode == null) {
      return status != null && status.is5xxServerError()
          ? "An unexpected error occurred. Please try again later."
          : (status != null ? status.getReasonPhrase() : "The request is invalid");
    }
    return switch (errorCode) {
      case "INVALID_IDENTITY" -> "Identity verification failed. Please verify the provided identity details.";
      case "INVALID_CREDENTIALS" -> "Invalid credentials provided";
      case "UNAUTHORIZED" -> "Full authentication is required to access this resource";
      case "ACCESS_DENIED" -> "Access is denied";
      case "CONSENT_REQUIRED" -> "Trainee consent is required to perform this action";
      case "RESOURCE_NOT_FOUND" -> "The requested resource was not found";
      case "VALIDATION_ERROR" -> "Request validation failed";
      case "BAD_REQUEST" -> "The request is invalid";
      case "MALFORMED_REQUEST", "MALFORMED_JSON" -> "Malformed or unreadable request body";
      case "TYPE_MISMATCH" -> "Invalid parameter value provided";
      case "MISSING_PARAMETER", "MISSING_REQUIRED_PARAMETER" -> "Required parameter is missing";
      case "MISSING_HEADER" -> "Required header is missing";
      case "INVALID_PAGE" -> "Page index must not be less than zero";
      case "INVALID_PAGE_SIZE" -> "Invalid page size provided";
      case "INVALID_SORT_FIELD" -> "Invalid sort field provided";
      case "INVALID_SORT_DIRECTION" -> "Invalid sort direction provided";
      case "RESOURCE_CONFLICT" -> "A resource with the specified unique attributes already exists";
      case "DUPLICATE_PLACEMENT_EMPLOYMENT" -> "Placement record is already linked to another employment record";
      case "DUPLICATE_ENROLLMENT" -> "Trainee is already enrolled in this training batch";
      case "EXIT_EVENT_ALREADY_EXISTS" -> "Employment exit event already exists for this employment record";
      case "UNEMPLOYMENT_EVENT_NOT_ACTIVE" -> "Trainee does not have an active unemployment record to transition";
      case "OPEN_REQUEST_EXISTS" -> "An open verification request already exists for this employment record";
      case "ACTIVE_VERIFICATION_ALREADY_EXISTS" -> "Employment record already has an active verification";
      case "DUPLICATE_SURVEY_RESPONSE" -> "Survey response already exists for this survey, trainee, and attempt";
      case "DUPLICATE_PROVIDER_MESSAGE_ID" -> "Communication log already exists for this provider message ID";
      case "ACTIVE_SKILL_GAP_ALREADY_EXISTS" -> "Trainee already has an active current skill gap for this skill";
      case "ASSESSMENT_RESULT_ALREADY_EXISTS" -> "Assessment result already exists for this trainee assessment";
      case "CERTIFICATE_NUMBER_ALREADY_EXISTS" -> "Certificate number already exists";
      case "INVALID_AADHAAR" -> "Invalid Aadhaar number format";
      case "INVALID_PAN" -> "Invalid PAN format";
      case "INVALID_TOKEN" -> "Invalid or expired authentication token";
      case "INVALID_DATE_RANGE" -> "Invalid date range";
      case "UNSUPPORTED_PARAMETER" -> "The request contains unsupported parameters";
      case "INVALID_FILTER_PARAMETER" -> "Invalid filter parameter provided";
      case "ENDPOINT_NOT_FOUND" -> "The requested resource was not found";
      case "METHOD_NOT_ALLOWED" -> "HTTP method is not supported for this endpoint";
      case "UNSUPPORTED_MEDIA_TYPE" -> "Content type is not supported";
      case "NOT_ACCEPTABLE" -> "Requested media type is not acceptable";
      case "DATABASE_ERROR" -> "An unexpected database error occurred. Please try again later.";
      case "INTERNAL_SERVER_ERROR" -> "An unexpected error occurred. Please try again later.";
      default -> {
        if (status == null || status.is5xxServerError()) {
          yield "An unexpected error occurred. Please try again later.";
        }
        yield switch (status) {
          case BAD_REQUEST -> "The request is invalid";
          case NOT_FOUND -> "The requested resource was not found";
          case CONFLICT -> "A resource with the specified unique attributes already exists";
          case UNAUTHORIZED -> "Full authentication is required to access this resource";
          case FORBIDDEN -> "Access is denied";
          case METHOD_NOT_ALLOWED -> "HTTP method is not supported for this endpoint";
          case UNSUPPORTED_MEDIA_TYPE -> "Content type is not supported";
          case NOT_ACCEPTABLE -> "Requested media type is not acceptable";
          default -> status.getReasonPhrase();
        };
      }
    };
  }

  private boolean isApprovedSafeDomainMessage(String errorCode, String candidateMessage) {
    if (candidateMessage == null || candidateMessage.isBlank() || containsSensitiveData(candidateMessage)) {
      return false;
    }
    String trimmed = candidateMessage.trim();
    if (APPROVED_DOMAIN_MESSAGES.contains(trimmed)) {
      return true;
    }
    if (SAFE_RESOURCE_NOT_FOUND_PATTERN.matcher(trimmed).matches()) {
      return true;
    }
    if ("INVALID_SORT_DIRECTION".equals(errorCode) && SAFE_SORT_DIRECTION_PATTERN.matcher(trimmed).matches()) {
      return true;
    }
    if ("INVALID_SORT_FIELD".equals(errorCode) && SAFE_SORT_FIELD_PATTERN.matcher(trimmed).matches()) {
      return true;
    }
    if ("INVALID_PAGE_SIZE".equals(errorCode) && SAFE_PAGE_SIZE_PATTERN.matcher(trimmed).matches()) {
      return true;
    }
    if (SAFE_ALREADY_EXISTS_PATTERN.matcher(trimmed).matches()) {
      return true;
    }
    if ("IMPORT_RECORD_ROW_EXISTS".equals(errorCode) && SAFE_ROW_EXISTS_PATTERN.matcher(trimmed).matches()) {
      return true;
    }
    return false;
  }

  private String resolveSafeClientMessage(BaseException ex) {
    if (ex.getStatus() == null || ex.getStatus().is5xxServerError()) {
      return "An unexpected error occurred. Please try again later.";
    }

    String candidateMessage = ex.getMessage();
    String errorCode = ex.getErrorCode();

    if (candidateMessage != null && !candidateMessage.isBlank()) {
      // 1. If candidate message contains any sensitive data, immediately fail closed to code-derived safe message
      if (containsSensitiveData(candidateMessage)) {
        return getSafeMessageForErrorCode(errorCode, ex.getStatus());
      }

      // 2. Check against explicit allowlist of known-safe application domain messages and approved templates
      if (isApprovedSafeDomainMessage(errorCode, candidateMessage.trim())) {
        return candidateMessage.trim();
      }
    }

    // 3. If not explicitly approved or if message is empty/null, derive safe message strictly from error code
    return getSafeMessageForErrorCode(errorCode, ex.getStatus());
  }

  private ErrorResponse sanitizeErrorResponse(ErrorResponse response, HttpStatus status) {
    if (response == null) {
      return null;
    }
    HttpStatus effectiveStatus = status != null ? status : HttpStatus.resolve(response.getStatus());
    if (effectiveStatus == null) {
      effectiveStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    if (response.getPath() != null && containsSensitiveData(response.getPath())) {
      response.setPath("/sanitized-path");
    }

    if (response.getCode() != null && containsSensitiveData(response.getCode())) {
      response.setCode(effectiveStatus.is5xxServerError() ? "INTERNAL_SERVER_ERROR" : "BAD_REQUEST");
    }

    if (response.getError() != null && containsSensitiveData(response.getError())) {
      response.setError(effectiveStatus.getReasonPhrase());
    }

    if (response.getMessage() != null && containsSensitiveData(response.getMessage())) {
      String safeMsg = getSafeMessageForErrorCode(response.getCode(), effectiveStatus);
      if (containsSensitiveData(safeMsg)) {
        safeMsg = effectiveStatus.is5xxServerError()
            ? "An unexpected error occurred. Please try again later."
            : effectiveStatus.getReasonPhrase();
      }
      response.setMessage(safeMsg);
    }

    if (response.getDetails() != null && !response.getDetails().isEmpty()) {
      List<String> sanitizedDetails = new ArrayList<>();
      for (String detail : response.getDetails()) {
        if (detail == null || detail.isBlank()) {
          continue;
        }
        if (containsSensitiveData(detail)) {
          int colonIdx = detail.indexOf(':');
          if (colonIdx > 0) {
            String field = detail.substring(0, colonIdx).trim();
            if (containsSensitiveData(field)) {
              field = "field";
            }
            sanitizedDetails.add(field + ": is invalid");
          } else {
            sanitizedDetails.add("field: is invalid");
          }
        } else {
          sanitizedDetails.add(detail);
        }
      }
      response.setDetails(sanitizedDetails);
    }

    return response;
  }

  private ResponseEntity<ErrorResponse> buildSanitizedResponse(ErrorResponse response, HttpStatus status) {
    return buildSanitizedResponse(response, null, status);
  }

  private ResponseEntity<ErrorResponse> buildSanitizedResponse(ErrorResponse response, HttpHeaders headers, HttpStatus status) {
    sanitizeErrorResponse(response, status);
    if (headers != null) {
      return new ResponseEntity<>(response, headers, status);
    }
    return new ResponseEntity<>(response, status);
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
    String clientMessage = resolveSafeClientMessage(ex);
    String exceptionType = !ex.getClass().getSimpleName().isEmpty()
        ? ex.getClass().getSimpleName()
        : ex.getClass().getName();
    String uri = sanitizeUri(request);
    log.warn("Application exception on {}: status={}, code={}, exception={}",
        uri, ex.getStatus().value(), ex.getErrorCode(), exceptionType);
    ErrorResponse errorResponse = new ErrorResponse(
        ex.getStatus().value(),
        ex.getStatus().getReasonPhrase(),
        ex.getErrorCode(),
        clientMessage,
        uri
    );
    return buildSanitizedResponse(errorResponse, ex.getStatus());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> validationErrors = Stream.concat(
            ex.getBindingResult().getFieldErrors().stream().map(this::sanitizeFieldError),
            ex.getBindingResult().getGlobalErrors().stream().map(this::sanitizeObjectError)
        )
        .sorted()
        .toList();
    List<String> invalidFields = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> {
          String f = fe.getField() != null ? fe.getField() : "field";
          return containsSensitiveData(f) ? "field" : f;
        })
        .distinct()
        .sorted()
        .toList();

    String uri = sanitizeUri(request);
    log.warn("Validation error on {}: status={}, code={}, invalidFields={}",
        uri, HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", invalidFields);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "VALIDATION_ERROR",
        "Request validation failed",
        uri,
        validationErrors
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpServletRequest request) {

    List<String> validationErrors = new ArrayList<>();
    List<String> invalidParams = new ArrayList<>();

    List<ParameterValidationResult> results = new ArrayList<>();
    if (ex.getValueResults() != null) {
      results.addAll(ex.getValueResults());
    }
    if (ex.getBeanResults() != null) {
      results.addAll(ex.getBeanResults());
    }
    for (ParameterValidationResult result : results) {
        if (result == null) {
          continue;
        }
        String paramName = "parameter";
        if (result.getMethodParameter() != null) {
          if (result.getMethodParameter().getParameterName() == null) {
            result.getMethodParameter().initParameterNameDiscovery(new org.springframework.core.StandardReflectionParameterNameDiscoverer());
          }
          if (result.getMethodParameter().getParameterName() != null) {
            paramName = result.getMethodParameter().getParameterName();
          }
        }
        invalidParams.add(containsSensitiveData(paramName) ? "parameter" : paramName);

        if (result.getResolvableErrors() != null) {
          for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
            if (resolvable instanceof FieldError fieldError) {
              validationErrors.add(sanitizeFieldError(fieldError));
            } else if (resolvable instanceof ObjectError objectError) {
              validationErrors.add(sanitizeObjectError(objectError));
            } else {
              validationErrors.add(sanitizeParameterError(paramName, resolvable, result.getArgument()));
            }
          }
        }
      }

    Collections.sort(validationErrors);
    Collections.sort(invalidParams);

    String uri = sanitizeUri(request);

    // Preserve established Phase 26.3 / 26.4 pagination bounds contract (INVALID_PAGE, INVALID_PAGE_SIZE)
    if (results.size() == 1) {
      ParameterValidationResult singleResult = results.get(0);
      String pName = "";
      if (singleResult.getMethodParameter() != null) {
        if (singleResult.getMethodParameter().getParameterName() == null) {
          singleResult.getMethodParameter().initParameterNameDiscovery(new org.springframework.core.StandardReflectionParameterNameDiscoverer());
        }
        pName = singleResult.getMethodParameter().getParameterName();
      }
      if ("page".equals(pName) && singleResult.getResolvableErrors() != null && !singleResult.getResolvableErrors().isEmpty()) {
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "INVALID_PAGE",
            "Page index must not be less than zero",
            uri
        );
        return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
      } else if ("size".equals(pName) && singleResult.getResolvableErrors() != null && !singleResult.getResolvableErrors().isEmpty()) {
        MessageSourceResolvable resolvable = singleResult.getResolvableErrors().iterator().next();
        String code = "";
        if (resolvable.getCodes() != null) {
          for (String c : resolvable.getCodes()) {
            if (c != null && (c.startsWith("Min") || c.endsWith("Min"))) { code = "Min"; break; }
            if (c != null && (c.startsWith("Max") || c.endsWith("Max"))) { code = "Max"; break; }
          }
        }
        String message = "Max".equals(code)
            ? "Page size must not exceed " + in.gov.sih.sih26135.response.PaginationUtils.MAX_SIZE
            : "Page size must be at least 1";
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "INVALID_PAGE_SIZE",
            message,
            uri
        );
        return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
      }
    }

    log.warn("Handler method validation error on {}: status={}, code={}, invalidParams={}",
        uri, HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", invalidParams);

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "VALIDATION_ERROR",
        "Request validation failed",
        uri,
        validationErrors
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request) {

    String uri = sanitizeUri(request);

    // Evaluate all violations: classify as HTTP 400 only when origin is positively identifiable
    // as supported client request validation (controller method parameter or request DTO).
    // Any entity/persistence, internal service/repository, unknown origin, or null root bean fails closed as HTTP 500.
    if (!isSupportedClientRequestValidation(ex)) {
      log.error("Internal or unverified constraint violation on {}: status={}, code={}, failing closed",
          uri, HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR");
      ErrorResponse errorResponse = new ErrorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
          "INTERNAL_SERVER_ERROR",
          "An unexpected error occurred. Please try again later.",
          uri
      );
      return buildSanitizedResponse(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Preserve established Phase 26.3 / 26.4 pagination bounds contract (INVALID_PAGE, INVALID_PAGE_SIZE)
    if (ex.getConstraintViolations().size() == 1) {
      ConstraintViolation<?> violation = ex.getConstraintViolations().iterator().next();
      String property = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "";
      String annotation = "";
      if (violation.getConstraintDescriptor() != null && violation.getConstraintDescriptor().getAnnotation() != null) {
        annotation = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
      }
      if (property.endsWith(".page") || property.equals("page")) {
        if ("Min".equals(annotation)) {
          log.warn("Pagination bounds violation on {}: status={}, code={}, property='{}'",
              uri, HttpStatus.BAD_REQUEST.value(), "INVALID_PAGE", property);
          ErrorResponse errorResponse = new ErrorResponse(
              HttpStatus.BAD_REQUEST.value(),
              HttpStatus.BAD_REQUEST.getReasonPhrase(),
              "INVALID_PAGE",
              "Page index must not be less than zero",
              uri
          );
          return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
        }
      } else if (property.endsWith(".size") || property.equals("size")) {
        if ("Min".equals(annotation)) {
          log.warn("Pagination bounds violation on {}: status={}, code={}, property='{}'",
              uri, HttpStatus.BAD_REQUEST.value(), "INVALID_PAGE_SIZE", property);
          ErrorResponse errorResponse = new ErrorResponse(
              HttpStatus.BAD_REQUEST.value(),
              HttpStatus.BAD_REQUEST.getReasonPhrase(),
              "INVALID_PAGE_SIZE",
              "Page size must be at least 1",
              uri
          );
          return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
        } else if ("Max".equals(annotation)) {
          log.warn("Pagination bounds violation on {}: status={}, code={}, property='{}'",
              uri, HttpStatus.BAD_REQUEST.value(), "INVALID_PAGE_SIZE", property);
          ErrorResponse errorResponse = new ErrorResponse(
              HttpStatus.BAD_REQUEST.value(),
              HttpStatus.BAD_REQUEST.getReasonPhrase(),
              "INVALID_PAGE_SIZE",
              "Page size must not exceed " + in.gov.sih.sih26135.response.PaginationUtils.MAX_SIZE,
              uri
          );
          return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
        }
      }
    }

    // Confirmed request / method parameter validation failure: map to HTTP 400 Bad Request
    List<String> validationErrors = ex.getConstraintViolations().stream()
        .map(this::sanitizeConstraintViolation)
        .sorted()
        .toList();
    List<String> invalidProperties = ex.getConstraintViolations().stream()
        .map(v -> {
          String p = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "unknown";
          return containsSensitiveData(p) ? "property" : p;
        })
        .distinct()
        .sorted()
        .toList();

    log.warn("Request constraint violation on {}: status={}, code={}, invalidProperties={}",
        uri, HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", invalidProperties);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "VALIDATION_ERROR",
        "Request validation failed",
        uri,
        validationErrors
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  private boolean isSupportedClientRequestValidation(ConstraintViolationException ex) {
    if (ex.getConstraintViolations() == null || ex.getConstraintViolations().isEmpty()) {
      return false;
    }

    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      if (violation == null) {
        return false;
      }

      Class<?> rootBeanClass = violation.getRootBeanClass();
      if (rootBeanClass == null) {
        // Unknown or missing root bean origin -> fail closed to HTTP 500
        return false;
      }

      // Explicitly reject entity / persistence-layer origins
      if (rootBeanClass.isAnnotationPresent(Entity.class)
          || rootBeanClass.getName().startsWith("in.gov.sih.sih26135.entity.")) {
        return false;
      }

      // Explicitly reject internal service or repository layer origins
      if (rootBeanClass.isAnnotationPresent(Service.class)
          || rootBeanClass.isAnnotationPresent(Repository.class)
          || rootBeanClass.getName().startsWith("in.gov.sih.sih26135.service.")
          || rootBeanClass.getName().startsWith("in.gov.sih.sih26135.repository.")) {
        return false;
      }

      // Positively identify supported Request DTOs from verified packages only
      if (isVerifiedRequestDto(rootBeanClass)) {
        continue;
      }

      // Positively identify supported project API Controller with ElementKind.PARAMETER node
      if (isVerifiedProjectController(rootBeanClass) && hasParameterNode(violation)) {
        continue;
      }

      // Unidentified origin (unknown class, unverified third-party controller, controller without parameter node, or internal helper) fails closed
      return false;
    }

    return true;
  }

  /**
   * Authoritative allowlist of verified application controller classes.
   * Merely declaring an approved package, class name ending with "Controller", or controller annotations
   * cannot spoof trust. Only authentic compiled project controllers are trusted.
   */
  private static final Set<Class<?>> TRUSTED_CONTROLLER_CLASSES = Set.of(
      // in.gov.sih.sih26135.controller
      in.gov.sih.sih26135.controller.AssessmentController.class,
      in.gov.sih.sih26135.controller.AssessmentResultController.class,
      in.gov.sih.sih26135.controller.AttendanceRecordController.class,
      in.gov.sih.sih26135.controller.AuditLogController.class,
      in.gov.sih.sih26135.controller.CertificationController.class,
      in.gov.sih.sih26135.controller.CommunicationLogController.class,
      in.gov.sih.sih26135.controller.ConsentController.class,
      in.gov.sih.sih26135.controller.CourseController.class,
      in.gov.sih.sih26135.controller.DataQualityIssueController.class,
      in.gov.sih.sih26135.controller.DataQualityIssueEventController.class,
      in.gov.sih.sih26135.controller.DataQualityRuleController.class,
      in.gov.sih.sih26135.controller.EmployerController.class,
      in.gov.sih.sih26135.controller.EmploymentExitEventController.class,
      in.gov.sih.sih26135.controller.EmploymentRecordController.class,
      in.gov.sih.sih26135.controller.EmploymentVerificationAttemptController.class,
      in.gov.sih.sih26135.controller.EmploymentVerificationController.class,
      in.gov.sih.sih26135.controller.EmploymentVerificationEvidenceController.class,
      in.gov.sih.sih26135.controller.EmploymentVerificationRequestController.class,
      in.gov.sih.sih26135.controller.FollowupCampaignController.class,
      in.gov.sih.sih26135.controller.FollowupTaskController.class,
      in.gov.sih.sih26135.controller.ImportBatchController.class,
      in.gov.sih.sih26135.controller.ImportRecordController.class,
      in.gov.sih.sih26135.controller.JobApplicationController.class,
      in.gov.sih.sih26135.controller.JobPostingController.class,
      in.gov.sih.sih26135.controller.PermissionController.class,
      in.gov.sih.sih26135.controller.PlacementRecordController.class,
      in.gov.sih.sih26135.controller.ProgramController.class,
      in.gov.sih.sih26135.controller.RoleController.class,
      in.gov.sih.sih26135.controller.SalaryHistoryController.class,
      in.gov.sih.sih26135.controller.SchemeController.class,
      in.gov.sih.sih26135.controller.SkillGapAssessmentController.class,
      in.gov.sih.sih26135.controller.SkillGapController.class,
      in.gov.sih.sih26135.controller.SkillGapObservationController.class,
      in.gov.sih.sih26135.controller.SkillGapRecommendationController.class,
      in.gov.sih.sih26135.controller.SurveyQuestionController.class,
      in.gov.sih.sih26135.controller.SurveyResponseAnswerController.class,
      in.gov.sih.sih26135.controller.SurveyResponseController.class,
      in.gov.sih.sih26135.controller.SystemEventLogController.class,
      in.gov.sih.sih26135.controller.TraineeAssessmentController.class,
      in.gov.sih.sih26135.controller.TraineeController.class,
      in.gov.sih.sih26135.controller.TraineeUnemploymentEventController.class,
      in.gov.sih.sih26135.controller.TrainingBatchController.class,
      in.gov.sih.sih26135.controller.TrainingCenterController.class,
      in.gov.sih.sih26135.controller.TrainingEnrollmentController.class,
      in.gov.sih.sih26135.controller.TrainingProviderController.class,
      in.gov.sih.sih26135.controller.UserController.class,
      // in.gov.sih.sih26135.controller.analytics
      in.gov.sih.sih26135.controller.analytics.AnalyticsFactController.class,
      in.gov.sih.sih26135.controller.analytics.AnalyticsSummaryController.class,
      // in.gov.sih.sih26135.controller.auth
      in.gov.sih.sih26135.controller.auth.AuthController.class,
      // in.gov.sih.sih26135.controller.workflow
      in.gov.sih.sih26135.controller.workflow.EmploymentLifecycleWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.FollowupEngagementWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.PlacementEmploymentWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.SkillGapRemediationWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.TrainingOutcomeWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.VerificationWorkflowController.class
  );

  /**
   * Authoritative allowlist of verified client request DTO classes.
   * Merely declaring a whitelisted package or class name ending with "Request" cannot spoof trust.
   */
  private static final Set<Class<?>> TRUSTED_REQUEST_DTO_CLASSES = Set.of(
      // in.gov.sih.sih26135.dto.request
      in.gov.sih.sih26135.dto.request.AssignCourseProgramRequest.class,
      in.gov.sih.sih26135.dto.request.AssignCourseSkillRequest.class,
      in.gov.sih.sih26135.dto.request.AssignJobRoleSkillRequest.class,
      in.gov.sih.sih26135.dto.request.AssignProgramTrainingProviderRequest.class,
      in.gov.sih.sih26135.dto.request.AssignRolePermissionRequest.class,
      in.gov.sih.sih26135.dto.request.AssignUserRoleRequest.class,
      in.gov.sih.sih26135.dto.request.ChangeBatchStatusRequest.class,
      in.gov.sih.sih26135.dto.request.CreateAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest.class,
      in.gov.sih.sih26135.dto.request.CreateAttendanceRecordRequest.class,
      in.gov.sih.sih26135.dto.request.CreateAuditLogRequest.class,
      in.gov.sih.sih26135.dto.request.CreateCertificationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest.class,
      in.gov.sih.sih26135.dto.request.CreateCourseRequest.class,
      in.gov.sih.sih26135.dto.request.CreateDataQualityIssueEventRequest.class,
      in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest.class,
      in.gov.sih.sih26135.dto.request.CreateDataQualityRuleRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmployerRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationAttemptRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationEvidenceRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest.class,
      in.gov.sih.sih26135.dto.request.CreateFollowupCampaignRequest.class,
      in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest.class,
      in.gov.sih.sih26135.dto.request.CreateImportBatchRequest.class,
      in.gov.sih.sih26135.dto.request.CreateImportRecordRequest.class,
      in.gov.sih.sih26135.dto.request.CreateJobApplicationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateJobPostingRequest.class,
      in.gov.sih.sih26135.dto.request.CreateJobRoleRequest.class,
      in.gov.sih.sih26135.dto.request.CreatePermissionRequest.class,
      in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest.class,
      in.gov.sih.sih26135.dto.request.CreateProgramRequest.class,
      in.gov.sih.sih26135.dto.request.CreateRoleRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSchemeRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillCategoryRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillGapObservationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillGapRecommendationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillGapRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSurveyQuestionRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSurveyResponseAnswerRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTraineeAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTraineeRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTrainingCenterRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTrainingProviderRequest.class,
      in.gov.sih.sih26135.dto.request.CreateUserRequest.class,
      in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest.class,
      in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest.class,
      in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest.class,
      in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest.class,
      in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest.class,
      in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest.class,
      in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest.class,
      in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest.class,
      in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest.class,
      in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest.class,
      in.gov.sih.sih26135.dto.request.TerminateEmpanelmentRequest.class,
      in.gov.sih.sih26135.dto.request.TraineeRecommendationDecisionRequest.class,
      in.gov.sih.sih26135.dto.request.UnemploymentToEmploymentTransitionRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateAssessmentResultRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateAttendanceRecordRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateCertificationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateCommunicationLogRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateCourseRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateCourseSkillRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateDataQualityRuleRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmployerRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentExitEventRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationAttemptRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationEvidenceRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequestRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateFollowupCampaignRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateFollowupTaskRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateImportBatchRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateImportRecordRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateJobApplicationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateJobPostingRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateJobRoleRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateJobRoleSkillRequest.class,
      in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateProgramRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateRoleRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSalaryHistoryRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSchemeRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillCategoryRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillGapAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillGapObservationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillGapRecommendationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillGapRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSurveyQuestionRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSurveyResponseAnswerRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTraineeAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTraineeRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTraineeUnemploymentEventRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTrainingBatchRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTrainingCenterRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTrainingProviderRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateUserRequest.class,
      // in.gov.sih.sih26135.dto.auth
      in.gov.sih.sih26135.dto.auth.LoginRequest.class,
      in.gov.sih.sih26135.dto.auth.RefreshTokenRequest.class
  );

  private boolean isVerifiedProjectController(Class<?> rootBeanClass) {
    if (rootBeanClass == null) {
      return false;
    }
    // Fail closed against package-spoofing: require inclusion in the explicit allowlist of known project controllers.
    // Merely declaring a whitelisted package, class suffix "Controller", or controller annotations is insufficient.
    return TRUSTED_CONTROLLER_CLASSES.contains(rootBeanClass);
  }

  private boolean isVerifiedRequestDto(Class<?> rootBeanClass) {
    if (rootBeanClass == null) {
      return false;
    }
    // Fail closed against package-spoofing: require inclusion in the explicit allowlist of known request DTOs.
    // Merely declaring a whitelisted package or class suffix "Request" is insufficient.
    return TRUSTED_REQUEST_DTO_CLASSES.contains(rootBeanClass);
  }

  private boolean hasParameterNode(ConstraintViolation<?> violation) {
    Path path = violation.getPropertyPath();
    if (path == null) {
      return false;
    }
    try {
      for (Path.Node node : path) {
        if (node != null && node.getKind() == ElementKind.PARAMETER) {
          return true;
        }
      }
    } catch (Exception ignored) {
      return false;
    }
    return false;
  }

  private String resolveSafeTypeName(Class<?> requiredType) {
    if (requiredType == null) {
      return "valid type";
    }
    if (requiredType.isEnum()) {
      return "valid enum value";
    }
    String name = requiredType.getSimpleName();
    if (containsSensitiveData(name)) {
      return "valid type";
    }
    return name;
  }

  private boolean isDateTimeType(Class<?> type) {
    if (type == null) {
      return false;
    }
    return java.time.temporal.TemporalAccessor.class.isAssignableFrom(type)
        || java.util.Date.class.isAssignableFrom(type)
        || java.util.Calendar.class.isAssignableFrom(type);
  }

  private boolean hasCauseOfType(Throwable ex, Class<?> targetClass) {
    Throwable current = ex;
    while (current != null) {
      if (targetClass.isInstance(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private String extractJacksonFieldPath(List<JsonMappingException.Reference> path) {
    if (path == null || path.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (JsonMappingException.Reference ref : path) {
      if (ref == null) {
        continue;
      }
      if (ref.getFieldName() != null && !ref.getFieldName().isBlank()) {
        if (sb.length() > 0) {
          sb.append(".");
        }
        sb.append(ref.getFieldName());
      } else if (ref.getIndex() >= 0) {
        sb.append("[").append(ref.getIndex()).append("]");
      }
    }
    String result = sb.toString();
    if (result.isBlank() || containsSensitiveData(result)) {
      return null;
    }
    return result;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex,
      HttpServletRequest request) {
    String uri = sanitizeUri(request);
    String paramName = ex.getName() != null ? ex.getName() : "parameter";
    if (containsSensitiveData(paramName)) {
      paramName = "parameter";
    }
    String requiredType = resolveSafeTypeName(ex.getRequiredType());
    String message = String.format("Invalid parameter value for '%s'. Expected type: %s", paramName, requiredType);

    log.warn("Parameter type mismatch on {} for parameter '{}': status={}, code={}, expectedType={}",
        uri, paramName, HttpStatus.BAD_REQUEST.value(), "TYPE_MISMATCH", requiredType);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "TYPE_MISMATCH",
        message,
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex,
      HttpServletRequest request) {
    String uri = sanitizeUri(request);
    String paramName = ex.getParameterName() != null ? ex.getParameterName() : "parameter";
    if (containsSensitiveData(paramName)) {
      paramName = "parameter";
    }
    String message = String.format("Required parameter '%s' is missing", paramName);

    log.warn("Missing request parameter on {}: status={}, code={}, parameter='{}'",
        uri, HttpStatus.BAD_REQUEST.value(), "MISSING_PARAMETER", paramName);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "MISSING_PARAMETER",
        message,
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
      MissingRequestHeaderException ex,
      HttpServletRequest request) {
    String uri = sanitizeUri(request);
    String headerName = ex.getHeaderName() != null ? ex.getHeaderName() : "header";
    if (containsSensitiveData(headerName)) {
      headerName = "header";
    }
    String message = String.format("Required header '%s' is missing", headerName);

    log.warn("Missing request header on {}: status={}, code={}, header='{}'",
        uri, HttpStatus.BAD_REQUEST.value(), "MISSING_HEADER", headerName);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "MISSING_HEADER",
        message,
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<ErrorResponse> handleServletRequestBinding(
      ServletRequestBindingException ex,
      HttpServletRequest request) {
    String uri = sanitizeUri(request);
    log.warn("Servlet request binding error on {}: status={}, code={}, exception={}",
        uri, HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getClass().getSimpleName());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "BAD_REQUEST",
        "Invalid request binding or missing parameter/header",
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MissingPathVariableException.class)
  public ResponseEntity<ErrorResponse> handleMissingPathVariable(
      MissingPathVariableException ex,
      HttpServletRequest request) {
    String uri = sanitizeUri(request);
    // MissingPathVariableException indicates a server-side route/controller parameter mismatch
    // (a controller method expects a URI variable not defined in its @RequestMapping pattern).
    // This is an internal configuration/server error (HTTP 500), not an invalid client request.
    log.error("Server-side path variable configuration mismatch on {}: status={}, code={}, variable='{}', exception={}",
        uri, HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", ex.getVariableName(), ex.getClass().getSimpleName());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred. Please try again later.",
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
    String uri = sanitizeUri(request);
    String clientMessage = "Malformed or unreadable request body";
    List<String> details = null;

    Throwable cause = ex.getCause();
    if (cause instanceof InvalidFormatException ife) {
      Class<?> targetType = ife.getTargetType();
      String fieldName = extractJacksonFieldPath(ife.getPath());
      if (targetType != null && targetType.isEnum()) {
        clientMessage = fieldName != null
            ? "Invalid enum value for field '" + fieldName + "'"
            : "Invalid enum value in request body";
        if (fieldName != null) {
          details = List.of(fieldName + ": invalid enum value");
        }
      } else if (targetType != null && isDateTimeType(targetType)) {
        clientMessage = fieldName != null
            ? "Invalid date/time format for field '" + fieldName + "'"
            : "Invalid date/time format in request body";
        if (fieldName != null) {
          details = List.of(fieldName + ": invalid date/time format");
        }
      } else {
        clientMessage = fieldName != null
            ? "Invalid value format for field '" + fieldName + "'"
            : "Invalid value format in request body";
        if (fieldName != null) {
          details = List.of(fieldName + ": invalid format");
        }
      }
    } else if (cause instanceof MismatchedInputException mie) {
      Class<?> targetType = mie.getTargetType();
      String fieldName = extractJacksonFieldPath(mie.getPath());
      if (targetType != null && isDateTimeType(targetType)) {
        clientMessage = fieldName != null
            ? "Invalid date/time format for field '" + fieldName + "'"
            : "Invalid date/time format in request body";
        if (fieldName != null) {
          details = List.of(fieldName + ": invalid date/time format");
        }
      } else {
        clientMessage = fieldName != null
            ? "Invalid value format for field '" + fieldName + "'"
            : "Invalid value format in request body";
        if (fieldName != null) {
          details = List.of(fieldName + ": invalid format");
        }
      }
    } else if (hasCauseOfType(ex, java.time.format.DateTimeParseException.class)) {
      clientMessage = "Invalid date/time format in request body";
    } else if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
      clientMessage = "Required request body is missing";
    }

    log.warn("Malformed or unreadable HTTP message on {}: status={}, code={}, reason={}",
        uri, HttpStatus.BAD_REQUEST.value(), "MALFORMED_JSON", clientMessage);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "MALFORMED_JSON",
        clientMessage,
        uri,
        details
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    String uri = sanitizeUri(request);
    String method = ex.getMethod() != null ? ex.getMethod() : "UNKNOWN";
    if (containsSensitiveData(method)) {
      method = "UNKNOWN";
    }
    log.warn("Method not supported on {}: status={}, code={}, method={}",
        uri, HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED", method);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.METHOD_NOT_ALLOWED.value(),
        HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
        "METHOD_NOT_ALLOWED",
        "HTTP method '" + method + "' is not supported for this endpoint",
        uri
    );
    HttpHeaders headers = new HttpHeaders();
    if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
      headers.setAllow(ex.getSupportedHttpMethods());
    }
    return buildSanitizedResponse(errorResponse, headers, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex,
      HttpServletRequest request) {
    String uri = sanitizeUri(request);
    String contentType = ex.getContentType() != null ? ex.getContentType().toString() : "unknown";
    if (containsSensitiveData(contentType)) {
      contentType = "unsupported";
    }
    log.warn("Media type not supported on {}: status={}, code={}, contentType={}",
        uri, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "UNSUPPORTED_MEDIA_TYPE", contentType);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
        "UNSUPPORTED_MEDIA_TYPE",
        "Content type '" + contentType + "' is not supported",
        uri
    );
    HttpHeaders headers = new HttpHeaders();
    if (ex.getSupportedMediaTypes() != null && !ex.getSupportedMediaTypes().isEmpty()) {
      headers.setAccept(ex.getSupportedMediaTypes());
    }
    return buildSanitizedResponse(errorResponse, headers, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException ex,
      HttpServletRequest request) {
    String uri = sanitizeUri(request);
    log.warn("Media type not acceptable on {}: status={}, code={}",
        uri, HttpStatus.NOT_ACCEPTABLE.value(), "NOT_ACCEPTABLE");
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.NOT_ACCEPTABLE.value(),
        HttpStatus.NOT_ACCEPTABLE.getReasonPhrase(),
        "NOT_ACCEPTABLE",
        "Requested media type is not acceptable",
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler({NoResourceFoundException.class, org.springframework.web.servlet.NoHandlerFoundException.class})
  public ResponseEntity<ErrorResponse> handleNoResourceFound(Exception ex, HttpServletRequest request) {
    String sanitizedUri = sanitizeUri(request);
    if (sanitizedUri.isBlank()) {
      sanitizedUri = "/sanitized-path";
    }
    log.warn("Resource not found on {}: status={}, code={}",
        sanitizedUri, HttpStatus.NOT_FOUND.value(), "ENDPOINT_NOT_FOUND");
    String message = "/sanitized-path".equals(sanitizedUri) || sanitizedUri.isBlank()
        ? "No static resource or endpoint found"
        : "No static resource or endpoint found for path: " + sanitizedUri;
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        "ENDPOINT_NOT_FOUND",
        message,
        sanitizedUri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public void handleAccessDeniedException(AccessDeniedException ex) {
    throw ex;
  }

  @ExceptionHandler(AuthenticationException.class)
  public void handleAuthenticationException(AuthenticationException ex) {
    throw ex;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex,
      HttpServletRequest request) {
    String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
    String errorMsg = rootMsg != null ? rootMsg : "";

    // 1. Employment spell and employment records
    if (errorMsg.contains("uk_employment_records_current_per_type")) {
      return conflictResponse("ACTIVE_SPELL_ALREADY_EXISTS",
          "Trainee already has an active current spell for this engagement type", request);
    }
    if (errorMsg.contains("uk_employment_records_placement")) {
      return conflictResponse("DUPLICATE_PLACEMENT_EMPLOYMENT",
          "Placement record is already linked to another employment record", request);
    }
    if (errorMsg.contains("uk_employment_records_number")) {
      return conflictResponse("EMPLOYMENT_NUMBER_ALREADY_EXISTS",
          "Employment number already exists", request);
    }
    if (errorMsg.contains("uk_employment_records_trainee_employer_start")) {
      return conflictResponse("DUPLICATE_TRAINEE_EMPLOYER_START",
          "Employment record already exists for trainee, employer and start date", request);
    }
    if (errorMsg.contains("uk_employment_exit_events_employment")) {
      return conflictResponse("EXIT_EVENT_ALREADY_EXISTS",
          "Employment exit event already exists for this employment record", request);
    }
    if (errorMsg.contains("uk_salary_history_employment_from")) {
      return conflictResponse("DUPLICATE_EMPLOYMENT_EFFECTIVE_FROM",
          "Salary history already exists for this employment record and effective from date", request);
    }
    if (errorMsg.contains("uk_salary_history_employment_milestone")) {
      return conflictResponse("DUPLICATE_EMPLOYMENT_MILESTONE",
          "Salary history already exists for this employment record and observation milestone", request);
    }

    // 2. Employment verification requests
    if (errorMsg.contains("uk_emp_verif_requests_open")) {
      return conflictResponse("OPEN_REQUEST_EXISTS",
          "An open verification request already exists for this employment record", request);
    }
    if (errorMsg.contains("uk_emp_verif_requests_cycle")) {
      return conflictResponse("DUPLICATE_CYCLE_NUMBER",
          "Verification request for this employment record and cycle number already exists", request);
    }
    if (errorMsg.contains("uk_emp_verif_requests_number")) {
      return conflictResponse("DUPLICATE_REQUEST_NUMBER",
          "Request number already exists", request);
    }

    // 3. Employment verification records (Phase 25.4)
    if (errorMsg.contains("uk_emp_verifications_current")) {
      return conflictResponse("ACTIVE_VERIFICATION_ALREADY_EXISTS",
          "Employment record already has an active verification", request);
    }
    if (errorMsg.contains("uk_emp_verifications_cycle")) {
      return conflictResponse("DUPLICATE_VERIFICATION_CYCLE",
          "Verification for this employment record and cycle number already exists", request);
    }
    if (errorMsg.contains("uk_emp_verifications_number")) {
      return conflictResponse("DUPLICATE_VERIFICATION_NUMBER",
          "Verification number already exists", request);
    }
    if (errorMsg.contains("uk_emp_verifications_request")) {
      return conflictResponse("DUPLICATE_VERIFICATION_REQUEST",
          "Verification record already exists for this verification request", request);
    }

    // 4. Unemployment events
    if (errorMsg.contains("uk_unemployment_events_current")) {
      return conflictResponse("ACTIVE_PERIOD_ALREADY_EXISTS",
          "Trainee already has an active unemployment period", request);
    }
    if (errorMsg.contains("uk_unemployment_events_trainee_period")) {
      return conflictResponse("DUPLICATE_UNEMPLOYMENT_PERIOD",
          "Unemployment event already exists for this trainee and period", request);
    }
    if (errorMsg.contains("uk_unemployment_events_trainee_start")) {
      return conflictResponse("UNEMPLOYMENT_START_DATE_EXISTS",
          "An unemployment event already exists for trainee on start date", request);
    }
    if (errorMsg.contains("uk_unemployment_events_exit")) {
      return conflictResponse("DUPLICATE_EXIT_UNEMPLOYMENT",
          "Unemployment event already exists for this exit event", request);
    }

    // 5. Skill gaps and recommendations (Phase 25.5)
    if (errorMsg.contains("uk_skill_gaps_current")) {
      return conflictResponse("ACTIVE_SKILL_GAP_ALREADY_EXISTS",
          "Trainee already has an active current skill gap for this skill", request);
    }
    if (errorMsg.contains("uk_skill_gaps_assessment_skill")) {
      return conflictResponse("DUPLICATE_ASSESSMENT_SKILL_GAP",
          "Skill gap already exists for this assessment and skill", request);
    }
    if (errorMsg.contains("uk_skill_gaps_number")) {
      return conflictResponse("DUPLICATE_SKILL_GAP_NUMBER",
          "Skill gap number already exists", request);
    }
    if (errorMsg.contains("uk_skill_gap_assessments_number")) {
      return conflictResponse("DUPLICATE_ASSESSMENT_NUMBER",
          "Skill gap assessment number already exists", request);
    }
    if (errorMsg.contains("uk_sgr_number")) {
      return conflictResponse("DUPLICATE_RECOMMENDATION_NUMBER",
          "Skill gap recommendation number already exists", request);
    }
    if (errorMsg.contains("uk_sgr_gap_action_course")) {
      return conflictResponse("DUPLICATE_GAP_ACTION_COURSE",
          "A recommendation with this action type and course already exists for this skill gap", request);
    }

    // 6. Training enrollments, batches, attendance, courses
    if (errorMsg.contains("uk_training_enrollments_trainee_batch")) {
      return conflictResponse("DUPLICATE_ENROLLMENT",
          "Trainee is already enrolled in this training batch", request);
    }
    if (errorMsg.contains("uk_training_enrollments_number")) {
      return conflictResponse("DUPLICATE_ENROLLMENT_NUMBER",
          "Training enrollment number already exists", request);
    }
    if (errorMsg.contains("uk_training_batches_code")) {
      return conflictResponse("BATCH_CODE_ALREADY_EXISTS",
          "Batch code already exists", request);
    }
    if (errorMsg.contains("uk_attendance_enrollment_session")) {
      return conflictResponse("ATTENDANCE_ALREADY_EXISTS",
          "Attendance record already exists for this enrollment, date, and sequence", request);
    }
    if (errorMsg.contains("uk_courses_code")) {
      return conflictResponse("COURSE_CODE_ALREADY_EXISTS",
          "Course code already exists", request);
    }
    if (errorMsg.contains("uk_training_centers_code")) {
      return conflictResponse("CENTER_CODE_ALREADY_EXISTS",
          "Center code already exists", request);
    }
    if (errorMsg.contains("uk_training_providers_code")) {
      return conflictResponse("PROVIDER_CODE_ALREADY_EXISTS",
          "Provider code already exists", request);
    }
    if (errorMsg.contains("uk_training_providers_registration")) {
      return conflictResponse("REGISTRATION_NUMBER_ALREADY_EXISTS",
          "Registration number already exists", request);
    }

    // 7. Users and identity
    if (errorMsg.contains("uk_users_username")) {
      return conflictResponse("USERNAME_ALREADY_EXISTS",
          "Username already exists", request);
    }
    if (errorMsg.contains("uk_users_email")) {
      return conflictResponse("EMAIL_ALREADY_EXISTS",
          "Email already exists", request);
    }
    if (errorMsg.contains("uk_trainees_registration_number")) {
      return conflictResponse("DUPLICATE_TRAINEE_REGISTRATION",
          "Trainee registration number already exists", request);
    }
    if (errorMsg.contains("uk_trainees_user_id")) {
      return conflictResponse("USER_ALREADY_LINKED",
          "Trainee already linked to this user ID", request);
    }

    // 8. Placements, surveys, follow-ups
    if (errorMsg.contains("uk_placement_records_number")) {
      return conflictResponse("PLACEMENT_NUMBER_ALREADY_EXISTS",
          "Placement number already exists", request);
    }
    if (errorMsg.contains("uk_placement_records_application")) {
      return conflictResponse("DUPLICATE_PLACEMENT_APPLICATION",
          "Placement record already exists for this job application", request);
    }
    if (errorMsg.contains("uk_placement_records_enrollment_posting")) {
      return conflictResponse("DUPLICATE_PLACEMENT_ENROLLMENT_POSTING",
          "Placement record already exists for this enrollment and job posting", request);
    }
    if (errorMsg.contains("uk_job_applications_trainee_posting")) {
      return conflictResponse("DUPLICATE_JOB_APPLICATION",
          "Trainee has already applied for this job posting", request);
    }
    if (errorMsg.contains("uk_job_postings_code")) {
      return conflictResponse("POSTING_CODE_ALREADY_EXISTS",
          "Job posting code already exists", request);
    }
    if (errorMsg.contains("uk_followup_campaigns_code")) {
      return conflictResponse("CAMPAIGN_CODE_ALREADY_EXISTS",
          "Campaign code already exists", request);
    }
    if (errorMsg.contains("uk_followup_tasks_campaign_trainee")) {
      return conflictResponse("DUPLICATE_FOLLOWUP_TASK",
          "Follow-up task already exists for this campaign and trainee", request);
    }
    if (errorMsg.contains("uk_survey_responses_attempt")) {
      return conflictResponse("DUPLICATE_SURVEY_RESPONSE",
          "Survey response already exists for this survey, trainee, and attempt", request);
    }
    if (errorMsg.contains("uk_survey_response_answers_question")) {
      return conflictResponse("DUPLICATE_RESPONSE_ANSWER",
          "Answer already exists for this survey response and question", request);
    }
    if (errorMsg.contains("uk_communication_logs_provider_message")) {
      return conflictResponse("DUPLICATE_PROVIDER_MESSAGE_ID",
          "Communication log already exists for this provider message ID", request);
    }

    // 9. Assessments and Certifications (Phase 25.10)
    if (errorMsg.contains("uk_assessments_code")) {
      return conflictResponse("ASSESSMENT_CODE_ALREADY_EXISTS",
          "Assessment code already exists", request);
    }
    if (errorMsg.contains("uk_trainee_assessments_attempt")) {
      return conflictResponse("TRAINEE_ASSESSMENT_ATTEMPT_ALREADY_EXISTS",
          "Trainee assessment attempt already exists", request);
    }
    if (errorMsg.contains("uk_assessment_results_attempt")) {
      return conflictResponse("ASSESSMENT_RESULT_ALREADY_EXISTS",
          "Assessment result already exists for this trainee assessment", request);
    }
    if (errorMsg.contains("uk_certifications_certificate_number")) {
      return conflictResponse("CERTIFICATE_NUMBER_ALREADY_EXISTS",
          "Certificate number already exists", request);
    }

    // 10. Employers and Organizations
    if (errorMsg.contains("uk_employers_code")) {
      return conflictResponse("EMPLOYER_CODE_ALREADY_EXISTS",
          "Employer code already exists", request);
    }
    if (errorMsg.contains("uk_employers_registration")) {
      return conflictResponse("REGISTRATION_NUMBER_ALREADY_EXISTS",
          "Registration number already exists", request);
    }
    if (errorMsg.contains("uk_employers_gstin")) {
      return conflictResponse("GSTIN_ALREADY_EXISTS",
          "GSTIN already exists", request);
    }

    // 11. Security, Roles, Skills, Schemes, Programs, Data Quality
    if (errorMsg.contains("uk_roles_code")) {
      return conflictResponse("ROLE_CODE_ALREADY_EXISTS",
          "Role code already exists", request);
    }
    if (errorMsg.contains("uk_permissions_code")) {
      return conflictResponse("PERMISSION_CODE_ALREADY_EXISTS",
          "Permission code already exists", request);
    }
    if (errorMsg.contains("uk_job_roles_code")) {
      return conflictResponse("JOB_ROLE_CODE_ALREADY_EXISTS",
          "Job role code already exists", request);
    }
    if (errorMsg.contains("uk_skills_code")) {
      return conflictResponse("SKILL_CODE_ALREADY_EXISTS",
          "Skill code already exists", request);
    }
    if (errorMsg.contains("uk_skills_name")) {
      return conflictResponse("SKILL_NAME_ALREADY_EXISTS",
          "Skill name already exists", request);
    }
    if (errorMsg.contains("uk_programs_code")) {
      return conflictResponse("PROGRAM_CODE_ALREADY_EXISTS",
          "Program code already exists", request);
    }
    if (errorMsg.contains("uk_schemes_code")) {
      return conflictResponse("SCHEME_CODE_ALREADY_EXISTS",
          "Scheme code already exists", request);
    }
    if (errorMsg.contains("uk_data_quality_rules_code")) {
      return conflictResponse("RULE_CODE_EXISTS",
          "Rule code already exists", request);
    }
    if (errorMsg.contains("uk_import_batches_code")) {
      return conflictResponse("BATCH_CODE_EXISTS",
          "Batch code already exists", request);
    }

    // 12. Additional domain unique constraints
    if (errorMsg.contains("uk_emp_verif_attempts_number")) {
      return conflictResponse("DUPLICATE_VERIFICATION_ATTEMPT_NUMBER",
          "Verification attempt number already exists", request);
    }
    if (errorMsg.contains("uk_emp_verif_evidence_reference")) {
      return conflictResponse("DUPLICATE_EVIDENCE_REFERENCE",
          "Verification evidence reference already exists", request);
    }
    if (errorMsg.contains("uk_employer_branches_code")) {
      return conflictResponse("BRANCH_CODE_ALREADY_EXISTS",
          "Employer branch code already exists", request);
    }
    if (errorMsg.contains("uk_import_records_batch_row")) {
      return conflictResponse("DUPLICATE_IMPORT_ROW",
          "Import record already exists for this batch and row", request);
    }
    if (errorMsg.contains("uk_skill_gap_observations_number")) {
      return conflictResponse("DUPLICATE_OBSERVATION_NUMBER",
          "Skill gap observation number already exists", request);
    }
    if (errorMsg.contains("uk_survey_questions_version_code")) {
      return conflictResponse("DUPLICATE_QUESTION_CODE",
          "Survey question already exists for this survey, version, and question code", request);
    }
    if (errorMsg.contains("uk_survey_questions_version_order")) {
      return conflictResponse("DUPLICATE_QUESTION_ORDER",
          "Survey question order already exists for this survey, version, and order number", request);
    }

    // 13. Generic unique constraint / duplicate entry fallback
    if (errorMsg.contains("Duplicate entry") || errorMsg.contains("duplicate key")) {
      return conflictResponse("RESOURCE_CONFLICT",
          "A resource with the specified unique attributes already exists", request);
    }

    // 14. Non-duplicate integrity violations (foreign keys, nulls, check constraints)
    String uri = sanitizeUri(request);
    log.error("Unrecognized database integrity violation on {}: status={}, code={}",
        uri, HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR");
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "INTERNAL_SERVER_ERROR",
        "An unexpected database error occurred. Please try again later.",
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> conflictResponse(String code, String message, HttpServletRequest request) {
    String uri = sanitizeUri(request);
    log.warn("Database integrity conflict on {}: status={}, code={}",
        uri, HttpStatus.CONFLICT.value(), code);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.CONFLICT.value(),
        HttpStatus.CONFLICT.getReasonPhrase(),
        code,
        message,
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    String uri = sanitizeUri(request);
    log.error("Unhandled exception processing request to {}: status={}, code={}, exception={}",
        uri, HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", ex.getClass().getName());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred. Please try again later.",
        uri
    );
    return buildSanitizedResponse(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
