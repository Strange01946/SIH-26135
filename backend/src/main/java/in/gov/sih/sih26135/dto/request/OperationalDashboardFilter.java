package in.gov.sih.sih26135.dto.request;

import in.gov.sih.sih26135.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Validated shared filter parameters for Phase 28.7 operational dashboard endpoints.
 *
 * <p>Supports half-open timestamp intervals {@code [windowStart, windowEndExclusive)}:
 * <ul>
 *   <li>When {@code startDate} / {@code endDate} are ISO dates ({@code YYYY-MM-DD}), the window spans
 *       {@code [startDate 00:00:00, (endDate + 1 day) 00:00:00)}.</li>
 *   <li>When ISO date-times ({@code YYYY-MM-DDTHH:mm:ss}) are supplied, the exact half-open interval
 *       {@code [start, end)} is used, rejecting {@code start >= end}.</li>
 *   <li>When both are omitted, scorecard queries treat the window as unrestricted while windowed
 *       rate/volume metrics default to the trailing 30 calendar days ending at end of today.</li>
 * </ul>
 */
public record OperationalDashboardFilter(
    LocalDateTime windowStart,
    LocalDateTime windowEndExclusive,
    boolean timeWindowApplied,
    Long requestedStateId,
    Long requestedDistrictId,
    Long requestedTrainingProviderId
) {

  public static final int DEFAULT_WINDOW_DAYS = 30;

  public static OperationalDashboardFilter parseAndValidate(
      String rawStartDate,
      String rawEndDate,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      LocalDateTime evaluatedAt) {

    if (stateId != null && stateId <= 0) {
      throw new BadRequestException("stateId must be positive", "INVALID_FILTER_PARAMETER");
    }
    if (districtId != null && districtId <= 0) {
      throw new BadRequestException("districtId must be positive", "INVALID_FILTER_PARAMETER");
    }
    if (trainingProviderId != null && trainingProviderId <= 0) {
      throw new BadRequestException("trainingProviderId must be positive", "INVALID_FILTER_PARAMETER");
    }

    String cleanStart = rawStartDate != null && !rawStartDate.isBlank() ? rawStartDate.trim() : null;
    String cleanEnd = rawEndDate != null && !rawEndDate.isBlank() ? rawEndDate.trim() : null;

    if (cleanStart == null && cleanEnd == null) {
      LocalDate today = (evaluatedAt != null ? evaluatedAt : LocalDateTime.now()).toLocalDate();
      LocalDateTime defaultStart = today.minusDays(DEFAULT_WINDOW_DAYS - 1L).atStartOfDay();
      LocalDateTime defaultEndExclusive = today.plusDays(1L).atStartOfDay();
      return new OperationalDashboardFilter(
          defaultStart,
          defaultEndExclusive,
          false,
          stateId,
          districtId,
          trainingProviderId
      );
    }

    if (cleanStart == null || cleanEnd == null) {
      throw new BadRequestException(
          "Both startDate and endDate must be supplied together",
          "INVALID_DATE_RANGE"
      );
    }

    ParsedTemporal parsedStart = parseTemporal(cleanStart);
    ParsedTemporal parsedEnd = parseTemporal(cleanEnd);

    if (parsedStart.isDateOnly() != parsedEnd.isDateOnly()) {
      throw new BadRequestException(
          "startDate and endDate must use the same ISO date or date-time format",
          "INVALID_DATE_RANGE"
      );
    }

    LocalDateTime startInclusive;
    LocalDateTime endExclusive;
    if (parsedStart.isDateOnly()) {
      if (parsedStart.date().isAfter(parsedEnd.date())) {
        throw new BadRequestException("startDate cannot be after endDate", "INVALID_DATE_RANGE");
      }
      startInclusive = parsedStart.date().atStartOfDay();
      endExclusive = parsedEnd.date().plusDays(1L).atStartOfDay();
    } else {
      if (!parsedStart.dateTime().isBefore(parsedEnd.dateTime())) {
        throw new BadRequestException(
            "startDate must be strictly before endDate for date-time intervals",
            "INVALID_DATE_RANGE"
        );
      }
      startInclusive = parsedStart.dateTime();
      endExclusive = parsedEnd.dateTime();
    }

    return new OperationalDashboardFilter(
        startInclusive,
        endExclusive,
        true,
        stateId,
        districtId,
        trainingProviderId
    );
  }

  public BigDecimal windowDurationExactSeconds() {
    if (windowStart == null || windowEndExclusive == null || !windowStart.isBefore(windowEndExclusive)) {
      return BigDecimal.ZERO;
    }
    Duration duration = Duration.between(windowStart, windowEndExclusive);
    long nanos = duration.toNanos();
    if (nanos <= 0L) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(nanos).divide(BigDecimal.valueOf(1_000_000_000L));
  }

  private record ParsedTemporal(LocalDate date, LocalDateTime dateTime) {
    boolean isDateOnly() {
      return date != null;
    }
  }

  private static ParsedTemporal parseTemporal(String value) {
    try {
      return new ParsedTemporal(LocalDate.parse(value), null);
    } catch (DateTimeParseException ignored) {
      // Try ISO LocalDateTime next
    }
    try {
      return new ParsedTemporal(null, LocalDateTime.parse(value));
    } catch (DateTimeParseException ex) {
      throw new BadRequestException(
          "Invalid ISO date/datetime filter value",
          "INVALID_FILTER_PARAMETER"
      );
    }
  }
}
