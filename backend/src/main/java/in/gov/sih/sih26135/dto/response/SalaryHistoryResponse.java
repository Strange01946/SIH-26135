package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SalaryHistoryResponse(
    Long id,
    Long employmentId,
    String employmentNumber,
    Long traineeId,
    String traineeRegistrationNumber,
    String traineeFirstName,
    String traineeLastName,
    BigDecimal salaryAmount,
    Long salaryFrequencyId,
    String salaryFrequencyCode,
    String salaryFrequencyName,
    String currencyCode,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Integer observationMonthOffset,
    Long employmentInfoSourceId,
    String employmentInfoSourceCode,
    String employmentInfoSourceName,
    Long recordVerificationStatusId,
    String recordVerificationStatusCode,
    LocalDateTime verifiedAt,
    Long verifiedByUserId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
