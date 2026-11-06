package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing a verification status breakdown item (formal Query Q21).
 *
 * @param statusCode status code (VERIFIED, PENDING, SELF_REPORTED, UNVERIFIED, REJECTED)
 * @param statusName human-readable status name
 * @param isVerified whether the status represents verified employment
 * @param verificationCount total verification records in this category
 * @param traineeCount distinct trainees in this category
 * @param employmentCount distinct employment spells in this category
 * @param sharePct percentage of total verifications represented by this category
 */
public record VerificationStatusBreakdownResponse(
    String statusCode,
    String statusName,
    Boolean isVerified,
    Long verificationCount,
    Long traineeCount,
    Long employmentCount,
    BigDecimal sharePct
) {}
