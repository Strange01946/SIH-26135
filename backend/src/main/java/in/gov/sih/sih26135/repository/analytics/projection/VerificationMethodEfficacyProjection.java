package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection for formal Query Q22 latest/current verification per employment spell, by method.
 *
 * @param verificationMethodCode verification method code
 * @param methodName human-readable method name
 * @param isTraineeSelfReported whether the method is trainee self-reported
 * @param isOfficialFlag whether the method constitutes official verification
 * @param recordVerificationStatusCode verification status code
 * @param currentVerificationCount count of current verifications using this method & status
 * @param traineeCount distinct trainees using this method & status
 */
public record VerificationMethodEfficacyProjection(
    String verificationMethodCode,
    String methodName,
    Boolean isTraineeSelfReported,
    Boolean isOfficialFlag,
    String recordVerificationStatusCode,
    Long currentVerificationCount,
    Long traineeCount
) {}
