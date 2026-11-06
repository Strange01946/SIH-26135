package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection for verification method distribution and efficacy (formal Query Q22 semantics),
 * grouping at the verification method level with database-side distinct trainee counting.
 *
 * @param verificationMethodCode verification method code
 * @param methodName human-readable method name
 * @param isTraineeSelfReported whether the method is trainee self-reported
 * @param isOfficialFlag whether the method constitutes official verification
 * @param currentVerificationCount count of current verifications using this method
 * @param traineeCount exact database-side distinct trainees using this method
 * @param verifiedCount count of verifications confirmed verified via this method
 * @param pendingCount count of verifications pending under this method
 * @param rejectedCount count of verifications rejected under this method
 */
public record VerificationMethodEfficacyProjection(
    String verificationMethodCode,
    String methodName,
    Boolean isTraineeSelfReported,
    Boolean isOfficialFlag,
    Long currentVerificationCount,
    Long traineeCount,
    Long verifiedCount,
    Long pendingCount,
    Long rejectedCount
) {}
