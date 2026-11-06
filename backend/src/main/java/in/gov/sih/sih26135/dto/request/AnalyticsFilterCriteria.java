package in.gov.sih.sih26135.dto.request;

import in.gov.sih.sih26135.exception.BadRequestException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Reusable multi-dimensional filter criteria DTO for analytics and dashboards.
 *
 * <p>Supports slicing across four analytical dimensions:
 * <ul>
 *   <li><b>Temporal:</b> Fiscal Year (e.g. {@code FY2024-25}), Calendar Year (e.g. {@code 2025}),
 *       or Custom Date Range ({@code startDate} to {@code endDate}).</li>
 *   <li><b>Organizational:</b> Scheme, Program, Sector, and Course identifiers.</li>
 *   <li><b>Spatial:</b> State and District identifiers.</li>
 *   <li><b>Demographic:</b> Gender (supported via {@code ref_gender} and {@code trainees.gender_id}).
 *       Social Category, Minority, and PwD classifications are explicitly validated; since the
 *       frozen database schema lacks columns for these dimensions ({@code Blocked — Existing Schema}),
 *       any non-null query for them is deterministically rejected fail-closed with HTTP 400.</li>
 * </ul>
 *
 * <p>Temporal Combination Rule:
 * Fiscal year, calendar year, and custom date range are mutually exclusive.
 * Specifying multiple temporal filter types is rejected with HTTP 400.
 */
public class AnalyticsFilterCriteria {

  private static final Set<String> SUPPORTED_GENDERS = Set.of("MALE", "FEMALE", "TRANSGENDER");

  // =========================================================================
  // 1. Temporal Dimensions
  // =========================================================================

  @Pattern(
      regexp = "^FY\\d{4}-\\d{2}$",
      message = "Fiscal year must match format 'FYyyyy-yy' (e.g. 'FY2024-25')"
  )
  private String fiscalYear;

  @Min(value = 2000, message = "Calendar year must be between 2000 and 2099")
  @Max(value = 2099, message = "Calendar year must be between 2000 and 2099")
  private Integer calendarYear;

  private LocalDate startDate;

  private LocalDate endDate;

  // =========================================================================
  // 2. Organizational Dimensions
  // =========================================================================

  @Positive(message = "schemeId must be positive")
  private Long schemeId;

  @Positive(message = "programId must be positive")
  private Long programId;

  @Positive(message = "sectorId must be positive")
  private Long sectorId;

  @Positive(message = "courseId must be positive")
  private Long courseId;

  // =========================================================================
  // 3. Spatial Dimensions
  // =========================================================================

  @Positive(message = "stateId must be positive")
  private Long stateId;

  @Positive(message = "districtId must be positive")
  private Long districtId;

  // =========================================================================
  // 4. Demographic Dimensions
  // =========================================================================

  private String gender;

  private String socialCategory;

  private String minority;

  private String pwd;

  // =========================================================================
  // Constructors
  // =========================================================================

  public AnalyticsFilterCriteria() {
  }

  // =========================================================================
  // Validation Logic
  // =========================================================================

