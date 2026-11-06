package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection for formal Query Q21 verification outcome distribution across all cycles.
 *
 * @param recordVerificationStatusCode verification status code (VERIFIED, PENDING, etc.)
 * @param statusName human-readable status name
 * @param isVerifiedFlag whether this status represents official/successful verification
 * @param verificationCount total verification records in this category
 * @param traineeCount distinct trainees in this category
 * @param employmentCount distinct employment spells in this category
 */
public record VerificationOutcomeProjection(
    String recordVerificationStatusCode,
    String statusName,
    Boolean isVerifiedFlag,
    Long verificationCount,
    Long traineeCount,
    Long employmentCount
) {}
