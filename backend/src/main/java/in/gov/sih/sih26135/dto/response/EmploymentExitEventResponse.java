package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmploymentExitEventResponse(
    Long id,
    Long employmentId,
    String employmentNumber,
    Long traineeId,
    String traineeRegistrationNumber,
    String traineeFirstName,
    String traineeLastName,
    Long enrollmentId,
    String enrollmentNumber,
    LocalDate separationDate,
    Long employmentExitReasonId,
    String employmentExitReasonCode,
    String employmentExitReasonName,
    Long separationNatureId,
    String separationNatureCode,
    String separationNatureName,
    Boolean isVoluntaryFlag,
    Boolean isInvoluntaryFlag,
    Long employmentInfoSourceId,
    String employmentInfoSourceCode,
    String employmentInfoSourceName,
    Long recordVerificationStatusId,
    String recordVerificationStatusCode,
    String recordVerificationStatusName,
    LocalDateTime verifiedAt,
    Long verifiedByUserId,
    String remarks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
