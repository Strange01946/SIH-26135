package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;

public record UnemploymentFactResponse(
    Long unemploymentEventId,
    Long traineeId,
    Integer periodNumber,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent,
    Integer durationDays,
    Long labourStatusId,
    String labourStatusCode,
    Long unemploymentReasonId,
    String unemploymentReasonCode,
    Long precedingEmploymentId,
    Long employmentExitEventId,
    Long succeedingEmploymentId,
    Integer reemployedFlag,
    Integer neverPrecededByEmploymentFlag,
    Long enrollmentId,
    Long placementId,
    Long followupTaskId,
    Long surveyResponseId,
    Long employmentInfoSourceId,
    String infoSourceCode,
    Boolean isSelfReportedFlag,
    Long recordVerificationStatusId,
    String recordVerificationStatusCode,
    Boolean isVerifiedFlag,
    Long traineeStateId,
    Long traineeDistrictId
) {}
