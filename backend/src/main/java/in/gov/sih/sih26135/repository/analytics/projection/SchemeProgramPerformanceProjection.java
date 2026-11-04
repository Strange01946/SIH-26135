package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Spring Data JPA native query interface projection for formal query Q03 (Scheme & Program Performance Matrix).
 */
public interface SchemeProgramPerformanceProjection {

  Long getProgramId();

  String getProgramCode();

  String getProgramName();

  Long getSchemeId();

  String getSchemeCode();

  String getSchemeName();

  Number getEnrollmentCount();

  Number getTraineeCount();

  Number getCompletedCount();

  Number getCertifiedCount();

  Number getJoinedPlacementCount();

  Number getEmployedCount();

  Number getSelfEmploymentSpellCount();

  Number getApprenticeshipSpellCount();

  BigDecimal getCompletionRatePct();

  BigDecimal getCertificationRatePct();

  BigDecimal getPlacementRatePct();

  BigDecimal getEmploymentRatePct();
}
