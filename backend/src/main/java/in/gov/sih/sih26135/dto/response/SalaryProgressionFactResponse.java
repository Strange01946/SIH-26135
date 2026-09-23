package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryProgressionFactResponse(
    Long employmentId,
    Long traineeId,
    Long enrollmentId,
    Long courseId,
    Long providerId,
    Long engagementTypeId,
    String currencyCode,
    BigDecimal startingSalary,
    BigDecimal firstRecordedSalary,
    LocalDate firstSalaryEffectiveFrom,
    Integer firstObservationMonthOffset,
    BigDecimal latestRecordedSalary,
    LocalDate latestSalaryEffectiveFrom,
    LocalDate latestSalaryEffectiveTo,
    Integer latestObservationMonthOffset,
    String latestSalaryFrequencyCode,
    BigDecimal salaryChangeAmount,
    BigDecimal salaryChangePct
) {}
