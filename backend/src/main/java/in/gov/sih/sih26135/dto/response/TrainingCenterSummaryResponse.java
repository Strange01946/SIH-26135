package in.gov.sih.sih26135.dto.response;

/**
 * Standardized response record for Training Center summary in geographic dashboards.
 * Contains only non-PII operational and geographic identifiers.
 */
public record TrainingCenterSummaryResponse(
    Long centerId,
    String centerCode,
    String centerName,
    Integer operationalCapacity,
    String pincode
) {}
