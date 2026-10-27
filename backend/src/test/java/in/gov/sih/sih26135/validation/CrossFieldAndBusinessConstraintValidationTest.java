package in.gov.sih.sih26135.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateJobPostingRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.response.JobPostingResponse;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.dto.response.TraineeResponse;
import in.gov.sih.sih26135.dto.response.TrainingBatchResponse;
import in.gov.sih.sih26135.exception.ErrorResponse;
import in.gov.sih.sih26135.exception.GlobalExceptionHandler;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.JobPostingService;
import in.gov.sih.sih26135.service.SkillGapService;
import in.gov.sih.sih26135.service.TraineeService;
import in.gov.sih.sih26135.service.TrainingBatchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive verification test suite for Phase 26.6:
 * Cross-Field & Business Constraint Validations.
 *
 * <p>Verifies:
 * <ol>
 *   <li>Declarative cross-field date sequencing (endDate >= startDate, resolvedOn >= identifiedOn).</li>
 *   <li>Declarative financial boundaries (maxSalary >= minSalary, non-negative wages).</li>
 *   <li>Date-of-birth sanity validation (past date, age >= 15 with calendar and leap-year semantics).</li>
 *   <li>Deterministic field-level error keys and structured API error responses.</li>
 *   <li>Fail-fast validator configuration and type compatibility checks.</li>
 *   <li>Comprehensive programmatic DTO constraint-mapping inspection across all request DTOs.</li>
 *   <li>Controller-boundary rejection preventing business logic and persistence execution.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.6 - Cross-Field & Business Constraint Validations")
public class CrossFieldAndBusinessConstraintValidationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private JobPostingService jobPostingService;

  @MockBean
  private TrainingBatchService trainingBatchService;

  @MockBean
  private SkillGapService skillGapService;

  @MockBean
  private TraineeService traineeService;

  private Validator validator;

  private String tokenPlacementManage;
  private String tokenCourseManage;
  private String tokenAssessmentManage;
  private String tokenTraineeWrite;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    this.validator = factory.getValidator();

    tokenPlacementManage = "Bearer " + jwtTokenProvider.generateAccessToken(101L, "placementUser", List.of("placement.manage"));
    tokenCourseManage = "Bearer " + jwtTokenProvider.generateAccessToken(102L, "courseUser", List.of("course.manage"));
    tokenAssessmentManage = "Bearer " + jwtTokenProvider.generateAccessToken(103L, "assessmentUser", List.of("assessment.manage"));
    tokenTraineeWrite = "Bearer " + jwtTokenProvider.generateAccessToken(104L, "traineeUser", List.of("trainee.write"));
  }

  // =========================================================================
  // 1. Cross-Field Date Sequence Validation Tests
  // =========================================================================

  @Nested
  @DisplayName("1. Cross-Field Date Sequence Validation")
  class CrossFieldDateSequenceTests {

    @DateSequence(startField = "startDate", endField = "endDate", allowEqual = false)
    public static class StrictDateSequenceDto {
      private LocalDate startDate;
      private LocalDate endDate;

      public LocalDate getStartDate() { return startDate; }
      public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
      public LocalDate getEndDate() { return endDate; }
      public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    @DateSequence(startField = "nonExistentStart", endField = "endDate")
    public static class MissingStartPropertyDateSequenceDto {
      private LocalDate endDate = LocalDate.now();
      public LocalDate getEndDate() { return endDate; }
      public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    @DateSequence(startField = "startDate", endField = "nonExistentEnd")
    public static class MissingEndPropertyDateSequenceDto {
      private LocalDate startDate = LocalDate.now();
      public LocalDate getStartDate() { return startDate; }
      public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    }

    @DateSequence(startField = "startDate", endField = "endDate", targetField = "nonExistentTarget")
    public static class MissingTargetPropertyDateSequenceDto {
      private LocalDate startDate = LocalDate.now();
      private LocalDate endDate = LocalDate.now();
      public LocalDate getStartDate() { return startDate; }
      public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
      public LocalDate getEndDate() { return endDate; }
      public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    @DateSequence(startField = "startDate", endField = "stringField")
    public static class IncompatibleTypeDateSequenceDto {
      private LocalDate startDate = LocalDate.now();
      private String stringField = "2026-12-31";
      public LocalDate getStartDate() { return startDate; }
      public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
      public String getStringField() { return stringField; }
      public void setStringField(String stringField) { this.stringField = stringField; }
    }

    @DateSequence(startField = "startDate", endField = "dateTimeField")
    public static class MismatchedDateTypesDateSequenceDto {
      private LocalDate startDate = LocalDate.now();
      private LocalDateTime dateTimeField = LocalDateTime.now();
      public LocalDate getStartDate() { return startDate; }
      public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
      public LocalDateTime getDateTimeField() { return dateTimeField; }
      public void setDateTimeField(LocalDateTime dateTimeField) { this.dateTimeField = dateTimeField; }
    }

    @Test
    @DisplayName("endDate earlier than startDate in CreateTrainingBatchRequest is rejected on endDate")
    void testDateSequence_earlierEndDate_rejected() {
      CreateTrainingBatchRequest request = new CreateTrainingBatchRequest();
      request.setBatchCode("BATCH-2026-001");
      request.setStartDate(LocalDate.of(2026, 6, 1));
      request.setEndDate(LocalDate.of(2026, 5, 31)); // 1 day before start date

      Set<ConstraintViolation<CreateTrainingBatchRequest>> violations = validator.validate(request);
      assertThat(violations).isNotEmpty();

      ConstraintViolation<CreateTrainingBatchRequest> violation = violations.stream()
          .filter(v -> "endDate".equals(v.getPropertyPath().toString()))
          .findFirst()
          .orElse(null);

      assertThat(violation).isNotNull();
      assertThat(violation.getMessage()).contains("must be greater than or equal to start date");
    }

    @Test
    @DisplayName("endDate equal to startDate is accepted when allowEqual is true")
    void testDateSequence_equalDates_accepted() {
      CreateTrainingBatchRequest request = new CreateTrainingBatchRequest();
      request.setBatchCode("BATCH-2026-001");
      request.setStartDate(LocalDate.of(2026, 6, 1));
      request.setEndDate(LocalDate.of(2026, 6, 1)); // Single-day batch

      Set<ConstraintViolation<CreateTrainingBatchRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("endDate equal to startDate is rejected when allowEqual is false")
    void testDateSequence_allowEqualFalse_equalDates_rejected() {
      StrictDateSequenceDto request = new StrictDateSequenceDto();
      request.setStartDate(LocalDate.of(2026, 6, 1));
      request.setEndDate(LocalDate.of(2026, 6, 1));

      Set<ConstraintViolation<StrictDateSequenceDto>> violations = validator.validate(request);
      assertThat(violations).isNotEmpty();
      assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("endDate");

      // Valid strictly later date passes
      request.setEndDate(LocalDate.of(2026, 6, 2));
      assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("endDate later than startDate is accepted")
    void testDateSequence_laterEndDate_accepted() {
      CreateTrainingBatchRequest request = new CreateTrainingBatchRequest();
      request.setBatchCode("BATCH-2026-001");
      request.setStartDate(LocalDate.of(2026, 6, 1));
      request.setEndDate(LocalDate.of(2026, 12, 31));

      Set<ConstraintViolation<CreateTrainingBatchRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Null startDate or null endDate passes date sequencing (nullable contract)")
    void testDateSequence_nullableDates_accepted() {
      CreateTrainingBatchRequest requestWithoutEnd = new CreateTrainingBatchRequest();
      requestWithoutEnd.setBatchCode("BATCH-2026-001");
      requestWithoutEnd.setStartDate(LocalDate.of(2026, 6, 1));
      requestWithoutEnd.setEndDate(null);

      assertThat(validator.validate(requestWithoutEnd)).isEmpty();

      CreateTrainingBatchRequest requestWithoutStart = new CreateTrainingBatchRequest();
      requestWithoutStart.setBatchCode("BATCH-2026-001");
      requestWithoutStart.setStartDate(null);
      requestWithoutStart.setEndDate(LocalDate.of(2026, 12, 31));

      assertThat(validator.validate(requestWithoutStart)).isEmpty();
    }

    @Test
    @DisplayName("resolvedOn earlier than identifiedOn in CreateSkillGapRequest is rejected on resolvedOn")
    void testSkillGapDateSequence_earlierResolvedOn_rejected() {
      CreateSkillGapRequest request = new CreateSkillGapRequest();
      request.setIdentifiedOn(LocalDate.of(2026, 3, 15));
      request.setResolvedOn(LocalDate.of(2026, 3, 14)); // Earlier than identifiedOn

      Set<ConstraintViolation<CreateSkillGapRequest>> violations = validator.validate(request);
      assertThat(violations).isNotEmpty();

      boolean hasResolvedOnViolation = violations.stream()
          .anyMatch(v -> "resolvedOn".equals(v.getPropertyPath().toString()));
      assertThat(hasResolvedOnViolation).isTrue();
    }

    @Test
    @DisplayName("resolvedOn on or after identifiedOn in CreateSkillGapRequest is accepted")
    void testSkillGapDateSequence_valid_accepted() {
      CreateSkillGapRequest requestEqual = new CreateSkillGapRequest();
      requestEqual.setIdentifiedOn(LocalDate.of(2026, 3, 15));
      requestEqual.setResolvedOn(LocalDate.of(2026, 3, 15));
      assertThat(validator.validate(requestEqual)).isEmpty();

      CreateSkillGapRequest requestLater = new CreateSkillGapRequest();
      requestLater.setIdentifiedOn(LocalDate.of(2026, 3, 15));
      requestLater.setResolvedOn(LocalDate.of(2026, 4, 1));
      assertThat(validator.validate(requestLater)).isEmpty();
    }

    @Test
    @DisplayName("Missing startField fails fast with ValidationException (root cause IllegalArgumentException)")
    void testDateSequence_missingStartProperty_failsFastWithValidationException() {
      MissingStartPropertyDateSequenceDto dto = new MissingStartPropertyDateSequenceDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @DateSequence configuration on " + MissingStartPropertyDateSequenceDto.class.getName()
              + ": startField 'nonExistentStart' does not exist or is not accessible");
    }

    @Test
    @DisplayName("Missing endField fails fast with ValidationException (root cause IllegalArgumentException)")
    void testDateSequence_missingEndProperty_failsFastWithValidationException() {
      MissingEndPropertyDateSequenceDto dto = new MissingEndPropertyDateSequenceDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @DateSequence configuration on " + MissingEndPropertyDateSequenceDto.class.getName()
              + ": endField 'nonExistentEnd' does not exist or is not accessible");
    }

    @Test
    @DisplayName("Missing targetField fails fast with ValidationException (root cause IllegalArgumentException)")
    void testDateSequence_missingTargetProperty_failsFastWithValidationException() {
      MissingTargetPropertyDateSequenceDto dto = new MissingTargetPropertyDateSequenceDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @DateSequence configuration on " + MissingTargetPropertyDateSequenceDto.class.getName()
              + ": targetField 'nonExistentTarget' does not exist or is not accessible");
    }

    @Test
    @DisplayName("Incompatible property type (non-date) fails fast with ValidationException")
    void testDateSequence_incompatiblePropertyTypes_failsFastWithValidationException() {
      IncompatibleTypeDateSequenceDto dto = new IncompatibleTypeDateSequenceDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @DateSequence configuration on " + IncompatibleTypeDateSequenceDto.class.getName()
              + ": endField 'stringField' (java.lang.String) is not a supported date type");
    }

    @Test
    @DisplayName("Mismatched date types (LocalDate vs LocalDateTime) fails fast with ValidationException")
    void testDateSequence_mismatchedDateTypes_failsFastWithValidationException() {
      MismatchedDateTypesDateSequenceDto dto = new MismatchedDateTypesDateSequenceDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @DateSequence configuration on " + MismatchedDateTypesDateSequenceDto.class.getName()
              + ": startField 'startDate' (LocalDate) and endField 'dateTimeField' (LocalDateTime) have incompatible date types");
    }
  }

  // =========================================================================
  // 2. Financial Boundary & Numeric Validation Tests
  // =========================================================================

  @Nested
  @DisplayName("2. Financial Boundary & Numeric Validation")
  class FinancialBoundaryTests {

    @NumberRange(minField = "minValue", maxField = "maxValue", allowEqual = false)
    public static class StrictNumberRangeDto {
      private Integer minValue;
      private Integer maxValue;

      public Integer getMinValue() { return minValue; }
      public void setMinValue(Integer minValue) { this.minValue = minValue; }
      public Integer getMaxValue() { return maxValue; }
      public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }
    }

    @NumberRange(minField = "minValue", maxField = "maxValue", targetField = "customTarget")
    public static class CustomTargetNumberRangeDto {
      private Integer minValue;
      private Integer maxValue;
      private Integer customTarget;

      public Integer getMinValue() { return minValue; }
      public void setMinValue(Integer minValue) { this.minValue = minValue; }
      public Integer getMaxValue() { return maxValue; }
      public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }
      public Integer getCustomTarget() { return customTarget; }
      public void setCustomTarget(Integer customTarget) { this.customTarget = customTarget; }
    }

    @NumberRange(minField = "nonExistentMin", maxField = "maxValue")
    public static class MissingMinPropertyNumberRangeDto {
      private Integer maxValue = 100;
      public Integer getMaxValue() { return maxValue; }
      public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }
    }

    @NumberRange(minField = "minValue", maxField = "nonExistentMax")
    public static class MissingMaxPropertyNumberRangeDto {
      private Integer minValue = 10;
      public Integer getMinValue() { return minValue; }
      public void setMinValue(Integer minValue) { this.minValue = minValue; }
    }

    @NumberRange(minField = "minValue", maxField = "maxValue", targetField = "nonExistentTarget")
    public static class MissingTargetPropertyNumberRangeDto {
      private Integer minValue = 10;
      private Integer maxValue = 100;
      public Integer getMinValue() { return minValue; }
      public void setMinValue(Integer minValue) { this.minValue = minValue; }
      public Integer getMaxValue() { return maxValue; }
      public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }
    }

    @NumberRange(minField = "stringMin", maxField = "stringMax")
    public static class NonNumericNumberRangeDto {
      private String stringMin = "10";
      private String stringMax = "100";
      public String getStringMin() { return stringMin; }
      public void setStringMin(String stringMin) { this.stringMin = stringMin; }
      public String getStringMax() { return stringMax; }
      public void setStringMax(String stringMax) { this.stringMax = stringMax; }
    }

    @NumberRange(minField = "intMin", maxField = "decimalMax")
    public static class IncompatibleNumericNumberRangeDto {
      private Integer intMin = 10;
      private BigDecimal decimalMax = new BigDecimal("100.00");
      public Integer getIntMin() { return intMin; }
      public void setIntMin(Integer intMin) { this.intMin = intMin; }
      public BigDecimal getDecimalMax() { return decimalMax; }
      public void setDecimalMax(BigDecimal decimalMax) { this.decimalMax = decimalMax; }
    }

    @Test
    @DisplayName("maxSalary less than minSalary in CreateJobPostingRequest is rejected on maxSalary")
    void testSalaryRange_maxSalaryLessThanMinSalary_rejected() {
      CreateJobPostingRequest request = new CreateJobPostingRequest();
      request.setPostingCode("JOB-2026-001");
      request.setMinSalary(new BigDecimal("50000.00"));
      request.setMaxSalary(new BigDecimal("40000.00")); // Less than minSalary

      Set<ConstraintViolation<CreateJobPostingRequest>> violations = validator.validate(request);
      assertThat(violations).isNotEmpty();

      ConstraintViolation<CreateJobPostingRequest> violation = violations.stream()
          .filter(v -> "maxSalary".equals(v.getPropertyPath().toString()))
          .findFirst()
          .orElse(null);

      assertThat(violation).isNotNull();
      assertThat(violation.getMessage()).contains("must be greater than or equal to minimum value");
    }

    @Test
    @DisplayName("maxSalary equal to minSalary is accepted when allowEqual is true")
    void testSalaryRange_equalSalary_accepted() {
      CreateJobPostingRequest request = new CreateJobPostingRequest();
      request.setPostingCode("JOB-2026-001");
      request.setMinSalary(new BigDecimal("50000.00"));
      request.setMaxSalary(new BigDecimal("50000.00"));

      Set<ConstraintViolation<CreateJobPostingRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("allowEqual=false rejects equal values in NumberRange")
    void testNumberRange_allowEqualFalse_equalValues_rejected() {
      StrictNumberRangeDto request = new StrictNumberRangeDto();
      request.setMinValue(100);
      request.setMaxValue(100);

      Set<ConstraintViolation<StrictNumberRangeDto>> violations = validator.validate(request);
      assertThat(violations).isNotEmpty();
      assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("maxValue");

      request.setMaxValue(101);
      assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Custom targetField attaches violation to specified property node")
    void testNumberRange_customTargetField_attachesViolationToTargetField() {
      CustomTargetNumberRangeDto request = new CustomTargetNumberRangeDto();
      request.setMinValue(100);
      request.setMaxValue(50); // Invalid: max < min
      request.setCustomTarget(0);

      Set<ConstraintViolation<CustomTargetNumberRangeDto>> violations = validator.validate(request);
      assertThat(violations).isNotEmpty();
      assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("customTarget");
    }

    @Test
    @DisplayName("maxSalary greater than minSalary is accepted")
    void testSalaryRange_greaterSalary_accepted() {
      CreateJobPostingRequest request = new CreateJobPostingRequest();
      request.setPostingCode("JOB-2026-001");
      request.setMinSalary(new BigDecimal("50000.00"));
      request.setMaxSalary(new BigDecimal("80000.00"));

      Set<ConstraintViolation<CreateJobPostingRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Nullable values pass NumberRange validation")
    void testSalaryRange_nullableValues_accepted() {
      CreateJobPostingRequest requestMinOnly = new CreateJobPostingRequest();
      requestMinOnly.setPostingCode("JOB-2026-001");
      requestMinOnly.setMinSalary(new BigDecimal("50000.00"));
      requestMinOnly.setMaxSalary(null);
      assertThat(validator.validate(requestMinOnly)).isEmpty();

      CreateJobPostingRequest requestMaxOnly = new CreateJobPostingRequest();
      requestMaxOnly.setPostingCode("JOB-2026-001");
      requestMaxOnly.setMinSalary(null);
      requestMaxOnly.setMaxSalary(new BigDecimal("80000.00"));
      assertThat(validator.validate(requestMaxOnly)).isEmpty();
    }

    @Test
    @DisplayName("BigDecimal comparisons preserve exact precision without floating-point loss")
    void testSalaryRange_bigDecimalScaleComparison() {
      CreateJobPostingRequest request = new CreateJobPostingRequest();
      request.setPostingCode("JOB-2026-001");
      // Scale differences: 50000 vs 50000.00 must be equal via compareTo
      request.setMinSalary(new BigDecimal("50000"));
      request.setMaxSalary(new BigDecimal("50000.00"));

      Set<ConstraintViolation<CreateJobPostingRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();

      // Precision boundary: 50000.01 vs 50000.00
      request.setMinSalary(new BigDecimal("50000.01"));
      request.setMaxSalary(new BigDecimal("50000.00"));
      assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Negative salary/wage value is rejected by @PositiveOrZero")
    void testSalary_negativeValue_rejected() {
      CreateEmploymentRecordRequest request = new CreateEmploymentRecordRequest();
      request.setStartingSalary(new BigDecimal("-1.00"));

      Set<ConstraintViolation<CreateEmploymentRecordRequest>> violations = validator.validate(request);
      assertThat(violations).isNotEmpty();
      boolean hasSalaryViolation = violations.stream()
          .anyMatch(v -> "startingSalary".equals(v.getPropertyPath().toString()));
      assertThat(hasSalaryViolation).isTrue();
    }

    @Test
    @DisplayName("Zero or positive salary/wage value is accepted")
    void testSalary_zeroAndPositive_accepted() {
      CreateEmploymentRecordRequest requestZero = new CreateEmploymentRecordRequest();
      requestZero.setStartingSalary(BigDecimal.ZERO);
      assertThat(validator.validate(requestZero)).isEmpty();

      CreateEmploymentRecordRequest requestPos = new CreateEmploymentRecordRequest();
      requestPos.setStartingSalary(new BigDecimal("45000.00"));
      assertThat(validator.validate(requestPos)).isEmpty();
    }

    @Test
    @DisplayName("Missing minField fails fast with ValidationException (root cause IllegalArgumentException)")
    void testNumberRange_missingMinProperty_failsFastWithValidationException() {
      MissingMinPropertyNumberRangeDto dto = new MissingMinPropertyNumberRangeDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @NumberRange configuration on " + MissingMinPropertyNumberRangeDto.class.getName()
              + ": minField 'nonExistentMin' does not exist or is not accessible");
    }

    @Test
    @DisplayName("Missing maxField fails fast with ValidationException (root cause IllegalArgumentException)")
    void testNumberRange_missingMaxProperty_failsFastWithValidationException() {
      MissingMaxPropertyNumberRangeDto dto = new MissingMaxPropertyNumberRangeDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @NumberRange configuration on " + MissingMaxPropertyNumberRangeDto.class.getName()
              + ": maxField 'nonExistentMax' does not exist or is not accessible");
    }

    @Test
    @DisplayName("Missing targetField fails fast with ValidationException (root cause IllegalArgumentException)")
    void testNumberRange_missingTargetProperty_failsFastWithValidationException() {
      MissingTargetPropertyNumberRangeDto dto = new MissingTargetPropertyNumberRangeDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @NumberRange configuration on " + MissingTargetPropertyNumberRangeDto.class.getName()
              + ": targetField 'nonExistentTarget' does not exist or is not accessible");
    }

    @Test
    @DisplayName("Non-numeric property type fails fast with ValidationException")
    void testNumberRange_nonNumericPropertyType_failsFastWithValidationException() {
      NonNumericNumberRangeDto dto = new NonNumericNumberRangeDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @NumberRange configuration on " + NonNumericNumberRangeDto.class.getName()
              + ": minField 'stringMin' (java.lang.String) is not a supported numeric type");
    }

    @Test
    @DisplayName("Incompatible numeric types (Integer vs BigDecimal) fails fast with ValidationException")
    void testNumberRange_incompatibleNumericTypes_failsFastWithValidationException() {
      IncompatibleNumericNumberRangeDto dto = new IncompatibleNumericNumberRangeDto();
      assertThatThrownBy(() -> validator.validate(dto))
          .isInstanceOf(ValidationException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Invalid @NumberRange configuration on " + IncompatibleNumericNumberRangeDto.class.getName()
              + ": minField 'intMin' (Integer) and maxField 'decimalMax' (BigDecimal) have incompatible numeric types");
    }
  }

  // =========================================================================
  // 3. Date-of-Birth Sanity Validation Tests
  // =========================================================================

  @Nested
  @DisplayName("3. Date-of-Birth Sanity Validation")
  class DateOfBirthSanityTests {

    private final Clock fixedClock = Clock.fixed(
        Instant.parse("2026-10-27T00:00:00Z"),
        ZoneId.of("Asia/Kolkata")
    );

    @Test
    @DisplayName("Date of birth in the future is rejected")
    void testDateOfBirth_futureDate_rejected() {
      DateOfBirthValidator dobValidator = new DateOfBirthValidator(fixedClock);
      // Future date: 2026-10-28 when current date is 2026-10-27
      boolean valid = dobValidator.isValid(LocalDate.of(2026, 10, 28), null);
      assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Date of birth for age below 15 (e.g. 14 years 364 days) is rejected")
    void testDateOfBirth_under15_rejected() {
      DateOfBirthValidator dobValidator = new DateOfBirthValidator(fixedClock);
      dobValidator.initialize(new ValidDateOfBirth() {
        @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ValidDateOfBirth.class; }
        @Override public String message() { return "must be at least 15 years old and not in the future"; }
        @Override public Class<?>[] groups() { return new Class[0]; }
        @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
        @Override public int minAge() { return 15; }
      });

      // Born 2011-10-28 (turns 15 on 2026-10-28, today is 2026-10-27 => age 14)
      boolean valid = dobValidator.isValid(LocalDate.of(2011, 10, 28), null);
      assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Date of birth for exactly 15 years old is accepted")
    void testDateOfBirth_exactly15_accepted() {
      DateOfBirthValidator dobValidator = new DateOfBirthValidator(fixedClock);
      dobValidator.initialize(new ValidDateOfBirth() {
        @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ValidDateOfBirth.class; }
        @Override public String message() { return "must be at least 15 years old and not in the future"; }
        @Override public Class<?>[] groups() { return new Class[0]; }
        @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
        @Override public int minAge() { return 15; }
      });

      // Born 2011-10-27 (turns 15 today, 2026-10-27 => age exactly 15)
      boolean valid = dobValidator.isValid(LocalDate.of(2011, 10, 27), null);
      assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Date of birth for age above 15 is accepted")
    void testDateOfBirth_over15_accepted() {
      DateOfBirthValidator dobValidator = new DateOfBirthValidator(fixedClock);
      dobValidator.initialize(new ValidDateOfBirth() {
        @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ValidDateOfBirth.class; }
        @Override public String message() { return "must be at least 15 years old and not in the future"; }
        @Override public Class<?>[] groups() { return new Class[0]; }
        @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
        @Override public int minAge() { return 15; }
      });

      // Born 2000-01-01 (age 26)
      boolean valid = dobValidator.isValid(LocalDate.of(2000, 1, 1), null);
      assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Leap-year birthday (Feb 29) evaluates accurately on non-leap 15th year boundary")
    void testDateOfBirth_leapYearBirthday() {
      // Born on leap day: 2008-02-29
      // 15th birthday in non-leap year (2023): turns 15 on 2023-02-28
      LocalDate leapDob = LocalDate.of(2008, 2, 29);

      // On 2023-02-27 (1 day before 15th birthday) -> REJECTED
      Clock clockFeb27 = Clock.fixed(Instant.parse("2023-02-27T00:00:00Z"), ZoneId.of("UTC"));
      DateOfBirthValidator validatorFeb27 = new DateOfBirthValidator(clockFeb27);
      validatorFeb27.initialize(new ValidDateOfBirth() {
        @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ValidDateOfBirth.class; }
        @Override public String message() { return "must be at least 15 years old"; }
        @Override public Class<?>[] groups() { return new Class[0]; }
        @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
        @Override public int minAge() { return 15; }
      });
      assertThat(validatorFeb27.isValid(leapDob, null)).isFalse();

      // On 2023-02-28 (15th birthday) -> ACCEPTED
      Clock clockFeb28 = Clock.fixed(Instant.parse("2023-02-28T00:00:00Z"), ZoneId.of("UTC"));
      DateOfBirthValidator validatorFeb28 = new DateOfBirthValidator(clockFeb28);
      validatorFeb28.initialize(new ValidDateOfBirth() {
        @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ValidDateOfBirth.class; }
        @Override public String message() { return "must be at least 15 years old"; }
        @Override public Class<?>[] groups() { return new Class[0]; }
        @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
        @Override public int minAge() { return 15; }
      });
      assertThat(validatorFeb28.isValid(leapDob, null)).isTrue();
    }

    @Test
    @DisplayName("Null dateOfBirth on CreateTraineeRequest is valid (optional field)")
    void testDateOfBirth_nullIsValid() {
      CreateTraineeRequest request = new CreateTraineeRequest();
      request.setRegistrationNumber("REG-TEST-001");
      request.setFirstName("Ramesh");
      request.setDateOfBirth(null);

      Set<ConstraintViolation<CreateTraineeRequest>> violations = validator.validate(request);
      boolean hasDobViolation = violations.stream()
          .anyMatch(v -> "dateOfBirth".equals(v.getPropertyPath().toString()));
      assertThat(hasDobViolation).isFalse();
    }

    @Test
    @DisplayName("Runtime Bean Validation provider uses default system clock for CreateTraineeRequest")
    void testDateOfBirth_beanValidationProviderRuntime_validAndInvalid() {
      CreateTraineeRequest validRequest = new CreateTraineeRequest();
      validRequest.setRegistrationNumber("REG-TEST-002");
      validRequest.setFirstName("Priya");
      validRequest.setDateOfBirth(LocalDate.now().minusYears(20)); // Valid adult

      Set<ConstraintViolation<CreateTraineeRequest>> validViolations = validator.validate(validRequest);
      boolean hasDobViolationValid = validViolations.stream()
          .anyMatch(v -> "dateOfBirth".equals(v.getPropertyPath().toString()));
      assertThat(hasDobViolationValid).isFalse();

      CreateTraineeRequest futureRequest = new CreateTraineeRequest();
      futureRequest.setRegistrationNumber("REG-TEST-003");
      futureRequest.setFirstName("Priya");
      futureRequest.setDateOfBirth(LocalDate.now().plusDays(1)); // Invalid future

      Set<ConstraintViolation<CreateTraineeRequest>> futureViolations = validator.validate(futureRequest);
      boolean hasDobViolationFuture = futureViolations.stream()
          .anyMatch(v -> "dateOfBirth".equals(v.getPropertyPath().toString()));
      assertThat(hasDobViolationFuture).isTrue();
    }
  }

  // =========================================================================
  // 4. Programmatic DTO Constraint-Mapping Tests
  // =========================================================================

  @Nested
  @DisplayName("4. Programmatic DTO Constraint-Mapping Verification")
  class DtoConstraintMappingTests {

    @Test
    @DisplayName("All request DTOs with @DateSequence or @NumberRange have real, accessible, compatible properties")
    void testAllRequestDtos_crossFieldConstraints_haveValidMappings() throws Exception {
      ClassPathScanningCandidateComponentProvider scanner =
          new ClassPathScanningCandidateComponentProvider(false);
      scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

      Set<BeanDefinition> candidateBeans = scanner.findCandidateComponents("in.gov.sih.sih26135.dto");
      assertThat(candidateBeans).isNotEmpty();

      int verifiedDtoCount = 0;
      int verifiedConstraintCount = 0;

      for (BeanDefinition beanDef : candidateBeans) {
        Class<?> clazz = Class.forName(beanDef.getBeanClassName());

        DateSequence[] dateSequences = clazz.getAnnotationsByType(DateSequence.class);
        NumberRange[] numberRanges = clazz.getAnnotationsByType(NumberRange.class);

        if (dateSequences.length > 0 || numberRanges.length > 0) {
          verifiedDtoCount++;

          for (DateSequence ds : dateSequences) {
            ValidationPropertyUtils.validateDateSequenceConfiguration(clazz, ds);
            verifiedConstraintCount++;
          }

          for (NumberRange nr : numberRanges) {
            ValidationPropertyUtils.validateNumberRangeConfiguration(clazz, nr);
            verifiedConstraintCount++;
          }
        }
      }

      // Assert that at least 25 DTOs and 27 constraints are discovered and verified
      assertThat(verifiedDtoCount).isGreaterThanOrEqualTo(25);
      assertThat(verifiedConstraintCount).isGreaterThanOrEqualTo(27);
    }

    @Test
    @DisplayName("Invalid cross-field constraint mappings fail fast with descriptive IllegalArgumentException")
    void testConstraintMapping_invalidMapping_failsFastAndClearly() {
      // 1. Missing start property on DateSequence
      assertThatThrownBy(() -> ValidationPropertyUtils.validateDateSequenceConfiguration(
          CrossFieldDateSequenceTests.MissingStartPropertyDateSequenceDto.class,
          CrossFieldDateSequenceTests.MissingStartPropertyDateSequenceDto.class.getAnnotation(DateSequence.class)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("startField 'nonExistentStart' does not exist or is not accessible");

      // 2. Incompatible property type on DateSequence
      assertThatThrownBy(() -> ValidationPropertyUtils.validateDateSequenceConfiguration(
          CrossFieldDateSequenceTests.IncompatibleTypeDateSequenceDto.class,
          CrossFieldDateSequenceTests.IncompatibleTypeDateSequenceDto.class.getAnnotation(DateSequence.class)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("endField 'stringField' (java.lang.String) is not a supported date type");

      // 3. Mismatched date types on DateSequence
      assertThatThrownBy(() -> ValidationPropertyUtils.validateDateSequenceConfiguration(
          CrossFieldDateSequenceTests.MismatchedDateTypesDateSequenceDto.class,
          CrossFieldDateSequenceTests.MismatchedDateTypesDateSequenceDto.class.getAnnotation(DateSequence.class)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("have incompatible date types");

      // 4. Missing min property on NumberRange
      assertThatThrownBy(() -> ValidationPropertyUtils.validateNumberRangeConfiguration(
          FinancialBoundaryTests.MissingMinPropertyNumberRangeDto.class,
          FinancialBoundaryTests.MissingMinPropertyNumberRangeDto.class.getAnnotation(NumberRange.class)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("minField 'nonExistentMin' does not exist or is not accessible");

      // 5. Non-numeric property type on NumberRange
      assertThatThrownBy(() -> ValidationPropertyUtils.validateNumberRangeConfiguration(
          FinancialBoundaryTests.NonNumericNumberRangeDto.class,
          FinancialBoundaryTests.NonNumericNumberRangeDto.class.getAnnotation(NumberRange.class)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("minField 'stringMin' (java.lang.String) is not a supported numeric type");

      // 6. Incompatible numeric types on NumberRange
      assertThatThrownBy(() -> ValidationPropertyUtils.validateNumberRangeConfiguration(
          FinancialBoundaryTests.IncompatibleNumericNumberRangeDto.class,
          FinancialBoundaryTests.IncompatibleNumericNumberRangeDto.class.getAnnotation(NumberRange.class)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("have incompatible numeric types");
    }
  }

  // =========================================================================
  // 5. Exception Handling & Error Response Security Tests
  // =========================================================================

  @Nested
  @DisplayName("5. Exception Handling & Error Response Security")
  class ExceptionHandlingAndSecurityTests {

    @Test
    @DisplayName("Internal/entity constraint violation fails closed as HTTP 500 without leaking details")
    void testExceptionHandling_entityValidationFailsClosedAsHttp500() {
      GlobalExceptionHandler handler = new GlobalExceptionHandler();
      HttpServletRequest mockRequest = mock(HttpServletRequest.class);
      when(mockRequest.getRequestURI()).thenReturn("/api/v1/internal-action");

      @SuppressWarnings("unchecked")
      ConstraintViolation<Object> mockViolation = mock(ConstraintViolation.class);
      // Entity class origin (not a client request DTO)
      when(mockViolation.getRootBeanClass()).thenReturn((Class) Object.class);

      ConstraintViolationException ex = new ConstraintViolationException("Internal violation", Set.of(mockViolation));
      ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, mockRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).contains("An unexpected error occurred");
    }

    @Test
    @DisplayName("Client request DTO constraint violation produces HTTP 400 with sanitized details")
    void testExceptionHandling_dtoValidationReturnsHttp400() {
      GlobalExceptionHandler handler = new GlobalExceptionHandler();
      HttpServletRequest mockRequest = mock(HttpServletRequest.class);
      when(mockRequest.getRequestURI()).thenReturn("/api/v1/job-postings");

      @SuppressWarnings("unchecked")
      ConstraintViolation<Object> mockViolation = mock(ConstraintViolation.class);
      when(mockViolation.getRootBeanClass()).thenReturn((Class) CreateJobPostingRequest.class);
      when(mockViolation.getMessage()).thenReturn("must be greater than or equal to minimum value");

      jakarta.validation.Path mockPath = mock(jakarta.validation.Path.class);
      when(mockPath.toString()).thenReturn("maxSalary");
      when(mockViolation.getPropertyPath()).thenReturn(mockPath);

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(mockViolation));
      ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, mockRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("maxSalary: must be greater than or equal to minimum value");
    }

    @Test
    @DisplayName("Cross-field validation error details strictly map to intended targetField node")
    void testExceptionHandling_crossFieldViolationMapsToTargetField() {
      GlobalExceptionHandler handler = new GlobalExceptionHandler();
      HttpServletRequest mockRequest = mock(HttpServletRequest.class);
      when(mockRequest.getRequestURI()).thenReturn("/api/v1/training-batches");

      @SuppressWarnings("unchecked")
      ConstraintViolation<Object> mockViolation = mock(ConstraintViolation.class);
      when(mockViolation.getRootBeanClass()).thenReturn((Class) CreateTrainingBatchRequest.class);
      when(mockViolation.getMessage()).thenReturn("must be greater than or equal to start date");

      jakarta.validation.Path mockPath = mock(jakarta.validation.Path.class);
      when(mockPath.toString()).thenReturn("endDate");
      when(mockViolation.getPropertyPath()).thenReturn(mockPath);

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(mockViolation));
      ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, mockRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getDetails()).containsExactly("endDate: must be greater than or equal to start date");
    }

    @Test
    @DisplayName("Sensitive rejected values and unsanitized parameters are not leaked in error details")
    void testExceptionHandling_rejectedValuesAreNotLeaked() {
      GlobalExceptionHandler handler = new GlobalExceptionHandler();
      HttpServletRequest mockRequest = mock(HttpServletRequest.class);
      when(mockRequest.getRequestURI()).thenReturn("/api/v1/job-postings");

      @SuppressWarnings("unchecked")
      ConstraintViolation<Object> mockViolation = mock(ConstraintViolation.class);
      when(mockViolation.getRootBeanClass()).thenReturn((Class) CreateJobPostingRequest.class);
      // Attempted message with sensitive leaked data
      when(mockViolation.getMessage()).thenReturn("must be greater than or equal to minimum value with leaked secret SENTINEL_API_KEY_999");

      jakarta.validation.Path mockPath = mock(jakarta.validation.Path.class);
      when(mockPath.toString()).thenReturn("maxSalary");
      when(mockViolation.getPropertyPath()).thenReturn(mockPath);

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(mockViolation));
      ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, mockRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      // Should be sanitized to safe fallback
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("SENTINEL_API_KEY_999");
      assertThat(response.getBody().getDetails()).containsExactly("maxSalary: is invalid");
    }
  }

  // =========================================================================
  // 6. Controller Boundary & Persistence Prevention Tests
  // =========================================================================

  @Nested
  @DisplayName("6. Controller Boundary & Persistence Prevention Tests")
  class ControllerBoundaryAndPersistenceTests {

    @Test
    @DisplayName("JobPosting: maxSalary < minSalary rejected with 400 VALIDATION_ERROR before service execution")
    void testJobPosting_invalidSalaryRange_rejectedBeforeService() throws Exception {
      CreateJobPostingRequest invalidRequest = new CreateJobPostingRequest();
      invalidRequest.setPostingCode("JOB-TEST-001");
      invalidRequest.setPostingTitle("Software Engineer");
      invalidRequest.setMinSalary(new BigDecimal("80000.00"));
      invalidRequest.setMaxSalary(new BigDecimal("50000.00")); // Invalid

      mockMvc.perform(post("/api/v1/job-postings")
              .header("Authorization", tokenPlacementManage)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").value("Request validation failed"))
          .andExpect(jsonPath("$.details[0]").value("maxSalary: must be greater than or equal to minimum value"));

      // Verify persistence/service method was NEVER reached
      verify(jobPostingService, never()).createJobPosting(any());
    }

    @Test
    @DisplayName("JobPosting: valid salary range succeeds and invokes service method")
    void testJobPosting_validSalaryRange_reachesService() throws Exception {
      CreateJobPostingRequest validRequest = new CreateJobPostingRequest();
      validRequest.setPostingCode("JOB-TEST-001");
      validRequest.setPostingTitle("Software Engineer");
      validRequest.setMinSalary(new BigDecimal("50000.00"));
      validRequest.setMaxSalary(new BigDecimal("80000.00")); // Valid

      JobPostingResponse mockResponse = new JobPostingResponse(
          10L, "JOB-TEST-001", "Software Engineer", null, null, null, null, null, null, null,
          null, null, null, null, 1, new BigDecimal("50000.00"), new BigDecimal("80000.00"),
          null, null, null, null, null, null, null, null, null, null, null, null, null, null
      );

      when(jobPostingService.createJobPosting(any(CreateJobPostingRequest.class)))
          .thenReturn(mockResponse);

      mockMvc.perform(post("/api/v1/job-postings")
              .header("Authorization", tokenPlacementManage)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validRequest)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(10L));

      // Verify service method WAS called
      verify(jobPostingService).createJobPosting(any(CreateJobPostingRequest.class));
    }

    @Test
    @DisplayName("TrainingBatch: endDate < startDate rejected with 400 VALIDATION_ERROR before service execution")
    void testTrainingBatch_invalidDateSequence_rejectedBeforeService() throws Exception {
      CreateTrainingBatchRequest invalidRequest = new CreateTrainingBatchRequest();
      invalidRequest.setBatchCode("BATCH-2026-999");
      invalidRequest.setStartDate(LocalDate.of(2026, 8, 1));
      invalidRequest.setEndDate(LocalDate.of(2026, 7, 31)); // Earlier than start date

      mockMvc.perform(post("/api/v1/training-batches")
              .header("Authorization", tokenCourseManage)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").value("Request validation failed"))
          .andExpect(jsonPath("$.details[0]").value("endDate: must be greater than or equal to start date"));

      verify(trainingBatchService, never()).createBatch(any());
    }

    @Test
    @DisplayName("SkillGap: resolvedOn < identifiedOn rejected with 400 VALIDATION_ERROR before service execution")
    void testSkillGap_invalidDateSequence_rejectedBeforeService() throws Exception {
      CreateSkillGapRequest invalidRequest = new CreateSkillGapRequest();
      invalidRequest.setIdentifiedOn(LocalDate.of(2026, 4, 10));
      invalidRequest.setResolvedOn(LocalDate.of(2026, 4, 9)); // Earlier than identifiedOn

      mockMvc.perform(post("/api/v1/skill-gaps")
              .header("Authorization", tokenAssessmentManage)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").value("Request validation failed"))
          .andExpect(jsonPath("$.details[0]").value("resolvedOn: must be greater than or equal to start date"));

      verify(skillGapService, never()).createSkillGap(any());
    }

    @Test
    @DisplayName("Trainee: future dateOfBirth rejected with 400 VALIDATION_ERROR before service execution")
    void testTrainee_futureDateOfBirth_rejectedBeforeService() throws Exception {
      CreateTraineeRequest invalidRequest = new CreateTraineeRequest();
      invalidRequest.setRegistrationNumber("REG-TEST-555");
      invalidRequest.setFirstName("Sunil");
      invalidRequest.setDateOfBirth(LocalDate.now().plusDays(10)); // Future date

      mockMvc.perform(post("/api/v1/trainees")
              .header("Authorization", tokenTraineeWrite)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").value("Request validation failed"))
          .andExpect(jsonPath("$.details[0]").value("dateOfBirth: must be at least 15 years old and not in the future"));

      verify(traineeService, never()).createTrainee(any(), any());
    }
  }
}
