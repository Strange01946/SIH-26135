package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmploymentVerificationFactResponse(
    Long employmentVerificationId,
    String verificationNumber,
    Long employmentId,
    Long traineeId,
    Long placementId,
    Long employerId,
    Integer cycleNumber,
    Boolean isReverification,
    Boolean isCurrent,
    Long recordVerificationStatusId,
    String recordVerificationStatusCode,
    Boolean isVerifiedFlag,
    Long employmentVerificationMethodId,
    String verificationMethodCode,
    Boolean isTraineeSelfReported,
    Boolean isOfficialFlag,
    Long employmentInfoSourceId,
    LocalDateTime requestedAt,
    LocalDateTime verifiedAt,
    Long traineeStateId,
    Long traineeDistrictId
) {}
