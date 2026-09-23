package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnrollmentOutcomeFactResponse(
    Long enrollmentId,
    String enrollmentNumber,
    Long traineeId,
    Long programId,
    Long schemeId,
    Long courseId,
    Long providerId,
    Long centerId,
    Long batchId,
    LocalDate enrollmentDate,
    LocalDate startDate,
    LocalDate actualCompletionDate,
    Long enrollmentStatusId,
    String enrollmentStatusCode,
    Boolean isCompletedFlag,
    Long traineeStateId,
    Long traineeDistrictId,
    Long issuedCertificateCount,
    Integer hasIssuedCertificateFlag,
    Long placementCount,
    BigDecimal joinedPlacementCount,
    Integer hasJoinedPlacementFlag,
    Long employmentCount,
    BigDecimal currentEmploymentCount,
    BigDecimal wageEmploymentCount,
    BigDecimal selfEmploymentCount,
    BigDecimal apprenticeshipCount,
    Integer hasEmploymentFlag
) {}
