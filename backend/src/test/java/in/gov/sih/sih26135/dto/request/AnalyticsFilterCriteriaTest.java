package in.gov.sih.sih26135.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.gov.sih.sih26135.exception.BadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AnalyticsFilterCriteria Unit and Validation Tests")
class AnalyticsFilterCriteriaTest {

  @Nested
  @DisplayName("1. Temporal Validation and Resolution")
  class TemporalValidationTests {

    @Test
    @DisplayName("Valid Indian fiscal year (FY2024-25) resolves April 1 to March 31")
    void testValidFiscalYear() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setFiscalYear("FY2024-25");

      assertThatCode(criteria::validate).doesNotThrowAnyException();
      assertThat(criteria.resolveEffectiveStartDate()).isEqualTo(LocalDate.of(2024, 4, 1));
      assertThat(criteria.resolveEffectiveEndDate()).isEqualTo(LocalDate.of(2025, 3, 31));
      assertThat(criteria.hasTemporalFilter()).isTrue();
      assertThat(criteria.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Fiscal year with leap year (FY2023-24) correctly spans across Feb 29")
    void testFiscalYearLeapYear() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setFiscalYear("FY2023-24");

      assertThatCode(criteria::validate).doesNotThrowAnyException();
      assertThat(criteria.resolveEffectiveStartDate()).isEqualTo(LocalDate.of(2023, 4, 1));
      assertThat(criteria.resolveEffectiveEndDate()).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    @DisplayName("Fiscal year with century wrap-around (FY2099-00) is valid")
    void testFiscalYearCenturyWrap() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setFiscalYear("FY2099-00");

      assertThatCode(criteria::validate).doesNotThrowAnyException();
      assertThat(criteria.resolveEffectiveStartDate()).isEqualTo(LocalDate.of(2099, 4, 1));
      assertThat(criteria.resolveEffectiveEndDate()).isEqualTo(LocalDate.of(2100, 3, 31));
    }

    @Test
    @DisplayName("Malformed fiscal year format throws BadRequestException (INVALID_FISCAL_YEAR)")
    void testMalformedFiscalYear() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setFiscalYear("2024-25");

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "INVALID_FISCAL_YEAR");

