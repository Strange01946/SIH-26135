package in.gov.sih.sih26135.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Contract and verification test suite for Phase 28.1 standardized operational error codes,
 * operational exception classes, GlobalExceptionHandler error resolution, and architectural invariants.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operational Error Codes & Architecture Invariant Tests")
class OperationalErrorCodeContractTest {

  private GlobalExceptionHandler exceptionHandler;

  @Mock
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
    lenient().when(request.getRequestURI()).thenReturn("/api/v1/operational-test");
    lenient().when(request.getAttribute(
        org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
        .thenAnswer(inv -> request.getRequestURI());
  }

  @Test
  @DisplayName("Standardized operational error code constants are defined and unique")
  void testOperationalErrorCodeConstants() {
    assertThat(OperationalErrorCodes.AUDIT_LOG_IMMUTABLE).isEqualTo("AUDIT_LOG_IMMUTABLE");
    assertThat(OperationalErrorCodes.DQ_RULE_NOT_FOUND).isEqualTo("DQ_RULE_NOT_FOUND");
    assertThat(OperationalErrorCodes.INVALID_DQ_TRANSITION).isEqualTo("INVALID_DQ_TRANSITION");
    assertThat(OperationalErrorCodes.IMPORT_BATCH_TERMINAL).isEqualTo("IMPORT_BATCH_TERMINAL");
    assertThat(OperationalErrorCodes.IMPORT_ROW_LIMIT_EXCEEDED).isEqualTo("IMPORT_ROW_LIMIT_EXCEEDED");

    List<String> codes = List.of(
        OperationalErrorCodes.AUDIT_LOG_IMMUTABLE,
        OperationalErrorCodes.DQ_RULE_NOT_FOUND,
        OperationalErrorCodes.INVALID_DQ_TRANSITION,
        OperationalErrorCodes.IMPORT_BATCH_TERMINAL,
        OperationalErrorCodes.IMPORT_ROW_LIMIT_EXCEEDED
    );
    assertThat(codes).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("AuditLogImmutableException maps to HTTP 405 Method Not Allowed")
  void testAuditLogImmutableException() {
    AuditLogImmutableException ex = new AuditLogImmutableException("Cannot modify audit log record id=101");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(ex.getErrorCode()).isEqualTo(OperationalErrorCodes.AUDIT_LOG_IMMUTABLE);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(OperationalErrorCodes.AUDIT_LOG_IMMUTABLE);
    assertThat(response.getBody().getMessage())
        .isEqualTo("Audit log records are immutable and cannot be modified or deleted");
  }

  @Test
  @DisplayName("DataQualityRuleNotFoundException maps to HTTP 404 Not Found")
  void testDataQualityRuleNotFoundException() {
    DataQualityRuleNotFoundException ex = new DataQualityRuleNotFoundException("Data quality rule not found with id: 42");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex.getErrorCode()).isEqualTo(OperationalErrorCodes.DQ_RULE_NOT_FOUND);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(OperationalErrorCodes.DQ_RULE_NOT_FOUND);
    assertThat(response.getBody().getMessage())
        .isEqualTo("Data quality rule not found");
  }

  @Test
  @DisplayName("InvalidDataQualityTransitionException maps to HTTP 409 Conflict")
  void testInvalidDataQualityTransitionException() {
    InvalidDataQualityTransitionException ex = new InvalidDataQualityTransitionException("Cannot transition data quality issue from RESOLVED to OPEN");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex.getErrorCode()).isEqualTo(OperationalErrorCodes.INVALID_DQ_TRANSITION);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(OperationalErrorCodes.INVALID_DQ_TRANSITION);
    assertThat(response.getBody().getMessage())
        .isEqualTo("Invalid data quality issue state transition");
  }

  @Test
  @DisplayName("ImportBatchTerminalException maps to HTTP 409 Conflict")
  void testImportBatchTerminalException() {
    ImportBatchTerminalException ex = new ImportBatchTerminalException("Import batch id=55 is in terminal status: COMPLETED");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex.getErrorCode()).isEqualTo(OperationalErrorCodes.IMPORT_BATCH_TERMINAL);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(OperationalErrorCodes.IMPORT_BATCH_TERMINAL);
    assertThat(response.getBody().getMessage())
        .isEqualTo("Import batch is in a terminal state and cannot be modified");
  }

  @Test
  @DisplayName("ImportRowLimitExceededException maps to HTTP 400 Bad Request")
  void testImportRowLimitExceededException() {
    ImportRowLimitExceededException ex = new ImportRowLimitExceededException(15000, 10000);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getErrorCode()).isEqualTo(OperationalErrorCodes.IMPORT_ROW_LIMIT_EXCEEDED);
    assertThat(ex.getRowCount()).isEqualTo(15000);
    assertThat(ex.getMaxLimit()).isEqualTo(10000);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(OperationalErrorCodes.IMPORT_ROW_LIMIT_EXCEEDED);
    assertThat(response.getBody().getMessage())
        .isEqualTo("Requested import exceeds maximum allowed row limit");
  }

  // =========================================================================
  // ARCHITECTURAL INVARIANT TESTS
  // =========================================================================

  @Test
  @DisplayName("Architectural Invariant: Exactly 137 Spring Data JPA repository interfaces are preserved")
  void testRepositoryInterfaceCountInvariant() throws IOException {
    Path srcMain = Paths.get("src/main/java");
    if (!Files.exists(srcMain)) {
      srcMain = Paths.get("backend/src/main/java");
    }

    long repositoryCount;
    try (Stream<Path> stream = Files.walk(srcMain)) {
      repositoryCount = stream
          .filter(p -> p.getFileName().toString().endsWith("Repository.java"))
          .filter(p -> {
            try {
              String content = Files.readString(p);
              return content.contains("public interface ");
            } catch (IOException e) {
              return false;
            }
          })
          .count();
    }

    assertThat(repositoryCount)
        .as("Phase 28.1 invariant: Exactly 137 Spring Data repository interfaces must be preserved")
        .isEqualTo(137L);
  }

  @Test
  @DisplayName("Architectural Invariant: Exactly 2 @Query annotations are preserved")
  void testQueryAnnotationCountInvariant() throws IOException {
    Path srcMain = Paths.get("src/main/java");
    if (!Files.exists(srcMain)) {
      srcMain = Paths.get("backend/src/main/java");
    }

    long queryAnnotationCount;
    try (Stream<Path> stream = Files.walk(srcMain)) {
      queryAnnotationCount = stream
          .filter(p -> p.getFileName().toString().endsWith(".java"))
          .mapToLong(p -> {
            try {
              return Files.lines(p)
                  .filter(line -> line.contains("@Query("))
                  .count();
            } catch (IOException e) {
              return 0L;
            }
          })
          .sum();
    }

    assertThat(queryAnnotationCount)
        .as("Phase 28.1 invariant: Exactly 2 @Query annotations must be preserved")
        .isEqualTo(2L);
  }
}
