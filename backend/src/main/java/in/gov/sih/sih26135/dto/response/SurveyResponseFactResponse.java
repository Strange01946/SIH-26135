package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SurveyResponseFactResponse(
    Long surveyResponseId,
    Long surveyId,
    Long surveyPurposeId,
    String surveyPurposeCode,
    Long programId,
    Long courseId,
    Long batchId,
    Integer followupOffsetMonths,
    Long traineeId,
    Long enrollmentId,
    Long followupTaskId,
    Integer attemptNumber,
    Long surveyResponseStatusId,
    String responseStatusCode,
    Boolean isSubmittedFlag,
    LocalDateTime startedAt,
    LocalDateTime submittedAt,
    Long traineeStateId,
    Long traineeDistrictId
) {}
