package in.gov.sih.sih26135.dto.assessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Candidate roster item with component scoring, pass/fail status, and certification details.
 */
public record AssessorCandidateRosterItemResponse(
    Long traineeId,
    String registrationNumber,
    String fullName,
    String gender,
    String email,
    String phone,
    Long enrollmentId,
    String enrollmentNumber,
    String enrollmentStatusCode,
    Long traineeAssessmentId,
    Integer attemptNumber,
    Boolean appearedFlag,
    LocalDate assessmentDate,
    Long assessmentResultId,
    BigDecimal theoryScore,
    BigDecimal practicalScore,
    BigDecimal vivaScore,
    BigDecimal totalScore,
    BigDecimal scorePercentage,
    Long assessmentOutcomeId,
    String assessmentOutcomeCode,
    String assessmentOutcomeName,
    Boolean isPassFlag,
    Boolean isFinalized,
    String remarks,
    Long certificateId,
    String certificateNumber,
    String certificateStatusCode,
    LocalDateTime resultDeclaredAt
) {}
