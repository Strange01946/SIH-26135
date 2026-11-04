package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Analytical query projection for District-level Training Center summary.
 */
public record TrainingCenterSummaryProjection(
    Long centerId,
    String centerCode,
    String centerName,
    Integer operationalCapacity,
    String pincode
) {}
