package in.gov.sih.sih26135.dto.response;

import in.gov.sih.sih26135.service.ExecutiveMisService.Stage;
import java.math.BigDecimal;
import java.util.List;

/**
 * Standardized response DTO representing training pipeline conversion metrics (Q02).
 * Captures stage counts as {@link Long} and conversion percentages as {@link BigDecimal}
 * with scale 2 and {@link java.math.RoundingMode#HALF_UP} rounding.
 */
public record PipelineFunnelResponse(
    Long enrollmentCount,
    Long completedCount,
    Long certifiedCount,
    Long joinedPlacementCount,
    Long employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct,
    List<Stage> candidateStages
) {
  public PipelineFunnelResponse(Long enrollmentCount, Long completedCount, Long certifiedCount,
      Long joinedPlacementCount, Long employedCount, BigDecimal completionRatePct, BigDecimal certificationRatePct,
      BigDecimal placementRatePct, BigDecimal employmentRatePct) {
    this(enrollmentCount, completedCount, certifiedCount, joinedPlacementCount, employedCount, completionRatePct,
        certificationRatePct, placementRatePct, employmentRatePct, null);
  }

  public static PipelineFunnelResponse executive(List<Stage> stages) {
    return new PipelineFunnelResponse(null, null, null, null, null, null, null, null, null, stages);
  }
}
