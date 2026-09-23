package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;

public record EmploymentExitFactResponse(
    Long employmentExitEventId,
    Long employmentId,
    Long traineeId,
    Long enrollmentId,
    LocalDate separationDate,
    Long employmentExitReasonId,
    String exitReasonCode,
    Long separationNatureId,
    String separationNatureCode,
    Boolean isVoluntaryFlag,
    Boolean isInvoluntaryFlag,
    Long employmentInfoSourceId,
    String infoSourceCode,
    Long recordVerificationStatusId,
    String recordVerificationStatusCode,
    Long traineeStateId,
    Long traineeDistrictId
) {}
