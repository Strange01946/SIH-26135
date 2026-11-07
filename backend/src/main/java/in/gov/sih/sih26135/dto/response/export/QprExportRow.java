package in.gov.sih.sih26135.dto.response.export;

import java.math.BigDecimal;

/**
 * Represents a single tabular/JSON row in the Ministry Quarterly Progress Report (QPR) MIS export.
 * Reflects state-level quarterly aggregation of training, completion, certification, placement, and employment.
 */
public record QprExportRow(
    Long stateId,
    String stateCode,
    String stateName,
    String fiscalYear,
    String quarter,
    Long enrollmentCount,
    Long traineeCount,
    Long completedCount,
    Long certifiedCount,
    Long joinedPlacementCount,
    Long employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct,
    Integer rank
) {}