  /**
   * Performs programmatic cross-field validation, enforcing temporal mutual exclusivity,
   * fiscal year consecutiveness, date ordering, demographic support, and spatial consistency.
   *
   * @throws BadRequestException if any filter combination or value is semantically invalid
   */
  public void validate() {
    // 1. Validate Fiscal Year Semantics
    if (fiscalYear != null && !fiscalYear.isBlank()) {
      String trimmed = fiscalYear.trim();
      if (!trimmed.matches("^FY\\d{4}-\\d{2}$")) {
        throw new BadRequestException(
            "Fiscal year must match format 'FYyyyy-yy' (e.g. 'FY2024-25')",
            "INVALID_FISCAL_YEAR"
        );
      }
      int startYear = Integer.parseInt(trimmed.substring(2, 6));
      int endYear = Integer.parseInt(trimmed.substring(7, 9));
      int expectedEndYear = (startYear + 1) % 100;
      if (endYear != expectedEndYear) {
        throw new BadRequestException(
            "Fiscal year must represent consecutive financial years: expected ending '"
                + String.format("%02d", expectedEndYear) + "' for start year " + startYear,
            "INVALID_FISCAL_YEAR"
        );
      }
      if (startYear < 2000 || startYear > 2099) {
        throw new BadRequestException(
            "Fiscal year start year must be between 2000 and 2099",
            "INVALID_FISCAL_YEAR"
        );
      }
    }

    // 2. Validate Calendar Year
    if (calendarYear != null) {
      if (calendarYear < 2000 || calendarYear > 2099) {
        throw new BadRequestException(
            "Calendar year must be between 2000 and 2099",
            "INVALID_CALENDAR_YEAR"
        );
      }
    }

    // 3. Validate Date Range
    if (startDate != null && endDate != null) {
      if (startDate.isAfter(endDate)) {
        throw new BadRequestException(
            "Start date (" + startDate + ") must not be after end date (" + endDate + ")",
            "INVALID_DATE_RANGE"
        );
      }
    }

    // 4. Validate Temporal Mutual Exclusivity
    int temporalCount = 0;
    if (fiscalYear != null && !fiscalYear.isBlank()) {
      temporalCount++;
    }
    if (calendarYear != null) {
      temporalCount++;
    }
    if (startDate != null || endDate != null) {
      temporalCount++;
    }
    if (temporalCount > 1) {
      throw new BadRequestException(
          "Conflicting temporal filters provided. Specify only one of fiscalYear, calendarYear, or custom date range",
          "CONFLICTING_TEMPORAL_FILTERS"
      );
    }

    // 5. Validate Gender Demographic
    if (gender != null && !gender.isBlank()) {
      String normalized = gender.trim().toUpperCase();
      if (!SUPPORTED_GENDERS.contains(normalized)) {
        throw new BadRequestException(
            "Unsupported gender classification '" + gender + "'. Supported values: MALE, FEMALE, TRANSGENDER",
            "INVALID_DEMOGRAPHIC_FILTER"
        );
      }
    }

    // 6. Validate Schema-Blocked Demographics (Social Category, Minority, PwD)
    if (socialCategory != null && !socialCategory.isBlank()) {
      throw new BadRequestException(
          "Demographic dimension 'socialCategory' is not supported by the existing database schema",
          "UNSUPPORTED_FILTER"
      );
    }
    if (minority != null && !minority.isBlank()) {
      throw new BadRequestException(
          "Demographic dimension 'minority' is not supported by the existing database schema",
          "UNSUPPORTED_FILTER"
      );
    }
    if (pwd != null && !pwd.isBlank()) {
      throw new BadRequestException(
          "Demographic dimension 'pwd' is not supported by the existing database schema",
          "UNSUPPORTED_FILTER"
      );
    }

    // 7. Validate Identifiers
    if (schemeId != null && schemeId <= 0) {
      throw new BadRequestException("schemeId must be positive", "INVALID_FILTER");
    }
    if (programId != null && programId <= 0) {
      throw new BadRequestException("programId must be positive", "INVALID_FILTER");
    }
    if (sectorId != null && sectorId <= 0) {
      throw new BadRequestException("sectorId must be positive", "INVALID_FILTER");
    }
    if (courseId != null && courseId <= 0) {
      throw new BadRequestException("courseId must be positive", "INVALID_FILTER");
    }
    if (stateId != null && stateId <= 0) {
      throw new BadRequestException("stateId must be positive", "INVALID_FILTER");
    }
    if (districtId != null && districtId <= 0) {
      throw new BadRequestException("districtId must be positive", "INVALID_FILTER");
    }
  }

  // =========================================================================
  // Resolution Helpers
  // =========================================================================

  /**
   * Resolves the effective inclusive start date for reporting based on the temporal specification.
   * Indian Financial Year starts on April 1.
   *
   * @return resolved start date, or null if no temporal start is specified
   */
  public LocalDate resolveEffectiveStartDate() {
    if (fiscalYear != null && !fiscalYear.isBlank()) {
      int startYear = Integer.parseInt(fiscalYear.trim().substring(2, 6));
      return LocalDate.of(startYear, 4, 1);
    }
    if (calendarYear != null) {
      return LocalDate.of(calendarYear, 1, 1);
    }
    return startDate;
  }

