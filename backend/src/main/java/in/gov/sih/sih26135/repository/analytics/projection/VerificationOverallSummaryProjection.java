package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection representing authoritative nationwide aggregate totals over {@code vw_employment_verification_fact}.
 *
 * @param totalVerifications total verification records
 * @param totalEmployments distinct employment spells
 * @param totalTrainees distinct trainees
 * @param verifiedCount count of verified claims
 * @param officialVerifiedCount count of verified claims verified via official methods
 * @param selfReportedCount count of claims reported via trainee self-attestation/survey
 * @param pendingCount count of claims pending verification
 * @param rejectedCount count of claims rejected
 * @param unverifiedCount count of unverified claims
 */
public record VerificationOverallSummaryProjection(
    Long totalVerifications,
    Long totalEmployments,
    Long totalTrainees,
    Long verifiedCount,
    Long officialVerifiedCount,
    Long selfReportedCount,
    Long pendingCount,
    Long rejectedCount,
    Long unverifiedCount
) {}
