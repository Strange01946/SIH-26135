package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Spring Data JPA native query interface projection for formal query Q01 (Dashboard KPI Strip).
 * Uses {@link Number} accessors to safely handle MySQL JDBC decimal/integer aggregate representations.
 */
public interface DashboardKpiProjection {

  Number getTraineeCount();

  Number getEnrollmentCount();

  Number getEnrolledTraineeCount();

  Number getCompletedEnrollmentCount();

  Number getCertifiedEnrollmentCount();

  Number getPlacementCount();

  Number getJoinedPlacementCount();

  Number getEmploymentSpellCount();

  Number getEmployedTraineeCount();

  Number getCurrentEmploymentSpellCount();

  Number getCurrentSelfEmploymentCount();

  Number getCurrentApprenticeshipCount();
}
