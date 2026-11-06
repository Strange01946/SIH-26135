package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated dashboard response DTO for Verification Funnel and Method Efficacy Analytics (Queries Q21 & Q22).
 *
 * @param totalVerifications total verification records across all cycles
 * @param totalEmployments distinct employment spells audited
 * @param totalTrainees distinct trainees audited
 * @param verifiedCount count of verified claims
 * @param officialVerifiedCount count of claims verified via official methods
 * @param selfReportedCount count of claims reported via trainee self-attestation/survey
 * @param pendingCount count of claims pending verification
 * @param rejectedCount count of claims rejected
 * @param unverifiedCount count of unverified claims
 * @param verificationCompletionRatePct percentage of claims that reached a final verified or rejected determination
 * @param verificationSuccessRatePct percentage of claims successfully verified
 * @param officialVerificationRatePct percentage of claims verified via official methods
 * @param selfReportedRatePct percentage of claims that are trainee self-reported
 * @param pendingRatePct percentage of claims pending verification
 * @param statusBreakdown distribution of verification outcomes across statuses (Q21)
 * @param methodEfficacyBreakdown distribution and efficacy across current verification methods (Q22)
 */
public record VerificationDashboardResponse(
    Long totalVerifications,
    Long totalEmployments,
    Long totalTrainees,
    Long verifiedCount,
    Long officialVerifiedCount,
    Long selfReportedCount,
    Long pendingCount,
    Long rejectedCount,
    Long unverifiedCount,
    BigDecimal verificationCompletionRatePct,
    BigDecimal verificationSuccessRatePct,
    BigDecimal officialVerificationRatePct,
    BigDecimal selfReportedRatePct,
    BigDecimal pendingRatePct,
    List<VerificationStatusBreakdownResponse> statusBreakdown,
    List<VerificationMethodEfficacyResponse> methodEfficacyBreakdown
) {}
