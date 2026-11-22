package in.gov.sih.sih26135.dto.assessor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary DTO of an assessment assigned to an assessor.
 */
public record AssessorAssessmentSummaryResponse(
    Long id,
    String assessmentCode,
    String assessmentName,
    Long assessmentTypeId,
    String assessmentTypeCode,
    String assessmentTypeName,
    Long courseId,
    String courseCode,
    String courseName,
    Long batchId,
    String batchCode,
    Long centerId,
    String centerCode,
    String centerName,
    Long providerId,
    String providerCode,
    String providerName,
    Long programId,
    String programCode,
    String programName,
    LocalDate assessmentDate,
    BigDecimal maximumScore,
    BigDecimal passScore,
    BigDecimal theoryMaxScore,
    BigDecimal practicalMaxScore,
    BigDecimal vivaMaxScore,
    Long lifecycleStatusId,
    String lifecycleStatusCode,
    int totalCandidates,
    int scoredCandidates,
    int passedCandidates,
    int failedCandidates,
    Long evaluatorUserId,
    String evaluatorName,
    Boolean isFinalized
) {}
