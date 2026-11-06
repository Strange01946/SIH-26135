package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing a verification method efficacy item (formal Query Q22).
 *
 * @param methodCode verification method code
 * @param methodName human-readable method name
 * @param isOfficial whether this method constitutes official verification
 * @param isTraineeSelfReported whether this method is trainee self-reported
 * @param currentVerificationCount total current verifications using this method
 * @param traineeCount distinct trainees using this method
 * @param verifiedCount count of verifications confirmed verified via this method
 * @param pendingCount count of verifications pending under this method
 * @param rejectedCount count of verifications rejected under this method
 * @param successRatePct percentage of attempts using this method successfully verified
 * @param sharePct percentage of all current verifications using this method
 */
public record VerificationMethodEfficacyResponse(
    String methodCode,
    String methodName,
    Boolean isOfficial,
    Boolean isTraineeSelfReported,
    Long currentVerificationCount,
    Long traineeCount,
    Long verifiedCount,
    Long pendingCount,
    Long rejectedCount,
    BigDecimal successRatePct,
    BigDecimal sharePct
) {}