  /**
   * Resolves the effective inclusive end date for reporting based on the temporal specification.
   * Indian Financial Year ends on March 31.
   *
   * @return resolved end date, or null if no temporal end is specified
   */
  public LocalDate resolveEffectiveEndDate() {
    if (fiscalYear != null && !fiscalYear.isBlank()) {
      int startYear = Integer.parseInt(fiscalYear.trim().substring(2, 6));
      return LocalDate.of(startYear + 1, 3, 31);
    }
    if (calendarYear != null) {
      return LocalDate.of(calendarYear, 12, 31);
    }
    return endDate;
  }

  /**
   * Resolves normalized uppercase gender code.
   *
   * @return uppercase gender code, or null if gender is unspecified
   */
  public String resolveGenderCode() {
    return gender == null || gender.isBlank() ? null : gender.trim().toUpperCase();
  }

  /**
   * Resolves authoritative gender ID from {@code ref_gender} table:
   * 1 -> MALE, 2 -> FEMALE, 3 -> TRANSGENDER.
   *
   * @return authoritative gender ID, or null if unspecified
   */
  public Long resolveGenderId() {
    String code = resolveGenderCode();
    if (code == null) {
      return null;
    }
    return switch (code) {
      case "MALE" -> 1L;
      case "FEMALE" -> 2L;
      case "TRANSGENDER" -> 3L;
      default -> null;
    };
  }

  public boolean isEmpty() {
    return fiscalYear == null
        && calendarYear == null
        && startDate == null
        && endDate == null
        && schemeId == null
        && programId == null
        && sectorId == null
        && courseId == null
        && stateId == null
        && districtId == null
        && (gender == null || gender.isBlank())
        && (socialCategory == null || socialCategory.isBlank())
        && (minority == null || minority.isBlank())
        && (pwd == null || pwd.isBlank());
  }

  public boolean hasTemporalFilter() {
    return (fiscalYear != null && !fiscalYear.isBlank())
        || calendarYear != null
        || startDate != null
        || endDate != null;
  }

  public boolean hasOrganizationalFilter() {
    return schemeId != null || programId != null || sectorId != null || courseId != null;
  }

  public boolean hasSpatialFilter() {
    return stateId != null || districtId != null;
  }

  public boolean hasDemographicFilter() {
    return (gender != null && !gender.isBlank())
        || (socialCategory != null && !socialCategory.isBlank())
        || (minority != null && !minority.isBlank())
        || (pwd != null && !pwd.isBlank());
  }

  // =========================================================================
  // Getters and Setters
  // =========================================================================

  public String getFiscalYear() {
    return fiscalYear;
  }

  public void setFiscalYear(String fiscalYear) {
    this.fiscalYear = fiscalYear;
  }

  public Integer getCalendarYear() {
    return calendarYear;
  }

  public void setCalendarYear(Integer calendarYear) {
    this.calendarYear = calendarYear;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public Long getSchemeId() {
    return schemeId;
  }

  public void setSchemeId(Long schemeId) {
    this.schemeId = schemeId;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public Long getSectorId() {
    return sectorId;
  }

  public void setSectorId(Long sectorId) {
    this.sectorId = sectorId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getStateId() {
    return stateId;
  }

  public void setStateId(Long stateId) {
    this.stateId = stateId;
  }

  public Long getDistrictId() {
    return districtId;
  }

  public void setDistrictId(Long districtId) {
    this.districtId = districtId;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getSocialCategory() {
    return socialCategory;
  }

  public void setSocialCategory(String socialCategory) {
    this.socialCategory = socialCategory;
  }

  public String getMinority() {
    return minority;
  }

  public void setMinority(String minority) {
    this.minority = minority;
  }

  public String getPwd() {
    return pwd;
  }

  public void setPwd(String pwd) {
    this.pwd = pwd;
  }

  @Override
  public String toString() {
    return "AnalyticsFilterCriteria{" +
        "fiscalYear='" + fiscalYear + '\'' +
        ", calendarYear=" + calendarYear +
        ", startDate=" + startDate +
        ", endDate=" + endDate +
        ", schemeId=" + schemeId +
        ", programId=" + programId +
        ", sectorId=" + sectorId +
        ", courseId=" + courseId +
        ", stateId=" + stateId +
        ", districtId=" + districtId +
        ", gender='" + gender + '\'' +
        ", socialCategory='" + socialCategory + '\'' +
        ", minority='" + minority + '\'' +
        ", pwd='" + pwd + '\'' +
        '}';
  }
}