      criteria.setFiscalYear("FY24-25");
      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "INVALID_FISCAL_YEAR");
    }

    @Test
    @DisplayName("Non-consecutive fiscal year (FY2024-26) throws BadRequestException (INVALID_FISCAL_YEAR)")
    void testNonConsecutiveFiscalYear() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setFiscalYear("FY2024-26");

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "INVALID_FISCAL_YEAR")
          .hasMessageContaining("consecutive financial years");
    }

    @Test
    @DisplayName("Valid calendar year (2025) resolves January 1 to December 31")
    void testValidCalendarYear() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setCalendarYear(2025);

      assertThatCode(criteria::validate).doesNotThrowAnyException();
      assertThat(criteria.resolveEffectiveStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
      assertThat(criteria.resolveEffectiveEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));
      assertThat(criteria.hasTemporalFilter()).isTrue();
    }

    @Test
    @DisplayName("Calendar year out of bounds throws BadRequestException (INVALID_CALENDAR_YEAR)")
    void testCalendarYearOutOfBounds() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setCalendarYear(1999);

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "INVALID_CALENDAR_YEAR");

      criteria.setCalendarYear(2100);
      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "INVALID_CALENDAR_YEAR");
    }

    @Test
    @DisplayName("Valid custom date range resolves properly")
    void testValidCustomDateRange() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStartDate(LocalDate.of(2024, 6, 1));
      criteria.setEndDate(LocalDate.of(2024, 12, 31));

      assertThatCode(criteria::validate).doesNotThrowAnyException();
      assertThat(criteria.resolveEffectiveStartDate()).isEqualTo(LocalDate.of(2024, 6, 1));
      assertThat(criteria.resolveEffectiveEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    @DisplayName("Reversed date range (startDate > endDate) throws BadRequestException (INVALID_DATE_RANGE)")
    void testReversedDateRange() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStartDate(LocalDate.of(2024, 12, 31));
      criteria.setEndDate(LocalDate.of(2024, 1, 1));

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "INVALID_DATE_RANGE")
          .hasMessageContaining("must not be after end date");
    }

    @Test
    @DisplayName("Conflicting temporal filters (fiscalYear + calendarYear) throws BadRequestException")
    void testConflictingTemporalFiscalAndCalendar() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setFiscalYear("FY2024-25");
      criteria.setCalendarYear(2024);

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONFLICTING_TEMPORAL_FILTERS");
    }

    @Test
    @DisplayName("Conflicting temporal filters (fiscalYear + custom date range) throws BadRequestException")
    void testConflictingTemporalFiscalAndRange() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setFiscalYear("FY2024-25");
      criteria.setStartDate(LocalDate.of(2024, 1, 1));

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONFLICTING_TEMPORAL_FILTERS");
    }
  }

  @Nested
  @DisplayName("2. Demographic Slicing and Schema Boundary Validation")
  class DemographicValidationTests {

    @Test
    @DisplayName("Supported genders (MALE, FEMALE, TRANSGENDER) validate and resolve to authoritative IDs")
    void testSupportedGenders() {
      AnalyticsFilterCriteria criteriaMale = new AnalyticsFilterCriteria();
      criteriaMale.setGender("MALE");
      assertThatCode(criteriaMale::validate).doesNotThrowAnyException();
      assertThat(criteriaMale.resolveGenderCode()).isEqualTo("MALE");
      assertThat(criteriaMale.resolveGenderId()).isEqualTo(1L);

      AnalyticsFilterCriteria criteriaFemale = new AnalyticsFilterCriteria();
      criteriaFemale.setGender("female"); // case-insensitive
      assertThatCode(criteriaFemale::validate).doesNotThrowAnyException();
      assertThat(criteriaFemale.resolveGenderCode()).isEqualTo("FEMALE");
      assertThat(criteriaFemale.resolveGenderId()).isEqualTo(2L);

      AnalyticsFilterCriteria criteriaTrans = new AnalyticsFilterCriteria();
      criteriaTrans.setGender("TRANSGENDER");
      assertThatCode(criteriaTrans::validate).doesNotThrowAnyException();
      assertThat(criteriaTrans.resolveGenderCode()).isEqualTo("TRANSGENDER");
      assertThat(criteriaTrans.resolveGenderId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Unsupported gender value throws BadRequestException (INVALID_DEMOGRAPHIC_FILTER)")
    void testUnsupportedGender() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setGender("UNKNOWN_GENDER");

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "INVALID_DEMOGRAPHIC_FILTER");
    }

    @Test
    @DisplayName("Social Category filter throws BadRequestException (UNSUPPORTED_FILTER) due to schema limitation")
    void testSocialCategorySchemaLimitation() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setSocialCategory("OBC");

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "UNSUPPORTED_FILTER")
          .hasMessageContaining("socialCategory");
    }

    @Test
    @DisplayName("Minority status filter throws BadRequestException (UNSUPPORTED_FILTER) due to schema limitation")
    void testMinoritySchemaLimitation() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setMinority("YES");

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "UNSUPPORTED_FILTER")
          .hasMessageContaining("minority");
    }

    @Test
    @DisplayName("PwD status filter throws BadRequestException (UNSUPPORTED_FILTER) due to schema limitation")
    void testPwdSchemaLimitation() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setPwd("YES");

      assertThatThrownBy(criteria::validate)
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "UNSUPPORTED_FILTER")
          .hasMessageContaining("pwd");
    }
  }

  @Nested
  @DisplayName("3. Identifier and Combinatorial Validation")
  class IdentifierValidationTests {

    @Test
    @DisplayName("Non-positive identifiers throw BadRequestException (INVALID_FILTER)")
    void testNonPositiveIdentifiers() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();

      criteria.setSchemeId(0L);
      assertThatThrownBy(criteria::validate).isInstanceOf(BadRequestException.class);

      criteria.setSchemeId(1L);
      criteria.setProgramId(-5L);
      assertThatThrownBy(criteria::validate).isInstanceOf(BadRequestException.class);

      criteria.setProgramId(1L);
      criteria.setStateId(0L);
      assertThatThrownBy(criteria::validate).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Empty criteria is correctly identified")
    void testEmptyCriteria() {
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      assertThat(criteria.isEmpty()).isTrue();
      assertThat(criteria.hasTemporalFilter()).isFalse();
      assertThat(criteria.hasOrganizationalFilter()).isFalse();
      assertThat(criteria.hasSpatialFilter()).isFalse();
      assertThat(criteria.hasDemographicFilter()).isFalse();
      assertThatCode(criteria::validate).doesNotThrowAnyException();
    }
  }
}
