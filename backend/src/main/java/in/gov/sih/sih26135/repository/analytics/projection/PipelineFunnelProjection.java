package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Spring Data JPA native query interface projection for formal query Q02 (Training Pipeline Conversion Funnel).
 */
public interface PipelineFunnelProjection {

  Number getEnrollmentCount();

  Number getCompletedCount();

  Number getCertifiedCount();

  Number getJoinedPlacementCount();

  Number getEmployedCount();

  BigDecimal getCompletionRatePct();

  BigDecimal getCertificationRatePct();

  BigDecimal getPlacementRatePct();

  BigDecimal getEmploymentRatePct();
}
